package dev.symbiot.menu.screen;

import dev.symbiot.menu.render.Background;
import dev.symbiot.menu.render.Spores;
import dev.symbiot.menu.render.SymbiotButton;
import dev.symbiot.menu.render.SymbiotButton.Kind;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dev.symbiot.menu.render.Draw.*;

/** Встроенный список модов (если Mod Menu не установлен). */
public class SymbiotModListScreen extends Screen {
    private static final int ROW = 26;

    private final Screen parent;
    private final List<ModMetadata> mods = new ArrayList<>();
    private final Spores spores = new Spores();
    private SymbiotButton back;
    private boolean closing;

    private float time, scroll, scrollTarget;
    private long lastNanos;
    private int px0, py0, px1, py1;

    public SymbiotModListScreen(Screen parent) {
        super(Component.translatable("symbiotmenu.mods.title"));
        this.parent = parent;
        FabricLoader.getInstance().getAllMods().stream()
                .filter(m -> m.getContainingMod().isEmpty())
                .map(m -> m.getMetadata())
                .filter(m -> !m.getId().equals("java"))
                .sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
                .forEach(mods::add);
    }

    @Override
    protected void init() {
        closing = false;
        int pw = Math.min(this.width - 40, 340);
        px0 = (this.width - pw) / 2;
        px1 = px0 + pw;
        py0 = 36;
        py1 = this.height - 44;
        back = new SymbiotButton(Kind.BACK, Component.translatable("symbiotmenu.button.back"), 71, this::onClose);
        back.layout((this.width - 150) / 2f, this.height - 32, 150, 20, 0.85f);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    private float maxScroll() {
        return Math.max(0, mods.size() * ROW - (py1 - py0 - 8));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollTarget = clamp(scrollTarget - (float) scrollY * ROW, 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !closing && back.contains(event.x(), event.y())) {
            closing = true;
            back.press();
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.72F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        time += dt;
        scroll = approach(scroll, scrollTarget, 14f, dt);

        Background.draw(g, this.width, this.height, time, 0f, 0f);
        spores.draw(g, this.width, this.height, time, dt);

        // панель
        g.fill(px0, py0, px1, py1, 0xC00B0714);
        g.fill(px0, py0, px1, py0 + 1, 0xFF6B3AA8);
        g.fill(px0, py1 - 1, px1, py1, 0xFF6B3AA8);
        g.fill(px0, py0, px0 + 1, py1, 0xFF3A2356);
        g.fill(px1 - 1, py0, px1, py1, 0xFF3A2356);

        Component title = this.title;
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, 14, 0xFFE6D6FF, true);
        Component count = Component.translatable("symbiotmenu.mods.count", mods.size());
        g.text(this.font, count, (this.width - this.font.width(count)) / 2, 24, 0xFF9C8BB8, false);

        g.enableScissor(px0 + 1, py0 + 1, px1 - 1, py1 - 1);
        int y = py0 + 4 - Math.round(scroll);
        for (ModMetadata m : mods) {
            if (y + ROW >= py0 && y <= py1) {
                boolean hov = mouseX >= px0 && mouseX < px1 && mouseY >= y && mouseY < y + ROW && mouseY >= py0 && mouseY < py1;
                if (hov) g.fill(px0 + 3, y, px1 - 3, y + ROW - 2, 0x402A0F4A);
                glow(g, px0 + 12, y + 11, 6, argb(hov ? 0.7f : 0.3f, 0x9B4DFF));
                blob(g, px0 + 12, y + 11, 2.2f, 0xFFC77DFF);
                String name = m.getName();
                String ver = m.getVersion().getFriendlyString();
                g.text(this.font, Component.literal(name), px0 + 22, y + 3, 0xFFEDE3FF, true);
                g.text(this.font, Component.literal(ver), px0 + 26 + this.font.width(name), y + 3, 0xFF7F6F99, false);
                String desc = trim(m.getDescription().replace('\n', ' '), px1 - px0 - 30);
                g.text(this.font, Component.literal(desc), px0 + 22, y + 13, 0xFF9C8BB8, false);
            }
            y += ROW;
        }
        g.disableScissor();

        back.update(dt, back.contains(mouseX, mouseY) || closing);
        back.render(g, this.font, time, mouseX, mouseY);
        if (closing && back.clickFinished()) {
            back.resetClick();
            closing = false;
            onClose();
        }
    }

    private String trim(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) return s;
        String dots = "...";
        int end = s.length();
        while (end > 0 && this.font.width(s.substring(0, end) + dots) > maxWidth) end--;
        return s.substring(0, end) + dots;
    }
}
