package com.punchy.fabric;

import com.punchy.PunchyConfig;
import com.punchy.UpdateChecker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class PunchyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Load Config
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        PunchyConfig.load(new File(configDir, "punchy.json"));

        // Check Updates
        UpdateChecker.checkForUpdates("fabric");
    }
}
