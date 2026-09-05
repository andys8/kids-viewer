# Kids Viewer

A dead-simple, toddler-proof photo & video viewer for Android. It shows the
photos and videos from your phone's camera roll full-screen, automatically
advances every 4 seconds, autoplays videos, and locks itself down so kids
can't accidentally swipe out, edit, or delete anything.

## Features

- Shows only photos/videos from the standard camera folder (no screenshots
  or downloads), most recent first.
- Auto-advances every 4 seconds; swipe left/right to navigate manually.
- Videos autoplay automatically.
- Full-screen immersive mode — system bars are hidden.
- Self-pinning kiosk mode (Android Screen Pinning) so the app locks itself
  down automatically when possible.
- No accounts, no cloud, no imports — reads your existing photos directly.

## Installing on your phone

Every push to this repository automatically builds a debug APK via GitHub
Actions and publishes it to the **[Releases](../../releases/tag/latest-debug-build)**
page under the tag `latest-debug-build`. That release is always kept up to
date with the latest build.

1. On your phone, open the [latest release](../../releases/tag/latest-debug-build)
   in a browser.
2. Tap the `.apk` file to download it.
3. Open the downloaded file to install. Android may ask you to allow
   "install unknown apps" for your browser the first time — allow it.
4. You may see a Google Play Protect warning since this is a self-built app
   not distributed through the Play Store — this is expected; you can
   proceed/install anyway.
5. Open the app and grant photo/video access when prompted.

This is a **debug-signed build**, which is fine for installing on your own
device but is not suitable for Play Store distribution.

## Screen pinning / kiosk lock-down

The app tries to automatically enable Android's built-in Screen Pinning
when it opens, which prevents the Back/Home/Recents buttons from leaving
the app. If your device doesn't allow this automatically, the app will show
instructions to turn it on manually:

**Settings → Security → Advanced → Screen pinning → On**, then open the
app-switcher (square button) and tap the pin icon on Kids Viewer.

Note: without enrolling the device as a managed ("kiosk") device — which
requires a computer/ADB and is out of scope here — Android does not allow
any regular app to make itself 100% unremovable. Screen Pinning is the
strongest lock-down available to a normal app and is what this app uses.

## Building locally

```
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
