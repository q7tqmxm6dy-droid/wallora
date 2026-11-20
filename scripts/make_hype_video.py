#!/usr/bin/env python3
"""Renders the high-energy Wallora edit: ~17 s, 1080x1920, every cut on the beat (128 BPM).

Sections (in beats, 1 beat = 0.469 s):
  0-4   hook: words slam in one by one
  4-12  7:00 AM vs 9:00 PM: the phone spins and the wallpaper flips from day to night
  12-20 rapid cuts through 4K wallpapers with zoom punches, RGB glitch and whip pans, a counter to 130+
  20-26 a grid of wallpapers flies in, then the camera dives into one
  26-31 pair maker: that photo splits into a light and a dark version
  31-36 logo burst and call to action

Two files are written: <out>.mp4 with a synthesised track, and <out>-sfx.mp4 with sound effects only, ready for a
TikTok sound to be added in the TikTok editor (TikTok sounds cannot be downloaded and re-uploaded).

Usage:
  make_hype_video.py <raw-dir> <out.mp4>
  make_hype_video.py <raw-dir> <out-dir> --stills 0.5 2 4 ...
<raw-dir>: the emulator captures used by make_store_shots.py (lock_light.png, lock_dark.png).
"""
import math, random, struct, subprocess, sys, wave
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops, ImageEnhance

W, H, FPS = 1080, 1920, 30
BPM = 128
BEAT = 60 / BPM
DUR = 36 * BEAT + 0.15
ROOT = Path(__file__).resolve().parent.parent
DRAW = ROOT / "app/src/main/res/drawable-nodpi"
RAW = Path(sys.argv[1])
FB = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FR = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"

def B(n): return n * BEAT
def font(size, path=FB): return ImageFont.truetype(path, size)
def clamp(x, a=0.0, b=1.0): return max(a, min(b, x))
def smooth(x): x = clamp(x); return x * x * (3 - 2 * x)
def lerp(a, b, t): return a + (b - a) * t
def lerp_c(a, b, t): return tuple(int(lerp(x, y, t)) for x, y in zip(a, b))
def out_back(x, k=2.2):
    x = clamp(x) - 1
    return 1 + (k + 1) * x ** 3 + k * x ** 2
def out_expo(x): x = clamp(x); return 1 - 2 ** (-10 * x) if x < 1 else 1.0
def in_out_cubic(x): x = clamp(x); return 4 * x ** 3 if x < 0.5 else 1 - (-2 * x + 2) ** 3 / 2

def vgrad(w, h, top, bottom):
    col = Image.new("RGB", (1, h))
    col.putdata([lerp_c(top, bottom, y / max(1, h - 1)) for y in range(h)])
    return col.resize((w, h))

# ------------------------------------------------------------------ images
_cache = {}
def wall(name, scale=1.25):
    """A wallpaper as a 'cover' image, a bit bigger than the frame so it can zoom and shake."""
    key = (name, scale)
    if key not in _cache:
        img = Image.open(DRAW / f"{name}.webp").convert("RGB")
        w, h = int(W * scale), int(H * scale)
        s = max(w / img.width, h / img.height)
        img = img.resize((int(img.width * s) + 1, int(img.height * s) + 1), Image.LANCZOS)
        x, y = (img.width - w) // 2, (img.height - h) // 2
        _cache[key] = img.crop((x, y, x + w, y + h))
    return _cache[key]

def zoomed(img, z, dx=0, dy=0):
    """Crop the centre of img so it appears zoomed by z (z=1 shows W*1.25 / 1.25 = the frame) and fit to W x H."""
    cw, ch = img.width / z, img.height / z
    cx = clamp(img.width / 2 + dx, cw / 2, img.width - cw / 2)
    cy = clamp(img.height / 2 + dy, ch / 2, img.height - ch / 2)
    box = (cx - cw / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2)
    return img.resize((W, H), Image.BILINEAR, box=box)

UHD = {"lux_noir_gold", "photo_tokyo_night", "dream_event_horizon", "viral_neon_rain", "lux_midnight_gold", "photo_milky_way"}
CUTS = ["photo_supercar_black", "scenic_10", "lux_noir_gold", "photo_tokyo_night", "dream_event_horizon", "nightcar_07", "viral_neon_rain", "scenic_16"]
GRID = ["scenic_08", "nightcar_03", "lux_midnight_gold", "trend_aura_dark", "photo_milky_way", "scenic_09",
        "viral_sakura_drive", "photo_lake_louise", "nightcar_11", "dream_amethyst_geode", "trend_cyber_grid_dark", "scenic_12",
        "photo_blossom", "lux_emerald_royale", "scenic_11", "photo_supercar_dusk", "trend_chrome_star_dark", "scenic_13",
        "photo_manhattan_night", "dream_ringed_giant", "nightcar_15", "viral_black_gold_marble", "scenic_15", "photo_aurora_hut"]
TARGET = GRID.index("photo_supercar_dusk")

# ------------------------------------------------------------------ text
_tcache = {}
def ktext(text, size, fill=(255, 255, 255), stroke=(0, 0, 0), sw=None, path=FB):
    key = (text, size, fill, stroke, path)
    if key not in _tcache:
        f = font(size, path)
        sw = sw if sw is not None else max(3, size // 14)
        d = ImageDraw.Draw(Image.new("L", (4, 4)))
        l, t, r, b = d.textbbox((0, 0), text, font=f, stroke_width=sw)
        img = Image.new("RGBA", (r - l + 20, b - t + 20), (0, 0, 0, 0))
        dd = ImageDraw.Draw(img)
        if stroke:
            sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
            ImageDraw.Draw(sh).text((10 - l + 4, 10 - t + 8), text, font=f, fill=(0, 0, 0, 150), stroke_width=sw, stroke_fill=(0, 0, 0, 150))
            img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(6)))
        dd.text((10 - l, 10 - t), text, font=f, fill=fill + (255,), stroke_width=sw if stroke else 0, stroke_fill=(stroke or (0, 0, 0)) + (255,))
        _tcache[key] = img
    return _tcache[key]

def pill(text, size=56, bg=(255, 255, 255), fg=(12, 12, 20)):
    key = ("pill", text, size, bg, fg)
    if key not in _tcache:
        f = font(size)
        d = ImageDraw.Draw(Image.new("L", (4, 4)))
        tw = d.textlength(text, font=f)
        w, h = int(tw + 80), int(size * 1.7)
        img = Image.new("RGBA", (w + 40, h + 40), (0, 0, 0, 0))
        sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle([20, 30, 20 + w, 30 + h], radius=h // 2, fill=(0, 0, 0, 120))
        img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(12)))
        dd = ImageDraw.Draw(img)
        dd.rounded_rectangle([20, 20, 20 + w, 20 + h], radius=h // 2, fill=bg + (255,))
        dd.text((20 + w / 2, 20 + h / 2), text, font=f, fill=fg + (255,), anchor="mm")
        _tcache[key] = img
    return _tcache[key]

def put(canvas, img, cx, cy, s=1.0, a=1.0, rot=0.0):
    """Paste img centred at (cx, cy), scaled, faded and rotated."""
    if s <= 0.01 or a <= 0.01: return
    if s != 1.0: img = img.resize((max(1, int(img.width * s)), max(1, int(img.height * s))), Image.BILINEAR)
    if rot: img = img.rotate(rot, expand=True, resample=Image.BICUBIC)
    if a < 1.0:
        img = img.copy(); img.putalpha(img.getchannel("A").point(lambda v: int(v * a)))
    canvas.paste(img, (int(cx - img.width / 2), int(cy - img.height / 2)), img)

# ------------------------------------------------------------------ effects
def rgb_split(img, amt):
    if amt < 1: return img
    r, g, b = img.split()
    return Image.merge("RGB", (ImageChops.offset(r, int(amt), 0), g, ImageChops.offset(b, -int(amt), int(amt * 0.3))))

def hblur(img, k):
    """Horizontal motion blur by squashing and stretching (cheap and looks like a whip pan)."""
    if k <= 1: return img
    return img.resize((max(8, int(W / k)), H), Image.BILINEAR).resize((W, H), Image.BILINEAR)

GRAIN = []
rg = random.Random(1)
for _ in range(4):
    n = Image.new("L", (270, 480))
    n.putdata([int(rg.gauss(128, 40)) for _ in range(270 * 480)])
    GRAIN.append(Image.merge("RGB", [n.resize((W, H), Image.BILINEAR)] * 3))

def finish(img, t):
    # camera shake on beats from 4 to 31, flashes on the big cuts, grain, vignette
    shake = 0.0
    nb = t / BEAT
    if 4 <= nb < 31:
        age = (nb - math.floor(nb)) * BEAT
        shake = 18 * math.exp(-age * 14)
    if shake > 0.5:
        rnd = random.Random(int(nb))
        dx, dy = rnd.uniform(-1, 1) * shake, rnd.uniform(-1, 1) * shake
        img = img.resize((W, H), Image.BILINEAR, box=(30 + dx, 53 + dy, W - 30 + dx, H - 53 + dy))
    for fb in (4, 12, 20, 26, 31):
        fl = 1 - clamp(abs(t - B(fb)) / 0.09)
        if fl > 0: img = Image.blend(img, Image.new("RGB", (W, H), (255, 255, 255)), 0.75 * fl)
    img = Image.blend(img, GRAIN[int(t * FPS) % 4], 0.045)
    return img

VIG = Image.new("L", (W, H), 0)
ImageDraw.Draw(VIG).ellipse([-260, -220, W + 260, H + 220], fill=255)
VIG = VIG.filter(ImageFilter.GaussianBlur(200)).point(lambda v: 120 + v * 135 // 255)
def vignette(img): return Image.composite(img, Image.new("RGB", (W, H), (0, 0, 0)), VIG)

# ------------------------------------------------------------------ phone
SCR = {k: Image.open(RAW / f"{k}.png").convert("RGB") for k in ("lock_light", "lock_dark")}
BW, BH, BEZ, RAD = 640, 1350, 18, 84
PW, PH = BW - 2 * BEZ, BH - 2 * BEZ
SCR_SMALL = {k: v.resize((PW, round(PW * v.height / v.width)), Image.LANCZOS) for k, v in SCR.items()}
ICON = Image.open(ROOT / "store/icon-512.png").convert("RGBA")

def phone_img(screen):
    ph = Image.new("RGBA", (BW, BH), (0, 0, 0, 0))
    d = ImageDraw.Draw(ph)
    d.rounded_rectangle([0, 0, BW - 1, BH - 1], radius=RAD + BEZ, fill=(14, 14, 22, 255), outline=(150, 150, 190, 255), width=4)
    box = Image.new("RGB", (PW, PH), (0, 0, 0))
    box.paste(screen, (0, (PH - screen.height) // 2))
    m = Image.new("L", (PW, PH), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, PW - 1, PH - 1], radius=RAD, fill=255)
    ph.paste(box, (BEZ, BEZ), m)
    d.ellipse([BW // 2 - 13, BEZ + 24, BW // 2 + 13, BEZ + 50], fill=(5, 5, 9, 255))
    return ph

def make_back():
    ph = Image.new("RGBA", (BW, BH), (0, 0, 0, 0))
    g = vgrad(BW, BH, (60, 40, 120), (20, 16, 50)).convert("RGBA")
    m = Image.new("L", (BW, BH), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, BW - 1, BH - 1], radius=RAD + BEZ, fill=255)
    ph.paste(g, (0, 0), m)
    ic = ICON.resize((200, 200), Image.LANCZOS)
    ph.paste(ic, (BW // 2 - 100, BH // 2 - 100), ic)
    ImageDraw.Draw(ph).rounded_rectangle([40, 40, 250, 300], radius=60, fill=(10, 10, 16, 255))
    return ph.transpose(Image.FLIP_LEFT_RIGHT)
PHONES = {k: phone_img(v) for k, v in SCR_SMALL.items()}
BACK = make_back()

def homography(dst, src):
    A, Bv = [], []
    for (x, y), (u, v) in zip(dst, src):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); Bv.append(u)
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y]); Bv.append(v)
    M = [r + [b] for r, b in zip(A, Bv)]
    for i in range(8):
        piv = max(range(i, 8), key=lambda r: abs(M[r][i]))
        M[i], M[piv] = M[piv], M[i]
        for r in range(8):
            if r != i:
                f = M[r][i] / M[i][i]
                M[r] = [a - f * b for a, b in zip(M[r], M[i])]
    return [M[i][8] / M[i][i] for i in range(8)]

def draw_phone(canvas, img, cx, cy, s, yaw, pitch=0.0, roll=0.0):
    ya, pa, ra = math.radians(yaw), math.radians(pitch), math.radians(roll)
    if abs(math.cos(ya)) < 0.04: return
    pts = []
    for (x, y) in ((-BW / 2, -BH / 2), (BW / 2, -BH / 2), (BW / 2, BH / 2), (-BW / 2, BH / 2)):
        x, y, z = x * s, y * s, 0.0
        x, z = x * math.cos(ya), -x * math.sin(ya)
        y, z = y * math.cos(pa) - z * math.sin(pa), y * math.sin(pa) + z * math.cos(pa)
        x, y = x * math.cos(ra) - y * math.sin(ra), x * math.sin(ra) + y * math.cos(ra)
        k = 2200 / (2200 + z)
        pts.append((cx + x * k, cy + y * k))
    coeff = homography(pts, [(0, 0), (BW, 0), (BW, BH), (0, BH)])
    lay = img.transform((W, H), Image.PERSPECTIVE, coeff, Image.BICUBIC)
    a = lay.getchannel("A")
    glow = a.resize((W // 4, H // 4)).filter(ImageFilter.GaussianBlur(14)).resize((W, H))
    canvas.paste(Image.new("RGB", (W, H), (0, 0, 0)), (0, 0), ImageChops.offset(glow, 10, 34).point(lambda v: v * 6 // 10))
    canvas.paste(lay, (0, 0), lay)

# ------------------------------------------------------------------ sections
def sec_hook(t):
    bg = zoomed(wall("scenic_10"), 1.0 + 0.06 * t / B(4))
    bg = ImageEnhance.Brightness(bg.filter(ImageFilter.GaussianBlur(6))).enhance(0.35)
    words = [("POV:", 0.0, (255, 214, 90)), ("your", 0.5, None), ("lock", 1.0, None), ("screen", 1.5, None),
             ("knows", 2.0, None), ("what", 2.5, None), ("time", 3.0, (255, 214, 90)), ("it is", 3.25, (255, 214, 90))]
    lines = [words[0:2], words[2:4], words[4:6], words[6:8]]
    for li, line in enumerate(lines):
        imgs = [ktext(w, 128, fill=c or (255, 255, 255)) for w, _, c in line]
        total = sum(i.width for i in imgs) - 10 * (len(imgs) - 1)
        x = W / 2 - total / 2
        for (w, at, c), im in zip(line, imgs):
            age = t - B(at)
            if age >= 0:
                s = out_back(age / 0.22, 2.6)
                put(bg, im, x + im.width / 2, 640 + li * 175 + (1 - clamp(age / 0.18)) * -60, s, clamp(age / 0.08))
            x += im.width - 10
    hint = clamp((t - B(3.5)) / 0.15)
    if hint > 0: put(bg, ktext("wait for it...", 52, fill=(220, 220, 255), sw=4, path=FR), W / 2, 1400, 1.0, hint)
    return bg

SUN, MOON = (255, 196, 70), (210, 220, 255)

def sun_moon(canvas, cx, cy, k, t):
    """Sun morphing into a moon (k: 0 = sun, 1 = moon)."""
    d = ImageDraw.Draw(canvas, "RGBA")
    col = lerp_c(SUN, MOON, k)
    r = 62
    if k < 0.98:
        for i in range(12):
            ang = i * math.pi / 6 + t * 1.5
            r1, r2 = r + 16, r + 38 * (1 - k)
            d.line([cx + math.cos(ang) * r1, cy + math.sin(ang) * r1, cx + math.cos(ang) * r2, cy + math.sin(ang) * r2], fill=col + (int(255 * (1 - k)),), width=9)
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([cx - r * 2, cy - r * 2, cx + r * 2, cy + r * 2], fill=col + (90,))
    canvas.paste(Image.alpha_composite(canvas.convert("RGBA"), glow.filter(ImageFilter.GaussianBlur(40))).convert("RGB"))
    d = ImageDraw.Draw(canvas, "RGBA")
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col + (255,))
    if k > 0:
        off = lerp(r * 2.2, r * 0.7, k)
        bgc = canvas.getpixel((int(cx + r * 1.9), int(cy - r * 0.9)))
        d.ellipse([cx - r + off, cy - r - 18 * k, cx + r + off, cy + r - 18 * k], fill=bgc + (255,))

def sec_dayNight(t):
    k = smooth((t - B(8.3)) / (BEAT * 0.6))            # 0 day, 1 night
    day = vgrad(W, H, (255, 190, 140), (255, 120, 160))
    night = vgrad(W, H, (12, 14, 50), (60, 26, 110))
    bg = Image.blend(day, night, k)
    if k > 0:
        d = ImageDraw.Draw(bg, "RGBA")
        rnd = random.Random(7)
        for _ in range(70):
            x, y, r = rnd.uniform(0, W), rnd.uniform(0, H * 0.7), rnd.uniform(1, 3.2)
            d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, int(200 * k * (0.5 + 0.5 * math.sin(t * 3 + x)))))
    sun_moon(bg, W / 2, 255, k, t)
    # the time label flips on the spin
    label = "7:00 AM" if k < 0.5 else "9:00 PM"
    lk = 1 - abs(k - 0.5) * 2 if 0 < k < 1 else 0
    put(bg, ktext(label, 150), W / 2, 450, 1.0 - 0.3 * lk, 1 - lk)
    # phone: bounces on beats, spins 360 on beat 8
    nb = t / BEAT
    bounce = math.exp(-((nb - math.floor(nb)) * BEAT) * 10) if nb >= 4.5 else 0
    spin = in_out_cubic((t - B(8)) / BEAT) * 360
    yaw = spin + 8 * math.sin(t * 2.2)
    side = math.cos(math.radians(yaw)) >= 0
    screen = "lock_dark" if spin >= 180 else "lock_light"
    img = PHONES[screen] if side else BACK
    canvas = bg
    entry = out_back((t - B(4)) / 0.35, 1.6)
    s = 0.78 * entry * (1 + 0.035 * bounce)
    sub = Image.new("RGB", (W, H)); sub.paste(canvas)
    draw_phone(sub, img, W / 2, 1170 + (1 - entry) * 500, s, yaw, 6, -2 + 2 * math.sin(t * 1.3))
    spinning = 0 < (t - B(8)) < BEAT
    if spinning:
        speed = math.sin(math.pi * clamp((t - B(8)) / BEAT))
        sub = Image.blend(sub, hblur(sub, 1 + 10 * speed), 0.6 * speed)
    canvas = sub
    # auto badge after the flip
    ba = out_back((t - B(10)) / 0.3)
    if ba > 0.02:
        put(canvas, pill("switches by itself", 50, bg=(124, 77, 255), fg=(255, 255, 255)), W / 2, 1790, ba)
    return canvas

def sec_cuts(t):
    i = int((t - B(12)) / BEAT)
    i = max(0, min(7, i))
    age = t - B(12 + i)
    name = CUTS[i]
    z = 1.0 + 0.16 * math.exp(-age * 7)
    img = zoomed(wall(name), z, dx=-40 * age, dy=0)
    # whip pan into cut 3 and 6: slide the old picture out and blur
    if i in (3, 6) and age < 0.16:
        p = age / 0.16
        prev = zoomed(wall(CUTS[i - 1]), 1.0)
        mix = Image.new("RGB", (W, H))
        mix.paste(prev, (int(-W * p), 0)); mix.paste(img, (int(W * (1 - p)), 0))
        img = hblur(mix, 14 * math.sin(math.pi * p) + 1)
    elif age < 0.1:
        img = rgb_split(img, 26 * (1 - age / 0.1))
    img = vignette(img)
    # counter that races to 130+
    p = clamp((t - B(12)) / (B(19) - B(12)))
    num = int(130 * out_expo(p))
    put(img, ktext(f"{num}{'+' if p >= 1 else ''}", 210, fill=(255, 255, 255)), W / 2, 330, 1.0 + 0.08 * math.exp(-age * 9))
    put(img, ktext("wallpapers", 72), W / 2, 485)
    if name in UHD:
        st = out_back(age / 0.2, 3.0)
        put(img, pill("4K", 76, bg=(0, 230, 190), fg=(5, 20, 20)), W - 210, 1580, st * 1.0, 1, rot=-12 * st)
    return img

TILE_W, TILE_H, GAP = 236, 420, 16
COLS, ROWS = 4, 6
GX0 = (W - (COLS * TILE_W + (COLS - 1) * GAP)) // 2
GY0 = (H - (ROWS * TILE_H + (ROWS - 1) * GAP)) // 2 + 40
_tiles = None
def tiles():
    global _tiles
    if _tiles is None:
        _tiles = []
        m = Image.new("L", (TILE_W, TILE_H), 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, TILE_W - 1, TILE_H - 1], radius=26, fill=255)
        for n in GRID:
            t_img = wall(n).resize((TILE_W, TILE_H), Image.LANCZOS).convert("RGBA")
            t_img.putalpha(m)
            _tiles.append(t_img)
    return _tiles

def grid_frame(t):
    bg = vgrad(W, H, (10, 10, 30), (34, 14, 60))
    rnd = random.Random(3)
    for k, tile in enumerate(tiles()):
        c, r = k % COLS, k // COLS
        tx = GX0 + c * (TILE_W + GAP) + TILE_W / 2
        ty = GY0 + r * (TILE_H + GAP) + TILE_H / 2
        ang = rnd.uniform(0, 2 * math.pi)
        dist = rnd.uniform(1200, 1700)
        delay = rnd.uniform(0, 0.45)
        p = out_back((t - B(20) - delay) / 0.5, 1.4)
        if p <= 0: continue
        x = lerp(tx + math.cos(ang) * dist, tx, p)
        y = lerp(ty + math.sin(ang) * dist, ty, p)
        rot = (1 - clamp(p)) * rnd.uniform(-70, 70)
        put(bg, tile, x, y, 1.0, 1.0, rot)
    return bg

def sec_grid(t):
    img = grid_frame(t)
    # caption
    ca = out_back((t - B(21)) / 0.3)
    if ca > 0.02: put(img, pill("10 categories", 64), W / 2, 150, ca)
    # dive into the target tile from beat 24 to 26
    z = in_out_cubic((t - B(24)) / B(2))
    if z > 0:
        c, r = TARGET % COLS, TARGET // COLS
        tx = GX0 + c * (TILE_W + GAP) + TILE_W / 2
        ty = GY0 + r * (TILE_H + GAP) + TILE_H / 2
        scale = lerp(1.0, W / TILE_W * 1.02, z)
        cx, cy = lerp(W / 2, tx, z), lerp(H / 2, ty, z)
        cw, ch = W / scale, H / scale
        img = img.resize((W, H), Image.BILINEAR, box=(cx - cw / 2, cy - ch / 2, cx + cw / 2, cy + ch / 2))
        if z > 0.75:   # swap in the full-quality photo at the end of the dive
            img = Image.blend(img, zoomed(wall("photo_supercar_dusk"), 1.0), smooth((z - 0.75) / 0.25))
        speed = math.sin(math.pi * z)
        if speed > 0.2:
            img = Image.blend(img, img.filter(ImageFilter.GaussianBlur(5)), 0.5 * speed)
    return img

_pair = None
def pair_imgs():
    global _pair
    if _pair is None:
        base = zoomed(wall("photo_supercar_dusk"), 1.0)
        light = ImageEnhance.Color(ImageEnhance.Brightness(base).enhance(1.45)).enhance(0.75)
        light = Image.blend(light, Image.new("RGB", (W, H), (255, 236, 220)), 0.18)
        dark = ImageEnhance.Brightness(base).enhance(0.42)
        dark = Image.blend(dark, Image.new("RGB", (W, H), (14, 20, 80)), 0.3)
        _pair = (base, light, dark)
    return _pair

def sec_pair(t):
    base, light, dark = pair_imgs()
    split_in = smooth((t - B(27)) / 0.3)
    keys = [(B(27), 1.0), (B(27.4), 0.5), (B(28), 0.28), (B(29), 0.72), (B(30), 0.5)]
    pos = 1.0
    for (t0, p0), (t1, p1) in zip(keys, keys[1:]):
        if t >= t0: pos = lerp(p0, p1, out_back((t - t0) / 0.3, 1.5)) if t < t1 else p1
    img = base.copy()
    if split_in > 0:
        x = int(W * pos)
        img = light.copy()
        img.paste(dark.crop((x, 0, W, H)), (x, 0))
        img = Image.blend(base, img, split_in)
        d = ImageDraw.Draw(img)
        d.rectangle([x - 4, 0, x + 4, H], fill=(255, 255, 255))
        d.ellipse([x - 46, H / 2 - 46, x + 46, H / 2 + 46], fill=(255, 255, 255))
        d.polygon([(x - 12, H / 2 - 16), (x - 28, H / 2), (x - 12, H / 2 + 16)], fill=(30, 30, 40))
        d.polygon([(x + 12, H / 2 - 16), (x + 28, H / 2), (x + 12, H / 2 + 16)], fill=(30, 30, 40))
        put(img, ktext("DAY", 70), max(110, x / 2), 1500, split_in)
        put(img, ktext("NIGHT", 70), min(W - 140, x + (W - x) / 2), 1500, split_in)
    a1 = out_back((t - B(26.2)) / 0.3); a2 = out_back((t - B(28)) / 0.3)
    put(img, ktext("Turn ANY photo", 104), W / 2, 300, a1)
    put(img, ktext("into a day & night pair", 76, fill=(255, 214, 90)), W / 2, 430, a2)
    return img

def sec_logo(t):
    age = t - B(31)
    bg = vgrad(W, H, (16, 10, 40), (70, 20, 100))
    d = ImageDraw.Draw(bg, "RGBA")
    # shockwave rings
    for k in range(3):
        a = age - k * 0.12
        if 0 < a < 1.0:
            r = 80 + 900 * out_expo(a / 1.0)
            al = int(220 * (1 - a / 1.0))
            d.ellipse([W / 2 - r, 760 - r, W / 2 + r, 760 + r], outline=(200, 170, 255, al), width=10 - 3 * k)
    # particles
    rnd = random.Random(8)
    for _ in range(90):
        ang = rnd.uniform(0, 2 * math.pi); sp = rnd.uniform(500, 1500); sz = rnd.uniform(3, 9)
        col = rnd.choice(((255, 214, 90), (180, 140, 255), (120, 220, 255), (255, 255, 255)))
        if age < 0: continue
        x = W / 2 + math.cos(ang) * sp * out_expo(age / 1.2) * 0.8
        y = 760 + math.sin(ang) * sp * out_expo(age / 1.2) * 0.8 + 300 * age * age
        al = int(255 * clamp(1 - age / 1.6))
        d.ellipse([x - sz, y - sz, x + sz, y + sz], fill=col + (al,))
    s = out_back(age / 0.45, 2.4)
    if s > 0.02:
        ic = ICON.resize((340, 340), Image.LANCZOS)
        m = Image.new("L", ic.size, 0)
        ImageDraw.Draw(m).rounded_rectangle([0, 0, 339, 339], radius=85, fill=255)
        ic.putalpha(m)
        put(bg, ic, W / 2, 760, s, 1, rot=(1 - clamp(age / 0.45)) * 30)
    # letters cascade
    word = "Wallora"
    whole = ktext(word, 170)
    f = font(170); dd = ImageDraw.Draw(Image.new("L", (4, 4)))
    edges = [0] + [dd.textlength(word[:k], font=f) for k in range(1, len(word))] + [whole.width]
    x0 = W / 2 - whole.width / 2
    for k in range(len(word)):
        l = int(edges[k] + (0 if k == 0 else 10)); r = int(edges[k + 1] + (10 if k < len(word) - 1 else 0))
        piece = whole.crop((l, 0, r, whole.height))
        la = age - 0.35 - k * 0.055
        if la > 0:
            put(bg, piece, x0 + (l + r) / 2, 1060 - 140 * (1 - out_back(la / 0.3, 2.0)), 1.0, clamp(la / 0.1))
    ca = out_back((t - B(33)) / 0.3)
    if ca > 0.02: put(bg, pill("Free on Google Play", 62), W / 2, 1290, ca)
    la = clamp((t - B(34)) / 0.3)
    if la > 0: put(bg, ktext("link in bio", 54, fill=(220, 220, 255), sw=4, path=FR), W / 2, 1420, 1.0, la)
    return bg

def frame(t):
    if t < B(4): img = sec_hook(t)
    elif t < B(12): img = sec_dayNight(t)
    elif t < B(20): img = sec_cuts(t)
    elif t < B(26): img = sec_grid(t)
    elif t < B(31): img = sec_pair(t)
    else: img = sec_logo(t)
    return finish(img, t)

# ------------------------------------------------------------------ audio (128 BPM)
SR = 44100
def synth_audio(path_full, path_sfx):
    n = int(SR * DUR)
    music, sfx = [0.0] * n, [0.0] * n
    def add(bus, t0, samples, gain=1.0):
        i0 = int(t0 * SR)
        for i, v in enumerate(samples):
            j = i0 + i
            if 0 <= j < n: bus[j] += v * gain
    def tone(freq, dur, kind="sine", attack=0.003, decay=3.0, sweep=None, detune=0.0):
        out = []; ph = ph2 = 0.0
        for i in range(int(dur * SR)):
            t = i / SR
            f = freq if sweep is None else freq + (sweep - freq) * min(1, t / dur * 3)
            ph += 2 * math.pi * f / SR; ph2 += 2 * math.pi * f * (1 + detune) / SR
            if kind == "sine": v = math.sin(ph)
            elif kind == "saw":
                v = ((ph / math.pi) % 2 - 1) * 0.5 + ((ph2 / math.pi) % 2 - 1) * 0.5
            else: v = 2 / math.pi * math.asin(math.sin(ph))
            out.append(v * min(1, t / attack) * math.exp(-decay * t))
        return out
    def lowpass(sig, cutoff):
        a = 2 * math.pi * cutoff / SR; k = a / (a + 1); y = 0.0; out = []
        for v in sig:
            y += k * (v - y); out.append(y)
        return out
    def noise(dur, lo, hi, shape=lambda x: math.sin(math.pi * x), seed=11):
        rnd = random.Random(seed); out = []; y = 0.0
        for i in range(int(dur * SR)):
            x = i / (dur * SR)
            a = 2 * math.pi * (lo + (hi - lo) * x) / SR
            y += (a / (a + 1)) * (rnd.uniform(-1, 1) - y)
            out.append(y * shape(x) * 4)
        return out
    kick = tone(150, 0.3, decay=9, sweep=45)
    clap = noise(0.16, 2600, 1800, lambda x: math.exp(-8 * x), 5)
    hat = noise(0.05, 9000, 9000, lambda x: math.exp(-16 * x), 6)
    prog = [(55.0, [220, 261.6, 329.6]), (43.65, [174.6, 220, 261.6]), (65.4, [261.6, 329.6, 392]), (49.0, [196, 246.9, 293.7])]
    stab_cache = {}
    for b in range(36):
        t = B(b)
        bar = b // 4
        root, chord = prog[bar % 4]
        drop = (4 <= b < 24) or (26 <= b < 31)
        if drop:
            add(music, t, kick, 0.95)
            if b % 2 == 1: add(music, t, clap, 0.5)
            add(music, t + BEAT / 2, hat, 0.3); add(music, t + BEAT / 4 * 3, hat, 0.15)
            add(music, t + BEAT / 2, lowpass(tone(root * 2, BEAT * 0.45, "saw", decay=6, detune=0.01), 900), 0.55)   # off-beat bass
            if b % 2 == 0:
                key = (root, tuple(chord))
                if key not in stab_cache:
                    st = [0.0] * int(BEAT * 0.9 * SR)
                    for f in chord:
                        for i, v in enumerate(tone(f, BEAT * 0.9, "saw", decay=5, detune=0.012)): st[i] += v
                    stab_cache[key] = lowpass(st, 2600)
                add(music, t + BEAT / 2, stab_cache[key], 0.12)
        elif b < 4:   # intro: soft pad and a pluck on every word
            if b == 0:
                for f in chord: add(music, 0, tone(f, B(4), "tri", attack=0.5, decay=0.4), 0.10)
        elif 24 <= b < 26:   # build-up: snare roll
            for q in range(4 if b == 24 else 8):
                add(music, t + q * BEAT / (4 if b == 24 else 8), clap, 0.25 + 0.2 * (b - 24))
    # risers before the drops
    add(music, B(2), noise(B(2), 200, 7000, lambda x: x ** 2, 9), 0.45)
    add(music, B(24), noise(B(2), 300, 8000, lambda x: x ** 2, 10), 0.5)
    add(music, B(29), noise(B(2), 300, 8000, lambda x: x ** 2, 12), 0.4)
    for b in range(31, 36):   # outro pad
        pass
    for f in (220, 261.6, 329.6, 440): add(music, B(31), tone(f, 2.6, "tri", attack=0.05, decay=1.2), 0.1)
    # --- sound effects (both versions)
    hook_times = [0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.25]
    for k, w in enumerate(hook_times): add(sfx, B(w), tone(300 + 60 * k, 0.12, decay=24, sweep=180), 0.45)
    boom = tone(70, 0.9, decay=4.5, sweep=32)
    for b in (4, 12, 20, 26, 31): add(sfx, B(b), boom, 0.9)
    add(sfx, B(8), noise(BEAT, 400, 6500, seed=21), 0.5)                          # spin whoosh
    for k, f in enumerate((1318.5, 659.25)): add(sfx, B(8.5), tone(f, 1.3, decay=3.0), 0.25)   # day->night chime
    for i in range(8):
        t = B(12 + i)
        if i in (3, 6): add(sfx, t - 0.05, noise(0.3, 600, 7000, seed=30 + i), 0.45)
        else: add(sfx, t, noise(0.09, 5000, 3000, lambda x: math.exp(-9 * x), 40 + i), 0.4)   # glitch tick
    for k in range(24): add(sfx, B(20) + k * 0.02, tone(900 + k * 30, 0.05, decay=40), 0.12)   # tiles landing
    add(sfx, B(24), noise(B(2), 200, 5000, lambda x: x ** 1.5, 50), 0.4)          # dive
    for b in (27.4, 28, 29, 30): add(sfx, B(b), noise(0.25, 1500, 400, seed=int(b * 10)), 0.3)  # slider swipes
    for k in range(7): add(sfx, B(31) + 0.35 + k * 0.055, tone(880 * (1 + k * 0.12), 0.25, decay=14), 0.16)  # letters
    for k, f in enumerate((659.25, 987.77, 1318.5, 1760.0)): add(sfx, B(33) + k * 0.08, tone(f, 1.5, decay=2.6), 0.16)
    def write(path, sig):
        peak = max(1e-6, max(abs(v) for v in sig)); fade = int(0.3 * SR)
        with wave.open(str(path), "wb") as wf:
            wf.setnchannels(1); wf.setsampwidth(2); wf.setframerate(SR)
            buf = bytearray()
            for i, v in enumerate(sig):
                g = 0.9 / peak
                if i > n - fade: g *= (n - i) / fade
                buf += struct.pack("<h", int(max(-1, min(1, v * g)) * 32767))
            wf.writeframes(bytes(buf))
    write(path_full, [m * 0.8 + s for m, s in zip(music, sfx)])
    write(path_sfx, sfx)

if __name__ == "__main__":
    out = Path(sys.argv[2])
    if "--stills" in sys.argv:
        out.mkdir(parents=True, exist_ok=True)
        for tt in sys.argv[sys.argv.index("--stills") + 1:]:
            frame(float(tt)).save(out / f"hype_{float(tt):05.2f}.png")
        sys.exit(0)
    import imageio_ffmpeg
    ff = imageio_ffmpeg.get_ffmpeg_exe()
    silent = out.with_name(out.stem + "-video.mp4")
    wav_full, wav_sfx = out.with_suffix(".wav"), out.with_name(out.stem + "-sfx.wav")
    synth_audio(wav_full, wav_sfx)
    cmd = [ff, "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
           "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "16", "-preset", "medium", "-profile:v", "high", str(silent)]
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stderr=subprocess.DEVNULL)
    total = int(DUR * FPS)
    for i in range(total):
        p.stdin.write(frame(i / FPS).tobytes())
        if i % 60 == 0: print(f"frame {i}/{total}", flush=True)
    p.stdin.close(); p.wait()
    for wav, dest in ((wav_full, out), (wav_sfx, out.with_name(out.stem + "-sfx.mp4"))):
        subprocess.run([ff, "-y", "-i", str(silent), "-i", str(wav), "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                        "-movflags", "+faststart", "-shortest", str(dest)], stderr=subprocess.DEVNULL, check=True)
        wav.unlink(missing_ok=True)
    silent.unlink(missing_ok=True)
    print("wrote", out)
