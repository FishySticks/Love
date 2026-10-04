package com.example.alemanti.mixin;

import com.example.alemanti.module.modules.NickHider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void alemanti$nickHider(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!NickHider.INSTANCE.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entry.getProfile().getId().equals(mc.player.getUuid())) {
            cir.setReturnValue(NickHider.INSTANCE.apply(cir.getReturnValue()));
        }
    }
}
