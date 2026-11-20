#!/usr/bin/env python3
"""Renders the Wallora promo video: a 20 second 1080x1920 motion-graphics ad in the "POV" style.

A phone floats in a dark, neon-lit scene and its screen shows the real app (emulator captures): the wallpaper
switching from day to night, browsing, previewing and making a pair. Captions pop in as white labels.
Music and sound effects are synthesised here (no third-party audio), so the video is free to post anywhere.

Usage:
  make_promo_video.py <raw-dir> <out.mp4>            full render
  make_promo_video.py <raw-dir> <out-dir> --stills 1.0 3.5 5 ...   PNG stills at those times (for checking)
<raw-dir> holds the 1080x2340 captures used by make_store_shots.py (lock_light, lock_dark, home, detail_pair,
discover, home_preview, create) plus icon-512.png (taken from ../store).
"""
import math, random, struct, subprocess, sys, wave
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops

W, H, FPS, DUR = 1080, 1920, 30, 20.0
FONT_B = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
RAW = Path(sys.argv[1])
ICON = Path(__file__).resolve().parent.parent / "store" / "icon-512.png"

def font(size): return ImageFont.truetype(FONT_B, size)
def clamp(x, a=0.0, b=1.0): return max(a, min(b, x))
def smooth(x): x = clamp(x); return x * x * (3 - 2 * x)
def lerp(a, b, t): return a + (b - a) * t
def out_back(x, k=1.9):
    x = clamp(x) - 1
    return 1 + (k + 1) * x ** 3 + k * x ** 2

# ---------------------------------------------------------------- screens
def load(name): return Image.open(RAW / f"{name}.png").convert("RGB")
SHOTS = {n: load(n) for n in ("lock_light", "lock_dark", "home", "detail_pair", "discover", "home_preview", "create")}
SW, SH = 1080, 2340

# when each app screen is shown (start time); the previous one is pushed out over 0.4 s
SEQUENCE = [("lock", 1.8), ("home", 6.7), ("detail_pair", 9.1), ("discover", 11.5), ("home_preview", 14.0), ("create", 15.6)]
TAPS = [(8.45, 540, 990), (12.9, 281, 1144), (16.2, 518, 540)]   # (time, x, y) on the screen, a ripple each
PUSH = 0.4

def lock_image(t):
    """Day lock screen that wipes to the night one between 4.5 s and 5.4 s."""
    p = smooth((t - 4.5) / 0.9)
    if p <= 0: return SHOTS["lock_light"]
    if p >= 1: return SHOTS["lock_dark"]
    feather = 420
    edge = p * (SH + feather)
    col = Image.new("L", (1, SH))
    col.putdata([int(255 * clamp((edge - y) / feather)) for y in range(SH)])
    mask = col.resize((SW, SH), Image.NEAREST)
    return Image.composite(SHOTS["lock_dark"], SHOTS["lock_light"], mask)

def screen_image(name, t):
    return lock_image(t) if name == "lock" else SHOTS[name]

def draw_day_night_bar(img, t):
    """A glass bar with a sun, a moon and a knob that slides from day to night."""
    a = smooth((t - 2.9) / 0.4) * (1 - smooth((t - 6.2) / 0.4))
    if a <= 0: return img
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    x0, x1, y, h = 150, 930, 2020, 118
    d.rounded_rectangle([x0, y, x1, y + h], radius=h // 2, fill=(255, 255, 255, int(70 * a)), outline=(255, 255, 255, int(120 * a)), width=3)
    p = smooth((t - 4.5) / 0.9)
    kx = lerp(x0 + 59, x1 - 59, p)
    cy = y + h // 2
    # sun
    sun_a = int(255 * a * (1 - 0.6 * p))
    d.ellipse([x0 + 38, cy - 21, x0 + 80, cy + 21], fill=(255, 205, 80, sun_a))
    for k in range(8):
        ang = k * math.pi / 4
        d.line([x0 + 59 + math.cos(ang) * 30, cy + math.sin(ang) * 30, x0 + 59 + math.cos(ang) * 40, cy + math.sin(ang) * 40], fill=(255, 205, 80, sun_a), width=5)
    # moon
    moon_a = int(255 * a * (0.4 + 0.6 * p))
    d.ellipse([x1 - 80, cy - 24, x1 - 36, cy + 24], fill=(210, 220, 255, moon_a))
    d.ellipse([x1 - 66, cy - 30, x1 - 22, cy + 18], fill=(40, 40, 80, 0))
    d.ellipse([x1 - 68, cy - 28, x1 - 28, cy + 12], fill=(20, 24, 60, int(200 * a)))
    # knob
    d.ellipse([kx - 44, cy - 44, kx + 44, cy + 44], fill=(255, 255, 255, int(235 * a)))
    return Image.alpha_composite(img.convert("RGBA"), lay).convert("RGB")

def ripple(img, t):
    lay = None
    for (tt, x, y) in TAPS:
        age = t - tt
        if 0 <= age < 0.6:
            if lay is None: lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
            d = ImageDraw.Draw(lay)
            r = 20 + 150 * smooth(age / 0.6)
            al = int(220 * (1 - age / 0.6))
            d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, al // 3), outline=(255, 255, 255, al), width=6)
            d.ellipse([x - 22, y - 22, x + 22, y + 22], fill=(255, 255, 255, al))
    return img if lay is None else Image.alpha_composite(img.convert("RGBA"), lay).convert("RGB")

def screen_at(t):
    """What the phone screen shows at time t (1080x2340 RGB)."""
    if t < SEQUENCE[0][1]:
        return Image.new("RGB", (SW, SH), (3, 5, 12))
    idx = max(i for i, (_, s) in enumerate(SEQUENCE) if t >= s)
    name, start = SEQUENCE[idx]
    cur = screen_image(name, t)
    if idx > 0 and t - start < PUSH:
        p = smooth((t - start) / PUSH)
        prev = screen_image(SEQUENCE[idx - 1][0], t)
        canvas = Image.new("RGB", (SW, SH))
        canvas.paste(Image.eval(prev, lambda v: int(v * (1 - 0.45 * p))), (int(-SW * 0.28 * p), 0))
        canvas.paste(cur, (int(SW * (1 - p)), 0))
        img = canvas
    else:
        img = cur
    if t < SEQUENCE[0][1] + 0.25:  # screen waking up
        img = Image.eval(img, lambda v, k=smooth((t - SEQUENCE[0][1]) / 0.25): int(v * k))
    img = draw_day_night_bar(img, t)
    return ripple(img, t)

# ---------------------------------------------------------------- phone and camera
BW, BH, BEZ, RAD = 700, 1480, 20, 92
PW, PH = BW - 2 * BEZ, BH - 2 * BEZ     # screen area (660 x 1440), a little taller than the capture

def make_body():
    body = Image.new("RGBA", (BW, BH), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle([0, 0, BW - 1, BH - 1], radius=RAD + BEZ, fill=(16, 16, 26, 255), outline=(96, 98, 130, 255), width=4)
    d.rounded_rectangle([5, 5, BW - 6, BH - 6], radius=RAD + BEZ - 4, outline=(40, 42, 62, 255), width=2)
    return body
BODY = make_body()
SCREEN_MASK = Image.new("L", (PW, PH), 0)
ImageDraw.Draw(SCREEN_MASK).rounded_rectangle([0, 0, PW - 1, PH - 1], radius=RAD, fill=255)

def gloss(t):
    """A soft highlight that sweeps across the glass now and then."""
    g = Image.new("RGBA", (PW, PH), (0, 0, 0, 0))
    d = ImageDraw.Draw(g)
    for k, at in enumerate((2.0, 7.0, 12.5)):
        x = lerp(-PW * 0.4, PW * 1.4, smooth((t - at) / 0.9))
        if 0 < t - at < 0.9:
            d.polygon([(x, 0), (x + 120, 0), (x - 160, PH), (x - 280, PH)], fill=(255, 255, 255, 38))
    return g

def phone_layer(t):
    scr = screen_at(t).resize((PW, round(PW * SH / SW)), Image.LANCZOS)
    box = Image.new("RGB", (PW, PH), (0, 0, 0))
    box.paste(scr, (0, (PH - scr.height) // 2))
    ph = BODY.copy()
    box = box.convert("RGBA")
    box.alpha_composite(gloss(t))
    ph.paste(box, (BEZ, BEZ), SCREEN_MASK)
    # punch-hole camera
    ImageDraw.Draw(ph).ellipse([BW // 2 - 14, BEZ + 26, BW // 2 + 14, BEZ + 54], fill=(6, 6, 10, 255))
    return ph

# camera keyframes: (t, centre x, centre y, scale, yaw, pitch, roll)
CAM = [
    (0.0, 560, 1190, 0.74, -30, 46, -14),
    (1.8, 548, 1170, 0.78, -26, 40, -11),
    (3.0, 540, 1150, 0.92, -8, 8, -2),
    (6.0, 540, 1140, 0.95, 7, 4, 1.5),
    (9.0, 540, 1140, 0.96, -5, 3, -1),
    (12.0, 540, 1140, 0.96, 6, 4, 1),
    (15.5, 540, 1140, 0.96, -4, 3, -1),
    (17.0, 540, 1170, 0.92, 0, 0, 0),
    (17.8, 540, 2500, 0.8, 0, 12, 0),
]
def camera(t):
    for (t0, *a), (t1, *b) in zip(CAM, CAM[1:]):
        if t0 <= t <= t1:
            k = smooth((t - t0) / (t1 - t0))
            return [lerp(x, y, k) for x, y in zip(a, b)]
    return CAM[-1][1:] if t > CAM[-1][0] else CAM[0][1:]

def homography(dst, src):
    """Coefficients for PIL's PERSPECTIVE transform: output points dst map to input points src."""
    A, B = [], []
    for (x, y), (u, v) in zip(dst, src):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); B.append(u)
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y]); B.append(v)
    n = 8
    M = [row + [b] for row, b in zip(A, B)]
    for i in range(n):
        piv = max(range(i, n), key=lambda r: abs(M[r][i]))
        M[i], M[piv] = M[piv], M[i]
        for r in range(n):
            if r != i:
                f = M[r][i] / M[i][i]
                M[r] = [a - f * b for a, b in zip(M[r], M[i])]
    return [M[i][n] / M[i][i] for i in range(n)]

def project(cx, cy, s, yaw, pitch, roll):
    f = 2000.0
    ya, pa, ra = (math.radians(v) for v in (yaw, pitch, roll))
    pts = []
    for (x, y) in ((-BW / 2, -BH / 2), (BW / 2, -BH / 2), (BW / 2, BH / 2), (-BW / 2, BH / 2)):
        x, y, z = x * s, y * s, 0.0
        x, z = x * math.cos(ya) + z * math.sin(ya), -x * math.sin(ya) + z * math.cos(ya)
        y, z = y * math.cos(pa) - z * math.sin(pa), y * math.sin(pa) + z * math.cos(pa)
        x, y = x * math.cos(ra) - y * math.sin(ra), x * math.sin(ra) + y * math.cos(ra)
        k = f / (f + z)
        pts.append((cx + x * k, cy + y * k))
    return pts

# ---------------------------------------------------------------- background
def make_background():
    w, h = W + 160, H + 160
    bg = Image.new("RGB", (w, h))
    px = []
    for y in range(h):
        k = y / (h - 1)
        px.append((int(lerp(7, 16, k)), int(lerp(10, 12, k)), int(lerp(30, 58, k))))
    col = Image.new("RGB", (1, h)); col.putdata(px)
    bg = col.resize((w, h))
    glow = Image.new("RGB", (w, h), (0, 0, 0))
    d = ImageDraw.Draw(glow)
    for (x, y, r, c) in ((200, 500, 340, (40, 80, 255)), (1000, 700, 300, (150, 50, 255)), (300, 1500, 380, (30, 140, 255)),
                         (950, 1750, 340, (210, 60, 220)), (560, 120, 300, (60, 90, 255)), (540, 1100, 420, (70, 50, 190))):
        d.ellipse([x - r, y - r, x + r, y + r], fill=c)
    glow = glow.filter(ImageFilter.GaussianBlur(150))
    bg = ImageChops.add(bg, Image.eval(glow, lambda v: int(v * 0.55)))
    # a faint floor line of light, like the desk edge in the reference
    d = ImageDraw.Draw(bg)
    return bg
BG = make_background()

def vignette():
    v = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(v)
    d.ellipse([-260, -200, W + 260, H + 200], fill=255)
    return v.filter(ImageFilter.GaussianBlur(220))
VIG = vignette()

def backdrop(t):
    ox = int(80 + 28 * math.sin(t * 0.55)); oy = int(80 + 22 * math.cos(t * 0.43))
    bg = BG.crop((ox, oy, ox + W, oy + H))
    return Image.composite(bg, Image.new("RGB", (W, H), (3, 4, 12)), VIG.point(lambda v: 90 + v * 165 // 255))

def sparkles(img, t):
    d = ImageDraw.Draw(img, "RGBA")
    rnd = random.Random(5)
    for i in range(26):
        x = rnd.uniform(0, W); y0 = rnd.uniform(0, H); sp = rnd.uniform(14, 40); r = rnd.uniform(1.5, 3.8)
        y = (y0 - sp * t) % H
        a = int(70 + 70 * math.sin(t * 1.3 + i))
        d.ellipse([x - r, y - r, x + r, y + r], fill=(190, 210, 255, max(10, a)))
    return img

# ---------------------------------------------------------------- captions
CAPTIONS = [  # (lines, start, end, y)
    (["POV: your wallpaper finally", "knows what time it is"], 0.25, 2.55, 300),
    (["Light by day."], 3.0, 4.6, 230),
    (["Dark at night."], 5.0, 6.5, 230),
    (["It switches by itself,", "even when the app is closed"], 6.9, 9.0, 215),
    (["One wallpaper.", "Two looks."], 9.3, 11.4, 215),
    (["10 categories.", "130+ wallpapers, 31 in 4K"], 11.7, 13.9, 215),
    (["Preview it with", "your own apps"], 14.2, 15.5, 215),
    (["Make your own", "day & night pair"], 15.8, 17.3, 215),
]

def pill(lines, size=62):
    f = font(size)
    d = ImageDraw.Draw(Image.new("L", (4, 4)))
    tw = max(d.textlength(l, font=f) for l in lines)
    lh = int(size * 1.3)
    w, h = int(tw + 96), lh * len(lines) + 46
    img = Image.new("RGBA", (w + 40, h + 40), (0, 0, 0, 0))
    sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle([20, 32, 20 + w, 32 + h], radius=46, fill=(0, 0, 0, 120))
    img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(14)))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([20, 20, 20 + w, 20 + h], radius=46, fill=(255, 255, 255, 255))
    for i, l in enumerate(lines):
        d.text((20 + w / 2, 20 + 23 + lh * i + lh / 2), l, font=f, fill=(12, 12, 20, 255), anchor="mm")
    return img

PILLS = {}
def draw_captions(img, t):
    for k, (lines, a, b, y) in enumerate(CAPTIONS):
        if not a <= t <= b: continue
        if k not in PILLS: PILLS[k] = pill(lines, 62 if len(lines) < 2 or max(len(l) for l in lines) < 24 else 56)
        p = PILLS[k]
        s = out_back((t - a) / 0.35) * (1 - smooth((t - (b - 0.25)) / 0.25))
        if s <= 0.02: continue
        q = p.resize((max(1, int(p.width * s)), max(1, int(p.height * s))), Image.LANCZOS)
        img.paste(q, (W // 2 - q.width // 2, y - q.height // 2), q)
    return img

def end_card(img, t):
    a = smooth((t - 17.3) / 0.5)
    if a <= 0: return img
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    d.rectangle([0, 0, W, H], fill=(6, 8, 22, int(235 * a)))
    img = Image.alpha_composite(img.convert("RGBA"), lay)
    s = out_back((t - 17.45) / 0.55)
    icon = Image.open(ICON).convert("RGBA").resize((int(300 * s) + 1, int(300 * s) + 1), Image.LANCZOS)
    mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, icon.width - 1, icon.height - 1], radius=icon.width // 4, fill=255)
    img.paste(icon, (W // 2 - icon.width // 2, 560 - icon.height // 2), mask)
    d = ImageDraw.Draw(img)
    ta = smooth((t - 17.8) / 0.4)
    d.text((W / 2, 830 - 20 * (1 - ta)), "Wallora", font=font(150), fill=(255, 255, 255, int(255 * ta)), anchor="mm")
    d.text((W / 2, 950), "Wallpapers that follow your day", font=font(48), fill=(190, 200, 255, int(255 * smooth((t - 18.0) / 0.4))), anchor="mm")
    pa = out_back((t - 18.3) / 0.4)
    if pa > 0.02:
        label = pill(["Free on Google Play"], 66)
        label = label.resize((int(label.width * pa), int(label.height * pa)), Image.LANCZOS)
        img.alpha_composite(label, (W // 2 - label.width // 2, 1130 - label.height // 2))
    d.text((W / 2, 1290), "Search “Wallora” · link in bio", font=font(44), fill=(170, 180, 230, int(255 * smooth((t - 18.7) / 0.4))), anchor="mm")
    return img.convert("RGB")

# ---------------------------------------------------------------- frame
def frame(t):
    img = backdrop(t)
    cx, cy, s, yaw, pitch, roll = camera(t)
    # a little hover
    cy += 8 * math.sin(t * 1.7)
    quad = project(cx, cy, s, yaw, pitch, roll)
    coeff = homography(quad, [(0, 0), (BW, 0), (BW, BH), (0, BH)])
    layer = phone_layer(t).transform((W, H), Image.PERSPECTIVE, coeff, Image.BICUBIC)
    alpha = layer.split()[3]
    # glow and shadow under the phone
    small = alpha.resize((W // 4, H // 4), Image.BILINEAR).filter(ImageFilter.GaussianBlur(16)).resize((W, H), Image.BILINEAR)
    glow = Image.new("RGB", (W, H), (70, 90, 255))
    img = Image.composite(glow, img, small.point(lambda v: int(v * 0.30)))
    shadow = ImageChops.offset(small, 12, 40).point(lambda v: int(v * 0.55))
    img = Image.composite(Image.new("RGB", (W, H), (0, 0, 8)), img, shadow)
    img.paste(layer, (0, 0), layer)
    img = sparkles(img, t)
    flash = max(smooth(1 - abs(t - 4.95) / 0.12) * 0.35, smooth(1 - abs(t - 1.85) / 0.1) * 0.5)
    if flash > 0:
        img = Image.blend(img, Image.new("RGB", (W, H), (255, 255, 255)), flash)
    img = draw_captions(img, t)
    return end_card(img, t)

# ---------------------------------------------------------------- audio
SR = 44100
def synth_audio(path):
    n = int(SR * DUR)
    mix = [0.0] * n
    def add(t0, samples, gain=1.0):
        i0 = int(t0 * SR)
        for i, v in enumerate(samples):
            j = i0 + i
            if 0 <= j < n: mix[j] += v * gain
    def tone(freq, dur, kind="sine", attack=0.005, decay=3.0, sweep=None):
        out = []
        ph = 0.0
        for i in range(int(dur * SR)):
            t = i / SR
            f = freq if sweep is None else freq + (sweep - freq) * min(1, t / dur * 4)
            ph += 2 * math.pi * f / SR
            if kind == "sine": v = math.sin(ph)
            elif kind == "tri": v = 2 / math.pi * math.asin(math.sin(ph))
            else: v = math.sin(ph) + 0.4 * math.sin(2 * ph) + 0.2 * math.sin(3 * ph)
            env = min(1, t / attack) * math.exp(-decay * t)
            out.append(v * env)
        return out
    def noise(dur, lo, hi, gain=1.0, shape=lambda x: math.sin(math.pi * x)):
        rnd = random.Random(11); out = []; y = 0.0
        for i in range(int(dur * SR)):
            x = i / (dur * SR)
            a = 2 * math.pi * (lo + (hi - lo) * x) / SR
            k = a / (a + 1)
            y += k * (rnd.uniform(-1, 1) - y)
            out.append(y * shape(x) * gain * 4)
        return out
    beat = 0.5
    # drums from 1.0 s, full groove from 2.6 s
    for b in range(int(DUR / beat)):
        t = b * beat
        if t >= 1.0 and t < 17.2: add(t, tone(110, 0.28, decay=11, sweep=42), 0.9 if t >= 2.6 else 0.55)
        if t >= 2.6 and t < 17.2:
            if b % 2 == 1: add(t, noise(0.18, 3500, 2500, 1.0, lambda x: math.exp(-6 * x)), 0.5)
            add(t, noise(0.06, 9000, 9000, 0.6, lambda x: math.exp(-12 * x)), 0.28)
            add(t + beat / 2, noise(0.05, 9000, 9000, 0.6, lambda x: math.exp(-14 * x)), 0.2)
    # bass and pad: Am - F - C - G, two seconds each
    chords = [(55.0, [220, 261.6, 329.6]), (43.65, [174.6, 220, 261.6]), (65.4, [261.6, 329.6, 392]), (49.0, [196, 246.9, 293.7])]
    for bar in range(10):
        t0 = bar * 2.0
        root, notes = chords[bar % 4]
        if t0 >= 2.4 and t0 < 17.2:
            for q in range(4):
                add(t0 + q * 0.5, tone(root, 0.45, "saw", decay=4.5), 0.26)
        for f in notes:
            add(t0, tone(f, 2.1, "tri", attack=0.25, decay=0.9), 0.10)
    # plucks from 6.6 s
    scale = [440, 523.3, 587.3, 659.3, 783.99, 880]
    rnd = random.Random(3)
    for b in range(int(6.6 / 0.25), int(17.2 / 0.25)):
        if rnd.random() < 0.6: add(b * 0.25, tone(rnd.choice(scale), 0.3, "tri", decay=9), 0.11)
    # whooshes into each new screen, tap clicks, pops
    for t in (2.45, 6.55, 8.95, 11.35, 13.85, 15.45, 17.2):
        add(t, noise(0.55, 300, 5000, 1.0), 0.32)
    for (t, _x, _y) in TAPS: add(t, tone(900, 0.12, decay=28), 0.35)
    for _l, a, _b, _y in CAPTIONS: add(a, tone(520, 0.14, decay=22, sweep=900), 0.22)
    # the day -> night moment: a glass chime, then the end boom and chime
    for k, f in enumerate((1318.5, 659.25)): add(4.95, tone(f, 1.5, decay=3.2), 0.30 if k == 0 else 0.22)
    add(17.25, tone(90, 1.2, decay=3.0, sweep=40), 0.9)
    for k, f in enumerate((659.25, 987.77, 1318.5)): add(17.5 + k * 0.12, tone(f, 1.6, decay=2.6), 0.2)
    peak = max(1e-6, max(abs(v) for v in mix))
    fade = int(0.4 * SR)
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(1); wf.setsampwidth(2); wf.setframerate(SR)
        frames = bytearray()
        for i, v in enumerate(mix):
            g = 0.88 / peak
            if i > n - fade: g *= (n - i) / fade
            frames += struct.pack("<h", int(max(-1, min(1, v * g)) * 32767))
        wf.writeframes(bytes(frames))

# ---------------------------------------------------------------- main
if __name__ == "__main__":
    out = Path(sys.argv[2])
    if "--stills" in sys.argv:
        out.mkdir(parents=True, exist_ok=True)
        for tt in sys.argv[sys.argv.index("--stills") + 1:]:
            frame(float(tt)).save(out / f"still_{float(tt):05.2f}.png")
        sys.exit(0)
    import imageio_ffmpeg
    wav = out.with_suffix(".wav")
    synth_audio(wav)
    cmd = [imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-",
           "-i", str(wav), "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "17", "-preset", "medium", "-profile:v", "high",
           "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", "-shortest", str(out)]
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stderr=subprocess.DEVNULL)
    for i in range(int(DUR * FPS)):
        p.stdin.write(frame(i / FPS).tobytes())
        if i % 60 == 0: print(f"frame {i}/{int(DUR * FPS)}", flush=True)
    p.stdin.close(); p.wait()
    wav.unlink(missing_ok=True)
    print("wrote", out)
