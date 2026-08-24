package com.beash.atmospherics.mixin;

import com.beash.atmospherics.screen.FogConfigScreen;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Atmospherics 2.6.5 ships its cloud mode and SMC button with no-op callbacks.
 * Reconnect both controls to the already-present cloud editor implementations.
 */
@Mixin(FogConfigScreen.class)
public abstract class FogConfigScreenCloudSupportMixin extends Screen {
    @Unique
    private static final Logger ATMOSPHERICS_CLOUD_LOGGER =
            LoggerFactory.getLogger("Atmospherics/FabricCloudCompat");

    @Shadow
    private Button cloudsModeButton;

    @Shadow
    private Button storyCloudSettingsButton;

    protected FogConfigScreenCloudSupportMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void atmospherics$restoreCloudButtons(CallbackInfo ci) {
        if (this.cloudsModeButton != null) {
            Button oldButton = this.cloudsModeButton;
            Button replacement = Button.builder(
                            oldButton.getMessage(),
                            button -> this.atmospherics$openCloudMode())
                    .bounds(oldButton.getX(), oldButton.getY(), oldButton.getWidth(), oldButton.getHeight())
                    .build();
            replacement.active = oldButton.active;
            replacement.visible = oldButton.visible;
            this.removeWidget(oldButton);
            this.cloudsModeButton = this.addRenderableWidget(replacement);
        }

        if (this.storyCloudSettingsButton != null) {
            Button oldButton = this.storyCloudSettingsButton;
            Button replacement = Button.builder(
                            oldButton.getMessage(),
                            button -> this.atmospherics$openStoryCloudSettings())
                    .bounds(oldButton.getX(), oldButton.getY(), oldButton.getWidth(), oldButton.getHeight())
                    .build();
            replacement.active = oldButton.active;
            replacement.visible = oldButton.visible;
            this.removeWidget(oldButton);
            this.storyCloudSettingsButton = this.addRenderableWidget(replacement);
        }
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void atmospherics$openCloudMode() {
        Object screen = this;
        try {
            Class<?> screenClass = FogConfigScreen.class;
            Field editModeField = screenClass.getDeclaredField("editMode");
            editModeField.setAccessible(true);

            Class<? extends Enum> editModeClass =
                    (Class<? extends Enum>) Class.forName(screenClass.getName() + "$EditMode");
            Enum<?> cloudsMode = Enum.valueOf(editModeClass, "CLOUDS");
            Object currentMode = editModeField.get(screen);

            if (currentMode != cloudsMode) {
                Method saveCurrentBiome =
                        screenClass.getDeclaredMethod("saveCurrentBiome", boolean.class, boolean.class);
                saveCurrentBiome.setAccessible(true);
                saveCurrentBiome.invoke(screen, false, true);
                editModeField.set(screen, cloudsMode);
            }

            Minecraft.getInstance().setScreen((Screen) screen);
        } catch (ReflectiveOperationException error) {
            ATMOSPHERICS_CLOUD_LOGGER.error("Could not open Atmospherics cloud mode", error);
        }
    }

    @Unique
    private void atmospherics$openStoryCloudSettings() {
        Object screen = this;
        try {
            Class<?> settingsClass = Class.forName(
                    FogConfigScreen.class.getName() + "$StoryCloudSettingsScreen");
            Constructor<?> constructor = settingsClass.getDeclaredConstructor(
                    FogConfigScreen.class,
                    Screen.class);
            constructor.setAccessible(true);
            Screen settingsScreen = (Screen) constructor.newInstance(screen, screen);
            Minecraft.getInstance().setScreen(settingsScreen);
        } catch (ReflectiveOperationException error) {
            ATMOSPHERICS_CLOUD_LOGGER.error("Could not open Atmospherics Story Mode Clouds settings", error);
        }
    }
}
