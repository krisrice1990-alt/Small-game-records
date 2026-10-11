# Small Game Record NL — iOS build

This folder packages the existing Small Game Records NL app in a Capacitor iOS shell. It is kept separate from HuntMate and does not modify the Android app.

## Build a test app

The GitHub Actions workflow builds an **unsigned iOS Simulator app** and uploads it as an artifact. It is for build verification, not installable on a physical iPhone and not ready for the App Store.

## Make an App Store release

1. Open this project on a Mac with Xcode installed.
2. In this folder run `npm install`, then `npx cap add ios` (only the first time), then `npx cap sync ios`.
3. Open `ios/App/App.xcworkspace` in Xcode.
4. Set the Apple Developer Team, signing, and a unique bundle identifier; confirm the bundle ID in `capacitor.config.json` matches.
5. Test on a physical iPhone, especially saving/reopening records and location permission behavior.
6. Archive and upload through Xcode to App Store Connect, then complete Apple's privacy, age-rating, pricing, and review forms.

A paid App Store listing requires Apple Developer Program membership (normally US$99/year). Apple approval is not guaranteed.

## Important limitations to verify before sale

- The current app's records are stored locally on the device; they do not automatically sync between devices. Back up/export records regularly.
- GPS/background behavior and iOS permission prompts must be tested on a real iPhone. This wrapper does not by itself add background location tracking.
- This app is a personal record book, not an official licence or source of current hunting regulations.
