# Kids Photo Viewer

**A safe, toddler-proof photo and video viewer for Android.**

Hand your phone to a small child without worrying. It shows the photos and
videos from your camera roll full-screen as a slideshow, and locks itself
down so little fingers can't swipe out of it, open other apps, or edit or
delete your pictures.

## Features

- Shows only photos/videos from the standard camera folder (no screenshots
  or downloads), most recent first.
- Two modes, switched by **holding the top-left corner for 2 seconds**.
  There is no button or menu, so a toddler can't reach it; the new mode's
  name appears briefly once the switch has happened.
  - **Swipe only** (default) — swipe left/right to move; nothing advances
    on its own and videos loop, so a photo stays up until someone swipes.
  - **Slideshow** — photos show for 3 seconds and videos play through once,
    then it moves on by itself. It cannot be interrupted: swiping is off
    and holding a finger down changes nothing, so it keeps going whatever
    a child does to the screen.
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
device but is not suitable for Play Store distribution.

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
