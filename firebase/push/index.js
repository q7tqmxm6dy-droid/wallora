/**
 * OPTIONAL — instant push for new wallpapers.
 *
 * The app already notifies on its own (it checks the catalogue at launch and every 6 hours via
 * WorkManager), so this function is not required. Deploy it only if you want the notification to
 * arrive within seconds of publishing.
 *
 * Requirements: the project must be on the Blaze plan — Firestore triggers are not available on
 * Spark. See README.md in this folder for the three commands needed.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// Must match NewWallpaperAlerts.TOPIC in the Android app.
const TOPIC = "new_wallpapers";

exports.announceNewWallpaper = onDocumentCreated("wallpapers/{wallpaperId}", async (event) => {
  const data = event.data?.data() || {};
  if (data.published === false) return; // honour the dashboard's publish flag

  const title = data.title || "New wallpaper";
  await getMessaging().send({
    topic: TOPIC,
    notification: {
      title: "New wallpapers",
      body: `${title} is ready to set.`,
    },
    data: { wallpaperId: event.params.wallpaperId, title },
    android: { priority: "normal", notification: { channelId: "wallora_new_wallpapers" } },
  });
});
