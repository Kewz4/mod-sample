package com.punchy.client;

import com.punchy.ItemFetcher;
import com.punchy.PunchyConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PunchyConfigScreen extends Screen {
    private final Screen parent;
    private ItemList itemSelectionList;
    private EditBox searchBox;
    private EditBox customAddBox;
    private List<ItemFetcher.ItemInfo> allItems;

    public PunchyConfigScreen(Screen parent) {
        super(Component.literal("Punchy Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (allItems == null) {
            allItems = ItemFetcher.getAllItems();
        }

        this.itemSelectionList = new ItemList(this.minecraft, this.width, this.height, 60, 24);
        // 1.21 might use ObjectSelectionList(minecraft, width, height, top, itemHeight) ?
        // The signature varies. I'll assume (minecraft, width, height, top, itemHeight) is WRONG.
        // It is (minecraft, width, height, top, bottom, itemHeight) usually.
        // But in 1.20.4+ "top" and "bottom" might be handled differently (layout).
        // Let's use (minecraft, width, height, top, itemHeight) constructor if creating from layout?
        // No, let's assume standard (minecraft, width, height, top, bottom, itemHeight) for now.
        // If it fails, I'll fix it.
        // Wait, standard mapped is (minecraft, width, height, top, bottom, itemHeight).
        // Actually, let's look at a known class if I could.
        // I will try to use the most common one.

        this.addWidget(this.itemSelectionList);

        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, Component.literal("Search"));
        this.searchBox.setResponder((text) -> this.itemSelectionList.refresh());
        this.addRenderableWidget(this.searchBox);

        this.customAddBox = new EditBox(this.font, 20, this.height - 50, 200, 20, Component.literal("Regex/ID"));
        this.addRenderableWidget(this.customAddBox);

        this.addRenderableWidget(Button.builder(Component.literal("Add Custom"), (btn) -> {
            String text = customAddBox.getValue();
            if (!text.isEmpty()) {
                if (!PunchyConfig.instance.itemBlacklist.contains(text)) {
                    PunchyConfig.instance.itemBlacklist.add(text);
                    PunchyConfig.instance.save();
                    this.itemSelectionList.refresh();
                    customAddBox.setValue("");
                }
            }
        }).bounds(230, this.height - 50, 80, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), (btn) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height - 25, 200, 20).build());

        this.itemSelectionList.refresh();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.itemSelectionList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (mouseX >= 20 && mouseX <= 220 && mouseY >= this.height - 50 && mouseY <= this.height - 30) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Use modid:item for one item, or modid for a whole mod."));
            tooltip.add(Component.literal("Supports regex (e.g. examplemod:*_sword)."));
            guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    class ItemList extends ObjectSelectionList<ItemList.Entry> {
        public ItemList(net.minecraft.client.Minecraft minecraft, int width, int height, int top, int itemHeight) {
             // Trying to guess the constructor for 1.21.1
             // It might be (minecraft, width, height, top, itemHeight) since 1.20.3 layouts.
             super(minecraft, width, height, top, itemHeight);
        }

        public void refresh() {
            this.clearEntries();
            String filter = searchBox != null ? searchBox.getValue().toLowerCase() : "";

            for (String blocked : PunchyConfig.instance.itemBlacklist) {
                 if (blocked.toLowerCase().contains(filter) || filter.isEmpty()) {
                     this.addEntry(new BlacklistEntry(blocked));
                 }
            }

            for (ItemFetcher.ItemInfo info : allItems) {
                String id = info.id().toString();
                if (id.toLowerCase().contains(filter) || info.item().getName(new ItemStack(info.item())).getString().toLowerCase().contains(filter)) {
                    this.addEntry(new ItemEntry(info));
                }
            }
            this.setScrollAmount(0);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        abstract class Entry extends ObjectSelectionList.Entry<Entry> {}

        class BlacklistEntry extends Entry {
            private final String rule;

            BlacklistEntry(String rule) {
                this.rule = rule;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(font, rule + " [Active Rule]", left + 30, top + 5, 0xFF5555, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                PunchyConfig.instance.itemBlacklist.remove(rule);
                PunchyConfig.instance.save();
                refresh();
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(rule);
            }
        }

        class ItemEntry extends Entry {
            private final ItemFetcher.ItemInfo info;
            private final ItemStack stack;

            ItemEntry(ItemFetcher.ItemInfo info) {
                this.info = info;
                this.stack = new ItemStack(info.item());
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.renderItem(stack, left + 5, top + 2);
                guiGraphics.drawString(font, info.id().toString(), left + 30, top + 5, 0xFFFFFF, false);

                boolean isBlacklisted = PunchyConfig.instance.isBlacklisted(stack);
                int color = isBlacklisted ? 0xFF0000 : 0x00FF00;
                guiGraphics.drawString(font, isBlacklisted ? "Disabled" : "Enabled", left + 220, top + 5, color, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

            @Override
            public Component getNarration() {
                return Component.literal(info.id().toString());
            }
        }
    }
}
