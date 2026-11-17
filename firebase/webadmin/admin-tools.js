// Pure helpers for the admin dashboard: filtering, sorting, quality checks and statistics.
// Nothing here touches Firebase or the page, so it can be tested on its own (see scripts/test_admin_tools.mjs).
// The only browser-only part is scanImage(), which reads pixels through a canvas.

export const toDate = (v) => (v && v.toDate ? v.toDate() : v ? new Date(v) : null);

export const isScheduled = (w, now = Date.now()) => {
  const t = toDate(w.publishAt);
  return !!t && t.getTime() > now;
};

/** Pixel size of a wallpaper: the stored size if it has one, otherwise what a scan measured. */
export function sizeOf(w, scan) {
  const width = Number(w.width) || scan?.width || 0;
  const height = Number(w.height) || scan?.height || 0;
  return width && height ? { width, height } : null;
}

/** "uhd" (genuine 4K), "hd" (1080 x 1920 or better), "low" (smaller than that) or "unknown". */
export function resolutionClass(w, scan) {
  const s = sizeOf(w, scan);
  if (!s) return "unknown";
  const lo = Math.min(s.width, s.height), hi = Math.max(s.width, s.height);
  if (lo >= 2160 && hi >= 3840) return "uhd";
  if (lo >= 1080 && hi >= 1920) return "hd";
  return "low";
}

export const ISSUE_LABEL = {
  lowres: "Low resolution",
  landscape: "Not a phone shape",
  flat: "Possibly a flat illustration",
  duplicate: "Looks like a duplicate",
  nosize: "Size unknown",
};

/** What is wrong with a wallpaper. [scan] is the result of scanImage(), or undefined when it has not been scanned. */
export function issuesFor(w, scan) {
  const out = [];
  const s = sizeOf(w, scan);
  if (!s) {
    if (scan === undefined) return out; // not scanned yet: say nothing rather than guess
    out.push("nosize");
  } else {
    if (resolutionClass(w, scan) === "low") out.push("lowres");
    const ratio = s.width / s.height;
    if (ratio > 0.75 || ratio < 0.4) out.push("landscape");
  }
  if (scan?.flat) out.push("flat");
  if (scan?.dupOf) out.push("duplicate");
  return out;
}

export const FILTERS = [
  ["all", "All"],
  ["uhd", "4K"],
  ["featured", "Featured"],
  ["scheduled", "Scheduled"],
  ["pair", "Light + dark"],
  ["single", "One image"],
  ["notags", "No tags"],
  ["issues", "Needs attention"],
];

export function matchesFilter(w, filter, scanById, now = Date.now()) {
  const scan = scanById[w.id];
  switch (filter) {
    case "uhd": return resolutionClass(w, scan) === "uhd";
    case "featured": return !!w.featured;
    case "scheduled": return isScheduled(w, now);
    case "pair": return !!w.urlDark;
    case "single": return !w.urlDark;
    case "notags": return !(w.tags && w.tags.length);
    case "issues": return issuesFor(w, scan).length > 0;
    default: return true;
  }
}

/** The order the app itself uses: featured first, then the Order number, then the title. */
export const appOrder = (a, b) =>
  (b.featured ? 1 : 0) - (a.featured ? 1 : 0) ||
  (Number(a.order) || 0) - (Number(b.order) || 0) ||
  String(a.title || "").toLowerCase().localeCompare(String(b.title || "").toLowerCase());

const SORTS = {
  app: appOrder,
  newest: (a, b) => (toDate(b.createdAt)?.getTime() || 0) - (toDate(a.createdAt)?.getTime() || 0) || appOrder(a, b),
  oldest: (a, b) => (toDate(a.createdAt)?.getTime() || 0) - (toDate(b.createdAt)?.getTime() || 0) || appOrder(a, b),
  title: (a, b) => String(a.title || "").toLowerCase().localeCompare(String(b.title || "").toLowerCase()),
  category: (a, b) => String(a.category || "").localeCompare(String(b.category || "")) || appOrder(a, b),
};
export const SORT_LABEL = { app: "App order", newest: "Newest first", oldest: "Oldest first", title: "Title A–Z", category: "Category" };

export function filterAndSort(items, { q = "", cat = "", filter = "all", sort = "app" }, scanById = {}, now = Date.now()) {
  const needle = q.trim().toLowerCase();
  return items
    .filter((w) =>
      (!cat || w.category === cat) &&
      matchesFilter(w, filter, scanById, now) &&
      (!needle ||
        String(w.title || "").toLowerCase().includes(needle) ||
        String(w.category || "").toLowerCase().includes(needle) ||
        (w.tags || []).some((t) => String(t).toLowerCase().includes(needle))))
    .sort(SORTS[sort] || SORTS.app);
}

/**
 * New Order numbers after moving [id] up (-1) or down (+1) in the app's order, among the wallpapers that are
 * featured like it (featured ones always come first, so they never swap with the others).
 * Returns only the wallpapers whose number changed: { id: newOrder }.
 */
export function reorder(items, id, dir) {
  const list = [...items].sort(appOrder);
  const i = list.findIndex((w) => w.id === id);
  if (i < 0) return {};
  let j = i + dir;
  while (j >= 0 && j < list.length && !!list[j].featured !== !!list[i].featured) j += dir;
  if (j < 0 || j >= list.length) return {};
  [list[i], list[j]] = [list[j], list[i]];
  const changes = {};
  list.forEach((w, k) => {
    const next = (k + 1) * 10;
    if ((Number(w.order) || 0) !== next) changes[w.id] = next;
  });
  return changes;
}

const ymd = (d) => d.toISOString().slice(0, 10);

/** Everything the Insights card shows. */
export function buildStats(items, scanById = {}, now = Date.now(), weeks = 8) {
  const res = { uhd: 0, hd: 0, low: 0, unknown: 0 };
  const byCategory = {}, tagCounts = {};
  let pairs = 0, featured = 0, scheduled = 0, noTags = 0, withIssues = 0, scanned = 0;
  for (const w of items) {
    res[resolutionClass(w, scanById[w.id])]++;
    byCategory[w.category || "—"] = (byCategory[w.category || "—"] || 0) + 1;
    for (const t of w.tags || []) tagCounts[t] = (tagCounts[t] || 0) + 1;
    if (w.urlDark) pairs++;
    if (w.featured) featured++;
    if (isScheduled(w, now)) scheduled++;
    if (!(w.tags && w.tags.length)) noTags++;
    if (scanById[w.id]) scanned++;
    if (issuesFor(w, scanById[w.id]).length) withIssues++;
  }
  // wallpapers added per week, oldest first, ending with the current week
  const weekMs = 7 * 24 * 3600 * 1000;
  const perWeek = Array.from({ length: weeks }, (_, k) => ({
    from: ymd(new Date(now - (weeks - 1 - k) * weekMs)), count: 0,
  }));
  for (const w of items) {
    const t = toDate(w.createdAt)?.getTime();
    if (!t) continue;
    const age = Math.floor((now - t) / weekMs);
    if (age >= 0 && age < weeks) perWeek[weeks - 1 - age].count++;
  }
  const total = items.length;
  return {
    total, pairs, singles: total - pairs, featured, scheduled, noTags, withIssues, scanned,
    res, byCategory, perWeek,
    topTags: Object.entries(tagCounts).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0])).slice(0, 10),
    healthPct: total && scanned === total ? Math.round(((total - withIssues) / total) * 100) : null,
  };
}

export function toCsv(items) {
  const cell = (v) => {
    const s = v == null ? "" : String(v);
    return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const head = ["id", "title", "category", "tags", "featured", "order", "publishAt", "width", "height", "uhd", "pair", "url"];
  const lines = items.map((w) => [
    w.id, w.title, w.category, (w.tags || []).join(" | "), !!w.featured, Number(w.order) || 0,
    toDate(w.publishAt)?.toISOString() || "", w.width || "", w.height || "", !!w.uhd, !!w.urlDark, w.url,
  ].map(cell).join(","));
  return [head.join(","), ...lines].join("\n");
}

// ---- image scanning (browser only) -------------------------------------------------------------------------

/** Hamming distance between two equal-length bit strings. */
export function hamming(a, b) {
  let d = 0;
  for (let i = 0; i < a.length; i++) if (a[i] !== b[i]) d++;
  return d;
}

const colorGap = (a, b) => Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]), Math.abs(a[2] - b[2]));

/** Marks wallpapers that look like another one (same tiny fingerprint and average colour) with scan.dupOf = other id. */
export function markDuplicates(scanById) {
  const ids = Object.keys(scanById).filter((id) => scanById[id]?.hash);
  for (const id of ids) delete scanById[id].dupOf;
  for (let i = 0; i < ids.length; i++) {
    for (let j = i + 1; j < ids.length; j++) {
      const a = scanById[ids[i]], b = scanById[ids[j]];
      if (hamming(a.hash, b.hash) <= 3 && colorGap(a.avg, b.avg) < 20 && !b.dupOf) b.dupOf = ids[i];
    }
  }
}

/** Pure part of the flat-picture check, on 64 x 64 RGBA pixels. Tuned on the 48 flat wallpapers removed in v9. */
export function flatness(rgba, size = 64) {
  const n = size * size, counts = new Map();
  for (let p = 0; p < n; p++) {
    const key = (rgba[p * 4] >> 4) * 256 + (rgba[p * 4 + 1] >> 4) * 16 + (rgba[p * 4 + 2] >> 4);
    counts.set(key, (counts.get(key) || 0) + 1);
  }
  const top1 = Math.max(...counts.values()) / n;
  let calm = 0, total = 0;
  for (let y = 0; y < size - 1; y++) {
    for (let x = 0; x < size - 1; x++) {
      const a = (y * size + x) * 4, r = a + 4, d = a + size * 4;
      total++;
      let calmHere = true;
      for (let c = 0; c < 3; c++) if (Math.abs(rgba[a + c] - rgba[r + c]) > 3 || Math.abs(rgba[a + c] - rgba[d + c]) > 3) calmHere = false;
      if (calmHere) calm++;
    }
  }
  return { top1, calm: calm / total, flat: top1 >= 0.3 && calm / total >= 0.85 };
}

/** Loads an image and measures it: pixel size, whether it looks flat, a small fingerprint and its average colour. */
export function scanImage(url) {
  return new Promise((resolve) => {
    const img = new Image();
    let anonymous = true; // needed to read pixels; if the host refuses, retry without it to at least get the size
    img.crossOrigin = "anonymous";
    img.onload = () => {
      const out = { width: img.naturalWidth, height: img.naturalHeight };
      try {
        const cv = document.createElement("canvas");
        cv.width = cv.height = 64;
        const ctx = cv.getContext("2d", { willReadFrequently: true });
        ctx.drawImage(img, 0, 0, 64, 64);
        const px = ctx.getImageData(0, 0, 64, 64).data;
        Object.assign(out, flatness(px));
        let r = 0, g = 0, b = 0;
        for (let p = 0; p < 64 * 64; p++) { r += px[p * 4]; g += px[p * 4 + 1]; b += px[p * 4 + 2]; }
        out.avg = [r / 4096, g / 4096, b / 4096];
        const sm = document.createElement("canvas");
        sm.width = 9; sm.height = 8;
        const sctx = sm.getContext("2d", { willReadFrequently: true });
        sctx.drawImage(img, 0, 0, 9, 8);
        const s = sctx.getImageData(0, 0, 9, 8).data;
        const grey = (p) => s[p * 4] * 0.3 + s[p * 4 + 1] * 0.59 + s[p * 4 + 2] * 0.11;
        let bits = "";
        for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) bits += grey(y * 9 + x) > grey(y * 9 + x + 1) ? "1" : "0";
        out.hash = bits;
      } catch (_) {
        out.pixelsBlocked = true; // the size is still known; flat/duplicate checks need pixel access
      }
      if (!anonymous) out.pixelsBlocked = true;
      resolve(out);
    };
    img.onerror = () => {
      if (anonymous) { anonymous = false; img.removeAttribute("crossorigin"); img.src = url; } else resolve({ failed: true });
    };
    img.src = url;
  });
}
