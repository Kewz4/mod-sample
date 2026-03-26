package com.punchy.client;

import com.punchy.PunchyConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Shows all active blacklist rules with per-rule remove buttons,
 * a "Clear All" action, and a shortcut to add a new rule.
 */
public class PunchyRulesScreen extends Screen {

    private static final int PAD      = 8;
    private static final int ROW_H    = 18;
    private static final int FOOTER_H = 30;
    private static final int HEADER_H = 28;

    private final Screen parent;
    private int scrollY = 0;

    public PunchyRulesScreen(Screen parent) {
        super(Component.literal("Blacklist Rules"));
        this.parent = parent;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
    }

    @Override
    protected void init() {
        int footY = this.height - FOOTER_H + (FOOTER_H - 20) / 2;

        this.addRenderableWidget(
                Button.builder(Component.literal("Add Rule / Regex"), btn ->
                        this.minecraft.setScreen(new PunchyRegexInputScreen(this, rule -> {
                            if (!PunchyConfig.instance.itemBlacklist.contains(rule)) {
                                PunchyConfig.instance.itemBlacklist.add(rule);
                                PunchyConfig.instance.save();
                            }
                        })))
                        .bounds(PAD, footY, 118, 20)
                        .build());

        this.addRenderableWidget(
                Button.builder(Component.literal("Clear All"), btn -> {
                    PunchyConfig.instance.itemBlacklist.clear();
                    PunchyConfig.instance.save();
                    scrollY = 0;
                })
                        .bounds(PAD + 122, footY, 72, 20)
                        .build());

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_BACK, btn ->
                        this.minecraft.setScreen(this.parent))
                        .bounds(this.width - 104, footY, 100, 20)
                        .build());
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);

        g.drawCenteredString(font, this.title, this.width / 2, 8, 0xFFFFFF);

        List<String> rules = PunchyConfig.instance.itemBlacklist;
        int listTop  = HEADER_H;
        int listH    = this.height - HEADER_H - FOOTER_H;
        int panelW   = Math.min(500, this.width - 2 * PAD);
        int panelX   = (this.width - panelW) / 2;

        // Panel background
        g.fill(panelX, listTop, panelX + panelW, listTop + listH, 0x44000000);

        g.enableScissor(panelX, listTop, panelX + panelW, listTop + listH);

        if (rules.isEmpty()) {
            g.drawCenteredString(font, "No rules yet. Use 'Add Rule / Regex' to add one.",
                    this.width / 2, listTop + listH / 2 - font.lineHeight / 2, 0x666666);
        } else {
            for (int i = 0; i < rules.size(); i++) {
                int rowY = listTop + i * ROW_H - scrollY;
                if (rowY + ROW_H <= listTop || rowY >= listTop + listH) continue;

                String rule = rules.get(i);

                // Row hover highlight
                boolean hovered = mx >= panelX && mx < panelX + panelW
                               && my >= rowY   && my < rowY + ROW_H;
                if (hovered) g.fill(panelX, rowY, panelX + panelW, rowY + ROW_H, 0x22FFFFFF);

                // Alternating row tint
                if (i % 2 == 0) g.fill(panelX, rowY, panelX + panelW, rowY + ROW_H, 0x11FFFFFF);

                // Rule text
                g.drawString(font, rule, panelX + PAD, rowY + (ROW_H - font.lineHeight) / 2,
                        0xFFCC44, false);

                // × remove button
                int rmX = panelX + panelW - 20;
                boolean rmHov = mx >= rmX && mx < rmX + 16 && my >= rowY && my < rowY + ROW_H;
                g.drawString(font, "×", rmX + 3, rowY + (ROW_H - font.lineHeight) / 2,
                        rmHov ? 0xFF6666 : 0x886666, false);
            }
        }

        g.disableScissor();

        // Scrollbar
        int totalH = rules.size() * ROW_H;
        if (totalH > listH) {
            int sbX     = panelX + panelW - 3;
            float ratio = (float) listH / totalH;
            int   thumbH = Math.max(8, (int)(listH * ratio));
            int   maxS   = totalH - listH;
            int   thumbY = listTop + (maxS == 0 ? 0 : (int)((listH - thumbH) * ((float) scrollY / maxS)));
            g.fill(sbX, listTop, sbX + 3, listTop + listH, 0x22FFFFFF);
            g.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, 0x99FFFFFF);
        }

        super.render(g, mx, my, partial);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mx, double my, double dX, double dY) {
        if (super.mouseScrolled(mx, my, dX, dY)) return true;
        List<String> rules = PunchyConfig.instance.itemBlacklist;
        int listH = this.height - HEADER_H - FOOTER_H;
        int totalH = rules.size() * ROW_H;
        if (totalH > listH) {
            scrollY = Math.max(0, Math.min(scrollY - (int)(dY * ROW_H), totalH - listH));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        if (btn != 0) return false;

        List<String> rules = PunchyConfig.instance.itemBlacklist;
        int listTop = HEADER_H;
        int listH   = this.height - HEADER_H - FOOTER_H;
        int panelW  = Math.min(500, this.width - 2 * PAD);
        int panelX  = (this.width - panelW) / 2;

        if (my >= listTop && my < listTop + listH) {
            int rmX = panelX + panelW - 20;
            if (mx >= rmX && mx < rmX + 16) {
                int i = ((int) my - listTop + scrollY) / ROW_H;
                if (i >= 0 && i < rules.size()) {
                    rules.remove(i);
                    PunchyConfig.instance.save();
                    scrollY = Math.max(0, Math.min(scrollY, rules.size() * ROW_H - listH));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
