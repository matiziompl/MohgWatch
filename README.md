# MohgWatch 🩸⌚

MohgWatch to rozbudowana, nowoczesna aplikacja towarzysząca i tarcza Wear OS dla użytkowników FreeStyle Libre, korzystająca z API LibreLinkUp.

## Funkcje ✨
- **Integracja z Wear OS**: kafelek zegarka przyjazny ekranom AMOLED pokazuje ostatnią historię glukozy, własne progi i bieżący poziom glukozy. Lokalna historia obejmuje maksymalnie 24 godziny.
- **Konfigurowalny interfejs telefonu**: do wyboru kilka kolorowych motywów, m.in. Monochromatyczny, AMOLED Black i Teal. Aplikacja na Wear i kafelek miejsc wkłucia mają stały, ciemny wygląd.
- **Ustawienia na żywo**: progi glukozy, preferencje alarmów i język synchronizują się z telefonu na zegarek. Ustawienia wyglądu dotyczą tylko telefonu.
- **Alarmy glukozy**: dźwięk alarmu przy glukozie poza progami, zależnie od ustawień powiadomień Androida i trybu Nie przeszkadzać.
- **Wibracje progowe**: alarmy niskiej i wysokiej glukozy korzystają z progów z ustawień **Glukoza i progi**. Opcjonalna wibracja powrotu sygnalizuje powrót do zakresu; zmiany strzałki trendu nie wywołują wibracji. Dotychczas wybrane wzorce zostają zachowane.
- **Diagnostyka**: **Ustawienia → Logi i diagnostyka** udostępnia krótki raport do skopiowania oraz pełne logi udostępniane przez Androida. Można filtrować po czasie, kategorii zdarzeń, błędach oraz identyfikatorze cyklu lub testu nocnego. Lokalna diagnostyka przetrwa restart procesu, z limitem 7 dni / 8 MiB.
- **Eksperymentalny CDM**: na Androidzie 16 MohgWatch może powiązać się z już sparowanym zegarkiem i poprosić o obserwację obecności urządzenia. Funkcja jest opcjonalna, nie wymaga roota ani Shizuku i nie gwarantuje synchronizacji co pięć minut.
- **Świeżość danych**: limit wieku pomiaru domyślnie wynosi 10 minut i można go ustawić w zakresie od 1 do 60 minut w **Powiadomienia → Brak danych i rozłączenia**. Przeterminowane komplikacje pokazują `- -`; lokalny alarm na zegarku ostrzega, gdy aktualizacje ustają.
- **Wielojęzyczność**: obsługa języka angielskiego i polskiego.

## Nawigacja
Dolny pasek zawiera **Status**, **Powiadomienia** i **Ustawienia**. Powiadomienia otwierają sześć osobnych stron alarmów. Ustawienia obejmują progi glukozy, dawki, synchronizację, pracę w tle, wygląd telefonu i widżetów, Profil, diagnostykę oraz trzystronicowe menu testów. Profil eksportuje i importuje ustawienia jako wersjonowany plik JSON bez danych logowania. Import sprawdza plik przed potwierdzeniem i zastąpieniem lokalnych ustawień. Ekran logowania pokazuje logo aplikacji. Stałe powiadomienie na telefonie rozróżnia stany „Na ręku”, „Zdjęty” i „Brak danych”; brakujące, nieprawidłowe lub nieaktualne odczyty nadgarstka są pokazywane jako „Brak danych”. Symulacja pomiarów pozwala wybrać glukozę i trend oraz ukryć lub przywrócić przycisk Demo. Strony dźwięków i wibracji prowadzą bezpośrednio do odpowiednich testów. Aktualizacje aplikacji zachowują zapisane ustawienia.

Wycofane ustawienia alarmów i trendu, nieużywane opcje czcionki i strzałek zegarka, nieaktywny wybór presetów oraz synchronizacja wyglądu zegarka nie są już używane ani eksportowane. Dotychczasowe eksporty ustawień w wersji 1 nadal można zaimportować; wycofane pola są pomijane. Tarcze WFF wybierane w systemie pozostają dostępne.

## Architektura
- **Technologie**: w 100% Kotlin, Jetpack Compose, Wear Compose, Coroutines, Flow.
- **Łączność**: Wearable Data Layer API zapewnia natychmiastowe współdzielenie stanu między telefonem a zegarkiem.
- **API**: bezpośrednie połączenie z LibreLinkUp z automatycznym odnawianiem tokenów i obsługą domen regionalnych.

## Jak używać
1. Zainstaluj aplikację na telefonie i na zegarku z Wear OS.
2. Wpisz na telefonie dane logowania do LibreLinkUp.
3. Dostosuj progi, wygląd telefonu i preferencje alarmów.
4. Dane będą automatycznie synchronizowane z kafelkiem na zegarku.

## Prywatność i bezpieczeństwo
Dane logowania do LibreLinkUp są szyfrowane lokalnie kluczem z Android Keystore i używane do automatycznego logowania. Nie trafiają do kopii zapasowych, więc po ponownej instalacji lub zmianie telefonu może być potrzebne jednorazowe zalogowanie. Twoje dane nie są przekazywane nikomu poza skonfigurowaną usługą LibreLinkUp i sparowanym zegarkiem.

## Monitorowanie w nocy
Aby MohgWatch miał dostęp do sieci w trybie Doze, trzeba wyłączyć dla niego optymalizację baterii. **Ustawienia → Bateria i praca w tle** pokazują stan wyjątku baterii, dokładnych alarmów i powiadomień. Stałe wybudzenie utrzymuje dostępność procesora, ale nie daje dostępu do sieci. Ograniczenia energii wprowadzane przez producenta telefonu mogą nadal wymagać ustawień specyficznych dla urządzenia.

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

## Znane ograniczenie pakowania tarcz zegarka
Starsze tarcze WFF są dołączone do pakietu razem z wykonywalnym kodem Wear. Android Lint zgłasza `WatchFaceFormatDeclaresHasNoCode` i `WatchFaceFormatMissingVersion`. Prawidłowe dostarczanie WFF wymaga osobnego pakietu zawierającego wyłącznie zasoby; ustawienie `hasCode=false` w obecnej aplikacji Wear wyłączyłoby jej usługi. Aplikacja, kafelki i dostawcy komplikacji pozostają w obecnym pakiecie.

---
Zbudowano w Jetpack Compose.

Wersja aplikacji: v1.7
