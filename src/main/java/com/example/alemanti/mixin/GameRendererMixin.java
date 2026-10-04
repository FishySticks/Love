package com.example.alemanti.mixin;

import com.example.alemanti.module.modules.HurtCam;
import com.example.alemanti.module.modules.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void alemanti$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (!changingFov) return; // leave hand FOV alone
        double m = Zoom.INSTANCE.fovMultiplier();
        if (m < 1.0) cir.setReturnValue(cir.getReturnValue() * m);
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void alemanti$hurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!HurtCam.INSTANCE.enabled) return;
        HurtCam.INSTANCE.apply(MinecraftClient.getInstance(), matrices, tickDelta);
        ci.cancel();
    }
}
