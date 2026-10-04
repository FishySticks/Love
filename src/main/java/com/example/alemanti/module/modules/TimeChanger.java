package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;
import net.minecraft.client.MinecraftClient;

/** Client-side only: the server's real time is unchanged. */
public class TimeChanger extends Module {
    public static final TimeChanger INSTANCE = new TimeChanger();
    public final Setting.Num time = add(new Setting.Num("Time of day", 6000f, 0f, 24000f, 500f));

    private TimeChanger() {
        super("Time Changer", "Locks the world time (client-side only)", Category.VISUAL, false);
    }

    @Override
    public void tick(MinecraftClient client) {
        if (client.world != null) client.world.setTimeOfDay((long) time.value);
    }
}
