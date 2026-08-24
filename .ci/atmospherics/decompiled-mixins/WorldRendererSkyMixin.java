/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$DestFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SourceFactor
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.BufferUploader
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.MeshData
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexBuffer
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.DimensionSpecialEffects$SkyType
 *  net.minecraft.client.renderer.FogRenderer
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.ShaderInstance
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.LevelHeightAccessor
 *  net.minecraft.world.level.material.FogType
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Constant
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyConstant
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.beash.atmospherics.mixin;

import com.beash.atmospherics.Atmospherics;
import com.beash.atmospherics.config.FogConfig;
import com.beash.atmospherics.config.StarsSettings;
import com.beash.atmospherics.util.HorizonHazeCometRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={LevelRenderer.class})
public abstract class WorldRendererSkyMixin {
    @Unique
    private static final ResourceLocation ATMOSPHERICS_SUN = ResourceLocation.withDefaultNamespace((String)"textures/environment/sun.png");
    @Unique
    private static final ResourceLocation ATMOSPHERICS_MOON = ResourceLocation.withDefaultNamespace((String)"textures/environment/moon_phases.png");
    @Unique
    private float atmospherics$appliedCountFactor = -1.0f;
    @Unique
    private float atmospherics$appliedSizeFactor = -1.0f;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private ClientLevel level;
    @Shadow
    private VertexBuffer skyBuffer;
    @Shadow
    private VertexBuffer darkBuffer;
    @Shadow
    private VertexBuffer starBuffer;

    @Shadow
    protected abstract void createStars();

    @Shadow
    protected abstract void renderEndSky(PoseStack var1);

    @Shadow
    protected abstract boolean doesMobEffectBlockSky(Camera var1);

    @ModifyConstant(method={"drawStars"}, constant={@Constant(intValue=1500)})
    private int atmospherics$modifyStarCount(int original) {
        return Math.max(0, Math.round((float)original * WorldRendererSkyMixin.atmospherics$starCountFactor()));
    }

    @ModifyConstant(method={"drawStars"}, constant={@Constant(floatValue=0.15f)})
    private float atmospherics$modifyStarBaseSize(float original) {
        return original * WorldRendererSkyMixin.atmospherics$starSizeFactor();
    }

    @ModifyConstant(method={"drawStars"}, constant={@Constant(floatValue=0.1f)})
    private float atmospherics$modifyStarRandomSize(float original) {
        return original * WorldRendererSkyMixin.atmospherics$starSizeFactor();
    }

    @Inject(method={"renderSky"}, at={@At(value="HEAD")}, cancellable=true)
    private void atmospherics$reimplementSky(Matrix4f positionMatrix, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        ClientLevel w;
        FogType submersion;
        if (!thickFog && (submersion = camera.getFluidInCamera()) != FogType.POWDER_SNOW && submersion != FogType.LAVA && !this.doesMobEffectBlockSky(camera) && (w = this.level).effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
            boolean disableRing;
            ci.cancel();
            fogCallback.run();
            FogConfig cfg = Atmospherics.getConfig();
            boolean hideVoid = WorldRendererSkyMixin.atmospherics$shouldHideSkyInVoid(w, this.minecraft.gameRenderer.getMainCamera());
            PoseStack matrices = new PoseStack();
            matrices.mulPose(positionMatrix);
            Vec3 skyColor = w.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), tickDelta);
            float skyR = (float)skyColor.x;
            float skyG = (float)skyColor.y;
            float skyB = (float)skyColor.z;
            FogRenderer.levelFogColor();
            Tesselator tessellator = Tesselator.getInstance();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.setShaderColor((float)skyR, (float)skyG, (float)skyB, (float)1.0f);
            ShaderInstance domeShader = RenderSystem.getShader();
            this.skyBuffer.bind();
            this.skyBuffer.drawWithShader(matrices.last().pose(), projectionMatrix, domeShader);
            VertexBuffer.unbind();
            RenderSystem.enableBlend();
            float[] sunrise = w.effects().getSunriseColor(w.getTimeOfDay(tickDelta), tickDelta);
            boolean bl = disableRing = cfg != null && cfg.disableTwilightRing || hideVoid;
            if (sunrise != null && !disableRing) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                matrices.pushPose();
                matrices.mulPose(Axis.XP.rotationDegrees(90.0f));
                float roll = Mth.sin((float)w.getSunAngle(tickDelta)) < 0.0f ? 180.0f : 0.0f;
                matrices.mulPose(Axis.ZP.rotationDegrees(roll));
                matrices.mulPose(Axis.ZP.rotationDegrees(90.0f));
                Matrix4f m = matrices.last().pose();
                BufferBuilder builder = tessellator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builder.addVertex(m, 0.0f, 100.0f, 0.0f).setColor(sunrise[0], sunrise[1], sunrise[2], sunrise[3]);
                for (int i = 0; i <= 16; ++i) {
                    float angle = (float)i * ((float)Math.PI * 2) / 16.0f;
                    float sin = Mth.sin((float)angle);
                    float cos = Mth.cos((float)angle);
                    builder.addVertex(m, sin * 120.0f, cos * 120.0f, -cos * 40.0f * sunrise[3]).setColor(sunrise[0], sunrise[1], sunrise[2], 0.0f);
                }
                BufferUploader.drawWithShader((MeshData)builder.buildOrThrow());
                matrices.popPose();
            }
            HorizonHazeCometRenderer.renderHaze(matrices, projectionMatrix, tickDelta);
            if (!hideVoid) {
                RenderSystem.blendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
                matrices.pushPose();
                float rainAlpha = 1.0f - w.getRainLevel(tickDelta);
                matrices.mulPose(Axis.YP.rotationDegrees(-90.0f));
                matrices.mulPose(Axis.XP.rotationDegrees(w.getTimeOfDay(tickDelta) * 360.0f));
                matrices.pushPose();
                float sunRotation = WorldRendererSkyMixin.atmospherics$getConfiguredSunRotation();
                if (sunRotation != 0.0f) {
                    matrices.mulPose(Axis.YP.rotationDegrees(sunRotation));
                }
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)rainAlpha);
                float sunSize = 30.0f * WorldRendererSkyMixin.atmospherics$getConfiguredSunScale();
                Matrix4f sunMatrix = matrices.last().pose();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture((int)0, (ResourceLocation)ATMOSPHERICS_SUN);
                BufferBuilder sunBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                sunBuilder.addVertex(sunMatrix, -sunSize, 100.0f, -sunSize).setUv(0.0f, 0.0f);
                sunBuilder.addVertex(sunMatrix, sunSize, 100.0f, -sunSize).setUv(1.0f, 0.0f);
                sunBuilder.addVertex(sunMatrix, sunSize, 100.0f, sunSize).setUv(1.0f, 1.0f);
                sunBuilder.addVertex(sunMatrix, -sunSize, 100.0f, sunSize).setUv(0.0f, 1.0f);
                BufferUploader.drawWithShader((MeshData)sunBuilder.buildOrThrow());
                matrices.popPose();
                matrices.pushPose();
                float moonRotation = WorldRendererSkyMixin.atmospherics$getConfiguredMoonRotation();
                if (moonRotation != 0.0f) {
                    matrices.mulPose(Axis.YP.rotationDegrees(moonRotation));
                }
                float moonSize = 20.0f * WorldRendererSkyMixin.atmospherics$getConfiguredMoonScale();
                Matrix4f moonMatrix = matrices.last().pose();
                RenderSystem.setShaderTexture((int)0, (ResourceLocation)ATMOSPHERICS_MOON);
                int phase = w.getMoonPhase();
                int phaseX = phase % 4;
                int phaseY = phase / 4 % 2;
                float u0 = (float)phaseX / 4.0f;
                float v0 = (float)phaseY / 2.0f;
                float u1 = (float)(phaseX + 1) / 4.0f;
                float v1 = (float)(phaseY + 1) / 2.0f;
                BufferBuilder moonBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                moonBuilder.addVertex(moonMatrix, -moonSize, -100.0f, moonSize).setUv(u1, v1);
                moonBuilder.addVertex(moonMatrix, moonSize, -100.0f, moonSize).setUv(u0, v1);
                moonBuilder.addVertex(moonMatrix, moonSize, -100.0f, -moonSize).setUv(u0, v0);
                moonBuilder.addVertex(moonMatrix, -moonSize, -100.0f, -moonSize).setUv(u1, v0);
                BufferUploader.drawWithShader((MeshData)moonBuilder.buildOrThrow());
                matrices.popPose();
                this.atmospherics$renderStars(matrices, projectionMatrix, w, tickDelta, rainAlpha, cfg, fogCallback);
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                matrices.popPose();
            }
            HorizonHazeCometRenderer.renderComets(matrices, projectionMatrix, tickDelta);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            double darkHeight = this.minecraft.player.getEyePosition((float)tickDelta).y - w.getLevelData().getHorizonHeight((LevelHeightAccessor)w);
            if (darkHeight < 0.0) {
                matrices.pushPose();
                matrices.translate(0.0f, 12.0f, 0.0f);
                this.darkBuffer.bind();
                this.darkBuffer.drawWithShader(matrices.last().pose(), projectionMatrix, domeShader);
                VertexBuffer.unbind();
                matrices.popPose();
            }
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            RenderSystem.depthMask((boolean)true);
        }
    }

    @Unique
    private void atmospherics$renderStars(PoseStack matrices, Matrix4f projectionMatrix, ClientLevel w, float tickDelta, float rainAlpha, FogConfig cfg, Runnable fogCallback) {
        float vanillaBrightness = w.getStarBrightness(tickDelta) * rainAlpha;
        this.atmospherics$ensureStarBuffer();
        if (cfg != null && cfg.stars != null && cfg.stars.enabled) {
            StarsSettings stars = cfg.stars;
            stars.sanitize();
            if (vanillaBrightness > 0.0f) {
                this.atmospherics$drawStarLayers(matrices, projectionMatrix, w, vanillaBrightness, false, stars, fogCallback);
            }
            if (stars.dayStars && !(vanillaBrightness > 0.05f)) {
                float dayBrightness = Mth.clamp((float)stars.dayBrightness, (float)0.0f, (float)2.0f);
                this.atmospherics$drawStarLayers(matrices, projectionMatrix, w, dayBrightness, true, stars, fogCallback);
            }
        } else if (vanillaBrightness > 0.0f) {
            RenderSystem.setShaderColor((float)vanillaBrightness, (float)vanillaBrightness, (float)vanillaBrightness, (float)vanillaBrightness);
            FogRenderer.setupNoFog();
            this.starBuffer.bind();
            this.starBuffer.drawWithShader(matrices.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            fogCallback.run();
        }
    }

    @Unique
    private void atmospherics$drawStarLayers(PoseStack matrices, Matrix4f projectionMatrix, ClientLevel w, float incomingBrightness, boolean day, StarsSettings stars, Runnable fogCallback) {
        float activeBrightness = Mth.clamp((float)(day ? stars.dayBrightness : stars.brightness), (float)0.0f, (float)2.0f);
        float activeTwinkle = Mth.clamp((float)(day ? stars.dayTwinkle : stars.twinkle), (float)0.0f, (float)1.0f);
        float amount = Mth.clamp((float)(day ? stars.dayAmount : stars.amount), (float)0.0f, (float)1500.0f);
        long worldTime = w.getGameTime();
        FogRenderer.setupNoFog();
        this.starBuffer.bind();
        this.atmospherics$drawStarLayer(matrices, projectionMatrix, 0, 0, incomingBrightness, activeBrightness, activeTwinkle, worldTime, 1.0f);
        float over = Math.max(0.0f, amount - 100.0f) / 100.0f;
        int fullLayers = (int)Math.floor(over);
        float partial = over - (float)fullLayers;
        int index = 1;
        int layer = 1;
        for (int i = 0; i < fullLayers; ++i) {
            this.atmospherics$drawStarLayer(matrices, projectionMatrix, layer++, index++, incomingBrightness, activeBrightness, activeTwinkle, worldTime, 1.0f);
        }
        if (partial > 0.01f) {
            this.atmospherics$drawStarLayer(matrices, projectionMatrix, layer++, index++, incomingBrightness, activeBrightness, activeTwinkle, worldTime, partial);
        }
        VertexBuffer.unbind();
        fogCallback.run();
    }

    @Unique
    private void atmospherics$drawStarLayer(PoseStack matrices, Matrix4f projectionMatrix, int layer, int index, float incomingBrightness, float activeBrightness, float activeTwinkle, long worldTime, float countFade) {
        float result = incomingBrightness * activeBrightness;
        float t = (float)worldTime * (0.22f + 0.05f * (float)(layer % 5));
        float phase = (float)layer * 1.73f;
        float layerTwinkle = activeTwinkle * (0.65f + 0.35f * ((float)(layer * 37 % 100) / 100.0f));
        float flicker = 1.0f + (float)(Math.sin(t + phase) * 0.24 + Math.sin(t * 1.91f + phase * 0.6f) * 0.12) * layerTwinkle;
        float brightness = Mth.clamp((float)(result * flicker), (float)0.0f, (float)1.0f) * countFade;
        RenderSystem.setShaderColor((float)brightness, (float)brightness, (float)brightness, (float)brightness);
        if (index == 0) {
            this.starBuffer.drawWithShader(matrices.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
        } else {
            matrices.pushPose();
            matrices.mulPose(Axis.ZP.rotationDegrees((float)index * 31.0f));
            matrices.mulPose(Axis.XP.rotationDegrees((float)index * 17.0f));
            this.starBuffer.drawWithShader(matrices.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
            matrices.popPose();
        }
    }

    @Unique
    private void atmospherics$ensureStarBuffer() {
        float countFactor = WorldRendererSkyMixin.atmospherics$starCountFactor();
        float sizeFactor = WorldRendererSkyMixin.atmospherics$starSizeFactor();
        if (this.starBuffer == null || Math.abs(countFactor - this.atmospherics$appliedCountFactor) > 1.0E-4f || Math.abs(sizeFactor - this.atmospherics$appliedSizeFactor) > 1.0E-4f) {
            this.createStars();
            this.atmospherics$appliedCountFactor = countFactor;
            this.atmospherics$appliedSizeFactor = sizeFactor;
        }
    }

    @Unique
    private static float atmospherics$starCountFactor() {
        FogConfig cfg = Atmospherics.getConfig();
        if (cfg != null && cfg.stars != null && cfg.stars.enabled) {
            cfg.stars.sanitize();
            return Mth.clamp((float)(cfg.stars.amount / 100.0f), (float)0.0f, (float)1.0f);
        }
        return 1.0f;
    }

    @Unique
    private static float atmospherics$starSizeFactor() {
        FogConfig cfg = Atmospherics.getConfig();
        if (cfg != null && cfg.stars != null) {
            cfg.stars.sanitize();
            return Mth.clamp((float)cfg.stars.size, (float)0.5f, (float)3.0f);
        }
        return 1.0f;
    }

    @Unique
    private static float atmospherics$getConfiguredSunScale() {
        FogConfig cfg = Atmospherics.getConfig();
        return cfg == null ? 1.0f : Mth.clamp((float)(cfg.sunTextureScale <= 0.0f ? 1.0f : cfg.sunTextureScale), (float)0.25f, (float)3.0f);
    }

    @Unique
    private static float atmospherics$getConfiguredMoonScale() {
        FogConfig cfg = Atmospherics.getConfig();
        return cfg == null ? 1.0f : Mth.clamp((float)(cfg.moonTextureScale <= 0.0f ? 1.0f : cfg.moonTextureScale), (float)0.25f, (float)3.0f);
    }

    @Unique
    private static float atmospherics$getConfiguredSunRotation() {
        FogConfig cfg = Atmospherics.getConfig();
        return cfg == null ? 0.0f : WorldRendererSkyMixin.atmospherics$normalizeDegrees(cfg.sunTextureRotation);
    }

    @Unique
    private static float atmospherics$getConfiguredMoonRotation() {
        FogConfig cfg = Atmospherics.getConfig();
        return cfg == null ? 0.0f : WorldRendererSkyMixin.atmospherics$normalizeDegrees(cfg.moonTextureRotation);
    }

    @Unique
    private static float atmospherics$normalizeDegrees(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return (value %= 360.0f) < 0.0f ? value + 360.0f : value;
    }

    @Unique
    private static boolean atmospherics$shouldHideSkyInVoid(ClientLevel w, Camera camera) {
        FogConfig cfg = Atmospherics.getConfig();
        if (cfg != null && cfg.removeSkyInVoid && w != null && camera != null) {
            Vec3 pos = camera.getPosition();
            return pos != null && pos.y < (double)w.getMinBuildHeight() - 1.5;
        }
        return false;
    }
}

