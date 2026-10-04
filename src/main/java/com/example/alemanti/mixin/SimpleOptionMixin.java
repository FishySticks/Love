package com.example.alemanti.mixin;

import com.example.alemanti.module.modules.Fullbright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {
    /** Fullbright: report a huge gamma value without touching the saved option. */
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void alemanti$fullbright(CallbackInfoReturnable<T> cir) {
        if (!Fullbright.INSTANCE.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && (Object) this == mc.options.getGamma()) {
            cir.setReturnValue((T) Double.valueOf(16.0));
        }
    }
}
