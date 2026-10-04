package com.example.alemanti.module;

import com.example.alemanti.hud.HudElement;
import com.example.alemanti.hud.HudManager;
import com.example.alemanti.module.modules.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<Module> modules = new ArrayList<>();

    public void registerDefaults() {
        modules.add(Nametags.INSTANCE);
        modules.add(NickHider.INSTANCE);
        modules.add(HurtCam.INSTANCE);
        modules.add(Zoom.INSTANCE);
        modules.add(MotionBlur.INSTANCE);
        modules.add(Fullbright.INSTANCE);
        modules.add(LowFire.INSTANCE);
        modules.add(TimeChanger.INSTANCE);
        modules.add(ToggleSprint.INSTANCE);
        modules.addAll(HudManager.ELEMENTS);
        modules.add(HudSettings.INSTANCE);
    }

    public List<Module> getModules() { return modules; }

    public void tick(MinecraftClient client) {
        for (Module module : modules) {
            if (module.isEnabled()) module.tick(client);
        }
    }

    // ---- persistence: <config>/alemanti-modules.json (HUD layout lives in alemanti-hud.json) ----
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("alemanti-modules.json"); }

    public void save() {
        JsonObject root = new JsonObject();
        for (Module m : modules) {
            if (m instanceof HudElement) continue;
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled);
            JsonObject s = new JsonObject();
            for (Setting st : m.settings()) s.add(st.name, st.toJson());
            o.add("settings", s);
            root.add(m.name, o);
        }
        try {
            Files.writeString(file(), GSON.toJson(root));
        } catch (IOException ex) {
            System.err.println("[Alemanti] Could not save modules: " + ex);
        }
    }

    public void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (Module m : modules) {
                if (m instanceof HudElement || !root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
                if (!o.has("settings")) continue;
                JsonObject s = o.getAsJsonObject("settings");
                for (Setting st : m.settings()) {
                    if (!s.has(st.name)) continue;
                    try { st.fromJson(s.get(st.name)); } catch (RuntimeException ignored) {}
                }
            }
        } catch (IOException | RuntimeException ex) {
            System.err.println("[Alemanti] Could not read modules: " + ex);
        }
    }
}
