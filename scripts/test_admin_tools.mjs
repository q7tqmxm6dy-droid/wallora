// Run with: node scripts/test_admin_tools.mjs
import assert from "node:assert/strict";
import {
  resolutionClass, issuesFor, filterAndSort, reorder, buildStats, toCsv, flatness, hamming, markDuplicates, isScheduled,
} from "../firebase/webadmin/admin-tools.js";

const NOW = Date.UTC(2026, 10, 18);
const day = 24 * 3600 * 1000;
const w = (id, extra = {}) => ({ id, title: id, category: "nature", ...extra });

// resolution classes
assert.equal(resolutionClass(w("a", { width: 2160, height: 3840 })), "uhd");
assert.equal(resolutionClass(w("a", { width: 3840, height: 2160 })), "uhd");
assert.equal(resolutionClass(w("a", { width: 1080, height: 1920 })), "hd");
assert.equal(resolutionClass(w("a", { width: 720, height: 1280 })), "low");
assert.equal(resolutionClass(w("a")), "unknown");
assert.equal(resolutionClass(w("a"), { width: 2160, height: 3840 }), "uhd"); // size from a scan

// issues
assert.deepEqual(issuesFor(w("a")), []); // not scanned: no guess
assert.deepEqual(issuesFor(w("a", { width: 720, height: 1280 })), ["lowres"]);
assert.deepEqual(issuesFor(w("a", { width: 1920, height: 1080 })), ["landscape"]);
assert.deepEqual(issuesFor(w("a"), { failed: true }), ["nosize"]);
assert.deepEqual(issuesFor(w("a", { width: 1080, height: 1920 }), { flat: true, dupOf: "b" }), ["flat", "duplicate"]);

// filters and sorting
const items = [
  w("c", { title: "Cedar", category: "city", featured: true, order: 5, tags: ["dark"], width: 2160, height: 3840, createdAt: new Date(NOW - 2 * day) }),
  w("a", { title: "Aurora", urlDark: "x", order: 1, createdAt: new Date(NOW - 20 * day) }),
  w("b", { title: "Birch", order: 1, tags: ["tree", "green"], publishAt: new Date(NOW + day), createdAt: new Date(NOW - 9 * day) }),
];
const ids = (l) => l.map((x) => x.id).join("");
assert.equal(ids(filterAndSort(items, {})), "cab"); // featured first, then order, then title
assert.equal(ids(filterAndSort(items, { sort: "title" })), "abc");
assert.equal(ids(filterAndSort(items, { sort: "newest" })), "cba");
assert.equal(ids(filterAndSort(items, { sort: "oldest" })), "abc");
assert.equal(ids(filterAndSort(items, { cat: "city" })), "c");
assert.equal(ids(filterAndSort(items, { filter: "uhd" })), "c");
assert.equal(ids(filterAndSort(items, { filter: "featured" })), "c");
assert.equal(ids(filterAndSort(items, { filter: "scheduled" }, {}, NOW)), "b");
assert.equal(ids(filterAndSort(items, { filter: "pair" })), "a");
assert.equal(ids(filterAndSort(items, { filter: "single" })), "cb");
assert.equal(ids(filterAndSort(items, { filter: "notags" })), "a");
assert.equal(ids(filterAndSort(items, { q: "gre" })), "b"); // matches a tag
assert.equal(ids(filterAndSort(items, { filter: "issues" }, { a: { width: 500, height: 900 } })), "a");
assert.equal(isScheduled(items[2], NOW), true);

// reorder: moving "b" up swaps it with "a" and renumbers
const r = reorder(items, "b", -1);
assert.deepEqual(r, { c: 10, b: 20, a: 30 });
assert.deepEqual(reorder(items, "c", -1), {}); // already first
assert.deepEqual(reorder(items, "zzz", 1), {});
assert.deepEqual(reorder(items, "a", -1), {}); // "a" is not featured, so it cannot move above the featured "c"
assert.deepEqual(reorder(items, "c", 1), {}); // and "c" cannot move below the others

// statistics
const s = buildStats(items, {}, NOW);
assert.equal(s.total, 3);
assert.equal(s.pairs, 1);
assert.equal(s.featured, 1);
assert.equal(s.scheduled, 1);
assert.equal(s.noTags, 1);
assert.equal(s.res.uhd, 1);
assert.equal(s.res.unknown, 2);
assert.equal(s.byCategory.nature, 2);
assert.equal(s.perWeek.length, 8);
assert.equal(s.perWeek.reduce((n, x) => n + x.count, 0), 3);
assert.equal(s.healthPct, null); // nothing scanned yet
const scans = { a: { width: 1080, height: 1920 }, b: { width: 1080, height: 1920 }, c: { width: 2160, height: 3840 } };
assert.equal(buildStats(items, scans, NOW).healthPct, 100);
assert.equal(buildStats(items, { ...scans, a: { width: 500, height: 900 } }, NOW).healthPct, 67);

// CSV quoting
const csv = toCsv([w("x", { title: 'Say "hi", ok', tags: ["a", "b"], url: "u" })]);
assert.ok(csv.split("\n")[1].includes('"Say ""hi"", ok"'));
assert.equal(csv.split("\n").length, 2);

// flat picture check on synthetic pixels
const px = (fn) => { const d = new Uint8ClampedArray(64 * 64 * 4); for (let i = 0; i < 4096; i++) { const [r, g, b] = fn(i % 64, Math.floor(i / 64)); d.set([r, g, b, 255], i * 4); } return d; };
assert.equal(flatness(px((x, y) => (x > 20 && x < 40 && y > 20 && y < 40 ? [20, 20, 25] : [225, 235, 250]))).flat, true); // dark blob on pale
let seed = 1; const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
assert.equal(flatness(px(() => [rnd() * 255, rnd() * 255, rnd() * 255])).flat, false); // noisy photo-like
assert.equal(flatness(px((x, y) => [x * 4, y * 4, 128])).flat, false); // smooth gradient is not flat art

// duplicates
const bits = (n) => "0".repeat(64 - n) + "1".repeat(n);
assert.equal(hamming(bits(3), bits(0)), 3);
const sc = { p: { hash: bits(10), avg: [100, 100, 100] }, q: { hash: bits(11), avg: [105, 100, 100] }, r: { hash: bits(40), avg: [100, 100, 100] }, t: { hash: bits(10), avg: [200, 0, 0] } };
markDuplicates(sc);
assert.equal(sc.q.dupOf, "p");
assert.equal(sc.r.dupOf, undefined);
assert.equal(sc.t.dupOf, undefined); // same shape, different colours

console.log("admin-tools: all tests passed");
