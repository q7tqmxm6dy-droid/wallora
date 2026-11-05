// Wallora web app service worker: opens instantly and keeps working offline.
//  - the app shell (HTML, JS, CSS) is served from the cache and refreshed in the background
//  - the catalogue is network-first, so new wallpapers show on the next load; the cache is only its offline fallback
//  - wallpaper images are cache-first, capped so the cache cannot grow without bound
// Bump VERSION to force every visitor onto a fresh set of caches.
const VERSION = 'v2';
const SHELL = `wallora-shell-${VERSION}`;
const IMAGES = `wallora-img-${VERSION}`;
const IMAGE_LIMIT = 400;
const SHELL_FILES = ['./', 'index.html', 'styles.css', 'core.js', 'generator.js', 'store.js', 'main.js', 'catalog.json', 'manifest.webmanifest',
  'icons/icon-192.png', 'icons/icon-512.png', 'icons/apple-touch-icon.png'];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(SHELL).then((cache) => cache.addAll(SHELL_FILES)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => ![SHELL, IMAGES].includes(k)).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});

async function trim(cache) {
  const keys = await cache.keys();
  if (keys.length > IMAGE_LIMIT) await Promise.all(keys.slice(0, keys.length - IMAGE_LIMIT + 40).map((k) => cache.delete(k)));
}

self.addEventListener('fetch', (event) => {
  const req = event.request, url = new URL(req.url);
  if (req.method !== 'GET' || url.origin !== self.location.origin) return;       // cloud images and Firestore go straight to the network

  if (url.pathname.includes('/img/')) {
    event.respondWith(caches.open(IMAGES).then(async (cache) => {
      const hit = await cache.match(req);
      if (hit) return hit;
      const res = await fetch(req);
      if (res.ok) { cache.put(req, res.clone()); trim(cache); }
      return res;
    }));
    return;
  }

  if (url.pathname.endsWith('/catalog.json')) {
    event.respondWith(caches.open(SHELL).then(async (cache) => {
      try {
        const res = await fetch(req, { cache: 'no-cache' });
        if (res.ok) cache.put(req, res.clone());
        return res;
      } catch (e) {
        return (await cache.match(req, { ignoreSearch: true })) || Response.error();
      }
    }));
    return;
  }

  event.respondWith(caches.open(SHELL).then(async (cache) => {
    const hit = await cache.match(req, { ignoreSearch: true });
    const network = fetch(req).then((res) => { if (res.ok) cache.put(req, res.clone()); return res; }).catch(() => null);
    if (hit) { event.waitUntil(network); return hit; }
    return (await network) || (req.mode === 'navigate' ? cache.match('index.html') : Response.error());
  }));
});
