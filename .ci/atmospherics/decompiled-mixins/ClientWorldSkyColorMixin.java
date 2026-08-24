/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.beash.atmospherics.mixin;

import com.beash.atmospherics.util.AtmosphericEnvironmentVisuals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientLevel.class})
public abstract class ClientWorldSkyColorMixin {
    @Inject(method={"getSkyColor"}, at={@At(value="RETURN")}, cancellable=true)
    private void atmospherics$overrideSkyColor(Vec3 cameraPos, float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        ClientLevel world = (ClientLevel)this;
        Vec3 original = (Vec3)cir.getReturnValue();
        int packed = ClientWorldSkyColorMixin.packRgb((float)original.x, (float)original.y, (float)original.z);
        int result = AtmosphericEnvironmentVisuals.getSkyColor(world, cameraPos, packed);
        if (result != packed) {
            double r = (float)(result >> 16 & 0xFF) / 255.0f;
            double g = (float)(result >> 8 & 0xFF) / 255.0f;
            double b = (float)(result & 0xFF) / 255.0f;
            cir.setReturnValue((Object)new Vec3(r, g, b));
        }
    }

    private static int packRgb(float r, float g, float b) {
        int ir = Mth.clamp((int)Math.round(r * 255.0f), (int)0, (int)255);
        int ig = Mth.clamp((int)Math.round(g * 255.0f), (int)0, (int)255);
        int ib = Mth.clamp((int)Math.round(b * 255.0f), (int)0, (int)255);
        return ir << 16 | ig << 8 | ib;
    }
}

