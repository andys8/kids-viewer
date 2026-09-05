# Kids Photo Viewer

**A safe, toddler-proof photo and video viewer for Android.**

Hand your phone to a small child without worrying. It shows the photos and
videos from your camera roll full-screen as a slideshow, and locks itself
down so little fingers can't swipe out of it, open other apps, or edit or
delete your pictures.

## Features

- Shows only photos/videos from the standard camera folder (no screenshots
  or downloads), most recent first.
- Runs as a continuous slideshow: photos show for 3 seconds, videos play
  through once and then move on. Swipe left/right to navigate manually.
- Plays high-frame-rate 4K footage smoothly, with display frame-rate
  matching and HDR output.
- Keeps the screen on, so the phone never locks while the app is open.
- Full-screen immersive mode — system bars are hidden.
- Self-pinning kiosk mode (Android Screen Pinning) so the app locks itself
  down automatically when possible.
- No accounts, no cloud, no imports — reads your existing photos directly.

## Installing on your phone

Every push to `master` automatically builds a debug APK via GitHub Actions
and publishes it as a **new** timestamped release on the
**[Releases](../../releases)** page. Older builds are kept, so you can always
go back to a previous one.

1. On your phone, open the [Releases page](../../releases) in a browser and
   pick the newest release at the top.
2. Tap the `.apk` file to download it.
3. Open the downloaded file to install. Android may ask you to allow
   "install unknown apps" for your browser the first time — allow it.
4. You may see a Google Play Protect warning since this is a self-built app
   not distributed through the Play Store — this is expected; you can
   proceed/install anyway.
5. Open the app and grant photo/video access when prompted.

This is a **debug-signed build**, which is fine for installing on your own
device. Signed release builds are produced by the separate
[Play Store release](.github/workflows/release.yml) workflow.

## Publishing to the Play Store

`.github/workflows/release.yml` builds a signed app bundle and APK and uploads
the bundle to Google Play. It is a manual trigger: **Actions → Play Store
release → Run workflow**, pick a track.

The one-time setup — signing key, Play Console app, store listing, policy
declarations, service account — is written up in
[docs/PLAY_STORE.md](docs/PLAY_STORE.md). The short version: sharing the app
through Play's **internal testing** track gets you real installs and automatic
updates for up to 100 people without the 12-tester/14-day hurdle that
production access can require.

## Screen pinning / kiosk lock-down

The app tries to automatically enable Android's built-in Screen Pinning
when it opens, which prevents the Back/Home/Recents buttons from leaving
the app. If your device doesn't allow this automatically, the app will show
instructions to turn it on manually:

**Settings → Security → Advanced → Screen pinning → On**, then open the
app-switcher (square button) and tap the pin icon on Kids Photo Viewer.

Note: without enrolling the device as a managed ("kiosk") device — which
requires a computer/ADB and is out of scope here — Android does not allow
any regular app to make itself 100% unremovable. Screen Pinning is the
strongest lock-down available to a normal app and is what this app uses.

## Building locally

```
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
