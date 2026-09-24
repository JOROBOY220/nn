package dev.symbiot.menu.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.symbiot.menu.render.Draw.*;

/** Глаз/ядро симбиота. open: 0 — закрыт (тонкая щель), 1 — полностью раскрыт. */
public final class Eye {
    private Eye() {}

    public static void draw(GuiGraphicsExtractor g, float cx, float cy, float size, float open,
                            float lookX, float lookY, float time, boolean round) {
        open = clamp01(open);
        // мясистое «гнездо»
        ellipse(g, cx, cy, size * 1.0f, size * 0.66f, 0f, 0xFF12081C);
        ellipse(g, cx, cy - size * 0.08f, size * 0.86f, size * 0.52f, 0f, 0xFF231233);

        if (open > 0.04f) {
            float pulse = 0.85f + 0.15f * sin(time * 5f);
            glow(g, cx, cy, size * 1.9f, argb(0.45f * open * pulse, 0xA24BFF));
        }
        float oh = size * 0.46f * open;
        if (oh > 0.35f) {
            ellipse(g, cx, cy, size * 0.78f, oh, 0f, 0xFF3A0F5E);
            float ir = Math.min(size * 0.34f, oh * 1.05f);
            float ix = cx + clamp(lookX, -1f, 1f) * size * 0.25f;
            float iy = cy + clamp(lookY, -1f, 1f) * oh * 0.25f;
            ellipse(g, ix, iy, ir, Math.min(ir, oh), 0f, 0xFFC981FF);
            glow(g, ix, iy, ir * 1.4f, argb(0.6f * open, 0xE7C4FF));
            float pw = round ? ir * 0.42f : ir * 0.2f;
            ellipse(g, ix, iy, pw, Math.min(ir * 0.8f, oh * 0.9f), 0f, 0xFF0B0312);
            blob(g, ix - ir * 0.35f, iy - Math.min(ir, oh) * 0.4f, Math.max(0.5f, ir * 0.18f), 0xDDFFFFFF);
        }
        // веко / щель
        float lidA = 1f - smooth(open * 1.3f);
        if (lidA > 0.02f) {
            ellipse(g, cx, cy, size * 0.8f, Math.max(0.55f, size * 0.07f), 0f, argb(lidA, 0x8C45D9));
        }
    }
}
