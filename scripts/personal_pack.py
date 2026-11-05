#!/usr/bin/env python3
"""Builds the private, debug-only wallpaper pack from personal-pack/pack.tsv.

pack.tsv lines are "<file name in the source folder><TAB><title>". The pictures are resized and written to
app/src/debug/res/drawable-nodpi/personal_<slug>.jpg, which only debug builds compile in (and which git ignores), so a
release bundle for Play can never contain them. Usage:  python3 scripts/personal_pack.py "<source folder>"
"""
import os, re, sys
from PIL import Image

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
src = sys.argv[1]
out = os.path.join(root, "app/src/debug/res/drawable-nodpi")
os.makedirs(out, exist_ok=True)
for old in os.listdir(out):
    if old.startswith("personal_"):
        os.remove(os.path.join(out, old))
count = 0
for line in open(os.path.join(root, "personal-pack/pack.tsv"), encoding="utf-8"):
    name, title = line.rstrip("\n").split("\t")
    slug = re.sub(r"[^a-z0-9]+", "_", title.lower()).strip("_")
    im = Image.open(os.path.join(src, name)).convert("RGB")
    im.thumbnail((1920, 1920), Image.LANCZOS)
    im.save(os.path.join(out, f"personal_{slug}.jpg"), quality=88, optimize=True)
    count += 1
print(count, "pictures written to", out)
