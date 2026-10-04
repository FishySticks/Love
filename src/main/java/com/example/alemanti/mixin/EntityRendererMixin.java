package com.example.alemanti.mixin;

import com.example.alemanti.module.modules.Nametags;
import com.example.alemanti.module.modules.NickHider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow @org.spongepowered.asm.mixin.Final protected EntityRenderDispatcher dispatcher;
    @Shadow public abstract TextRenderer getTextRenderer();

    /** Reset once per frame by AlemantiClient (WorldRenderEvents.START). */
    public static int alemanti$lastPingId = -1;

    /** Nick hider: swap your own name in the label. */
    @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
    private Text alemanti$label(Text original, T entity) {
        if (entity == MinecraftClient.getInstance().player && NickHider.INSTANCE.enabled) {
            return NickHider.INSTANCE.apply(original);
        }
        return original;
    }

    /** Draws "NN ms" on its own line ABOVE the username. */
    @Inject(method = "renderLabelIfPresent", at = @At("RETURN"))
    private void alemanti$pingAbove(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vcp,
                                    int light, float tickDelta, CallbackInfo ci) {
        Nametags nt = Nametags.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!nt.enabled || !nt.ping.value || !(entity instanceof PlayerEntity player) || mc.getNetworkHandler() == null) return;
        if (alemanti$lastPingId == entity.getId()) return; // skip the scoreboard sub-label call
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null || (entry.getLatency() <= 0 && player != mc.player)) return;
        alemanti$lastPingId = entity.getId();

        int ping = entry.getLatency();
        Formatting color = ping < 80 ? Formatting.GREEN : ping < 150 ? Formatting.YELLOW : Formatting.RED;
        Text label = Text.literal(ping + " ms").formatted(color);

        Vec3d pos = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw(tickDelta));
        if (pos == null) return;
        boolean visibleThroughWalls = !entity.isSneaky();

        matrices.push();
        matrices.translate(pos.x, pos.y + 0.5 + 0.27, pos.z); // one text line above the name
        matrices.multiply(dispatcher.getRotation());
        matrices.scale(0.025f, -0.025f, 0.025f);
        Matrix4f m = matrices.peek().getPositionMatrix();
        int bg = (int) (mc.options.getTextBackgroundOpacity(0.25f) * 255.0f) << 24;
        TextRenderer tr = getTextRenderer();
        float x = -tr.getWidth(label) / 2.0f;
        tr.draw(label, x, 0, 0x20FFFFFF, false, m, vcp,
                visibleThroughWalls ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL, bg, light);
        if (visibleThroughWalls) {
            tr.draw(label, x, 0, -1, false, m, vcp, TextRenderer.TextLayerType.NORMAL, 0, light);
        }
        matrices.pop();
    }
}
