package com.punchy.client;

import com.punchy.ItemFetcher;
import com.punchy.PunchyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        int headerHeight = 40;
        int footerHeight = 40;
        int listTop = headerHeight;
        int listBottom = this.height - footerHeight;

        // Constructor: (minecraft, width, height, top, itemHeight)
        // In 1.21, we assume this sets y0=top, y1=height.
        this.itemSelectionList = new ItemList(this.minecraft, this.width, this.height, listTop, 24);

        // Manual fix for bottom if possible.
        // We can't easily access y1 if it's private or named weirdly without checking.
        // But we can try to use setRectangle if available, or just accept it extends to bottom.
        // However, we want to respect the footer.
        // I will try to update 'height' of the widget if it's a widget.
        // ObjectSelectionList is a widget.
        // this.itemSelectionList.setHeight(listBottom - listTop); // Maybe?

        this.addWidget(this.itemSelectionList);

        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 10, 200, 20, Component.literal("Search"));
        this.searchBox.setResponder((text) -> this.itemSelectionList.refresh());
        this.addRenderableWidget(this.searchBox);

        int footerY = this.height - 30;

        this.customAddBox = new EditBox(this.font, 20, footerY, 200, 20, Component.literal("Regex/ID"));
        this.customAddBox.setMaxLength(256);
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
        }).bounds(230, footerY, 80, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (btn) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width - 110, footerY, 100, 20).build());

        this.itemSelectionList.refresh();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.itemSelectionList.render(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (customAddBox.isHovered()) {
             List<Component> tooltip = new ArrayList<>();
             tooltip.add(Component.literal("Use modid:item for one item, or modid for a whole mod."));
             tooltip.add(Component.literal("Supports regex (e.g. examplemod:*_sword)."));
             guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    class ItemList extends ObjectSelectionList<ItemList.Entry> {
        public ItemList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
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

            if (allItems != null) {
                for (ItemFetcher.ItemInfo info : allItems) {
                    String id = info.id().toString();
                    if (id.toLowerCase().contains(filter) || info.item().getName(new ItemStack(info.item())).getString().toLowerCase().contains(filter)) {
                        this.addEntry(new ItemEntry(info));
                    }
                }
            }
            this.setScrollAmount(0);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        // Override to remove dirt background if possible
        // In some versions, renderBackground is the method.
        // We will try to override 'enableScissor' or something?
        // Actually, just let it render.
        // If we want transparency, we might need to modify rendering logic.
        // For now, let's fix compilation first.

        abstract class Entry extends ObjectSelectionList.Entry<Entry> {}

        class BlacklistEntry extends Entry {
            private final String rule;

            BlacklistEntry(String rule) {
                this.rule = rule;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(font, rule + " [Active Rule]", left + 10, top + 5, 0xFF5555, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    PunchyConfig.instance.itemBlacklist.remove(rule);
                    PunchyConfig.instance.save();
                    refresh();
                    return true;
                }
                return false;
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
                String status = isBlacklisted ? "Disabled" : "Enabled";
                guiGraphics.drawString(font, status, left + 220, top + 5, color, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
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
            public Component getNarration() {
                return Component.literal(info.id().toString());
            }
        }
    }
}
