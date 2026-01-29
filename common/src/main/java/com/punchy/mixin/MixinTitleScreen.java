package com.punchy.mixin;

import com.punchy.UpdateChecker;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends net.minecraft.client.gui.screens.Screen {
    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        String loader = "forge";
        if (isClassPresent("net.fabricmc.loader.api.FabricLoader")) {
            loader = "fabric";
        } else if (isClassPresent("net.neoforged.neoforge.common.NeoForge")) {
            loader = "neoforge";
        }
        UpdateChecker.checkForUpdates(loader);

        if (!UpdateChecker.popupShown) {
             UpdateChecker.popupShown = true; // Mark as "attempted to show" to prevent duplicates on resize?
             // Actually, we want it to show on every TitleScreen visit if the timer hasn't expired?
             // Or just once per session?
             // The widget handles its own visibility.
             // We just add it.

             this.addRenderableWidget(new com.punchy.client.UpdateNotificationWidget(this.width - 160 - 5, 5));
        }
    }

    private boolean isClassPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
