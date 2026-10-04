package com.example.alemanti.cosmetic;

import com.example.alemanti.AlemantiClient;

public class CosmeticManager {
    public boolean wingsEnabled() { return AlemantiClient.CONFIG.isWings(); }
    public boolean capeEnabled() { return AlemantiClient.CONFIG.isCape(); }

    public void toggleWings() {
        AlemantiClient.CONFIG.setWings(!AlemantiClient.CONFIG.isWings());
    }

    public void toggleCape() {
        AlemantiClient.CONFIG.setCape(!AlemantiClient.CONFIG.isCape());
    }

    public void setWings(boolean value) { AlemantiClient.CONFIG.setWings(value); }
    public void setCape(boolean value) { AlemantiClient.CONFIG.setCape(value); }
}
