/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.core.BlockPos
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.beash.atmospherics.mixin;

import com.beash.atmospherics.sound.SandstormSoundController;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LevelRenderer.class})
public abstract class WeatherSoundMixin {
    @Inject(method={"tickRain"}, at={@At(value="FIELD", opcode=181, shift=At.Shift.AFTER, ordinal=1, target="Lnet/minecraft/client/renderer/LevelRenderer;rainSoundTime:I")}, require=0)
    private void atmospherics$hookWeatherSound(Camera camera, CallbackInfo ci, @Local(ordinal=0) BlockPos cameraPos, @Local(ordinal=1) BlockPos rainPos) {
        ClientLevel world = Minecraft.getInstance().level;
        SandstormSoundController.onWeatherSoundTick(world, cameraPos, rainPos);
    }
}

