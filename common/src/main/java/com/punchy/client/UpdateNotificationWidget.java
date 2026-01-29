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
    // Replaced ResourceLocation.parse with ResourceLocation.tryParse for better compatibility/safety
    // If tryParse is missing (it shouldn't be in 1.21), we can fallback to constructor if available.
    // However, the crash was NoSuchMethodError for 'm_338530_' which is 'parse'.
    // 'tryParse' is usually 'm_135820_' or similar.
    // Let's use ResourceLocation.fromNamespaceAndPath if available, or just new ResourceLocation(ns, path) is deprecated/removed?
    // In 1.21, `new ResourceLocation` is gone.
    // `ResourceLocation.withDefaultNamespace` is common.
    // Let's stick to `ResourceLocation.parse` but ensure we are compiling against the right mapping that Forge expects?
    // Actually, Common uses official Mojang mappings. Forge runtime uses SRG remapped to Official.
    // If `parse` isn't found, it might be that Forge is remapping it to something else or the runtime jar is weird.
    // But `tryParse` is safer.

    // BUT, wait. `toast/advancement` is not a valid namespace:path. It's missing the namespace!
    // It should be `minecraft:toast/advancement`.
    // `parse("toast/advancement")` assumes namespace `minecraft` in some contexts or fails?
    // `ResourceLocation.parse` handles default namespace? Yes.

    // The safest way is `ResourceLocation.fromNamespaceAndPath("minecraft", "toast/advancement")` if it exists.
    // Or just `ResourceLocation.tryParse("minecraft:toast/advancement")`.

    private static final ResourceLocation TEXTURE = ResourceLocation.tryParse("minecraft:toast/advancement");
    private static final int TOAST_WIDTH = 160;
    private static final int TOAST_HEIGHT = 32;
    private final Button downloadBtn;
    private long firstRenderTime = -1;

    public UpdateNotificationWidget(int x, int y) {
        super(x, y, TOAST_WIDTH, TOAST_HEIGHT, Component.literal("Update Notification"));
        this.visible = false; // Start hidden

        this.downloadBtn = Button.builder(Component.literal("Download"), (btn) -> {
            if (!UpdateChecker.downloadUrl.isEmpty()) {
                Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
            }
        }).bounds(x + TOAST_WIDTH - 50 - 5, y + TOAST_HEIGHT - 20 - 2, 50, 16).build();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!UpdateChecker.updateAvailable) {
            this.visible = false;
            return;
        }

        // Activate if not already
        if (!this.visible) {
            this.visible = true;
            this.firstRenderTime = System.currentTimeMillis();
        }

        // Timer check
        if (firstRenderTime > 0 && System.currentTimeMillis() - firstRenderTime > 15000) {
            this.visible = false;
            return;
        }

        // Render Background
        if (TEXTURE != null) {
            guiGraphics.blitSprite(TEXTURE, this.getX(), this.getY(), this.width, this.height);
        }

        guiGraphics.drawString(Minecraft.getInstance().font, "New Punchy! Update Available", this.getX() + 10, this.getY() + 5, 0xFFFFFF00, false);
        guiGraphics.drawString(Minecraft.getInstance().font, "v" + UpdateChecker.latestVersion, this.getX() + 10, this.getY() + 16, 0xFFFFFFFF, false);

        this.downloadBtn.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;
        if (this.downloadBtn.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
