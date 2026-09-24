package dev.symbiot.menu.screen;

import dev.symbiot.menu.SymbiotMenuClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Кнопка «Моды»: если установлен Mod Menu — открывается его экран,
 * иначе — встроенный список модов в стиле симбиота.
 * Mod Menu вызывается через рефлексию, чтобы не требовать его при сборке.
 */
public final class ModsScreens {
    private ModsScreens() {}

    public static Screen create(Screen parent) {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            try {
                Class<?> cls = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                return (Screen) cls.getConstructor(Screen.class).newInstance(parent);
            } catch (Throwable t) {
                SymbiotMenuClient.LOG.warn("Mod Menu found, but its screen could not be opened; using built-in list", t);
            }
        }
        return new SymbiotModListScreen(parent);
    }
}
