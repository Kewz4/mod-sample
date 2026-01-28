package com.punchy.neoforge;

import com.punchy.PunchyConfig;
import com.punchy.UpdateChecker;
import com.punchy.client.PunchyConfigScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.ModLoadingContext;

import java.net.URI;

@Mod("punchy")
public class PunchyNeoForge {
    public PunchyNeoForge(IEventBus modEventBus) {
        PunchyConfig.load(FMLPaths.CONFIGDIR.get().resolve("punchy.json").toFile());
        UpdateChecker.checkForUpdates("neoforge");

        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (client, parent) -> new PunchyConfigScreen(parent));

        NeoForge.EVENT_BUS.register(this);
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
