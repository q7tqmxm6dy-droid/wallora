# Wallora — Phone Wallpaper App

A native Android wallpaper app built with **Kotlin + Jetpack Compose**. It ships with
**66 original, code-generated wallpapers** (no third-party images) — plus any real photos
you add — and lets you browse, favorite, preview, set, save and share them.

## Features

- **Grid gallery** — browse all wallpapers in an adaptive thumbnail grid.
- **Categories** — Aurora, Gradients, Waves, Geometric, Minimal, Neon, Cars, Moto, **Posters**.
- **Full-screen preview** — tap any wallpaper to open it edge-to-edge.
- **Set as wallpaper** — choose Home screen, Lock screen, or both.
- **Favorites** — heart any wallpaper; stored persistently with DataStore.
- **Save** — writes the image to `Pictures/Wallora` on the device.
- **Share** — sends the image to any app via the system share sheet.
- **Dark mode** — a **Light / Dark / Auto** selector in the top bar. *Auto* follows the
  phone's setting; your choice is remembered.
- **Material You** — uses dynamic color on Android 12+.
- **Light & dark variants** — every wallpaper can have a dark-mode image. The app shows the
  matching variant in the grid and preview, and **Set / Save / Share use the same one**, so
  what you see is what gets applied.
- **Photo credits** — CC-licensed photos you add can carry a `credit` + `license` field,
  shown under the title on the detail screen.
- **Cloud wallpapers (optional)** — Firebase Firestore catalogue + Cloud Storage images, so you can publish wallpapers without shipping an app update. Falls back to the bundled set when offline.
- **Admin panel** — manage the catalogue from the app (shield icon → sign in) or from a browser dashboard.
- **Analytics & Crashlytics** — Firebase Analytics events (set / save / share / favourite) and crash reporting.

## Screenshots

Captured from the app running on an emulator (`screenshots/`):

| Home (light) | Home (dark) | Categories |
|---|---|---|
| ![Home light](screenshots/home-light.png) | ![Home dark](screenshots/home-dark.png) | ![Categories](screenshots/categories.png) |

| Favorites | Detail | Set wallpaper |
|---|---|---|
| ![Favorites](screenshots/favorites.png) | ![Detail](screenshots/detail.png) | ![Set wallpaper](screenshots/set-wallpaper.png) |

Proof the wallpaper is applied to the device (launcher now shows the chosen wallpaper):

![Applied](screenshots/wallpaper-applied.png)

## Tech stack

| Piece | Version |
|---|---|
| Android Gradle Plugin | 9.4.1 (built-in Kotlin) |
| Gradle | 9.6.0 |
| Kotlin | 2.2.10 |
| Jetpack Compose | BOM 2026.09.00 (Material 3) |
| Navigation Compose | 2.10.2 |
| DataStore Preferences | 1.2.1 |
| Coil | 2.7.0 |
| Firebase | BOM 34.19.0 (Firestore, Storage, Auth, Analytics, Crashlytics) |
| compileSdk / targetSdk | 37 (Android 17) |
| minSdk | 29 (Android 10) |

## Firebase (cloud wallpapers + admin)

**Project:** `wallora-bc950` · **Admin dashboard (live):** <https://wallora-bc950.web.app>

Already configured:

- Android app registered (`com.wallora.app`) → `app/google-services.json` fetched
- Web app registered → `firebase/webadmin/firebase-config.js` filled in
- Hosting deployed (the dashboard URL above)
- CI workflows in `.github/workflows/`

**Status**

- ✅ Firestore database + rules deployed (public read, authenticated write) — verified
- ✅ Cloud Storage (Blaze) + storage rules — **real file upload verified end-to-end**
- ✅ Email/Password sign-in + admin user (`ahmad.qiblawi112@gmail.com`)
- ✅ Hosting dashboard live; Android + Web configs fetched
- ✅ Verified: upload a JPEG → Cloud Storage → Firestore → it renders in the app

Two ways to add wallpapers:

- **Upload a file** — the web dashboard or the in-app admin uploads it to Cloud Storage.
  Both let you attach an optional **dark-mode variant** as a second image.
- **Add by image URL** — paste a link to an image hosted anywhere (optionally a second
  link for the dark variant).

### ⚠️ Security: disable sign-ups

Sign-up is enabled by default, so anyone holding the (public) API key could create an
account — and the rules allow any signed-in user to add or delete wallpapers.

Turn it off: **Authentication → Settings → User actions → uncheck "Enable create (sign-up)"**.

Accounts you add in the console keep working, so your admin login is unaffected.

Full walkthrough with screenshots-worth of detail: [`firebase/SETUP.md`](firebase/SETUP.md)

| Piece | Where |
|---|---|
| Firestore rules | `firebase/firestore.rules` |
| Storage rules | `firebase/storage.rules` |
| Web admin dashboard | `firebase/webadmin/` |
| GitHub Actions (CI/CD) | `.github/workflows/` |

Data model:

| Path | Fields |
|---|---|
| `wallpapers/{id}` | `title`, `category`, `url`, `urlDark` (optional), `path`, `pathDark` (optional), `createdAt` |
| `categories/{id}` | `title` |
| Storage `wallpapers/{uuid}.jpg` | the image file (plus `{uuid}_dark.jpg` when a dark variant is uploaded) |

In the app, tap the **shield icon** in the top bar to open the admin screen and sign in
with the account you created.

## Open it in Android Studio
1. Open Android Studio.
2. **File → Open…** and select this folder (`Wallora`).
3. Let Gradle sync, then press **Run ▶**.

## Build from the command line

Gradle needs a JDK 17+ (Android Studio bundles one). From this folder:

```bash
# Use Android Studio's bundled JDK (adjust the path if yours differs)
export JAVA_HOME="/home/ahmadq/Downloads/android-studio-quail4-patch1-linux/android-studio/jbr"

./gradlew :app:assembleDebug          # build APK
./gradlew :app:installDebug           # build + install on a connected device
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Release builds & CI

```bash
./gradlew :app:assembleRelease     # -> app/build/outputs/apk/release/app-release.apk
```

**R8 shrinking cuts the APK from ~61 MB (debug) to ~12 MB (release)** — minification,
resource shrinking and removal of unused Compose/Firebase code.

Signing: release builds are signed with your local **debug keystore** so they install
straight away. **Replace `signingConfigs.release` with your own upload key before
publishing.**

GitHub Actions (`.github/workflows/android-ci.yml`) runs unit tests, builds both APKs on
every push/PR, uploads them as artifacts, and attaches the release APK to a GitHub
Release when you push a `v*` tag. It needs one secret:

```bash
base64 -w0 app/google-services.json | gh secret set GOOGLE_SERVICES_JSON
```

## Project structure

```
app/src/main/
├─ java/com/wallora/app/
│  ├─ MainActivity.kt              # entry point, wires theme + ViewModel
│  ├─ data/
│  │  ├─ Wallpaper.kt              # model: Wallpaper (bundled or remote), Category
│  │  ├─ WallpaperCatalog.kt       # the bundled catalogue (edit here to add images)
│  │  ├─ RemoteWallpaperRepository.kt # live Firestore reads
│  │  ├─ AdminRepository.kt        # auth, upload, delete, categories
│  │  └─ PreferencesRepository.kt  # DataStore: favorites + dark mode
│  ├─ ui/
│  │  ├─ WalloraApp.kt             # Scaffold, bottom nav, NavHost
│  │  ├─ WallpaperViewModel.kt
│  │  ├─ components/               # WallpaperCard, WallpaperGrid, CategoryChips…
│  │  ├─ screens/                  # Home, Categories, Category, Favorites, Detail, Admin
│  │  └─ theme/                    # Color, Type, Theme
│  └─ util/
│     ├─ WallpaperSetter.kt        # WallpaperManager
│     ├─ WallpaperImages.kt        # load bitmap from resource (URL coming) or URL
│     ├─ ImageSaver.kt             # MediaStore (Pictures/Wallora)
│     ├─ ShareUtils.kt             # FileProvider + ACTION_SEND
│     └─ Analytics.kt              # Firebase Analytics + Crashlytics
└─ res/
   ├─ drawable-nodpi/               # 66 wallpapers × light/dark (132 images, 1080×1920)
   ├─ drawable/ic_*.xml            # app + UI icons (vector)
   └─ mipmap-anydpi-v26/           # adaptive launcher icon
```

## Adding your own wallpapers

1. Drop your images into `app/src/main/res/drawable-nodpi/` using lowercase names,
   e.g. `my_wall_01.jpg` (letters, digits, underscores only).
2. Add a line to `WallpaperCatalog.wallpapers`:

   ```kotlin
   Wallpaper("my_wall_01", "My Wallpaper", "gradient", R.drawable.my_wall_01),
   ```

3. If you want a new category, add a `Category("mycat", "My Category")` entry too.

Portrait images around **1080 × 1920** look best.

## Renaming the app

- Display name: `app/src/main/res/values/strings.xml` → `app_name`.
- Package / applicationId: change `namespace` and `applicationId` in
  `app/build.gradle.kts` (and the `package` lines in the Kotlin files).

## Notes

- **Verified** on Android emulators (x86_64): builds, installs, launches, and the
  gallery, categories, category filtering, favorites (persisted via DataStore),
  save-to-gallery, share sheet, dark-mode toggle, and **setting the wallpaper**
  (confirmed by the launcher background changing) all work. A saved image was verified
  as a valid 1080×1920 JPEG. The images in `screenshots/` come from the running app.
- **Wallpaper setting** uses `WallpaperManager`; lock-screen setting may be restricted
  by some device manufacturers.
- The **debug** APK is ~36 MB because it keeps debugging metadata and Compose tooling.
  Release builds with R8 will be much smaller.
- The bundled artwork is generated by a script (`gen_wallpapers.py` was used during
  development); it is original and free to use.
