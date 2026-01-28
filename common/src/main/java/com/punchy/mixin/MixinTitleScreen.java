package com.punchy.mixin;

import com.punchy.UpdateChecker;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends net.minecraft.client.gui.screens.Screen {
    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (UpdateChecker.updateAvailable && !UpdateChecker.popupShown) {
            UpdateChecker.popupShown = true;

            this.minecraft.setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
                    }
                    this.minecraft.setScreen(this);
                },
                Component.literal("Update Available"),
                Component.literal("A new version of Punchy (" + UpdateChecker.latestVersion + ") is available.\nPlease download and install the new version."),
                Component.literal("Download"),
                Component.literal("Cancel")
            ));
        }
    }
}
