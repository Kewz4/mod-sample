package com.beash.atmospherics.mixin;

import com.beash.atmospherics.compat.ShaderCompat;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CI-only assertion gated behind a JVM property; inert for normal players. */
@Mixin(Minecraft.class)
public abstract class ShaderCompatSmokeMixin {
    @Unique
    private static final Logger ATMOSPHERICS_SHADER_SMOKE_LOGGER =
            LoggerFactory.getLogger("Atmospherics/ShaderCompatSmoke");
    @Unique
    private static int atmospherics$shaderSmokeTicks;
    @Unique
    private static boolean atmospherics$shaderSmokeFinished;

    @Inject(method = "tick", at = @At("TAIL"))
    private void atmospherics$verifyActiveShaderPack(CallbackInfo ci) {
        if (atmospherics$shaderSmokeFinished
                || !Boolean.getBoolean("atmospherics.fabric.port.expectShader")) {
            return;
        }

        if (++atmospherics$shaderSmokeTicks < 250) {
            return;
        }

        atmospherics$shaderSmokeFinished = true;
        if (!ShaderCompat.isShaderPackInUse()) {
            throw new IllegalStateException(
                    "Iris loaded for the Atmospherics smoke test, but no shader pack became active");
        }

        ATMOSPHERICS_SHADER_SMOKE_LOGGER.info(
                "Atmospherics confirmed an active Iris shader pipeline after 250 client ticks");
    }
}
