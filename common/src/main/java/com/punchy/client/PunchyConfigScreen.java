package com.punchy.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.punchy.ItemFetcher;
import com.punchy.PunchyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class PunchyConfigScreen extends Screen {

    // ── Sort mode ─────────────────────────────────────────────────────────────
    private enum SortMode {
        NAME("A-Z"), MOD("Mod"), TAG("Tag");
        final String label;
        SortMode(String l) { this.label = l; }
    }
    private SortMode sortMode = SortMode.NAME;

    // ── Display rows (items + separators) ─────────────────────────────────────
    /** A separator header row (spans full column width). */
    private record Sep(String label) {}
    /** A row of up to cols() item cells. */
    private record ItemRow(List<ItemFetcher.ItemInfo> cells) {}

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int CELL        = 20;
    private static final int SEP_H       = 13;   // separator row height (< CELL keeps grid aligned)
    private static final int SCROLLBAR_W = 4;
    private static final int GAP         = 6;
    private static final int PAD         = 4;

    private static final int SORT_BTN_W  = 26;
    private static final int SORT_BTN_H  = 14;
    private static final int SORT_COUNT  = SortMode.values().length;
    private static final int SORT_AREA_W = SORT_COUNT * SORT_BTN_W + (SORT_COUNT - 1) * 2 + PAD;

    private static final int HEADER_H  = 46;
    private static final int RULES_H   = 18;
    private static final int COL_HDR_H = 13;
    private static final int FOOTER_H  = 30;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private List<ItemFetcher.ItemInfo> allItems;
    private final List<ItemFetcher.ItemInfo> enabledItems  = new ArrayList<>();
    private final List<ItemFetcher.ItemInfo> disabledItems = new ArrayList<>();

    /** Flat display lists — each entry is either Sep or ItemRow. */
    private List<Object> enabledRows  = List.of();
    private List<Object> disabledRows = List.of();

    /** Items that threw during renderItem — skip to avoid per-frame exceptions. */
    private final Set<String> brokenItems = new HashSet<>();

    private EditBox searchBox;
    private int enabledScroll  = 0;
    private int disabledScroll = 0;

    private ItemStack pendingTooltipStack = null;
    private String    pendingTooltipId    = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public PunchyConfigScreen(Screen parent) {
        super(Component.literal("Punchy – Item Blacklist"));
        this.parent = parent;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (allItems == null) allItems = ItemFetcher.getAllItems();

        int labelW = this.font.width("Search: ");
        int sbX    = PAD + labelW + 2;
        int sbW    = Math.min(220, this.width - sbX - SORT_AREA_W - PAD);
        searchBox  = new EditBox(this.font, sbX, HEADER_H - 14, sbW, 12,
                Component.literal("Search"));
        searchBox.setMaxLength(128);
        searchBox.setResponder(t -> refresh());
        this.addRenderableWidget(searchBox);

        int footY = this.height - FOOTER_H + (FOOTER_H - 20) / 2;
        int x = PAD;

        this.addRenderableWidget(
                Button.builder(Component.literal("Add Rule / Regex"), btn ->
                        this.minecraft.setScreen(new PunchyRegexInputScreen(this, rule -> {
                            if (!PunchyConfig.instance.itemBlacklist.contains(rule)) {
                                PunchyConfig.instance.itemBlacklist.add(rule);
                                PunchyConfig.instance.save();
                                refresh();
                            }
                        })))
                        .bounds(x, footY, 118, 20).build());
        x += 122;

        this.addRenderableWidget(
                Button.builder(Component.literal("Add All"), btn -> addAll())
                        .bounds(x, footY, 58, 20).build());
        x += 62;

        this.addRenderableWidget(
                Button.builder(Component.literal("Remove All"), btn -> removeAll())
                        .bounds(x, footY, 74, 20).build());
        x += 78;

        this.addRenderableWidget(
                Button.builder(Component.literal("View Rules"), btn ->
                        this.minecraft.setScreen(new PunchyRulesScreen(this)))
                        .bounds(x, footY, 68, 20).build());

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn ->
                        this.minecraft.setScreen(this.parent))
                        .bounds(this.width - 104, footY, 100, 20).build());

        refresh();
    }

    // ── Button actions ────────────────────────────────────────────────────────

    /**
     * Adds one {@code namespace:.*} wildcard per mod in the enabled column instead
     * of thousands of individual item IDs (which would tank FPS in the chip row).
     */
    private void addAll() {
        LinkedHashSet<String> namespaces = new LinkedHashSet<>();
        for (ItemFetcher.ItemInfo info : enabledItems) {
            namespaces.add(info.id().getNamespace());
        }
        List<String> bl = PunchyConfig.instance.itemBlacklist;
        for (String ns : namespaces) {
            String pat = ns + ":.*";   // raw regex → passthrough in entryToRegex
            if (!bl.contains(pat)) bl.add(pat);
        }
        PunchyConfig.instance.save();
        refresh();
    }

    private void removeAll() {
        PunchyConfig.instance.itemBlacklist.clear();
        PunchyConfig.instance.save();
        refresh();
    }

    // ── Sort + grouping ───────────────────────────────────────────────────────

    private Comparator<ItemFetcher.ItemInfo> makeComparator() {
        return switch (sortMode) {
            case NAME -> Comparator.comparing(i -> i.id().toString());
            case MOD  -> Comparator.comparing((ItemFetcher.ItemInfo i) -> i.id().getNamespace())
                                   .thenComparing(i -> i.id().getPath());
            case TAG  -> Comparator.comparing(this::itemCategory)
                                   .thenComparing(i -> i.id().toString());
        };
    }

    private String itemCategory(ItemFetcher.ItemInfo info) {
        String p = info.id().getPath();
        if (p.endsWith("_sword") || (p.endsWith("_axe") && !p.contains("block")))
            return "0_weapons";
        if (p.endsWith("_pickaxe") || p.endsWith("_shovel") || p.endsWith("_hoe"))
            return "1_tools";
        if (p.endsWith("_helmet") || p.endsWith("_chestplate")
                || p.endsWith("_leggings") || p.endsWith("_boots"))
            return "2_armor";
        if (p.endsWith("_block") || p.endsWith("_slab") || p.endsWith("_stairs")
                || p.endsWith("_wall") || p.endsWith("_planks") || p.endsWith("_log")
                || p.endsWith("_bricks") || p.endsWith("_tile"))
            return "3_blocks";
        if (p.endsWith("_ore"))  return "4_ores";
        if (p.endsWith("_ingot") || p.endsWith("_nugget")
                || p.endsWith("_dust") || p.endsWith("_crystal")) return "5_materials";
        if (p.endsWith("_seeds") || p.endsWith("_sapling") || p.endsWith("_flower")
                || p.endsWith("_mushroom") || p.endsWith("_crop")) return "6_plants";
        return "9_misc";
    }

    private String groupKey(ItemFetcher.ItemInfo info) {
        return switch (sortMode) {
            case MOD  -> info.id().getNamespace();
            case NAME -> String.valueOf(info.id().getPath().charAt(0)).toUpperCase();
            case TAG  -> itemCategory(info);
        };
    }

    private String groupLabel(String key) {
        return switch (sortMode) {
            case MOD  -> key;
            case NAME -> key;
            case TAG  -> switch (key) {
                case "0_weapons"   -> "⚔ Weapons";
                case "1_tools"     -> "⛏ Tools";
                case "2_armor"     -> "🛡 Armor";
                case "3_blocks"    -> "🧱 Blocks";
                case "4_ores"      -> "⛏ Ores";
                case "5_materials" -> "✦ Materials";
                case "6_plants"    -> "🌿 Plants";
                default            -> "● Misc";
            };
        };
    }

    // ── Refresh / build display ───────────────────────────────────────────────

    private void refresh() {
        enabledItems.clear();
        disabledItems.clear();
        enabledScroll  = 0;
        disabledScroll = 0;

        String filter = searchBox != null ? searchBox.getValue().toLowerCase() : "";

        if (allItems != null) {
            for (ItemFetcher.ItemInfo info : allItems) {
                String id   = info.id().toString();
                String name = info.item().getName(new ItemStack(info.item()))
                        .getString().toLowerCase();
                if (!filter.isEmpty() && !id.contains(filter) && !name.contains(filter)) continue;

                if (PunchyConfig.instance.isBlacklisted(new ItemStack(info.item()))) {
                    disabledItems.add(info);
                } else {
                    enabledItems.add(info);
                }
            }
        }

        Comparator<ItemFetcher.ItemInfo> cmp = makeComparator();
        enabledItems.sort(cmp);
        disabledItems.sort(cmp);
        buildDisplay();
    }

    /**
     * Converts sorted item lists into flat display-row lists.
     * Each entry is either a {@link Sep} (full-width separator header)
     * or an {@link ItemRow} (up to {@link #cols()} cells).
     * Must be called after width is known and after sort.
     */
    private void buildDisplay() {
        enabledRows  = buildRows(enabledItems);
        disabledRows = buildRows(disabledItems);
    }

    private List<Object> buildRows(List<ItemFetcher.ItemInfo> items) {
        if (this.width == 0) return List.of();
        int nCols = cols();
        List<Object> rows = new ArrayList<>();
        String lastGroup = null;
        List<ItemFetcher.ItemInfo> pending = new ArrayList<>();

        for (ItemFetcher.ItemInfo info : items) {
            String g = groupKey(info);
            if (!g.equals(lastGroup)) {
                if (!pending.isEmpty()) { flushCells(pending, nCols, rows); pending.clear(); }
                rows.add(new Sep(groupLabel(g)));
                lastGroup = g;
            }
            pending.add(info);
        }
        if (!pending.isEmpty()) flushCells(pending, nCols, rows);
        return rows;
    }

    private void flushCells(List<ItemFetcher.ItemInfo> cells, int nCols, List<Object> rows) {
        for (int i = 0; i < cells.size(); i += nCols) {
            rows.add(new ItemRow(new ArrayList<>(cells.subList(i, Math.min(i + nCols, cells.size())))));
        }
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private int leftX()   { return PAD; }
    private int colW()    { return (this.width - 2 * PAD - GAP) / 2; }
    private int rightX()  { return leftX() + colW() + GAP; }
    private int gridTop() { return HEADER_H + RULES_H + COL_HDR_H; }
    private int gridH()   { return this.height - gridTop() - FOOTER_H; }
    private int cols()    { return Math.max(1, (colW() - SCROLLBAR_W - 2) / CELL); }

    private int rowHeight(Object row) {
        return (row instanceof Sep) ? SEP_H : CELL;
    }

    private int contentH(List<Object> rows) {
        int h = 0;
        for (Object row : rows) h += rowHeight(row);
        return h;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);

        pendingTooltipStack = null;
        pendingTooltipId    = null;

        drawPanels(g);
        drawHeader(g);
        drawRulesAndSortRow(g, mx, my);
        drawColumnHeaders(g);
        drawGrid(g, mx, my, enabledRows,  leftX(),  enabledScroll,  false);
        drawGrid(g, mx, my, disabledRows, rightX(), disabledScroll, true);

        super.render(g, mx, my, partial);

        if (pendingTooltipStack != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(pendingTooltipStack.getHoverName().getVisualOrderText());
            lines.add(Component.literal(pendingTooltipId)
                    .withStyle(ChatFormatting.GRAY).getVisualOrderText());
            g.renderTooltip(this.font, lines, mx, my);
        }
    }

    private void drawPanels(GuiGraphics g) {
        g.fill(0, HEADER_H, this.width, HEADER_H + RULES_H, 0x40FFFFFF);
        int chY = HEADER_H + RULES_H;
        g.fill(0, chY, this.width, chY + COL_HDR_H, 0x30FFFFFF);
        int sepX = leftX() + colW() + GAP / 2;
        g.fill(sepX, HEADER_H, sepX + 1, this.height - FOOTER_H, 0x55FFFFFF);
    }

    private void drawHeader(GuiGraphics g) {
        int cx = this.width / 2;
        g.drawCenteredString(font, this.title, cx, 4, 0xFFFFFF);
        g.drawCenteredString(font,
                "Click items to toggle · modid:item · modid · modid:*_sword · modid:.*_axe",
                cx, 15, 0x888888);
        g.drawString(font, "Search:", PAD, HEADER_H - 11, 0xAAAAAA, false);
    }

    private void drawRulesAndSortRow(GuiGraphics g, int mx, int my) {
        int rowY = HEADER_H;
        int rowH = RULES_H;
        int chipsRight = this.width - SORT_AREA_W;

        g.enableScissor(0, rowY, chipsRight, rowY + rowH);
        List<String> rules = PunchyConfig.instance.itemBlacklist;
        if (rules.isEmpty()) {
            g.drawString(font, "No rules – use 'Add Rule / Regex' below.",
                    PAD, rowY + (rowH - font.lineHeight) / 2, 0x555555, false);
        } else {
            int x  = PAD;
            int cy = rowY + 1, ch = rowH - 2;
            for (String rule : rules) {
                int chipW = font.width(rule) + 20;
                g.fill(x, cy, x + chipW, cy + ch, 0xAA222222);
                g.drawString(font, rule, x + 3, cy + (ch - font.lineHeight) / 2, 0xFFCC44, false);
                int xBtnX = x + chipW - 13;
                boolean hov = mx >= xBtnX && mx < xBtnX + 11 && my >= cy && my < cy + ch;
                g.drawString(font, "×", xBtnX, cy + (ch - font.lineHeight) / 2,
                        hov ? 0xFF6666 : 0x777777, false);
                x += chipW + 3;
            }
        }
        g.disableScissor();

        SortMode[] modes = SortMode.values();
        int btnY  = rowY + (rowH - SORT_BTN_H) / 2;
        int btnX0 = this.width - SORT_AREA_W + PAD / 2;
        for (int i = 0; i < modes.length; i++) {
            SortMode mode   = modes[i];
            int      btnX   = btnX0 + i * (SORT_BTN_W + 2);
            boolean  active = sortMode == mode;
            boolean  hov    = mx >= btnX && mx < btnX + SORT_BTN_W
                           && my >= btnY && my < btnY + SORT_BTN_H;
            int bg     = active ? 0xFF224422 : (hov ? 0x44FFFFFF : 0x22FFFFFF);
            int border = active ? 0xFF55AA55 : 0x44888888;
            g.fill(btnX, btnY, btnX + SORT_BTN_W, btnY + SORT_BTN_H, bg);
            g.fill(btnX,                   btnY,                   btnX + SORT_BTN_W, btnY + 1,           border);
            g.fill(btnX,                   btnY + SORT_BTN_H - 1,  btnX + SORT_BTN_W, btnY + SORT_BTN_H, border);
            g.fill(btnX,                   btnY,                   btnX + 1,           btnY + SORT_BTN_H, border);
            g.fill(btnX + SORT_BTN_W - 1, btnY,                   btnX + SORT_BTN_W, btnY + SORT_BTN_H, border);
            int lc = active ? 0xFF88FF88 : (hov ? 0xFFCCCCCC : 0xFF888888);
            g.drawCenteredString(font, mode.label,
                    btnX + SORT_BTN_W / 2, btnY + (SORT_BTN_H - font.lineHeight) / 2, lc);
        }
    }

    private void drawColumnHeaders(GuiGraphics g) {
        int y = HEADER_H + RULES_H;
        String sl = " [" + sortMode.label + "]";
        g.drawCenteredString(font, "✓ Enabled (" + enabledItems.size() + ")" + sl,
                leftX() + colW() / 2, y + 2, 0x55FF55);
        g.drawCenteredString(font, "✗ Disabled (" + disabledItems.size() + ")" + sl,
                rightX() + colW() / 2, y + 2, 0xFF5555);
    }

    // ── Item grid ─────────────────────────────────────────────────────────────

    private void drawGrid(GuiGraphics g, int mx, int my,
                          List<Object> rows, int colX, int scroll, boolean isDisabled) {
        int gTop = gridTop();
        int gH   = gridH();

        g.enableScissor(colX, gTop, colX + colW(), gTop + gH);

        if (rows.isEmpty()) {
            g.drawCenteredString(font,
                    isDisabled ? "No disabled items" : "No enabled items",
                    colX + colW() / 2, gTop + gH / 2 - 4, 0x444444);
        } else {
            // Walk rows, skip those entirely above/below viewport
            int y = gTop - scroll;
            for (Object row : rows) {
                int rh = rowHeight(row);
                if (y + rh > gTop && y < gTop + gH) {
                    if (row instanceof Sep sep) {
                        drawSepRow(g, colX, y, sep.label());
                    } else if (row instanceof ItemRow itemRow) {
                        drawItemRow(g, mx, my, colX, y, gTop, gH, itemRow, isDisabled);
                    }
                }
                y += rh;
                if (y >= gTop + gH) break;
            }
        }

        g.disableScissor();
        drawScrollbar(g, colX + colW() - SCROLLBAR_W, gTop, gH,
                      scroll, contentH(rows));
    }

    private void drawSepRow(GuiGraphics g, int colX, int y, String label) {
        g.fill(colX, y, colX + colW() - SCROLLBAR_W - 2, y + SEP_H, 0x55000000);
        g.fill(colX, y + SEP_H - 1, colX + colW() - SCROLLBAR_W - 2, y + SEP_H, 0x44FFFFFF);
        g.drawString(font, label, colX + 3, y + (SEP_H - font.lineHeight) / 2, 0xCCCCCC, false);
    }

    private void drawItemRow(GuiGraphics g, int mx, int my,
                             int colX, int y, int gTop, int gH,
                             ItemRow row, boolean isDisabled) {
        List<ItemFetcher.ItemInfo> cells = row.cells();
        for (int col = 0; col < cells.size(); col++) {
            int x = colX + col * CELL;

            ItemFetcher.ItemInfo info  = cells.get(col);
            ItemStack            stack = new ItemStack(info.item());

            boolean hovered = mx >= x && mx < x + CELL
                           && my >= y && my < y + CELL
                           && my >= gTop && my < gTop + gH;

            if (hovered) {
                g.fill(x, y, x + CELL, y + CELL, 0x55FFFFFF);
                pendingTooltipStack = stack;
                pendingTooltipId    = info.id().toString();
            } else if (isDisabled) {
                g.fill(x, y, x + CELL, y + CELL, 0x33FF2222);
            }

            String itemId = info.id().toString();
            if (!brokenItems.contains(itemId)) {
                try {
                    g.renderItem(stack, x + 2, y + 2);
                } catch (Exception e) {
                    brokenItems.add(itemId);
                    RenderSystem.enableDepthTest();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }
            }
        }
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int h, int scroll, int totalH) {
        if (totalH <= h) return;
        g.fill(x, y, x + SCROLLBAR_W, y + h, 0x22FFFFFF);
        float ratio  = (float) h / totalH;
        int   thumbH = Math.max(8, (int)(h * ratio));
        int   maxS   = totalH - h;
        int   thumbY = y + (maxS == 0 ? 0 : (int)((h - thumbH) * ((float) scroll / maxS)));
        g.fill(x, thumbY, x + SCROLLBAR_W, thumbY + thumbH, 0x99FFFFFF);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double dX, double dY) {
        if (super.mouseScrolled(mx, my, dX, dY)) return true;
        int gTop  = gridTop();
        int gH    = gridH();
        int delta = (int)(-dY * CELL);

        if (my >= gTop && my < gTop + gH) {
            if (mx >= leftX() && mx < leftX() + colW())
                enabledScroll  = clampScroll(enabledScroll  + delta, enabledRows,  gH);
            else if (mx >= rightX() && mx < rightX() + colW())
                disabledScroll = clampScroll(disabledScroll + delta, disabledRows, gH);
            return true;
        }
        return false;
    }

    private int clampScroll(int v, List<Object> rows, int h) {
        return Math.max(0, Math.min(v, Math.max(0, contentH(rows) - h)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        if (btn != 0) return false;

        // Sort buttons + chip × in rules row
        if (my >= HEADER_H && my < HEADER_H + RULES_H) {
            int btnY  = HEADER_H + (RULES_H - SORT_BTN_H) / 2;
            int btnX0 = this.width - SORT_AREA_W + PAD / 2;
            SortMode[] modes = SortMode.values();
            for (int i = 0; i < modes.length; i++) {
                int btnX = btnX0 + i * (SORT_BTN_W + 2);
                if (mx >= btnX && mx < btnX + SORT_BTN_W && my >= btnY && my < btnY + SORT_BTN_H) {
                    sortMode = modes[i];
                    refresh();
                    return true;
                }
            }
            int chipsRight = this.width - SORT_AREA_W;
            if (mx < chipsRight) {
                int x  = PAD;
                int cy = HEADER_H + 1, ch = RULES_H - 2;
                List<String> rules = PunchyConfig.instance.itemBlacklist;
                for (int i = 0; i < rules.size(); i++) {
                    String rule  = rules.get(i);
                    int    chipW = font.width(rule) + 20;
                    int    xBtn  = x + chipW - 13;
                    if (mx >= xBtn && mx < xBtn + 11 && my >= cy && my < cy + ch) {
                        rules.remove(i);
                        PunchyConfig.instance.save();
                        refresh();
                        return true;
                    }
                    x += chipW + 3;
                }
            }
        }

        // Grid item clicks — exclude scrollbar area (last SCROLLBAR_W px of each column)
        int gTop    = gridTop();
        int gH      = gridH();
        int clickableW = colW() - SCROLLBAR_W - 2;
        if (my >= gTop && my < gTop + gH) {
            if (mx >= leftX() && mx < leftX() + clickableW)
                return clickRow((int) mx, (int) my, leftX(),  enabledScroll,  enabledRows,  false);
            if (mx >= rightX() && mx < rightX() + clickableW)
                return clickRow((int) mx, (int) my, rightX(), disabledScroll, disabledRows, true);
        }
        return false;
    }

    private boolean clickRow(int mx, int my, int colX, int scroll,
                              List<Object> rows, boolean isDisabled) {
        int gTop   = gridTop();
        int targetY = my - gTop + scroll;  // Y within content
        int y = 0;
        for (Object row : rows) {
            int rh = rowHeight(row);
            if (targetY >= y && targetY < y + rh) {
                if (row instanceof Sep) return true;  // clicked separator — no action
                if (row instanceof ItemRow itemRow) {
                    int col = (mx - colX) / CELL;
                    if (col < 0 || col >= itemRow.cells().size()) return true;
                    String id = itemRow.cells().get(col).id().toString();
                    List<String> bl = PunchyConfig.instance.itemBlacklist;
                    if (bl.contains(id)) bl.remove(id); else bl.add(id);
                    PunchyConfig.instance.save();
                    refresh();
                    return true;
                }
            }
            y += rh;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
