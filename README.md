# MohgWatch 🩸⌚

MohgWatch is a comprehensive, modern companion app and Wear OS watch face for FreeStyle Libre users via the LibreLinkUp API.

## Features ✨
- **Wear OS Integration**: Clean, AMOLED-friendly watch tile displaying your glucose trend graph (up to 3 hours), custom thresholds, and current glucose values.
- **Customizable UI**: Choose from multiple colorful themes including Monochrome, AMOLED Black, Teal, and more.
- **Live Settings**: Adjust settings on your phone and watch them instantly sync to your wrist! No manual saving needed.
- **Smart Notifications**: Overcome system DND rules for critical glucose alerts (e.g. going below 70 mg/dl or above 180 mg/dl). 
- **Graph on Demand**: Pull your Libre data and visualize it natively in the app, built perfectly for round watches and modern Android phones.
- **Multilingual Support**: Supports English and Polish localizations.

## Architecture
- **Tech Stack**: 100% Kotlin, Jetpack Compose, Wear Compose, Coroutines, Flow.
- **Connectivity**: Wearable Data Layer API ensures instantaneous state sharing between phone and watch.
- **API**: Connects directly to LibreLinkUp, handling token renewals and regional domains automatically.

## How to use
1. Install the app on both your phone and Wear OS watch.
2. Enter your LibreLinkUp credentials on the phone.
3. Customize your thresholds, themes, and alert preferences.
4. Your data will automatically sync with the watch face tile.

## Privacy & Security
Your LibreLinkUp credentials are saved locally and securely. We do not store or transmit your data to any third parties.

---
Built with Jetpack Compose.
