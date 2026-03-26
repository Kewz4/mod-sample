package com.punchy.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * A polished update-available toast inspired by MusicNotification's MusicToast.
 *
 * Uses the vanilla {@code toast/system} nine-slice sprite as the background so
 * it blends naturally with Minecraft's UI, then draws a coloured accent bar,
 * title, subtitle, and a progress bar that drains over the display period.
 *
 * Only one instance is shown at a time (shared {@link Token}).
 */
public class PunchyUpdateToast implements Toast {

    private static final Token TOKEN       = new Token() {};
    private static final long  DISPLAY_MS  = 8_000L;  // 8 s display time

    // Vanilla nine-slice toast background
    private static final ResourceLocation TOAST_BG =
            ResourceLocation.withDefaultNamespace("toast/system");

    private static final int W = 160;
    private static final int H = 32;

    // Punchy accent colour (warm orange)
    private static final int COL_ACCENT   = 0xFFFF8C00;
    private static final int COL_TITLE    = 0xFFFFFFFF;
    private static final int COL_SUBTITLE = 0xFFAAAAAA;
    private static final int COL_PROG_BAR = 0xCCFF8C00;

    private final String version;

    public PunchyUpdateToast(String version) {
        this.version = version;
    }

    @Override public int    width()    { return W; }
    @Override public int    height()   { return H; }
    @Override public Token  getToken() { return TOKEN; }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent tc, long elapsed) {
        Font font = tc.getMinecraft().font;

        // ── 1. Vanilla nine-slice background ──────────────────────────────────
        g.blitSprite(TOAST_BG, 0, 0, W, H);

        // ── 2. Left accent bar ────────────────────────────────────────────────
        g.fill(2, 2, 4, H - 2, COL_ACCENT);

        // ── 3. Progress bar (drains left→right as time passes) ────────────────
        float progress = Math.min(1f, (float) elapsed / DISPLAY_MS);
        int   barW     = (int)((W - 6) * (1f - progress));
        if (barW > 0) {
            g.fill(6, H - 3, 6 + barW, H - 1, COL_PROG_BAR);
        }

        // ── 4. Title ──────────────────────────────────────────────────────────
        g.drawString(font, "\u2B06 Punchy Update Available", 8, 7, COL_TITLE, false);

        // ── 5. Subtitle ───────────────────────────────────────────────────────
        String sub = "v" + version + "  \u2014  click button on title screen";
        // Trim if too wide for the toast
        while (font.width(sub) > W - 10 && sub.length() > 10) {
            sub = sub.substring(0, sub.length() - 1);
        }
        g.drawString(font, sub, 8, 18, COL_SUBTITLE, false);

        return elapsed >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
    }
}
