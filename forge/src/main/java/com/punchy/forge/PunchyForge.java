package com.punchy.forge;

import com.punchy.PunchyConfig;
import com.punchy.UpdateChecker;
import com.punchy.client.PunchyConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("punchy")
public class PunchyForge {
    public PunchyForge() {
        PunchyConfig.load(FMLPaths.CONFIGDIR.get().resolve("punchy.json").toFile());
        UpdateChecker.checkForUpdates("forge");

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () ->
            new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new PunchyConfigScreen(parent)));

        MinecraftForge.EVENT_BUS.register(this);
    }
}
