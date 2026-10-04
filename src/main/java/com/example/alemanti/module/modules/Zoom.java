package com.example.alemanti.module.modules;

import com.example.alemanti.AlemantiClient;
import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;

public class Zoom extends Module {
    public static final Zoom INSTANCE = new Zoom();
    public final Setting.Num level = add(new Setting.Num("Zoom level", 0.25f, 0.05f, 0.9f, 0.05f));
    private double current = 1.0;

    private Zoom() {
        super("Zoom", "Hold C to zoom in smoothly", Category.VISUAL, true);
    }

    /** FOV multiplier for this frame (1.0 = no zoom). */
    public double fovMultiplier() {
        boolean held = enabled && AlemantiClient.zoomKey != null && AlemantiClient.zoomKey.isPressed();
        double target = held ? level.value : 1.0;
        current += (target - current) * 0.25;
        if (Math.abs(current - target) < 0.001) current = target;
        return current;
    }
}
