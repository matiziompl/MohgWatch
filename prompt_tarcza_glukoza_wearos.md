# Prompt do Claude Opus 4.6 — tarcza WearOS z odczytem glukozy (LibreLinkUp)

Skopiuj poniższy tekst i wklej go jako wiadomość do Claude Opus 4.6.

---

## PROMPT

Jesteś doświadczonym programistą Android/Kotlin, specjalizującym się w Wear OS (Watch Face Format, Jetpack Compose for Wear OS, Data Layer API) oraz w integracjach z nieoficjalnymi API zdrowotnymi. Pomóż mi zaprojektować i zaimplementować od podstaw aplikację o nazwie **MoghWatch** na Wear OS + Android.

### Kontekst sprzętowy
- Zegarek: **OnePlus Watch 2R** (Wear OS, Snapdragon W5, najprawdopodobniej Wear OS 4/5)
- Telefon: **OnePlus Nord 5** na **OxygenOS 16** (Android, wersję API sprawdź/założyć najnowszą stabilną)
- Sensor: **FreeStyle Libre 2**, dane odczytywane przez oficjalną apkę **LibreLink**, a następnie udostępniane do konta **LibreLinkUp** (funkcja "opiekuna"/"follower")

### Cel projektu
Chcę stworzyć **własną tarczę zegarka (watch face) na Wear OS**, którą będę mógł:
1. Swobodnie modyfikować (kolory, layout, czcionki, dodatkowe komplikacje) — czyli chcę mieć pełen dostęp do kodu źródłowego i architektury, a nie gotowe rozwiązanie no-code.
2. Wybierać spośród kilku wbudowanych, gotowych **presetów wyglądu** (np. minimalistyczny, sportowy, cyfrowy/analogowy hybrydowy, night-mode/AMOLED-friendly).
3. Najważniejsza funkcja: tarcza ma **na bieżąco wyświetlać mój aktualny poziom cukru (glukozy)** pobierany z konta LibreLinkUp, wraz z:
   - aktualną wartością (mg/dL lub mmol/L — do wyboru w ustawieniach),
   - strzałką/wskaźnikiem trendu (rosnący/malejący/stabilny — LibreLinkUp zwraca `TrendArrow`),
   - kolorystycznym oznaczeniem zakresu (np. zielony = w normie, żółty = ostrzeżenie, czerwony = hipo/hiperglikemia — z progami konfigurowalnymi przez użytkownika),
   - informacją "ile minut temu" był ostatni odczyt (żeby wiedzieć, czy dane nie są nieaktualne, np. gdy sensor stracił łączność),
   - opcjonalnie: mini-wykres ostatnich odczytów (sparkline).

### Wymagania architektoniczne
Chcę, żeby to działało **dokładnie na wzór aplikacji WatchGlucose** (istniejąca apka na Wear OS integrująca się z LibreLinkUp) — czyli dwie osobne aplikacje w jednym projekcie (moduły `phone-app` + `wear-app`), z podziałem obowiązków:

1. **Aplikacja na telefonie** (OnePlus Nord 5) — pełni rolę **ekranu konfiguracyjnego/onboardingowego**, nie musi utrzymywać stałego połączenia z API:
   - wygodny formularz logowania do LibreLinkUp (login, hasło, wybór regionu/serwera — `api-eu.libreview.io`, `api-us.libreview.io` itd., bo na telefonie łatwiej wpisać dane niż na zegarku),
   - po udanym logowaniu przesyła dane uwierzytelniające/token sesji oraz ustawienia (jednostki mg/dL vs mmol/L, progi alarmowe, wybrany preset tarczy) do zegarka przez **Wearable Data Layer API** (`DataClient`),
   - dalej może służyć jako zapasowy ekran ustawień, ale **nie jest wymagana do bieżącego działania** — po skonfigurowaniu może być zamknięta.
2. **Aplikacja + tarcza na zegarku (Wear OS)** — to ona wykonuje całą bieżącą pracę, tak jak w WatchGlucose:
   - samodzielnie, cyklicznie (np. co 1–5 min) odpytuje LibreLinkUp API o najnowszy odczyt, korzystając z połączenia sieciowego dostępnego na zegarku (własne Wi-Fi zegarka LUB automatyczne udostępnianie internetu telefonu przez Bluetooth, które Wear OS obsługuje transparentnie dla aplikacji — nie trzeba tego ręcznie proxować przez telefon),
   - przechowuje token/sesję bezpiecznie lokalnie na zegarku po otrzymaniu ich z telefonu (żeby nie logować się od nowa przy każdym uruchomieniu),
   - udostępnia dane przez **własnego dostawcę komplikacji (Complication Data Source Service)** — tak by dowolna tarcza (moja własna lub inna zainstalowana) mogła pokazywać aktualny poziom cukru, trend i status jako komplikację,
   - udostępnia **Tile (kafelek)** z 12-godzinną historią odczytów, dostępny przesunięciem palcem w lewo od tarczy — dokładnie jak w WatchGlucose,
   - zawiera moją własną tarczę (watch face) zbudowaną w oparciu o **Watch Face Format (WFF, deklaratywny XML/JSON)** LUB **AndroidX Wear Watchface library (Canvas-based, Kotlin)** — przeanalizuj oba podejścia i zarekomenduj, które lepiej pasuje do wymogu "łatwej dalszej modyfikacji przeze mnie" oraz obsługi wielu presetów,
   - obsługuje tryb ambient (always-on display) z energooszczędnym renderowaniem,
   - ma edytor presetów/kolorów dostępny przez długie przytrzymanie na tarczy (Watch Face Editor).

Zaznacz jasno w odpowiedzi, jak dokładnie Wear OS zapewnia zegarkowi dostęp do internetu bez własnego Wi-Fi (przez sparowany Bluetooth z telefonem) i kiedy warto/trzeba dodatkowo poprosić o uprawnienie `ACCESS_NETWORK_STATE`/`INTERNET` oraz `foreground service` (żeby system nie usypiał okresowego odpytywania API w tle).

### Integracja z LibreLinkUp API
- API jest **nieoficjalne i zreverse-engineerowane** przez społeczność (m.in. projekty takie jak `libre-link-up-api-client`, `nightscout-librelink-up`) — potraktuj je jako punkt odniesienia co do struktury requestów/odpowiedzi, ale nie zakładaj, że jest stabilne; dodaj solidną obsługę błędów, retry i informowanie użytkownika o wygaśnięciu sesji/tokenu.
- Zaimplementuj bezpieczne przechowywanie danych logowania (Android Keystore / EncryptedSharedPreferences), nie trzymaj hasła w plaintext.
- Uwzględnij, że LibreLinkUp wymaga zaakceptowania zaproszenia jako "follower" na koncie głównym LibreLink — wspomnij o tym w instrukcji konfiguracji dla użytkownika.

### Czego oczekuję od Ciebie w odpowiedzi
1. Krótkiej analizy: **Watch Face Format vs AndroidX Canvas** dla mojego przypadku użycia — rekomendacja z uzasadnieniem.
2. Struktury projektu (moduły: `phone-app`, `wear-app`, ewentualnie `shared` na współdzielone modele danych).
3. Pełnego, działającego kodu (Kotlin) dla:
   - klienta API LibreLinkUp (logowanie, pobranie ostatniego odczytu, mapowanie na model danych) — używanego przez aplikację na zegarku,
   - przesyłania danych logowania/ustawień telefon → zegarek przez Data Layer API (jednorazowo, przy konfiguracji),
   - cyklicznego pobierania danych **bezpośrednio na zegarku** (WorkManager/foreground service dopasowany do ograniczeń Wear OS),
   - dostawcy komplikacji (`ComplicationDataSourceService`) pokazującego aktualny poziom cukru/trend,
   - Tile z 12-godzinną historią odczytów (wykres),
   - renderowania własnej tarczy z co najmniej 3 presetami wizualnymi oraz systemem kolorów wg progów glukozy,
   - prostego ekranu logowania/ustawień na telefonie.
4. Instrukcji konfiguracji projektu w Android Studio (wersje SDK, zależności Gradle, uprawnienia w `AndroidManifest.xml`, w tym `INTERNET`, `FOREGROUND_SERVICE`, itp.).
5. Wskazówek dot. testowania na fizycznych urządzeniach (OnePlus Watch 2R + Nord 5), w tym parowania przez Wear OS app i debugowania ADB przez Wi-Fi.
6. Krótkiej listy ograniczeń/zastrzeżeń (np. brak gwarancji stabilności nieoficjalnego API, kwestie opóźnień danych, brak odpowiedzialności medycznej — to narzędzie pomocnicze, nie zamiennik odczytu z oficjalnej apki w sytuacjach krytycznych).

Pytaj mnie o doprecyzowanie, jeśli czegoś brakuje, zanim zaczniesz pisać duże partie kodu — ale możesz od razu zaproponować rozsądne założenia domyślne (np. mg/dL jako domyślną jednostkę, progi 70–180 mg/dL jako "norma") i iść dalej.

---

### Wskazówka dla Ciebie (poza samym promptem)
Jeśli Opus zapyta o Twoje konto LibreLinkUp (login/hasło) — **nigdy nie wklejaj prawdziwych danych logowania do czatu**. Podawaj je dopiero bezpośrednio w kodzie/lokalnie na swoim komputerze, nigdy w rozmowie z modelem.
