package com.punchy.mixin;

import com.punchy.UpdateChecker;
import com.punchy.client.PunchyUpdateScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends net.minecraft.client.gui.screens.Screen {

    protected MixinTitleScreen(Component title) { super(title); }

    // ── Kick off async check + add button when already known ──────────────────

    @Inject(method = "init", at = @At("RETURN"))
    private void punchy$onInit(CallbackInfo ci) {
        // Always reset so the button is re-added after returning from popup / world
        punchy$buttonAdded = false;

        UpdateChecker.checkForUpdates(detectLoader());

        if (UpdateChecker.updateAvailable) {
            punchy$addUpdateButton();
        }
    }

    // ── Tick: handles async check finishing after init ─────────────────────────

    @Inject(method = "tick", at = @At("HEAD"))
    private void punchy$onTick(CallbackInfo ci) {
        if (UpdateChecker.updateAvailable && !punchy$buttonAdded) {
            punchy$addUpdateButton();
        }

        // Show popup once – scheduled here so MC is fully ready
        if (UpdateChecker.updateAvailable && !UpdateChecker.popupShown) {
            UpdateChecker.popupShown = true;
            this.minecraft.setScreen(new PunchyUpdateScreen(
                    (TitleScreen)(Object) this,
                    UpdateChecker.latestVersion,
                    UpdateChecker.downloadUrl));
        }
    }

    // ── Persistent "Update Available" button (top-right, always visible) ──────

    @Unique private boolean punchy$buttonAdded = false;

    @Unique
    private void punchy$addUpdateButton() {
        punchy$buttonAdded = true;

        int btnW = 160, btnH = 20;
        int x = this.width - btnW - 5;

        Component label = Component.literal("\u2B06 Update Available!")
                .withStyle(ChatFormatting.RED);

        Component tip = Component.literal(
                "Punchy " + UpdateChecker.latestVersion + " is available.\nClick to open the download page.");

        this.addRenderableWidget(
                Button.builder(label, btn -> {
                    if (!UpdateChecker.downloadUrl.isEmpty()) {
                        Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
                    }
                })
                .bounds(x, 5, btnW, btnH)
                .tooltip(Tooltip.create(tip))
                .build());
    }

    // ── Loader detection ──────────────────────────────────────────────────────

    @Unique
    private static String detectLoader() {
        if (isClassPresent("net.fabricmc.loader.api.FabricLoader")) return "fabric";
        if (isClassPresent("net.neoforged.neoforge.common.NeoForge"))   return "neoforge";
        return "forge";
    }

    @Unique
    private static boolean isClassPresent(String name) {
        try { Class.forName(name); return true; }
        catch (ClassNotFoundException e) { return false; }
    }
}
