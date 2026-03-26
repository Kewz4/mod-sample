package com.punchy.client;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Two-column item-grid config screen.
 *
 * Z-order (back → front):
 *   1. renderBackground() — MC world/blur (overridden to add solid dark overlay)
 *   2. Panel strips / fills / item renders
 *   3. super.render()     — Button + EditBox widgets
 *   4. Item tooltip        — drawn absolutely last
 */
public class PunchyConfigScreen extends Screen {

    // ── Sort mode ─────────────────────────────────────────────────────────────
    private enum SortMode {
        NAME("A-Z"), MOD("Mod"), TAG("Tag");
        final String label;
        SortMode(String l) { this.label = l; }
    }
    private SortMode sortMode = SortMode.NAME;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int CELL        = 20;
    private static final int SCROLLBAR_W = 3;
    private static final int GAP         = 6;    // gap between the two columns
    private static final int PAD         = 4;    // outer padding

    // Sort buttons drawn on right end of the rules row
    private static final int SORT_BTN_W  = 26;
    private static final int SORT_BTN_H  = 14;
    private static final int SORT_COUNT  = SortMode.values().length;
    private static final int SORT_AREA_W = SORT_COUNT * SORT_BTN_W + (SORT_COUNT - 1) * 2 + PAD;

    private static final int HEADER_H    = 52;   // title + help lines + search row
    private static final int RULES_H     = 22;   // active-rules chip row (+ sort buttons)
    private static final int COL_HDR_H   = 13;   // "✓ ENABLED / ✗ DISABLED" labels
    private static final int FOOTER_H    = 30;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private List<ItemFetcher.ItemInfo> allItems;
    private final List<ItemFetcher.ItemInfo> enabledItems  = new ArrayList<>();
    private final List<ItemFetcher.ItemInfo> disabledItems = new ArrayList<>();

    private EditBox searchBox;
    private int enabledScroll  = 0;
    private int disabledScroll = 0;
    private int rulesScrollX   = 0;

    private ItemStack pendingTooltipStack = null;
    private String    pendingTooltipId    = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public PunchyConfigScreen(Screen parent) {
        super(Component.literal("Punchy – Item Blacklist"));
        this.parent = parent;
    }

    // ── Background ────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // Use the standard MC background (dirt/panorama in main menu, world+overlay
        // in-game). MixinGameRenderer cancels the blur shader so the world stays sharp.
        super.renderBackground(g, mx, my, partial);
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (allItems == null) allItems = ItemFetcher.getAllItems();

        // Search box: left-label + box that stops before the sort button area
        int labelW = this.font.width("Search: ");
        int sbX    = PAD + labelW + 2;
        int sbW    = Math.min(220, this.width - sbX - SORT_AREA_W - PAD);
        searchBox  = new EditBox(this.font, sbX, HEADER_H - 18, sbW, 16,
                Component.literal("Search"));
        searchBox.setMaxLength(128);
        searchBox.setResponder(t -> refresh());
        this.addRenderableWidget(searchBox);

        // Footer buttons
        int footY = this.height - FOOTER_H + 5;
        this.addRenderableWidget(
                Button.builder(Component.literal("Add Rule / Regex"), btn ->
                        this.minecraft.setScreen(new PunchyRegexInputScreen(this, rule -> {
                            if (!PunchyConfig.instance.itemBlacklist.contains(rule)) {
                                PunchyConfig.instance.itemBlacklist.add(rule);
                                PunchyConfig.instance.save();
                                refresh();
                            }
                        })))
                        .bounds(PAD, footY, 130, 20)
                        .build());

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn ->
                        this.minecraft.setScreen(this.parent))
                        .bounds(this.width - 106, footY, 100, 20)
                        .build());

        refresh();
    }

    // ── Sorting + refresh ─────────────────────────────────────────────────────

    private Comparator<ItemFetcher.ItemInfo> makeComparator() {
        return switch (sortMode) {
            case NAME -> Comparator.comparing(i -> i.id().toString());
            case MOD  -> Comparator.comparing((ItemFetcher.ItemInfo i) -> i.id().getNamespace())
                                   .thenComparing(i -> i.id().getPath());
            case TAG  -> Comparator.comparing(this::itemCategory)
                                   .thenComparing(i -> i.id().toString());
        };
    }

    /**
     * Returns a stable sort key based on common item-ID suffixes, so items of
     * the same "type" (weapons, tools, armour, blocks…) cluster together.
     */
    private String itemCategory(ItemFetcher.ItemInfo info) {
        String p = info.id().getPath();
        if (p.endsWith("_sword"))                                        return "0_weapons";
        if (p.endsWith("_axe") && !p.contains("block"))                  return "0_weapons";
        if (p.endsWith("_pickaxe") || p.endsWith("_shovel")
                || p.endsWith("_hoe"))                                   return "1_tools";
        if (p.endsWith("_helmet") || p.endsWith("_chestplate")
                || p.endsWith("_leggings") || p.endsWith("_boots"))      return "2_armor";
        if (p.endsWith("_block") || p.endsWith("_slab")
                || p.endsWith("_stairs") || p.endsWith("_wall")
                || p.endsWith("_planks") || p.endsWith("_log")
                || p.endsWith("_bricks") || p.endsWith("_tile"))         return "3_blocks";
        if (p.endsWith("_ore"))                                           return "4_ores";
        if (p.endsWith("_ingot") || p.endsWith("_nugget")
                || p.endsWith("_dust") || p.endsWith("_crystal"))        return "5_materials";
        if (p.endsWith("_seeds") || p.endsWith("_sapling")
                || p.endsWith("_flower") || p.endsWith("_mushroom")
                || p.endsWith("_crop"))                                   return "6_plants";
        return "9_misc";
    }

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
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private int leftX()   { return PAD; }
    private int colW()    { return (this.width - 2 * PAD - GAP) / 2; }
    private int rightX()  { return leftX() + colW() + GAP; }
    private int gridTop() { return HEADER_H + RULES_H + COL_HDR_H; }
    private int gridH()   { return this.height - gridTop() - FOOTER_H; }
    private int cols()    { return Math.max(1, (colW() - SCROLLBAR_W - 1) / CELL); }
    private int contentH(int count) {
        int c = cols();
        return c == 0 ? 0 : ((count + c - 1) / c) * CELL;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);   // solid dark overlay included

        pendingTooltipStack = null;
        pendingTooltipId    = null;

        drawPanels(g);
        drawHeader(g);
        drawRulesAndSortRow(g, mx, my);
        drawColumnHeaders(g);
        drawGrid(g, mx, my, enabledItems,  leftX(),  enabledScroll,  false);
        drawGrid(g, mx, my, disabledItems, rightX(), disabledScroll, true);

        super.render(g, mx, my, partial);   // widgets on top of everything

        // Tooltip last – floats above all widgets
        if (pendingTooltipStack != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(pendingTooltipStack.getHoverName().getVisualOrderText());
            lines.add(Component.literal(pendingTooltipId)
                    .withStyle(ChatFormatting.GRAY).getVisualOrderText());
            g.renderTooltip(this.font, lines, mx, my);
        }
    }

    // ── Panel strips ──────────────────────────────────────────────────────────

    private void drawPanels(GuiGraphics g) {
        // Rules / sort strip (slightly lighter than the dark base)
        g.fill(0, HEADER_H, this.width, HEADER_H + RULES_H, 0x50FFFFFF);
        // Column-header strip
        int chY = HEADER_H + RULES_H;
        g.fill(0, chY, this.width, chY + COL_HDR_H, 0x40FFFFFF);
        // Vertical divider
        int sepX = leftX() + colW() + GAP / 2;
        g.fill(sepX, HEADER_H, sepX + 1, this.height - FOOTER_H, 0x66FFFFFF);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private void drawHeader(GuiGraphics g) {
        int cx = this.width / 2;
        g.drawCenteredString(font, this.title, cx, 4, 0xFFFFFF);
        g.drawCenteredString(font,
                "Click items to toggle.  Use  modid:item,  modid,  or  modid:*_sword  wildcards.",
                cx, 14, 0x888888);
        g.drawCenteredString(font,
                "F3+H to find item IDs  ·  Regex also supported  (e.g.  examplemod:.*_axe)",
                cx, 23, 0x666666);
        g.drawString(font, "Search:", PAD, HEADER_H - 13, 0xAAAAAA, false);
    }

    // ── Rules chips + sort buttons (share one row) ────────────────────────────

    private void drawRulesAndSortRow(GuiGraphics g, int mx, int my) {
        int rowY = HEADER_H;
        int rowH = RULES_H;

        // ── Chips (left side, clipped before the sort area) ───────────────────
        int chipsRight = this.width - SORT_AREA_W;
        g.enableScissor(0, rowY, chipsRight, rowY + rowH);

        List<String> rules = PunchyConfig.instance.itemBlacklist;
        if (rules.isEmpty()) {
            g.drawString(font, "No active rules – use 'Add Rule / Regex' to add one.",
                    PAD - rulesScrollX, rowY + (rowH - font.lineHeight) / 2, 0x555555, false);
        } else {
            int x  = PAD - rulesScrollX;
            int cy = rowY + 2;
            int ch = rowH - 4;
            for (String rule : rules) {
                int chipW = font.width(rule) + 22;
                g.fill(x, cy, x + chipW, cy + ch, 0xAA222222);
                g.fill(x, cy, x + chipW, cy + 1,  0x66888888);  // top border
                g.drawString(font, rule, x + 4, cy + (ch - font.lineHeight) / 2, 0xFFCC44, false);
                int xBtnX = x + chipW - 14;
                boolean hov = mx >= xBtnX && mx < xBtnX + 12 && my >= cy && my < cy + ch;
                g.drawString(font, "×", xBtnX, cy + (ch - font.lineHeight) / 2,
                        hov ? 0xFF4455 : 0x777777, false);
                x += chipW + 4;
            }
        }

        g.disableScissor();

        // ── Sort buttons (right side, always visible) ─────────────────────────
        SortMode[] modes = SortMode.values();
        int btnY  = rowY + (rowH - SORT_BTN_H) / 2;
        int btnX0 = this.width - SORT_AREA_W + PAD / 2;

        for (int i = 0; i < modes.length; i++) {
            SortMode mode   = modes[i];
            int      btnX   = btnX0 + i * (SORT_BTN_W + 2);
            boolean  active = sortMode == mode;
            boolean  hov    = mx >= btnX && mx < btnX + SORT_BTN_W
                           && my >= btnY && my < btnY + SORT_BTN_H;

            // bg
            int bg = active ? 0xFF224422 : (hov ? 0x44FFFFFF : 0x22FFFFFF);
            g.fill(btnX, btnY, btnX + SORT_BTN_W, btnY + SORT_BTN_H, bg);
            // border (bright if active)
            int border = active ? 0xFF55AA55 : 0x44888888;
            g.fill(btnX,                   btnY,                    btnX + SORT_BTN_W, btnY + 1,           border);
            g.fill(btnX,                   btnY + SORT_BTN_H - 1,   btnX + SORT_BTN_W, btnY + SORT_BTN_H, border);
            g.fill(btnX,                   btnY,                    btnX + 1,           btnY + SORT_BTN_H, border);
            g.fill(btnX + SORT_BTN_W - 1, btnY,                    btnX + SORT_BTN_W, btnY + SORT_BTN_H, border);

            int labelColor = active ? 0xFF88FF88 : (hov ? 0xFFCCCCCC : 0xFF888888);
            g.drawCenteredString(font, mode.label,
                    btnX + SORT_BTN_W / 2,
                    btnY + (SORT_BTN_H - font.lineHeight) / 2,
                    labelColor);
        }
    }

    // ── Column headers ────────────────────────────────────────────────────────

    private void drawColumnHeaders(GuiGraphics g) {
        int y    = HEADER_H + RULES_H;
        String sortLabel = " [" + sortMode.label + "]";
        g.drawCenteredString(font, "✓ Enabled  (" + enabledItems.size() + ")" + sortLabel,
                leftX() + colW() / 2, y + 2, 0x55FF55);
        g.drawCenteredString(font, "✗ Disabled  (" + disabledItems.size() + ")" + sortLabel,
                rightX() + colW() / 2, y + 2, 0xFF5555);
    }

    // ── Item grids ────────────────────────────────────────────────────────────

    private void drawGrid(GuiGraphics g, int mx, int my,
                          List<ItemFetcher.ItemInfo> items,
                          int colX, int scroll, boolean isDisabled) {
        int gTop  = gridTop();
        int gH    = gridH();
        int nCols = cols();

        g.enableScissor(colX, gTop, colX + colW(), gTop + gH);

        if (items.isEmpty()) {
            g.drawCenteredString(font,
                    isDisabled ? "No disabled items" : "No enabled items",
                    colX + colW() / 2, gTop + gH / 2 - 4, 0x444444);
        } else {
            for (int i = 0; i < items.size(); i++) {
                int col = i % nCols;
                int row = i / nCols;
                int x   = colX + col * CELL;
                int y   = gTop - scroll + row * CELL;

                if (y + CELL <= gTop || y >= gTop + gH) continue;

                ItemFetcher.ItemInfo info  = items.get(i);
                ItemStack            stack = new ItemStack(info.item());

                boolean hovered = mx >= x && mx < x + CELL
                               && my >= y && my < y + CELL
                               && my >= gTop && my < gTop + gH;

                // Per-cell background so items are visible against the dark screen
                g.fill(x, y, x + CELL, y + CELL, 0xFF2A2A3C);

                if (hovered) {
                    g.fill(x, y, x + CELL, y + CELL, 0x66FFFFFF);
                    pendingTooltipStack = stack;
                    pendingTooltipId    = info.id().toString();
                } else if (isDisabled) {
                    g.fill(x, y, x + CELL, y + CELL, 0x44FF2222);
                }

                try {
                    g.renderItem(stack, x + 2, y + 2);
                } catch (Exception ignored) {
                    // Some mod items (e.g. Avaritia cosmic items) crash when rendered
                    // outside of a world context — skip them silently.
                }

                // Red tint on top of item for disabled cells
                if (isDisabled && !hovered) {
                    g.fill(x, y, x + CELL, y + CELL, 0x33FF2222);
                }
            }
        }

        g.disableScissor();

        drawScrollbar(g, colX + colW() - SCROLLBAR_W, gTop, gH, scroll, contentH(items.size()));
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
                enabledScroll  = clampScroll(enabledScroll  + delta, enabledItems.size(),  gH);
            else if (mx >= rightX() && mx < rightX() + colW())
                disabledScroll = clampScroll(disabledScroll + delta, disabledItems.size(), gH);
            return true;
        }
        if (my >= HEADER_H && my < HEADER_H + RULES_H) {
            rulesScrollX = Math.max(0, rulesScrollX - (int)(dY * 20));
            return true;
        }
        return false;
    }

    private int clampScroll(int v, int count, int h) {
        return Math.max(0, Math.min(v, Math.max(0, contentH(count) - h)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        if (btn != 0) return false;

        // Sort buttons (rules row, right side)
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

            // Chip × buttons
            int chipsRight = this.width - SORT_AREA_W;
            if (mx < chipsRight) {
                int x  = PAD - rulesScrollX;
                int cy = HEADER_H + 2, ch = RULES_H - 4;
                List<String> rules = PunchyConfig.instance.itemBlacklist;
                for (int i = 0; i < rules.size(); i++) {
                    String rule  = rules.get(i);
                    int    chipW = font.width(rule) + 22;
                    int    xBtn  = x + chipW - 14;
                    if (mx >= xBtn && mx < xBtn + 12 && my >= cy && my < cy + ch) {
                        rules.remove(i);
                        PunchyConfig.instance.save();
                        refresh();
                        return true;
                    }
                    x += chipW + 4;
                }
            }
        }

        // Grid item clicks
        int gTop = gridTop();
        int gH   = gridH();
        if (my >= gTop && my < gTop + gH) {
            if (mx >= leftX()  && mx < leftX()  + colW()) { clickGridItem((int) mx, (int) my, leftX(),  enabledScroll,  enabledItems);  return true; }
            if (mx >= rightX() && mx < rightX() + colW()) { clickGridItem((int) mx, (int) my, rightX(), disabledScroll, disabledItems); return true; }
        }
        return false;
    }

    private void clickGridItem(int mx, int my, int colX, int scroll,
                                List<ItemFetcher.ItemInfo> items) {
        int nCols = cols();
        int col   = (mx - colX) / CELL;
        int row   = (my - gridTop() + scroll) / CELL;
        int idx   = row * nCols + col;
        if (idx < 0 || idx >= items.size()) return;

        String id = items.get(idx).id().toString();
        List<String> bl = PunchyConfig.instance.itemBlacklist;
        if (bl.contains(id)) bl.remove(id);
        else                  bl.add(id);

        PunchyConfig.instance.save();
        refresh();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
