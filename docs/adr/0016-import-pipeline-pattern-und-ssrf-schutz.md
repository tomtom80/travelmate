# ADR-0016: Import-Pipeline-Pattern fuer externe Datenquellen mit SSRF-Schutzmassnahmen

## Status

Accepted

## Context

Mit Iteration 10 fuehrt Travelmate erstmals serverseitiges Abrufen externer URLs ein. Das Headline-Feature ist der **Accommodation URL Import** im Trips SCS: Nutzer geben die URL einer Ferienunterkunft (booking.com, Ferienhausmiete.de, Airbnb, FeWo-direkt) ein, und das System extrahiert automatisch Name, Adresse, Preis, Check-in/Check-out-Daten und Zimmerstruktur.

Parallel wird in Iteration 10 ein zweites Import-Feature eingefuehrt: **Kassenzettel-Scan (Receipt OCR)** im Expense SCS, bei dem ein fotografierter Kassenzettel per OCR analysiert wird.

**Decision Drivers**:

1. Beide Features teilen ein gemeinsames UX-Muster: Input -> Analyse -> Vorschau -> Edit -> Bestaetigung -> Speichern
2. Server-seitiges URL-Fetching oeffnet einen SSRF-Angriffsvektor (Server-Side Request Forgery)
3. Die bestehende Hexagonal Architecture muss eingehalten werden: Port im Domain-Layer, Adapter in `adapters/`
4. Import-Ergebnisse sind immer unzuverlaessig (Webscraping, OCR) -- der Nutzer muss korrigieren koennen
5. Zukuenftige Import-Features (z.B. Rezept-Import aus URL) sollen dasselbe Muster wiederverwenden
6. Kein Adapter fuer externe Datenquellen existiert bisher im Projekt

## Decision

### 1. Gemeinsames Import-Pipeline-Pattern

Alle Import-Features folgen einem einheitlichen UX- und Command-Flow:

```
Input --> Analyse --> Vorschau --> EDIT --> Bestaetigung --> Speichern
```

**Entscheidend**: Der Import gibt NIEMALS direkt gespeicherte Daten zurueck. Stattdessen wird ein vollstaendig editierbares, vorausgefuelltes Formular angezeigt. Alle extrahierten Felder sind Vorschlaege, die der Nutzer vor dem Speichern korrigieren kann.

Der Speichervorgang nutzt die bestehenden Commands und Aggregate-Methoden (z.B. `Accommodation.create()`, `Expense.addReceipt()`). Es werden keine neuen Domain Events oder Event-Contracts eingefuehrt.

### 2. Hexagonal Architecture: Import-Ports und -Adapter

Import-Funktionalitaet wird als **sekundaerer Port** (Driven Port) im Domain-Layer modelliert:

```
domain/accommodation/
  +-- AccommodationImportPort (Interface)
  +-- AccommodationImportResult (Record)
  +-- ImportedRoom (Record)

adapters/integration/
  +-- WebScrapingAccommodationImportAdapter (implementiert AccommodationImportPort)
  +-- HtmlFetcher (wiederverwendbar, SSRF-geschuetzt)
  +-- JsonLdExtractor
  +-- OpenGraphExtractor
```

Analoges Muster im Expense SCS:

```
domain/expense/
  +-- ReceiptScanPort (Interface)
  +-- ReceiptScanResult (Record)

adapters/ocr/
  +-- TesseractReceiptScanAdapter (implementiert ReceiptScanPort)
```

Die Port-Interfaces verwenden `@Profile("!test")` auf den Adapter-Implementierungen, sodass in Tests gemockt werden kann.

### 3. Extraktionsstrategie fuer URL-Import (Accommodation)

Die Datenextraktion aus HTML-Seiten folgt einer Fallback-Kette mit absteigender Zuverlaessigkeit:

1. **JSON-LD** (schema.org): Strukturierte Daten mit `@type` wie `LodgingBusiness`, `Hotel`, `House` -- hoechste Zuverlaessigkeit
2. **Open Graph Fallback**: `og:title`, `og:description`, `og:image` -- mittlere Zuverlaessigkeit
3. **HTML Heuristik Fallback**: `<title>`, `meta[name="description"]` -- niedrigste Zuverlaessigkeit

Technologie: **Jsoup** (Java HTML Parser) fuer HTML-Parsing, **Jackson** (bereits via Spring vorhanden) fuer JSON-LD-Parsing, **java.net.http.HttpClient** (Java 21 Built-in) fuer HTTP-Requests.

### 4. SSRF-Schutzmassnahmen fuer URL-Import

Da der Server beliebige vom Nutzer eingegebene URLs abruft, ist ein umfassender SSRF-Schutz erforderlich. Die Implementierung erfolgt in einer wiederverwendbaren `HtmlFetcher`-Komponente:

| Massnahme | Implementierung |
|-----------|----------------|
| **HTTPS-Only** | URL-Schema-Pruefung vor Fetch -- kein HTTP, kein FTP, kein File |
| **Private-IP-Blacklist** | RFC 1918 (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16), Loopback (127.0.0.0/8), Link-Local (169.254.0.0/16), IPv6 Loopback (::1) |
| **DNS-Rebinding-Schutz** | IP-Adresse nach DNS-Aufloesung pruefen (nicht nur Hostname), `InetAddress.getByName()` vor Connect |
| **Response-Size-Limit** | Maximal 5 MB, InputStream mit Limit lesen |
| **Timeout** | 10 Sekunden (Connect + Read), konfiguriert via `HttpClient.Builder` |
| **Redirect-Limit** | Maximal 3 Redirects, jede Redirect-URL wird erneut SSRF-validiert |
| **User-Agent** | `Travelmate/0.10.0` -- transparentes, nicht-anonymes Scraping |

### 5. Bild-Upload-Sicherheit fuer OCR (Kassenzettel-Scan)

| Massnahme | Implementierung |
|-----------|----------------|
| **Size-Limit** | Maximal 10 MB (`spring.servlet.multipart.max-file-size`) |
| **Content-Type-Pruefung** | Magic Bytes pruefen (nicht nur HTTP Content-Type Header), erlaubt: JPEG, PNG, HEIC |
| **Keine Persistierung** | Foto wird nur transient im Memory verarbeitet und nach OCR verworfen (Datenschutz, Speicherplatz) |
| **Kein Server-Rendering** | Bild wird nicht an Browser zurueckgesendet (kein XSS via SVG/HTML-in-Image) |

### 6. Command-Flow (technisch)

```
     Nutzer                   Controller              App-Service              Port/Adapter
       |                         |                        |                        |
       |  1. Input (URL/Foto)   |                        |                        |
       |------------------------>|                        |                        |
       |                         |  2. AnalyseCommand     |                        |
       |                         |----------------------->|                        |
       |                         |                        |  3. Port aufrufen      |
       |                         |                        |----------------------->|
       |                         |                        |                        |  4. Externe Daten
       |                         |                        |                        |  holen + parsen
       |                         |                        |<-----------------------|
       |                         |                        |  5. ImportResult       |
       |                         |<-----------------------|                        |
       |  6. Editierbares        |                        |                        |
       |  Formular               |                        |                        |
       |<------------------------|                        |                        |
       |  7. Korrigieren         |                        |                        |
       |  + Bestaetigen          |                        |                        |
       |------------------------>|                        |                        |
       |                         |  8. SaveCommand        |                        |
       |                         |----------------------->|                        |
       |                         |                        |  9. Aggregate          |
       |                         |                        |  create/update         |
```

Schritte 1-6 sind ein normaler GET/POST-Roundtrip ohne Aggregate-Zugriff. Erst Schritt 8 erzeugt oder mutiert ein Aggregate ueber die bestehenden Commands.

## Consequences

### Positiv

- **Wiederverwendbares Muster**: Import-Pipeline ist ein generisches Pattern, das fuer zukuenftige Features (Rezept-Import, etc.) direkt anwendbar ist
- **Hexagonal-konform**: Port-Abstraktion im Domain-Layer, austauschbare Adapter-Implementierungen
- **SSRF-geschuetzt**: Umfassende Massnahmen gegen Server-Side Request Forgery in einer wiederverwendbaren `HtmlFetcher`-Komponente
- **Nutzer behaelt Kontrolle**: Editierbares Formular faengt fehlerhafte Extraktion zuverlaessig auf -- Import ist Vorschlag, nicht Pflicht
- **Keine neuen Events**: Import nutzt bestehende Aggregate-Methoden und Event-Flows
- **Framework-freier Domain-Layer**: Port-Interfaces und Result-Records haben keine Spring-Abhaengigkeiten

### Negativ

- **Best-Effort-Extraktion**: Nicht jede Website liefert strukturierte Daten (JSON-LD); manche Portale blockieren Bot-Requests oder rendern clientseitig (SPA)
- **Wartungsaufwand**: Website-Strukturen aendern sich -- Extraktoren koennen ohne Vorwarnung brechen
- **Neue Abhaengigkeit**: Jsoup (HTML-Parser) als neue Maven-Dependency im Trips SCS
- **Kein Offline-Support**: URL-Import erfordert Internetzugang des Servers; OCR erfordert Tesseract-Installation (native Abhaengigkeit)

## Alternatives

### Option 1: Manuelles Copy-Paste (Status quo)

- **Vorteile**: Keine SSRF-Risiken, keine externen Abhaengigkeiten, einfachste Loesung
- **Nachteile**: Hoher manueller Aufwand fuer Nutzer, insbesondere bei Unterkunftsdetails (Name, Adresse, Preis, Zimmer muessen einzeln uebertragen werden); widerspricht dem Ziel der UX-Verbesserung

### Option 2: Client-seitiges Scraping (Browser Extension)

- **Vorteile**: Kein SSRF-Risiko, Zugriff auf gerenderte Seiten (nach JavaScript-Ausfuehrung)
- **Nachteile**: Erfordert Browser-Extension-Entwicklung und -Verteilung; nicht kompatibel mit PWA/Mobile-Strategie; hohe Komplexitaet fuer geringen Nutzerkreis

### Option 3: Headless Browser (Puppeteer/Playwright auf Server)

- **Vorteile**: Kann JavaScript-gerenderte Seiten lesen (SPA-Portale wie Airbnb)
- **Nachteile**: Massiv erhoehte Server-Ressourcen (Chromium-Instanz), groessere Angriffsflaeche, komplexere Docker-Images, laengere Antwortzeiten; unverhältnismässig fuer v1

## Related

- ADR-0001: SCS-Architektur (unabhaengige Bounded Contexts)
- ADR-0008: DDD + Hexagonale Architektur (Port/Adapter-Muster)
- ADR-0017: OCR-Technologiewahl fuer Kassenzettel-Scan
- EventStorming Iteration 10: `docs/design/eventstorming/iteration-10-scope.md`
- OWASP SSRF Prevention Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
