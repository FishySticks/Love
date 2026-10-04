package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;

/** Own nametag + ping next to names. Rendering is done by the nametag mixins. */
public class Nametags extends Module {
    public static final Nametags INSTANCE = new Nametags();
    public final Setting.Bool own = add(new Setting.Bool("Show own nametag", true));
    public final Setting.Bool ping = add(new Setting.Bool("Show ping", true));

    private Nametags() {
        super("Nametags", "Own nametag and ping shown above heads", Category.VISUAL, true);
    }
}
