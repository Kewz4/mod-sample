/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.FogRenderer
 *  net.minecraft.client.renderer.FogRenderer$FogMode
 *  net.minecraft.util.Mth
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.level.material.FogType
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.beash.atmospherics.mixin;

import com.beash.atmospherics.Atmospherics;
import com.beash.atmospherics.util.AtmosphericEnvironmentVisuals;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={FogRenderer.class})
public abstract class BackgroundRendererMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method={"setupColor"}, at={@At(value="TAIL")})
    private static void atmospherics$overrideFogColor(Camera camera, float tickDelta, ClientLevel world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        if (camera.getFluidInCamera() == FogType.NONE) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && (player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS))) {
                RenderSystem.clearColor((float)fogRed, (float)fogGreen, (float)fogBlue, (float)0.0f);
                return;
            }
            int packed = BackgroundRendererMixin.packRgb(fogRed, fogGreen, fogBlue);
            int result = AtmosphericEnvironmentVisuals.getFogColor(world, camera.getPosition(), packed);
            if (result != packed) {
                fogRed = (float)(result >> 16 & 0xFF) / 255.0f;
                fogGreen = (float)(result >> 8 & 0xFF) / 255.0f;
                fogBlue = (float)(result & 0xFF) / 255.0f;
            }
            BackgroundRendererMixin.applyTwilightDirectionalFog(camera, world, tickDelta, viewDistance);
            RenderSystem.clearColor((float)fogRed, (float)fogGreen, (float)fogBlue, (float)0.0f);
        }
    }

    private static void applyTwilightDirectionalFog(Camera camera, ClientLevel world, float tickDelta, int viewDistance) {
        if (viewDistance < 4) {
            return;
        }
        if (!Atmospherics.getConfig().sunsetFogEnabled) {
            return;
        }
        float sign = Mth.sin((float)world.getSunAngle(tickDelta)) > 0.0f ? -1.0f : 1.0f;
        float dot = camera.getLookVector().dot((Vector3fc)new Vector3f(sign, 0.0f, 0.0f));
        if (dot <= 0.0f) {
            return;
        }
        float[] sunriseColor = world.effects().getSunriseColor(world.getTimeOfDay(tickDelta), tickDelta);
        if (sunriseColor == null) {
            return;
        }
        fogRed = fogRed * (1.0f - (dot *= sunriseColor[3])) + sunriseColor[0] * dot;
        fogGreen = fogGreen * (1.0f - dot) + sunriseColor[1] * dot;
        fogBlue = fogBlue * (1.0f - dot) + sunriseColor[2] * dot;
    }

    @Inject(method={"setupFog"}, at={@At(value="TAIL")})
    private static void atmospherics$overrideFogDistance(Camera camera, FogRenderer.FogMode fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (fogType == FogRenderer.FogMode.FOG_TERRAIN && camera.getFluidInCamera() == FogType.NONE) {
            Vec3 pos;
            float start;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && (player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS))) {
                return;
            }
            ClientLevel world = Minecraft.getInstance().level;
            if (world != null && !Float.isNaN(start = AtmosphericEnvironmentVisuals.getFogStart(world, pos = camera.getPosition(), Float.NaN))) {
                float end = AtmosphericEnvironmentVisuals.getFogEnd(world, pos, Float.NaN);
                RenderSystem.setShaderFogStart((float)start);
                RenderSystem.setShaderFogEnd((float)end);
            }
        }
    }

    private static int packRgb(float r, float g, float b) {
        int ir = Mth.clamp((int)Math.round(r * 255.0f), (int)0, (int)255);
        int ig = Mth.clamp((int)Math.round(g * 255.0f), (int)0, (int)255);
        int ib = Mth.clamp((int)Math.round(b * 255.0f), (int)0, (int)255);
        return ir << 16 | ig << 8 | ib;
    }
}

