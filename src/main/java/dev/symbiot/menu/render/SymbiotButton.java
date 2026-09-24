package dev.symbiot.menu.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import static dev.symbiot.menu.render.Draw.*;

/**
 * Кнопка — кусок живой ткани симбиота, обвившей тёмную каменную плиту.
 * У каждого вида кнопки своя форма нароста (см. {@link Kind}).
 */
public final class SymbiotButton {

    public enum Kind {
        /** массивный симбиотический нарост-панцирь */
        SINGLEPLAYER,
        /** тонкие щупальца, тянущиеся друг к другу */
        MULTIPLAYER,
        /** симбиот обвивает металлическую шестерню */
        SETTINGS,
        /** отростки с прорастающими фиолетовыми кристаллами */
        MODS,
        /** симбиот стекает вниз, отпуская игрока */
        QUIT,
        /** простая кнопка «Назад» */
        BACK
    }

    private static final float CLICK_TIME = 0.36f;

    public final Kind kind;
    public final Component label;
    private final Runnable action;
    private final int seed;

    public float x, y, w, h, k = 1f;
    public float hover;
    public float click = -1f;
    private float at; // собственные «часы» — ускоряются при наведении

    private final Tentacle[] wraps = new Tentacle[4];
    private final Tentacle[] extra = new Tentacle[12];

    public SymbiotButton(Kind kind, Component label, int seed, Runnable action) {
        this.kind = kind;
        this.label = label;
        this.seed = seed;
        this.action = action;
        for (int i = 0; i < wraps.length; i++) wraps[i] = new Tentacle();
        for (int i = 0; i < extra.length; i++) extra[i] = new Tentacle();
        this.at = hash(seed) * 50f;
    }

    public void layout(float x, float y, float w, float h, float k) {
        this.x = x; this.y = y; this.w = w; this.h = h; this.k = k;
        float cy = y + h / 2f;
        // по два щупальца на каждом конце плиты — одно закручивается вверх, другое вниз
        wraps[0].set(x + 5 * k, cy - 1, PI, 15 * k, 3.4f * k, 0.6f).motion(0.28f, 1.1f, 1.1f, seed).curl(2.9f);
        wraps[1].set(x + 5 * k, cy + 2, PI, 11 * k, 2.8f * k, 0.5f).motion(0.3f, 1.2f, 1.3f, seed + 2).curl(-2.6f);
        wraps[2].set(x + w - 5 * k, cy - 1, 0f, 15 * k, 3.4f * k, 0.6f).motion(0.28f, 1.1f, 1.15f, seed + 4).curl(-2.9f);
        wraps[3].set(x + w - 5 * k, cy + 2, 0f, 11 * k, 2.8f * k, 0.5f).motion(0.3f, 1.2f, 1.25f, seed + 6).curl(2.6f);
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void press() { click = 0f; }
    public boolean clickFinished() { return click >= 1f; }
    public void resetClick() { click = -1f; }
    public void run() { action.run(); }

    public void update(float dt, boolean hovered) {
        hover = approach(hover, hovered || click >= 0 ? 1f : 0f, 9f, dt);
        at += dt * (1f + 2.4f * hover);
        if (click >= 0f && click < 1f) click = Math.min(1f, click + dt / CLICK_TIME);
    }

    /** 0..1 — насколько симбиот сжался вокруг кнопки. */
    private float squeeze() {
        if (click < 0f) return 0f;
        return smooth(click * 2.4f);
    }

    private float eyeOpen(float time) {
        float peek = clamp01(sin(time * 0.3f + seed * 1.7f) * 2f - 1.2f);
        float open = Math.max(0.08f + 0.5f * peek, hover);
        float blink = frac(time / 3.7f + hash(seed + 99));
        if (blink < 0.05f) open *= Math.abs(blink / 0.025f - 1f);
        if (click >= 0f) open = 1f;
        return open;
    }

    // ------------------------------------------------------------------ render

    public void render(GuiGraphicsExtractor g, Font font, float time, int mouseX, int mouseY) {
        float c = squeeze();
        float hv = hover;
        float cx = x + w / 2f, cy = y + h / 2f;
        float lenMul = 1f - 0.45f * c;

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        float sc = 1f - 0.06f * c + 0.015f * hv;
        pose.translate(cx, cy);
        pose.scale(sc, sc);
        pose.translate(-cx, -cy);

        // фиолетовое свечение за кнопкой
        glow(g, cx, cy, w * 0.66f, h * 2.0f, argb(0.06f + 0.45f * hv + 0.35f * c, 0x8A3CFF));

        drawBehind(g, time, mouseX, mouseY, c, lenMul);

        for (Tentacle t : wraps) {
            float grip = 1.8f * c * Math.signum(t.curl);
            t.draw(g, at, hv, hv, lenMul, grip);
        }

        // каменная плита
        rect(g, Tex.BUTTON, x, y, w, h, lerpColor(0xFFB3AAC2, 0xFFFFFFFF, hv));
        if (c > 0f) g.fill((int) x + 2, (int) y + 2, (int) (x + w) - 2, (int) (y + h) - 2, argb(0.35f * c, 0x12001F));
        if (hv > 0.01f) glow(g, cx, cy, w * 0.55f, h * 0.95f, argb(0.22f * hv, 0xA55CFF));
        tissue(g, c);

        drawFront(g, time, mouseX, mouseY, c, lenMul);

        // подпись
        int col = lerpColor(0xFFD6CCE4, 0xFFF6E6FF, hv);
        int tw = font.width(label);
        g.text(font, label, Math.round(cx - tw / 2f), Math.round(cy - 3.5f), col, true);

        // вспышка в конце нажатия
        if (click > 0.55f) {
            float f = (click - 0.55f) / 0.45f;
            glow(g, cx, cy, w * (0.4f + 0.5f * f), h * (1.2f + 1.5f * f), argb(0.55f * (1f - f), 0xC98BFF));
        }
        pose.popMatrix();
    }

    /** Живая ткань, облепившая верхний край плиты, и две «ленты», охватывающие её. */
    private void tissue(GuiGraphicsExtractor g, float c) {
        float hv = hover;
        int n = Math.max(8, (int) (w / (3.2f * k)));
        for (int i = 0; i < n; i++) {
            float s = i / (float) (n - 1);
            float px = x + 7 * k + s * (w - 14 * k);
            float py = y + 0.8f + sin(at * 1.4f + i * 0.55f) * 0.7f * k - c * 0.8f;
            float r = (1.2f + 0.8f * (0.5f + 0.5f * sin(i * 1.93f + seed))) * k;
            blob(g, px, py, r, 0xFF1B0E2A);
            float p = 0.5f + 0.5f * sin(at * 5f - i * 0.6f);
            blob(g, px, py - r * 0.2f, Math.max(0.5f, r * 0.38f), argb(0.18f + 0.62f * hv * p * p, 0xB35CFF));
        }
        // охватывающие ленты у краёв
        for (int side = 0; side < 2; side++) {
            float bx = side == 0 ? x + 13 * k : x + w - 13 * k;
            for (float yy = y - 1f; yy <= y + h + 1f; yy += 1.3f) {
                float wob = sin(at * 2f + yy * 0.4f + side * 2f) * 0.6f * k;
                blob(g, bx + wob, yy, 2.1f * k, 0xFF1B0E2A);
            }
            for (float yy = y; yy <= y + h; yy += 1.3f) {
                float p = 0.5f + 0.5f * sin(at * 6f - yy * 0.5f + side);
                blob(g, bx + sin(at * 2f + yy * 0.4f + side * 2f) * 0.6f * k - 0.5f, yy,
                        Math.max(0.45f, 0.6f * k), argb(0.2f + 0.7f * hover * p * p * p, 0xC77DFF));
            }
        }
    }

    private void eye(GuiGraphicsExtractor g, float ex, float ey, float size, float time, int mx, int my, boolean round, float openMul) {
        Eye.draw(g, ex, ey, size, eyeOpen(time) * openMul, (mx - ex) / 90f, (my - ey) / 60f, time, round);
    }

    // ------------------------------------------------------------------ per-kind shapes

    private void drawBehind(GuiGraphicsExtractor g, float time, int mx, int my, float c, float lenMul) {
        float hv = hover;
        float cx = x + w / 2f;
        switch (kind) {
            case SINGLEPLAYER -> {
                float br = (1f + 0.035f * sin(at * 1.1f) + 0.07f * hv) * (1f - 0.3f * c);
                float rx = 25 * k * br, ry = 11 * k * br;
                float oy = y + 3 * k;
                for (int j = 0; j < 7; j++) {
                    float a = PI + (j + 0.5f) / 7f * PI;
                    Tentacle t = extra[j];
                    t.set(cx + cos(a) * rx * 0.85f, oy + sin(a) * ry * 0.85f, a,
                            (6f + 3f * hash(j + seed)) * k * (1f + 0.5f * hv), 2.3f * k, 0.35f)
                            .motion(0.25f, 1.1f, 1.3f, j * 1.3f).curl(0f);
                    t.draw(g, at, hv, hv, lenMul, 0f);
                }
                ellipse(g, cx, oy - ry * 0.3f, rx * 1.02f, ry * 0.95f, 0f, 0xFF120A1C);
                glow(g, cx, oy - ry * 0.3f, rx * 1.3f, ry * 1.4f, argb(0.25f * hv, 0x9B4DFF));
            }
            case SETTINGS -> {
                float gy = y - 4.5f * k;
                float size = 30 * k * (1f - 0.12f * c);
                glow(g, cx, gy, size * 0.85f, argb(0.12f + 0.4f * hv, 0x9B4DFF));
                sprite(g, Tex.GEAR, cx, gy, size, size, at * 0.45f + c * 3f, lerpColor(0xFFB4ACC2, 0xFFF2EAFF, hv));
            }
            default -> { }
        }
    }

    private void drawFront(GuiGraphicsExtractor g, float time, int mx, int my, float c, float lenMul) {
        float hv = hover;
        float cx = x + w / 2f;
        switch (kind) {
            case SINGLEPLAYER -> {
                float br = (1f + 0.035f * sin(at * 1.1f) + 0.07f * hv) * (1f - 0.3f * c);
                float rx = 25 * k * br, ry = 11 * k * br;
                float oy = y + 3 * k;
                for (int p = 0; p < 9; p++) {
                    float a = PI * (1.07f + 0.86f * p / 8f);
                    float pulse = 0.5f + 0.5f * sin(at * 3f + p * 0.9f);
                    float lift = hv * 1.3f * k * pulse;
                    float px = cx + cos(a) * (rx * 0.7f + lift);
                    float py = oy - ry * 0.1f + sin(a) * (ry * 0.7f + lift);
                    ellipse(g, px, py, ry * 0.55f, rx * 0.13f, a, 0xFF2A1A3C);
                    ellipse(g, px + cos(a) * 1.2f, py + sin(a) * 1.2f, ry * 0.36f, rx * 0.06f, a,
                            argb(0.3f + 0.55f * hv * pulse, 0x9C5BE0));
                    if (p < 8 && hv > 0.02f) {
                        float a2 = a + PI * 0.86f / 16f;
                        glow(g, cx + cos(a2) * rx * 0.6f, oy + sin(a2) * ry * 0.6f, 3.5f * k,
                                argb(0.55f * hv * pulse, 0xB866FF));
                    }
                }
                eye(g, cx, oy - ry * 0.4f, 7.5f * k, time, mx, my, false, 1f);
            }
            case MULTIPLAYER -> {
                for (int p = 0; p < 3; p++) {
                    float bx = x + w * (0.2f + 0.085f * p), by = y + 1.5f;
                    float tx = cx, ty = y - (3.5f + 4.5f * p) * k;
                    float gap = (lerp(13 * k, 1.2f * k, hv) + 1.3f * k * sin(at * 2.2f + p)) * (1f - c);
                    float dx = tx - bx, dy = ty - by;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    float ang = (float) Math.atan2(dy, dx);
                    float len = Math.max(2f, d - Math.max(0f, gap) / 2f);
                    Tentacle l = extra[p], r = extra[p + 3];
                    l.set(bx, by, ang, len, 1.9f * k, 0.45f).motion(0.2f, 1.4f, 1.6f, p * 2f + seed).curl(0f);
                    r.set(x + w - (bx - x), by, PI - ang, len, 1.9f * k, 0.45f).motion(0.2f, 1.4f, 1.6f, p * 2f + seed + 1f).curl(0f);
                    l.draw(g, at, hv, hv, 1f, 0f);
                    r.draw(g, at, hv, hv, 1f, 0f);
                    float spark = hv * (0.45f + 0.55f * (0.5f + 0.5f * sin(at * 9f + p * 2f)));
                    if (spark > 0.03f) {
                        glow(g, tx, ty, 6 * k, argb(0.6f * spark, 0xC680FF));
                        blob(g, tx, ty, 0.9f * k, argb(spark, 0xF2DDFF));
                    }
                }
                eye(g, cx, y - 0.5f * k, 5.5f * k, time, mx, my, true, 1f);
            }
            case SETTINGS -> {
                float gy = y - 4.5f * k;
                int m = 44;
                float rot = at * 0.2f;
                for (int i = 0; i < m; i++) {
                    float th = i / (float) m * TAU;
                    float tt = frac(th / TAU);
                    boolean wrapped = (tt > 0.08f && tt < 0.36f) || (tt > 0.55f && tt < 0.80f);
                    if (!wrapped) continue;
                    float a = th + rot;
                    float rr = (12.2f * k + sin(th * 7f + at * 2.5f) * 1.6f * k * (1f + hv)) * (1f - 0.2f * c);
                    float px = cx + cos(a) * rr, py = gy + sin(a) * rr;
                    if (py > y + 2f) continue;
                    float r = (1.25f + 0.4f * sin(th * 3f + seed)) * k;
                    blob(g, px, py, r, 0xFF1B0E2A);
                    float p = 0.5f + 0.5f * sin(at * 5f - th * 3f);
                    blob(g, px, py - r * 0.2f, Math.max(0.45f, r * 0.35f), argb(0.2f + 0.7f * hv * p * p, 0xB35CFF));
                }
                for (int side = 0; side < 2; side++) {
                    float sgn = side == 0 ? -1f : 1f;
                    Tentacle t = extra[side];
                    float bx = cx + sgn * 20 * k, by = y + 1.5f;
                    float ang = (float) Math.atan2(gy - 8 * k - by, cx + sgn * 9 * k - bx);
                    t.set(bx, by, ang, 17 * k, 2.2f * k, 0.5f).motion(0.22f, 1.2f, 1.4f, side * 3f + seed).curl(-sgn * 1.4f);
                    t.draw(g, at, hv, hv, lenMul, -sgn * 1.2f * c);
                }
                eye(g, cx, gy, 4.6f * k, time, mx, my, true, 1f);
            }
            case MODS -> {
                int idx = 0;
                for (int i = -3; i <= 3; i++) {
                    if (i == 0) continue;
                    Tentacle t = extra[idx++];
                    float bx = cx + i * 12.5f * k + (hash(i + seed) - 0.5f) * 4f * k, by = y + 1.5f;
                    float ang = -PI / 2f + i * 0.13f;
                    float len = (5f + 3.5f * hash(i * 7 + seed)) * k * (1f + 0.55f * hv);
                    t.set(bx, by, ang, len, 1.9f * k, 0.8f).motion(0.3f, 1.0f, 1.2f, i).curl(0f);
                    t.draw(g, at, hv, hv, lenMul, 0f);
                    float pulse = 0.5f + 0.5f * sin(at * 4f + i * 1.7f);
                    float cs = (0.8f + 0.4f * hv) * (1f + 0.07f * pulse) * (1f - 0.3f * c);
                    float chh = 9.5f * k * cs, cw = 4.8f * k * cs;
                    float ccx = t.tipX + cos(t.tipAngle) * chh * 0.35f, ccy = t.tipY + sin(t.tipAngle) * chh * 0.35f;
                    glow(g, ccx, ccy, 7 * k, argb(0.12f + 0.5f * hv * pulse, 0xB266FF));
                    sprite(g, Tex.CRYSTAL, ccx, ccy, cw, chh, t.tipAngle + PI / 2f, lerpColor(0xFF9C86C4, 0xFFFFFFFF, hv));
                }
                for (int i = -1; i <= 1; i += 2) {
                    sprite(g, Tex.CRYSTAL, cx + i * 6 * k, y - 1.5f * k, 2.6f * k, 5.2f * k, i * 0.35f,
                            lerpColor(0xFF7E6AA8, 0xFFE9D6FF, hv));
                }
                eye(g, cx, y - 1.5f * k, 5f * k, time, mx, my, false, 1f);
            }
            case QUIT -> {
                for (int i = 0; i < 8; i++) {
                    float s = (i + 0.5f) / 8f;
                    float bx = x + 18 * k + s * (w - 36 * k) + (hash(i * 5 + seed) - 0.5f) * 6f * k;
                    float by = y + h - 1.5f;
                    float rate = 0.22f * (0.7f + 0.6f * hash(i * 7 + seed));
                    float cyc = frac(at * rate + hash(i * 3 + seed));
                    float maxL = (5f + 6f * hash(i * 11 + seed)) * k;
                    float len;
                    float dropY = 0f, dropA = 0f;
                    if (cyc < 0.8f) {
                        len = maxL * (float) Math.pow(cyc / 0.8f, 1.6);
                    } else {
                        float q = (cyc - 0.8f) / 0.2f;
                        len = maxL * (1f - q) * 0.9f;
                        dropY = by + maxL + q * q * 30f * k;
                        dropA = 1f - q;
                    }
                    len *= (1f - 0.7f * c);
                    Tentacle t = extra[i];
                    t.set(bx, by, PI / 2f, Math.max(0.6f, len), 1.9f * k, 1.0f * k).motion(0.06f, 0.7f, 1f, i).curl(0f);
                    t.draw(g, at, hv * 0.6f, hv, 1f, 0f);
                    float bulb = 1.1f * k * (0.6f + 0.5f * Math.min(1f, cyc / 0.8f));
                    blob(g, t.tipX, t.tipY, bulb, 0xFF1B0E2A);
                    blob(g, t.tipX - bulb * 0.3f, t.tipY - bulb * 0.3f, Math.max(0.4f, bulb * 0.35f), argb(0.35f + 0.5f * hv, 0xC77DFF));
                    if (dropA > 0f) {
                        glow(g, bx, dropY, 3 * k, argb(0.35f * dropA * (0.3f + hv), 0x9B4DFF));
                        blob(g, bx, dropY, 1.1f * k, argb(dropA, 0x1B0E2A));
                        blob(g, bx - 0.3f, dropY - 0.4f, 0.45f, argb(0.8f * dropA, 0xC77DFF));
                    }
                }
                eye(g, cx, y - 1f * k, 5f * k, time, mx, my, false, 0.75f);
            }
            case BACK -> eye(g, cx, y - 1f * k, 4.5f * k, time, mx, my, true, 1f);
        }
    }
}
