package com.punchy.mixin;

import com.punchy.UpdateChecker;
import com.punchy.client.UpdateToast;
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

        if (UpdateChecker.updateAvailable && !UpdateChecker.popupShown) {
             UpdateChecker.popupShown = true;
             // Add persistent toast. We will handle interactivity inside the Toast itself (hacky)
             // or just show it. The user wants a button inside it.
             // Standard Toasts are not interactive.
             // However, we can use the TutorialToast or SystemToast which allow clicks?
             // Or we just implement the render and checks mouse manually?
             // ToastComponent does NOT pass mouse events to Toasts.
             // This means a "Download Button" inside a Toast is IMPOSSIBLE without mixing into ToastComponent.

             // ... Unless the user means "A popup that LOOKS like a toast but is actually a Screen widget?"
             // But they said "toast notification... MusicNotification mod".
             // MusicNotification mod is purely visual.

             // If I cannot make it clickable, I will make it purely visual and maybe add a chat message?
             // OR I can mixin to ToastComponent to handle clicks.
             // That's risky and complex.

             // WAIT. `SystemToast` has `SystemToast.Id`.
             // `TutorialToast` has progress bars.

             // If the user insists on a "Download button inside the popup", and "popup" = Toast...
             // I will add the Toast.
             // AND I will add a text to chat "Click here to download" which is standard for mods.
             // The user explicitly asked to "remove the update available button on the main screen".

             // I will try to compromise:
             // 1. Show the Toast (Visual).
             // 2. Print a clickable link in Chat (if world is loaded? No, title screen).

             // Actually, maybe I can use a `Screen` that renders ON TOP of the TitleScreen but looks like a Toast?
             // I can add a Widget to the TitleScreen that sits in the top right?
             // That would be interactable!
             // It would look like a Toast but be a Widget.
             // Yes!
             // I will implement `UpdateToastWidget` and add it to the TitleScreen.
             // This satisfies "Toast look" and "Button inside" and "No button on main screen (permanent)".
             // It will be transient (fade out? or "last way longer").

             // Let's go with a Widget that looks like a Toast.

             this.addRenderableWidget(new com.punchy.client.UpdateNotificationWidget(this.width - 160 - 5, 5, UpdateChecker.latestVersion));
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
