#!/usr/bin/env python3
"""
Генератор текстур для Symbiot Menu.
Все картинки создаются процедурно (без чужих ассетов), поэтому их можно
свободно перегенерировать/править: python3 tools/gen_assets.py
"""
import os, math, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "symbiotmenu", "textures", "gui")
os.makedirs(OUT, exist_ok=True)
W, H = 960, 540
rng = np.random.default_rng(1337)
random.seed(1337)

# ---------------------------------------------------------------- helpers
def value_noise(w, h, cell, seed):
    r = np.random.default_rng(seed)
    gw, gh = w // cell + 3, h // cell + 3
    g = r.random((gh, gw))
    ys = np.arange(h) / cell; xs = np.arange(w) / cell
    y0 = ys.astype(int); x0 = xs.astype(int)
    fy = ys - y0; fx = xs - x0
    fy = fy * fy * (3 - 2 * fy); fx = fx * fx * (3 - 2 * fx)
    a = g[y0][:, x0]; b = g[y0][:, x0 + 1]
    c = g[y0 + 1][:, x0]; d = g[y0 + 1][:, x0 + 1]
    top = a + (b - a) * fx[None, :]; bot = c + (d - c) * fx[None, :]
    return top + (bot - top) * fy[:, None]

def fbm(w, h, cell, octaves, seed):
    acc = np.zeros((h, w)); amp = 1.0; tot = 0
    for o in range(octaves):
        acc += value_noise(w, h, max(2, cell >> o), seed + o * 17) * amp
        tot += amp; amp *= 0.5
    return acc / tot

def lerp(a, b, t):
    return a + (b - a) * t

def hexc(h):
    h = h.lstrip('#'); return np.array([int(h[i:i+2], 16) for i in (0, 2, 4)], float)

def save(arr, name):
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)).save(os.path.join(OUT, name), optimize=True)
    print("saved", name)

def radial(w, h, cx, cy, r):
    ys, xs = np.mgrid[0:h, 0:w]
    return np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2) / r

MOON = (0.80 * W, 0.20 * H)

# ================================================================ SKY
def gen_sky():
    ys = np.linspace(0, 1, H)[:, None]
    stops = [(0.0, '#04030c'), (0.28, '#0f0a26'), (0.46, '#281444'),
             (0.56, '#6a2a5c'), (0.63, '#d8703e'), (0.67, '#f0a050'), (0.71, '#4a2040'), (1.0, '#0a0814')]
    img = np.zeros((H, W, 3))
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]; t1, c1 = stops[i + 1]
        m = (ys >= t0) & (ys <= t1)
        t = np.clip((ys - t0) / (t1 - t0), 0, 1)
        col = hexc(c0)[None, None, :] * (1 - t[..., None]) + hexc(c1)[None, None, :] * t[..., None]
        img = np.where(m[..., None], np.broadcast_to(col, (H, W, 3)), img)
    # moon halo
    d = radial(W, H, *MOON, 260)
    halo = np.clip(1 - d, 0, 1) ** 2.2
    img += halo[..., None] * hexc('#7a3cc8')[None, None, :] * 0.85
    sun = np.clip(1 - radial(W, H, W * 0.70, H * 0.66, 330), 0, 1) ** 2
    img += sun[..., None] * hexc('#ff8a3a') * 0.55
    # clouds
    n = fbm(W, H, 128, 5, 11)
    n2 = fbm(W, H, 64, 4, 99)
    band = np.exp(-((ys - 0.46) / 0.16) ** 2) + 0.6 * np.exp(-((ys - 0.18) / 0.10) ** 2)
    cl = np.clip((n * 0.7 + n2 * 0.3 - 0.44) * 4.0, 0, 1) * np.clip(band, 0, 1)
    lit = np.clip(1.2 - d * 0.7, 0.25, 1.2)
    ccol = lerp(hexc('#1c1236'), hexc('#b77ae0'), np.clip(lit - 0.3, 0, 1)[..., None])
    ccol = ccol * (0.75 + 0.5 * n2[..., None])
    low = np.exp(-((ys - 0.55) / 0.09) ** 2)
    ccol = ccol + low[..., None] * hexc('#c26a48') * 0.8
    img = img * (1 - cl[..., None] * 0.9) + ccol * cl[..., None] * 0.9
    # stars
    for _ in range(420):
        x = rng.integers(0, W); y = int(rng.random() ** 1.6 * H * 0.55)
        b = rng.random() ** 3
        s = 1 if b < 0.8 else 2
        c = lerp(hexc('#b9a8ff'), hexc('#ffffff'), rng.random())
        img[y:y+s, x:x+s] = img[y:y+s, x:x+s] * (1 - b) + c * b
    # pixelate slightly (minecraft feel): 2px blocks
    small = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).resize((W // 2, H // 2), Image.BILINEAR)
    img = np.array(small.resize((W, H), Image.NEAREST)).astype(float)
    save(img, "bg_sky.png")

# ================================================================ MOON (eclipse)
def gen_moon():
    S = 192
    img = np.zeros((S, S, 4))
    d = radial(S, S, S / 2, S / 2, S / 2)
    core = d < 0.42
    rim = np.clip(1 - np.abs(d - 0.44) / 0.05, 0, 1)
    corona = np.clip(1 - (d - 0.42) / 0.58, 0, 1) ** 2.2 * (d >= 0.42)
    ang = np.arctan2(*np.mgrid[0:S, 0:S][::-1] - S / 2)
    streak = np.clip(0.55 + 0.45 * np.sin(ang * 13) * np.sin(ang * 5 + 1) + (fbm(S, S, 12, 3, 8) - 0.5), 0, 1) * corona
    col = hexc('#a55cff')
    img[..., :3] = np.clip(col * (0.6 + streak)[..., None] + hexc('#ffe6ff') * (rim ** 2)[..., None], 0, 255)
    img[..., 3] = np.clip(streak * 255 + rim * 255, 0, 255)
    # dark disk with faint texture
    tex = fbm(S, S, 24, 3, 5)
    img[core, :3] = (hexc('#07040c') + tex[core][:, None] * 18)
    img[core, 3] = 255
    save(img, "moon.png")

# ================================================================ block-silhouette helpers
def blocky(mask_small, block, base, var, seed, top_col=None, edge_col=None, light_dir=None):
    """mask_small: bool array (h,w) at block resolution. returns RGBA at full res"""
    r = np.random.default_rng(seed)
    h, w = mask_small.shape
    col = np.zeros((h, w, 4))
    noise = r.random((h, w))
    c = base[None, None, :] * (1 - var / 2 + noise[..., None] * var)
    if top_col is not None:
        top = mask_small & ~np.roll(mask_small, 1, axis=0)
        c[top] = top_col * (0.85 + 0.3 * noise[top][:, None])
    if edge_col is not None:
        edge = mask_small & ~np.roll(mask_small, -1, axis=1)
        c[edge] = c[edge] * 0.4 + edge_col * 0.6
    col[..., :3] = c
    col[..., 3] = mask_small * 255
    im = Image.fromarray(np.clip(col, 0, 255).astype(np.uint8)).resize((w * block, h * block), Image.NEAREST)
    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    canvas.paste(im, (0, 0))
    if h * block < H:  # extend last row downwards
        last = im.crop((0, h * block - block, w * block, h * block))
        for yy in range(h * block, H, block):
            canvas.paste(last, (0, yy))
    return np.array(canvas).astype(float)

def over(dst, src):
    a = src[..., 3:4] / 255.0
    out = dst.copy()
    out[..., :3] = src[..., :3] * a + dst[..., :3] * (1 - a)
    out[..., 3:4] = np.maximum(dst[..., 3:4], src[..., 3:4]) if dst.shape[2] == 4 else 255
    out[..., 3:4] = src[..., 3:4] + dst[..., 3:4] * (1 - a)
    return out

# ================================================================ FAR LAYER (castle, islands, hills, water)
def gen_far():
    B = 4
    bw, bh = W // B, H // B
    layer = np.zeros((H, W, 4))
    ys, xs = np.mgrid[0:bh, 0:bw]

    # distant hills
    hills = np.zeros((bh, bw), bool)
    prof = fbm(bw, 1, 30, 4, 3)[0]
    for x in range(bw):
        top = int(bh * 0.60 - prof[x] * bh * 0.10)
        hills[top:, x] = True
    layer = over(layer, blocky(hills, B, hexc('#1c1430'), 0.25, 4, top_col=hexc('#2d1f47')))

    # castle
    castle = np.zeros((bh, bw), bool)
    windows = []
    cx0 = int(bw * 0.70)
    towers = [(0, 12, 0.14), (13, 7, 0.26), (-12, 8, 0.30), (24, 9, 0.34), (-22, 7, 0.40),
              (33, 6, 0.42), (-31, 6, 0.46), (6, 5, 0.22), (-6, 5, 0.24), (42, 7, 0.40),
              (18, 4, 0.20), (-17, 4, 0.33), (50, 5, 0.48), (-40, 5, 0.52), (28, 4, 0.28)]
    for off, tw, top in towers:
        x0 = cx0 + off - tw // 2; x1 = x0 + tw
        ty = int(bh * top)
        castle[ty:int(bh * 0.66), x0:x1] = True
        # spire
        sp = int(tw * 1.9)
        for k in range(sp):
            half = int((tw / 2) * (1 - k / sp) + 0.5)
            c = (x0 + x1) // 2
            castle[ty - k, max(0, c - half):c + half] = True
        castle[ty - sp - 3:ty - sp + 1, (x0 + x1) // 2] = True
        # battlements
        for xx in range(x0 - 1, x1 + 1, 2):
            castle[ty + 3, xx] = True
        for wy in range(ty + 6, int(bh * 0.64), 5):
            for wx in range(x0 + 1, x1 - 1, 3):
                if random.random() < 0.45:
                    windows.append((wx, wy))
    castle[int(bh * 0.52):int(bh * 0.66), cx0 - 34:cx0 + 44] = True  # walls
    for xx in range(cx0 - 34, cx0 + 44, 2):
        castle[int(bh * 0.52) - 1, xx] = True
    for wx in range(cx0 - 32, cx0 + 42, 4):
        if random.random() < 0.5: windows.append((wx, int(bh * 0.57)))
    lay = blocky(castle, B, hexc('#1a1030'), 0.35, 7, edge_col=hexc('#6a3aa8'))
    layer = over(layer, lay)
    for wx, wy in windows:
        c = hexc('#ffb34d') if random.random() < 0.8 else hexc('#c77dff')
        layer[wy * B:wy * B + B, wx * B:wx * B + B // 2 + 1, :3] = c
        layer[wy * B:wy * B + B, wx * B:wx * B + B // 2 + 1, 3] = 255

    # floating islands
    isl = np.zeros((bh, bw), bool); grass = np.zeros((bh, bw), bool)
    for (ix, iy, iw) in [(0.08, 0.20, 16), (0.30, 0.10, 9), (0.93, 0.34, 12), (0.70, 0.08, 7),
                         (0.42, 0.28, 6), (0.18, 0.40, 8), (0.98, 0.12, 8)]:
        x = int(ix * bw); y = int(iy * bh)
        depth = int(iw * 1.1)
        for k in range(depth):
            half = int(iw / 2 * (1 - (k / depth) ** 0.8) + random.random() * 1.5)
            isl[y + k, max(0, x - half):min(bw, x + half)] = True
        grass[y, max(0, x - iw // 2):min(bw, x + iw // 2)] = True
        # dangling roots
        for _ in range(3):
            rx = x + random.randint(-iw // 3, iw // 3)
            for k in range(random.randint(3, 8)):
                if 0 <= rx < bw: isl[y + depth + k - 2, rx] = True
    layer = over(layer, blocky(isl, B, hexc('#1a1128'), 0.4, 9, top_col=hexc('#2f4a2a')))

    # water / lake with reflections
    wy0 = int(H * 0.72)
    wat = np.zeros((H - wy0, W, 4))
    t = np.linspace(0, 1, H - wy0)[:, None]
    wat[..., :3] = lerp(hexc('#2a1838'), hexc('#07050d'), t[..., None])
    rip = fbm(W, H - wy0, 16, 3, 21)
    refl = np.exp(-((np.arange(W) - W * 0.70) / (W * 0.12)) ** 2)[None, :]
    streak = (np.sin(np.arange(H - wy0) * 1.3)[:, None] > 0.3) * refl * rip
    wat[..., :3] += streak[..., None] * hexc('#e0843c') * 0.55 * (1 - t[..., None])
    mref = np.exp(-((np.arange(W) - MOON[0]) / 40) ** 2)[None, :] * rip
    wat[..., :3] += mref[..., None] * hexc('#9a5cff') * 0.4
    wat[..., 3] = 255
    wat = np.array(Image.fromarray(np.clip(wat, 0, 255).astype(np.uint8)).resize((W // 2, (H - wy0) // 2)).resize((W, H - wy0), Image.NEAREST)).astype(float)
    layer[wy0:] = over(layer[wy0:], wat)

    # ruins on the shore (blocky)
    ru = np.zeros((bh, bw), bool)
    for x in range(0, bw):
        if random.random() < 0.18:
            hgt = random.randint(1, 7)
            ru[int(bh * 0.72) - hgt:int(bh * 0.74), x:x + random.randint(1, 3)] = True
    layer = over(layer, blocky(ru, B, hexc('#120c1c'), 0.3, 13, top_col=hexc('#26351f')))
    save(layer, "bg_far.png")

# ================================================================ NEAR LAYER (frames the menu)
def gen_near():
    B = 8
    bw, bh = W // B, (H + B - 1) // B
    layer = np.zeros((H, W, 4))
    m = np.zeros((bh, bw), bool)
    leaves = np.zeros((bh, bw), bool)
    # left cliff
    prof = fbm(1, bh, 6, 3, 31)[:, 0]
    for y in range(bh):
        wl = int(10 + prof[y] * 8 + (y / bh) ** 2 * 10)
        if y > bh * 0.25: m[y, :wl] = True
    # right ruined wall/pillar
    for y in range(bh):
        wr = int(6 + prof[bh - 1 - y] * 5 + (y / bh) ** 3 * 14)
        if y > bh * 0.45: m[y, bw - wr:] = True
    m[int(bh * 0.30):, bw - 18:bw - 15] = True  # pillar
    m[int(bh * 0.28):int(bh * 0.31), bw - 20:bw - 13] = True
    # bottom ground
    for x in range(bw):
        m[bh - 3 - int(prof[x % bh] * 2):, x] = True
    # tree canopy top-left
    for (cx, cy, r) in [(6, 8, 9), (14, 4, 7), (2, 16, 7), (20, 10, 5), (bw - 4, 3, 6), (bw - 10, 1, 5)]:
        for y in range(max(0, cy - r), min(bh, cy + r)):
            for x in range(max(0, cx - r), min(bw, cx + r)):
                if (x - cx) ** 2 + (y - cy) ** 2 < r * r * (0.7 + random.random() * 0.4):
                    leaves[y, x] = True
    trunk = np.zeros((bh, bw), bool)
    trunk[10:int(bh * 0.4), 9:11] = True
    layer = over(layer, blocky(m, B, hexc('#0d0914'), 0.45, 41, top_col=hexc('#1c2a17'), edge_col=hexc('#2b1740')))
    layer = over(layer, blocky(trunk, B, hexc('#1a110c'), 0.3, 42))
    layer = over(layer, blocky(leaves, B, hexc('#0d0f12'), 0.7, 43, top_col=hexc('#1a2416')))
    # vignette-ish dark gradient at bottom
    save(layer, "bg_near.png")

# ================================================================ FOG (tileable)
def gen_fog():
    w, h = 512, 96
    n = fbm(w, h, 64, 4, 71)
    n = (n + np.roll(n, w // 2, axis=1)) / 2  # soften seam
    xs = np.linspace(0, 2 * np.pi, w)
    blend = (np.sin(xs)[None, :] + 1) / 2
    n = n * blend + np.roll(n, w // 2, axis=1) * (1 - blend)
    ys = np.linspace(-1, 1, h)[:, None]
    a = np.clip((n - 0.35) * 2.5, 0, 1) * np.exp(-ys ** 2 * 2.5)
    img = np.zeros((h, w, 4))
    img[..., :3] = 255
    img[..., 3] = a * 170
    save(img, "fog.png")

# ================================================================ sprites
def gen_blob():
    S = 32
    d = radial(S, S, S / 2 - 0.5, S / 2 - 0.5, S / 2)
    a = np.clip((1 - d) * 6, 0, 1)
    shade = np.clip(1.0 - 0.45 * d ** 3, 0.5, 1)
    img = np.zeros((S, S, 4))
    img[..., :3] = 255 * shade[..., None]
    img[..., 3] = a * 255
    save(img, "blob.png")
    g = np.clip(1 - d, 0, 1) ** 2
    img = np.zeros((S * 2, S * 2, 4))
    d2 = radial(S * 2, S * 2, S - 0.5, S - 0.5, S)
    g = np.clip(1 - d2, 0, 1) ** 2.2
    img[..., :3] = 255; img[..., 3] = g * 255
    save(img, "glow.png")

def gen_gear():
    S = 64
    im = Image.new("RGBA", (S * 4, S * 4), (0, 0, 0, 0))
    dr = ImageDraw.Draw(im)
    c = S * 2; R = S * 1.55; r = S * 1.25; teeth = 10
    pts = []
    for i in range(teeth * 4):
        a = i / (teeth * 4) * 2 * math.pi
        rr = R if (i % 4) in (1, 2) else r
        pts.append((c + math.cos(a) * rr, c + math.sin(a) * rr))
    dr.polygon(pts, fill=(120, 116, 130, 255))
    dr.ellipse([c - r * 0.82, c - r * 0.82, c + r * 0.82, c + r * 0.82], fill=(88, 84, 98, 255))
    for i in range(6):
        a = i / 6 * 2 * math.pi
        x = c + math.cos(a) * r * 0.55; y = c + math.sin(a) * r * 0.55
        dr.ellipse([x - 14, y - 14, x + 14, y + 14], fill=(0, 0, 0, 0))
    dr.ellipse([c - 30, c - 30, c + 30, c + 30], fill=(140, 136, 150, 255))
    dr.ellipse([c - 14, c - 14, c + 14, c + 14], fill=(0, 0, 0, 0))
    im = im.resize((S, S), Image.LANCZOS)
    a = np.array(im).astype(float)
    ys, xs = np.mgrid[0:S, 0:S]
    shade = 0.75 + 0.45 * ((S - xs - ys) / (2 * S))
    a[..., :3] *= shade[..., None]
    save(a, "gear.png")

def gen_crystal():
    w, h = 16, 32
    im = Image.new("RGBA", (w * 4, h * 4), (0, 0, 0, 0))
    dr = ImageDraw.Draw(im)
    W4, H4 = w * 4, h * 4
    dr.polygon([(W4 / 2, 0), (W4, H4 * 0.3), (W4 * 0.8, H4), (W4 * 0.2, H4), (0, H4 * 0.3)], fill=(150, 70, 255, 255))
    dr.polygon([(W4 / 2, 0), (W4, H4 * 0.3), (W4 * 0.8, H4), (W4 / 2, H4)], fill=(95, 30, 190, 255))
    dr.polygon([(W4 / 2, 0), (W4 * 0.2, H4 * 0.35), (W4 * 0.45, H4 * 0.9)], fill=(230, 190, 255, 255))
    im = im.resize((w, h), Image.LANCZOS)
    save(np.array(im).astype(float), "crystal.png")

def gen_button():
    w, h = 256, 40
    base = fbm(w, h, 8, 3, 81)
    img = np.zeros((h, w, 4))
    img[..., :3] = lerp(hexc('#15111d'), hexc('#2a2233'), base[..., None])
    ys, xs = np.mgrid[0:h, 0:w]
    # beveled plate shape: cut corners (octagon)
    cut = 8
    inside = (xs + ys >= cut) & ((w - 1 - xs) + ys >= cut) & (xs + (h - 1 - ys) >= cut) & ((w - 1 - xs) + (h - 1 - ys) >= cut)
    border = inside & ~((xs + ys >= cut + 3) & ((w - 1 - xs) + ys >= cut + 3) & (xs + (h - 1 - ys) >= cut + 3)
                        & ((w - 1 - xs) + (h - 1 - ys) >= cut + 3) & (xs > 2) & (xs < w - 3) & (ys > 2) & (ys < h - 3))
    img[..., :3] *= (1.15 - ys[..., None] / h * 0.5)
    img[border, :3] = lerp(hexc('#3d3547'), hexc('#6b5d7a'), base[border][:, None])
    img[border & (ys < h / 2), :3] *= 1.25
    # cracks
    cr = fbm(w, h, 16, 3, 83)
    crack = (np.abs(cr - 0.5) < 0.012) & inside & ~border
    img[crack, :3] = hexc('#0a0710')
    # rivets
    for rx in (10, w - 11):
        for ry in (h // 2,):
            m = (xs - rx) ** 2 + (ys - ry) ** 2 <= 5
            img[m, :3] = hexc('#8a7f95'); img[(xs - rx + 1) ** 2 + (ys - ry + 1) ** 2 <= 1, :3] = hexc('#d0c6d8')
    img[..., 3] = inside * 255
    save(img, "button.png")

def gen_logo():
    font_path = "/usr/share/fonts/opentype/noto/NotoSerifCJK-Black.ttc"
    text = "SYMBIOT"
    Wl, Hl = 1024, 300
    f = ImageFont.truetype(font_path, 190)
    mask = Image.new("L", (Wl, Hl), 0)
    dr = ImageDraw.Draw(mask)
    bb = dr.textbbox((0, 0), text, font=f)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    ox = (Wl - tw) // 2 - bb[0]; oy = 40 - bb[1]
    dr.text((ox, oy), text, font=f, fill=255)
    m = np.array(mask).astype(float) / 255
    # organic displacement
    dx = (fbm(Wl, Hl, 24, 3, 101) - 0.5) * 14
    dy = (fbm(Wl, Hl, 24, 3, 202) - 0.5) * 14
    ys, xs = np.mgrid[0:Hl, 0:Wl]
    sx = np.clip(xs + dx, 0, Wl - 1).astype(int); sy = np.clip(ys + dy, 0, Hl - 1).astype(int)
    m = m[sy, sx]
    # drips & spikes
    mi = Image.fromarray((m * 255).astype(np.uint8))
    dr = ImageDraw.Draw(mi)
    r = random.Random(5)
    arr = np.array(mi)
    cols = [x for x in range(0, Wl, 3) if arr[:, x].max() > 128]
    for _ in range(46):
        x = r.choice(cols)
        col = np.where(arr[:, x] > 128)[0]
        yb = col.max(); length = r.randint(10, 60); wdt = r.randint(3, 8)
        dr.polygon([(x - wdt, yb - 4), (x + wdt, yb - 4), (x + 1, yb + length), (x - 1, yb + length)], fill=255)
        dr.ellipse([x - wdt * 0.7, yb + length - wdt, x + wdt * 0.7, yb + length + wdt * 0.4], fill=255)
    for _ in range(30):
        x = r.choice(cols)
        col = np.where(arr[:, x] > 128)[0]
        yt = col.min(); length = r.randint(12, 45); wdt = r.randint(3, 7)
        lean = r.randint(-18, 18)
        dr.polygon([(x - wdt, yt + 6), (x + wdt, yt + 6), (x + lean, yt - length)], fill=255)
    mi = mi.filter(ImageFilter.GaussianBlur(1.2))
    m = np.clip((np.array(mi).astype(float) / 255 - 0.4) * 4, 0, 1)
    # shading: rim light from top-right, veins
    edge = np.clip(m - np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(7))).astype(float) / 255, 0, 1)
    shiftm = np.roll(np.roll(m, 4, 0), -4, 1)
    rim = np.clip(m - shiftm, 0, 1)
    tex = fbm(Wl, Hl, 20, 4, 303)
    veins = np.clip(1 - np.abs(fbm(Wl, Hl, 40, 3, 404) - 0.5) * 30, 0, 1)
    body = lerp(hexc('#b9adc9'), hexc('#efe7ff'), tex[..., None])
    body = body * (0.55 + 0.45 * (1 - ys[..., None] / Hl * 1.2))
    body = lerp(body, hexc('#1a0f26'), np.clip(ys / Hl * 1.6 - 0.55, 0, 1)[..., None])
    body = lerp(body, hexc('#b56cff'), (veins * 0.7)[..., None])
    body = lerp(body, hexc('#ffffff'), rim[..., None])
    body = lerp(body, hexc('#2a1540'), (edge * 0.7)[..., None])
    img = np.zeros((Hl, Wl, 4))
    img[..., :3] = body
    img[..., 3] = m * 255
    # dark outline + purple aura baked
    aura = np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(10))).astype(float) / 255
    outline = np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(7))).astype(float) / 255
    under = np.zeros_like(img)
    under[..., :3] = lerp(hexc('#7a2cff'), hexc('#07030c'), outline[..., None])
    under[..., 3] = np.clip(aura * 170 + outline * 255, 0, 255)
    img = over(under, img)
    # crop
    alpha = img[..., 3]
    ys_, xs_ = np.where(alpha > 4)
    y0, y1, x0, x1 = ys_.min(), ys_.max() + 1, xs_.min(), xs_.max() + 1
    img = img[y0:y1, x0:x1]
    save(img, "logo.png")
    # energy mask (white where letters are) for the sweeping energy effect
    en = np.zeros_like(img)
    en[..., :3] = 255
    mm = m[y0:y1, x0:x1]
    en[..., 3] = np.clip(mm * veins[y0:y1, x0:x1] * 255 * 1.4 + rim[y0:y1, x0:x1] * 255, 0, 255)
    save(en, "logo_energy.png")
    print("logo size", img.shape)

def gen_icon():
    S = 128
    img = np.zeros((S, S, 4))
    d = radial(S, S, S / 2, S / 2, S / 2)
    img[..., :3] = lerp(hexc('#3b1466'), hexc('#07030c'), np.clip(d, 0, 1)[..., None])
    img[..., 3] = (d < 1) * 255
    eye = radial(S, S * 2, S / 2, S, S * 0.35)[::2]
    iris = (np.abs(np.mgrid[0:S, 0:S][1] - S / 2) < 5) & (d < 0.3)
    img[d < 0.34, :3] = hexc('#b46cff'); img[iris, :3] = hexc('#07030c')
    save(img, "../../icon.png")

if __name__ == "__main__":
    # фон теперь нарисованный (master.png); процедурные слои оставлены для истории
    gen_fog()
    gen_blob(); gen_gear(); gen_crystal(); gen_button(); gen_logo(); gen_icon()
