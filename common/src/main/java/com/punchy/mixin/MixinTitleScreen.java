package com.punchy.mixin;

import com.punchy.UpdateChecker;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
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

    /** Tracks whether we have already added the update button this session. */
    @Unique
    private boolean punchy$updateWidgetsAdded = false;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    // ── Detect loader + kick off the async check when the title screen inits ─

    @Inject(method = "init", at = @At("RETURN"))
    private void punchy$onInit(CallbackInfo ci) {
        String loader = detectLoader();
        UpdateChecker.checkForUpdates(loader);

        // If update was already known when we inited (e.g. returning from a world),
        // add the widgets immediately instead of waiting for the next tick.
        if (UpdateChecker.updateAvailable && !punchy$updateWidgetsAdded) {
            punchy$addUpdateWidgets();
        }
    }

    // ── Tick: handles the common case where the async check finishes AFTER init ─

    @Inject(method = "tick", at = @At("HEAD"))
    private void punchy$onTick(CallbackInfo ci) {
        if (UpdateChecker.updateAvailable && !punchy$updateWidgetsAdded) {
            punchy$addUpdateWidgets();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Unique
    private void punchy$addUpdateWidgets() {
        punchy$updateWidgetsAdded = true;

        // ── Persistent button (top-right corner) ─────────────────────────────
        int btnWidth  = 160;
        int btnHeight = 20;
        int x = this.width - btnWidth - 5;
        int y = 5;

        Component label = Component.literal("⬆ Update Available!")
                .withStyle(ChatFormatting.RED);

        Component tooltip = Component.literal(
                "A new version of Punchy is available: " + UpdateChecker.latestVersion
                + "\nClick to download.");

        this.addRenderableWidget(
                Button.builder(label, btn -> {
                    if (!UpdateChecker.downloadUrl.isEmpty()) {
                        Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
                    }
                })
                .bounds(x, y, btnWidth, btnHeight)
                .tooltip(Tooltip.create(tooltip))
                .build()
        );

        // ── One-time toast notification ───────────────────────────────────────
        if (!UpdateChecker.popupShown) {
            UpdateChecker.popupShown = true;
            SystemToast.add(
                    this.minecraft.getToasts(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal("Punchy – Update Available"),
                    Component.literal(
                            "Update available, please download and install\n"
                            + "the new version (" + UpdateChecker.latestVersion + ") and relaunch.")
            );
        }
    }

    @Unique
    private static String detectLoader() {
        if (isClassPresent("net.fabricmc.loader.api.FabricLoader")) return "fabric";
        if (isClassPresent("net.neoforged.neoforge.common.NeoForge"))   return "neoforge";
        return "forge";
    }

    @Unique
    private static boolean isClassPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
