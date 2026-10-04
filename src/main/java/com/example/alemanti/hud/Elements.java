package com.example.alemanti.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;

import net.minecraft.entity.player.PlayerInventory;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** All built-in HUD widgets. */
public final class Elements {
    private Elements() {}

    /** Simple one-line boxed text widget. */
    abstract static class TextElement extends HudElement {
        TextElement(String id, String name, String desc, boolean on) { super(id, name, desc, on); }
        abstract String line(MinecraftClient mc);

        @Override public int width(MinecraftClient mc) { return Math.max(60, mc.textRenderer.getWidth(line(mc)) + 10); }
        @Override public int height(MinecraftClient mc) { return 17; }
        @Override protected void draw(DrawContext c, MinecraftClient mc, boolean editing) {
            box(c, width(mc), 17);
            text(c, mc, line(mc), 5, 4, 0xFFFFFFFF);
        }
    }

    public static final class Fps extends TextElement {
        public Fps() { super("fps", "FPS", "Frames per second", true); }
        @Override String line(MinecraftClient mc) { return "FPS: " + mc.getCurrentFps(); }
        @Override protected void defaultPosition(int sw, int sh) { x = 6; y = 6; }
    }

    public static final class Cps extends TextElement {
        public Cps() { super("cps", "CPS", "Clicks per second (left | right)", true); }
        @Override String line(MinecraftClient mc) { return "CPS: " + HudManager.leftCps() + " | " + HudManager.rightCps(); }
        @Override protected void defaultPosition(int sw, int sh) { x = 6; y = 25; }
    }

    public static final class Ping extends TextElement {
        public Ping() { super("ping", "Ping", "Your latency to the server", true); }
        @Override String line(MinecraftClient mc) {
            int ping = 0;
            if (mc.player != null && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            return "Ping: " + ping + " ms";
        }
        @Override protected void defaultPosition(int sw, int sh) { x = 6; y = 44; }
    }

    public static final class Coords extends TextElement {
        public Coords() { super("coords", "Coordinates", "Your XYZ position", true); }
        @Override String line(MinecraftClient mc) {
            if (mc.player == null) return "XYZ: 0 64 0";
            return "XYZ: " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        }
        @Override protected void defaultPosition(int sw, int sh) { x = 6; y = 63; }
    }

    public static final class Armor extends HudElement {
        private static final EquipmentSlot[] SLOTS =
                {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        public Armor() { super("armor", "Armor Status", "Armor pieces and durability", true); }
        @Override public int width(MinecraftClient mc) { return 56; }
        @Override public int height(MinecraftClient mc) { return 72; }

        @Override protected void draw(DrawContext c, MinecraftClient mc, boolean editing) {
            ItemStack[] stacks = new ItemStack[4];
            boolean any = false;
            for (int i = 0; i < 4; i++) {
                stacks[i] = mc.player == null ? ItemStack.EMPTY : mc.player.getEquippedStack(SLOTS[i]);
                if (!stacks[i].isEmpty()) any = true;
            }
            if (!any && editing) {
                stacks[0] = new ItemStack(Items.DIAMOND_HELMET);
                stacks[1] = new ItemStack(Items.DIAMOND_CHESTPLATE);
                stacks[2] = new ItemStack(Items.DIAMOND_LEGGINGS);
                stacks[3] = new ItemStack(Items.DIAMOND_BOOTS);
            }
            box(c, 56, 72);
            for (int i = 0; i < 4; i++) {
                ItemStack s = stacks[i];
                if (s.isEmpty()) continue;
                int ry = i * 18;
                c.drawItem(s, 3, ry + 1);
                c.drawItemInSlot(mc.textRenderer, s, 3, ry + 1);
                if (s.isDamageable()) {
                    int max = s.getMaxDamage();
                    int pct = Math.max(0, (max - s.getDamage()) * 100 / max);
                    int col = pct > 60 ? 0xFF7CF29A : pct > 30 ? 0xFFFFE066 : 0xFFFF6B6B;
                    text(c, mc, pct + "%", 24, ry + 5, col);
                }
            }
        }
        @Override protected void defaultPosition(int sw, int sh) { x = 6; y = 86; }
    }

    public static final class Potions extends HudElement {
        record Row(String text, int color) {}
        public Potions() { super("potions", "Potion Status", "Active effects and timers", true); }

        private List<Row> rows(MinecraftClient mc) {
            List<Row> rows = new ArrayList<>();
            if (mc.player != null) {
                for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                    String name = e.getEffectType().value().getName().getString();
                    if (e.getAmplifier() > 0) name += " " + roman(e.getAmplifier() + 1);
                    rows.add(new Row(name + "  " + time(e), color(e.getEffectType())));
                }
            }
            if (rows.isEmpty() && mc.currentScreen instanceof HudEditorScreen) {
                rows.add(new Row("Speed II  1:24", 0xFF55B8FF));
                rows.add(new Row("Strength II  1:24", 0xFFFF8A3D));
                rows.add(new Row("Absorption  0:48", 0xFFFFD54A));
            }
            return rows;
        }

        @Override public int width(MinecraftClient mc) {
            int w = 90;
            for (Row r : rows(mc)) w = Math.max(w, mc.textRenderer.getWidth(r.text()) + 10);
            return w;
        }
        @Override public int height(MinecraftClient mc) { return Math.max(1, rows(mc).size()) * 17; }

        @Override protected void draw(DrawContext c, MinecraftClient mc, boolean editing) {
            List<Row> rows = rows(mc);
            if (rows.isEmpty()) return;
            box(c, width(mc), rows.size() * 17);
            for (int i = 0; i < rows.size(); i++) text(c, mc, rows.get(i).text(), 5, i * 17 + 4, rows.get(i).color());
        }
        @Override protected void defaultPosition(int sw, int sh) { x = sw - width(MinecraftClient.getInstance()) - 6; y = 6; }

        private static String time(StatusEffectInstance e) {
            if (e.isInfinite()) return "inf";
            int s = e.getDuration() / 20;
            return (s / 60) + ":" + String.format("%02d", s % 60);
        }
        private static String roman(int n) {
            String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
            return n > 0 && n < r.length ? r[n] : String.valueOf(n);
        }
        private static int color(RegistryEntry<StatusEffect> effect) {
            String id = effect.getKey().map(k -> k.getValue().getPath()).orElse("");
            return switch (id) {
                case "speed" -> 0xFF55B8FF;
                case "strength" -> 0xFFFF8A3D;
                case "fire_resistance" -> 0xFFFFB347;
                case "regeneration" -> 0xFFFF69B4;
                case "absorption" -> 0xFFFFD54A;
                case "resistance" -> 0xFFB0B7C3;
                case "jump_boost" -> 0xFF7CF29A;
                case "haste" -> 0xFFFFE066;
                default -> 0xFFE5E7EB;
            };
        }
    }

    public static final class Keystrokes extends HudElement {
        public Keystrokes() { super("keystrokes", "Keystrokes", "WASD, mouse buttons and jump", true); }
        @Override public int width(MinecraftClient mc) { return 70; }
        @Override public int height(MinecraftClient mc) { return 86; }

        @Override protected void draw(DrawContext c, MinecraftClient mc, boolean editing) {
            var o = mc.options;
            key(c, mc, 24, 0, 22, 22, "W", o.forwardKey.isPressed());
            key(c, mc, 0, 24, 22, 22, "A", o.leftKey.isPressed());
            key(c, mc, 24, 24, 22, 22, "S", o.backKey.isPressed());
            key(c, mc, 48, 24, 22, 22, "D", o.rightKey.isPressed());
            key(c, mc, 0, 48, 34, 22, "LMB", o.attackKey.isPressed());
            key(c, mc, 36, 48, 34, 22, "RMB", o.useKey.isPressed());
            key(c, mc, 0, 72, 70, 14, "---", o.jumpKey.isPressed());
        }

        private void key(DrawContext c, MinecraftClient mc, int x, int y, int w, int h, String label, boolean down) {
            c.fill(x, y, x + w, y + h, down ? 0xAAFFFFFF : 0x78000000);
            int tw = mc.textRenderer.getWidth(label);
            c.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2,
                    down ? 0xFF111111 : 0xFFFFFFFF, !down);
        }
        @Override protected void defaultPosition(int sw, int sh) { x = 6; y = Math.max(160, sh - 96); }
    }

    // ---- extra widgets (off by default) ----
    private static void rightColumn(HudElement e, int sw, int index) {
        MinecraftClient mc = MinecraftClient.getInstance();
        e.x = sw - e.width(mc) - 6;
        e.y = 120 + index * 19;
    }

    public static final class Clock extends TextElement {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
        public Clock() { super("clock", "Clock", "Your local time", false); }
        @Override String line(MinecraftClient mc) { return LocalTime.now().format(FMT); }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 0); }
    }

    public static final class Memory extends TextElement {
        public Memory() { super("memory", "Memory", "Used / max game memory", false); }
        @Override String line(MinecraftClient mc) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) / 1048576L;
            return "Mem: " + used + "/" + (r.maxMemory() / 1048576L) + " MB";
        }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 1); }
    }

    public static final class Direction extends TextElement {
        private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        public Direction() { super("direction", "Direction", "Compass heading", false); }
        @Override String line(MinecraftClient mc) {
            if (mc.player == null) return "Facing: N";
            int i = Math.floorMod(Math.round(mc.player.getYaw() / 45f), 8);
            return "Facing: " + DIRS[i];
        }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 2); }
    }

    public static final class Speed extends TextElement {
        public Speed() { super("speed", "Speed", "Horizontal speed in blocks per second", false); }
        @Override String line(MinecraftClient mc) {
            if (mc.player == null) return "Speed: 0.0 b/s";
            double d = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ) * 20.0;
            return String.format("Speed: %.1f b/s", d);
        }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 3); }
    }

    public static final class Server extends TextElement {
        public Server() { super("server", "Server", "Address of the server you are on", false); }
        @Override String line(MinecraftClient mc) {
            var info = mc.getCurrentServerEntry();
            return info != null ? info.address : "Singleplayer";
        }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 4); }
    }

    public static final class Combo extends TextElement {
        public Combo() { super("combo", "Combo Counter", "Consecutive hits without being hit", false); }
        @Override String line(MinecraftClient mc) { return "Combo: " + HudManager.combo(); }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 5); }
    }

    public static final class Reach extends TextElement {
        public Reach() { super("reach", "Reach Display", "Distance of your last hit", false); }
        @Override String line(MinecraftClient mc) {
            double r = HudManager.lastReach();
            return r < 0 ? "Reach: -" : String.format("Reach: %.2f", r);
        }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 6); }
    }

    public static final class Pots extends TextElement {
        public Pots() { super("pots", "Pot Counter", "Splash potions in your inventory", false); }
        @Override String line(MinecraftClient mc) {
            int n = 0;
            if (mc.player != null) {
                PlayerInventory inv = mc.player.getInventory();
                for (int i = 0; i < 36; i++) {
                    ItemStack s = inv.getStack(i);
                    if (s.isOf(Items.SPLASH_POTION)) n += s.getCount();
                }
            }
            return "Pots: " + n;
        }
        @Override protected void defaultPosition(int sw, int sh) { rightColumn(this, sw, 7); }
    }
}
