package dev.symbiot.menu.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.symbiot.menu.render.Draw.*;

/**
 * Процедурное щупальце: позвоночник из точек, изгибаемый бегущей синусоидой,
 * тело из «мясных» капель, светящаяся прожилка и пульс, бегущий от основания к кончику.
 */
public final class Tentacle {
    public float x, y;          // основание
    public float angle;         // направление, рад (0 = вправо, PI/2 = вниз)
    public float length = 20f;
    public float baseR = 3f, tipR = 0.6f;
    public float amp = 0.35f;   // амплитуда изгиба (рад)
    public float waves = 1.3f;  // сколько «волн» вдоль длины
    public float curl = 0f;     // постоянное закручивание к кончику (рад)
    public float speed = 1f;
    public float phase = 0f;
    public int body = 0xFF170C24;
    public int vein = 0xFFB35CFF;
    public int glowColor = 0x8A3CFF;

    // результат последнего draw()
    public float tipX, tipY, tipAngle;

    private static final int MAX = 160;
    private final float[] px = new float[MAX], py = new float[MAX], pr = new float[MAX], pa = new float[MAX];

    public Tentacle set(float x, float y, float angle, float length, float baseR, float tipR) {
        this.x = x; this.y = y; this.angle = angle; this.length = length; this.baseR = baseR; this.tipR = tipR;
        return this;
    }

    public Tentacle motion(float amp, float waves, float speed, float phase) {
        this.amp = amp; this.waves = waves; this.speed = speed; this.phase = phase;
        return this;
    }

    public Tentacle curl(float curl) { this.curl = curl; return this; }

    /** Рассчитать позвоночник и вернуть индекс последней точки. */
    private int build(float time, float lengthMul, float extraCurl) {
        float len = Math.max(0.5f, length * lengthMul);
        float avgR = (baseR + tipR) * 0.5f;
        float step = Math.max(0.7f, avgR * 0.33f);
        int n = (int) clamp(len / step, 3, MAX - 1);
        float ds = len / n;
        float cx = x, cy = y;
        for (int i = 0; i <= n; i++) {
            float s = i / (float) n;
            float a = angle
                    + amp * s * sin(time * speed + phase - s * waves * TAU)
                    + (curl + extraCurl) * s * s;
            px[i] = cx; py[i] = cy; pa[i] = a;
            pr[i] = lerp(baseR, tipR, (float) Math.pow(s, 0.85));
            cx += cos(a) * ds;
            cy += sin(a) * ds;
        }
        tipX = px[n]; tipY = py[n]; tipAngle = pa[n];
        return n;
    }

    /**
     * @param intensity  0..1 — сила фиолетового свечения вокруг (наведение)
     * @param veinBoost  0..1 — яркость/скорость пульса прожилок
     * @param lengthMul  множитель длины (сжатие при клике)
     * @param extraCurl  дополнительное закручивание (хватка при клике)
     */
    public void draw(GuiGraphicsExtractor g, float time, float intensity, float veinBoost, float lengthMul, float extraCurl) {
        int n = build(time, lengthMul, extraCurl);

        if (intensity > 0.01f) {
            int gc = argb(0.16f * intensity, glowColor);
            for (int i = 0; i <= n; i += 3) glow(g, px[i], py[i], pr[i] * 2.6f + 2f, gc);
        }
        for (int i = 0; i <= n; i++) blob(g, px[i], py[i], pr[i], body);

        float pulseSpeed = 3.2f + 5f * veinBoost;
        float veinA = 0.28f + 0.55f * veinBoost;
        for (int i = 0; i <= n; i++) {
            if (pr[i] < 0.9f) continue;
            float s = i / (float) n;
            float p = 0.5f + 0.5f * sin(time * pulseSpeed - s * 11f + phase);
            p = p * p * p;
            float a = veinA * (0.25f + 0.75f * p) * (1f - s * 0.5f);
            float nx = -sin(pa[i]) * pr[i] * 0.3f, ny = cos(pa[i]) * pr[i] * 0.3f;
            blob(g, px[i] + nx, py[i] + ny, Math.max(0.45f, pr[i] * 0.32f), argb(a, vein & 0xFFFFFF));
        }
    }

    public void draw(GuiGraphicsExtractor g, float time) {
        draw(g, time, 0f, 0f, 1f, 0f);
    }
}
