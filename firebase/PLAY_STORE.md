# Wallora — Google Play publishing kit

Everything the Play Console asks for, ready to copy/paste.

## Store listing copy

**App name (max 30 chars)**

```
Wallora
```

**Short description (max 80 chars)**

```
Beautiful wallpapers for your phone — light & dark, live wallpaper, no clutter.
```

**Full description (max 4000 chars)**

```
Wallora keeps your phone looking fresh with a hand-made collection of wallpapers that
match your mood, your theme and your home screen.

LIGHT & DARK BUILT IN
Every wallpaper is designed for both modes, so your home screen always fits the rest of
your phone. Pick Light, Dark, or let Wallora follow your system setting.

LIVE WALLPAPER
Turn your home screen into a slow, calm slideshow: wallpapers crossfade with a gentle
zoom, or switch to the animated gradient mode when you want something lighter on battery.
Choose the interval (5 / 10 / 30 / 60 seconds) and which categories it cycles.

FIND YOUR FAVOURITE FAST
• Browse by category — Aurora, Gradients, Waves, Geometric, Minimal, Neon, Cars, Posters
• Search by name, or filter by tag
• Sort by name or category
• Shuffle button when you can't decide

MAKE IT YOURS
• Favourite anything and find it again instantly
• Build your own collections
• Apply looks before you set: Blur, B&W, Warm, Cool and Vignette, each with an
  adjustable strength slider
• Save to your gallery or share with a friend
• See exactly how a wallpaper will look behind your lock screen before you set it

DAILY ROUTINE
Wallpaper of the day, a daily streak and badges to collect as you go. Turn on daily
rotation and Wallora changes your wallpaper for you each day.

SET IT YOUR WAY
• Home screen, lock screen, or both
• Swipe between wallpapers without leaving the page
• Quick Settings tile, home-screen widget and share-target support

PRIVATE BY DESIGN
No account and no sign-in. Your favourites, collections and settings stay on your device.
Wallora is free and supported by ads served through Google AdMob; you are asked for consent
before any personalised ad is requested, and you can change that choice in Settings at any
time.

Note: wallpapers are original artwork. Photographs used in the "Cars" category are
freely licensed (CC0 / CC BY) and credited inside the app.
```

**App category:** Personalization
**Tags:** Wallpaper, Personalization, Themes

## Content rating

Answer the questionnaire as: **no** to violence, sexuality, language, controlled
substances, gambling, and user-generated content. Wallora has no social features, no
chat and no user content sharing between users — expect **Everyone / PEGI 3**.

## Data safety answers

| Question | Answer |
|---|---|
| Does your app collect or share user data? | **Yes** |
| Data types collected | **App activity** (in-app actions, e.g. “wallpaper set”), **App info & performance** (crash logs, diagnostics), **Device or other IDs** (advertising ID, random app-instance ID) |
| Is this data shared with third parties? | **Yes** — the advertising identifier is shared with Google AdMob for advertising. Analytics and crash data are processed by Google Firebase on our behalf |
| Is data encrypted in transit? | **Yes** |
| Can users request deletion? | **Yes** — uninstalling removes all local data; there is no account to delete |
| Is data used for advertising or tracking? | **Yes** — advertising only, via AdMob. Not used for cross-app tracking beyond that, and opt-in where consent is required |
| Photos / videos collected? | **No** (a shared image is only used locally to set your wallpaper) |
| Location, contacts, messages, health, financial info? | **No** |

## Graphics

| Asset | Requirement | File |
|---|---|---|
| App icon | 512×512 PNG, 32-bit, no alpha | `store/icon-512.png` |
| Feature graphic | 1024×500 PNG/JPG | `store/feature-graphic.png` |
| Phone screenshots | 2–8, 16:9 or 9:16, min 1080px on the short side | `store/screenshots/*.png` |

## Privacy policy URL

Already hosted with your dashboard:

```
https://wallora-bc950.web.app/privacy.html
```

Source: `firebase/webadmin/privacy.html` (edit and redeploy with `firebase deploy --only hosting`).

## Release checklist

1. **Create a real upload key** — don't ship with the debug keystore:
   ```bash
   keytool -genkeypair -v -keystore wallora-upload.jks -alias wallora \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
   Then point `signingConfigs.release` in `app/build.gradle.kts` at it (keep the passwords
   out of git — use `keystore.properties` and add it to `.gitignore`).
2. **Bump `versionCode` / `versionName`** in `app/build.gradle.kts` for every upload.
3. **Build the bundle** (Play prefers AAB over APK):
   ```bash
   ./gradlew :app:bundleRelease
   # -> app/build/outputs/bundle/release/app-release.aab
   ```
4. **Tag a release** (`git tag v1.0.0 && git push --tags`) — the CI workflow will build and
   attach the APK to a GitHub Release.
5. **Play Console → Create app**, fill the listing above, upload the AAB to Internal testing
   first, then promote to Production.
6. **Testers**: add a few emails under Internal testing to try it before going public.

## Store listing "What's new" (first release)

```
First release: 63 wallpapers across 8 categories, light & dark variants, live wallpaper
(slideshow or animated gradient), search, tags, collections, favourites, photo effects,
daily rotation and a lock-screen preview.
```
