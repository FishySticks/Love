package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import net.minecraft.client.MinecraftClient;

public class ToggleSprint extends Module {
    public static final ToggleSprint INSTANCE = new ToggleSprint();
    private boolean forced;

    private ToggleSprint() {
        super("Toggle Sprint", "Sprint automatically while moving forward", Category.MOVEMENT, true);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.options.forwardKey.isPressed()) {
            client.options.sprintKey.setPressed(true);
            forced = true;
        } else if (forced) {
            client.options.sprintKey.setPressed(false);
            forced = false;
        }
    }

    @Override
    protected void onDisable() {
        if (forced) {
            MinecraftClient.getInstance().options.sprintKey.setPressed(false);
            forced = false;
        }
    }
}
