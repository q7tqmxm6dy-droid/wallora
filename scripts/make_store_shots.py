#!/usr/bin/env python3
"""Composes the Play Store phone screenshots (1080x1920) and the feature graphic (1024x500) from raw emulator captures.

Usage: make_store_shots.py <raw-dir> <out-dir>
<raw-dir> holds 1080x2340 PNG captures named below; <out-dir> receives store/screenshots/*.png and feature-graphic.png.
Play wants 9:16 images (long side at most 2x the short side), so the 19.5:9 captures are framed on a 1080x1920 canvas.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

RAW, OUT = Path(sys.argv[1]), Path(sys.argv[2])
(OUT / "screenshots").mkdir(parents=True, exist_ok=True)
FONT = "/usr/share/fonts/truetype/noto/NotoSans-%s.ttf"
W, H = 1080, 1920

def font(style, size): return ImageFont.truetype(FONT % style, size)

def background(w, h):
    top, bottom = (66, 42, 108), (24, 38, 70)
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        c = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(w): px[x, y] = c
    return img

def centered(draw, text, y, fnt, fill, max_w=980):
    """Centres text, shrinking the font until it fits max_w."""
    size = fnt.size
    while draw.textlength(text, font=fnt) > max_w and size > 30:
        size -= 2
        fnt = ImageFont.truetype(fnt.path, size)
    w = draw.textlength(text, font=fnt)
    draw.text(((W - w) / 2, y), text, font=fnt, fill=fill)

def phone(img: Image.Image, width: int) -> Image.Image:
    """The capture inside a dark rounded bezel with a soft shadow."""
    img = img.convert("RGB")
    h = round(img.height * width / img.width)
    shot = img.resize((width, h), Image.LANCZOS)
    bez = max(8, width // 60)
    r = width // 9
    frame = Image.new("RGBA", (width + 2 * bez, h + 2 * bez), (0, 0, 0, 0))
    d = ImageDraw.Draw(frame)
    d.rounded_rectangle([0, 0, frame.width - 1, frame.height - 1], radius=r + bez, fill=(14, 12, 22, 255), outline=(70, 66, 92, 255), width=3)
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, shot.width - 1, shot.height - 1], radius=r, fill=255)
    frame.paste(shot, (bez, bez), mask)
    return frame

def shadowed(canvas, frame, x, y):
    sh = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    blob = Image.new("RGBA", frame.size, (0, 0, 0, 150))
    sh.paste(blob, (x + 10, y + 22), frame.split()[3])
    canvas.paste(Image.alpha_composite(canvas.convert("RGBA"), sh.filter(ImageFilter.GaussianBlur(28))).convert("RGB"))
    canvas.paste(frame, (x, y), frame)

def single(name, raw, title, sub, out):
    img = background(W, H)
    d = ImageDraw.Draw(img)
    centered(d, title, 96, font("Bold", 78), (255, 255, 255))
    centered(d, sub, 200, font("Regular", 42), (205, 200, 232))
    fr = phone(Image.open(RAW / raw), 740)
    shadowed(img, fr, (W - fr.width) // 2, 300)
    img.save(OUT / "screenshots" / out)

def pair(raw_a, raw_b, title, sub, label_a, label_b, out):
    img = background(W, H)
    d = ImageDraw.Draw(img)
    centered(d, title, 96, font("Bold", 78), (255, 255, 255))
    centered(d, sub, 200, font("Regular", 42), (205, 200, 232))
    fa, fb = phone(Image.open(RAW / raw_a), 470), phone(Image.open(RAW / raw_b), 470)
    y = 470
    xa, xb = 40, W - 40 - fb.width
    shadowed(img, fa, xa, y); shadowed(img, fb, xb, y)
    d = ImageDraw.Draw(img)
    for fr, x, label, col in ((fa, xa, label_a, (255, 214, 120)), (fb, xb, label_b, (160, 190, 255))):
        f = font("Bold", 46)
        tw = d.textlength(label, font=f)
        cx = x + fr.width / 2
        d.rounded_rectangle([cx - tw / 2 - 34, y + fr.height + 34, cx + tw / 2 + 34, y + fr.height + 104], radius=35, fill=(255, 255, 255, 40), outline=col, width=3)
        d.text((cx - tw / 2, y + fr.height + 40), label, font=f, fill=col)
    img.save(OUT / "screenshots" / out)

SHOTS = [
    lambda: single("detail", "detail_pair.png", "Light by day, dark at night", "One wallpaper, two looks", "01-day-night.png"),
    lambda: pair("lock_light.png", "lock_dark.png", "It switches by itself", "Even when the app is closed", "Day", "Night", "02-switches.png"),
    lambda: single("create", "pair_maker.png", "Make your own day & night", "Turn any picture into a pair", "03-make-your-own.png"),
    lambda: single("home", "home.png", "Fresh wallpapers every day", "A new pick every morning", "04-fresh.png"),
    lambda: single("preview", "home_preview.png", "Preview before you set it", "See it with your own apps", "05-preview.png"),
    lambda: single("discover", "discover.png", "10 categories, 130+ wallpapers", "Search by style, vibe or colour", "06-discover.png"),
]
for make in SHOTS:
    make()

# Feature graphic 1024x500: name and tagline on the left, two framed captures on the right
fg = background(1024, 500)
d = ImageDraw.Draw(fg)
d.text((56, 130), "Wallora", font=font("Bold", 118), fill=(226, 190, 255))
d.text((60, 280), "Day/Night Wallpapers", font=font("Bold", 40), fill=(255, 255, 255))
d.text((60, 340), "Light by day, dark at night", font=font("Regular", 30), fill=(200, 196, 228))
for raw, x, y in (("lock_light.png", 560, 70), ("lock_dark.png", 790, 40)):
    fr = phone(Image.open(RAW / raw), 190)
    tmp = Image.new("RGBA", fg.size, (0, 0, 0, 0)); tmp.paste(fr, (x, y), fr)
    fg = Image.alpha_composite(fg.convert("RGBA"), tmp).convert("RGB")
fg.save(OUT / "feature-graphic.png")
print("done")
