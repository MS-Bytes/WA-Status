# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project uses [Semantic Versioning](https://semver.org/).

## [1.2.0] – 2026-11-14

### Added
- **Instant Detection**: optional foreground service that watches the status folders
  every few seconds while the app is closed and notifies within seconds — no need to
  open WhatsApp. Restarts after reboot; offers to ignore battery optimization.

### Note
- Only statuses WhatsApp has downloaded to the device can be detected; the app never
  contacts WhatsApp servers.

## [1.1.0] – 2026-11-14

### Changed
- Renamed the app to **WAVAULT**; Title Case UI text, shorter copy.
- Universal status list: WhatsApp and WhatsApp Business together (one folder grant), no app switcher.
- Faster first load, throttled video metadata reads.
- New-status alerts on by default; background detection every 15 minutes.
- Saved files now go to Pictures/WAVAULT and Movies/WAVAULT.
- Android 7.0+ support (minSdk 24) with legacy saving on Android 7–9.

### Added
- Donate (USDT TRC20) in Settings.

### Removed
- Direct Chat, “How To Use” page (merged into FAQ), Ads section in Settings.

## [1.0.0] – 2026-11-13

### Added
- View image & video statuses from WhatsApp and WhatsApp Business through the Storage Access Framework.
- One-tap save (original quality, duplicate check), share, repost, delete, info.
- Full-screen viewer with pinch-zoom and Media3 video playback.
- Saved tab with filters, favorites and storage summary.
- Image editor (crop ratios, rotate, flip, adjustments, filters, text, draw) and video editor (trim, mute).
- Multi-select, sort, auto refresh with NEW badges, auto-save, new-status notifications, home-screen widget.
- App Lock (PIN + biometrics), onboarding, FAQ, About, theme & dynamic color.
- Google AdMob (banner, native, interstitial, app open, rewarded) with UMP consent and frequency caps.
- In-app review and flexible in-app updates.
- Privacy policy, Play Store release kit, GitHub Actions CI.
