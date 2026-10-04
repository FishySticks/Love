package com.example.alemanti.mixin;

import com.example.alemanti.module.modules.Nametags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    /** Vanilla never labels the camera entity; allow our own nametag in third person. */
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void alemanti$ownNametag(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        Nametags nt = Nametags.INSTANCE;
        if (!nt.enabled || !nt.own.value || cir.getReturnValueZ()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity == mc.player && MinecraftClient.isHudEnabled()
                && !mc.options.getPerspective().isFirstPerson() && !entity.isInvisible()) {
            cir.setReturnValue(true);
        }
    }
}
