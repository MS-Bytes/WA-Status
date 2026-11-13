<div align="center">

# Status Saver – Story Downloader

**View, save, edit, share and repost photo & video statuses — offline, on-device, no login.**

Works with WhatsApp & WhatsApp Business · Android 10 – 16 · Kotlin · Jetpack Compose · Material 3

</div>

> **Disclaimer:** This app is not affiliated with, endorsed or sponsored by WhatsApp LLC or Meta.
> WhatsApp is a trademark of WhatsApp LLC.

---

## ✨ Features

| | |
|---|---|
| 📥 **One-tap save** | Original quality, stream-to-stream copy (no re-compression) into `Pictures/StatusSaver` and `Movies/StatusSaver`. |
| 🔁 **Two apps** | Separate folder access for WhatsApp and WhatsApp Business with a quick switcher. |
| 🔄 **Auto refresh** | On resume, on tab switch, pull-to-refresh and a lightweight periodic rescan. New items get a **NEW** badge. |
| 🖼️ **Viewer** | Swipe between items, pinch-zoom images, Media3 ExoPlayer for videos (seek, mute, loop). |
| ✂️ **Editor** | Images: crop (free/1:1/4:5/9:16/16:9), rotate, flip, brightness/contrast/saturation, filters, text, draw. Videos: trim and mute. Always writes a new file. |
| 🔗 **Share & repost** | Share anywhere, or repost directly to WhatsApp / WhatsApp Business. |
| ☑️ **Multi-select** | Long-press for bulk save / share / delete, select all. |
| ⭐ **Favorites, sort & filter** | By date, type, size; favorites stored with Room. |
| 🤖 **Auto-save & alerts** | Optional auto-save per app, and a 15-minute WorkManager check with “X new statuses” notification. |
| 💬 **Direct chat** | Message any number without saving the contact (country code picker + optional message). |
| 🔒 **App Lock** | PIN or BiometricPrompt. |
| 🎨 **Theming** | Light / Dark / System, Material You dynamic color on Android 12+. |
| 🌐 **Languages** | English and Urdu (full RTL support), per-app language on Android 13+. |
| 🧩 **Widget** | Glance home-screen widget showing the new-status count. |

### Privacy by design
- No root, no account, no QR / WhatsApp Web.
- Reads **only** the status files WhatsApp already cached on the device, so opening them here sends no “seen” receipt.
- Folder access through the **Storage Access Framework** (one folder, user-approved). **No** `MANAGE_EXTERNAL_STORAGE`, **no** `READ_EXTERNAL_STORAGE`, **no** `READ_MEDIA_*`.
- No files are uploaded. No analytics. The only third-party SDK that sends data is Google Mobile Ads (with UMP consent).

---

## 📸 Screenshots

| Home | Viewer | Editor | Saved | Settings |
|:---:|:---:|:---:|:---:|:---:|
| _add `docs/screenshots/home.png`_ | _viewer.png_ | _editor.png_ | _saved.png_ | _settings.png_ |

---

## 🧱 Tech stack

- **Language:** Kotlin 2.2 (K2), Coroutines + Flow / StateFlow
- **UI:** Jetpack Compose, Material 3, Navigation Compose (type-safe routes), WindowSizeClass, SplashScreen API, edge-to-edge, predictive back
- **Architecture:** MVVM + Clean Architecture (`data` / `domain` / `ui`), single activity, Hilt DI
- **Media:** Coil 3 (+ video frame & GIF decoders), Media3 ExoPlayer, Media3 Transformer
- **Storage:** SAF + `DocumentsContract` queries, MediaStore (`IS_PENDING`), Room, DataStore Preferences
- **Background:** WorkManager (Hilt workers), Glance App Widget
- **Google Play:** AdMob + UMP consent, In-App Review, In-App Updates
- **Build:** AGP 8.13, Gradle 8.14 (Kotlin DSL), version catalog, KSP, R8 + resource shrinking, 16 KB page-size compatible
- **SDK levels:** `minSdk 29` · `targetSdk 36` · `compileSdk 36`

---

## 📁 Project structure

```
WA-Status/
├── app/
│   ├── build.gradle.kts            # app module, ad IDs & signing from local.properties
│   ├── proguard-rules.pro
│   ├── schemas/                    # exported Room schemas (generated)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/msbytes/statussaver/
│       │   │   ├── StatusSaverApp.kt          # @HiltAndroidApp, WorkManager config
│       │   │   ├── MainActivity.kt            # single activity, splash, edge-to-edge
│       │   │   ├── core/                      # dispatchers, result types, utils, extensions
│       │   │   ├── di/                        # Hilt modules
│       │   │   ├── data/
│       │   │   │   ├── saf/                   # SAF folder access + DocumentsContract scanner
│       │   │   │   ├── media/                 # MediaStore saver / saved-media source
│       │   │   │   ├── local/db/              # Room (favorites, seen/auto-saved records)
│       │   │   │   ├── local/prefs/           # DataStore settings & security
│       │   │   │   └── repository/            # repository implementations
│       │   │   ├── domain/
│       │   │   │   ├── model/                 # StatusItem, StatusSource, MediaType, …
│       │   │   │   ├── repository/            # repository interfaces
│       │   │   │   └── usecase/               # Save, Share, Delete, Observe, AutoSave…
│       │   │   ├── ui/
│       │   │   │   ├── theme/                 # Material 3 theme, colors, typography
│       │   │   │   ├── navigation/            # NavHost + type-safe routes
│       │   │   │   ├── components/            # grid tile, shimmer, empty/error states…
│       │   │   │   ├── onboarding/  permission/  home/  viewer/  saved/
│       │   │   │   ├── editor/  settings/  about/  faq/  directchat/  applock/
│       │   │   ├── ads/                       # AdManager, ConsentManager, ad composables
│       │   │   ├── work/                      # status check & auto-save workers
│       │   │   ├── notifications/             # channels & builders
│       │   │   └── widget/                    # Glance widget + receiver
│       │   └── res/                           # values (EN), values-ur (UR), night, xml, icons
│       ├── test/                              # unit tests (JVM / Robolectric)
│       └── androidTest/                       # instrumented tests
├── docs/                           # GitHub Pages: privacy-policy.html, app-ads.txt guide
├── gradle/libs.versions.toml       # version catalog
├── .github/workflows/android.yml   # CI: build, lint, unit tests, APK artifact
├── local.properties.example
├── CHANGELOG.md · LICENSE · README.md
```

---

## 🛠️ Build

**Requirements:** Android Studio (latest stable), JDK 17, Android SDK 36.

```bash
git clone https://github.com/MS-Bytes/WA-Status.git
```

```bash
cd WA-Status
```

```bash
cp local.properties.example local.properties   # then edit sdk.dir and (optionally) secrets
```

```bash
./gradlew assembleDebug        # debug APK (always uses Google test ads)
```

```bash
./gradlew lintDebug testDebugUnitTest
```

```bash
./gradlew bundleRelease        # Play Store .aab → app/build/outputs/bundle/release/
```

### App version
Bump `APP_VERSION_CODE` and `APP_VERSION_NAME` in **`gradle.properties`** — the single source of truth.

---

## 💰 AdMob setup

1. Create the app and six ad units (Banner, Interstitial, Native Advanced, App Open, Rewarded) in the AdMob console.
2. Put the IDs in `local.properties` (see `local.properties.example`):
   `ADMOB_APP_ID, ADMOB_BANNER_ID, ADMOB_INTERSTITIAL_ID, ADMOB_NATIVE_ID, ADMOB_APP_OPEN_ID, ADMOB_REWARDED_ID`.
3. **Debug builds always use Google’s test IDs.** Release builds use your real IDs only when *all* six are present; otherwise they fall back to test IDs and Gradle logs a warning.
4. The IDs are injected into `BuildConfig` and the manifest placeholder `${admobAppId}` — they are never committed.
5. Set `ADS_ENABLED=false` to build a completely ad-free binary (global kill-switch).
6. Set up GDPR / US-state messages in **AdMob → Privacy & messaging** so the UMP consent form can be shown.

### app-ads.txt
Host a file named `app-ads.txt` at the **root of the developer website** listed on your Play Store page (e.g. `https://ms-bytes.github.io/app-ads.txt` — note: GitHub Pages must serve it from the domain root, which means a `<user>.github.io` repository or a custom domain, not a project sub-path). Content:

```
google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0
```

Replace `pub-XXXXXXXXXXXXXXXX` with your AdMob publisher ID. Then press **“Check for updates”** under AdMob → Apps → app-ads.txt.

---

## 🔐 Release signing

1. Create an upload key (once), outside the repo or in a git-ignored folder:
   ```bash
   keytool -genkeypair -v -keystore keystore/upload-keystore.jks -keyalg RSA -keysize 4096 -validity 10000 -alias upload
   ```
2. Fill `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in `local.properties`.
3. `./gradlew bundleRelease` → upload the `.aab` to Play Console with **Play App Signing** enabled.

Without these keys the release bundle is built unsigned (useful for CI).

---

## 🔏 Privacy policy

Hosted from `docs/` with GitHub Pages:
**https://ms-bytes.github.io/WA-Status/privacy-policy.html**
(Repo → Settings → Pages → Source: `main` branch, `/docs` folder.)

---

## 🤝 Contributing

Issues and pull requests are welcome. Please run `./gradlew lintDebug testDebugUnitTest` before submitting.

## 📄 License

[MIT](LICENSE) © 2026 MS-Bytes
