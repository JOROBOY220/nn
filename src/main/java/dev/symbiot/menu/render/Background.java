package dev.symbiot.menu.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.symbiot.menu.render.Draw.*;

/**
 * Фон: нарисованный мастер-кадр + живые эффекты поверх:
 * плавный сдвиг за мышью, пульс сияния вокруг луны, вспышки молний,
 * мерцание фонарей, плывущий туман над озером, виньетка.
 *
 * Координаты эффектов заданы в долях кадра (0..1), поэтому при замене
 * master.png на картинку с другой компоновкой их нужно поправить ниже.
 */
public final class Background {
    private Background() {}

    // ---- координаты на master.png (доли ширины/высоты кадра)
    private static final float MOON_X = 0.745f, MOON_Y = 0.162f, MOON_R = 0.036f;
    private static final float[][] LANTERNS = {
            // x, y, размер (доля ширины)
            {0.067f, 0.255f, 0.030f},
            {0.989f, 0.514f, 0.022f},
            {0.870f, 0.723f, 0.012f},
    };
    private static final float FOG_Y1 = 0.66f, FOG_Y2 = 0.78f;

    /** mx, my — положение мыши, нормализованное в -1..1. */
    public static void draw(GuiGraphicsExtractor g, int w, int h, float time, float mx, float my) {
        Tex.T img = Tex.MASTER;
        float s = Math.max(w / (float) img.w(), h / (float) img.h()) * 1.05f;
        float dw = img.w() * s, dh = img.h() * s;
        float marginX = (dw - w) / 2f, marginY = (dh - h) / 2f;

        // лёгкий сдвиг за мышью + очень медленный «дрейф камеры»
        float offX = -mx * marginX * 0.65f + sin(time * 0.05f) * marginX * 0.25f;
        float offY = -my * marginY * 0.65f + sin(time * 0.037f + 1f) * marginY * 0.2f;
        float x0 = (w - dw) / 2f + offX, y0 = (h - dh) / 2f + offY;

        g.fill(0, 0, w, h, 0xFF05030B);
        sprite(g, img, x0 + dw / 2f, y0 + dh / 2f, dw, dh, 0f, 0xFFFFFFFF);

        // ---- луна: дышащее сияние и молнии
        float moonX = x0 + MOON_X * dw, moonY = y0 + MOON_Y * dh, moonR = MOON_R * dw;
        float pulse = 0.5f + 0.5f * sin(time * 0.7f);
        glow(g, moonX, moonY, moonR * (2.6f + 0.25f * pulse), argb(0.14f + 0.12f * pulse, 0xA055FF));
        float flash = lightning(time);
        if (flash > 0.01f) {
            glow(g, moonX, moonY, moonR * 4.5f, argb(0.45f * flash, 0xD9B8FF));
            glow(g, moonX, moonY, moonR * 1.6f, argb(0.35f * flash, 0xFFFFFF));
            g.fill(0, 0, w, h, argb(0.06f * flash, 0xB08CFF));
        }

        // ---- фонари
        for (int i = 0; i < LANTERNS.length; i++) {
            float[] l = LANTERNS[i];
            lantern(g, x0 + l[0] * dw, y0 + l[1] * dh, l[2] * dw, time, i);
        }

        // ---- туман над озером (два слоя навстречу друг другу)
        fog(g, w, y0 + FOG_Y1 * dh, s * 1.9f, time * 9f, 0.22f, 0xC8E2DC);
        fog(g, w, y0 + FOG_Y2 * dh, s * 2.6f, -time * 14f, 0.18f, 0xB9D6D2);

        // ---- виньетка
        g.fillGradient(0, 0, w, (int) (h * 0.22f), 0x80000000, 0x00000000);
        g.fillGradient(0, (int) (h * 0.72f), w, h, 0x00000000, 0xA005020A);
    }

    /** Две короткие вспышки примерно раз в 11 секунд. */
    private static float lightning(float time) {
        float t = time % 11f;
        float a = Math.max(0f, 1f - Math.abs(t - 0.10f) / 0.06f);
        float b = 0.7f * Math.max(0f, 1f - Math.abs(t - 0.32f) / 0.08f);
        return Math.max(a, b);
    }

    private static void fog(GuiGraphicsExtractor g, int w, float y, float scale, float scroll, float alpha, int rgb) {
        float tw = Tex.FOG.w() * scale, th = Tex.FOG.h() * scale;
        float start = -frac(scroll / tw) * tw;
        int col = argb(alpha, rgb);
        for (float x = start - tw; x < w + tw; x += tw - 1f) {
            sprite(g, Tex.FOG, x + tw / 2f, y, tw, th, 0f, col);
        }
    }

    private static void lantern(GuiGraphicsExtractor g, float x, float y, float size, float time, int seed) {
        float fl = 0.8f + 0.12f * sin(time * 9f + seed * 3f) + 0.08f * sin(time * 23f + seed * 5f);
        glow(g, x, y, size * 2.8f * fl, argb(0.22f * fl, 0xFF9A3C));
        glow(g, x, y, size * 1.1f, argb(0.30f * fl, 0xFFC56B));
    }

}
