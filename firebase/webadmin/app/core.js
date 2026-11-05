// Pure logic shared by the UI and the tests (no DOM). It mirrors the Android app: Curation.kt, Recommender.kt,
// Taxonomy.kt and CatalogMerge.kt, so a wallpaper is filed, ranked and merged the same way on both.

export const TREND_WINDOW_DAYS = 3;
const FLAGSHIP_PREFIXES = ['photo_', 'dream_', 'lux_', 'trend_', 'viral_', 'nightcar_'];

/** Java's String.hashCode, so ids hash the same here as in the app. */
export function javaHash(s) {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0;
  return h;
}

function mix(a, b) {
  let h = (Math.imul(a, -1640531535) + ((b * 40503) | 0)) | 0;
  h ^= h >>> 15; h = Math.imul(h, 0x2c1b3c6d); h ^= h >>> 12; h = Math.imul(h, 0x297a2d39); h ^= h >>> 15;
  return h | 0;
}

/** Small seeded PRNG (mulberry32): the same seed always gives the same sequence. */
export function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6D2B79F5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function shuffled(arr, seed) {
  const r = rng(seed), a = [...arr];
  for (let i = a.length - 1; i > 0; i--) { const j = Math.floor(r() * (i + 1)); [a[i], a[j]] = [a[j], a[i]]; }
  return a;
}

/** Local calendar day as a number, so "today" changes at the user's midnight. */
export function epochDay(date = new Date()) {
  return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86400000);
}

export const isFlagship = (w) => !!w.uhd || FLAGSHIP_PREFIXES.some((p) => w.id.startsWith(p));

export function trendScore(w, day) {
  const rolled = (mix(javaHash(w.id), Math.floor(day / TREND_WINDOW_DAYS)) >>> 1) % 1000;
  return rolled + (w.featured ? 400 : 0) + (w.uhd ? 150 : 0) + (w.remote ? 100 : 0) + (isFlagship(w) ? 350 : 0);
}

export const trending = (all, day, n) => [...all].sort((a, b) => trendScore(b, day) - trendScore(a, day)).slice(0, n);

/** The hero carousel: the day's strongest flagship pick from each category, so it has variety. */
export function featured(all, day, n) {
  const flagship = all.filter(isFlagship);
  const pool = flagship.length ? flagship : all;
  const groups = new Map();
  for (const w of pool) groups.set(w.cat, [...(groups.get(w.cat) || []), w]);
  const best = [...groups.values()].map((g) => g.reduce((a, b) => (trendScore(b, day + 1) > trendScore(a, day + 1) ? b : a)));
  return best.sort((a, b) => trendScore(b, day + 1) - trendScore(a, day + 1)).slice(0, n);
}

export function dailyDrop(all, day, n) {
  const flagship = all.filter(isFlagship);
  return shuffled(flagship.length >= n ? flagship : all, day * 7919).slice(0, n);
}

export const feed = (all, day) => shuffled(all, day * 104729 + 17);

const RATIOS = [0.5625, 0.66, 0.75, 0.62, 0.5625, 0.7];
/** Varied card heights give the grid its Pinterest rhythm even though every image is 9:16. */
export const cardRatio = (id) => RATIOS[(javaHash(id) & 0x7fffffff) % RATIOS.length];

const titled = (...names) => { const set = new Set(names.map((n) => n.toLowerCase())); return (w) => set.has(w.title.trim().toLowerCase()); };
export const viralStyles = [
  { id: 'aura', title: 'Aura & Dreamy', emoji: '🌈', matches: titled('Aura Glow', 'Coquette', 'Boho Arches', 'Soft Circle', 'Aqua Ring', 'Pastel Arch', 'Sunset Fade', 'Violet Dusk', 'Cherry Blossom', 'Glass Orbs', 'Paper Dawn') },
  { id: 'y2k', title: 'Y2K Chrome', emoji: '💿', matches: titled('Chrome Star', 'Liquid Metal', 'Retro Glow', 'Laser Arc', 'Neon Dreams', 'Prism', 'Low Poly', 'Facets') },
  { id: 'moody', title: 'Dark & Moody', emoji: '🕯️', matches: (w) => w.cat === 'dark' || titled('Noir Gold', 'Foggy Trail', 'Night Runner', 'Graphite Coupe')(w) },
  { id: 'neon', title: 'Neon Nights', emoji: '🌃', matches: (w) => w.cat === 'city' || w.cat === 'gaming' },
  { id: 'cosmic', title: 'Cosmic', emoji: '🪐', matches: (w) => w.cat === 'space' },
  { id: 'luxe', title: 'Golden Luxe', emoji: '✨', matches: (w) => w.cat === 'luxury' || titled('Golden Hour', 'Golden Ridge', 'Golden Bars')(w) },
  { id: 'soft', title: 'Soft Girl', emoji: '🎀', matches: (w) => w.cat === 'cute' || titled('Coquette', 'Whiskers')(w) },
];

export const COLORS = [
  ['red', '#E53935'], ['orange', '#FB8C00'], ['yellow', '#FDD835'], ['green', '#43A047'], ['teal', '#00ACC1'], ['blue', '#1E88E5'],
  ['purple', '#8E24AA'], ['pink', '#EC407A'], ['brown', '#6D4C41'], ['black', '#111111'], ['gray', '#9E9E9E'], ['white', '#F5F5F5'],
];

/**
 * "Picked for you": scored on the device, so nothing about a visitor's taste leaves the browser.
 * A favourite counts most, a download next, a view least; favourited wallpapers are left out.
 */
export function pick(all, favouriteIds, viewedIds, downloadedIds, day, n) {
  const byId = new Map(all.map((w) => [w.id, w]));
  const cat = new Map(), col = new Map();
  const learn = (ids, weight) => { for (const id of ids) { const w = byId.get(id); if (!w) continue;
    cat.set(w.cat, (cat.get(w.cat) || 0) + weight); for (const c of w.colors || []) col.set(c, (col.get(c) || 0) + weight); } };
  learn(favouriteIds, 4); learn(downloadedIds, 3); learn(viewedIds, 1);
  if (!cat.size) return trending(all, day, n);
  const maxCat = Math.max(...cat.values()), maxCol = Math.max(1, ...col.values());
  const score = (w) => ((cat.get(w.cat) || 0) / maxCat) * 700 + ((w.colors || []).reduce((s, c) => s + (col.get(c) || 0), 0) / maxCol) * 250 + trendScore(w, day) * 0.12;
  const fav = new Set(favouriteIds);
  return all.filter((w) => !fav.has(w.id)).sort((a, b) => score(b) - score(a)).slice(0, n);
}

/** Old category ids (aurora, posters ...) and titles are filed under the ten browsing categories. */
export function categoryFor(oldId, title, tax, categoryIds) {
  if (categoryIds.includes(oldId)) return oldId;
  return tax.byTitle[title.trim().toLowerCase()] || tax.byOldId[oldId] || 'pinterest';
}

const titleKey = (w) => w.title.trim().toLowerCase();

/** Cloud wallpapers replace bundled ones with the same id or title, keeping the bundled category, credit and colours. */
export function mergeCatalog(cloud, bundled) {
  const byId = new Map(bundled.map((w) => [w.id, w])), byTitle = new Map(bundled.map((w) => [titleKey(w), w]));
  const replaced = new Set(), merged = new Map();
  for (const c of cloud) {
    const twin = byId.get(c.id) || byTitle.get(titleKey(c));
    if (twin) replaced.add(twin.id);
    merged.set(c.id, twin ? { ...c, cat: twin.cat, credit: c.credit ?? twin.credit, license: c.license ?? twin.license,
      colors: c.colors && c.colors.length ? c.colors : twin.colors, thumb: twin.thumb,
      // keep the bundled same-origin copy: it previews fast and can be resized in the browser, unlike the cloud original
      local: twin.local, localLight: twin.localLight } : c);
  }
  for (const b of bundled) if (!replaced.has(b.id) && !merged.has(b.id)) merged.set(b.id, b);
  return [...merged.values()];
}

/** Turns the Firestore REST response into wallpaper objects. Anything without an https image is dropped. */
export function parseCloud(json, tax, categoryIds, retired = []) {
  const https = (u) => (typeof u === 'string' && u.startsWith('https://') ? u : null);
  const out = [];
  for (const d of json.documents || []) {
    const id = d.name.split('/').pop();
    if (retired.includes(id)) continue;
    const f = d.fields || {}, s = (k) => f[k]?.stringValue;
    const url = https(s('url'));
    if (!url) continue;
    const publishAt = f.publishAt?.timestampValue;
    if (publishAt && Date.parse(publishAt) > Date.now()) continue;
    const title = ((s('title') || 'Untitled').replace(/\.(jpe?g|png|webp)$/i, '').trim()) || 'Untitled';
    const dark = https(s('urlDark'));
    const width = Number(f.width?.integerValue || 0), height = Number(f.height?.integerValue || 0);
    out.push({
      id, title, cat: categoryFor(s('category') || 'remote', title, tax, categoryIds),
      colors: (f.colors?.arrayValue?.values || []).map((v) => v.stringValue).filter(Boolean),
      credit: s('credit') || undefined, license: s('license') || undefined, remote: true,
      featured: !!f.featured?.booleanValue, order: Number(f.order?.integerValue || 0),
      uhd: !!f.uhd?.booleanValue || (Math.min(width, height) >= 2160 && Math.max(width, height) >= 3840),
      full: dark || url, fullLight: dark ? url : null, both: !!dark, thumb: dark || url,
    });
  }
  return out.sort((a, b) => (b.featured - a.featured) || (a.order - b.order) || a.title.localeCompare(b.title));
}
