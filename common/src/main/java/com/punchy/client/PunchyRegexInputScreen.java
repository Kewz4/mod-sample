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

    private static final int BOX_W     = 240;
    private static final int PANEL_PAD = 14;

    private final Screen          parent;
    private final Consumer<String> onAdd;
    private EditBox input;

    public PunchyRegexInputScreen(Screen parent, Consumer<String> onAdd) {
        super(Component.literal("Add Blacklist Rule"));
        this.parent = parent;
        this.onAdd  = onAdd;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // Skip super — no blur. Solid opaque dark background.
        g.fillGradient(0, 0, this.width, this.height, 0xFF0E0E18, 0xFF080810);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;

        this.input = new EditBox(this.font,
                cx - BOX_W / 2, cy - 10,
                BOX_W, 20,
                Component.literal("Rule"));
        this.input.setMaxLength(256);
        this.input.setHint(Component.literal("e.g.  modid:*_sword"));
        this.addRenderableWidget(this.input);

        int btnY = cy + 16;
        int half = BOX_W / 2 - 2;
        this.addRenderableWidget(
                Button.builder(Component.literal("Add"), btn -> {
                    String val = input.getValue().trim();
                    if (!val.isEmpty()) {
                        onAdd.accept(val);
                        this.minecraft.setScreen(parent);
                    }
                }).bounds(cx - BOX_W / 2, btnY, half, 20).build());

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn ->
                        this.minecraft.setScreen(parent))
                        .bounds(cx + 2, btnY, half, 20).build());
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);

        int cx = this.width  / 2;
        int cy = this.height / 2;

        // ── Dialog panel ──────────────────────────────────────────────────────
        int panelX = cx - BOX_W / 2 - PANEL_PAD;
        int panelY = cy - 68;
        int panelW = BOX_W + PANEL_PAD * 2;
        int panelH = 108;

        // panel bg
        g.fill(panelX,         panelY,         panelX + panelW, panelY + panelH, 0xFF111118);
        // border
        g.fill(panelX,         panelY,         panelX + panelW, panelY + 1,      0xFF444466);
        g.fill(panelX,         panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF333355);
        g.fill(panelX,         panelY,         panelX + 1,      panelY + panelH, 0xFF444466);
        g.fill(panelX + panelW - 1, panelY,   panelX + panelW, panelY + panelH, 0xFF444466);
        // top accent line
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 3, 0xFFFF8C00);

        // ── Title ─────────────────────────────────────────────────────────────
        g.drawCenteredString(font, this.title, cx, cy - 63, 0xFFFFFF);

        // ── Syntax guide ──────────────────────────────────────────────────────
        int lineY = cy - 50;
        g.drawCenteredString(font,
                "Enter item or mod IDs you want to look like normal Minecraft.",
                cx, lineY, 0xAAAAAA);
        lineY += 10;
        g.drawCenteredString(font,
                "This turns off Punchy animations for them.",
                cx, lineY, 0x888888);
        lineY += 14;
        drawSyntaxLine(g, cx, lineY,      "modid:item_id",  "→  one specific item");
        drawSyntaxLine(g, cx, lineY + 10, "modid",          "→  all items from that mod");
        drawSyntaxLine(g, cx, lineY + 20, "modid:*_sword",  "→  wildcard (glob)");
        drawSyntaxLine(g, cx, lineY + 30, "modid:.*axe",    "→  raw regex");

        // ── Widgets on top ────────────────────────────────────────────────────
        super.render(g, mouseX, mouseY, partial);
    }

    private void drawSyntaxLine(GuiGraphics g, int cx, int y, String pattern, String desc) {
        int patW = font.width(pattern);
        g.drawString(font, pattern, cx - 90,           y, 0x88FFCC44, false);
        g.drawString(font, desc,    cx - 90 + patW + 4, y, 0x666666,  false);
    }
}
