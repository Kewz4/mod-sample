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

    private static final int PANEL_W   = 280;
    private static final int PANEL_H   = 152;
    private static final int PANEL_PAD = 12;

    private final Screen           parent;
    private final Consumer<String> onAdd;
    private EditBox input;

    public PunchyRegexInputScreen(Screen parent, Consumer<String> onAdd) {
        super(Component.literal("Add Blacklist Rule"));
        this.parent = parent;
        this.onAdd  = onAdd;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private int panelX() { return this.width  / 2 - PANEL_W / 2; }
    private int panelY() { return this.height / 2 - PANEL_H / 2; }

    @Override
    protected void init() {
        int px = panelX();
        int py = panelY();
        int inputW = PANEL_W - PANEL_PAD * 2;

        // Input box: 98px below panel top
        this.input = new EditBox(this.font,
                px + PANEL_PAD, py + 98, inputW, 16,
                Component.literal("Rule"));
        this.input.setMaxLength(256);
        this.input.setHint(Component.literal("e.g.  cobblemon  or  modid:*_sword"));
        this.addRenderableWidget(this.input);

        // Buttons: 120px below panel top
        int half = inputW / 2 - 2;
        int btnY = py + 122;
        this.addRenderableWidget(
                Button.builder(Component.literal("Add"), btn -> {
                    String val = input.getValue().trim();
                    if (!val.isEmpty()) {
                        onAdd.accept(val);
                        this.minecraft.setScreen(parent);
                    }
                }).bounds(px + PANEL_PAD, btnY, half, 20).build());

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn ->
                        this.minecraft.setScreen(parent))
                        .bounds(px + PANEL_PAD + half + 4, btnY, half, 20).build());
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);

        int px = panelX();
        int py = panelY();
        int cx = this.width / 2;

        // Panel background + border
        g.fill(px,           py,           px + PANEL_W, py + PANEL_H, 0xF0111118);
        g.fill(px,           py,           px + PANEL_W, py + 1,       0xFF444466);
        g.fill(px,           py + PANEL_H - 1, px + PANEL_W, py + PANEL_H, 0xFF333355);
        g.fill(px,           py,           px + 1,       py + PANEL_H, 0xFF444466);
        g.fill(px + PANEL_W - 1, py,       px + PANEL_W, py + PANEL_H, 0xFF444466);
        // Orange accent stripe
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + 3, 0xFFFF8C00);

        // Title
        g.drawCenteredString(font, this.title, cx, py + 8, 0xFFFFFF);

        // Description
        g.drawCenteredString(font,
                "Enter items or mods to disable Punchy animations for.",
                cx, py + 24, 0xAAAAAA);

        // Syntax reference
        int lineY = py + 42;
        int labelX = cx - 100;
        int descX  = cx - 14;
        drawSyntaxLine(g, labelX, descX, lineY,      "modid:item_id",  "exact item");
        drawSyntaxLine(g, labelX, descX, lineY + 12, "modid",          "whole mod");
        drawSyntaxLine(g, labelX, descX, lineY + 24, "modid:*_sword",  "glob wildcard");
        drawSyntaxLine(g, labelX, descX, lineY + 36, "*_leggings",     "any mod, path glob");
        drawSyntaxLine(g, labelX, descX, lineY + 48, "modid:.*axe",    "raw regex");

        // Divider above input
        g.fill(px + PANEL_PAD, py + 92, px + PANEL_W - PANEL_PAD, py + 93, 0x44FFFFFF);

        super.render(g, mouseX, mouseY, partial);
    }

    private void drawSyntaxLine(GuiGraphics g, int labelX, int descX, int y,
                                String pattern, String desc) {
        g.drawString(font, pattern, labelX,        y, 0xFFCC44, false);
        g.drawString(font, "→",     descX - 10,    y, 0x555555, false);
        g.drawString(font, desc,    descX + 2,     y, 0x888888, false);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
