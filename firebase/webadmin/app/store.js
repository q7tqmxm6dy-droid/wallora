// Everything a visitor keeps (favourites, recent views, saved downloads) lives in this browser only.
// Storage can be unavailable (private windows, blocked cookies), so every read and write is guarded and the app
// keeps working in memory when it fails.
const memory = new Map();
const read = (key, fallback) => {
  try { const raw = localStorage.getItem(key); if (raw != null) return JSON.parse(raw); } catch (_) { /* fall through */ }
  return memory.has(key) ? memory.get(key) : fallback;
};
const write = (key, value) => {
  memory.set(key, value);
  try { localStorage.setItem(key, JSON.stringify(value)); } catch (_) { /* in-memory only */ }
};

export const favourites = {
  all: () => new Set(read('wl_fav', [])),
  has: (id) => favourites.all().has(id),
  /** Flips a favourite and returns whether it is now a favourite. */
  toggle: (id) => {
    const s = favourites.all();
    const on = !s.has(id);
    if (on) s.add(id); else s.delete(id);
    write('wl_fav', [...s]);
    return on;
  },
};
export const viewed = {
  all: () => read('wl_viewed', []),
  add: (id) => write('wl_viewed', [id, ...viewed.all().filter((x) => x !== id)].slice(0, 24)),
  clear: () => write('wl_viewed', []),
};
export const downloads = {
  all: () => read('wl_dl', []),
  add: (rec) => write('wl_dl', [rec, ...downloads.all()].slice(0, 300)),
  clear: () => write('wl_dl', []),
};
export const prefs = {
  get: (k, d) => read('wl_pref_' + k, d),
  set: (k, v) => write('wl_pref_' + k, v),
};
export const cloudCache = {
  get: () => read('wl_cloud', null),
  set: (json) => write('wl_cloud', { at: Date.now(), json }),
};
