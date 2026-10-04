package com.example.alemanti;

import com.example.alemanti.config.ClientConfig;
import com.example.alemanti.cosmetic.CosmeticManager;
import com.example.alemanti.gui.ClickGuiScreen;
import com.example.alemanti.hud.HudEditorScreen;
import com.example.alemanti.hud.HudManager;
import com.example.alemanti.hud.HudRenderer;
import com.example.alemanti.module.ModuleManager;
import com.example.alemanti.mixin.EntityRendererMixin;
import com.example.alemanti.module.modules.MotionBlur;
import com.example.alemanti.module.modules.NickHider;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AlemantiClient implements ClientModInitializer {
    public static final String MOD_ID = "alemanti";
    public static final String DEVELOPER = "svbx";
    public static final ModuleManager MODULES = new ModuleManager();
    public static final CosmeticManager COSMETICS = new CosmeticManager();
    public static final ClientConfig CONFIG = new ClientConfig();

    /** Right Shift opens the client menu. */
    public static KeyBinding clickGuiKey;
    /** Right Ctrl opens the HUD editor. */
    public static KeyBinding hudEditorKey;
    /** Hold C to zoom. */
    public static KeyBinding zoomKey;

    public static void saveAll() {
        MODULES.save();
        HudManager.save();
    }

    @Override
    public void onInitializeClient() {
        MODULES.registerDefaults();
        MODULES.load();

        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.alemanti.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.alemanti"));
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.alemanti.hud_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, "category.alemanti"));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.alemanti.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.alemanti"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clickGuiKey.wasPressed()) client.setScreen(new ClickGuiScreen());
            while (hudEditorKey.wasPressed()) client.setScreen(new HudEditorScreen(null));
            MODULES.tick(client);
        });

        // Nick hider for game/system chat messages.
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) ->
                NickHider.INSTANCE.enabled ? NickHider.INSTANCE.apply(message) : message);

        MotionBlur.register();
        WorldRenderEvents.START.register(ctx -> EntityRendererMixin.alemanti$lastPingId = -1);

        HudRenderer.register();
    }
}
