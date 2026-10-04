package com.example.alemanti.mixin;

import com.example.alemanti.module.modules.LowFire;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void alemanti$lowFireStart(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (LowFire.INSTANCE.enabled) matrices.translate(0.0f, -0.3f, 0.0f);
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void alemanti$lowFireEnd(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (LowFire.INSTANCE.enabled) matrices.translate(0.0f, 0.3f, 0.0f);
    }
}
