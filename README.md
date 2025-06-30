# File_transfer

## Opis Projektu

Testowa aplikacja CNApp oparta na protokole SSMMP. Interfejsem użytkownika jest klient wiersza poleceń (CLI), który komunikuje się z systemem mikrousług poprzez API Gateway. Żądania i odpowiedzi przesyłane są w formie obiektów klasy String, a cała architektura jest bezstanowa.

## Rola Komponentu

Ta mikrousługa zarządza procesem transferu plików, umożliwiając ich przesyłanie na serwer i pobieranie z serwera. Obsługuje żądania `send_file_request` oraz `get_file_request`, dzieląc pliki na pakiety i zapisując je lub odczytując z dysku twardego.

## Konfiguracja

Ten komponent wymaga następujących zmiennych środowiskowych w pliku `.env`:

FILE_TRANSFER_MICROSERVICE_PORT=


## Uruchomienie

Serwis można uruchomić, wykonując główną metodę `main` w klasie `File_transfer.java`