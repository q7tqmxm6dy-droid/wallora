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
• 100+ wallpapers in 15 categories — Trending, Aurora, Gradients, Waves, Geometric, Minimal, Neon, Cars, Posters, Space and more
• Trending: fresh looks plus real photographs of the Milky Way, aurora, cherry blossom, Lake Louise, Earthrise and more
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

WALLORA PRO
Wallora is free to use. Upgrade once with Wallora Pro to unlock the photo effects and turn on
daily wallpaper rotation. One payment, no subscription.
(When ads are switched back on, use: "Wallora is free and supported by ads. Upgrade once with Wallora Pro to remove every ad, ...")

PRIVATE BY DESIGN
No account and no sign-in. Your favourites, collections and settings stay on your device.
(Only once ads are switched back on: Ads are served through Google AdMob; you are asked for consent before any
personalised ad is requested, and you can change that choice in Settings at any time.)

Note: most wallpapers are original artwork. The photographs in the Trending category are
freely licensed (CC0, public domain or CC BY) and credited on each wallpaper inside the app.
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
| Does the app have in-app purchases? | **Yes** — one optional one-time purchase (Wallora Pro). Payment is handled entirely by Google Play; the app stores no payment data |
| Is this data shared with third parties? | **Yes** — the advertising identifier is shared with Google AdMob for advertising. Analytics and crash data are processed by Google Firebase on our behalf |
| Is data encrypted in transit? | **Yes** |
| Can users request deletion? | **Yes** — uninstalling removes all local data; there is no account to delete |
| Is data used for advertising or tracking? | **Yes** — advertising only, via AdMob. Not used for cross-app tracking beyond that, and opt-in where consent is required |
| Photos / videos collected? | **No** (a shared image is only used locally to set your wallpaper) |
| Location, contacts, messages, health, financial info? | **No** |

> **Ads are off in the first release.** AdMob declined the account application, so `ADS_ENABLED` is false. The AdMob SDK is still bundled, so keep
> the Data safety answers about the advertising ID as they are; Play checks the SDKs inside the bundle.

## App content declarations (Play Console → Policy → App content)

| Declaration | Answer |
|---|---|
| Ads | **No ads in the first release** (`AdsConfig.ADS_ENABLED = false` while AdMob is not approved). Switch to **Yes, contains ads** when ads are turned back on |
| In-app purchases | **Yes** — Wallora Pro |
| Target audience | **13 and over** (not designed for children, so the Families policy does not apply) |
| News app / government / health / financial features | **No** |
| Advertising ID | **Yes** — used for advertising (the `AD_ID` permission is declared) |
| App access | All functionality is available without login; no special access instructions needed |

## Wallora Pro in-app product

The app sells one non-consumable product. Create it before testing a purchase:

1. Play Console → your app → **Monetize → Products → In-app products → Create product**
2. **Product ID: `wallora_pro`** (must match `ProManager.PRODUCT_ID`)
3. Name "Wallora Pro", description "Remove all ads and unlock photo effects and daily rotation", set a price, then **Activate**.
4. Add your Google account under **Setup → License testing** so test purchases are free.
5. Install the build from an Internal testing track (purchases do not work from a sideloaded APK).

Until the product is active the Pro screen shows a disabled "Unlock Pro" button.

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

1. **Upload key** — already created at `keys/wallora-upload.jks` and wired in through
   `keystore.properties` (both git-ignored). **Back both up somewhere safe** (password
   manager / encrypted drive): if the key is lost you must ask Play support to reset it.
   Enrol in **Play App Signing** when you upload the first bundle.
2. **Bump `versionCode`** in `app/build.gradle.kts` for every upload (it is `1` for the first
   release; Play rejects a repeated number) and `versionName` to match.
3. **Build the bundle** (Play prefers AAB over APK):
   ```bash
   ./gradlew :app:bundleRelease
   # -> app/build/outputs/bundle/release/app-release.aab
   ```
4. **Tag a release** (`git tag v1.0.0 && git push --tags`) — the CI workflow will build and
   attach the APK to a GitHub Release.
5. **Play Console → Create app**, fill the listing above and the declarations, upload the AAB
   to Internal testing first, then promote to Production.
6. **Testers**: add a few emails under Internal testing to try it before going public.
   New personal developer accounts must run a **closed test with at least 12 testers for
   14 days** before Production access is granted — start this early.
7. **Redeploy the privacy policy** after any edit: `firebase deploy --only hosting`.

## Store listing "What's new" (first release)

```
First release: 100+ wallpapers across 15 categories including Trending photos, light & dark
variants, live wallpaper (slideshow or animated gradient), search, tags, collections,
favourites, photo effects, daily rotation, a lock-screen preview and an optional one-time
Wallora Pro upgrade that removes ads.
```
