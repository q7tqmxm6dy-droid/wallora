#!/usr/bin/env python3
"""Builds app/src/main/res/values-<lang>/strings.xml from scripts/locales/<lang>.txt.

Each .txt line is `key|text` (a plain string) or `plural:name:quantity|text`. The texts are written as plain text: this script escapes
&, ', " and < for Android. It refuses to write a language unless every translatable string and every counted phrase of the English
file is present, with the same placeholders (%1$s, %d ...), so a missing or broken translation can never reach the app.

Run from the repo root:  python3 scripts/gen_locales.py es pt hi tr
"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
EN = (RES / "values/strings.xml").read_text()

strings = [(k, v) for k, a, v in re.findall(r'<string name="([^"]+)"([^>]*)>(.*?)</string>', EN, flags=re.S) if 'translatable="false"' not in a]
plurals = {m.group(1): dict(re.findall(r'<item quantity="(\w+)">(.*?)</item>', m.group(2))) for m in re.finditer(r'<plurals name="([^"]+)">(.*?)</plurals>', EN, flags=re.S)}
# English quantities define which placeholder each phrase needs; the other languages may need more quantities (es/pt: many)
QUANTITIES = {"es": ["one", "many", "other"], "pt": ["one", "many", "other"], "hi": ["one", "other"], "tr": ["one", "other"]}

def unescape(v): return v.replace("\\'", "'").replace('\\"', '"').replace("&amp;", "&")
def escape(v): return v.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'").replace('"', '\\"')
def holders(v): return sorted(re.findall(r"%\d*\$?\d*[sd]", unescape(v)))

def build(lang):
    src = {}
    for n, line in enumerate((ROOT / f"scripts/locales/{lang}.txt").read_text().splitlines(), 1):
        if not line.strip(): continue
        key, _, text = line.partition("|")
        if not text: sys.exit(f"{lang}.txt line {n}: no text for {key!r}")
        src[key] = text
    out, problems = [], []
    for k, v in strings:
        if k not in src: problems.append(f"missing string {k}"); continue
        if holders(src[k]) != holders(v): problems.append(f"placeholders differ in {k}: {holders(v)} vs {holders(src[k])}")
        out.append(f'    <string name="{k}">{escape(src[k])}</string>')
    for name, items in plurals.items():
        lines = []
        for q in QUANTITIES[lang]:
            key = f"plural:{name}:{q}"
            if key not in src: problems.append(f"missing {key}"); continue
            lines.append(f'        <item quantity="{q}">{escape(src[key])}</item>')
        out.append(f'    <plurals name="{name}">\n' + "\n".join(lines) + "\n    </plurals>")
    known = {k for k, _ in strings} | {f"plural:{n}:{q}" for n in plurals for q in QUANTITIES[lang]}
    problems += [f"unknown key {k}" for k in src if k not in known]
    if problems: sys.exit(f"{lang}: " + "; ".join(problems[:12]) + (f" (+{len(problems) - 12} more)" if len(problems) > 12 else ""))
    d = RES / f"values-{lang}"; d.mkdir(exist_ok=True)
    (d / "strings.xml").write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' + "\n".join(out) + "\n</resources>\n")
    print(lang, "ok:", len(strings), "strings,", len(plurals), "plurals")

for lang in sys.argv[1:]: build(lang)
