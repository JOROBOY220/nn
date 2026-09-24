package dev.symbiot.menu;

import dev.symbiot.menu.screen.SymbiotTitleScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа мода. Основная подмена главного меню делается миксином
 * (см. {@link dev.symbiot.menu.mixin.GuiMixin}). Здесь стоит страховка:
 * если по какой-то причине ванильное меню всё же открылось, оно будет
 * заменено на меню симбиота в следующем кадре.
 */
public final class SymbiotMenuClient implements ClientModInitializer {
    public static final String MOD_ID = "symbiotmenu";
    public static final Logger LOG = LoggerFactory.getLogger("SymbiotMenu");

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) {
                client.execute(() -> {
                    if (client.gui.screen() instanceof TitleScreen) {
                        client.gui.setScreen(new SymbiotTitleScreen());
                    }
                });
            }
        });
        LOG.info("Symbiot menu awakened");
    }
}
