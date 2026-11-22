# MohgWatch — zasady pracy

- Zachowuj istniejącą architekturę i niezacommitowane zmiany. Stosuj KISS/YAGNI.
- Nie wykonuj commitów, publikacji ani instalacji na urządzeniach bez osobnego polecenia użytkownika. Nie używaj ADB do urządzeń.
- Nie uruchamiaj przeglądarek ani Playwright. Plany prowadź w rozmowie, bez plików planów/specyfikacji.
- Kompilacje Gradle uruchamiaj kolejno; przed zakończeniem wykonaj odpowiednie testy i build obu aplikacji debug.
- Po każdym zestawie zmian podaj użytkownikowi gotowe komendy Git (`git add`, `git commit`, `git push`) do wykonania na Windows w `C:\Containers\ai-workspace\workspace\MohgWatch`. Nie wykonuj commitów ani pushy z kontenera.

## Wersjonowanie

- Wersje prezentowane użytkownikowi zapisuj wszędzie z prefiksem `v`, np. **v1.7** — w UI, README, pozostałej dokumentacji, opisach zmian i odpowiedziach. Nie używaj w tych opisach samego `1.7` ani podwójnego prefiksu `vv1.7`.
- Techniczna wartość `mohgwatch.versionName` w `gradle.properties` pozostaje bez prefiksu, np. `1.7`; przy prezentowaniu jej użytkownikowi dodawaj dokładnie jedno `v`.
- Numerowanie rozpoczyna się od **v1.2**. Każdy kolejny zakończony zestaw zmian zlecony przez użytkownika zwiększa wersję: **v1.3, v1.4, v1.5…**, dopóki użytkownik nie zmieni tej zasady.
- Wersję zwiększaj raz na zestaw zmian, nie osobno za każdy plik, test, build lub poprawkę w ramach tego samego zadania. Po v1.9 następuje v1.10.
- Wspólnym źródłem wersji telefonu i zegarka są `mohgwatch.versionName` oraz `mohgwatch.versionCode` w `gradle.properties`. Przy zmianie wersji zwiększ także całkowity `versionCode` o 1.
- Na samym końcu `README.md` utrzymuj aktualną linię `Wersja aplikacji: vX.Y`.
- W UI numer pochodzi z `BuildConfig.VERSION_NAME`: na dole głównych Ustawień telefonu i małym drukiem pod statusem nadgarstka na zegarku. Nie wpisuj wersji na sztywno w Kotlinie.
