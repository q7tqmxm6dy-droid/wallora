#!/usr/bin/env python3
"""Exports the bundled catalogue and web-sized images for the web app (firebase/webadmin/app).

Reads the same Kotlin sources as the Android app (WallpaperCatalog.kt, Taxonomy.kt, ColorIndex.kt), so the web
app cannot drift from it. Run from the repo root:   python3 scripts/export_web_catalog.py

Images whose rights have not been verified (the owner-supplied nightcar_* and mood_* sets) are NOT published on
the open web unless you pass --include-unverified: a public website is crawled and indexed in a way a store listing
is not. See NIGHT_CARS_SOURCE.md and MOOD_IMAGES_SOURCE.md.
"""
import json, re, sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "app/src/main/java/com/wallora/app/data"
RES = ROOT / "app/src/main/res/drawable-nodpi"
OUT = ROOT / "firebase/webadmin/app"
UNVERIFIED = ("nightcar_", "mood_")
THUMB_W, FULL_W = 420, 1080

include_unverified = "--include-unverified" in sys.argv
catalog_src = (DATA / "WallpaperCatalog.kt").read_text()
taxonomy_src = re.sub(r"//[^\n]*", "", (DATA / "Taxonomy.kt").read_text())   # comments can contain ")" and quotes
color_src = (DATA / "ColorIndex.kt").read_text()

def block(name):
    m = re.search(name + r"\s*[:=][^=]*?mapOf\((.*?)\n    \)", taxonomy_src, re.S)
    return dict(re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', m.group(1)))

by_old = block("val byOldId")
by_title = block("val byTitle")
retired = re.search(r"val retiredIds[^=]*=\s*setOf\((.*?)\)", taxonomy_src, re.S).group(1)
retired = re.findall(r'"([^"]+)"', retired)
ids = re.findall(r'Category\("([a-z0-9]+)", "([^"]+)", "([^"]*)"\)', taxonomy_src)
colors = {k: re.findall(r'"([a-z]+)"', v) for k, v in re.findall(r'"([^"]+)"\s+to\s+listOf\(([^)]*)\)', color_src)}

def category_for(old, title):
    if old in {i for i, _, _ in ids}: return old
    return by_title.get(title.strip().lower()) or by_old.get(old) or "pinterest"

rows = []
for m in re.finditer(r'Wallpaper\("([^"]+)", "([^"]+)", "([^"]+)", R\.drawable\.(\w+)([^\n]*)\),', catalog_src):
    wid, title, cat, light, rest = m.groups()
    if not include_unverified and wid.startswith(UNVERIFIED): continue
    dark = re.search(r"darkResId = R\.drawable\.(\w+)", rest)
    credit = re.search(r'credit = "([^"]*)"', rest); lic = re.search(r'license = "([^"]*)"', rest)
    rows.append(dict(id=wid, title=title, cat=category_for(cat, title), light=light, dark=dark.group(1) if dark else None,
                     credit=credit.group(1) if credit else None, license=lic.group(1) if lic else None,
                     colors=colors.get(wid, [])))

(OUT / "img/t").mkdir(parents=True, exist_ok=True); (OUT / "img/f").mkdir(parents=True, exist_ok=True)
def save(src, dst, width, q):
    im = Image.open(RES / f"{src}.jpg").convert("RGB")
    if im.width > width: im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    im.save(dst, "WEBP", quality=q, method=4)

out = []
for r in rows:
    main = r["dark"] or r["light"]                      # the web app is dark, so a wallpaper's dark variant is its default
    save(main, OUT / f"img/t/{r['id']}.webp", THUMB_W, 70); save(main, OUT / f"img/f/{r['id']}.webp", FULL_W, 80)
    both = bool(r["dark"])
    if both: save(r["light"], OUT / f"img/f/{r['id']}~l.webp", FULL_W, 80)
    out.append(dict(id=r["id"], title=r["title"], cat=r["cat"], colors=r["colors"], both=both,
                    **({"credit": r["credit"]} if r["credit"] else {}), **({"license": r["license"]} if r["license"] else {})))

catalog = dict(
    categories=[dict(id=i, title=t, emoji=e) for i, t, e in ids],
    taxonomy=dict(byOldId=by_old, byTitle=by_title), retired=retired, wallpapers=out)
(OUT / "catalog.json").write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")))
total = sum(f.stat().st_size for f in (OUT / "img").rglob("*.webp"))
print(f"{len(out)} wallpapers ({len(rows)} parsed, unverified sets {'included' if include_unverified else 'excluded'}); "
      f"{len(ids)} categories; images {total/1e6:.1f} MB; catalog {(OUT/'catalog.json').stat().st_size/1e3:.0f} KB")
