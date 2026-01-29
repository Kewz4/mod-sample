package com.punchy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class UpdateToast implements Toast {
    // 1.21 uses ResourceLocation.parse or similar, but for safety in "common" without knowing exact mappings:
    // We'll try to use a String identifier if possible, or guess the method.
    // Actually, let's look at what's available.
    // Standard Mojang mapping for 1.21 usually has ResourceLocation.parse(String)
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("textures/gui/sprites/toast/advancement.png");
    // Wait, 1.21 uses sprites? or textures/gui/toasts.png?
    // It moved to sprites in 1.20.2+.
    // It's likely `textures/gui/toasts.png` is gone or changed.
    // The standard toast texture is `toast/advancement`.
    // In 1.21, gui sprites are used.
    // ToastComponent.render uses `guiGraphics.blitSprite(...)`.
    // So we should use `guiGraphics.blitSprite(new ResourceLocation("toast/advancement"), ...)`

    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.parse("toast/advancement");

    private final String version;
    private long lastChanged;
    private boolean changed;

    public UpdateToast(String version) {
        this.version = version;
    }

    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        // Draw background
        guiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());

        guiGraphics.drawString(toastComponent.getMinecraft().font, Component.literal("Update Available!"), 30, 7, 0xFFFFFF00, false);
        guiGraphics.drawString(toastComponent.getMinecraft().font, Component.literal("Version " + version), 30, 18, 0xFFFFFFFF, false);

        return timeSinceLastVisible >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
