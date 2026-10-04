package com.example.alemanti.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A persisted, GUI-editable option belonging to a {@link Module}. */
public abstract class Setting {
    public final String name;

    protected Setting(String name) { this.name = name; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement e);
    public abstract String display();

    public static final class Bool extends Setting {
        public boolean value;
        public Bool(String name, boolean def) { super(name); value = def; }
        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) { value = e.getAsBoolean(); }
        @Override public String display() { return value ? "ON" : "OFF"; }
    }

    public static final class Num extends Setting {
        public float value;
        public final float min, max, step;
        public Num(String name, float def, float min, float max, float step) {
            super(name);
            this.min = min; this.max = max; this.step = step;
            set(def);
        }
        public void set(float v) {
            float snapped = Math.round((v - min) / step) * step + min;
            value = Math.max(min, Math.min(max, snapped));
        }
        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) { set(e.getAsFloat()); }
        @Override public String display() { return String.format("%.2f", value); }
    }

    public static final class Choice<E extends Enum<E>> extends Setting {
        public E value;
        private final E[] all;
        public Choice(String name, E def) {
            super(name);
            value = def;
            all = def.getDeclaringClass().getEnumConstants();
        }
        public void next() { value = all[(value.ordinal() + 1) % all.length]; }
        @Override public JsonElement toJson() { return new JsonPrimitive(value.name()); }
        @Override public void fromJson(JsonElement e) {
            for (E c : all) if (c.name().equalsIgnoreCase(e.getAsString())) value = c;
        }
        @Override public String display() { return value.name(); }
    }

    public static final class Str extends Setting {
        public String value;
        public Str(String name, String def) { super(name); value = def; }
        @Override public JsonElement toJson() { return new JsonPrimitive(value); }
        @Override public void fromJson(JsonElement e) { value = e.getAsString(); }
        @Override public String display() { return value; }
    }
}
