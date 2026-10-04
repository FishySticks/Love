package com.example.alemanti.hud;

/** Kept for compatibility: the HUD is now a set of draggable elements managed by {@link HudManager}. */
public final class HudRenderer {
    private HudRenderer() {}

    public static void register() {
        HudManager.register();
    }
}
