package dev.symbiot.menu.render;

import dev.symbiot.menu.SymbiotMenuClient;
import net.minecraft.resources.Identifier;

/** Все текстуры мода и их реальные размеры в пикселях. */
public final class Tex {
    private Tex() {}

    public record T(Identifier id, int w, int h) {}

    private static T t(String name, int w, int h) {
        return new T(Identifier.fromNamespaceAndPath(SymbiotMenuClient.MOD_ID, "textures/gui/" + name + ".png"), w, h);
    }

    /** Нарисованный фон (мастер-кадр). */
    public static final T MASTER = t("master", 1664, 928);
    public static final T FOG = t("fog", 512, 96);
    public static final T BLOB = t("blob", 32, 32);
    public static final T GLOW = t("glow", 64, 64);
    public static final T GEAR = t("gear", 64, 64);
    public static final T CRYSTAL = t("crystal", 16, 32);
    public static final T BUTTON = t("button", 256, 40);
    public static final T LOGO = t("logo", 968, 263);
    public static final T LOGO_ENERGY = t("logo_energy", 968, 263);
}
