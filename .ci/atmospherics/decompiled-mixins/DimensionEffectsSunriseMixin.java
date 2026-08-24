/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.DimensionSpecialEffects
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.beash.atmospherics.mixin;

import com.beash.atmospherics.util.AtmosphericEnvironmentVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={DimensionSpecialEffects.class})
public abstract class DimensionEffectsSunriseMixin {
    @Inject(method={"getSunriseColor(FF)[F"}, at={@At(value="RETURN")}, cancellable=true)
    private void atmospherics$overrideSunriseColor(float skyAngle, float tickDelta, CallbackInfoReturnable<float[]> cir) {
        int packed;
        Vec3 pos;
        ClientLevel world;
        int result;
        Minecraft client;
        float[] original = (float[])cir.getReturnValue();
        if (original != null && (client = Minecraft.getInstance()) != null && client.level != null && (result = AtmosphericEnvironmentVisuals.getSunriseSunsetColor(world = client.level, pos = client.gameRenderer.getMainCamera().getPosition(), packed = DimensionEffectsSunriseMixin.packArgb(original))) != packed) {
            original[0] = (float)(result >> 16 & 0xFF) / 255.0f;
            original[1] = (float)(result >> 8 & 0xFF) / 255.0f;
            original[2] = (float)(result & 0xFF) / 255.0f;
            original[3] = (float)(result >>> 24 & 0xFF) / 255.0f;
            cir.setReturnValue((Object)original);
        }
    }

    private static int packArgb(float[] rgba) {
        int r = Mth.clamp((int)Math.round(rgba[0] * 255.0f), (int)0, (int)255);
        int g = Mth.clamp((int)Math.round(rgba[1] * 255.0f), (int)0, (int)255);
        int b = Mth.clamp((int)Math.round(rgba[2] * 255.0f), (int)0, (int)255);
        int a = Mth.clamp((int)Math.round(rgba[3] * 255.0f), (int)0, (int)255);
        return a << 24 | r << 16 | g << 8 | b;
    }
}

