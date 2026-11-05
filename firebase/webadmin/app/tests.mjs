// Run with:  node firebase/webadmin/app/tests.mjs     (needs catalog.json from scripts/export_web_catalog.py)
import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import * as core from './core.js';

const catalog = JSON.parse(readFileSync(new URL('./catalog.json', import.meta.url)));
const ids = catalog.categories.map((c) => c.id);
const all = catalog.wallpapers.map((w) => ({ ...w, thumb: `img/t/${w.id}.webp`, full: `img/f/${w.id}.webp`, local: `img/f/${w.id}.webp` }));
let passed = 0;
const test = (name, fn) => { try { fn(); passed++; } catch (e) { console.error('FAIL', name, '\n ', e.message); process.exitCode = 1; } };

test('java hash matches the JVM', () => { assert.equal(core.javaHash('abc'), 96354); assert.equal(core.javaHash(''), 0); });
test('every wallpaper is in one of the ten categories', () => assert.deepEqual(all.filter((w) => !ids.includes(w.cat)).map((w) => w.id), []));
test('every category has wallpapers', () => assert.deepEqual(ids.filter((id) => !all.some((w) => w.cat === id)), []));
test('every wallpaper has colours', () => assert.deepEqual(all.filter((w) => !w.colors.length).map((w) => w.id), []));
test('every viral style has wallpapers', () => assert.deepEqual(core.viralStyles.filter((s) => !all.some(s.matches)).map((s) => s.id), []));
test('curation is deterministic and changes daily', () => {
  assert.deepEqual(core.dailyDrop(all, 20000, 8), core.dailyDrop(all, 20000, 8));
  assert.notDeepEqual(core.dailyDrop(all, 20000, 8).map((w) => w.id), core.dailyDrop(all, 20001, 8).map((w) => w.id));
  assert.notDeepEqual(core.trending(all, 20000, 10).map((w) => w.id), core.trending(all, 20009, 10).map((w) => w.id));
});
test('trending is stable inside a window', () => {
  const start = 20001 - (20001 % core.TREND_WINDOW_DAYS);
  assert.deepEqual(core.trending(all, start, 10), core.trending(all, start + 1, 10));
});
test('featured spans categories', () => { const f = core.featured(all, 20000, 6); assert.equal(f.length, 6); assert.equal(new Set(f.map((w) => w.cat)).size, 6); });
test('new users get trending; a favourite category dominates', () => {
  assert.deepEqual(core.pick(all, [], [], [], 5, 8), core.trending(all, 5, 8));
  const space = all.filter((w) => w.cat === 'space').slice(0, 2).map((w) => w.id);
  assert.ok(core.pick(all, space, space, [], 5, 6).filter((w) => w.cat === 'space').length >= 4);
});
test('favourites are not recommended again', () => {
  const favs = all.slice(0, 3).map((w) => w.id);
  assert.ok(core.pick(all, favs, [], [], 1, 20).every((w) => !favs.includes(w.id)));
});
test('card ratios vary but are stable', () => { assert.ok(new Set(all.map((w) => core.cardRatio(w.id))).size > 2); assert.equal(core.cardRatio('x'), core.cardRatio('x')); });

const doc = (id, f) => ({ name: `projects/p/databases/(default)/documents/wallpapers/${id}`, fields: f });
const S = (v) => ({ stringValue: v });
test('cloud parsing files, strips extensions, drops bad and retired entries', () => {
  const json = { documents: [
    doc('a', { title: S('White Audi R8 fl.jpg'), category: S('cars'), url: S('https://x/a.jpg') }),
    doc('b', { title: S('Old'), category: S('aurora'), url: S('http://insecure/b.jpg') }),
    doc('cars_p01', { title: S('Scarlet Coupe'), category: S('cars'), url: S('https://x/c.jpg') }),
    doc('d', { title: S('Future'), category: S('cars'), url: S('https://x/d.jpg'), publishAt: { timestampValue: '2999-01-01T00:00:00Z' } }),
    doc('e', { title: S('Plain'), category: S('aurora'), url: S('https://x/e.jpg'), width: { integerValue: '2160' }, height: { integerValue: '3840' } }),
  ] };
  const out = core.parseCloud(json, catalog.taxonomy, ids, catalog.retired);
  assert.deepEqual(out.map((w) => w.id).sort(), ['a', 'e']);
  assert.equal(out.find((w) => w.id === 'a').title, 'White Audi R8 fl');
  assert.equal(out.find((w) => w.id === 'e').cat, 'nature');
  assert.equal(out.find((w) => w.id === 'e').uhd, true);
});
test('a cloud copy replaces its bundled twin and keeps credit, category and thumbnail', () => {
  const bundled = [{ id: 'photo_lake', title: 'Lake Louise', cat: 'nature', colors: ['blue'], credit: 'T. Ott', license: 'CC BY 2.0', thumb: 'img/t/photo_lake.webp', full: 'img/f/photo_lake.webp', local: 'img/f/photo_lake.webp', localLight: null }];
  const cloud = [{ id: 'auto123', title: ' lake louise ', cat: 'pinterest', colors: [], remote: true, uhd: true, full: 'https://x/4k.jpg', thumb: 'https://x/4k.jpg' }];
  const m = core.mergeCatalog(cloud, bundled);
  assert.equal(m.length, 1); assert.equal(m[0].id, 'auto123'); assert.equal(m[0].cat, 'nature'); assert.equal(m[0].credit, 'T. Ott');
  assert.equal(m[0].thumb, 'img/t/photo_lake.webp'); assert.equal(m[0].local, 'img/f/photo_lake.webp'); assert.equal(m[0].full, 'https://x/4k.jpg'); assert.deepEqual(m[0].colors, ['blue']); assert.equal(m[0].uhd, true);
});
test('different titles stay separate', () => assert.equal(core.mergeCatalog([{ id: 'n', title: 'Sunrise', cat: 'x' }], [{ id: 'b', title: 'Sunset', cat: 'x' }]).length, 2));
test('old category ids are mapped', () => {
  assert.equal(core.categoryFor('neon', 'Anything', catalog.taxonomy, ids), 'gaming');
  assert.equal(core.categoryFor('posters', 'Summit', catalog.taxonomy, ids), 'nature');
  assert.equal(core.categoryFor('dark', 'x', catalog.taxonomy, ids), 'dark');
  assert.equal(core.categoryFor('unknown', 'x', catalog.taxonomy, ids), 'pinterest');
});
console.log(process.exitCode ? 'FAILED' : `all ${passed} tests passed`);
