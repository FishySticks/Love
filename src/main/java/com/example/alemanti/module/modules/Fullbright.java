package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;

/** Effect is applied by SimpleOptionMixin (gamma override). */
public class Fullbright extends Module {
    public static final Fullbright INSTANCE = new Fullbright();

    private Fullbright() {
        super("Fullbright", "Maximum brightness everywhere", Category.VISUAL, false);
    }
}
