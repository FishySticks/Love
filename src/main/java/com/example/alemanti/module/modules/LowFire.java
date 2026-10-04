package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;

/** Effect is applied by InGameOverlayRendererMixin. */
public class LowFire extends Module {
    public static final LowFire INSTANCE = new LowFire();

    private LowFire() {
        super("Low Fire", "Lowers the fire overlay on your screen", Category.VISUAL, false);
    }
}
