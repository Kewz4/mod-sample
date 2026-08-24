package com.beash.atmospherics.mixin;

import com.beash.atmospherics.compat.ShaderCompat;
import com.beash.atmospherics.util.HorizonHazeCometRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Iris keeps ownership of the sky/celestial pass. Atmospherics adds its horizon
 * haze after that pass instead of replacing or disabling the shader-pack sky.
 */
@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class ShaderHazePassMixin {
    @Inject(method = "renderSky", at = @At("RETURN"), require = 0)
    private void atmospherics$renderHazeAfterShaderSky(
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            float tickDelta,
            Camera camera,
            boolean thickFog,
            Runnable fogCallback,
            CallbackInfo ci) {
        if (!ShaderCompat.isShaderPackInUse() || thickFog || camera == null) {
            return;
        }

        FogType fogType = camera.getFluidInCamera();
        if (fogType == FogType.LAVA || fogType == FogType.POWDER_SNOW || fogType == FogType.WATER) {
            return;
        }

        PoseStack matrices = new PoseStack();
        matrices.mulPose(positionMatrix);
        HorizonHazeCometRenderer.renderHaze(matrices, projectionMatrix, tickDelta);
        HorizonHazeCometRenderer.renderComets(matrices, projectionMatrix, tickDelta);
    }
}
