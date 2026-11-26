# MohgWatch 🩸⌚

MohgWatch to rozbudowana, nowoczesna aplikacja na telefon i zegarek z Wear OS dla użytkowników FreeStyle Libre, korzystająca z API LibreLinkUp.

## Funkcje ✨
- **Integracja z Wear OS**: kafelek zegarka przyjazny ekranom AMOLED pokazuje ostatnią historię glukozy, własne progi i bieżący poziom glukozy. Lokalna historia obejmuje maksymalnie 24 godziny.
- **Konfigurowalny interfejs telefonu**: do wyboru kilka kolorowych motywów, m.in. Monochromatyczny, AMOLED Black i Teal. Aplikacja na Wear i kafelek miejsc wkłucia mają stały, ciemny wygląd.
- **Ustawienia na żywo**: progi glukozy, preferencje alarmów i język synchronizują się z telefonu na zegarek. Ustawienia wyglądu dotyczą tylko telefonu.
- **Alarmy glukozy**: dźwięk alarmu przy glukozie poza progami, zależnie od ustawień powiadomień Androida i trybu Nie przeszkadzać.
- **Wibracje progowe**: alarmy niskiej i wysokiej glukozy korzystają z progów z ustawień **Glukoza i progi**. Opcjonalna wibracja powrotu sygnalizuje powrót do zakresu; zmiany strzałki trendu nie wywołują wibracji. Dotychczas wybrane wzorce zostają zachowane.
- **Diagnostyka**: **Ustawienia → Logi i diagnostyka** udostępnia krótki raport do skopiowania oraz pełne logi udostępniane przez Androida. Można filtrować po czasie, kategorii zdarzeń, błędach oraz identyfikatorze cyklu lub testu nocnego. Lokalna diagnostyka przetrwa restart procesu, z limitem 7 dni / 8 MiB.
- **Wyciszanie alarmów**: przycisk nad „Przerwij wszystkie alerty” na ekranie Status wycisza alarmy niskiego i wysokiego cukru na telefonie i zegarku na 1–30 minut. Czas ustawia się suwakiem w **Glukoza i progi**; wartość 0 ukrywa przycisk. Wyciszenie można anulować tym samym przyciskiem. Przycisk „Przerwij wszystkie alerty” można ukryć w **Ustawienia → Wygląd → Status**, gdzie jest też opcja ukrycia nagłówka. Jeśli po jego zakończeniu cukier nadal jest poza zakresem, alarm włącza się przy następnym odczycie. Alarmy rozłączenia i braku danych nie są wyciszane.
- **Gotowość na noc**: aplikacja sprawdza wyjątek baterii, dokładne wybudzenia i powiadomienia, a na telefonach OnePlus/OPPO/realme prowadzi ręczną listę ustawień producenta. Wykrywa też dłuższy brak internetu w nocy i pokazuje ostrzeżenie na ekranie Status.
- **Świeżość danych**: limit wieku pomiaru domyślnie wynosi 10 minut i można go ustawić w zakresie od 1 do 60 minut w **Powiadomienia → Brak danych i rozłączenia**. Przeterminowane komplikacje pokazują `- -`; lokalny alarm na zegarku ostrzega, gdy aktualizacje ustają.
- **Wielojęzyczność**: obsługa języka angielskiego i polskiego.

## Nawigacja
Dolny pasek zawiera **Status**, **Powiadomienia** i **Ustawienia**. Na ekranie Status przeciągnięcie w dół od razu pobiera nowy odczyt, gdy synchronizacja jest włączona; gest nigdy jej nie włącza. Gdy trwa pomiar nocny albo telefon nie jest gotowy na noc, nad odczytem pojawia się pasek prowadzący do zakończenia pomiaru lub do ustawień. Powiadomienia otwierają sześć osobnych stron alarmów.

Ustawienia są podzielone na grupy: **Cukrzyca** (Glukoza i progi, Dawki i miejsca wkłuć), **Działanie w tle** (Synchronizacja i bateria), **Wygląd** (Telefon, Widżety), **Konto** (Profil) i **Diagnostyka** (Diagnostyka i testy). Ekran **Synchronizacja i bateria** zawiera kartę „Gotowość na noc”, interwał, przycisk „Synchronizuj teraz” oraz zwinięte ustawienia zaawansowane ze stałym wybudzeniem. Ekran **Diagnostyka i testy** zaczyna się od pomiaru nocnego, a dalej prowadzi do logów, testów dźwięków i zegarka oraz symulacji pomiarów.

Profil eksportuje i importuje ustawienia jako wersjonowany plik JSON bez danych logowania. Import sprawdza plik przed potwierdzeniem i zastąpieniem lokalnych ustawień. Ekran logowania pokazuje logo aplikacji. Stałe powiadomienie na telefonie rozróżnia stany „Na ręku”, „Zdjęty” i „Brak danych”; brakujące, nieprawidłowe lub nieaktualne odczyty nadgarstka są pokazywane jako „Brak danych”. Symulacja pomiarów pozwala wybrać glukozę i trend oraz ukryć lub przywrócić przycisk Demo. Strony dźwięków i wibracji prowadzą bezpośrednio do odpowiednich testów. Aktualizacje aplikacji zachowują zapisane ustawienia.

Wycofane ustawienia alarmów i trendu, progi kolorów komplikacji, nieużywane opcje czcionki i strzałek zegarka, nieaktywny wybór presetów oraz synchronizacja wyglądu zegarka nie są już używane ani eksportowane. Dotychczasowe eksporty ustawień w wersji 1 nadal można zaimportować; wycofane pola są pomijane.

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
Synchronizację w nocy utrzymuje usługa pierwszoplanowa z dokładnymi wybudzeniami. Potrzebne są wyjątek od optymalizacji baterii, zgoda na dokładne wybudzenia i powiadomienia; ich stan i przyciski naprawy są w karcie **Ustawienia → Synchronizacja i bateria → Gotowość na noc**. Stałe wybudzenie (w sekcji „Zaawansowane”) nie jest potrzebne i zwiększa zużycie baterii.

### OnePlus, OPPO i realme
Opcja **Ustawienia → Bateria → Więcej ustawień → Optymalizacja gotowości podczas snu** wyłącza w godzinach snu Wi-Fi i dane komórkowe, mimo opisu sugerującego jedynie opóźnienie powiadomień. Musi być wyłączona. Dla MohgWatch trzeba też wybrać **Zezwalaj na aktywność w tle** w ustawieniach baterii aplikacji. Aplikacja nie może odczytać tych opcji, więc karta „Gotowość na noc” pokazuje je jako ręczną listę do zaznaczenia z odnośnikami do ustawień systemu. Po aktualizacji systemu trzeba je sprawdzić ponownie.

Jeśli w nocy przy wyłączonym ekranie internet zniknie na co najmniej 10 minut, MohgWatch zapisze to w logach jako `NIGHT_OUTAGE` i pokaże ostrzeżenie na ekranie Status oraz w karcie „Gotowość na noc”. Zaznaczenia listy pozostają bez zmian, bo przyczyną może być też np. wyłączony router. Ostrzeżenie zamyka przycisk „Rozumiem”. Przerwa jest wykrywana także wtedy, gdy Android zatrzymał aplikację w trakcie braku internetu.

Tryb oszczędzania energii włączany automatycznie przy niskim poziomie baterii także może ograniczać pracę w tle, więc nie warto zaczynać nocy z prawie rozładowanym telefonem.

## Pomiar nocny i logi
Pomiar nocny uruchamia się w **Ustawienia → Diagnostyka i testy**. Zakończ go zaraz po przebudzeniu, najlepiej paskiem na ekranie Status, zanim zaczniesz korzystać z telefonu. Zmiana synchronizacji, interwału, stałego wybudzenia lub limitu świeżości przerywa bieżący pomiar. Restart procesu w tym samym rozruchu pozwala kontynuować, ale raport ostrożnie oznacza możliwą niekompletność logów. Restart telefonu przerywa sesję; czasów monotonicznych z różnych rozruchów nie wolno porównywać.

**Logi i diagnostyka** zachowują istniejące okresy 24 h / 72 h / 7 dni oraz umożliwiają własny zakres, kategorię sieć/API/synchronizacja, same błędy i identyfikatory cyklu lub pomiaru. Daty można podać jako `2026-10-25 01:00` w lokalnej strefie albo ISO z offsetem, np. `2026-10-25T02:30:00+02:00`. Przy zmianie czasu offset usuwa niejednoznaczność lokalnej godziny.

Eksportuj **pełne logi**, nie tylko krótki raport. Raport podaje straty kolejki, retencję i obcięcie. Liczby planowanych/dostarczonych alarmów i timerów są odrębne od liczby wykonanych cykli. HTTP 401 odzyskane przez ponowne logowanie lub błąd opcjonalnej historii nie oznaczają automatycznie utraty bieżącego pomiaru. Potwierdzenie operacji Data Layer nie jest potwierdzeniem odbioru na fizycznym zegarku.

Procent baterii jest orientacyjny; ładowanie oraz zmiany konfiguracji ograniczają porównywalność. Do oceny zmian potrzebne są rzeczywiste noce na telefonie i zegarku. Testy JVM i build nie potwierdzają zachowania OxygenOS ani regularności pobrań.

Sesje pomiarowe nie są dołączane do przenośnego eksportu ustawień ani wysyłane na Wear. Eksperyment z Companion Device Manager został usunięty w v1.9: nocne testy pokazały, że nie poprawiał synchronizacji, a po rozłączeniu zegarka odbierał aplikacji wyjątek baterii.

---
Zbudowano w Jetpack Compose.

Wersja aplikacji: v1.11
