package com.example.alemanti.hud;

import com.example.alemanti.AlemantiClient;
import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

/** One draggable, scalable, toggleable HUD widget (Lunar-style). */
public abstract class HudElement extends Module {
    public final String id;

    public int x, y;
    public float scale = 1.0f;
    private boolean positioned;

    protected HudElement(String id, String name, String description, boolean enabledByDefault) {
        super(name, description, Category.HUD, enabledByDefault);
        this.id = id;
    }

    public abstract int width(MinecraftClient mc);
    public abstract int height(MinecraftClient mc);
    protected abstract void draw(DrawContext c, MinecraftClient mc, boolean editing);
    protected abstract void defaultPosition(int sw, int sh);

    public int scaledWidth(MinecraftClient mc) { return Math.round(width(mc) * scale); }
    public int scaledHeight(MinecraftClient mc) { return Math.round(height(mc) * scale); }

    public void ensurePositioned(int sw, int sh) {
        if (!positioned) {
            defaultPosition(sw, sh);
            positioned = true;
        }
    }

    public void markPositioned() { positioned = true; }

    public void resetLayout(int sw, int sh) {
        scale = 1.0f;
        enabled = enabledByDefault;
        defaultPosition(sw, sh);
        positioned = true;
    }

    public void render(DrawContext c, MinecraftClient mc, boolean editing) {
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate((float) x, (float) y, 0f);
        m.scale(scale, scale, 1f);
        draw(c, mc, editing);
        m.pop();
    }

    protected void box(DrawContext c, int w, int h) {
        if (AlemantiClient.CONFIG.isHudBackground()) c.fill(0, 0, w, h, 0x78000000);
    }

    protected void text(DrawContext c, MinecraftClient mc, String s, int tx, int ty, int color) {
        c.drawText(mc.textRenderer, s, tx, ty, color, AlemantiClient.CONFIG.isHudTextShadow());
    }
}
