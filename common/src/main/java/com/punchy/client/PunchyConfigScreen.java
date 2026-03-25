package com.punchy.client;

import com.punchy.ItemFetcher;
import com.punchy.PunchyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PunchyConfigScreen extends Screen {

    // ── Blacklist help text shown at the top of the screen ──────────────────
    private static final String HELP_LINE_1 =
            "Enter item or mod IDs to disable Punchy animations for them.";
    private static final String HELP_LINE_2 =
            "Use  modid:item  for one item,  modid  for a whole mod, or  modid:*_sword  for wildcards.";
    private static final String HELP_LINE_3 =
            "Press F3+H in-game and hover an item to see its ID.";

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int HEADER_H = 70;  // title + help text
    private static final int FOOTER_H = 36;
    private static final int ITEM_ROW = 24;

    private final Screen parent;
    private ItemList itemSelectionList;
    private EditBox  searchBox;
    private List<ItemFetcher.ItemInfo> allItems;

    public PunchyConfigScreen(Screen parent) {
        super(Component.literal("Punchy – Item Blacklist Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (allItems == null) {
            allItems = ItemFetcher.getAllItems();
        }

        // Scrollable list
        this.itemSelectionList = new ItemList(
                this.minecraft, this.width, this.height, HEADER_H, ITEM_ROW);
        this.addRenderableWidget(this.itemSelectionList);

        // Search box
        this.searchBox = new EditBox(
                this.font,
                this.width / 2 - 100, HEADER_H - 22,
                200, 18,
                Component.literal("Search items…"));
        this.searchBox.setResponder(text -> this.itemSelectionList.refresh());
        this.addRenderableWidget(this.searchBox);

        // Footer buttons
        int footerY = this.height - FOOTER_H + 8;

        // "Add Rule" button with tooltip explaining the syntax
        Component addRuleTooltip = Component.literal(
                "Add an item ID, mod ID, or wildcard/regex pattern.\n"
                + "Examples:\n"
                + "  minecraft:lantern        → one item\n"
                + "  minecraft                → entire mod\n"
                + "  minecraft:*_axe          → all axes\n"
                + "  examplemod:.*_sword      → raw regex");

        this.addRenderableWidget(
                Button.builder(Component.literal("Add Rule / Regex"), btn ->
                        this.minecraft.setScreen(new PunchyRegexInputScreen(this, rule -> {
                            if (!PunchyConfig.instance.itemBlacklist.contains(rule)) {
                                PunchyConfig.instance.itemBlacklist.add(rule);
                                PunchyConfig.instance.save();
                            }
                        }))
                )
                .bounds(12, footerY, 130, 20)
                .tooltip(Tooltip.create(addRuleTooltip))
                .build()
        );

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn ->
                        this.minecraft.setScreen(this.parent))
                .bounds(this.width - 112, footerY, 100, 20)
                .build()
        );

        this.itemSelectionList.refresh();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        // Title
        g.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        // Help text (three lines, grey, below title)
        int helpColor = 0xAAAAAA;
        g.drawCenteredString(this.font, HELP_LINE_1, this.width / 2, 18, helpColor);
        g.drawCenteredString(this.font, HELP_LINE_2, this.width / 2, 29, helpColor);
        g.drawCenteredString(this.font, HELP_LINE_3, this.width / 2, 40, helpColor);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Item list widget
    // ────────────────────────────────────────────────────────────────────────

    class ItemList extends ObjectSelectionList<ItemList.Entry> {

        ItemList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        void refresh() {
            this.clearEntries();
            String filter = searchBox != null ? searchBox.getValue().toLowerCase() : "";

            // Active blacklist rules at the top
            for (String rule : PunchyConfig.instance.itemBlacklist) {
                if (filter.isEmpty() || rule.toLowerCase().contains(filter)) {
                    this.addEntry(new BlacklistEntry(rule));
                }
            }

            // All registered items below
            if (allItems != null) {
                for (ItemFetcher.ItemInfo info : allItems) {
                    String id   = info.id().toString();
                    String name = info.item()
                            .getName(new ItemStack(info.item()))
                            .getString()
                            .toLowerCase();
                    if (filter.isEmpty()
                            || id.toLowerCase().contains(filter)
                            || name.contains(filter)) {
                        this.addEntry(new ItemEntry(info));
                    }
                }
            }
            this.setScrollAmount(0);
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, PunchyConfigScreen.this.width - 40);
        }

        // ── Entry base ───────────────────────────────────────────────────────

        abstract class Entry extends ObjectSelectionList.Entry<Entry> {}

        // ── Active blacklist rule row ─────────────────────────────────────────

        class BlacklistEntry extends Entry {
            private final String rule;

            BlacklistEntry(String rule) { this.rule = rule; }

            @Override
            public void render(GuiGraphics g, int idx, int top, int left,
                               int width, int height, int mx, int my,
                               boolean hovered, float partial) {
                // Red bullet
                g.drawString(font, "✖ " + rule, left + 4, top + 7, 0xFF5555, false);
                // Right-aligned remove hint
                String hint = "click to remove";
                int hintX = left + width - font.width(hint) - 4;
                g.drawString(font, hint, hintX, top + 7, 0x888888, false);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                if (btn == 0) {
                    PunchyConfig.instance.itemBlacklist.remove(rule);
                    PunchyConfig.instance.save();
                    refresh();
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() { return Component.literal(rule); }
        }

        // ── Item row ─────────────────────────────────────────────────────────

        class ItemEntry extends Entry {
            private final ItemFetcher.ItemInfo info;
            private final ItemStack stack;

            ItemEntry(ItemFetcher.ItemInfo info) {
                this.info  = info;
                this.stack = new ItemStack(info.item());
            }

            @Override
            public void render(GuiGraphics g, int idx, int top, int left,
                               int width, int height, int mx, int my,
                               boolean hovered, float partial) {
                // Item icon
                g.renderItem(stack, left + 2, top + 3);

                // Item ID
                g.drawString(font, info.id().toString(), left + 24, top + 7, 0xFFFFFF, false);

                // Status badge
                boolean blacklisted = PunchyConfig.instance.isBlacklisted(stack);
                String  status      = blacklisted ? "Disabled" : "Enabled";
                int     color       = blacklisted ? 0xFF5555  : 0x55FF55;
                int statusX = left + width - font.width(status) - 4;
                g.drawString(font, status, statusX, top + 7, color, false);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                if (btn == 0) {
                    String id = info.id().toString();
                    if (PunchyConfig.instance.itemBlacklist.contains(id)) {
                        PunchyConfig.instance.itemBlacklist.remove(id);
                    } else {
                        PunchyConfig.instance.itemBlacklist.add(id);
                    }
                    PunchyConfig.instance.save();
                    refresh();
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() { return Component.literal(info.id().toString()); }
        }
    }
}
