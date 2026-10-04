package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;

/** Global look of every HUD widget. Settings-only panel. */
public class HudSettings extends Module {
    public static final HudSettings INSTANCE = new HudSettings();
    public final Setting.Bool shadow = add(new Setting.Bool("Text shadow", true));
    public final Setting.Bool background = add(new Setting.Bool("Backgrounds", true));

    private HudSettings() {
        super("HUD Style", "Text shadows and backgrounds for all widgets", Category.HUD, true);
    }

    @Override public boolean toggleable() { return false; }
}
