package com.punchy.mixin;

import com.punchy.client.PunchyConfigScreen;
import com.punchy.client.PunchyRegexInputScreen;
import com.punchy.client.PunchyRulesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents the world-blur post-process effect from running when a Punchy
 * config screen is open. The blur is triggered by Screen.renderBlurredBackground
 * → GameRenderer.processBlurEffect and is applied to the game framebuffer each
 * frame, which causes the blurred world to show through semi-transparent GUIs
 * and bleed into the config screen visuals.
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void punchy$cancelBlur(float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PunchyConfigScreen
                || mc.screen instanceof PunchyRegexInputScreen
                || mc.screen instanceof PunchyRulesScreen) {
            ci.cancel();
        }
    }
}
