package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Replaces the vanilla damage camera tilt (called from GameRendererMixin). */
public class HurtCam extends Module {
    public enum Mode { OFF, OLD, NEW }

    public static final HurtCam INSTANCE = new HurtCam();
    public final Setting.Choice<Mode> mode = add(new Setting.Choice<>("Mode", Mode.NEW));
    public final Setting.Num sensitivity = add(new Setting.Num("Sensitivity", 1.0f, 0.0f, 2.0f, 0.1f));

    private HurtCam() {
        super("HurtCam", "OFF / OLD (plain roll) / NEW (directional) damage tilt", Category.PVP, true);
    }

    public void apply(MinecraftClient mc, MatrixStack m, float tickDelta) {
        if (!(mc.getCameraEntity() instanceof LivingEntity le)) return;
        if (le.isDead()) {
            float g = Math.min(le.deathTime + tickDelta, 20.0f);
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(40.0f - 8000.0f / (g + 200.0f)));
        }
        float f = le.hurtTime - tickDelta;
        if (f < 0.0f || mode.value == Mode.OFF || le.maxHurtTime <= 0) return;
        f /= le.maxHurtTime;
        f = MathHelper.sin(f * f * f * f * (float) Math.PI);
        float strength = (float) (sensitivity.value * mc.options.getDamageTiltStrength().getValue());
        float roll = -f * 14.0f * strength;
        if (mode.value == Mode.NEW) {
            float yaw = le.getDamageTiltYaw();
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        } else {
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        }
    }
}
