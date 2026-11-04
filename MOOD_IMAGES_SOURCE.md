# Mood images: source, rights and risk

These 19 wallpapers (`mood_01` to `mood_19`) were added at the app owner's request from the local folder
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
