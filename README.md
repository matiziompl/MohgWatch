# MohgWatch 🩸⌚

MohgWatch is a comprehensive, modern companion app and Wear OS watch face for FreeStyle Libre users via the LibreLinkUp API.

## Features ✨
- **Wear OS Integration**: AMOLED-friendly watch tile displaying recent glucose history, custom thresholds, and current glucose values. Local history is bounded to 24 hours.
- **Customizable Phone UI**: Choose from multiple colorful themes including Monochrome, AMOLED Black, Teal, and more. The Wear application and injection-site tile use a fixed dark appearance.
- **Live Settings**: Glucose thresholds, alert preferences and language sync from the phone to the watch. Appearance settings apply only to the phone.
- **Glucose Alerts**: Alarm audio for glucose outside your thresholds, subject to Android notification and Do Not Disturb settings.
- **Threshold Vibrations**: Low and high glucose alarms use the thresholds in Glucose and thresholds settings. Optional recovery vibration follows the return to range; arrow changes do not vibrate. Existing pattern choices are preserved.
- **Diagnostics**: Settings → Logs and diagnostics provides a short copyable report and full logs shared through Android. Filter by time, event category, errors, cycle or night-test ID. Local diagnostics survive process restart with a 7-day / 8 MiB limit.
- **Experimental CDM**: Android 16 can associate MohgWatch with an already paired watch and request device-presence observation. This is optional, uses no root or Shizuku, and does not guarantee five-minute synchronization.
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

## Test CDM na Androidzie 16

Funkcja znajduje się w **Ustawienia → Synchronizacja → Zaawansowane → Companion Device Manager**. Domyślnie obserwacja jest wyłączona. Na starszym Androidzie pozostaje dotychczasowa synchronizacja. Klient LibreLinkUp, alarmy, harmonogram i komunikacja Wear OS nie wymagają obecności zegarka.

### Najpierw pomiar bazowy

1. Użyj tej samej wersji testowej APK we wszystkich porównywanych nocach. Ustal interwał, stałe wybudzenie i limit wieku danych; nie zmieniaj ich między pomiarami.
2. Otwórz ekran CDM i wybierz **Odśwież stan**. Pomiar bazowy wymaga braku jakiejkolwiek asocjacji MohgWatch. Wyłączona obserwacja przy zachowanej asocjacji to osobny wariant, nie czysty baseline.
3. Przy włączonej synchronizacji wybierz **Rozpocznij pomiar nocny**. Zanotuj widoczny stan baterii i danych w tle. Pozostaw ekran wyłączony w zwykłych warunkach nocnych.
4. Rano wybierz **Zakończ pomiar nocny** i zachowaj podsumowanie oraz pełne logi z tego okresu.

### Powiązanie i noc z obserwacją

1. Wybierz **Dodaj już sparowany zegarek**, udziel dostępu do sparowanych urządzeń Bluetooth, wybierz swój zegarek i potwierdź asocjację w systemowym oknie.
2. To powiązanie aplikacji z istniejącym urządzeniem. MohgWatch nie wywołuje tworzenia/usuwania bondingu, nie przejmuje profilu WATCH i nie zastępuje aplikacji zarządzającej zegarkiem. Jeśli zegarka nie widać, przerwij tę próbę i wyeksportuj diagnostykę; nie resetuj go ani nie paruj ponownie tylko na potrzeby testu.
3. Wybierz własną asocjację i włącz **Żądanie obserwacji**. Sprawdź rzeczywiste zdarzenia obecności oraz wyjątki baterii/danych. Samo żądanie nie potwierdza aktywnego bindingu ani dostępu do internetu.
4. Rozpocznij kolejny pomiar nocny przy tych samych ustawieniach i podobnych warunkach. Osobno można zmierzyć wariant z asocjacją bez obserwacji.
5. Zweryfikuj również nieobecność zegarka, restart telefonu, brak internetu i powrót połączenia. Telefon nadal powinien próbować pobierać dane, a stare pomiary zachować swój prawdziwy wiek.

Zmiana synchronizacji, interwału, stałego WakeLocka, limitu świeżości lub konfiguracji CDM przerywa bieżący pomiar. Naturalne pojawienie się lub zniknięcie zegarka go nie przerywa. Restart procesu w tym samym rozruchu pozwala kontynuować, ale raport ostrożnie oznacza możliwą niekompletność logów. Restart telefonu przerywa sesję; czasów monotonicznych z różnych rozruchów nie wolno porównywać.

### Logi i interpretacja

**Logi i diagnostyka** zachowują istniejące okresy 24 h / 72 h / 7 dni oraz umożliwiają własny zakres, kategorię CDM/sieć/API/synchronizacja, same błędy i identyfikatory cyklu lub pomiaru. Daty można podać jako `2026-10-25 01:00` w lokalnej strefie albo ISO z offsetem, np. `2026-10-25T02:30:00+02:00`. Przy zmianie czasu offset usuwa niejednoznaczność lokalnej godziny.

Eksportuj **pełne logi**, nie tylko krótki raport. Raport podaje straty kolejki, retencję i obcięcie. Liczby planowanych/dostarczonych alarmów i timerów są odrębne od liczby wykonanych cykli. HTTP 401 odzyskane przez ponowne logowanie lub błąd opcjonalnej historii nie oznaczają automatycznie utraty bieżącego pomiaru. Potwierdzenie operacji Data Layer nie jest potwierdzeniem odbioru na fizycznym zegarku.

Procent baterii jest orientacyjny; ładowanie oraz zmiany konfiguracji ograniczają porównywalność. Do oceny poprawy potrzebne są rzeczywiste noce na CPH2709 i Watch 2R. Testy JVM i build nie potwierdzają zachowania OxygenOS ani regularności pobrań.

### Wycofanie eksperymentu

Wyłączenie **Żądania obserwacji** zatrzymuje obserwację, ale zachowuje asocjację i może zachować część przywilejów. Aby wrócić do wariantu bez CDM, wybierz **Usuń wybrane powiązanie aplikacji** i potwierdź operację dla własnych asocjacji MohgWatch. Odśwież listę oraz porównaj wyjątki baterii i danych z zapisanym stanem początkowym. Jeśli się zmieniły, przywróć je ręcznie w systemowych ustawieniach aplikacji. MohgWatch nie obiecuje cichego przywrócenia wszystkich polityk producenta.

Ograniczenie to wynika m.in. z mechanizmu wyjątków w [AOSP Android 16](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r1/services/companion/java/com/android/server/companion/CompanionExemptionProcessor.java). Nowe API obecności jest opisane w [dokumentacji CompanionDeviceService](https://developer.android.com/reference/android/companion/CompanionDeviceService). Lokalna konfiguracja CDM i sesje pomiarowe nie są dołączane do przenośnego eksportu ustawień ani wysyłane na Wear.

## Known watch-face packaging limitation
The legacy WFF faces are bundled alongside executable Wear code. Android Lint reports `WatchFaceFormatDeclaresHasNoCode` and `WatchFaceFormatMissingVersion`. Proper WFF delivery needs a separate resource-only package; setting `hasCode=false` on the current Wear application would disable its services. The app, tiles and complication providers retain their current package.

---
Built with Jetpack Compose.

Wersja aplikacji: v1.7
