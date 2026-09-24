package dev.symbiot.menu.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

import static dev.symbiot.menu.render.Draw.*;

/** Фиолетовые споры, медленно поднимающиеся вверх. */
public final class Spores {
    private static final int N = 70;
    private final float[] x = new float[N], y = new float[N], vy = new float[N], ph = new float[N], sz = new float[N];
    private final Random rnd = new Random(42);
    private int w = -1, h = -1;

    private void respawn(int i, boolean anywhere) {
        x[i] = rnd.nextFloat() * w;
        y[i] = anywhere ? rnd.nextFloat() * h : h + 5 + rnd.nextFloat() * 20;
        vy[i] = 4f + rnd.nextFloat() * 10f;
        ph[i] = rnd.nextFloat() * TAU;
        sz[i] = 0.6f + rnd.nextFloat() * 1.4f;
    }

    public void draw(GuiGraphicsExtractor g, int width, int height, float time, float dt) {
        if (width != w || height != h) {
            w = width; h = height;
            for (int i = 0; i < N; i++) respawn(i, true);
        }
        for (int i = 0; i < N; i++) {
            y[i] -= vy[i] * dt;
            float dx = sin(time * 0.7f + ph[i]) * 6f;
            if (y[i] < -10) respawn(i, false);
            float life = clamp01((h - y[i]) / (h * 0.25f)) * clamp01(y[i] / (h * 0.2f));
            float tw = 0.55f + 0.45f * sin(time * 2.3f + ph[i] * 3f);
            float a = life * tw;
            glow(g, x[i] + dx, y[i], sz[i] * 3.2f, argb(0.35f * a, 0x9B4DFF));
            blob(g, x[i] + dx, y[i], sz[i] * 0.55f, argb(0.9f * a, 0xE8C8FF));
        }
    }
}
