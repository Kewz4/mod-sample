package com.punchy.fabric;

import com.punchy.client.PunchyConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class PunchyModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new PunchyConfigScreen((Screen) parent);
    }
}
