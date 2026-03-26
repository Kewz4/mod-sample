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
import net.minecraft.world.item.ItemStack;

import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Two-column item-grid config screen.
 *
 * ┌─────────────────────────────────────────────────┐
 * │  Title / help / search box                       │  HEADER_H px
 * ├─────────────────────────────────────────────────┤
 * │  [rule chip ×]  [rule chip ×]  …                │  RULES_H px
 * ├──────────────────┬──────────────────────────────┤
 * │  ✓ ENABLED (N)  │  ✗ DISABLED (M)              │  COL_HDR_H px
 * │  □ □ □ □ □ □    │  □ □ □ □ □ □                 │
 * │  □ □ □ □ □ □    │  □ □ □ □ □ □                 │  ← scrollable grid
 * │  …              │  …                            │
 * ├─────────────────────────────────────────────────┤
 * │  [Add Rule]                          [Done]      │  FOOTER_H px
 * └─────────────────────────────────────────────────┘
 *
 * Z-order (back → front):
 *   1. Background panels / fills / item renders  (renderBackground + our custom code)
 *   2. Widgets (buttons, search box)             (super.render)
 *   3. Item tooltip                              (last, above everything)
 */
public class PunchyConfigScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int CELL        = 20;   // item-cell size in pixels
    private static final int SCROLLBAR_W = 3;    // thin scroll indicator
    private static final int GAP         = 6;    // gap between the two grid columns
    private static final int PAD         = 4;    // outer horizontal padding

    // Heights – kept constant so scrollable area can be computed once
    private static final int HEADER_H    = 52;   // title + 2 help lines + search
    private static final int RULES_H     = 22;   // active-rule chip row
    private static final int COL_HDR_H   = 13;   // "ENABLED / DISABLED" labels
    private static final int FOOTER_H    = 30;   // Add-Rule + Done buttons

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private List<ItemFetcher.ItemInfo> allItems;

    /** Items NOT blacklisted (left column). */
    private final List<ItemFetcher.ItemInfo> enabledItems  = new ArrayList<>();
    /** Items that ARE blacklisted (right column). */
    private final List<ItemFetcher.ItemInfo> disabledItems = new ArrayList<>();

    private EditBox searchBox;

    /** Pixel scroll offset for each column (clamped in clampScroll). */
    private int enabledScroll  = 0;
    private int disabledScroll = 0;
    /** Horizontal scroll offset for the rules chip row. */
    private int rulesScrollX   = 0;

    /**
     * The item the mouse is currently hovering over (set during grid render,
     * consumed at the very end of render() to ensure tooltip is drawn last).
     */
    private ItemStack pendingTooltipStack = null;
    private String    pendingTooltipId    = null;

    // ── Construction ──────────────────────────────────────────────────────────

    public PunchyConfigScreen(Screen parent) {
        super(Component.literal("Punchy – Item Blacklist"));
        this.parent = parent;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        if (allItems == null) allItems = ItemFetcher.getAllItems();

        // Search box – full width minus a short label on the left
        int labelW = this.font.width("Search: ");
        int sbX    = PAD + labelW + 2;
        int sbW    = Math.min(220, this.width - sbX - PAD);
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

    // ── Refresh split lists ───────────────────────────────────────────────────

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
    }

    // ── Layout helpers (all in GUI-coordinate pixels) ─────────────────────────

    /** X of the left column. */
    private int leftX()  { return PAD; }

    /** Width of one column (both are equal). */
    private int colW()   { return (this.width - 2 * PAD - GAP) / 2; }

    /** X of the right column. */
    private int rightX() { return leftX() + colW() + GAP; }

    /** Y where the scrollable item grids begin. */
    private int gridTop() { return HEADER_H + RULES_H + COL_HDR_H; }

    /** Pixel height of the scrollable grid area. */
    private int gridH()   { return this.height - gridTop() - FOOTER_H; }

    /** How many item cells fit across one column's width (minus scrollbar). */
    private int cols()    { return Math.max(1, (colW() - SCROLLBAR_W - 1) / CELL); }

    /** Total pixel height the given number of items needs in the grid. */
    private int contentH(int count) {
        int c = cols();
        return c == 0 ? 0 : ((count + c - 1) / c) * CELL;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);

        // Reset tooltip tracking for this frame
        pendingTooltipStack = null;
        pendingTooltipId    = null;

        // ── 1. Background panels ──────────────────────────────────────────────
        drawPanels(g);

        // ── 2. Header ─────────────────────────────────────────────────────────
        drawHeader(g);

        // ── 3. Active-rules chip row ──────────────────────────────────────────
        drawRulesRow(g, mx, my);

        // ── 4. Column header labels ───────────────────────────────────────────
        drawColumnHeaders(g);

        // ── 5. Item grids (clipped with scissor) ──────────────────────────────
        drawGrid(g, mx, my, enabledItems,  leftX(),  enabledScroll,  false);
        drawGrid(g, mx, my, disabledItems, rightX(), disabledScroll, true);

        // ── 6. Widgets (search box, buttons) on top of grid content ───────────
        super.render(g, mx, my, partial);

        // ── 7. Tooltip – must be absolutely last to sit above every widget ─────
        if (pendingTooltipStack != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(pendingTooltipStack.getHoverName().getVisualOrderText());
            lines.add(Component.literal(pendingTooltipId)
                    .withStyle(ChatFormatting.GRAY).getVisualOrderText());
            g.renderTooltip(this.font, lines, mx, my);
        }
    }

    // ── Panel backgrounds ─────────────────────────────────────────────────────

    private void drawPanels(GuiGraphics g) {
        // Rules strip
        g.fill(0, HEADER_H, this.width, HEADER_H + RULES_H, 0x33000000);
        // Column-header strip
        int chY = HEADER_H + RULES_H;
        g.fill(0, chY, this.width, chY + COL_HDR_H, 0x22FFFFFF);
        // Vertical divider between the two columns
        int sepX = leftX() + colW() + GAP / 2;
        g.fill(sepX, HEADER_H, sepX + 1, this.height - FOOTER_H, 0x44FFFFFF);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private void drawHeader(GuiGraphics g) {
        int cx = this.width / 2;
        g.drawCenteredString(font, this.title, cx, 4, 0xFFFFFF);
        g.drawCenteredString(font,
                "Click items to toggle.  Use  modid:item,  modid,  or  modid:*_sword  wildcards.",
                cx, 14, 0x888888);
        g.drawCenteredString(font,
                "F3+H to find item IDs  ·  Regex supported  (e.g.  examplemod:.*_axe)",
                cx, 23, 0x666666);
        // "Search:" label aligned to the left of the search box
        g.drawString(font, "Search:", PAD, HEADER_H - 13, 0xAAAAAA, false);
    }

    // ── Active-rules chip row ─────────────────────────────────────────────────

    private void drawRulesRow(GuiGraphics g, int mx, int my) {
        int rowY = HEADER_H;
        int rowH = RULES_H;
        List<String> rules = PunchyConfig.instance.itemBlacklist;

        g.enableScissor(0, rowY, this.width, rowY + rowH);

        if (rules.isEmpty()) {
            g.drawString(font,
                    "No active rules – use 'Add Rule / Regex' to add one.",
                    PAD - rulesScrollX, rowY + (rowH - font.lineHeight) / 2,
                    0x555555, false);
        } else {
            int x = PAD - rulesScrollX;
            int cy = rowY + 2;
            int ch = rowH - 4;
            for (String rule : rules) {
                int chipW = font.width(rule) + 22;
                // chip bg
                g.fill(x, cy, x + chipW, cy + ch, 0xAA2A2A2A);
                g.fill(x, cy,     x + chipW, cy + 1,  0x66888888); // top border
                // rule text
                g.drawString(font, rule, x + 4, cy + (ch - font.lineHeight) / 2, 0xFFCC44, false);
                // × button
                int xBtnX = x + chipW - 14;
                boolean hov = mx >= xBtnX && mx < xBtnX + 12
                           && my >= cy    && my < cy + ch;
                g.drawString(font, "×", xBtnX, cy + (ch - font.lineHeight) / 2,
                        hov ? 0xFF4455 : 0x777777, false);
                x += chipW + 4;
            }
        }

        g.disableScissor();
    }

    // ── Column header labels ──────────────────────────────────────────────────

    private void drawColumnHeaders(GuiGraphics g) {
        int y = HEADER_H + RULES_H;
        g.drawCenteredString(font,
                "✓ Enabled  (" + enabledItems.size() + ")",
                leftX() + colW() / 2, y + 2, 0x55FF55);
        g.drawCenteredString(font,
                "✗ Disabled  (" + disabledItems.size() + ")",
                rightX() + colW() / 2, y + 2, 0xFF5555);
    }

    // ── Item grid ─────────────────────────────────────────────────────────────

    private void drawGrid(GuiGraphics g, int mx, int my,
                          List<ItemFetcher.ItemInfo> items,
                          int colX, int scroll, boolean isDisabled) {
        int gTop  = gridTop();
        int gH    = gridH();
        int nCols = cols();

        // Clip to this column's grid region only
        g.enableScissor(colX, gTop, colX + colW(), gTop + gH);

        if (items.isEmpty()) {
            // Empty-state message
            String msg = isDisabled ? "No disabled items" : "No enabled items";
            g.drawCenteredString(font, msg,
                    colX + colW() / 2, gTop + gH / 2 - 4, 0x444444);
        } else {
            for (int i = 0; i < items.size(); i++) {
                int col = i % nCols;
                int row = i / nCols;
                int x   = colX + col * CELL;
                int y   = gTop - scroll + row * CELL;

                // Vertical cull – skip rows fully outside the clip region
                if (y + CELL <= gTop || y >= gTop + gH) continue;

                ItemFetcher.ItemInfo info  = items.get(i);
                ItemStack            stack = new ItemStack(info.item());

                boolean hovered = mx >= x && mx < x + CELL
                               && my >= y && my < y + CELL
                               && my >= gTop && my < gTop + gH;

                // ── Cell background ───────────────────────────────────────────
                if (hovered) {
                    g.fill(x, y, x + CELL, y + CELL, 0x55FFFFFF);
                    pendingTooltipStack = stack;
                    pendingTooltipId    = info.id().toString();
                } else if (isDisabled) {
                    g.fill(x, y, x + CELL, y + CELL, 0x22FF2222);
                }

                // ── Item icon ─────────────────────────────────────────────────
                g.renderItem(stack, x + 2, y + 2);

                // ── Dark overlay on disabled items so they look greyed out ─────
                if (isDisabled && !hovered) {
                    g.fill(x + 2, y + 2, x + CELL - 2, y + CELL - 2, 0x55000000);
                }
            }
        }

        g.disableScissor();

        // ── Scrollbar (drawn outside scissor so it's always visible) ─────────
        drawScrollbar(g, colX + colW() - SCROLLBAR_W, gTop, gH,
                scroll, contentH(items.size()));
    }

    private void drawScrollbar(GuiGraphics g, int x, int y, int h,
                                int scroll, int totalH) {
        if (totalH <= h) return;
        // Track
        g.fill(x, y, x + SCROLLBAR_W, y + h, 0x22FFFFFF);
        // Thumb
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
            if (mx >= leftX() && mx < leftX() + colW()) {
                enabledScroll  = clampScroll(enabledScroll  + delta, enabledItems.size(),  gH);
            } else if (mx >= rightX() && mx < rightX() + colW()) {
                disabledScroll = clampScroll(disabledScroll + delta, disabledItems.size(), gH);
            }
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

        // ── Rules row – click × to remove ────────────────────────────────────
        if (my >= HEADER_H && my < HEADER_H + RULES_H) {
            int x = PAD - rulesScrollX;
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

        // ── Grid clicks ───────────────────────────────────────────────────────
        int gTop = gridTop();
        int gH   = gridH();
        if (my >= gTop && my < gTop + gH) {
            if (mx >= leftX()  && mx < leftX()  + colW()) {
                clickGridItem((int) mx, (int) my, leftX(),  enabledScroll,  enabledItems);
                return true;
            }
            if (mx >= rightX() && mx < rightX() + colW()) {
                clickGridItem((int) mx, (int) my, rightX(), disabledScroll, disabledItems);
                return true;
            }
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
