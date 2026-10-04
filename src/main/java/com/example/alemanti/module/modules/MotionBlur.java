package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Frame-blending motion blur: the previous frame is drawn over the new one with
 * alpha = Amount, then the result is stored for the next frame. Original implementation.
 */
public class MotionBlur extends Module {
    public static final MotionBlur INSTANCE = new MotionBlur();
    public final Setting.Num amount = add(new Setting.Num("Amount", 0.5f, 0.05f, 0.9f, 0.05f));

    private Framebuffer previous;
    private boolean hasPrevious;

    private MotionBlur() {
        super("Motion Blur", "Smooths movement by blending frames", Category.VISUAL, false);
    }

    public static void register() {
        WorldRenderEvents.END.register(ctx -> INSTANCE.onWorldEnd());
    }

    @Override
    protected void onDisable() { hasPrevious = false; }

    private void onWorldEnd() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth, h = main.textureHeight;
        if (previous == null) {
            previous = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
            hasPrevious = false;
        } else if (previous.textureWidth != w || previous.textureHeight != h) {
            previous.resize(w, h, MinecraftClient.IS_SYSTEM_MAC);
            hasPrevious = false;
        }

        int vw = mc.getWindow().getFramebufferWidth(), vh = mc.getWindow().getFramebufferHeight();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        var oldSort = RenderSystem.getVertexSorting();

        if (hasPrevious) {
            main.beginWrite(true);
            drawFrame(previous, vw, vh, Math.round(amount.value * 255f));
        }
        // store the blended result for the next frame
        previous.beginWrite(true);
        drawFrame(main, vw, vh, 255);
        main.beginWrite(true);
        hasPrevious = true;

        RenderSystem.setProjectionMatrix(oldProj, oldSort);
    }

    private static void drawFrame(Framebuffer src, int vw, int vh, int alpha) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f ortho = new Matrix4f().setOrtho(0.0f, vw, vh, 0.0f, 1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix(ortho, com.mojang.blaze3d.systems.VertexSorter.BY_Z);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        mv.translate(0.0f, 0.0f, -2000.0f);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, src.getColorAttachment());
        float u = (float) src.viewportWidth / (float) src.textureWidth;
        float v = (float) src.viewportHeight / (float) src.textureHeight;
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(0.0f, vh, 0.0f).texture(0.0f, 0.0f).color(255, 255, 255, alpha);
        b.vertex(vw, vh, 0.0f).texture(u, 0.0f).color(255, 255, 255, alpha);
        b.vertex(vw, 0.0f, 0.0f).texture(u, v).color(255, 255, 255, alpha);
        b.vertex(0.0f, 0.0f, 0.0f).texture(0.0f, v).color(255, 255, 255, alpha);
        BufferRenderer.drawWithGlobalProgram(b.end());

        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
