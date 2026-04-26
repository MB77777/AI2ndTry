Aplikacja będzie posiadała użytkowników. Użytkownik będzie miał następująće atrybuty:
    - imie, nazwisko, login, haslo, id, data urodzenia, data utworzenia



Aplikacja będzie posiadała konta. Konta przypisane są do użytkowników i mają następujące atrybuty
    - id, numer, data utworzenia, waluta, stan, data ostatniej operacji. 

    Numery kont powinny być 5 cyfrowe.
    Możliwe są następujące waluty: PLN, EUR, USD, CHF, NOK

    Stan konta nie może spaść poniżej 0. W przypadku próby operacji zmniejszającej stan poniżej 0 powinien być
    zgłaszany błąd operacji.


Aplikacja będzie posiadała operacje. Operacja będzie miała następujące atryuty:
    - id, typ, konto z, konto na, data operacji co do sekudny, id użytkownika inicjującego, status operacji 

Statusy operacji: Zarejestrowana, Zrealizowana, Anulowana.

Są następujące typy operacji:
Wplata - konto z jest null, konto na wypełnione. 
Wypłata - odwrotnie do wpłaty
Transakcja - pomiędzy dwoma kontami o tej samej walucie


Dla celów testowych powinien być stworzony obiekt, który generuje losowo operacje. Powinny ona wpadać do systemu
w odstępach od 2 do 20 sekund. Wszystkie dane operajci powinny być losowane. Jaki użytkownik zainicjował,
jaka kwota, z jakiego konta na jakie. Operacje mogą być tylko na kontach instniejących w systemie.

Na początku system powinien wygenerować 20 użytkowników. Każdy użytkownik powinien posiadać polskie imię, 
i imiona nie powinny się powtarzać. Nazwiska powinny być też losowe i polskie.

Dla każdego użytkownika system powinien wylosować od 1 do 3 kont.

DLa każdego z kont powinien zostać wylosowany stan konta w zakresie od 0 do 100 000.

Pierwotnie lista operacji powinna być pusta. 

Id powinny być na typie Long.