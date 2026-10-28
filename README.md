# Small Game Records NL

A standalone Android/web app for keeping a personal small-game hunting record in Newfoundland and Labrador. This project is separate from HuntMate.

## Features
- Record species, date, quantity, hunting area/zone, and notes
- View harvest totals and species counts
- Search, edit, and delete entries
- Export records as CSV; back up / restore all records to a file from Settings
- Five tabs — Totals, Stats, Add, Live Hunt, Records — plus a Settings screen (gear icon)
- Works offline; records stay locally on the device
- The Android app can check for updates itself (Settings → Check for updates, or
  automatically) and installs updates in place — saved records are kept

## Updates
Release APKs and the update feed (`update.json`) are published in the separate
public releases repository, `Small-game-records-releases`, so the app can fetch
them with no account or sign-in. This code repository stays private.

## Run the web version
Open `index.html` in a modern browser.

## Build and download the Android test APK
1. Open the [Actions page](https://github.com/krisrice1990-alt/Small-game-records/actions).
2. Select **Build Small Game Records NL APK**.
3. Open a successful run and download the **Small-Game-Records-NL-debug-APK** artifact.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installs from that source.

You can also trigger a build manually from the Actions page using **Run workflow**.

## Important notes
This is a personal record book, not an official licence, harvest report, or source of hunting regulations. Verify current Newfoundland and Labrador hunting rules through official government sources. Records are stored on the device and do not automatically sync. Export CSV backups regularly. Uninstalling the app or clearing app data may erase local records.

The APK produced by GitHub Actions is a debug/test build, not a signed Play Store release.
