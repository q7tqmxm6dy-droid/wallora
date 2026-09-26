# Wallora — Firebase Setup (click-by-click)

> **Status for this repo:** the project already exists (`wallora-bc950`), the Android app
> is registered, `app/google-services.json` is filled in, the web config is in place and
> the dashboard is deployed at <https://wallora-bc950.web.app>.
> **You can skip Steps 1, 2, 3 (partly), 6 and 7** — jump to Steps 4, 5 and 3's user
> creation, then deploy the rules.

The Android app and the web dashboard both talk to the same Firebase project. Until you
finish this guide the app still works — it just shows the 24 bundled wallpapers. Once
configured, wallpapers you upload appear in the app instantly.

> You need a Google account. Everything below is on the **free Spark plan**, which is
> plenty (5 GB Storage, 1 GiB/day Firestore reads).

---

## Step 1 — Create the Firebase project

1. Go to <https://console.firebase.google.com/>
2. Click **Create a project**
3. Name it e.g. `wallora`
4. Google Analytics: **Enable** (this powers the Analytics/Crashlytics you asked for)
5. Choose/create an Analytics account → **Create project** → wait → **Continue**

---

## Step 2 — Register the Android app

1. On the project home, click the **Android** icon (or **Add app → Android**)
2. **Android package name**: `com.wallora.app` ← must be exactly this
3. Nickname: `Wallora Android` (optional)
4. **Register app**
5. **Download `google-services.json`**
6. Replace the placeholder file in this repo with the real one:

   ```
   Wallora/app/google-services.json
   ```

   (The placeholder has `wallora-placeholder` as its project id — overwrite it.)

7. Skip the "Add the Firebase SDK" steps (already done) and **Continue to console**

---

## Step 3 — Turn on email/password sign-in and create your admin user

1. Left menu → **Build → Authentication → Get started**
2. **Sign-in method** tab → **Email/Password** → toggle **Enable** → **Save**
3. **Users** tab → **Add user**
4. Enter an email (e.g. `admin@wallora.app`) and a password → **Add user**

   That email + password is what you'll type into the app's **Admin** screen and the
   web dashboard.

> Note: `admin@wallora.app` is not a real mailbox — that's fine, Firebase doesn't email it.

---

## Step 4 — Create Firestore and paste the rules

1. Left menu → **Build → Firestore Database → Create database**
2. Location: pick the closest to you
3. Start in **Production mode** → **Create**
4. Open the **Rules** tab, delete everything, and paste the contents of
   [`firestore.rules`](firestore.rules) → **Publish**

---

## Step 5 — Turn on Storage and paste its rules

1. Left menu → **Build → Storage → Get started**
2. Start in **Production mode** → **Next** → pick the location → **Done**
3. Open the **Rules** tab, paste the contents of [`storage.rules`](storage.rules) → **Publish**

---

## Step 6 — (Optional) Seed a category

The app's own Admin screen and the web dashboard can both create categories, so you can
skip this. If you want one ready up front:

1. **Firestore Database → Data → Start collection**
2. Collection ID: `categories` → **Next**
3. Document ID: `nature` → Field: `title` (string) = `Nature` → **Save**

---

## Step 7 — Run the web dashboard

The dashboard is plain HTML/JS in [`webadmin/`](webadmin/); there's no build step.

1. **Project settings (⚙) → General → Your apps → Add app → Web**
2. Nickname `Wallora Admin` → **Register app**
3. Copy the `firebaseConfig = { … }` object
4. Paste it into `firebase/webadmin/firebase-config.js`
5. Serve the folder over HTTP (must be HTTP, not `file://`):

   ```bash
   cd /home/ahmadq/Downloads/Wallora/firebase/webadmin
   python3 -m http.server 8080
   ```

6. Open <http://localhost:8080> and sign in with the admin user from Step 3.

> **If you serve it from another host/IP**, add that host under
> **Authentication → Settings → Authorized domains**, or sign-in will fail.
> `localhost` is already authorized.

**Deploying it properly (optional):**

```bash
npm install -g firebase-tools
firebase login
cd /home/ahmadq/Downloads/Wallora
firebase deploy --only hosting,firestore:rules,storage
```

---

## Step 8 — Build the app with your config

```bash
cd /home/ahmadq/Downloads/Wallora
JAVA_HOME="/home/ahmadq/Downloads/android-studio-quail4-patch1-linux/android-studio/jbr" \
  ./gradlew :app:assembleDebug
```

Then install `app/build/outputs/apk/debug/app-debug.apk` on your phone, or run it from
Android Studio.

---

## Verifying it works

1. In the **web dashboard**, upload a wallpaper (title + category + image)
2. It appears in the "Cloud wallpapers" table with a thumbnail
3. Open the app → the new wallpaper shows up in the grid (usually within a second)
4. In the app, tap the **shield icon** (top-right) → sign in → you'll see the same
   wallpapers and can upload/delete from the phone too

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Firebase is not configured` in the app | `app/google-services.json` is still the placeholder — redo Step 2 |
| App shows only the 24 bundled wallpapers | Normal until you finish Step 2 **and** add wallpapers. Check logcat for `PERMISSION_DENIED` |
| Dashboard: "Missing or insufficient permissions" | Rules weren't published (Step 4/5), or you're not signed in |
| Dashboard sign-in fails with `auth/unauthorized-domain` | Add your host to **Authorized domains** (Step 7) |
| Dashboard shows placeholder warning | `firebase-config.js` wasn't edited (Step 7) |
| Upload works but app doesn't update | Firestore listener; check the device has internet and `INTERNET` permission is present (it is) |

---

## What lives where

| Collection / path | Purpose |
|---|---|
| `wallpapers/{id}` | `title`, `category`, `url`, `path`, `createdAt` |
| `categories/{id}` | `title` |
| Storage `wallpapers/{uuid}.jpg` | the actual image files |

The app reads these live, so anything you change in the dashboard or console shows up
on every device without an app update.
