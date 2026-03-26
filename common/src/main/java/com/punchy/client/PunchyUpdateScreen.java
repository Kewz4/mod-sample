package com.punchy.client;

import com.punchy.UpdateChecker;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;

/**
 * Modal popup shown once when the title screen loads and an update is detected.
 *
 * The title screen is still visible behind a semi-transparent overlay, giving a
 * clean "dialog on top of game" feel without fully replacing the screen.
 */
public class PunchyUpdateScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 148;

    private final Screen parent;
    private final String version;
    private final String downloadUrl;

    public PunchyUpdateScreen(Screen parent, String version, String downloadUrl) {
        super(Component.literal("Punchy Update"));
        this.parent      = parent;
        this.version     = version;
        this.downloadUrl = downloadUrl;
    }

    // ── Background: dim the title screen behind the dialog ────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // Don't call super — we want the title screen visible, just dimmed
        g.fill(0, 0, this.width, this.height, 0x99000000);
    }

    // ── Widgets ───────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        int btnY = py + PANEL_H - 28;
        int btnW = (PANEL_W - 30) / 2;   // two equal buttons with gap

        // Download button (left, prominent)
        this.addRenderableWidget(
                Button.builder(Component.literal("\u2B07 Download v" + version), btn -> {
                    if (!downloadUrl.isEmpty()) {
                        Util.getPlatform().openUri(URI.create(downloadUrl));
                    }
                    this.minecraft.setScreen(parent);
                }).bounds(px + 10, btnY, btnW, 20).build());

        // Dismiss button (right)
        this.addRenderableWidget(
                Button.builder(Component.literal("Later"), btn ->
                        this.minecraft.setScreen(parent))
                        .bounds(cx + 6, btnY, btnW, 20).build());
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);

        int cx = this.width  / 2;
        int cy = this.height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        // ── Panel body ────────────────────────────────────────────────────────
        g.fill(px,             py,             px + PANEL_W, py + PANEL_H, 0xFF0E0E16);
        // Border
        g.fill(px,             py,             px + PANEL_W, py + 1,       0xFF4A4A7A);
        g.fill(px,             py + PANEL_H-1, px + PANEL_W, py + PANEL_H, 0xFF2A2A4A);
        g.fill(px,             py,             px + 1,       py + PANEL_H, 0xFF4A4A7A);
        g.fill(px + PANEL_W-1, py,             px + PANEL_W, py + PANEL_H, 0xFF4A4A7A);
        // Orange accent stripe below top border
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + 4, 0xFFFF8C00);

        // ── Title ─────────────────────────────────────────────────────────────
        g.drawCenteredString(font, "\u2B06  Update Available!",
                cx, py + 10, 0xFFFF8C00);

        // ── Version badge ─────────────────────────────────────────────────────
        String badge = "  v" + version + "  ";
        int bw = font.width(badge) + 4;
        int bx = cx - bw / 2;
        int by = py + 26;
        g.fill(bx, by - 1, bx + bw, by + font.lineHeight + 2, 0xFF1E3820);
        g.fill(bx, by - 1, bx + bw, by,                       0xFF55AA55);
        g.drawString(font, badge, bx + 2, by, 0xFF88FF88, false);

        // ── Body text ─────────────────────────────────────────────────────────
        g.drawCenteredString(font,
                "A new version of Punchy is available.",
                cx, py + 50, 0xFFCCCCCC);
        g.drawCenteredString(font,
                "Please download and install it, then",
                cx, py + 62, 0xFF999999);
        g.drawCenteredString(font,
                "relaunch Minecraft to apply the update.",
                cx, py + 72, 0xFF999999);

        // ── Small hint ────────────────────────────────────────────────────────
        g.drawCenteredString(font,
                "The \u2B06 button at the top-right of the title",
                cx, py + 90, 0xFF555577);
        g.drawCenteredString(font,
                "screen lets you download at any time.",
                cx, py + 100, 0xFF555577);

        // ── Widgets (buttons) on top ──────────────────────────────────────────
        super.render(g, mx, my, partial);
    }

    // ── Close on ESC ─────────────────────────────────────────────────────────

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void onClose() { this.minecraft.setScreen(parent); }
}
