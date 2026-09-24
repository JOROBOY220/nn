package dev.symbiot.menu.mixin;

import dev.symbiot.menu.screen.SymbiotTitleScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * В 26.2 экраны открываются через {@code Minecraft.getInstance().gui.setScreen(...)}.
 * Подменяем ванильный TitleScreen на наш ещё до того, как он будет показан, —
 * поэтому нет ни одного кадра со старым меню.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private Screen symbiot$replaceTitleScreen(Screen screen) {
        if (screen instanceof TitleScreen) {
            return new SymbiotTitleScreen();
        }
        return screen;
    }
}
