#!/usr/bin/env python3
"""Renders the Wallora "desk" ad: a 14 second 1080x1920 motion-graphics ad in the popular TikTok format where a
white "POV" label sits over a neon-lit desk and the monitor plays the product's motion graphics.

Screen story: night wallpaper -> app window (typing, chips) -> menu click with a glowing ring ->
big words on a gradient (day, night, automatic) -> logo reveal.
The desk scene is drawn here (stylised, not a photo); music and sound effects are synthesised, so nothing is borrowed.

Usage:
  make_desk_ad.py <out.mp4>                    full render
  make_desk_ad.py <out-dir> --stills 1 3 5     PNG stills at those seconds
"""
import math, random, struct, subprocess, sys, wave
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops

W, H, FPS, DUR = 1080, 1920, 30, 14.0
RW, RH = 1300, 2200                     # the room is drawn larger than the frame so the camera can move
OX, OY = (RW - W) // 2, (RH - H) // 2
ROOT = Path(__file__).resolve().parent.parent
FB = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
FR = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
FS = "/usr/share/fonts/truetype/noto/NotoSerif-Bold.ttf"

def font(size, path=FB): return ImageFont.truetype(path, size)
def clamp(x, a=0.0, b=1.0): return max(a, min(b, x))
def smooth(x): x = clamp(x); return x * x * (3 - 2 * x)
def lerp(a, b, t): return a + (b - a) * t
def lerp_c(a, b, t): return tuple(int(lerp(x, y, t)) for x, y in zip(a, b))
def out_back(x, k=1.7):
    x = clamp(x) - 1
    return 1 + (k + 1) * x ** 3 + k * x ** 2

def vgrad(w, h, top, bottom):
    col = Image.new("RGB", (1, h))
    col.putdata([lerp_c(top, bottom, y / max(1, h - 1)) for y in range(h)])
    return col.resize((w, h))

def hgrad(w, h, left, right):
    row = Image.new("RGB", (w, 1))
    row.putdata([lerp_c(left, right, x / max(1, w - 1)) for x in range(w)])
    return row.resize((w, h))

def homography(dst, src):
    """PIL PERSPECTIVE coefficients: output points dst map to input points src."""
    A, B = [], []
    for (x, y), (u, v) in zip(dst, src):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); B.append(u)
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y]); B.append(v)
    n = 8
    M = [r + [b] for r, b in zip(A, B)]
    for i in range(n):
        piv = max(range(i, n), key=lambda r: abs(M[r][i]))
        M[i], M[piv] = M[piv], M[i]
        for r in range(n):
            if r != i:
                f = M[r][i] / M[i][i]
                M[r] = [a - f * b for a, b in zip(M[r], M[i])]
    return [M[i][n] / M[i][i] for i in range(n)]

# ------------------------------------------------------------------ the room
SCR_Q = [(215, 800), (1090, 878), (1090, 1200), (215, 1262)]     # monitor glass: TL, TR, BR, BL (room coordinates)

def leaf(length, width, colour):
    img = Image.new("RGBA", (width * 2, length * 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([width // 2, length // 2, width * 3 // 2, length * 2 - 4], fill=colour)
    d.line([(width, length // 2 + 10), (width, length * 2 - 14)], fill=(255, 255, 255, 40), width=2)
    return img

def plant(room, x, y, seed, scale=1.0):
    rnd = random.Random(seed)
    lay = Image.new("RGBA", (400, 400), (0, 0, 0, 0))
    for k in range(9):
        ang = rnd.uniform(-62, 62)
        ln = int(rnd.uniform(70, 120) * scale)
        lf = leaf(ln, int(22 * scale), (int(rnd.uniform(20, 50)), int(rnd.uniform(110, 160)), int(rnd.uniform(90, 130)), 255))
        lf = lf.rotate(ang, expand=True, resample=Image.BICUBIC, center=(lf.width // 2, lf.height))
        lay.alpha_composite(lf, (200 - lf.width // 2, 200 - lf.height + 10))
    d = ImageDraw.Draw(lay)
    d.polygon([(165, 200), (235, 200), (222, 262), (178, 262)], fill=(18, 20, 34, 255))
    d.line([(165, 200), (235, 200)], fill=(90, 120, 230, 255), width=3)
    lay = lay.filter(ImageFilter.GaussianBlur(0.8))
    room.paste(lay, (x - 200, y - 200), lay)

def build_room():
    room = vgrad(RW, RH, (8, 12, 40), (16, 14, 52))
    # wall panels: soft diagonal facets
    d = ImageDraw.Draw(room, "RGBA")
    rnd = random.Random(9)
    for i in range(14):
        x = i * 110 - 60
        d.polygon([(x, 0), (x + 150, 0), (x + 40, 900), (x - 110, 900)], fill=(255, 255, 255, rnd.choice((4, 7, 10)) if i % 2 else 0))
        d.polygon([(x, 0), (x + 30, 0), (x - 80, 900), (x - 110, 900)], fill=(0, 0, 20, 40))
    # LED glow on the wall: blue on the left, violet top right, magenta low right
    glow = Image.new("RGB", (RW, RH), (0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.rectangle([-40, 380, 60, 1500], fill=(30, 110, 255))
    g.ellipse([880, 150, 1500, 700], fill=(120, 60, 230))
    g.ellipse([-300, 1000, 250, 1700], fill=(40, 120, 255))
    g.ellipse([950, 900, 1500, 1500], fill=(200, 60, 200))
    glow = glow.filter(ImageFilter.GaussianBlur(120))
    room = ImageChops.add(room, Image.eval(glow, lambda v: int(v * 0.7)))
    # desk
    desk = vgrad(RW, RH - 1320, (22, 24, 46), (6, 7, 16))
    room.paste(desk, (0, 1320))
    d = ImageDraw.Draw(room, "RGBA")
    d.line([(0, 1320), (RW, 1320)], fill=(120, 150, 255, 150), width=3)
    sheen = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
    ImageDraw.Draw(sheen).ellipse([60, 1330, RW - 60, 1560], fill=(90, 110, 255, 40))
    room.paste(Image.alpha_composite(room.convert("RGBA"), sheen.filter(ImageFilter.GaussianBlur(40))).convert("RGB"))
    d = ImageDraw.Draw(room, "RGBA")
    # plants behind the monitor
    plant(room, 105, 1300, 1, 1.2)
    plant(room, 1195, 1262, 4, 1.05)
    # monitor stand
    d = ImageDraw.Draw(room, "RGBA")
    d.polygon([(610, 1240), (690, 1240), (700, 1345), (600, 1345)], fill=(14, 15, 26, 255))
    d.ellipse([430, 1330, 870, 1385], fill=(16, 17, 30, 255), outline=(80, 90, 150, 255), width=3)
    # monitor body (bezel quad, slightly bigger than the glass)
    bez = [(201, 786), (1104, 866), (1104, 1214), (201, 1276)]
    d.polygon(bez, fill=(10, 10, 18, 255), outline=(70, 80, 130, 255))
    d.line([bez[0], bez[1]], fill=(110, 130, 220, 200), width=3)
    # light bar over the monitor
    bar = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bar)
    bd.line([(650, 790), (650, 706)], fill=(30, 32, 50, 255), width=8)
    bd.rounded_rectangle([330, 690, 970, 722], radius=14, fill=(20, 22, 38, 255), outline=(90, 100, 150, 255), width=2)
    bd.rounded_rectangle([345, 718, 955, 726], radius=4, fill=(255, 244, 220, 255))
    halo = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
    ImageDraw.Draw(halo).polygon([(345, 722), (955, 722), (1090, 900), (210, 900)], fill=(255, 235, 200, 70))
    halo = halo.filter(ImageFilter.GaussianBlur(30))
    room = Image.alpha_composite(room.convert("RGBA"), halo)
    room = Image.alpha_composite(room, bar.filter(ImageFilter.GaussianBlur(0.6))).convert("RGB")
    return room

def build_keyboard():
    """A glowing mechanical keyboard in the foreground (RGBA, same size as the room)."""
    lay = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
    far_y, near_y = 1575, 2010
    far_x0, far_x1, near_x0, near_x1 = 330, 960, 90, 1260
    def pt(u, v):   # u across 0..1, v from far (0) to near (1)
        y = lerp(far_y, near_y, v)
        return (lerp(lerp(far_x0, near_x0, v), lerp(far_x1, near_x1, v), u), y)
    def rgb(u):     # blue -> violet -> magenta across the board
        return lerp_c((60, 130, 255), (225, 70, 215), u)
    gl = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gl)
    for i in range(12):
        u0, u1 = i / 12, (i + 1) / 12
        gd.polygon([pt(u0, -0.08), pt(u1, -0.08), pt(u1, 1.08), pt(u0, 1.08)], fill=rgb((u0 + u1) / 2) + (255,))
    lay.alpha_composite(gl.filter(ImageFilter.GaussianBlur(30)))
    d = ImageDraw.Draw(lay)
    d.polygon([pt(-0.025, -0.03), pt(1.025, -0.03), pt(1.025, 1.03), pt(-0.025, 1.03)], fill=(15, 16, 28, 255), outline=(120, 140, 230, 255))
    rows, cols = 5, 15
    rnd = random.Random(2)
    for r in range(rows):
        for c in range(cols):
            u0, u1 = c / cols + 0.006, (c + 1) / cols - 0.006
            v0, v1 = r / rows + 0.018, (r + 1) / rows - 0.018
            base = [pt(u0, v0), pt(u1, v0), pt(u1, v1), pt(u0, v1)]
            lift = 10 + 10 * (r / rows)
            top = [(x, y - lift) for x, y in base]
            lit = rnd.random() < 0.16
            side = rgb((u0 + u1) / 2) if lit else (36, 40, 70)
            d.polygon([base[3], base[2], top[2], top[3]], fill=side + (255,))                 # the key's front face
            d.polygon(top, fill=(60, 66, 110, 255) if lit else (34, 37, 64, 255), outline=(150, 165, 245, 255) if lit else (88, 98, 160, 255))
            d.line([top[0], top[1]], fill=(190, 205, 255, 150), width=2)
    return lay

ROOM = build_room()
KEYBOARD = build_keyboard()

# ------------------------------------------------------------------ screen content (1600 x 700)
SWD, SHT = 1600, 620
PHOTO = Image.open(ROOT / "app/src/main/res/drawable-nodpi/scenic_10.webp").convert("RGB")
ICON = Image.open(ROOT / "store/icon-512.png").convert("RGBA")

def cover(img, w, h, zoom=1.0, dx=0.0, dy=0.0):
    s = max(w / img.width, h / img.height) * zoom
    r = img.resize((int(img.width * s) + 1, int(img.height * s) + 1), Image.LANCZOS)
    x = int((r.width - w) / 2 + dx); y = int((r.height - h) / 2 + dy)
    return r.crop((x, y, x + w, y + h))

def cursor(d, x, y, press=0.0):
    pts = [(0, 0), (0, 30), (8, 23), (14, 36), (20, 33), (14, 21), (24, 21)]
    sc = 1.0 - 0.12 * press
    poly = [(x + px * sc, y + py * sc) for px, py in pts]
    d.polygon([(a + 2, b + 3) for a, b in poly], fill=(0, 0, 0, 110))
    d.polygon(poly, fill=(255, 255, 255, 255), outline=(20, 20, 30, 255))

def screen_wallpaper(t):
    p = clamp(t / 2.4)
    img = cover(PHOTO, SWD, SHT, 1.05 + 0.08 * p, dx=-30 * p, dy=0).convert("RGBA")
    d = ImageDraw.Draw(img)
    d.text((SWD - 90, 60), "12:00", font=font(76), fill=(255, 255, 255, 235), anchor="ra")
    d.text((SWD - 90, 124), "Monday, 17 November", font=font(26, FR), fill=(255, 255, 255, 190), anchor="ra")
    return img

# word suggestions typed into the search box
QUERY = "something dark and calm"
CHIPS = ["Dark", "Cars", "Space", "City", "Nature", "Cute", "Create"]

def screen_window(t, base):
    """The app window opening over the wallpaper (t is seconds since 2.2)."""
    a = out_back((t - 2.2) / 0.55)
    img = base.copy()
    if a <= 0.02: return img
    ww, wh = 1000, 520
    win = Image.new("RGBA", (ww, wh), (0, 0, 0, 0))
    d = ImageDraw.Draw(win)
    d.rounded_rectangle([0, 0, ww - 1, wh - 1], radius=26, fill=(28, 24, 40, 245), outline=(150, 130, 255, 120), width=2)
    for i, c in enumerate(((255, 95, 86), (255, 189, 46), (39, 201, 63))):
        d.ellipse([28 + i * 30, 26, 44 + i * 30, 42], fill=c + (255,))
    ic = ICON.resize((84, 84), Image.LANCZOS)
    win.paste(ic, (ww // 2 - 190, 92), ic)
    d.text((ww // 2 - 86, 134), "Welcome", font=font(70, FS), fill=(255, 255, 255, 255), anchor="lm")
    # search box with typed text
    d.rounded_rectangle([110, 230, ww - 110, 320], radius=24, fill=(18, 16, 28, 255), outline=(170, 150, 255, 160), width=2)
    n = int(clamp((t - 2.7) / 1.2) * len(QUERY))
    shown = QUERY[:n]
    if n == 0: shown = ""
    placeholder = "Search wallpapers, styles, vibes" if n == 0 else ""
    d.text((146, 275), shown or placeholder, font=font(32, FR), fill=(255, 255, 255, 255) if shown else (150, 150, 175, 255), anchor="lm")
    if int(t * 2.5) % 2 == 0 and n < len(QUERY) + 8:
        cx = 146 + d.textlength(shown, font=font(32, FR))
        d.rectangle([cx + 3, 252, cx + 6, 298], fill=(255, 255, 255, 255))
    # chips
    x = 110
    for c in CHIPS:
        f = font(28)
        w = int(d.textlength(c, font=f)) + 56
        d.rounded_rectangle([x, 360, x + w, 416], radius=28, fill=(48, 42, 72, 255), outline=(140, 125, 220, 120), width=2)
        d.text((x + w / 2, 388), c, font=f, fill=(235, 232, 255, 255), anchor="mm")
        x += w + 16
    # little appearance button, top right
    d.rounded_rectangle([ww - 222, 16, ww - 24, 56], radius=20, fill=(48, 42, 72, 255))
    d.text((ww - 123, 36), "◐  Appearance", font=font(22), fill=(235, 232, 255, 255), anchor="mm")
    s = a
    win = win.resize((max(1, int(ww * s)), max(1, int(wh * s))), Image.LANCZOS)
    sh = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ox, oy = SWD // 2 - win.width // 2, SHT // 2 - win.height // 2 + 10
    ImageDraw.Draw(sh).rounded_rectangle([ox, oy + 16, ox + win.width, oy + win.height + 16], radius=30, fill=(0, 0, 0, 150))
    img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(20)))
    img.alpha_composite(win, (ox, oy))
    return img

MENU_ITEMS = [("Light", "always day"), ("Dark", "always night"), ("Auto", "follows the sun")]

def screen_menu(t):
    """Window with the Appearance menu open, a glowing ring on 'Auto' and a click."""
    base = screen_window(4.4, screen_wallpaper(4.4))   # window fully open
    img = base.copy()
    ox, oy = SWD // 2 - 500, SHT // 2 - 260 + 10
    bx, by = ox + 1000 - 123, oy + 36
    # cursor path: from the middle to the button, then down to "Auto"
    mx, my = bx + 40, by + 330
    p1 = smooth((t - 4.45) / 0.5); p2 = smooth((t - 5.15) / 0.45)
    cx = lerp(lerp(SWD * 0.45, bx + 10, p1), mx, p2)
    cy = lerp(lerp(SHT * 0.7, by + 8, p1), my, p2)
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    open_a = smooth((t - 4.95) / 0.2)
    if open_a > 0:
        mw, mh = 330, 260
        mxl, myt = bx - 130, by + 30
        d.rounded_rectangle([mxl, myt, mxl + mw, myt + mh * open_a], radius=20, fill=(20, 18, 32, 250), outline=(150, 130, 255, 140), width=2)
        if open_a > 0.95:
            for i, (name, sub) in enumerate(MENU_ITEMS):
                yy = myt + 26 + i * 76
                hot = i == 2
                if hot: d.rounded_rectangle([mxl + 12, yy - 14, mxl + mw - 12, yy + 56], radius=14, fill=(70, 52, 120, 255))
                d.text((mxl + 36, yy + 8), name, font=font(30), fill=(255, 255, 255, 255), anchor="lm")
                d.text((mxl + 36, yy + 38), sub, font=font(20, FR), fill=(190, 185, 225, 255), anchor="lm")
            # glowing ring around "Auto", pulsing like the highlight in the reference
            ring_t = clamp((t - 5.6) / 0.4)
            if ring_t > 0:
                rx, ry = mxl + mw / 2, myt + 26 + 2 * 76 + 21
                r = 120 + 14 * math.sin(t * 6)
                glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
                gd = ImageDraw.Draw(glow)
                gd.ellipse([rx - r, ry - r * 0.62, rx + r, ry + r * 0.62], outline=(255, 90, 60, int(255 * ring_t)), width=14)
                lay.alpha_composite(glow.filter(ImageFilter.GaussianBlur(10)))
                gd = ImageDraw.Draw(lay)
                gd.ellipse([rx - r, ry - r * 0.62, rx + r, ry + r * 0.62], outline=(255, 150, 110, int(255 * ring_t)), width=5)
    d = ImageDraw.Draw(lay)
    cursor(d, cx, cy, press=1.0 if 5.55 < t < 5.7 else 0.0)
    img.alpha_composite(lay)
    return img

DAY = ((255, 196, 140), (255, 112, 150))      # warm morning gradient
NIGHT = ((16, 20, 70), (112, 40, 140))        # evening
DEEP = ((14, 10, 40), (90, 22, 70))

def word_img(text, size=150):
    f = font(size, FS)
    d = ImageDraw.Draw(Image.new("L", (4, 4)))
    w = int(d.textlength(text, font=f)) + 80
    img = Image.new("RGBA", (w, size * 2), (0, 0, 0, 0))
    ImageDraw.Draw(img).text((w / 2, size), text, font=f, fill=(255, 255, 255, 255), anchor="mm")
    return img

WORDS = [("Light by day", 6.25, 7.45), ("Dark at night", 7.45, 8.75), ("Automatic.", 8.75, 10.0)]

def screen_words(t):
    # gradient moves from morning to evening to deep night as the words change
    k1 = smooth((t - 7.2) / 0.8); k2 = smooth((t - 8.6) / 0.8)
    top = lerp_c(lerp_c(DAY[0], NIGHT[0], k1), DEEP[0], k2)
    bot = lerp_c(lerp_c(DAY[1], NIGHT[1], k1), DEEP[1], k2)
    img = hgrad(SWD, SHT, top, bot).convert("RGBA")
    img = Image.blend(img.convert("RGB"), vgrad(SWD, SHT, top, bot), 0.5).convert("RGBA")
    for text, a, b in WORDS:
        for jitter, base in ((0, 1.0), (1, 0.55)):
            fade_in = smooth((t - a) / 0.25); fade_out = 1 - smooth((t - (b - 0.1)) / 0.25)
            alpha = clamp(min(fade_in, fade_out)) * base
            if alpha <= 0.02: continue
            wi = word_img(text, 128 if len(text) > 11 else 150)
            # the two copies drift apart while the word changes, like the overlapping text in the reference
            off = (1 - min(fade_in, fade_out)) * 40 * (1 if jitter else -1) + jitter * 6
            wi = wi.copy(); wi.putalpha(wi.getchannel("A").point(lambda v: int(v * alpha)))
            img.alpha_composite(wi, (int(SWD / 2 - wi.width / 2 + off), int(SHT / 2 - wi.height / 2)))
    return img

def screen_logo(t):
    a = smooth((t - 10.0) / 0.6)
    top = lerp_c(DEEP[0], (12, 10, 28), a); bot = lerp_c(DEEP[1], (70, 24, 96), a)
    img = Image.blend(hgrad(SWD, SHT, top, bot), vgrad(SWD, SHT, top, bot), 0.5).convert("RGBA")
    s = out_back((t - 10.35) / 0.7)
    if s > 0.02:
        size = max(2, int(150 * s))
        ic = ICON.resize((size, size), Image.LANCZOS)
        mask = Image.new("L", ic.size, 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=size // 4, fill=255)
        img.paste(ic, (SWD // 2 - 330, SHT // 2 - size // 2 - 20), mask)
    ta = smooth((t - 10.7) / 0.5)
    if ta > 0:
        d = ImageDraw.Draw(img)
        d.text((SWD // 2 - 150 - 24 * (1 - ta), SHT // 2 - 20), "Wallora", font=font(150, FS), fill=(255, 255, 255, int(255 * ta)), anchor="lm")
    tb = smooth((t - 11.6) / 0.5)
    if tb > 0:
        d = ImageDraw.Draw(img)
        d.text((SWD // 2, SHT // 2 + 150), "Free on Google Play  ·  link in bio", font=font(34, FR), fill=(235, 225, 255, int(255 * tb)), anchor="mm")
    return img

def screen_at(t):
    if t < 2.2: return screen_wallpaper(t)
    if t < 4.4: return screen_window(t, screen_wallpaper(t))
    if t < 6.25:
        img = screen_menu(t)
        k = smooth((t - 6.05) / 0.2)                       # quick wipe to the gradient
        if k > 0: return Image.blend(img, screen_words(6.25), k)
        return img
    if t < 10.0: return screen_words(t)
    return screen_logo(t)

# ------------------------------------------------------------------ frame
def camera(t):
    """Zoom, shift and tilt of the whole scene: a slow push-in plus a little handheld sway."""
    z = 1.0 + 0.10 * smooth(t / DUR) + 0.035 * smooth((t - 9.8) / 1.2)
    dx = 14 * math.sin(t * 0.9) + 6 * math.sin(t * 2.3)
    dy = 10 * math.cos(t * 0.7) + 4 * math.sin(t * 2.9) - 30 * smooth((t - 9.8) / 1.5)
    rot = math.radians(0.45 * math.sin(t * 0.8))
    return z, dx, dy, rot

def frame(t):
    room = ROOM.convert("RGBA")
    scr = screen_at(t).convert("RGB")
    coeff = homography(SCR_Q, [(0, 0), (SWD, 0), (SWD, SHT), (0, SHT)])
    glass = scr.transform((RW, RH), Image.PERSPECTIVE, coeff, Image.BICUBIC).convert("RGBA")
    mask = Image.new("L", (RW, RH), 0)
    ImageDraw.Draw(mask).polygon(SCR_Q, fill=255)
    # what the screen lights up in the room: a blurred copy of its average colour
    tiny = scr.resize((8, 4), Image.BILINEAR)
    avg = tiny.resize((1, 1), Image.BILINEAR).getpixel((0, 0))
    lamp = Image.new("RGBA", (RW // 4, RH // 4), (0, 0, 0, 0))
    ImageDraw.Draw(lamp).polygon([(x // 4, y // 4) for x, y in SCR_Q], fill=avg + (255,))
    lamp = lamp.filter(ImageFilter.GaussianBlur(26)).resize((RW, RH), Image.BILINEAR)
    lamp_rgb = Image.merge("RGB", lamp.split()[:3])
    room = Image.blend(room.convert("RGB"), ImageChops.add(room.convert("RGB"), Image.eval(lamp_rgb, lambda v: int(v * 0.28))), 1.0).convert("RGBA")
    room.paste(glass, (0, 0), mask)
    # glass reflection: a faint diagonal sheen
    sheen = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
    ImageDraw.Draw(sheen).polygon([(215, 800), (560, 828), (380, 1262), (215, 1262)], fill=(255, 255, 255, 16))
    room.alpha_composite(Image.composite(sheen, Image.new("RGBA", (RW, RH), (0, 0, 0, 0)), mask))
    room.alpha_composite(KEYBOARD)
    # camera
    z, dx, dy, rot = camera(t)
    cx, cy = RW / 2, RH / 2 + 40
    cs, sn = math.cos(rot), math.sin(rot)
    # output (x,y) -> input: undo the zoom/rotation about the frame centre
    ocx, ocy = W / 2, H / 2
    a = cs / z; b = -sn / z; d_ = sn / z; e = cs / z
    c = cx - (a * ocx + b * ocy) - dx
    f = cy - (d_ * ocx + e * ocy) - dy
    img = room.convert("RGB").transform((W, H), Image.AFFINE, (a, b, c, d_, e, f), Image.BICUBIC)
    # soft vignette and a little film grain
    v = Image.new("L", (W, H), 0)
    ImageDraw.Draw(v).ellipse([-200, -160, W + 200, H + 160], fill=255)
    v = v.filter(ImageFilter.GaussianBlur(190))
    img = Image.composite(img, Image.new("RGB", (W, H), (2, 3, 10)), v.point(lambda x: 105 + x * 150 // 255))
    img = draw_pov(img, t)
    return img

PILL = None
def draw_pov(img, t):
    global PILL
    if PILL is None:
        f = font(54)
        text = "Pov : Wallora hires us for a short ad"
        lines = ["Pov : Wallora hires us", "for a short ad"]
        d = ImageDraw.Draw(Image.new("L", (4, 4)))
        tw = max(d.textlength(l, font=f) for l in lines)
        w, h = int(tw + 76), 74 * 2 + 30
        PILL = Image.new("RGBA", (w + 40, h + 40), (0, 0, 0, 0))
        sh = Image.new("RGBA", PILL.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle([20, 30, 20 + w, 30 + h], radius=28, fill=(0, 0, 0, 120))
        PILL.alpha_composite(sh.filter(ImageFilter.GaussianBlur(12)))
        dd = ImageDraw.Draw(PILL)
        dd.rounded_rectangle([20, 20, 20 + w, 20 + h], radius=28, fill=(255, 255, 255, 255))
        for i, l in enumerate(lines):
            dd.text((20 + w / 2, 20 + 15 + 74 * i + 37), l, font=f, fill=(12, 12, 20, 255), anchor="mm")
    s = out_back((t - 0.2) / 0.4)
    if s <= 0.02: return img
    p = PILL.resize((max(1, int(PILL.width * s)), max(1, int(PILL.height * s))), Image.LANCZOS)
    img.paste(p, (W // 2 - p.width // 2, 330 - p.height // 2), p)
    return img

# ------------------------------------------------------------------ audio
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
        out = []; ph = 0.0
        for i in range(int(dur * SR)):
            t = i / SR
            f = freq if sweep is None else freq + (sweep - freq) * min(1, t / dur * 4)
            ph += 2 * math.pi * f / SR
            v = math.sin(ph) if kind == "sine" else (2 / math.pi * math.asin(math.sin(ph)) if kind == "tri" else math.sin(ph) + 0.35 * math.sin(2 * ph))
            out.append(v * min(1, t / attack) * math.exp(-decay * t))
        return out
    def noise(dur, lo, hi, shape=lambda x: math.sin(math.pi * x), seed=11):
        rnd = random.Random(seed); out = []; y = 0.0
        for i in range(int(dur * SR)):
            x = i / (dur * SR)
            a = 2 * math.pi * (lo + (hi - lo) * x) / SR
            y += (a / (a + 1)) * (rnd.uniform(-1, 1) - y)
            out.append(y * shape(x) * 4)
        return out
    beat = 60 / 84
    chords = [(55.0, [220, 261.6, 329.6]), (43.65, [174.6, 220, 261.6]), (65.4, [261.6, 329.6, 392]), (49.0, [196, 246.9, 293.7])]
    bar = beat * 4
    for k in range(int(DUR / bar) + 1):
        t0 = k * bar
        root, notes = chords[k % 4]
        for f in notes: add(t0, tone(f, bar + 0.2, "tri", attack=0.4, decay=0.7), 0.11)
        if t0 >= 2.2:
            for q in range(4): add(t0 + q * beat, tone(root, beat * 0.9, "saw", decay=4.0), 0.2)
    for b in range(int(DUR / beat)):
        t = b * beat
        if 2.2 <= t < 10.2:
            add(t, tone(100, 0.25, decay=12, sweep=40), 0.65)
            if b % 2 == 1: add(t, noise(0.15, 3200, 2400, lambda x: math.exp(-7 * x), 5), 0.32)
            add(t + beat / 2, noise(0.04, 9000, 9000, lambda x: math.exp(-14 * x), 7), 0.16)
    rnd = random.Random(4)
    for k in range(int(0.3 / 0.18) + int(1.3 / 0.06)):         # typing clicks
        pass
    for tt in [2.75 + i * 0.052 for i in range(23)]: add(tt, tone(1500 + rnd.uniform(-200, 200), 0.03, decay=90), 0.22)
    add(2.2, noise(0.5, 400, 4500, seed=2), 0.3)               # window whoosh
    add(5.55, tone(900, 0.1, decay=30), 0.4)                   # the click
    add(5.58, noise(0.25, 2000, 6000, lambda x: math.exp(-8 * x), 3), 0.2)
    for tt in (6.05, 7.4, 8.7, 9.95): add(tt, noise(0.5, 300, 5200, seed=int(tt * 10)), 0.34)   # word whooshes
    for k, f in enumerate((1318.5, 659.25)): add(7.5, tone(f, 1.2, decay=3.4), 0.22 if k else 0.28)
    add(10.0, tone(80, 1.4, decay=2.6, sweep=36), 0.9)         # logo hit
    for k, f in enumerate((659.25, 987.77, 1318.5, 1760.0)): add(10.45 + k * 0.1, tone(f, 1.8, decay=2.4), 0.2)
    add(9.4, noise(0.6, 200, 6000, lambda x: x * x, 9), 0.35)  # riser
    peak = max(1e-6, max(abs(v) for v in mix))
    fade = int(0.5 * SR)
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(1); wf.setsampwidth(2); wf.setframerate(SR)
        buf = bytearray()
        for i, v in enumerate(mix):
            g = 0.88 / peak
            if i > n - fade: g *= (n - i) / fade
            buf += struct.pack("<h", int(max(-1, min(1, v * g)) * 32767))
        wf.writeframes(bytes(buf))

if __name__ == "__main__":
    out = Path(sys.argv[1])
    if "--stills" in sys.argv:
        out.mkdir(parents=True, exist_ok=True)
        for tt in sys.argv[sys.argv.index("--stills") + 1:]:
            frame(float(tt)).save(out / f"desk_{float(tt):05.2f}.png")
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
