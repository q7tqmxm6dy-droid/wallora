#!/usr/bin/env python3
"""Exports the bundled catalogue and web-sized images for the web app (firebase/webadmin/app).

Reads the same Kotlin sources as the Android app (WallpaperCatalog.kt, Taxonomy.kt, ColorIndex.kt), so the web
app cannot drift from it. Run from the repo root:   python3 scripts/export_web_catalog.py

It also writes firebase/webadmin/admin-catalog.json, the full list of wallpapers built into the app, for the admin
dashboard. That list names every wallpaper but points only at images that are already on the website.

Images whose rights have not been verified (the owner-supplied nightcar_* and mood_* sets) are NOT published on
the open web, because a public website is crawled and indexed in a way a store listing is not. Two exceptions:
  * WEB_ALLOWED_UNVERIFIED: a short allow-list of pictures judged low-risk (scenery, space, abstract art, quote art,
    gradients: no people, characters, cars or brands), chosen by the owner for the website.
  * --include-unverified publishes everything.
See NIGHT_CARS_SOURCE.md and MOOD_IMAGES_SOURCE.md.
"""
import json, re, sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "app/src/main/java/com/wallora/app/data"
RES = ROOT / "app/src/main/res/drawable-nodpi"
OUT = ROOT / "firebase/webadmin/app"
UNVERIFIED = ("nightcar_", "mood_")
# Owner-approved subset of the unverified sets for the open web. Everything else (cars, gym photos, anime characters,
# interiors, product shots, people) stays app-only.
WEB_ALLOWED_UNVERIFIED = {
    "mood_01", "mood_02", "mood_03", "mood_06", "mood_08", "mood_09", "mood_16", "mood_17",   # tech render, shore, rose, peak, sky, wings, tide, chrome
    "mood_20", "mood_21", "mood_22", "mood_23", "mood_25", "mood_26", "mood_27", "mood_28",   # quote art, silk, nebula, sunset runner, galaxy
    "mood_29", "mood_32", "mood_33", "mood_35", "mood_36", "mood_37",                         # neon facets, chrome, quote art, peaks, gradient
}
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

rows, everything = [], []
for m in re.finditer(r'Wallpaper\("([^"]+)", "([^"]+)", "([^"]+)", R\.drawable\.(\w+)([^\n]*)\),', catalog_src):
    wid, title, cat, light, rest = m.groups()
    on_web = include_unverified or not wid.startswith(UNVERIFIED) or wid in WEB_ALLOWED_UNVERIFIED
    source = "supplied" if wid.startswith(UNVERIFIED) else ("photo" if 'credit = "' in rest else "original")
    dark = re.search(r"darkResId = R\.drawable\.(\w+)", rest)
    everything.append(dict(id=wid, title=title, cat=category_for(cat, title), source=source, web=on_web, both=bool(dark)))
    if not on_web: continue
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
    if both:
        save(r["light"], OUT / f"img/f/{r['id']}~l.webp", FULL_W, 80)
        save(r["light"], OUT / f"img/t/{r['id']}~l.webp", THUMB_W, 70)    # the admin dashboard draws pairs split, light | dark
    out.append(dict(id=r["id"], title=r["title"], cat=r["cat"], colors=r["colors"], both=both,
                    **({"credit": r["credit"]} if r["credit"] else {}), **({"license": r["license"]} if r["license"] else {})))

catalog = dict(
    categories=[dict(id=i, title=t, emoji=e) for i, t, e in ids],
    taxonomy=dict(byOldId=by_old, byTitle=by_title), retired=retired, wallpapers=out)
(OUT / "catalog.json").write_text(json.dumps(catalog, ensure_ascii=False, separators=(",", ":")))
(OUT.parent / "admin-catalog.json").write_text(json.dumps(dict(
    categories=[dict(id=i, title=t, emoji=e) for i, t, e in ids], wallpapers=everything), ensure_ascii=False, separators=(",", ":")))
total = sum(f.stat().st_size for f in (OUT / "img").rglob("*.webp"))
print(f"{len(out)} wallpapers on the web, {len(everything)} in the app ({len(rows)} parsed, unverified sets {'all included' if include_unverified else 'limited to the allow-list'}); "
      f"{len(ids)} categories; images {total/1e6:.1f} MB; catalog {(OUT/'catalog.json').stat().st_size/1e3:.0f} KB")
