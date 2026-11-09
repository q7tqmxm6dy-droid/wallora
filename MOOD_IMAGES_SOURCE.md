# Mood images: source, rights and risk

These wallpapers (`mood_01` to `mood_37`) were added at the app owner's request from the local folder
`Wallpaper App Prompt_files` (images saved from a web page). **Their origin and licence were not verified.**
Most look AI-generated. One image from the folder, a silver cross pendant (320 x 640), was left out on request.

## Higher risk: check these first
Google Play removes apps that publish other people's copyrighted characters, brands or likenesses, and the developer
account behind this app belongs to someone else, so a policy strike would land on that account.

| id | title | why it is higher risk |
|---|---|---|
| `mood_07` | Black Silhouette | photograph of a real person |
| `mood_10` | Red Shadow | anime character (fan art) |
| `mood_11` | Green Blade | anime character (fan art) |
| `mood_12` | Violet Hunter | anime character (fan art) |
| `mood_14` | Steel Chronograph | brand name printed on the watch dial |
| `mood_19` | Silver Gaze | anime character (fan art) |

**Removed from the Play release (9 November 2026):** `mood_07`, `mood_10`, `mood_11`, `mood_12`, `mood_14`, `mood_19` and
`mood_31` (Black Gloves, which shows a boxing brand's logo). They are listed in `Taxonomy.retiredIds` and now live only in the
private debug-only pack (`personal-pack/extra/`, git-ignored, built by `scripts/personal_pack.py`).

## To remove any of them
Delete its line in `WallpaperCatalog.kt`, delete `app/src/main/res/drawable-nodpi/<id>.jpg`, and add the id to
`Taxonomy.retiredIds`.

## All 19
Each source image was either cropped to fill 1080 x 1920 (`crop`) or fitted over a blurred, darkened copy of itself
(`fit`). Sources are 512 to 1024 px wide, so the smaller ones look soft on a large screen. Titles are neutral on purpose.

| id | title | category | source image | original size | handling |
|---|---|---|---|---|---|
| `mood_01` | Blue Pedestal | dark | n1 | 800x800 | fit |
| `mood_02` | Moonlit Shore | nature | n2 | 800x1131 | fit |
| `mood_03` | Black Rose | dark | n3 | 800x800 | fit |
| `mood_04` | Private Cabin | luxury | n4 | 736x736 | fit |
| `mood_05` | Gothic Keep | dark | n5 | 512x768 | fit |
| `mood_06` | Misty Peak | nature | n6 | 564x1002 | crop |
| `mood_07` | Black Silhouette | luxury | n7 | 1024x1241 | fit |
| `mood_08` | Violet Night Sky | pinterest | n8 | 1024x2226 | crop |
| `mood_09` | Dark Wings | dark | n9 | 1024x1089 | fit |
| `mood_10` | Red Shadow | gaming | n10 | 1024x1820 | crop |
| `mood_11` | Green Blade | gaming | n11 | 1024x1820 | crop |
| `mood_12` | Violet Hunter | gaming | n12 | 675x1200 | crop |
| `mood_13` | Retro Sunset PC | gaming | n13 | 707x1000 | fit |
| `mood_14` | Steel Chronograph | luxury | n14 | 1024x1215 | fit |
| `mood_15` | Neon Avenue | city | n15 | 1024x1024 | fit |
| `mood_16` | Purple Tide | nature | n16 | 768x1344 | crop |
| `mood_17` | Liquid Chrome | abstract3d | n17 | 1005x1508 | fit |
| `mood_18` | Skyline Penthouse | luxury | n18 | 1024x1024 | fit |
| `mood_19` | Silver Gaze | gaming | n20 | 1024x1821 | crop |

## Second batch: `mood_20` to `mood_37` (18 images)
Added later from the same folder. The folder now holds about 120 images; 36 were already in the app. Of the rest, these 18
were added. Origin and licence are unverified, as above.

| id | title | category | source file (start of name) | original size | handling |
|---|---|---|---|---|---|
| `mood_20` | Your Own Priority | pinterest | 1D-hEYCqkdNlQB… | 675x1200 | crop |
| `mood_21` | Built in Silence | luxury | 3yvt23FnS8ywdl… | 857x1200 | fit |
| `mood_22` | Stoic Power | dark | 6G-rSCnEAZeUcM… | 800x1000 | fit |
| `mood_23` | Dark Silk | abstract3d | 7MQUda5XJyqXI1… | 1529x860 | crop |
| `mood_24` | Forge | dark | DfSCQyOsIwIqdw… | 1024x1024 | fit |
| `mood_25` | Violet Nebula | space | L7G6XE8FXm7Pji… | 1320x860 | crop |
| `mood_26` | Sunset Run | nature | NtgCZlfFNQB3Bw… | 1024x1024 | fit |
| `mood_27` | Discipline | dark | OW03lygsicKamD… | 1024x1818 | crop |
| `mood_28` | Mirror Lake Galaxy | space | TYeJDe0CCaTVeh… | 1000x1777 | crop |
| `mood_29` | Neon Facets | abstract3d | U5WO0TAMAXt4xw… | 1024x1820 | crop |
| `mood_30` | Heavy Lift | dark | ZdajMM06iVL4vX… | 768x1344 | crop |
| `mood_31` | Black Gloves | dark | alCfVmOT7tNXfB… | 1024x1024 | fit |
| `mood_32` | Chrome Flow | abstract3d | ejcSOXcwenmSAG… | 1000x667 | crop |
| `mood_33` | Stop When Done | dark | euwFuB1CH0KYB_… | 850x1511 | crop |
| `mood_34` | Empty Gym | dark | orAvAa4NKxnpBr… | 1024x1024 | fit |
| `mood_35` | Teal Peaks | nature | r46lXxQfe2EQtN… | 616x952 | crop |
| `mood_36` | Pastel Haze | pinterest | sijKVamzivw9w-… | 1024x2217 | crop |
| `mood_37` | Day One | dark | umYBWF-bRhx3g6… | 736x1104 | crop |

The text on the quote wallpapers (`mood_20`, `mood_21`, `mood_22`, `mood_27`, `mood_33`, `mood_37`) is part of the pictures.
`mood_22` carries a quote attributed to Marcus Aurelius; the attribution is printed on the image and was not checked.

### Left out of the second batch, and why
- **About 60 images of Marvel characters, comic covers, film stills and real actors** (Iron Man, Spider-Man, Wolverine, Venom,
  Thor, Hulk, Thanos, Doctor Strange and others, plus a photo of a real bodybuilder). These are Disney/Marvel's copyrighted
  characters and people's likenesses, and publishing them on a Play Store account risks removal of the whole app.
- **Phone mock-ups and app screenshots** (not wallpapers).
- **Pictures skipped on earlier rounds:** the silver cross, a car with a tyre-brand banner and plate, a game-franchise livery,
  a photograph carrying a photographer's signature, a portrait of a real person, and images under 700 px.
- A completely black image.

## Which of these are on the public website
The web app publishes only these 22: `mood_01`, `02`, `03`, `06`, `08`, `09`, `16`, `17`, `20`, `21`, `22`, `23`, `25`, `26`,
`27`, `28`, `29`, `32`, `33`, `35`, `36`, `37` (scenery, space, abstract art, quote art and gradients). Everything else here, and
all `nightcar_*` images, are Android-only. The allow-list is `WEB_ALLOWED_UNVERIFIED` in `scripts/export_web_catalog.py`;
a test in `firebase/webadmin/app/tests.mjs` fails if anything outside it reaches the web build. To take a picture off the
website, delete its id from that list, run the export and deploy.
