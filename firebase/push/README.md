# Instant push for new wallpapers (optional)

**You probably don't need this.** The app already announces new wallpapers on its own:

- on every launch (a few seconds after the catalogue loads), and
- every 6 hours in the background via WorkManager.

That works on the free Spark plan with no server. This function only makes the notification
arrive *within seconds* of publishing, instead of on the next check.

## Enabling it

Firestore triggers require the **Blaze** (pay-as-you-go) plan. Once that is active:

```bash
cd firebase/push
npm install
firebase deploy --only functions --project wallora-bc950
```

Then add this to `firebase.json` so the function is part of the project:

```json
"functions": { "source": "firebase/push" }
```

## How the pieces fit

```
wallpapers/{id} created  ──►  announceNewWallpaper (this function)
                                    │  FCM to topic "new_wallpapers"
                                    ▼
                          WalloraMessagingService  ──►  notification
```

The app subscribes to the topic at startup (`NewWallpaperAlerts.TOPIC`), so no app changes are
needed — `WalloraMessagingService.onMessageReceived` already turns a push into a notification.

Because the app also records which ids it has already announced, a push arriving for a wallpaper
the local check already reported will not produce a second notification.
