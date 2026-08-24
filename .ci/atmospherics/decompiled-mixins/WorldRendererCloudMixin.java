/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.DimensionSpecialEffects
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Constant
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.ModifyConstant
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.beash.atmospherics.mixin;

import com.beash.atmospherics.Atmospherics;
import com.beash.atmospherics.config.CloudsSettings;
import com.beash.atmospherics.config.FogConfig;
import com.beash.atmospherics.config.WeatherSettings;
import com.beash.atmospherics.util.AtmosphericEnvironmentVisuals;
import com.beash.atmospherics.util.BiomeBlendUtil;
import com.beash.atmospherics.util.DayCycleWindowUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LevelRenderer.class})
public abstract class WorldRendererCloudMixin {
    @Unique
    private static boolean ambientfog$hasSmooth;
    @Unique
    private static float ambientfog$r;
    @Unique
    private static float ambientfog$g;
    @Unique
    private static float ambientfog$b;
    @Unique
    private static long ambientfog$lastNanos;
    @Unique
    private static boolean ambientfog$hasFogTintSmooth;
    @Unique
    private static float ambientfog$fogTintR;
    @Unique
    private static float ambientfog$fogTintG;
    @Unique
    private static float ambientfog$fogTintB;
    @Unique
    private static long ambientfog$fogTintLastNanos;
    @Unique
    private static float ambientfog$lastAppliedCloudOpacity;
    @Shadow
    private boolean generateClouds;

    @Inject(method={"renderClouds"}, at={@At(value="HEAD")})
    private void atmospherics$invalidateCloudBuffer(PoseStack matrices, Matrix4f matrix1, Matrix4f matrix2, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        float opacity;
        CloudsSettings clouds = WorldRendererCloudMixin.ambientfog$getActiveStoryCloudSettings();
        float f = opacity = clouds == null ? 1.0f : Mth.clamp((float)clouds.storeModeCloudOpacity, (float)0.0f, (float)1.0f);
        if (Float.isNaN(ambientfog$lastAppliedCloudOpacity) || Math.abs(opacity - ambientfog$lastAppliedCloudOpacity) > 1.0E-4f) {
            this.generateClouds = true;
            ambientfog$lastAppliedCloudOpacity = opacity;
        }
    }

    @Redirect(method={"renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;getCloudColor(F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 atmospherics$modifyCloudColor(ClientLevel world, float tickDelta) {
        Vec3 vanilla = world.getCloudColor(tickDelta);
        FogConfig cfg = Atmospherics.getConfig();
        CloudsSettings clouds = WorldRendererCloudMixin.ambientfog$getEnabledCloudSettings();
        if (cfg != null && clouds != null && cfg.biomes != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                String biomeId = mc.level.getBiome(BlockPos.containing((Position)playerPos)).unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
                List<BiomeBlendUtil.WeightedBiomeSample> samples = BiomeBlendUtil.collect(cfg, mc.level, playerPos);
                if (samples.isEmpty()) {
                    ambientfog$hasSmooth = false;
                    ambientfog$hasFogTintSmooth = false;
                    return vanilla;
                }
                int origRgb = WorldRendererCloudMixin.ambientfog$packVec3(vanilla);
                long worldTime = mc.level.getDayTime();
                float weatherDark = WorldRendererCloudMixin.ambientfog$getWeatherDarken();
                float rain = Mth.clamp((float)mc.level.getRainLevel(1.0f), (float)0.0f, (float)1.0f);
                float thunder = Mth.clamp((float)mc.level.getThunderLevel(1.0f), (float)0.0f, (float)1.0f);
                int rainCloudColor = WorldRendererCloudMixin.ambientfog$getRainCloudColor();
                int thunderCloudColor = WorldRendererCloudMixin.ambientfog$getThunderCloudColor();
                float weatherNightDarkening = WorldRendererCloudMixin.ambientfog$getWeatherCloudNightDarkening();
                int blendedCloudColor = BiomeBlendUtil.blendRgb(samples, s -> s.cloudColor, 0xFFFFFF);
                int blendedFogColor = BiomeBlendUtil.blendRgb(samples, s -> s.fogColor, 9881850);
                int blendedNightFogColor = BiomeBlendUtil.blendRgb(samples, s -> s.nightFogColor, 3820902);
                int blendedNightCloudColor = BiomeBlendUtil.blendRgb(samples, s -> s.nightCloudColor, 526361);
                float twilightCloudBlend = WorldRendererCloudMixin.ambientfog$getTwilightCloudTimeFactor(cfg, worldTime);
                float[] fogTint = clouds.storeModeCloud ? WorldRendererCloudMixin.ambientfog$getSmoothedFogTint(cfg, biomeId, blendedFogColor, blendedNightFogColor, worldTime) : WorldRendererCloudMixin.ambientfog$getImmediateFogTint(blendedFogColor, blendedNightFogColor, worldTime);
                float[] target = WorldRendererCloudMixin.ambientfog$resolveTarget(clouds, blendedCloudColor, fogTint, worldTime, origRgb, weatherDark, rain, thunder, rainCloudColor, thunderCloudColor, weatherNightDarkening, twilightCloudBlend, cfg.twilightCloudColor, blendedNightCloudColor);
                float[] smooth = WorldRendererCloudMixin.ambientfog$getSmoothedTarget(cfg, biomeId, target);
                return new Vec3((double)smooth[0], (double)smooth[1], (double)smooth[2]);
            }
            ambientfog$hasSmooth = false;
            ambientfog$hasFogTintSmooth = false;
            return vanilla;
        }
        ambientfog$hasSmooth = false;
        ambientfog$hasFogTintSmooth = false;
        return vanilla;
    }

    @Redirect(method={"renderClouds"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"))
    private float atmospherics$modifyCloudHeight(DimensionSpecialEffects effects) {
        float height = effects.getCloudHeight();
        CloudsSettings clouds = WorldRendererCloudMixin.ambientfog$getActiveStoryCloudSettings();
        return clouds == null ? height : height + clouds.storeModeCloudLayerHeight;
    }

    @ModifyConstant(method={"renderClouds"}, constant={@Constant(floatValue=0.03f)}, require=0)
    private float atmospherics$modifyCloudSpeed(float original) {
        CloudsSettings clouds = WorldRendererCloudMixin.ambientfog$getActiveStoryCloudSettings();
        return clouds == null ? original : original * Mth.clamp((float)clouds.storeModeCloudSpeed, (float)0.0f, (float)10.0f);
    }

    @ModifyArg(method={"buildClouds"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index=3, require=0)
    private float atmospherics$modifyCloudOpacity(float originalAlpha) {
        CloudsSettings clouds = WorldRendererCloudMixin.ambientfog$getActiveStoryCloudSettings();
        return clouds == null ? originalAlpha : originalAlpha * Mth.clamp((float)clouds.storeModeCloudOpacity, (float)0.0f, (float)1.0f);
    }

    @Inject(method={"renderClouds"}, at={@At(value="HEAD")}, cancellable=true)
    private void atmospherics$cancelNightClouds(PoseStack matrices, Matrix4f matrix1, Matrix4f matrix2, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        CloudsSettings clouds = WorldRendererCloudMixin.ambientfog$getEnabledCloudSettings();
        if (clouds != null && !clouds.renderNightClouds) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && WorldRendererCloudMixin.ambientfog$isFullNight(mc.level.getDayTime())) {
                ambientfog$hasSmooth = false;
                ci.cancel();
            }
        }
    }

    @Unique
    private static int ambientfog$packVec3(Vec3 color) {
        int r = Mth.clamp((int)Math.round((float)color.x * 255.0f), (int)0, (int)255);
        int g = Mth.clamp((int)Math.round((float)color.y * 255.0f), (int)0, (int)255);
        int b = Mth.clamp((int)Math.round((float)color.z * 255.0f), (int)0, (int)255);
        return r << 16 | g << 8 | b;
    }

    @Unique
    private static float[] ambientfog$resolveTarget(CloudsSettings clouds, int cloudColor, float[] fogTint, long worldTime, int originalRgb, float weatherDark, float rainBlend, float thunderBlend, int rainCloudColor, int thunderCloudColor, float weatherNightDarkening, float twilightBlend, int twilightCloudColor, int nightCloudColor) {
        float weatherNightDark;
        float strength = Mth.clamp((float)clouds.tintStrength, (float)0.0f, (float)1.0f);
        float fogMix = Mth.clamp((float)clouds.fogColorMixStrength, (float)0.0f, (float)1.0f);
        float or = (float)(originalRgb >> 16 & 0xFF) / 255.0f;
        float og = (float)(originalRgb >> 8 & 0xFF) / 255.0f;
        float ob = (float)(originalRgb & 0xFF) / 255.0f;
        float tr = (float)(cloudColor >> 16 & 0xFF) / 255.0f;
        float tg = (float)(cloudColor >> 8 & 0xFF) / 255.0f;
        float tb = (float)(cloudColor & 0xFF) / 255.0f;
        float fr = fogTint[0];
        float fg = fogTint[1];
        float fb = fogTint[2];
        float nightBlend = WorldRendererCloudMixin.ambientfog$getNightBlend(worldTime);
        float r = Mth.lerp((float)strength, (float)or, (float)(or * tr));
        float g = Mth.lerp((float)strength, (float)og, (float)(og * tg));
        float b = Mth.lerp((float)strength, (float)ob, (float)(ob * tb));
        r = Mth.lerp((float)fogMix, (float)r, (float)fr);
        g = Mth.lerp((float)fogMix, (float)g, (float)fg);
        b = Mth.lerp((float)fogMix, (float)b, (float)fb);
        float nightDark = Mth.clamp((float)clouds.nightDarkening, (float)0.0f, (float)1.0f) * nightBlend;
        float nightKeep = 1.0f - nightDark;
        r *= nightKeep;
        g *= nightKeep;
        b *= nightKeep;
        if (rainBlend > 0.0f) {
            r = Mth.lerp((float)rainBlend, (float)r, (float)((float)(rainCloudColor >> 16 & 0xFF) / 255.0f));
            g = Mth.lerp((float)rainBlend, (float)g, (float)((float)(rainCloudColor >> 8 & 0xFF) / 255.0f));
            b = Mth.lerp((float)rainBlend, (float)b, (float)((float)(rainCloudColor & 0xFF) / 255.0f));
        }
        if (thunderBlend > 0.0f) {
            r = Mth.lerp((float)thunderBlend, (float)r, (float)((float)(thunderCloudColor >> 16 & 0xFF) / 255.0f));
            g = Mth.lerp((float)thunderBlend, (float)g, (float)((float)(thunderCloudColor >> 8 & 0xFF) / 255.0f));
            b = Mth.lerp((float)thunderBlend, (float)b, (float)((float)(thunderCloudColor & 0xFF) / 255.0f));
        }
        if (twilightBlend > 0.0f) {
            r = Mth.lerp((float)twilightBlend, (float)r, (float)((float)(twilightCloudColor >> 16 & 0xFF) / 255.0f));
            g = Mth.lerp((float)twilightBlend, (float)g, (float)((float)(twilightCloudColor >> 8 & 0xFF) / 255.0f));
            b = Mth.lerp((float)twilightBlend, (float)b, (float)((float)(twilightCloudColor & 0xFF) / 255.0f));
        }
        if (nightBlend > 0.0f) {
            r = Mth.lerp((float)nightBlend, (float)r, (float)((float)(nightCloudColor >> 16 & 0xFF) / 255.0f));
            g = Mth.lerp((float)nightBlend, (float)g, (float)((float)(nightCloudColor >> 8 & 0xFF) / 255.0f));
            b = Mth.lerp((float)nightBlend, (float)b, (float)((float)(nightCloudColor & 0xFF) / 255.0f));
        }
        if ((rainBlend > 0.0f || thunderBlend > 0.0f) && (weatherNightDark = nightBlend * weatherNightDarkening) > 0.0f) {
            float keep = 1.0f - weatherNightDark;
            r *= keep;
            g *= keep;
            b *= keep;
        }
        if (weatherDark > 0.0f) {
            float weatherKeep = 1.0f - weatherDark;
            r *= weatherKeep;
            g *= weatherKeep;
            b *= weatherKeep;
        }
        return new float[]{Mth.clamp((float)r, (float)0.0f, (float)1.0f), Mth.clamp((float)g, (float)0.0f, (float)1.0f), Mth.clamp((float)b, (float)0.0f, (float)1.0f)};
    }

    @Unique
    private static float[] ambientfog$getSmoothedFogTint(FogConfig cfg, String biomeId, int fogColor, int nightFogColor, long worldTime) {
        float fr = (float)(fogColor >> 16 & 0xFF) / 255.0f;
        float fg = (float)(fogColor >> 8 & 0xFF) / 255.0f;
        float fb = (float)(fogColor & 0xFF) / 255.0f;
        float nfr = (float)(nightFogColor >> 16 & 0xFF) / 255.0f;
        float nfg = (float)(nightFogColor >> 8 & 0xFF) / 255.0f;
        float nfb = (float)(nightFogColor & 0xFF) / 255.0f;
        float nightBlend = WorldRendererCloudMixin.ambientfog$getNightBlend(worldTime);
        float targetR = Mth.lerp((float)nightBlend, (float)fr, (float)nfr);
        float targetG = Mth.lerp((float)nightBlend, (float)fg, (float)nfg);
        float targetB = Mth.lerp((float)nightBlend, (float)fb, (float)nfb);
        long now = System.nanoTime();
        if (!ambientfog$hasFogTintSmooth) {
            ambientfog$fogTintR = targetR;
            ambientfog$fogTintG = targetG;
            ambientfog$fogTintB = targetB;
            ambientfog$fogTintLastNanos = now;
            ambientfog$hasFogTintSmooth = true;
            return new float[]{targetR, targetG, targetB};
        }
        if (!(Atmospherics.shouldInstantApplyFor(biomeId) || Atmospherics.shouldInstantApplyFor("clouds") || Atmospherics.shouldInstantApplyFor("weather"))) {
            float dt = Math.max(0.0f, Math.min(0.5f, (float)(now - ambientfog$fogTintLastNanos) / 1.0E9f));
            ambientfog$fogTintLastNanos = now;
            float blendBoost = Atmospherics.getPresetBlendTauMultiplier();
            float baseTau = (cfg == null ? 2.0f : Mth.clamp((float)cfg.transitionColorTau, (float)0.05f, (float)2.0f)) * blendBoost;
            baseTau = Mth.clamp((float)baseTau, (float)0.05f, (float)12.0f);
            float fogTintTau = Mth.clamp((float)(baseTau * 4.0f), (float)0.35f, (float)8.0f);
            float alpha = WorldRendererCloudMixin.ambientfog$expAlpha(dt, fogTintTau);
            ambientfog$fogTintR = Mth.lerp((float)alpha, (float)ambientfog$fogTintR, (float)targetR);
            ambientfog$fogTintG = Mth.lerp((float)alpha, (float)ambientfog$fogTintG, (float)targetG);
            ambientfog$fogTintB = Mth.lerp((float)alpha, (float)ambientfog$fogTintB, (float)targetB);
            return new float[]{ambientfog$fogTintR, ambientfog$fogTintG, ambientfog$fogTintB};
        }
        ambientfog$fogTintR = targetR;
        ambientfog$fogTintG = targetG;
        ambientfog$fogTintB = targetB;
        ambientfog$fogTintLastNanos = now;
        return new float[]{ambientfog$fogTintR, ambientfog$fogTintG, ambientfog$fogTintB};
    }

    @Unique
    private static float[] ambientfog$getImmediateFogTint(int fogColor, int nightFogColor, long worldTime) {
        ambientfog$hasFogTintSmooth = false;
        float fr = (float)(fogColor >> 16 & 0xFF) / 255.0f;
        float fg = (float)(fogColor >> 8 & 0xFF) / 255.0f;
        float fb = (float)(fogColor & 0xFF) / 255.0f;
        float nfr = (float)(nightFogColor >> 16 & 0xFF) / 255.0f;
        float nfg = (float)(nightFogColor >> 8 & 0xFF) / 255.0f;
        float nfb = (float)(nightFogColor & 0xFF) / 255.0f;
        float nightBlend = WorldRendererCloudMixin.ambientfog$getNightBlend(worldTime);
        return new float[]{Mth.lerp((float)nightBlend, (float)fr, (float)nfr), Mth.lerp((float)nightBlend, (float)fg, (float)nfg), Mth.lerp((float)nightBlend, (float)fb, (float)nfb)};
    }

    @Unique
    private static float[] ambientfog$getSmoothedTarget(FogConfig cfg, String biomeId, float[] target) {
        long now = System.nanoTime();
        if (!ambientfog$hasSmooth) {
            ambientfog$r = target[0];
            ambientfog$g = target[1];
            ambientfog$b = target[2];
            ambientfog$lastNanos = now;
            ambientfog$hasSmooth = true;
            return target;
        }
        if (!(Atmospherics.shouldInstantApplyFor(biomeId) || Atmospherics.shouldInstantApplyFor("clouds") || Atmospherics.shouldInstantApplyFor("weather"))) {
            float dt = Math.max(0.0f, Math.min(0.5f, (float)(now - ambientfog$lastNanos) / 1.0E9f));
            ambientfog$lastNanos = now;
            float blendBoost = Atmospherics.getPresetBlendTauMultiplier();
            float colorTau = (cfg == null ? 2.0f : Mth.clamp((float)cfg.transitionColorTau, (float)0.05f, (float)2.0f)) * blendBoost;
            colorTau = Mth.clamp((float)colorTau, (float)0.05f, (float)12.0f);
            float alpha = WorldRendererCloudMixin.ambientfog$expAlpha(dt, colorTau);
            ambientfog$r = Mth.lerp((float)alpha, (float)ambientfog$r, (float)target[0]);
            ambientfog$g = Mth.lerp((float)alpha, (float)ambientfog$g, (float)target[1]);
            ambientfog$b = Mth.lerp((float)alpha, (float)ambientfog$b, (float)target[2]);
            return new float[]{ambientfog$r, ambientfog$g, ambientfog$b};
        }
        ambientfog$r = target[0];
        ambientfog$g = target[1];
        ambientfog$b = target[2];
        ambientfog$lastNanos = now;
        return new float[]{ambientfog$r, ambientfog$g, ambientfog$b};
    }

    @Unique
    private static float ambientfog$expAlpha(float dtSeconds, float tauSeconds) {
        return tauSeconds <= 0.0f ? 1.0f : (float)(1.0 - Math.exp(-dtSeconds / tauSeconds));
    }

    @Unique
    private static float ambientfog$getNightBlend(long worldTime) {
        long dayTime = worldTime % 24000L;
        if (dayTime < 0L) {
            dayTime += 24000L;
        }
        if (dayTime < 12000L) {
            return 0.0f;
        }
        if (dayTime < 14000L) {
            return WorldRendererCloudMixin.ambientfog$smoothstep(12000.0f, 14000.0f, dayTime);
        }
        if (dayTime <= 22000L) {
            return 1.0f;
        }
        return dayTime <= 24000L ? 1.0f - WorldRendererCloudMixin.ambientfog$smoothstep(22000.0f, 24000.0f, dayTime) : 0.0f;
    }

    @Unique
    private static float ambientfog$getTwilightCloudTimeFactor(FogConfig cfg, long worldTime) {
        if (cfg == null) {
            return 0.0f;
        }
        float sunset = DayCycleWindowUtil.factor(worldTime, cfg.twilightSunsetTime);
        float sunrise = DayCycleWindowUtil.factor(worldTime, cfg.twilightSunriseTime);
        return Math.max(sunset, sunrise);
    }

    @Unique
    private static boolean ambientfog$isFullNight(long worldTime) {
        long dayTime = worldTime % 24000L;
        if (dayTime < 0L) {
            dayTime += 24000L;
        }
        return dayTime >= 14100L && dayTime <= 21900L;
    }

    @Unique
    private static float ambientfog$smoothstep(float edge0, float edge1, float x) {
        if (edge1 <= edge0) {
            return x < edge0 ? 0.0f : 1.0f;
        }
        float t = Mth.clamp((float)((x - edge0) / (edge1 - edge0)), (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    @Unique
    private static float ambientfog$getWeatherDarken() {
        return 0.0f;
    }

    @Unique
    private static int ambientfog$getRainCloudColor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            WeatherSettings settings = AtmosphericEnvironmentVisuals.resolveWeatherSettings(mc.level, new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            return settings.rainCloudColor & 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    @Unique
    private static int ambientfog$getThunderCloudColor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            WeatherSettings settings = AtmosphericEnvironmentVisuals.resolveWeatherSettings(mc.level, new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            return settings.thunderCloudColor & 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    @Unique
    private static float ambientfog$getWeatherCloudNightDarkening() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            WeatherSettings settings = AtmosphericEnvironmentVisuals.resolveWeatherSettings(mc.level, new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
            return settings.cloudNightColorDarkening;
        }
        return 0.0f;
    }

    @Unique
    private static CloudsSettings ambientfog$getEnabledCloudSettings() {
        FogConfig cfg = Atmospherics.getConfig();
        if (cfg != null && cfg.clouds != null) {
            CloudsSettings clouds = cfg.clouds;
            clouds.sanitize();
            return clouds.enabled ? clouds : null;
        }
        return null;
    }

    @Unique
    private static CloudsSettings ambientfog$getActiveStoryCloudSettings() {
        CloudsSettings clouds = WorldRendererCloudMixin.ambientfog$getEnabledCloudSettings();
        return clouds != null && clouds.storeModeCloud ? clouds : null;
    }

    static {
        ambientfog$lastAppliedCloudOpacity = Float.NaN;
    }
}

