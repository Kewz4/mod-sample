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

    private static final int BOX_W = 240;

    private final Screen          parent;
    private final Consumer<String> onAdd;
    private EditBox input;

    public PunchyRegexInputScreen(Screen parent, Consumer<String> onAdd) {
        super(Component.literal("Add Blacklist Rule"));
        this.parent = parent;
        this.onAdd  = onAdd;
    }

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;

        this.input = new EditBox(
                this.font,
                cx - BOX_W / 2, cy - 12,
                BOX_W, 20,
                Component.literal("Rule"));
        this.input.setMaxLength(256);
        this.input.setHint(Component.literal("e.g.  modid:*_sword"));
        this.addRenderableWidget(this.input);

        this.addRenderableWidget(
                Button.builder(Component.literal("Add"), btn -> {
                    String val = input.getValue().trim();
                    if (!val.isEmpty()) {
                        onAdd.accept(val);
                        this.minecraft.setScreen(parent);
                    }
                })
                .bounds(cx - BOX_W / 2, cy + 14, (BOX_W / 2) - 2, 20)
                .build()
        );

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn ->
                        this.minecraft.setScreen(parent))
                .bounds(cx + 2, cy + 14, (BOX_W / 2) - 2, 20)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;

        g.drawCenteredString(this.font, this.title, cx, cy - 46, 0xFFFFFF);

        // ── Syntax guide ──────────────────────────────────────────────────────
        int lineColor = 0xAAAAAA;
        int y = cy - 34;
        g.drawCenteredString(this.font,
                "Enter the IDs of items or mods you want to look like normal Minecraft.",
                cx, y, lineColor);
        y += 10;
        g.drawCenteredString(this.font,
                "This turns off Punchy animations for them.",
                cx, y, lineColor);
        y += 14;
        g.drawCenteredString(this.font,
                "modid:item_id   →  one specific item",
                cx, y, 0x888888);
        y += 10;
        g.drawCenteredString(this.font,
                "modid           →  all items from that mod",
                cx, y, 0x888888);
        y += 10;
        g.drawCenteredString(this.font,
                "modid:*_sword   →  wildcard (all swords in that mod)",
                cx, y, 0x888888);
        y += 10;
        g.drawCenteredString(this.font,
                "modid:.*axe     →  raw regex",
                cx, y, 0x888888);

        super.render(g, mouseX, mouseY, partialTick);
    }
}
