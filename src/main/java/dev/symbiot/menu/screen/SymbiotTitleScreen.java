package dev.symbiot.menu.screen;

import dev.symbiot.menu.SymbiotMenuClient;
import dev.symbiot.menu.render.Background;
import dev.symbiot.menu.render.Draw;
import dev.symbiot.menu.render.Eye;
import dev.symbiot.menu.render.Spores;
import dev.symbiot.menu.render.SymbiotButton;
import dev.symbiot.menu.render.SymbiotButton.Kind;
import dev.symbiot.menu.render.Tentacle;
import dev.symbiot.menu.render.Tex;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static dev.symbiot.menu.render.Draw.*;

/** Главное меню «Симбиот». */
public class SymbiotTitleScreen extends Screen {

    private final List<SymbiotButton> buttons = new ArrayList<>();
    private final List<Tentacle> ambient = new ArrayList<>();
    private final Tentacle[] logoTentacles = new Tentacle[8];
    private final Spores spores = new Spores();

    private long lastNanos;
    private float time;
    private float fade = 1f;
    private float smx, smy;
    private int focus = -1;
    private int lastMouseX = Integer.MIN_VALUE, lastMouseY = Integer.MIN_VALUE;
    private SymbiotButton pending;

    private float logoW, logoH, logoX, logoY;

    private static String mcVersion;
    private static int modCount = -1;

    public SymbiotTitleScreen() {
        super(Component.translatable("symbiotmenu.title"));
        for (int i = 0; i < logoTentacles.length; i++) logoTentacles[i] = new Tentacle();
    }

    // ---------------------------------------------------------------- setup

    @Override
    protected void init() {
        if (mcVersion == null) {
            mcVersion = FabricLoader.getInstance().getModContainer("minecraft")
                    .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("26.2");
            modCount = (int) FabricLoader.getInstance().getAllMods().stream()
                    .filter(m -> m.getContainingMod().isEmpty())
                    .map(m -> m.getMetadata().getId())
                    .filter(id -> !id.equals("java") && !id.equals("minecraft") && !id.equals("fabricloader"))
                    .count();
        }
        pending = null;
        if (lastNanos != 0) fade = Math.max(fade, 0.35f); // мягкое появление при возврате

        buttons.clear();
        buttons.add(new SymbiotButton(Kind.SINGLEPLAYER, Component.translatable("symbiotmenu.button.singleplayer"), 11,
                () -> open(new SelectWorldScreen(this))));
        buttons.add(new SymbiotButton(Kind.MULTIPLAYER, Component.translatable("symbiotmenu.button.multiplayer"), 23,
                () -> open(new JoinMultiplayerScreen(this))));
        buttons.add(new SymbiotButton(Kind.SETTINGS, Component.translatable("symbiotmenu.button.settings"), 37,
                () -> open(new OptionsScreen(this, this.minecraft.options, false))));
        buttons.add(new SymbiotButton(Kind.MODS, Component.translatable("symbiotmenu.button.mods"), 41,
                () -> open(ModsScreens.create(this))));
        buttons.add(new SymbiotButton(Kind.QUIT, Component.translatable("symbiotmenu.button.quit"), 53,
                () -> this.minecraft.stop()));

        doLayout();
    }

    private void doLayout() {
        int w = this.width, h = this.height;
        logoW = Math.min(w * 0.42f, 300f);
        logoW = Math.min(logoW, h * 0.78f);
        logoH = logoW * Tex.LOGO.h() / (float) Tex.LOGO.w();
        logoX = (w - logoW) / 2f;
        logoY = h * 0.045f;

        float start = logoY + logoH + 24f;
        float avail = h - 20f - start;
        float spacing = Math.min(42f, avail / 5f);
        float k = clamp(spacing / 36f, 0.62f, 1.2f);
        float bw = clamp(w * 0.34f, 150f, 210f);
        float bh = 20f;
        float first = start + (avail - spacing * 5f) * 0.35f + (spacing - bh) * 0.5f;
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).layout((w - bw) / 2f, first + i * spacing, bw, bh, k);
        }

        // окружающие щупальца отключены: на нарисованном фоне уже есть свои корни
        ambient.clear();
    }

    private void open(Screen screen) {
        this.minecraft.gui.setScreen(screen);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    // ---------------------------------------------------------------- input

    private void press(SymbiotButton b) {
        if (pending != null) return;
        pending = b;
        b.press();
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.72F));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && pending == null) {
            for (SymbiotButton b : buttons) {
                if (b.contains(event.x(), event.y())) {
                    press(b);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        int n = buttons.size();
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_TAB) {
            focus = (focus + 1 + n) % n;
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            focus = focus < 0 ? n - 1 : (focus - 1 + n) % n;
            return true;
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) && focus >= 0) {
            press(buttons.get(focus));
            return true;
        }
        return super.keyPressed(event);
    }

    // ---------------------------------------------------------------- render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        time += dt;
        fade = Math.max(0f, fade - dt * 0.9f);

        int w = this.width, h = this.height;
        float nmx = clamp((mouseX - w / 2f) / (w / 2f), -1f, 1f);
        float nmy = clamp((mouseY - h / 2f) / (h / 2f), -1f, 1f);
        smx = approach(smx, nmx, 2.5f, dt);
        smy = approach(smy, nmy, 2.5f, dt);

        Background.draw(g, w, h, time, smx, smy);

        for (Tentacle t : ambient) t.draw(g, time, 0.35f, 0.3f, 1f, 0f);

        drawLogo(g, mouseX, mouseY);

        boolean mouseMoved = mouseX != lastMouseX || mouseY != lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (mouseMoved) focus = -1; // мышь «перехватывает» управление у клавиатуры
        for (int i = 0; i < buttons.size(); i++) {
            SymbiotButton b = buttons.get(i);
            boolean hov = b.contains(mouseX, mouseY);
            b.update(dt, pending == null && (hov || focus == i) || pending == b);
            b.render(g, this.font, time, mouseX, mouseY);
        }

        spores.draw(g, w, h, time, dt);
        drawHud(g);

        // мышиная «аура»
        glow(g, mouseX, mouseY, 26f, argb(0.08f, 0x9B4DFF));

        if (pending != null) {
            float c = clamp01(pending.click);
            g.fill(0, 0, w, h, argb(0.28f * c * c, 0x0A0012));
        }
        if (fade > 0f) g.fill(0, 0, w, h, argb(smooth(fade), 0x000000));

        if (pending != null && pending.clickFinished()) {
            SymbiotButton b = pending;
            b.resetClick();
            pending = null;
            try {
                b.run();
            } catch (Throwable t) {
                SymbiotMenuClient.LOG.error("Failed to run menu action {}", b.kind, t);
            }
        }
    }

    private void drawLogo(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        float bob = sin(time * 0.8f) * 1.2f;
        float lx = logoX, ly = logoY + bob;
        float cx = lx + logoW / 2f, cy = ly + logoH / 2f;

        float breathe = 0.5f + 0.5f * sin(time * 1.1f);
        glow(g, cx, cy, logoW * 0.62f, logoH * 1.2f, argb(0.20f + 0.12f * breathe, 0x7A2CFF));

        // отростки, растущие из-за логотипа
        float u = logoW / 300f;
        setLogoT(0, lx + logoW * 0.06f, ly + logoH * 0.50f, PI + 0.35f, 55 * u, 6 * u, 0.25f, -1.2f);
        setLogoT(1, lx + logoW * 0.10f, ly + logoH * 0.70f, PI - 0.25f, 45 * u, 5 * u, 1.3f, 1.4f);
        setLogoT(2, lx + logoW * 0.94f, ly + logoH * 0.50f, -0.35f, 55 * u, 6 * u, 2.1f, 1.2f);
        setLogoT(3, lx + logoW * 0.90f, ly + logoH * 0.70f, 0.25f, 45 * u, 5 * u, 3.3f, -1.4f);
        setLogoT(4, lx + logoW * 0.35f, ly + logoH * 0.20f, -PI / 2f - 0.55f, 32 * u, 4.5f * u, 4.2f, -0.9f);
        setLogoT(5, lx + logoW * 0.65f, ly + logoH * 0.20f, -PI / 2f + 0.55f, 32 * u, 4.5f * u, 5.4f, 0.9f);
        setLogoT(6, lx + logoW * 0.50f, ly + logoH * 0.10f, -PI / 2f, 26 * u, 5f * u, 6.1f, 0f);
        setLogoT(7, lx + logoW * 0.50f, ly + logoH * 0.85f, PI / 2f, 24 * u, 5f * u, 7.3f, 0.3f);
        for (Tentacle t : logoTentacles) t.draw(g, time, 0.6f, 0.5f, 1f, 0f);

        sprite(g, Tex.LOGO, cx, cy, logoW, logoH, 0f, 0xFFFFFFFF);

        // фиолетовая энергия, периодически пробегающая по буквам
        float period = 5.0f, dur = 2.1f;
        float p = (time % period) / dur;
        if (p <= 1f) {
            float band = logoW * 0.22f;
            float bx = lx - band + smooth(p) * (logoW + band * 2f);
            energyBand(g, bx - band * 0.5f, bx + band * 0.5f, ly, 0.45f, 0xB872FF);
            energyBand(g, bx - band * 0.18f, bx + band * 0.18f, ly, 0.95f, 0xE9D2FF);
        }
        // тихое мерцание прожилок постоянно
        sprite(g, Tex.LOGO_ENERGY, cx, cy, logoW, logoH, 0f, argb(0.10f + 0.12f * breathe, 0xA45CFF));

        Eye.draw(g, cx, ly + logoH * 0.93f, 6f * u, 0.35f + 0.65f * clamp01(sin(time * 0.25f) * 2f - 0.6f),
                (mouseX - cx) / 150f, (mouseY - ly) / 100f, time, false);

        // подзаголовок
        Component sub = Component.translatable("symbiotmenu.subtitle");
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        float sScale = 0.75f;
        float sw = this.font.width(sub) * sScale;
        pose.translate(cx - sw / 2f, logoY + logoH + 5f);
        pose.scale(sScale, sScale);
        g.text(this.font, sub, 0, 0, 0xFFB9A5D6, true);
        pose.popMatrix();
    }

    private void setLogoT(int i, float x, float y, float ang, float len, float r, float phase, float curl) {
        logoTentacles[i].set(x, y, ang, len, r, 0.6f).motion(0.35f, 1.2f, 0.9f, phase).curl(curl);
    }

    private void energyBand(GuiGraphicsExtractor g, float x0, float x1, float ly, float alpha, int rgb) {
        int sx0 = (int) Math.max(0, Math.floor(x0)), sx1 = (int) Math.min(this.width, Math.ceil(x1));
        if (sx1 <= sx0) return;
        g.enableScissor(sx0, (int) (ly - 2), sx1, (int) (ly + logoH + 2));
        sprite(g, Tex.LOGO_ENERGY, logoX + logoW / 2f, ly + logoH / 2f, logoW, logoH, 0f, argb(alpha, rgb));
        g.disableScissor();
    }

    private void drawHud(GuiGraphicsExtractor g) {
        int w = this.width, h = this.height;
        Matrix3x2fStack pose = g.pose();

        // слева сверху — версия
        Eye.draw(g, 11, 11, 5f, 0.6f + 0.4f * sin(time * 0.9f), 0f, 0f, time, false);
        pose.pushMatrix();
        pose.translate(20, 5);
        pose.scale(0.75f, 0.75f);
        g.text(this.font, Component.translatable("symbiotmenu.version", mcVersion), 0, 0, 0xC0B7A9CC, true);
        g.text(this.font, Component.translatable("symbiotmenu.loader", modCount), 0, 10, 0x90B7A9CC, true);
        pose.popMatrix();

        // слева снизу — девиз
        Component motto = Component.translatable("symbiotmenu.motto");
        Eye.draw(g, 11, h - 10, 5f, 0.5f + 0.5f * sin(time * 0.7f + 1f), 0f, 0f, time, true);
        pose.pushMatrix();
        pose.translate(20, h - 13);
        pose.scale(0.75f, 0.75f);
        g.text(this.font, motto, 0, 0, 0xA0B7A9CC, true);
        pose.popMatrix();

        // справа снизу — копирайт (как в ванилле)
        Component copy = Component.translatable("title.credits");
        pose.pushMatrix();
        float cw = this.font.width(copy) * 0.75f;
        pose.translate(w - cw - 4, h - 11);
        pose.scale(0.75f, 0.75f);
        g.text(this.font, copy, 0, 0, 0x80B7A9CC, true);
        pose.popMatrix();
    }
}
