package com.example.alemanti.module;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final boolean enabledByDefault;
    public boolean enabled;
    private final List<Setting> settings = new ArrayList<>();

    protected Module(String name, String description, Category category, boolean enabledByDefault) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabledByDefault = enabledByDefault;
        this.enabled = enabledByDefault;
    }

    protected <S extends Setting> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting> settings() { return settings; }
    /** False for panels that only hold settings (no on/off state). */
    public boolean toggleable() { return true; }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void tick(MinecraftClient client) {}
}
