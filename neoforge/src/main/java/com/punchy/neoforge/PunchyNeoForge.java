package com.punchy.neoforge;

import com.punchy.PunchyConfig;
import com.punchy.UpdateChecker;
import com.punchy.client.PunchyConfigScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModLoadingContext;

@Mod("punchy")
public class PunchyNeoForge {
    public PunchyNeoForge(IEventBus modEventBus) {
        PunchyConfig.load(FMLPaths.CONFIGDIR.get().resolve("punchy.json").toFile());
        UpdateChecker.checkForUpdates("neoforge");

        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (client, parent) -> new PunchyConfigScreen(parent));

        NeoForge.EVENT_BUS.register(this);
    }
}
