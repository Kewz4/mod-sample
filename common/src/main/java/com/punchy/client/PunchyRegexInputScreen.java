package com.punchy.client;

import com.punchy.PunchyConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class PunchyRegexInputScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onAdd;
    private EditBox input;

    public PunchyRegexInputScreen(Screen parent, Consumer<String> onAdd) {
        super(Component.literal("Add Regex Rule"));
        this.parent = parent;
        this.onAdd = onAdd;
    }

    @Override
    protected void init() {
        this.input = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Component.literal("Regex"));
        this.input.setMaxLength(256);
        this.addRenderableWidget(this.input);

        this.addRenderableWidget(Button.builder(Component.literal("Add"), (btn) -> {
            String val = input.getValue();
            if (!val.isEmpty()) {
                onAdd.accept(val);
                this.minecraft.setScreen(parent);
            }
        }).bounds(this.width / 2 - 105, this.height / 2 + 10, 100, 20).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (btn) -> {
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 + 5, this.height / 2 + 10, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Enter item ID or Regex (e.g. modid:*_sword)", this.width / 2, this.height / 2 - 60, 0xAAAAAA);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
