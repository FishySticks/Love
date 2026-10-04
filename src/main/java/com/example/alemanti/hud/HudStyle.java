package com.example.alemanti.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

public final class HudStyle {
    private HudStyle() {}

    public static void background(DrawContext c, int x, int y, int w, int h, boolean enabled) {
        if (enabled) c.fill(x, y, x+w, y+h, 0xAA101216);
    }

    public static void text(DrawContext c, TextRenderer renderer, String text,
                            int x, int y, int color, boolean shadow) {
        c.drawText(renderer, text, x, y, color, shadow);
    }
}
