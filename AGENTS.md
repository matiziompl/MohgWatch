# MohgWatch — zasady pracy

- Zachowuj istniejącą architekturę i niezacommitowane zmiany. Stosuj KISS/YAGNI.
- Nie wykonuj commitów, publikacji ani instalacji na urządzeniach bez osobnego polecenia użytkownika. Nie używaj ADB do urządzeń.
- Nie uruchamiaj przeglądarek ani Playwright. Plany prowadź w rozmowie, bez plików planów/specyfikacji.
- Kompilacje Gradle uruchamiaj kolejno; przed zakończeniem wykonaj odpowiednie testy i build obu aplikacji debug.

## Katalog pakowany przez użytkownika

- Użytkownik przesyła projekt poleceniem `~/scripts/localsend.sh MohgWatch`, które pakuje `~/workspace/MohgWatch` — w środowisku agenta jest to `/workspace/MohgWatch`.
- Przed zakończeniem pracy aktualne źródła, nowe pliki i uzgodnione usunięcia muszą znaleźć się w tym katalogu. Same niezacommitowane zmiany w osobnym worktree nie są przekazywane przez ten skrypt.
- Przy przenoszeniu zmian zachowaj inne zmiany użytkownika, metadane Git i lokalną konfigurację. Nie zastępuj katalogu projektu ani jego `.git`; nie wymagaj commita do przekazania zmian.
- Końcowe testy i build wykonuj w katalogu pakowanym przez użytkownika. Nie uruchamiaj LocalSend ani wysyłania bez osobnego polecenia.

## Wersjonowanie

- Wersje prezentowane użytkownikowi zapisuj wszędzie z prefiksem `v`, np. **v1.7** — w UI, README, pozostałej dokumentacji, opisach zmian i odpowiedziach. Nie używaj w tych opisach samego `1.7` ani podwójnego prefiksu `vv1.7`.
- Techniczna wartość `mohgwatch.versionName` w `gradle.properties` pozostaje bez prefiksu, np. `1.7`; przy prezentowaniu jej użytkownikowi dodawaj dokładnie jedno `v`.
- Numerowanie rozpoczyna się od **v1.2**. Każdy kolejny zakończony zestaw zmian zlecony przez użytkownika zwiększa wersję: **v1.3, v1.4, v1.5…**, dopóki użytkownik nie zmieni tej zasady.
- Wersję zwiększaj raz na zestaw zmian, nie osobno za każdy plik, test, build lub poprawkę w ramach tego samego zadania. Po v1.9 następuje v1.10.
- Wspólnym źródłem wersji telefonu i zegarka są `mohgwatch.versionName` oraz `mohgwatch.versionCode` w `gradle.properties`. Przy zmianie wersji zwiększ także całkowity `versionCode` o 1.
- Na samym końcu `README.md` utrzymuj aktualną linię `Wersja aplikacji: vX.Y`.
- W UI numer pochodzi z `BuildConfig.VERSION_NAME`: na dole głównych Ustawień telefonu i małym drukiem pod statusem nadgarstka na zegarku. Nie wpisuj wersji na sztywno w Kotlinie.
