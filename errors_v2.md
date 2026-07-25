# Aktualne Błędy Kompilacji (Wersja 2)

## Problemy Systemowe (I/O)
Występują błędy dostępu do plików w folderach `build`, co uniemożliwia poprawne przeprowadzenie czyszczenia (`clean`) i budowania projektu. Prawdopodobnie jakiś proces (np. Android Studio, Kotlin Daemon) blokuje te pliki.

### Moduł :app
- **Błąd:** `java.io.IOException: Unable to delete directory`
- **Lokalizacja:** `app\build` oraz `app\build\intermediates`
- **Szczegóły:** Proces nie może usunąć plików tymczasowych, co blokuje zadania `clean` oraz `mergeDebugResources`.

### Moduł :wear
- **Błąd:** `java.io.IOException: Unable to delete directory` / `AccessDeniedException`
- **Lokalizacja:** `wear\build`
- **Szczegóły:** Podobny problem z uprawnieniami/blokadą plików jak w module `:app`.

### Moduł :core
- **Błąd:** `java.io.IOException: Unable to delete directory`
- **Lokalizacja:** `core\build`

## Poprzednio zidentyfikowane błędy (obecnie maskowane przez błędy I/O)
Zanim wystąpiły problemy z dostępem do plików, raportowano brak ikony startowej:

- **Błąd:** `AAPT: error: resource mipmap/ic_launcher not found.`
- **Lokalizacja:** `app/src/main/AndroidManifest.xml:12` oraz `wear/src/main/AndroidManifest.xml:8`

---
> [!TIP]
> Aby rozwiązać błędy I/O, spróbuj zamknąć Android Studio i zabić procesy `java.exe` (Gradle/Kotlin Deamons) w Menedżerze Zadań, a następnie uruchom budowanie ponownie.
