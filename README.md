<div align="center">

# WAVAULT

**View, save, edit, share and repost photo & video statuses — offline, on-device, no login.**

Works with WhatsApp & WhatsApp Business · Android 7.0 – 16 · Kotlin · Jetpack Compose · Material 3 · English

</div>

> **Disclaimer:** This app is not affiliated with, endorsed or sponsored by WhatsApp LLC or Meta.
> WhatsApp is a trademark of WhatsApp LLC.

---

## ✨ Features

| | |
|---|---|
| 📥 **One-tap save** | Original quality, stream-to-stream copy (no re-compression) into `Pictures/WAVAULT` and `Movies/WAVAULT`. |
| 🔁 **Universal** | One folder grant covers WhatsApp and WhatsApp Business — statuses from both appear together. |
| 🔄 **Auto refresh** | On resume, on tab switch, pull-to-refresh and a lightweight periodic rescan. New items get a **NEW** badge. |
| 🖼️ **Viewer** | Swipe between items, pinch-zoom images, Media3 ExoPlayer for videos (seek, mute, loop). |
| ✂️ **Editor** | Images: crop (free/1:1/4:5/9:16/16:9), rotate, flip, brightness/contrast/saturation, filters, text, draw. Videos: trim and mute. Always writes a new file. |
| 🔗 **Share & repost** | Share anywhere, or repost directly to WhatsApp / WhatsApp Business. |
| ☑️ **Multi-select** | Long-press for bulk save / share / delete, select all. |
| ⭐ **Favorites, sort & filter** | By date, type, size; favorites stored with Room. |
| 🤖 **Auto-detect** | Rescans every few seconds while open and every 15 minutes in the background; notifies about new statuses, optional auto-save. |
| 🔒 **App Lock** | PIN or BiometricPrompt. |
| 🎨 **Theming** | Light / Dark / System, Material You dynamic color on Android 12+. |
| 🧩 **Widget** | Glance home-screen widget showing the new-status count. |

### Privacy by design
- No root, no account, no QR / WhatsApp Web.
- Reads **only** the status files WhatsApp already cached on the device, so opening them here sends no “seen” receipt.
- Folder access through the **Storage Access Framework** (one folder, user-approved). **No** `MANAGE_EXTERNAL_STORAGE` and **no** `READ_MEDIA_*`. `WRITE_EXTERNAL_STORAGE` is requested only on Android 7–9 (`maxSdkVersion 28`).
- No files are uploaded. No analytics. The only third-party SDK that sends data is Google Mobile Ads (with UMP consent).

---

## 📸 Screenshots

| Home | Viewer | Editor | Saved | Settings |
|:---:|:---:|:---:|:---:|:---:|
| _add `docs/screenshots/home.png`_ | _viewer.png_ | _editor.png_ | _saved.png_ | _settings.png_ |

---

## 🧱 Tech stack

- **Language:** Kotlin 2.2 (K2), Coroutines + Flow / StateFlow
- **UI:** Jetpack Compose, Material 3, Navigation Compose (type-safe routes), adaptive grid, SplashScreen API, edge-to-edge, predictive back
- **Architecture:** MVVM + Clean Architecture (`data` / `domain` / `ui`), single activity, Hilt DI
- **Media:** Coil 3 (+ video frame & GIF decoders), Media3 ExoPlayer, Media3 Transformer
- **Storage:** SAF + `DocumentsContract` queries, MediaStore (`IS_PENDING`), Room, DataStore Preferences
- **Background:** WorkManager (Hilt workers), Glance App Widget
- **Google Play:** AdMob + UMP consent, In-App Review, In-App Updates
- **Build:** AGP 8.13, Gradle 8.14 (Kotlin DSL), version catalog, KSP, R8 + resource shrinking, 16 KB page-size compatible
- **SDK levels:** `minSdk 24` (Android 7.0) · `targetSdk 36` · `compileSdk 36`

---

## 📁 Project structure

```
WA-Status/
├── app/
│   ├── build.gradle.kts            # ad IDs & signing read from local.properties / env
│   ├── proguard-rules.pro
│   ├── schemas/                    # exported Room schema
│   └── src/
│       ├── main/java/com/msbytes/statussaver/
│       │   ├── StatusSaverApp.kt · MainActivity.kt · MainViewModel.kt
│       │   ├── core/                 # UiState, UiEvent, formatters, intents, media metadata cache
│       │   ├── domain/model/         # StatusItem, StatusSource, SaveResult, settings …
│       │   ├── domain/repository/    # repository interfaces
│       │   ├── data/saf/             # SAF folder access + DocumentsContract scanner
│       │   ├── data/media/           # MediaStore saver (IS_PENDING, duplicate check)
│       │   ├── data/share/           # share / repost intents
│       │   ├── data/local/           # Room (favorites) + DataStore (settings, PIN)
│       │   ├── data/repository/      # repository implementations
│       │   ├── di/                   # Hilt modules
│       │   ├── ads/                  # AdManager, ConsentManager (UMP), frequency policy, banner, native
│       │   ├── work/                 # 15-min status check (auto-save + notifications)
│       │   ├── notifications/ · widget/ · security/ · play/
│       │   └── ui/                   # theme, navigation, components, home, saved, viewer,
│       │                             # editor, settings, about, faq, applock, onboarding
│       ├── main/res/                 # strings, themes, adaptive icon, native ad layout, xml configs
│       └── test/                     # JVM unit tests (repository, saver, ad caps, validator)
├── docs/
│   ├── privacy-policy.html         # GitHub Pages
│   └── PLAY_STORE.md               # listing, Data safety, content rating, test checklist
├── gradle/libs.versions.toml       # version catalog
├── .github/workflows/android.yml   # CI: lint, unit tests, debug APK, release AAB
└── local.properties.example · CHANGELOG.md · LICENSE · README.md
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
7. Set `DONATE_ENABLED=false` to hide the crypto donation entry (see Play policy note in `docs/PLAY_STORE.md`).
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

## 🚀 Publishing

See **[docs/PLAY_STORE.md](docs/PLAY_STORE.md)** for the store listing text, Data safety answers,
content-rating guidance, AdMob checklist and the device test checklist.

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
