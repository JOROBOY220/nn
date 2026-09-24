package dev.symbiot.menu.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;

/**
 * Низкоуровневые примитивы: спрайт с дробной позицией, поворотом, масштабом и цветом.
 * Вся «живая» графика симбиота собирается из этих вызовов.
 */
public final class Draw {
    private Draw() {}

    public static final float PI = (float) Math.PI;
    public static final float TAU = PI * 2f;

    /** Спрайт по центру (cx, cy) размером w x h, повернутый на rot радиан. */
    public static void sprite(GuiGraphicsExtractor g, Tex.T tex, float cx, float cy, float w, float h, float rot, int argb) {
        if (w < 0.05f || h < 0.05f || (argb >>> 24) == 0) return;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        if (rot != 0f) pose.rotate(rot);
        pose.scale(w / tex.w(), h / tex.h());
        g.blit(RenderPipelines.GUI_TEXTURED, tex.id(), -tex.w() / 2, -tex.h() / 2, 0f, 0f,
                tex.w(), tex.h(), tex.w(), tex.h(), argb);
        pose.popMatrix();
    }

    /** Спрайт от левого верхнего угла. */
    public static void rect(GuiGraphicsExtractor g, Tex.T tex, float x, float y, float w, float h, int argb) {
        sprite(g, tex, x + w / 2f, y + h / 2f, w, h, 0f, argb);
    }

    public static void blob(GuiGraphicsExtractor g, float cx, float cy, float r, int argb) {
        sprite(g, Tex.BLOB, cx, cy, r * 2f, r * 2f, 0f, argb);
    }

    public static void ellipse(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, float rot, int argb) {
        sprite(g, Tex.BLOB, cx, cy, rx * 2f, ry * 2f, rot, argb);
    }

    public static void glow(GuiGraphicsExtractor g, float cx, float cy, float r, int argb) {
        sprite(g, Tex.GLOW, cx, cy, r * 2f, r * 2f, 0f, argb);
    }

    public static void glow(GuiGraphicsExtractor g, float cx, float cy, float rx, float ry, int argb) {
        sprite(g, Tex.GLOW, cx, cy, rx * 2f, ry * 2f, 0f, argb);
    }

    // ------------------------------------------------------------ math & color

    public static int argb(float alpha, int rgb) {
        int a = Math.round(clamp01(alpha) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerpColor(int c0, int c1, float t) {
        t = clamp01(t);
        int a = (int) lerp(c0 >>> 24, c1 >>> 24, t);
        int r = (int) lerp((c0 >> 16) & 0xFF, (c1 >> 16) & 0xFF, t);
        int gg = (int) lerp((c0 >> 8) & 0xFF, (c1 >> 8) & 0xFF, t);
        int b = (int) lerp(c0 & 0xFF, c1 & 0xFF, t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    public static int withAlpha(int argb, float mul) {
        int a = Math.round((argb >>> 24) * clamp01(mul));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    public static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
    public static float sin(float v) { return (float) Math.sin(v); }
    public static float cos(float v) { return (float) Math.cos(v); }
    public static float smooth(float t) { t = clamp01(t); return t * t * (3f - 2f * t); }
    public static float frac(float v) { return v - (float) Math.floor(v); }

    /** Экспоненциальное сглаживание, не зависящее от FPS. */
    public static float approach(float cur, float target, float speed, float dt) {
        return cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
    }

    /** Детерминированный «шум» 0..1 по целому ключу. */
    public static float hash(int n) {
        n = (n << 13) ^ n;
        int m = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff;
        return m / (float) 0x7fffffff;
    }
}
