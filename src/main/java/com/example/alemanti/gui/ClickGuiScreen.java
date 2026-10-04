package com.example.alemanti.gui;

import com.example.alemanti.AlemantiClient;
import com.example.alemanti.hud.HudEditorScreen;
import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Module browser: category tabs, a card grid (left-click toggles, right-click opens settings). */
public class ClickGuiScreen extends Screen {
    private static final Identifier LOGO = Identifier.of(AlemantiClient.MOD_ID, "textures/gui/logo.png");
    private static final String CREDIT = "Developed by " + AlemantiClient.DEVELOPER;
    private static final int SIDEBAR_W = 96, CARD_H = 44, GAP = 6, SETTINGS_W = 190, MIN_CARD_W = 118;

    private Category tab;          // null = all
    private Module selected;
    private int scroll;
    private Setting.Num dragging;
    private TextFieldWidget field;
    private int px, py, pw, ph;

    public ClickGuiScreen() { super(Text.literal("Alemanti Client")); }

    // ---------- layout ----------
    private int gridX() { return px + SIDEBAR_W + 8; }
    private int gridY() { return py + 48; }
    private int gridH() { return ph - 48 - 26; }
    private int gridW() { return pw - SIDEBAR_W - 16 - (selected != null ? SETTINGS_W + 8 : 0); }
    private int cols() { return Math.max(1, (gridW() + GAP) / (MIN_CARD_W + GAP)); }
    private int cardW() { int c = cols(); return (gridW() - (c - 1) * GAP) / c; }
    private int paneX() { return px + pw - SETTINGS_W - 8; }
    private int rowY(int i) { return py + 92 + i * 34; }

    private List<Module> visible() {
        List<Module> out = new ArrayList<>();
        for (Module m : AlemantiClient.MODULES.getModules()) if (tab == null || m.category == tab) out.add(m);
        return out;
    }

    private int maxScroll() {
        int rows = (visible().size() + cols() - 1) / cols();
        return Math.max(0, rows * (CARD_H + GAP) - gridH());
    }

    @Override
    protected void init() {
        pw = Math.min(640, width - 16);
        ph = Math.min(380, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        field = null;

        addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD"), b -> client.setScreen(new HudEditorScreen(this)))
                .dimensions(px + pw - 110, py + 9, 100, 22).build());

        if (selected != null) {
            int i = 0;
            for (Setting s : selected.settings()) {
                if (s instanceof Setting.Str str) {
                    field = new TextFieldWidget(textRenderer, paneX() + 8, rowY(i) + 11, SETTINGS_W - 16, 16, Text.literal(str.name));
                    field.setMaxLength(16);
                    field.setText(str.value);
                    field.setChangedListener(v -> str.value = v);
                    addDrawableChild(field);
                }
                i++;
            }
        }
    }

    @Override public void renderBackground(DrawContext c, int mx, int my, float delta) {}

    // ---------- render ----------
    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, 0x66000000);
        c.fill(px, py, px + pw, py + ph, 0xEE101216);
        c.fill(px, py, px + pw, py + 40, 0xFF1C2028);
        c.drawTexture(LOGO, px + 8, py + 6, 28, 28, 0f, 0f, 512, 512, 512, 512);
        c.drawTextWithShadow(textRenderer, "Alemanti Client", px + 44, py + 8, 0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer, CREDIT, px + 44, py + 22, 0xFFFF5A4D);

        // sidebar
        c.fill(px, py + 40, px + SIDEBAR_W, py + ph, 0xFF151922);
        for (int i = 0; i <= Category.values().length; i++) {
            Category cat = i == 0 ? null : Category.values()[i - 1];
            int ty = py + 52 + i * 26;
            boolean on = cat == tab;
            c.fill(px + 6, ty, px + SIDEBAR_W - 6, ty + 22, on ? 0xFF334B68 : 0xFF1C2028);
            c.drawTextWithShadow(textRenderer, cat == null ? "All" : cat.label, px + 14, ty + 7, on ? 0xFFBDE3FF : 0xFFD0D4DA);
        }

        // card grid
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        List<Module> mods = visible();
        int cw = cardW(), cols = cols();
        c.enableScissor(gridX(), gridY(), gridX() + gridW(), gridY() + gridH());
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = gridX() + (i % cols) * (cw + GAP);
            int y = gridY() + (i / cols) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < gridY() || y > gridY() + gridH()) continue;
            boolean on = m.enabled && m.toggleable();
            c.fill(x, y, x + cw, y + CARD_H, on ? 0xFF2A3F57 : 0xFF1B1F27);
            if (m == selected) outline(c, x, y, cw, CARD_H, 0xFF7DD3FC);
            c.drawTextWithShadow(textRenderer, m.name, x + 8, y + 8, 0xFFFFFFFF);
            c.drawText(textRenderer, textRenderer.trimToWidth(m.description, cw - 16), x + 8, y + 24, 0xFF8B93A1, false);
            if (m.toggleable()) {
                c.drawTextWithShadow(textRenderer, on ? "ON" : "OFF", x + cw - 24, y + 8, on ? 0xFF7DD3FC : 0xFF7B818C);
            }
            if (!m.settings().isEmpty()) c.drawText(textRenderer, ">", x + cw - 12, y + CARD_H - 12, 0xFF7DD3FC, false);
        }
        c.disableScissor();

        if (selected != null) renderPane(c, mouseX, mouseY);

        c.drawText(textRenderer, "Click: toggle  |  Right-click: settings  |  Right Shift: close",
                gridX(), py + ph - 16, 0xFF808894, false);
        super.render(c, mouseX, mouseY, delta);
    }

    private void renderPane(DrawContext c, int mx, int my) {
        int x = paneX();
        c.fill(x, py + 48, x + SETTINGS_W, py + 48 + gridH(), 0xFF171B22);
        c.drawTextWithShadow(textRenderer, selected.name, x + 8, py + 54, 0xFFFFFFFF);
        c.drawText(textRenderer, textRenderer.trimToWidth(selected.description, SETTINGS_W - 16), x + 8, py + 68, 0xFF8B93A1, false);
        if (selected.settings().isEmpty()) {
            c.drawText(textRenderer, "No settings", x + 8, rowY(0), 0xFF808894, false);
            return;
        }
        int i = 0;
        for (Setting s : selected.settings()) {
            int y = rowY(i++);
            int w = SETTINGS_W - 16;
            if (s instanceof Setting.Bool b) {
                c.fill(x + 8, y, x + 8 + w, y + 24, b.value ? 0xFF334B68 : 0xFF242932);
                c.drawTextWithShadow(textRenderer, b.name, x + 14, y + 8, 0xFFE5E7EB);
                c.drawTextWithShadow(textRenderer, b.display(), x + 8 + w - 26, y + 8, b.value ? 0xFF7DD3FC : 0xFF7B818C);
            } else if (s instanceof Setting.Choice<?> ch) {
                c.fill(x + 8, y, x + 8 + w, y + 24, 0xFF242932);
                c.drawTextWithShadow(textRenderer, ch.name + ": " + ch.display(), x + 14, y + 8, 0xFFE5E7EB);
            } else if (s instanceof Setting.Num n) {
                c.drawTextWithShadow(textRenderer, n.name + "  " + n.display(), x + 8, y, 0xFFE5E7EB);
                c.fill(x + 8, y + 14, x + 8 + w, y + 18, 0xFF343A45);
                int knob = x + 8 + (int) ((n.value - n.min) / (n.max - n.min) * w);
                c.fill(knob - 3, y + 10, knob + 3, y + 22, 0xFF7DD3FC);
            } else if (s instanceof Setting.Str str) {
                c.drawTextWithShadow(textRenderer, str.name, x + 8, y, 0xFFE5E7EB);
            }
        }
    }

    private void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;

        // tabs
        if (button == 0 && mx >= px + 6 && mx <= px + SIDEBAR_W - 6) {
            for (int i = 0; i <= Category.values().length; i++) {
                int ty = py + 52 + i * 26;
                if (my >= ty && my <= ty + 22) {
                    tab = i == 0 ? null : Category.values()[i - 1];
                    scroll = 0;
                    return true;
                }
            }
        }

        // cards
        if (mx >= gridX() && mx <= gridX() + gridW() && my >= gridY() && my <= gridY() + gridH()) {
            List<Module> mods = visible();
            int cw = cardW(), cols = cols();
            for (int i = 0; i < mods.size(); i++) {
                int x = gridX() + (i % cols) * (cw + GAP);
                int y = gridY() + (i / cols) * (CARD_H + GAP) - scroll;
                if (mx >= x && mx <= x + cw && my >= y && my <= y + CARD_H) {
                    Module m = mods.get(i);
                    if (button == 1 || (button == 0 && !m.toggleable())) {
                        selected = selected == m ? null : m;
                        clearAndInit();
                    } else if (button == 0) {
                        m.toggle();
                        AlemantiClient.saveAll();
                    }
                    return true;
                }
            }
        }

        // settings pane
        if (selected != null && button == 0) {
            int x = paneX(), w = SETTINGS_W - 16;
            int i = 0;
            for (Setting s : selected.settings()) {
                int y = rowY(i++);
                if (mx < x + 8 || mx > x + 8 + w) continue;
                if (s instanceof Setting.Bool b && my >= y && my <= y + 24) {
                    b.value = !b.value;
                    AlemantiClient.saveAll();
                    return true;
                } else if (s instanceof Setting.Choice<?> ch && my >= y && my <= y + 24) {
                    ch.next();
                    AlemantiClient.saveAll();
                    return true;
                } else if (s instanceof Setting.Num n && my >= y && my <= y + 26) {
                    dragging = n;
                    setFromMouse(n, mx);
                    return true;
                }
            }
        }
        return true;
    }

    private void setFromMouse(Setting.Num n, double mx) {
        double t = (mx - (paneX() + 8)) / (SETTINGS_W - 16);
        n.set((float) (n.min + Math.max(0, Math.min(1, t)) * (n.max - n.min)));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && button == 0) {
            setFromMouse(dragging, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            AlemantiClient.saveAll();
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (v * 20)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = field != null && field.isFocused();
        if (!typing && keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void removed() { AlemantiClient.saveAll(); }
    @Override public boolean shouldPause() { return false; }
}
