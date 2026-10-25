# MohgWatch 🩸⌚

MohgWatch is a comprehensive, modern companion app and Wear OS watch face for FreeStyle Libre users via the LibreLinkUp API.

## Features ✨
- **Wear OS Integration**: AMOLED-friendly watch tile displaying recent glucose history, custom thresholds, and current glucose values. Local history is bounded to 24 hours.
- **Customizable Phone UI**: Choose from multiple colorful themes including Monochrome, AMOLED Black, Teal, and more. The Wear application and injection-site tile use a fixed dark appearance.
- **Live Settings**: Glucose thresholds, alert preferences and language sync from the phone to the watch. Appearance settings apply only to the phone.
- **Glucose Alerts**: Alarm audio for glucose outside your thresholds, subject to Android notification and Do Not Disturb settings.
- **Threshold Vibrations**: Low and high glucose alarms use the thresholds in Glucose and thresholds settings. Optional recovery vibration follows the return to range; arrow changes do not vibrate. Existing pattern choices are preserved.
- **Diagnostics**: Settings → Logs and diagnostics provides a short copyable report and full logs shared through Android (24 h, 72 h or 7 days). Local diagnostics survive process restart with a 7-day / 8 MiB limit.
- **Freshness**: The measurement age limit defaults to 10 minutes and is configurable from 1 to 60 minutes under Notifications → Missing data and disconnections. Expired complications display `- -`; a local watch alarm warns when updates stop.
- **Multilingual Support**: Supports English and Polish localizations.

## Navigation
The bottom bar has Status, Notifications and Settings. Notifications opens six focused alarm pages; Settings contains glucose thresholds, doses, sync, background operation, phone/widget appearance, Profile, diagnostics and a three-page test menu. Profile exports and imports settings as versioned JSON without credentials. Import validates the file before confirmation and replacing local settings. The login screen displays the app logo. The persistent phone notification distinguishes “Na ręku”, “Zdjęty” and “Brak danych”; missing, invalid or stale wrist observations display “Brak danych”. Reading simulation lets you select glucose and trend, and hide or restore the Demo button. Sound and vibration pages link directly to their tests. App updates preserve existing saved settings.

Retired alarm/trend settings, unused watch font/arrow options, the inactive preset selector and watch appearance synchronization are no longer used or exported. Existing version-1 settings exports remain importable; retired fields are ignored. System-selected WFF faces remain available.

## Architecture
- **Tech Stack**: 100% Kotlin, Jetpack Compose, Wear Compose, Coroutines, Flow.
- **Connectivity**: Wearable Data Layer API ensures instantaneous state sharing between phone and watch.
- **API**: Connects directly to LibreLinkUp, handling token renewals and regional domains automatically.

## How to use
1. Install the app on both your phone and Wear OS watch.
2. Enter your LibreLinkUp credentials on the phone.
3. Customize your thresholds, phone appearance, and alert preferences.
4. Your data will automatically sync with the watch face tile.

## Privacy & Security
Your LibreLinkUp credentials are encrypted locally with an Android Keystore key and reused for automatic login. Credentials are excluded from backups; reinstalling or changing phones can require one login. We do not transmit your data to any third parties other than the configured LibreLinkUp service and your paired watch.

## Overnight monitoring
Android battery optimization must be disabled for MohgWatch to access the network in Doze. Settings → Battery and background shows the battery exemption, exact-alarm and notification status. Persistent wake protection keeps the CPU available but does not grant network access. Manufacturer power restrictions can still require device-specific settings.

## Known watch-face packaging limitation
The legacy WFF faces are bundled alongside executable Wear code. Android Lint reports `WatchFaceFormatDeclaresHasNoCode` and `WatchFaceFormatMissingVersion`. Proper WFF delivery needs a separate resource-only package; setting `hasCode=false` on the current Wear application would disable its services. The app, tiles and complication providers retain their current package.

---
Built with Jetpack Compose.

Wersja aplikacji: v1.5
