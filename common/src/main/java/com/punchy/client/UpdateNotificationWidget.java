package com.punchy.client;

import com.punchy.UpdateChecker;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.net.URI;

public class UpdateNotificationWidget extends AbstractWidget {
    // 1. Fix Forge Crash: Avoid ResourceLocation.parse/tryParse static init issues.
    // We'll use a safer approach: ResourceLocation.fromNamespaceAndPath if possible, or lazy init.
    // The crash `NoSuchMethodError: '... m_135820_(java.lang.String)'` implies `tryParse` is also problematic in the Forge env.
    // We will use `ResourceLocation.fromNamespaceAndPath("minecraft", "toast/advancement")`.
    // In 1.21.1 Mojang mappings, this is `ResourceLocation.fromNamespaceAndPath(String, String)`.

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "toast/advancement");

    private static final int TOAST_WIDTH = 160;
    private static final int TOAST_HEIGHT = 32;
    private final Button downloadBtn;
    private long firstRenderTime = -1;

    public UpdateNotificationWidget(int x, int y) {
        super(x, y, TOAST_WIDTH, TOAST_HEIGHT, Component.literal("Update Notification"));
        // 2. Fix Missing Notification: Start visible so renderWidget is called.
        this.visible = true;

        this.downloadBtn = Button.builder(Component.literal("Download"), (btn) -> {
            if (!UpdateChecker.downloadUrl.isEmpty()) {
                Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
            }
        }).bounds(x + TOAST_WIDTH - 50 - 5, y + TOAST_HEIGHT - 20 - 2, 50, 16).build();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Check update availability. If not available, do not render.
        if (!UpdateChecker.updateAvailable) {
            return;
        }

        // Start timer on first *actual* render
        if (firstRenderTime == -1) {
            this.firstRenderTime = System.currentTimeMillis();
        }

        // Hide after 15 seconds
        if (System.currentTimeMillis() - firstRenderTime > 15000) {
            this.visible = false;
            return;
        }

        // Render
        if (TEXTURE != null) {
            guiGraphics.blitSprite(TEXTURE, this.getX(), this.getY(), this.width, this.height);
        }

        guiGraphics.drawString(Minecraft.getInstance().font, "New Punchy! Update Available", this.getX() + 10, this.getY() + 5, 0xFFFFFF00, false);
        guiGraphics.drawString(Minecraft.getInstance().font, "v" + UpdateChecker.latestVersion, this.getX() + 10, this.getY() + 16, 0xFFFFFFFF, false);

        this.downloadBtn.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If not effectively visible (no update or timer expired), ignore clicks
        if (!this.visible || !UpdateChecker.updateAvailable || (firstRenderTime > 0 && System.currentTimeMillis() - firstRenderTime > 15000)) {
            return false;
        }

        if (this.downloadBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
