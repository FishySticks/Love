package com.example.alemanti.module.modules;

import com.example.alemanti.module.Category;
import com.example.alemanti.module.Module;
import com.example.alemanti.module.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

/** Replaces your own username in nametags, the tab list and game chat messages. */
public class NickHider extends Module {
    public static final NickHider INSTANCE = new NickHider();
    public final Setting.Str nick = add(new Setting.Str("Nickname", "You"));

    private NickHider() {
        super("Nick Hider", "Hides your real name from screenshots and streams", Category.MISC, false);
    }

    public static String ownName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc == null || mc.getSession() == null ? "" : mc.getSession().getUsername();
    }

    public Text apply(Text text) {
        String own = ownName();
        if (own.isEmpty()) return text;
        String to = nick.value.isEmpty() ? "You" : nick.value;
        return replace(text, own, to);
    }

    private static Text replace(Text t, String from, String to) {
        MutableText out;
        if (t.getContent() instanceof PlainTextContent.Literal lit) {
            out = Text.literal(lit.string().replace(from, to));
        } else if (t.getContent() instanceof TranslatableTextContent tc) {
            Object[] args = tc.getArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Text a) args[i] = replace(a, from, to);
                else if (args[i] instanceof String s) args[i] = s.replace(from, to);
            }
            out = Text.translatable(tc.getKey(), args);
        } else {
            out = t.copyContentOnly();
        }
        out.setStyle(t.getStyle());
        for (Text sibling : t.getSiblings()) out.append(replace(sibling, from, to));
        return out;
    }
}
