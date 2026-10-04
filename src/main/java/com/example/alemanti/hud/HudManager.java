package com.example.alemanti.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Owns every HUD element: rendering, click tracking and layout persistence. */
public final class HudManager {
    private HudManager() {}

    public static final List<HudElement> ELEMENTS = List.of(
            new Elements.Fps(), new Elements.Cps(), new Elements.Ping(),
            new Elements.Coords(), new Elements.Armor(), new Elements.Potions(), new Elements.Keystrokes(),
            new Elements.Clock(), new Elements.Memory(), new Elements.Direction(), new Elements.Speed(),
            new Elements.Server(), new Elements.Combo(), new Elements.Reach(), new Elements.Pots());

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private static boolean lastAttack, lastUse;

    public static void register() {
        load();
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
        ClientTickEvents.END_CLIENT_TICK.register(HudManager::tick);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient && entity instanceof LivingEntity && player == MinecraftClient.getInstance().player) {
                onAttack(player.getEyePos(), entity.getBoundingBox());
            }
            return ActionResult.PASS;
        });
    }

    // ---- combo + reach ----
    private static int combo;
    private static long lastHit;
    private static int lastHurtTime;
    private static double lastReach = -1;

    private static void onAttack(Vec3d eye, Box b) {
        double dx = Math.max(Math.max(b.minX - eye.x, 0), eye.x - b.maxX);
        double dy = Math.max(Math.max(b.minY - eye.y, 0), eye.y - b.maxY);
        double dz = Math.max(Math.max(b.minZ - eye.z, 0), eye.z - b.maxZ);
        lastReach = Math.sqrt(dx * dx + dy * dy + dz * dz);
        long now = System.currentTimeMillis();
        if (now - lastHit > 2000) combo = 0;
        combo++;
        lastHit = now;
    }

    public static int combo() {
        if (System.currentTimeMillis() - lastHit > 2000) combo = 0;
        return combo;
    }

    public static double lastReach() { return lastReach; }

    public static HudElement get(String id) {
        for (HudElement e : ELEMENTS) if (e.id.equals(id)) return e;
        throw new IllegalArgumentException(id);
    }

    private static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen instanceof HudEditorScreen) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        for (HudElement e : ELEMENTS) {
            if (!e.enabled) continue;
            e.ensurePositioned(sw, sh);
            clamp(e, mc, sw, sh);
            e.render(c, mc, false);
        }
    }

    public static void clamp(HudElement e, MinecraftClient mc, int sw, int sh) {
        e.x = Math.max(0, Math.min(e.x, sw - e.scaledWidth(mc)));
        e.y = Math.max(0, Math.min(e.y, sh - e.scaledHeight(mc)));
    }

    public static void resetAll(int sw, int sh) {
        for (HudElement e : ELEMENTS) e.resetLayout(sw, sh);
        save();
    }

    // ---- CPS (counts new presses once per tick) ----
    private static void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        boolean atk = mc.currentScreen == null && mc.options.attackKey.isPressed();
        boolean use = mc.currentScreen == null && mc.options.useKey.isPressed();
        if (atk && !lastAttack) LEFT.addLast(now);
        if (use && !lastUse) RIGHT.addLast(now);
        lastAttack = atk;
        lastUse = use;
        if (mc.player != null) {
            if (mc.player.hurtTime > lastHurtTime) combo = 0; // we just got hit
            lastHurtTime = mc.player.hurtTime;
        }
    }

    public static int leftCps() { return count(LEFT); }
    public static int rightCps() { return count(RIGHT); }

    private static int count(Deque<Long> d) {
        long now = System.currentTimeMillis();
        while (!d.isEmpty() && now - d.peekFirst() > 1000) d.pollFirst();
        return d.size();
    }

    // ---- persistence: <config>/alemanti-hud.json ----
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("alemanti-hud.json"); }

    public static void save() {
        JsonObject root = new JsonObject();
        for (HudElement e : ELEMENTS) {
            JsonObject o = new JsonObject();
            o.addProperty("x", e.x);
            o.addProperty("y", e.y);
            o.addProperty("scale", e.scale);
            o.addProperty("enabled", e.enabled);
            root.add(e.id, o);
        }
        try {
            Files.writeString(file(), GSON.toJson(root));
        } catch (IOException ex) {
            System.err.println("[Alemanti] Could not save HUD layout: " + ex);
        }
    }

    private static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (HudElement e : ELEMENTS) {
                if (!root.has(e.id)) continue;
                JsonObject o = root.getAsJsonObject(e.id);
                e.x = o.get("x").getAsInt();
                e.y = o.get("y").getAsInt();
                e.scale = Math.max(0.5f, Math.min(2.0f, o.get("scale").getAsFloat()));
                e.enabled = o.get("enabled").getAsBoolean();
                e.markPositioned();
            }
        } catch (IOException | RuntimeException ex) {
            System.err.println("[Alemanti] Could not read HUD layout: " + ex);
        }
    }
}
