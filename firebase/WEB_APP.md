# Wallora web app: hosting notes

**URL once deployed:** `https://wallora-bc950.web.app/app/`

## Deploy
```bash
cd /home/ahmadq/Downloads/Wallora
python3 scripts/export_web_catalog.py
node firebase/webadmin/app/tests.mjs
firebase deploy --only hosting --project wallora-bc950
```
`firebase.json` already sets the cache headers (the service worker, catalogue and scripts are never cached
stale; images are cached for a week). To force every visitor onto fresh caches, bump `VERSION` in
`firebase/webadmin/app/sw.js`.

## One-time step: let the web app resize and share your 4K originals
Cloud wallpapers live in Firebase Storage, which sends no CORS header by default. Without one, the browser cannot read
those files, so the web app's **Highest quality** download falls back to "press and hold the picture to save it".
Everything else (preview, 1080/720/480 downloads, Create) already works because it uses the bundled copies.

To switch on the full flow (save/share sheet with the original JPEG), allow cross-origin reads on the bucket. The images
are public wallpapers, so reading them from any origin is fine. As the project owner, with the Google Cloud SDK signed in:

```bash
gcloud storage buckets update gs://wallora-bc950.firebasestorage.app --cors-file=firebase/storage-cors.json
# or:  gsutil cors set firebase/storage-cors.json gs://wallora-bc950.firebasestorage.app
```
No app change is needed: the web app tries the cross-origin read first and only falls back if it is refused.

## Known limits
- Cloud-only wallpapers (uploaded from the admin page with no bundled twin) show their full-size image in the grid,
  because there is no thumbnail service. Large 4K uploads therefore load slowly on mobile data. The Firebase
  "Resize Images" extension would fix that.
- The browser cannot set a wallpaper. On iPhone the user saves to Photos first.
- Favourites, history and downloads are per browser; clearing site data or switching browsers starts fresh.

## Light and dark wallpapers

A wallpaper with a light and a dark version (`both: true` in `catalog.json`; `urlDark` on a cloud wallpaper) is drawn as one picture
split down the middle, light on the left and dark on the right, the same way the Android app draws it. In the full-screen view the
half-moon button cycles Split, Light and Dark. Saving a pair uses the light or dark version that matches the device's day/night
setting (or the one chosen with the half-moon button or in the download sheet). The web app itself also has a day look that follows
the device (`prefers-color-scheme`). A web page cannot set the phone's wallpaper, so the automatic day/night wallpaper switch is
Android-only. Bump `VERSION` in `sw.js` after changing app files so installed copies refresh.
