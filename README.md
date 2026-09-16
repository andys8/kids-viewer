# Kids Photo Viewer

**A safe, toddler-proof photo and video viewer for Android.**

Hand your phone to a small child without worrying. It shows the photos and
videos from your camera roll full-screen, and locks itself down so little
fingers can't swipe out of it, open other apps, or edit or delete your
pictures.

## Features

- Shows only photos/videos from the standard camera folder (no screenshots
  or downloads), most recent first.
- 122 bundled pictures — animals, things that go, food, toys — for when the
  camera roll has run its course. Held in the app, so they need no photo
  permission and no network. See [Pictures](#pictures).
- Two modes — swipe-only and slideshow — switched by a hidden hold in the
  top-left corner. See [The two modes](#the-two-modes).
- Plays high-frame-rate 4K footage smoothly, with display frame-rate
  matching and HDR output.
- Keeps the screen on, so the phone never locks while the app is open.
- Full-screen immersive mode — system bars are hidden.
- Self-pinning kiosk mode (Android Screen Pinning) so the app locks itself
  down automatically when possible.
- No accounts, no cloud, no imports — reads your existing photos directly.
- No network access at all. The app ships without the internet permission,
  so nothing can leave your phone even in principle.

## Installing on your phone

Requires **Android 8.0 or newer**.

Every merge to `master` builds a signed release APK and publishes it on the
**[Releases](../../releases)** page. Older releases stay available, so you can
always go back to a previous build.

1. On your phone, open the [Releases page](../../releases) and pick the
   newest release at the top.
2. Tap the `kids-photo-viewer-<version>.apk` file to download it.
3. Open the downloaded file to install. The first time, Android asks you to
   allow "install unknown apps" for your browser — allow it, then go back
   and open the file again.
4. Android may warn that the app was not checked by Play Protect, because it
   did not come from the Play Store. Choose to install anyway.
5. Open the app and grant access to your photos and videos when asked.

Installing a newer release over an older one keeps everything in place — no
need to uninstall first.

### Automatic updates

There is no Play Store to push updates, so the tidy way to stay current is
[**Obtainium**](https://github.com/ImranR98/Obtainium), which watches a
GitHub repository's releases and installs new ones for you:

1. Install Obtainium.
2. Add an app, and paste this repository's URL:
   `https://github.com/andys8/kids-viewer`
3. Obtainium picks up every new release from then on.

### Verifying a download

Each release includes a `.sha256` file next to the APK, and the release notes
list the same hash. To check a download on a computer:

```
sha256sum -c kids-photo-viewer-<version>.apk.sha256
```

### A note on Google's developer verification

Google is introducing a requirement that apps installed on certified Android
devices come from a registered developer. It starts in Brazil, Indonesia,
Singapore and Thailand on 30 September 2026 and expands to the rest of the
world during 2027. Once it reaches your country, installing an app from an
unregistered developer needs a more involved install flow with a waiting
period. There is a free account tier for hobbyists distributing to a small
number of devices, which is what this app would use. Nothing changes for
existing installs before then.

## The two modes

The app starts in **swipe-only**. To switch, **press and hold the top-left
corner of the screen for two seconds**. There is no button and no menu — the
target is invisible and deliberately awkward, so a toddler won't find it by
accident. When the switch happens, the name of the mode now running appears
briefly in the middle of the screen.

**Swipe only** (the default)
Nothing moves on its own. Swipe left or right to go between photos and
videos, and videos repeat until you swipe away. Good for looking at pictures
together, or for a child old enough to swipe.

**Slideshow**
It runs itself: photos show for three seconds, videos play through once, and
then it moves on. Swiping is turned off and nothing a finger does interrupts
it — no pausing, no skipping. Good for handing the phone to a child who would
otherwise just mash the screen.

## Pictures

The app also carries 122 pictures of its own: animals, things that go, food,
fruit, toys, weather. One clear subject per picture on a soft colour, no words
and nothing to read.

**Press and hold the top-right corner for two seconds** to switch between your
photos and the pictures, and hold again to switch back. It works the same way
as the mode switch on the left — unmarked, and too slow to trigger by accident.

- The order is shuffled every single time you switch to them, so it is never
  the same run twice.
- Swipe between them, or hold the top-left corner to let them run as a
  slideshow, exactly as with photos.
- They need no permission and no network, so they work on a phone where you
  never granted access to photos at all.
- Every launch starts back on your photos and videos.

### Regenerating the pictures

The pictures are rendered from [Microsoft Fluent
Emoji](https://github.com/microsoft/fluentui-emoji) (MIT) and committed, so no
build ever needs the network. To change the set, edit the subject list and
re-run the script:

```
$EDITOR tools/pictures/subjects.txt
python3 tools/pictures/build-pictures.py
```

It needs Pillow (`pip install Pillow`) and a Chromium binary, and it rewrites
`app/src/main/assets/pictures/` — the images, the index the app reads, and
`tools/pictures/SOURCES.md`. Credits are in
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## Screen pinning / kiosk lock-down

The app tries to turn on Android's built-in **Screen Pinning** when it opens,
which stops the Back, Home and Recents buttons from leaving the app. Nothing
to configure — it happens by itself where the device allows it.

If your device doesn't allow it automatically, the app shows how to turn it
on by hand:

**Settings → Security → Advanced → Screen pinning → On**, then open the
app-switcher (square button) and tap the pin icon on Kids Photo Viewer.

Note: without enrolling the device as a managed ("kiosk") device — which
requires a computer and ADB, and is out of scope here — Android does not let
any ordinary app make itself completely unremovable. Screen Pinning is the
strongest lock-down available to a normal app, and is what this app uses.

## Releases and the build pipeline

[`.github/workflows/release.yml`](.github/workflows/release.yml) runs on every
push to `master`. It runs lint and unit tests, builds the release APK with R8
minification and resource shrinking, verifies the resulting signature with
`apksigner`, and publishes a GitHub release containing:

- the signed APK, named after its version,
- a `.sha256` checksum file,
- the gzipped R8 `mapping.txt`, so an obfuscated crash report can be read back.

Pull requests run the same build and checks but publish nothing.

Versions come from the commit count, so they only ever go up:
`1.0.<commits>`, tagged `v1.0.<commits>`.

## Signing

Release builds are signed with a **release key taken from repository
secrets** when one is configured, and fall back to the checked-in debug key
otherwise. The release notes of every build say which key was used.

To move to a real release key, create one and add it to the repository
secrets:

```bash
keytool -genkeypair -v \
  -keystore release.jks -storetype PKCS12 \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias release -dname "CN=Kids Photo Viewer, O=andys8, C=DE"

base64 -w0 release.jks    # the value for KEYSTORE_BASE64
```

Then, under **Settings → Secrets and variables → Actions**, add
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`release`) and
`KEY_PASSWORD`. Keep `release.jks` somewhere safe and do not commit it —
`*.jks` is ignored.

**Switching keys is a one-way door.** Android refuses to install an update
signed with a different key than the installed app, so everyone already
running a debug-signed build has to uninstall it once and install the new one.
Worth doing early, while that is nobody but you.

## Building locally

```
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

For a release build — minified, shrunk, and signed with the debug key unless
you export the keystore variables:

```
./gradlew assembleRelease
```
