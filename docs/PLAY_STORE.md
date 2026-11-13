# Google Play release kit

Everything needed to publish **Status Saver – Story Downloader** (`com.msbytes.statussaver`).

---

## 1. Build the release bundle

1. Create the upload keystore once (keep it **outside** the repo or in the git-ignored `keystore/` folder):
   ```bash
   keytool -genkeypair -v -keystore keystore/upload-keystore.jks -keyalg RSA -keysize 4096 -validity 10000 -alias upload
   ```
2. Fill `local.properties` (see `local.properties.example`): AdMob IDs + `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Bump `APP_VERSION_CODE` / `APP_VERSION_NAME` in `gradle.properties`.
4. Build:
   ```bash
   ./gradlew clean bundleRelease
   ```
5. Check the Gradle log does **not** print `⚠ AdMob: real IDs not found` (otherwise the bundle contains test ads).
6. Upload `app/build/outputs/bundle/release/app-release.aab` in Play Console with **Play App Signing** enabled.

R8 minification + resource shrinking are enabled; rules are in `app/proguard-rules.pro`. Native libraries are
stored uncompressed and page-aligned (16 KB page size compatible).

---

## 2. Store listing

**App name (30):** `Status Saver – Story Downloader`

**Short description (80):**
`Save, edit & repost photo and video statuses. Fast, private, no login needed.`

**Full description:**

```
Status Saver – Story Downloader lets you view, save, edit and share photo and video statuses that are already on your phone. Works with WhatsApp and WhatsApp Business.

★ ONE-TAP SAVE
Download any image or video status in its original quality. Saved files appear instantly in your gallery (Pictures/StatusSaver and Movies/StatusSaver).

★ PRIVATE BY DESIGN
• No login, no account, no QR code scanning
• Works offline – everything happens on your device
• Nothing is uploaded, ever
• Reads only the status folder you approve

★ BUILT-IN STATUS EDITOR
• Crop (free, 1:1, 4:5, 9:16, 16:9), rotate and flip
• Brightness, contrast and saturation
• 8 beautiful filters
• Add text and draw
• Trim videos and remove audio
Your original file is never changed – edits are saved as a new copy.

★ SHARE & REPOST
Share statuses to any app or repost them directly to WhatsApp or WhatsApp Business.

★ SMART FEATURES
• Auto-refresh with “NEW” badges
• Optional auto-save of new statuses
• New-status notifications and home-screen widget
• Multi-select: save, share or delete many files at once
• Favorites, sorting and filters
• Direct Chat: message any number without saving it as a contact
• App Lock with PIN or fingerprint
• Light, dark and Material You themes
• Works on phones, tablets and foldables

HOW TO USE
1. Open WhatsApp and view the Updates tab so statuses are downloaded.
2. Open Status Saver and allow access to the status folder (one time).
3. Tap the download button on any status.

DISCLAIMER
This app is not affiliated with, endorsed or sponsored by WhatsApp LLC or Meta. WhatsApp is a trademark of WhatsApp LLC. Please respect the privacy and copyright of others – only save and repost content with the owner’s permission.
```

**Category:** Tools (alternative: Video Players & Editors) · **Tags:** status saver, story saver, video downloader, photo editor.

**Contact:** support e-mail (replace `support@example.com` in `AboutScreen.kt` and `docs/privacy-policy.html`).
**Privacy policy URL:** `https://ms-bytes.github.io/WA-Status/privacy-policy.html` (enable GitHub Pages → branch `main`, folder `/docs`).

### Graphics ideas
- **Icon:** 512×512 export of the adaptive icon (indigo gradient, white story ring + download arrow). No WhatsApp logo or green phone.
- **Feature graphic (1024×500):** indigo→coral gradient, phone mock-up with the status grid, headline “Save Statuses in One Tap”.
- **Screenshots (phone 1080×2400, 6–8):**
  1. “Save photo & video statuses in one tap” – Images grid with NEW badges
  2. “Watch, zoom & save” – Viewer
  3. “Edit before you repost” – Image editor with filters/text
  4. “Trim videos in seconds” – Video editor
  5. “All your downloads in one place” – Saved tab
  6. “Chat without saving contacts” – Direct Chat
  7. “Private. Offline. Secure.” – App Lock / settings
- Tablet screenshots (optional): same scenes on a 7" and 10" layout.

Never show the WhatsApp logo, real people’s photos without permission, or text implying an official WhatsApp product.

---

## 3. Data safety form

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (via Google Mobile Ads SDK) |
| Is all user data encrypted in transit? | **Yes** |
| Do you provide a way for users to request data deletion? | **Yes** – uninstalling deletes app data; ad data via Android “Delete advertising ID” / Ad privacy options |
| Account creation required? | **No** |

**Data types (collected and shared by AdMob):**

| Data type | Collected | Shared | Optional? | Purposes |
|---|---|---|---|---|
| Device or other IDs (Advertising ID) | Yes | Yes | No | Advertising or marketing, Analytics, Fraud prevention, security & compliance |
| App interactions (ad impressions/clicks) | Yes | Yes | No | Advertising or marketing, Analytics |
| Diagnostics (crash logs / performance of the ads SDK) | Yes | Yes | No | Analytics, Fraud prevention |
| Approximate location (derived from IP) | Yes | Yes | No | Advertising or marketing, Fraud prevention |

**Not collected:** photos & videos (processed on device only, never transmitted), files, contacts, messages, personal info, financial info, health, precise location, web history.

> Processing that happens only on the device is not “collection” under Play’s definition, so photos/videos are **not** declared.

---

## 4. Other Play Console declarations

- **Ads:** Yes, the app contains ads.
- **Target audience:** 18+ only. Not designed for children → not in the Families program.
- **Content rating (IARC questionnaire):** Category *Utility, Productivity, Communication or Other*. Answer **No** to violence, sexual content, profanity, drugs, gambling. *Users can interact / share content:* **No** (the app has no social features of its own; sharing is via Android share sheet). *Shares location:* No. *Digital purchases:* No. Expected rating: Everyone / PEGI 3 (the 18+ target audience is still set separately).
- **Advertising ID:** Yes – used for advertising (declared `AD_ID` permission).
- **Permissions:** no storage, location, contacts or SMS permissions. `POST_NOTIFICATIONS` (optional alerts), `USE_BIOMETRIC` (optional App Lock).
- **News app / government app / financial features / health:** No.
- **App access:** All features available without login – no credentials needed.

### Trademark / impersonation policy checklist
- Name, icon and screenshots do not use “WhatsApp” as branding or its logo.
- Descriptive mention only (“Works with WhatsApp & WhatsApp Business”).
- Disclaimer in description, About screen, onboarding and privacy policy.

---

## 5. AdMob checklist

- Create app + 6 ad units; set IDs in `local.properties` (or GitHub secrets).
- **Privacy & messaging:** publish a GDPR message and a US state regulations message (required for the UMP form).
- **app-ads.txt:** host at the root of the developer website listed on Play (e.g. `https://<your-domain>/app-ads.txt`):
  ```
  google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0
  ```
  A GitHub Pages *project* site (`ms-bytes.github.io/WA-Status/`) is not a domain root — use a `ms-bytes.github.io` user/org repository or a custom domain.
- Add your test device ID while testing real ad units, never click your own live ads.

Placements implemented: adaptive banner (Viewer, Saved tab), native row every 12 grid items (labelled “Ad”),
interstitial after a successful save (never the first save, max 1 / 3 min), App Open on resume (max 1 / 4 h, never on first launch,
onboarding, permission guide, editor or App Lock), rewarded ad → 30 minutes without full-screen ads.

---

## 6. Test checklist

Run on: **Android 10, 11, 13, 14, 15, 16** – Pixel (stock) and Samsung (One UI). Include one tablet/foldable and one small phone.

**Install & onboarding**
- [ ] Fresh install → splash → onboarding (no ads, consent form appears in EEA via VPN / debug geography)
- [ ] Privacy policy link opens; disclaimer visible

**Folder access**
- [ ] “Grant access” opens the picker directly in `.Statuses` → “Use this folder” → “Allow”
- [ ] Choosing a wrong folder shows the error and retry works
- [ ] WhatsApp Business switch asks for its own folder
- [ ] Revoke in Settings → app shows the re-grant screen
- [ ] WhatsApp not installed → install card
- [ ] Android 10 with legacy `WhatsApp/Media/.Statuses` path

**Statuses**
- [ ] Images/Videos tabs, video duration + play icon, NEW badges after new statuses arrive
- [ ] Pull-to-refresh, auto rescan, tab switch refresh
- [ ] 500+ items scroll smoothly
- [ ] Download → animation → snackbar Open / Share; second tap shows “Already saved”
- [ ] Files appear in Gallery (Pictures/StatusSaver, Movies/StatusSaver) in original size
- [ ] Low storage message (fill device) and missing file message (delete status while viewing)

**Viewer / editor**
- [ ] Swipe, pinch-zoom, double-tap zoom, video play/pause/seek/mute/loop
- [ ] Share, repost to WhatsApp & WA Business, favorite, info, delete (Saved)
- [ ] Image editor: every crop ratio, rotate, flip, sliders, filters, text add/edit/move/delete, draw/eraser, undo/redo, save → new file
- [ ] Video editor: trim, mute, progress dialog, cancel, saved copy plays

**Extras**
- [ ] Multi-select save/share/delete, select all, back clears selection
- [ ] Delete on Android 11+ for files from a previous install (system dialog)
- [ ] Auto-save + notifications (Android 13+ permission flow, “Don’t ask again” → settings)
- [ ] Widget shows new count and opens the app
- [ ] App Lock PIN, wrong PIN, lockout, biometrics, 30 s grace period
- [ ] Direct chat with country picker, invalid number error
- [ ] Theme light/dark/system, dynamic color (12+), rotation, tablet layout, large fonts, TalkBack labels
- [ ] Predictive back gesture (Android 14+), edge-to-edge on 15/16

**Ads (debug build = test ads)**
- [ ] Banner on Viewer & Saved, native rows in grid, interstitial on 2nd save, cap 3 min
- [ ] App open after background, not on first launch / onboarding / permission / editor / lock
- [ ] Rewarded ad pauses interstitials 30 min
- [ ] Airplane mode: no crashes, ads silently absent
- [ ] Ad privacy options entry visible where required
