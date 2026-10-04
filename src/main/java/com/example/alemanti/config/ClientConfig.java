package com.example.alemanti.config;

import com.example.alemanti.module.modules.HudSettings;

/** Thin read-only facade; every option now lives on a Module and is saved in alemanti-modules.json. */
public class ClientConfig {
    public boolean isHudTextShadow() { return HudSettings.INSTANCE.shadow.value; }
    public boolean isHudBackground() { return HudSettings.INSTANCE.background.value; }
}
