package com.punchy.mixin;

import com.punchy.UpdateChecker;
import net.minecraft.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
        // Trigger update check if not started (safe to call multiple times as it has internal flag)
        // We attempt to detect loader via reflection for this common mixin
        String loader = "forge";
        if (isClassPresent("net.fabricmc.loader.api.FabricLoader")) {
            loader = "fabric";
        } else if (isClassPresent("net.neoforged.neoforge.common.NeoForge")) {
            loader = "neoforge";
        }
        UpdateChecker.checkForUpdates(loader);

        if (UpdateChecker.updateAvailable) {
             int btnWidth = 140;
             int btnHeight = 20;
             int x = this.width - btnWidth - 5;
             int y = 5;

             Component text = Component.literal("Update Available!").withStyle(ChatFormatting.RED);

             this.addRenderableWidget(Button.builder(text, (btn) -> {
                 if (!UpdateChecker.downloadUrl.isEmpty()) {
                    Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
                 }
             })
             .bounds(x, y, btnWidth, btnHeight)
             .tooltip(Tooltip.create(Component.literal("New version: " + UpdateChecker.latestVersion + "\nClick to download.")))
             .build());
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
