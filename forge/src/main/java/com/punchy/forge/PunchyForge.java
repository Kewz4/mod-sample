package com.punchy.forge;

import com.punchy.PunchyConfig;
import com.punchy.UpdateChecker;
import com.punchy.client.PunchyConfigScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.net.URI;

@Mod("punchy")
public class PunchyForge {
    public PunchyForge() {
        PunchyConfig.load(FMLPaths.CONFIGDIR.get().resolve("punchy.json").toFile());
        UpdateChecker.checkForUpdates("forge");

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () ->
            new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new PunchyConfigScreen(parent)));

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen && UpdateChecker.updateAvailable && !UpdateChecker.popupShown) {
            UpdateChecker.popupShown = true;
            event.getScreen().getMinecraft().setScreen(new ConfirmScreen(
                (confirmed) -> {
                    if (confirmed) {
                        Util.getPlatform().openUri(URI.create(UpdateChecker.downloadUrl));
                    }
                    event.getScreen().getMinecraft().setScreen(event.getScreen());
                },
                Component.literal("Update Available"),
                Component.literal("A new version of Punchy (" + UpdateChecker.latestVersion + ") is available.\nPlease download and install the new version."),
                Component.literal("Download"),
                Component.literal("Cancel")
            ));
        }
    }
}
