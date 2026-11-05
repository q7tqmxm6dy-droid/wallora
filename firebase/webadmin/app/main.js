import * as core from './core.js';
import { STYLES, PALETTES, render as renderGen } from './generator.js';
import { favourites, viewed, downloads, prefs, cloudCache } from './store.js';

const FIRESTORE = 'https://firestore.googleapis.com/v1/projects/wallora-bc950/databases/(default)/documents/wallpapers?pageSize=300';
const SUPPORT_EMAIL = 'ahmad.qiblawi112@gmail.com';
const IOS = /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
const STANDALONE = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const enc = encodeURIComponent;

const ICON = {
  home: 'M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z',
  search: 'M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
  sparkle: 'M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z',
  trend: 'M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z',
  heart: 'M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z',
  back: 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
  lock: 'M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z',
  phone: 'M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z',
  download: 'M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z',
  share: 'M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z',
  close: 'M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
  contrast: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18V4c4.41 0 8 3.59 8 8s-3.59 8-8 8z',
};
const svg = (n) => `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="${ICON[n]}"/></svg>`;

const state = { catalog: null, bundled: [], all: [], byId: new Map(), cats: [], navCount: 0, viewName: null, viewArg: null,
  query: '', color: null, styleId: null, only4k: false, tStyle: 0, ctx: [], feedCount: 36, dirty: false, detail: null, gen: { style: 'aura', pal: 0, seed: 1, grain: 0.35 } };

let toastTimer;
function toast(msg) { const t = $('#toast'); t.textContent = msg; t.classList.add('show'); clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 2600); }
const catTitle = (id) => state.cats.find((c) => c.id === id)?.title || id;

// ---------------------------------------------------------------- building blocks
const heartBtn = (w) => { const on = favourites.has(w.id);
  return `<button class="heart" data-act="fav" data-id="${esc(w.id)}" aria-pressed="${on}" aria-label="${on ? 'Remove from favorites' : 'Add to favorites'}: ${esc(w.title)}">${svg('heart')}</button>`; };
const img = (w, src = w.thumb) => `<img src="${esc(src)}" alt="${esc(w.title)}" loading="lazy" decoding="async">`;
const card = (w) => `<div class="card" style="--r:${(1 / core.cardRatio(w.id)).toFixed(3)}"><a href="#/w/${enc(w.id)}" aria-label="${esc(w.title)}">${img(w)}</a><span class="shade"></span>${w.uhd ? '<span class="badge">4K</span>' : ''}${heartBtn(w)}</div>`;
const poster = (w, width = 140) => `<div class="poster" style="--w:${width}px"><a href="#/w/${enc(w.id)}" aria-label="${esc(w.title)}">${img(w)}</a><span class="shade"></span><span class="cap">${esc(w.title)}</span>${w.uhd ? '<span class="badge" style="top:8px;bottom:auto">4K</span>' : ''}</div>`;
const ranked = (w, i) => `<div class="ranked">${poster(w, 168)}<span class="rank">${i + 1}</span></div>`;
const hero = (w) => `<div class="hero"><a href="#/w/${enc(w.id)}" aria-label="${esc(w.title)}">${img(w)}</a><span class="shade"></span><div class="meta"><div class="eyebrow">${esc(catTitle(w.cat))}</div><div class="name">${esc(w.title)}</div></div></div>`;
const masonry = (list, id = '') => `<div class="masonry" ${id ? `id="${id}"` : ''}>${list.map(card).join('')}</div>`;
const section = (title, sub, right = '') => `<div class="sec-row"><div><h2 class="section">${esc(title)}</h2>${sub ? `<p class="sub">${esc(sub)}</p>` : ''}</div>${right}</div>`;
const empty = (msg) => `<div class="empty">${esc(msg)}</div>`;

// ---------------------------------------------------------------- views
const VIEWS = {
  home() {
    const day = core.epochDay(), a = state.all;
    const feat = core.featured(a, day, 6), trend = core.trending(a, day, 12), drop = core.dailyDrop(a, day, 8), feed = core.feed(a, day);
    const recent = viewed.all().map((id) => state.byId.get(id)).filter(Boolean).slice(0, 12);
    const picks = core.pick(a, [...favourites.all()], viewed.all(), downloads.all().map((d) => d.id), day, 10);
    state.ctx = feed;
    const date = new Date().toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
    return `
      <div class="topbar"><div class="grow"><h1 class="brand">Wallora</h1><p class="tag">Trending wallpapers, fresh every day</p></div>
        <a class="icon-btn" href="#/discover" aria-label="Search">${svg('search')}</a></div>
      <div class="hero-wrap"><div class="hero-track">${feat.map(hero).join('')}</div><div class="dots">${feat.map(() => '<i></i>').join('')}</div></div>
      ${section('Trending now', '', '<a class="link" href="#/trending">See all</a>')}<div class="row">${trend.map((w) => poster(w, 138)).join('')}</div>
      ${section('Browse by vibe')}<div class="row">${state.cats.map((c) => `<a class="chip" href="#/c/${c.id}">${c.emoji} ${esc(c.title)}</a>`).join('')}</div>
      ${recent.length ? `${section('Recently viewed')}<div class="row">${recent.map((w) => poster(w, 110)).join('')}</div>` : ''}
      ${section('Picked for you', 'Based on what you favourite, view and download. Stays in this browser.')}<div class="row">${picks.map((w) => poster(w, 150)).join('')}</div>
      ${section("Today's Drop", `A new collection for ${date}`)}<div class="row">${drop.map((w) => poster(w, 176)).join('')}</div>
      ${section('For you', 'A different mix every day')}
      <div id="feed">${masonry(feed.slice(0, state.feedCount))}</div>
      ${feed.length > state.feedCount ? '<div class="more"><button class="btn" data-act="more">Show more</button></div>' : ''}`;
  },

  discover() {
    return `
      <h1 class="page-title">Discover</h1>
      <div class="search">${svg('search')}<input id="q" type="search" inputmode="search" enterkeyhint="search" placeholder="Search wallpapers, styles, vibes" value="${esc(state.query)}" aria-label="Search wallpapers" autocomplete="off">
        <button class="icon-btn clear" data-act="clearq" aria-label="Clear search" ${state.query ? '' : 'hidden'}>${svg('close')}</button></div>
      <div class="swatches" role="group" aria-label="Filter by colour">${core.COLORS.map(([n, hex]) => `<button class="swatch" data-act="color" data-v="${n}" aria-pressed="${state.color === n}" aria-label="${n}" style="background:${hex}"></button>`).join('')}</div>
      <div class="row" role="group" aria-label="Filter by style">
        ${state.all.some((w) => w.uhd) ? `<button class="chip" data-act="only4k" aria-pressed="${state.only4k}">4K Ultra HD</button>` : ''}
        ${core.viralStyles.map((s) => `<button class="chip" data-act="vstyle" data-v="${s.id}" aria-pressed="${state.styleId === s.id}">${s.emoji} ${esc(s.title)}</button>`).join('')}</div>
      <div id="results">${discoverResults()}</div>`;
  },

  c(id) {
    const list = state.all.filter((w) => w.cat === id);
    state.ctx = list;
    return `<div class="topbar"><a class="icon-btn" href="#/discover" aria-label="Back">${svg('back')}</a><h1 class="page-title" style="margin:0">${esc(catTitle(id))}</h1></div>
      <p class="sub" style="margin-top:6px">${list.length} wallpapers</p>${list.length ? masonry(list) : empty('Nothing here yet.')}`;
  },

  trending() {
    const day = core.epochDay(), top = core.trending(state.all, day, 10);
    const dl = new Map(); downloads.all().forEach((d) => dl.set(d.id, (dl.get(d.id) || 0) + 1));
    const mine = [...dl.entries()].sort((a, b) => b[1] - a[1]).map(([id]) => state.byId.get(id)).filter(Boolean).slice(0, 10);
    return `<h1 class="page-title">Trending</h1><p class="sub">Handpicked viral looks, refreshed every ${core.TREND_WINDOW_DAYS} days</p>
      ${section('Top 10 right now')}<div class="row">${top.map(ranked).join('')}</div>
      ${mine.length ? `${section('Your most downloaded')}<div class="row">${mine.map(ranked).join('')}</div>` : ''}
      ${section('Viral styles')}<div class="row">${core.viralStyles.map((s, i) => `<button class="chip" data-act="tstyle" data-v="${i}" aria-pressed="${i === state.tStyle}">${s.emoji} ${esc(s.title)}</button>`).join('')}</div>
      <div id="styleGrid">${styleGrid()}</div>`;
  },

  favorites() {
    const favs = favourites.all(), list = state.all.filter((w) => favs.has(w.id));
    state.ctx = list;
    return `<div class="topbar"><h1 class="page-title grow" style="margin-bottom:0">Favorites</h1><a class="btn" href="#/downloads" style="min-height:42px">Downloads</a></div>
      <p class="sub" style="margin-top:6px">Saved in this browser only.</p>
      ${list.length ? masonry(list) : empty('No favorites yet. Tap the heart on any wallpaper and it will appear here.')}`;
  },

  downloads() {
    const rows = downloads.all();
    return `<div class="topbar"><a class="icon-btn" href="#/favorites" aria-label="Back">${svg('back')}</a><h1 class="page-title grow" style="margin:0">Downloads</h1>
      ${rows.length ? '<button class="link" data-act="dlclear">Clear</button>' : ''}</div>
      ${rows.length ? `<div class="list" style="margin-top:14px">${rows.map((r) => { const w = state.byId.get(r.id);
        return `<a class="li" href="${w ? `#/w/${enc(r.id)}` : '#/downloads'}">${w ? img(w) : '<span style="width:54px"></span>'}<div><b>${esc(w ? w.title : r.id)}</b><span>${esc(r.label)}${r.w ? ` · ${r.w} × ${r.h}` : ''}</span><span>${new Date(r.at).toLocaleString()}</span></div></a>`; }).join('')}</div>`
        : empty('Nothing saved yet. Tap Download on any wallpaper and it will appear here.')}`;
  },

  create() {
    const g = state.gen;
    return `<h1 class="page-title">Create</h1><p class="sub">Design your own wallpaper in seconds. Made in your browser, free.</p>
      <div class="stage"><canvas id="gen" aria-label="Wallpaper preview"></canvas></div>
      ${section('Style').replace('class="section"', 'class="section" style="font-size:16px;margin-top:10px"')}
      <div class="row">${STYLES.map((s) => `<button class="chip" data-act="gstyle" data-v="${s}" aria-pressed="${g.style === s}">${s[0].toUpperCase() + s.slice(1)}</button>`).join('')}</div>
      ${section('Colors').replace('class="section"', 'class="section" style="font-size:16px;margin-top:10px"')}
      <div class="row">${PALETTES.map((p, i) => `<button class="palette" data-act="gpal" data-v="${i}" aria-pressed="${g.pal === i}" aria-label="${p.name}" style="background:conic-gradient(${p.colors.join(',')},${p.colors[0]})"></button>`).join('')}</div>
      <div class="field"><label for="grain">Grain</label><input id="grain" type="range" min="0" max="100" value="${Math.round(g.grain * 100)}"></div>
      <div class="actions"><button class="btn" data-act="shuffle">Shuffle</button><button class="btn primary" data-act="gsave">${svg('download')} Download</button></div>
      <div class="actions"><button class="btn" data-act="gshare">${svg('share')} Share</button></div>`;
  },
};
const AFTER = { home: startHero, create: drawCreate };

function filtered() {
  const q = state.query.trim().toLowerCase(), style = core.viralStyles.find((s) => s.id === state.styleId);
  return state.all.filter((w) => (!state.only4k || w.uhd) && (!state.color || (w.colors || []).includes(state.color)) && (!style || style.matches(w)) &&
    (!q || w.title.toLowerCase().includes(q) || catTitle(w.cat).toLowerCase().includes(q) || (w.tags || []).some((t) => t.toLowerCase().includes(q))));
}
function discoverResults() {
  if (!(state.query.trim() || state.color || state.styleId || state.only4k)) {
    const day = core.epochDay();
    return `<p class="sub" style="margin-top:6px">Browse categories</p><div class="tiles">${state.cats.map((c) => {
      const list = state.all.filter((w) => w.cat === c.id), cover = core.trending(list, day, 1)[0];
      return `<div class="tile"><a href="#/c/${c.id}" aria-label="${esc(c.title)}">${cover ? img(cover) : ''}</a><span class="shade"></span><span class="emoji">${c.emoji}</span><div class="info"><b>${esc(c.title)}</b><span>${list.length} wallpapers</span></div></div>`; }).join('')}</div>`;
  }
  const list = filtered(); state.ctx = list;
  return list.length ? masonry(list) : empty('No wallpapers match. Try another word or colour.');
}
function styleGrid() {
  const s = core.viralStyles[state.tStyle], day = core.epochDay();
  const list = state.all.filter(s.matches).sort((a, b) => core.trendScore(b, day) - core.trendScore(a, day)); state.ctx = list;
  return `${section(`${s.emoji} ${s.title}`, `${list.length} wallpapers`)}${masonry(list)}`;
}

// ---------------------------------------------------------------- home carousel
let heroTimer;
function startHero() {
  const track = $('.hero-track'); if (!track) return;
  const dots = $$('.dots i'), step = () => track.firstElementChild.offsetWidth + 14;
  const setDot = () => { const i = Math.round(track.scrollLeft / step()); dots.forEach((d, k) => d.classList.toggle('on', k === i)); };
  track.addEventListener('scroll', setDot, { passive: true }); setDot();
  heroTimer = setInterval(() => { if (document.hidden || state.detail) return;
    const next = (Math.round(track.scrollLeft / step()) + 1) % track.children.length; track.scrollTo({ left: next * step(), behavior: 'smooth' }); }, 4500);
}

// ---------------------------------------------------------------- create
let drawQueued = false;
function drawCreate() {
  const c = $('#gen'); if (!c || drawQueued) return; drawQueued = true;
  requestAnimationFrame(() => { drawQueued = false; const g = state.gen; if ($('#gen')) renderGen($('#gen'), g.style, PALETTES[g.pal], g.seed, g.grain, 540, 960); });
}
async function saveCreation(width, height) {
  const g = state.gen, c = document.createElement('canvas'); renderGen(c, g.style, PALETTES[g.pal], g.seed, g.grain, width, height);
  const blob = await new Promise((r) => c.toBlob(r, 'image/jpeg', 0.92));
  return deliver(blob, `wallora_create_${g.seed}_${width}.jpg`, 'My Wallora wallpaper');
}

// ---------------------------------------------------------------- download / share
const loadImage = (u) => new Promise((res, rej) => { const i = new Image(); i.onload = () => res(i); i.onerror = rej; i.src = u; });
// What the preview shows: the bundled copy when there is one (fast, same origin), else the cloud image.
const viewUrl = (w, d = state.detail) => (d && d.light ? (w.localLight || w.fullLight) : null) || w.local || w.full;
const localUrl = (w, d = state.detail) => (d && d.light ? w.localLight : null) || w.local || null;
// The best copy that exists: the cloud original when there is one.
const remoteUrl = (w, d = state.detail) => (w.remote ? ((d && d.light && w.fullLight) || w.full) : null);

async function deliver(blob, name, title) {
  const file = new File([blob], name, { type: 'image/jpeg' });
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    try { await navigator.share({ files: [file], title }); return 'shared'; }
    catch (e) { if (e.name === 'AbortError') return 'cancelled'; /* otherwise fall back below */ }
  }
  const url = URL.createObjectURL(blob);
  if (IOS) { openSaveSheet(url); return 'sheet'; }
  const a = Object.assign(document.createElement('a'), { href: url, download: name }); document.body.append(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 15000); return 'saved';
}
function openSaveSheet(url) {
  openSheet(`<h3>Save to your phone</h3><img class="save-img" src="${esc(url)}" alt="Wallpaper to save">
    <p class="hint">${IOS ? 'Press and hold the picture, then choose <b>Add to Photos</b>.' : 'Press and hold (or right-click) the picture to save it.'}</p>
    <button class="btn block" data-act="closesheet" style="margin-top:8px">Done</button>`);
}
function openDownloadSheet(w) {
  const local = localUrl(w), remote = remoteUrl(w), opts = [];
  if (remote) opts.push(['remote', null, 'Highest quality', 'Highest']);
  if (local) opts.push(['local', null, 'Original · 1080 × 1920', 'Original'], ['local', 720, 'Medium · 720p (smaller file)', '720p'], ['local', 480, 'Small · 480p (smallest file)', '480p']);
  openSheet(`<h3>Download quality</h3>${opts.map(([src, wd, label, tag]) => `<button class="opt" data-act="dl" data-src="${src}" data-w="${wd ?? ''}" data-tag="${tag}">${svg('download')}<span>${label}</span></button>`).join('')}
    ${IOS ? '<p class="hint">On iPhone: after saving, open Photos, tap the picture, then Share → <b>Use as Wallpaper</b>.</p>' : ''}`);
}
/** Fetches a cloud image as a blob. Works only if the storage bucket allows cross-origin reads (see firebase/storage-cors.json). */
async function remoteBlob(url) { try { const r = await fetch(url, { mode: 'cors' }); return r.ok ? await r.blob() : null; } catch (_) { return null; } }
async function runDownload(w, src, width, tag) {
  closeSheet();
  try {
    let blob = null, sw = 0, sh = 0;
    if (src === 'remote') {
      const url = remoteUrl(w), raw = await remoteBlob(url);
      if (!raw) { openSaveSheet(url); downloads.add({ id: w.id, at: Date.now(), label: tag, w: 0, h: 0 }); return; }   // no CORS: let the user press and hold
      const bmp = await createImageBitmap(raw); sw = bmp.width; sh = bmp.height; bmp.close();
      blob = raw.type === 'image/jpeg' ? raw : await toJpeg(await createImageBitmap(raw), sw, sh);
    } else {
      const im = await loadImage(localUrl(w));
      sw = width && im.naturalWidth > width ? width : im.naturalWidth; sh = Math.round(im.naturalHeight * sw / im.naturalWidth);
      blob = await toJpeg(im, sw, sh);
    }
    const res = await deliver(blob, `${w.id}${width ? `_${width}w` : ''}.jpg`, w.title);
    if (res !== 'cancelled') { downloads.add({ id: w.id, at: Date.now(), label: tag, w: sw, h: sh }); if (res === 'saved') toast('Saved to your downloads'); }
  } catch (_) { toast("Couldn't save that picture. Try again."); }
}
async function toJpeg(source, w, h) {
  const c = Object.assign(document.createElement('canvas'), { width: w, height: h }); c.getContext('2d').drawImage(source, 0, 0, w, h);
  return new Promise((r) => c.toBlob(r, 'image/jpeg', 0.92));
}
async function shareLink(w) {
  const url = `${location.origin}${location.pathname}#/w/${enc(w.id)}`;
  if (navigator.share) { try { await navigator.share({ title: w.title, text: `${w.title} on Wallora`, url }); } catch (_) { /* cancelled */ } }
  else { try { await navigator.clipboard.writeText(url); toast('Link copied'); } catch (_) { toast(url); } }
}

// ---------------------------------------------------------------- sheet
function openSheet(html) { const s = $('#sheet'); s.innerHTML = `<div class="panel" role="dialog" aria-modal="true">${html}</div>`; s.hidden = false; }
function closeSheet() { const s = $('#sheet'); s.hidden = true; s.innerHTML = ''; }

// ---------------------------------------------------------------- detail
const MOCK_TINTS = ['#4DD0E1', '#FF8A65', '#BA68C8', '#81C784', '#FFD54F', '#64B5F6'];
function openDetail(id) {
  const w = state.byId.get(id);
  if (!w) { toast("That wallpaper isn't available."); history.replaceState(null, '', '#/home'); route(); return; }
  if (!state.detail || state.detail.id !== id) state.detail = { id, light: false, lock: false, home: false, zoom: false };
  viewed.add(id); drawDetail();
}
function closeDetail() { if (!state.detail) return; state.detail = null; const d = $('#detail'); d.hidden = true; d.innerHTML = '';
  if (state.dirty) { state.dirty = false; show(state.viewName, state.viewArg, true); } }
function drawDetail() {
  const d = state.detail, w = d && state.byId.get(d.id); if (!w) return;
  const fav = favourites.has(w.id), now = new Date(), credit = w.credit ? `Photo: ${w.credit}${w.license ? ` · ${w.license}` : ''}` : '';
  $('#detail').innerHTML = `
    <img class="bg ${d.zoom ? 'zoom' : ''}" src="${esc(viewUrl(w))}" alt="${esc(w.title)}" decoding="async">
    ${d.home ? `<div class="home-sim" aria-hidden="true"><div class="pill"></div><div class="grid">${Array.from({ length: 20 }, (_, i) => `<span class="app" style="background:${MOCK_TINTS[i % 6]}"></span>`).join('')}</div></div>` : ''}
    ${d.lock ? `<div class="lock-sim" aria-hidden="true"><div class="clock">${now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })}</div><div class="date">${now.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })}</div>${svg('lock')}<small>Swipe up to unlock</small></div>` : ''}
    <div class="scrim"></div>
    <div class="top"><button class="icon-btn" data-act="dback" aria-label="Back">${svg('back')}</button>
      <button class="icon-btn" data-act="dlock" aria-pressed="${d.lock}" aria-label="Lock screen preview">${svg('lock')}</button>
      <button class="icon-btn" data-act="dhome" aria-pressed="${d.home}" aria-label="Home screen preview">${svg('phone')}</button>
      <button class="icon-btn" data-act="dzoom" aria-pressed="${d.zoom}" aria-label="Zoom" style="font-weight:800;font-size:13px">${d.zoom ? '1x' : '2x'}</button>
      ${w.both ? `<button class="icon-btn" data-act="dtheme" aria-pressed="${d.light}" aria-label="${d.light ? 'Show dark version' : 'Show light version'}">${svg('contrast')}</button>` : ''}
      <span style="flex:1"></span>
      <button class="icon-btn ${fav ? 'heart-on' : ''}" data-act="fav" data-id="${esc(w.id)}" aria-pressed="${fav}" aria-label="${fav ? 'Remove from favorites' : 'Add to favorites'}">${svg('heart')}</button></div>
    <div class="bottom"><h1>${esc(w.title)}</h1>${credit ? `<p class="credit">${esc(credit)}</p>` : ''}<a class="cat" href="#/c/${esc(w.cat)}">${esc(catTitle(w.cat))}</a>
      <div class="buttons"><button class="btn primary" data-act="dlsheet">${svg('download')} Download</button><button class="btn" data-act="dshare">${svg('share')} Share</button></div>
      <a class="report" href="mailto:${SUPPORT_EMAIL}?subject=${enc('Report: ' + w.title)}&body=${enc(`I'd like to report this wallpaper.\n\nTitle: ${w.title}\nId: ${w.id}\nReason: `)}">Report this wallpaper</a></div>`;
  $('#detail').hidden = false;
}
function stepDetail(dir) {
  const list = state.ctx.some((w) => w.id === state.detail?.id) ? state.ctx : state.all, i = list.findIndex((w) => w.id === state.detail.id), next = list[i + dir];
  if (next) { state.detail = { id: next.id, light: false, lock: state.detail.lock, home: state.detail.home, zoom: false }; history.replaceState(null, '', `#/w/${enc(next.id)}`); viewed.add(next.id); drawDetail(); }
}

// ---------------------------------------------------------------- routing & actions
const TAB_OF = { c: 'discover', downloads: 'favorites' };
const TITLES = { home: 'Wallora — trending wallpapers, fresh every day', discover: 'Discover — Wallora', create: 'Create — Wallora', trending: 'Trending — Wallora', favorites: 'Favorites — Wallora', downloads: 'Downloads — Wallora' };
function renderTabs(active) {
  $('#tabs').innerHTML = [['home', 'Home', 'home'], ['discover', 'Discover', 'search'], ['create', 'Create', 'sparkle'], ['trending', 'Trending', 'trend'], ['favorites', 'Favorites', 'heart']]
    .map(([r, l, i]) => `<a href="#/${r}" ${r === active ? 'aria-current="page"' : ''}>${svg(i)}<span>${l}</span></a>`).join('');
}
function show(name, arg, keepScroll = false) {
  clearInterval(heroTimer); if (!VIEWS[name]) name = 'home';
  state.viewName = name; state.viewArg = arg;
  const y = window.scrollY; $('#view').innerHTML = VIEWS[name](arg); renderTabs(TAB_OF[name] || name);
  AFTER[name]?.(); document.title = TITLES[name] || (name === 'c' ? `${catTitle(arg)} — Wallora` : 'Wallora');
  window.scrollTo(0, keepScroll ? y : 0);
}
function route() {
  const [name, arg] = location.hash.replace(/^#\/?/, '').split('/'), a = arg ? decodeURIComponent(arg) : null;
  state.navCount++;
  if (name === 'w') { if (!state.viewName) show('home'); openDetail(a); return; }
  closeSheet(); closeDetail(); show(name || 'home', a);
}
function setAll(list) { state.all = list; state.byId = new Map(list.map((w) => [w.id, w])); }
function refreshView() { if (state.detail) state.dirty = true; else if (state.viewName && state.viewName !== 'create') show(state.viewName, state.viewArg, true); }

const ACTIONS = {
  fav(t) {
    const id = t.dataset.id, w = state.byId.get(id), on = favourites.toggle(id);
    $$(`[data-act="fav"][data-id="${CSS.escape(id)}"]`).forEach((b) => { b.setAttribute('aria-pressed', on); b.classList.toggle('heart-on', on && !!b.closest('#detail'));
      b.setAttribute('aria-label', `${on ? 'Remove from favorites' : 'Add to favorites'}${b.closest('#detail') ? '' : `: ${w?.title ?? ''}`}`); });
    if (state.viewName === 'favorites' && !state.detail) show('favorites', null, true);
  },
  more() { state.feedCount += 36; const feed = core.feed(state.all, core.epochDay());
    $('#feed').innerHTML = masonry(feed.slice(0, state.feedCount)); if (feed.length <= state.feedCount) $('.more')?.remove(); },
  clearq() { state.query = ''; const q = $('#q'); q.value = ''; q.focus(); syncSearch(); },
  color(t) { state.color = state.color === t.dataset.v ? null : t.dataset.v; $$('.swatch').forEach((s) => s.setAttribute('aria-pressed', s.dataset.v === state.color)); syncSearch(); },
  vstyle(t) { state.styleId = state.styleId === t.dataset.v ? null : t.dataset.v; $$('[data-act="vstyle"]').forEach((s) => s.setAttribute('aria-pressed', s.dataset.v === state.styleId)); syncSearch(); },
  only4k(t) { state.only4k = !state.only4k; t.setAttribute('aria-pressed', state.only4k); syncSearch(); },
  tstyle(t) { state.tStyle = Number(t.dataset.v); $$('[data-act="tstyle"]').forEach((s) => s.setAttribute('aria-pressed', Number(s.dataset.v) === state.tStyle)); $('#styleGrid').innerHTML = styleGrid(); },
  dlclear() { downloads.clear(); show('downloads', null, true); },
  gstyle(t) { state.gen.style = t.dataset.v; $$('[data-act="gstyle"]').forEach((s) => s.setAttribute('aria-pressed', s.dataset.v === state.gen.style)); drawCreate(); },
  gpal(t) { state.gen.pal = Number(t.dataset.v); $$('[data-act="gpal"]').forEach((s) => s.setAttribute('aria-pressed', Number(s.dataset.v) === state.gen.pal)); drawCreate(); },
  shuffle() { state.gen.seed += 1; drawCreate(); },
  gsave() { openSheet(`<h3>Download quality</h3><button class="opt" data-act="gdl" data-w="1080">${svg('download')}<span>Full HD · 1080 × 1920</span></button>
    <button class="opt" data-act="gdl" data-w="2160">${svg('download')}<span>4K Ultra HD · 2160 × 3840</span></button>
    ${IOS ? '<p class="hint">On iPhone: after saving, open Photos, tap the picture, then Share → <b>Use as Wallpaper</b>.</p>' : ''}`); },
  async gdl(t) { closeSheet(); const wd = Number(t.dataset.w); try { const r = await saveCreation(wd, wd * 16 / 9); if (r === 'saved') toast('Saved to your downloads'); } catch (_) { toast("Couldn't create that picture. Try again."); } },
  async gshare() { try { await saveCreation(1080, 1920); } catch (_) { toast("Couldn't share that picture."); } },
  closesheet: closeSheet,
  dback() { if (state.navCount > 1) history.back(); else location.hash = '#/home'; },
  dlock() { state.detail.lock = !state.detail.lock; if (state.detail.lock) state.detail.home = false; drawDetail(); },
  dhome() { state.detail.home = !state.detail.home; if (state.detail.home) state.detail.lock = false; drawDetail(); },
  dzoom() { state.detail.zoom = !state.detail.zoom; drawDetail(); },
  dtheme() { state.detail.light = !state.detail.light; drawDetail(); },
  dlsheet() { openDownloadSheet(state.byId.get(state.detail.id)); },
  dl(t) { runDownload(state.byId.get(state.detail.id), t.dataset.src, t.dataset.w ? Number(t.dataset.w) : null, t.dataset.tag); },
  dshare() { shareLink(state.byId.get(state.detail.id)); },
};
function syncSearch() { $('#results').innerHTML = discoverResults(); $('.clear')?.toggleAttribute('hidden', !state.query); }

document.addEventListener('click', (e) => { if (e.target.id === 'sheet') return closeSheet(); const t = e.target.closest('[data-act]'); if (t) { e.preventDefault(); ACTIONS[t.dataset.act]?.(t); } });
document.addEventListener('input', (e) => {
  if (e.target.id === 'q') { state.query = e.target.value; syncSearch(); }
  if (e.target.id === 'grain') { state.gen.grain = Number(e.target.value) / 100; drawCreate(); }
});
document.addEventListener('load', (e) => { if (e.target.tagName === 'IMG') e.target.classList.add('in'); }, true);
document.addEventListener('error', (e) => { if (e.target.tagName === 'IMG') e.target.classList.add('in'); }, true);
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') { if (!$('#sheet').hidden) closeSheet(); else if (state.detail) ACTIONS.dback(); }
  if (state.detail && $('#sheet').hidden && e.key === 'ArrowRight') stepDetail(1);
  if (state.detail && $('#sheet').hidden && e.key === 'ArrowLeft') stepDetail(-1);
});
// swipe between wallpapers in the detail view
let sx = 0, sy = 0;
$('#detail').addEventListener('pointerdown', (e) => { sx = e.clientX; sy = e.clientY; });
$('#detail').addEventListener('pointerup', (e) => { const dx = e.clientX - sx, dy = e.clientY - sy;
  if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy) * 1.4 && !e.target.closest('button, a')) stepDetail(dx < 0 ? 1 : -1); });
window.addEventListener('hashchange', route);

// ---------------------------------------------------------------- install hint
function showBanner(text, actionLabel, action) {
  if (prefs.get('bannerDismissed', false)) return;
  const b = $('#banner'); b.innerHTML = `<p>${text}</p>${actionLabel ? `<button class="btn primary" style="min-height:38px;padding:0 16px" id="bAct">${actionLabel}</button>` : ''}<button class="icon-btn" id="bX" aria-label="Dismiss">${svg('close')}</button>`;
  b.hidden = false; $('#bX').onclick = () => { b.hidden = true; prefs.set('bannerDismissed', true); }; if (actionLabel) $('#bAct').onclick = () => { b.hidden = true; action(); };
}
window.addEventListener('beforeinstallprompt', (e) => { e.preventDefault(); setTimeout(() => showBanner('Install Wallora for one-tap access.', 'Install', () => e.prompt()), 5000); });
if (IOS && !STANDALONE) setTimeout(() => showBanner('Add Wallora to your Home Screen: tap <b>Share</b>, then <b>Add to Home Screen</b>.', null), 6000);

// ---------------------------------------------------------------- cloud catalogue
async function loadCloud() {
  let json = null;
  try {
    const ctrl = new AbortController(), timer = setTimeout(() => ctrl.abort(), 8000);
    let docs = [], url = FIRESTORE;
    for (let i = 0; i < 5; i++) {
      const r = await fetch(url, { signal: ctrl.signal }); if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const j = await r.json(); docs = docs.concat(j.documents || []); if (!j.nextPageToken) break; url = `${FIRESTORE}&pageToken=${enc(j.nextPageToken)}`;
    }
    clearTimeout(timer); json = { documents: docs }; cloudCache.set(json);
  } catch (_) { json = cloudCache.get()?.json || null; }   // offline: fall back to the last copy we saw
  return json ? core.parseCloud(json, state.catalog.taxonomy, state.cats.map((c) => c.id), state.catalog.retired) : [];
}

// ---------------------------------------------------------------- boot
(async function boot() {
  try {
    state.catalog = await (await fetch('catalog.json', { cache: 'no-cache' })).json();
  } catch (_) { $('#view').innerHTML = empty("Couldn't load Wallora. Check your connection and reload."); return; }
  state.cats = state.catalog.categories;
  state.bundled = state.catalog.wallpapers.map((w) => { const full = `img/f/${w.id}.webp`, light = w.both ? `img/f/${w.id}~l.webp` : null;
    return { ...w, thumb: `img/t/${w.id}.webp`, full, fullLight: light, local: full, localLight: light }; });
  setAll(state.bundled); route();
  loadCloud().then((cloud) => { if (cloud.length) { setAll(core.mergeCatalog(cloud, state.bundled)); refreshView(); } });
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
})();
