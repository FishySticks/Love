package com.example.alemanti.module;

public enum Category {
    PVP("PvP"), VISUAL("Visual"), MOVEMENT("Movement"), HUD("HUD"), MISC("Misc");

    public final String label;

    Category(String label) { this.label = label; }
}
