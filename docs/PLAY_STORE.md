# Publishing to the Google Play Store

This repo can build a signed release and upload it to Google Play from a
GitHub Action. Everything that a machine *can* do is done by
[`.github/workflows/release.yml`](../.github/workflows/release.yml). What is
left is a one-time pass through the Play Console, because Google requires a
human to fill in the store listing and the policy declarations — there is no
API for those.

Rough time budget for the one-time part: **1–2 hours**, plus a wait for
review. After that, shipping a new version is one button in the Actions tab.

---

## 1. Create the upload key

Do this once, on your own machine, and keep the file somewhere safe. If you
lose it you can ask Google to reset it, but it is a hassle.

```bash
keytool -genkeypair -v \
  -keystore upload.jks \
  -storetype PKCS12 \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias upload \
  -dname "CN=Kids Photo Viewer, O=andys8, C=DE"
```

It asks for a password twice — use the same one for the store and the key, it
keeps the secrets simpler.

Then turn it into a secret-sized string:

```bash
base64 -w0 upload.jks          # Linux
base64 -i upload.jks | tr -d '\n'   # macOS
```

Add these under **GitHub → Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | the base64 blob from above |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | the key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | the JSON from step 5 (add it later) |

The keystore itself is never committed — `*.jks` is in `.gitignore`.

---

## 2. Create the app in the Play Console

<https://play.google.com/console> → **Create app**.

- App name: `Kids Photo Viewer` (max 30 characters)
- Default language: English (or German)
- App or game: **App**
- Free or paid: **Free** — note that this is irreversible; a free app can
  never be switched to paid.

---

## 3. Fill in the store listing

You need these assets. Everything else is text.

| Asset | Requirement |
| --- | --- |
| App icon | 512 × 512 PNG, 32-bit with alpha |
| Feature graphic | 1024 × 500 PNG or JPG |
| Phone screenshots | at least 2 (up to 8), each side between 320 and 3840 px |

The icon can be exported from `app/src/main/res/drawable/ic_launcher_*.xml`.
Screenshots are just a screen recording still from the app running on your
phone — a photo slideshow frame and the permission screen are enough.

Suggested text, ready to paste:

**Short description** (max 80 characters)

```
Hand your phone to a toddler. A locked-down slideshow of your own photos.
```

**Full description** (max 4000 characters)

```
Kids Photo Viewer turns your phone into a safe photo album for a small child.

It shows the photos and videos from your camera roll full screen, one after
another, and locks itself down so little fingers cannot swipe out of it, open
other apps, or change or delete your pictures.

• Shows only photos and videos from the standard camera folder — no
  screenshots, no downloads. Most recent first.
• Runs as a continuous slideshow. Photos show for three seconds, videos play
  through once and then move on. Swipe left or right to go back and forward.
• Plays high frame rate 4K footage smoothly, with display frame-rate matching
  and HDR output.
• Keeps the screen awake, so the phone does not lock while your child is
  watching.
• Full screen — the status and navigation bars are hidden.
• Uses Android's built-in Screen Pinning so the app locks itself down
  automatically where the device allows it.
• No accounts, no cloud, no adverts, no tracking. The app has no internet
  access at all and reads your existing photos directly from the device.

Nothing is uploaded, copied, changed or deleted. Uninstalling the app leaves
your photos exactly as they were.
```

**Privacy policy URL** — required. Use the file in this repo:

```
https://github.com/andys8/kids-viewer/blob/master/docs/privacy-policy.md
```

(If you would rather have a proper page, turn on GitHub Pages for the `docs/`
folder in the repo settings and use that URL instead.)

---

## 4. Work through the declarations

Play Console → **Policy → App content**. All of these are mandatory before you
can publish anything, even to a test track.

- **Privacy policy** — the URL above.
- **Ads** — no.
- **App access** — all functionality is available without restrictions; no
  login needed.
- **Content rating** — fill in the questionnaire. Category "Utility,
  Productivity, Communication or Other", and the answer to every content
  question is no. Comes out as rated for everyone.
- **Target audience and content** — **this is the one to think about.**
  Because the app is a tool *for the parent*, select an adult age group
  (18 and over) and answer "no" to "is your app appealing to children". That
  keeps you out of the Families policy programme, which brings a much heavier
  set of requirements. Be aware that Google reviews this against your listing:
  the word "Kids" in the title may prompt them to disagree. If they do, the
  listing text above deliberately frames the app as a parent's tool, which is
  the argument to make. Worst case you rename it to something like
  "Toddler-safe Photo Slideshow".
- **Data safety** — the honest answers are: no data collected, no data shared,
  no data encrypted in transit (nothing is transmitted), no deletion request
  mechanism needed.
- **Photo and video permissions** — the app requests `READ_MEDIA_IMAGES` and
  `READ_MEDIA_VIDEO`, which Google treats as restricted. You have to submit a
  declaration explaining why broad access is core to the app (a continuous
  slideshow of the camera roll cannot use the one-shot system photo picker)
  and attach a short screen recording showing it. Google reviews this
  manually; expect a few days.
  *If you want to skip this form entirely*, the alternative is to drop the
  broad permissions and rely only on
  `READ_MEDIA_VISUAL_USER_SELECTED`, i.e. the parent picks the photos the
  child may see. That is arguably a nicer design for this app, but it is a
  code change, not a form.
- **Government apps / financial features / health** — no to all.

---

## 5. Set up the service account so the Action can upload

1. Play Console → **Setup → API access** → link a Google Cloud project
   (create a free one if you have none).
2. In that project, create a **service account** and download a **JSON key**.
3. Back in Play Console → **Users and permissions**, invite the service
   account's email address and grant it, for this app: *Release to testing
   tracks* and *Release to production* (plus "View app information").
4. Paste the whole JSON file into the `PLAY_SERVICE_ACCOUNT_JSON` GitHub
   secret.

---

## 6. The first upload has to be manual

The Play Developer API refuses to publish to an app that has never had a
release. So for the very first one:

```bash
KEYSTORE_PATH=/path/to/upload.jks \
KEYSTORE_PASSWORD=... KEY_ALIAS=upload KEY_PASSWORD=... \
BUILD_NUMBER=1 BUILD_STAMP=1.0 \
./gradlew bundleRelease
```

and upload `app/build/outputs/bundle/release/app-release.aab` to the
**Internal testing** track by hand. Accept **Play App Signing** when offered —
that is what makes the upload key above replaceable if you ever lose it.

From then on, every release goes through the Action.

---

## 7. Releasing

GitHub → **Actions → Play Store release → Run workflow**. Pick a track
(`internal` to start with) and optionally a version name.

The workflow:

- derives the version code from the commit count, so it always goes up,
- builds a signed AAB and a signed APK,
- uploads the AAB to the track you chose,
- publishes the APK as a GitHub release, so sideloading still works.

Release notes come from `distribution/whatsnew/whatsnew-en-US` — edit that
file before you release if you want to say something specific.

---

## 8. The bit that costs real time: production access

If your Play developer account is a **personal** account created on or after
**13 November 2023**, Google requires a closed test with at least **12
testers, opted in continuously for 14 days**, and they check the testers
actually opened the app, before you may apply for production access. Twelve
real people, two weeks, per app.

If your account predates that (or is a registered business account), none of
this applies and you can go straight to production.

**You do not have to go to production at all.** The tracks below are all
free and all give people a proper Play install with automatic updates:

| Track | Who can install | Limit | Setup cost |
| --- | --- | --- | --- |
| Internal testing | email addresses you list | 100 testers | listing + declarations only |
| Closed testing | an email list or a Google Group | 2000 per list | same |
| Open testing | anyone with the link | unlimited | same, but reviewed more strictly |
| Production | anyone, searchable | — | the 12-tester rule may apply |

For "I want to share this with friends and family", **internal testing** is
the sweet spot: no Play Protect warnings, automatic updates, no 14-day wait.

---

## Notes and gotchas

- The app targets Android 16 (API 36). Since 31 August 2026 Google rejects new
  submissions targeting anything lower.
- The Play version and a sideloaded APK cannot be installed at the same time —
  the signatures differ once Play App Signing re-signs the app. Uninstall one
  before installing the other.
- Version codes are derived from the commit count. Never rewrite history on
  `master` after a release, or the next code may not be higher than the last.
- Google's review is slow for a first submission (often several days) and much
  faster afterwards.
