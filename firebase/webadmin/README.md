# Wallora web admin

A single-page dashboard (no build step) to manage the wallpaper catalogue in Firebase:

- sign in with the admin account
- upload wallpapers to Cloud Storage + Firestore
- add categories
- view and delete cloud wallpapers

## Run it

1. Paste your web app config into `firebase-config.js`
   (Firebase Console → Project settings → General → Your apps → Web app)
2. Serve this folder over HTTP:

   ```bash
   cd firebase/webadmin
   python3 -m http.server 8080
   ```

3. Open <http://localhost:8080>

See [`../SETUP.md`](../SETUP.md) for the full setup walkthrough.
