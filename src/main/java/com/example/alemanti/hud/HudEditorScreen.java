package com.example.alemanti.hud;

import com.example.alemanti.AlemantiClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Lunar-style HUD editor: drag to move, right-click to toggle, scroll to resize. */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 6;
    private final Screen parent;
    private HudElement dragging;
    private int dragDX, dragDY;
    private boolean guideV, guideH;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset Layout"), b -> HudManager.resetAll(width, height))
                .dimensions(width / 2 - 104, height - 30, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 + 4, height - 30, 100, 20).build());
    }

    @Override public void renderBackground(DrawContext c, int mx, int my, float delta) {}

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, 0x66000000);
        if (dragging != null) {
            if (guideV) c.fill(width / 2, 0, width / 2 + 1, height, 0x887DD3FC);
            if (guideH) c.fill(0, height / 2, width, height / 2 + 1, 0x887DD3FC);
        }

        HudElement hover = elementAt(mouseX, mouseY);
        for (HudElement e : HudManager.ELEMENTS) {
            e.ensurePositioned(width, height);
            HudManager.clamp(e, client, width, height);
            int w = e.scaledWidth(client), h = e.scaledHeight(client);
            if (e.enabled) {
                e.render(c, client, true);
            } else {
                c.fill(e.x, e.y, e.x + w, e.y + h, 0x40FFFFFF);
                c.drawText(textRenderer, e.name, e.x + 3, e.y + Math.max(0, (h - 8) / 2), 0xFF9FA7B5, false);
            }
            boolean active = e == hover || e == dragging;
            outline(c, e.x, e.y, w, h, active ? 0xFF7DD3FC : e.enabled ? 0x55FFFFFF : 0x557B818C);
        }

        if (hover != null) {
            String label = hover.name + " • " + Math.round(hover.scale * 100) + "%" + (hover.enabled ? "" : " • OFF");
            c.drawTextWithShadow(textRenderer, label, hover.x, Math.max(2, hover.y - 11), 0xFFFFFFFF);
        }

        c.drawCenteredTextWithShadow(textRenderer, "HUD Editor", width / 2, 12, 0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer,
                "Drag to move • Right-click to toggle • Scroll to resize • Esc to save", width / 2, 26, 0xFF9FA7B5);
        c.drawCenteredTextWithShadow(textRenderer,
                "Developed by " + AlemantiClient.DEVELOPER, width / 2, 40, 0xFFFF5A4D);

        super.render(c, mouseX, mouseY, delta);
    }

    private void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x - 1, y - 1, x + w + 1, y, col);
        c.fill(x - 1, y + h, x + w + 1, y + h + 1, col);
        c.fill(x - 1, y, x, y + h, col);
        c.fill(x + w, y, x + w + 1, y + h, col);
    }

    private HudElement elementAt(double mx, double my) {
        for (int i = HudManager.ELEMENTS.size() - 1; i >= 0; i--) {
            HudElement e = HudManager.ELEMENTS.get(i);
            if (mx >= e.x && mx <= e.x + e.scaledWidth(client) && my >= e.y && my <= e.y + e.scaledHeight(client)) return e;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        HudElement e = elementAt(mx, my);
        if (e == null) return false;
        if (button == 0) {
            dragging = e;
            dragDX = (int) mx - e.x;
            dragDY = (int) my - e.y;
        } else if (button == 1) {
            e.enabled = !e.enabled;
            HudManager.save();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == null || button != 0) return super.mouseDragged(mx, my, button, dx, dy);
        int w = dragging.scaledWidth(client), h = dragging.scaledHeight(client);
        int nx = (int) mx - dragDX, ny = (int) my - dragDY;

        guideV = Math.abs(nx + w / 2 - width / 2) < SNAP;
        guideH = Math.abs(ny + h / 2 - height / 2) < SNAP;
        if (guideV) nx = width / 2 - w / 2;
        else if (Math.abs(nx) < SNAP) nx = 0;
        else if (Math.abs(nx + w - width) < SNAP) nx = width - w;
        if (guideH) ny = height / 2 - h / 2;
        else if (Math.abs(ny) < SNAP) ny = 0;
        else if (Math.abs(ny + h - height) < SNAP) ny = height - h;

        dragging.x = nx;
        dragging.y = ny;
        HudManager.clamp(dragging, client, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            guideV = guideH = false;
            HudManager.save();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        HudElement e = elementAt(mx, my);
        if (e == null) return super.mouseScrolled(mx, my, horizontal, vertical);
        float s = e.scale + (vertical > 0 ? 0.1f : -0.1f);
        e.scale = Math.round(Math.max(0.5f, Math.min(2.0f, s)) * 10f) / 10f;
        HudManager.clamp(e, client, width, height);
        HudManager.save();
        return true;
    }

    @Override
    public void close() {
        HudManager.save();
        client.setScreen(parent);
    }

    @Override public boolean shouldPause() { return false; }
}
