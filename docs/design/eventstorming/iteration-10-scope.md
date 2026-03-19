# Design-Level EventStorming: Iteration 10 -- Revidierter Scope

**Datum**: 2026-03-19 (Replan)
**Scope**: Cross-SCS Analyse -- Accommodation URL Import, Kassenzettel-Scan, Settlement UI, PDF Export
**Methode**: Design-Level EventStorming (Alberto Brandolini)
**Bounded Contexts**: Trips (Core), Expense (Generic)
**Ausgangslage**: v0.9.0 -- Accommodation Aggregate, Party-Level Settlement, Advance Payments, Room Assignments, PWA Manifest

**KRITISCHE KORREKTUR**: Dieser Scope ersetzt das vorherige Iteration-10-Dokument vollstaendig. Die Prioritaeten wurden vom Nutzer korrigiert: Accommodation URL Import ist das Headline-Feature (nicht Recipe Import).

---

## 1. Bestandsaufnahme (Ist-Zustand nach Iteration 9)

### 1.1 Trips SCS -- Relevante Aggregates

```
Accommodation (AggregateRoot)
  +-- AccommodationId, TenantId, TripId
  +-- name, address, url, checkIn, checkOut, totalPrice
  +-- rooms: List<Room(roomId, name, roomType, bedCount, pricePerNight)>
  +-- assignments: List<RoomAssignment(assignmentId, roomId, partyTenantId, partyName, personCount)>
  +-- Factory: Accommodation.create(tenantId, tripId, name, ..., rooms)
  +-- updateDetails(name, address, url, checkIn, checkOut, totalPrice)
  +-- addRoom(room), removeRoom(roomId)
  +-- assignPartyToRoom(roomId, partyTenantId, partyName, personCount)

Room (Entity innerhalb Accommodation)
  +-- roomId, name, roomType (SINGLE/DOUBLE/QUAD/DORMITORY/OTHER), bedCount, pricePerNight

RoomType: SINGLE | DOUBLE | QUAD | DORMITORY | OTHER
```

### 1.2 Expense SCS -- Relevante Aggregates

```
Expense (AggregateRoot)
  +-- ExpenseId, TenantId, tripId, status (OPEN/SETTLED), reviewRequired
  +-- receipts: List<Receipt(receiptId, description, amount, paidBy, submittedBy, date, category, reviewStatus)>
  +-- weightings: List<ParticipantWeighting(participantId, weight)>
  +-- advancePayments: List<AdvancePayment(advancePaymentId, partyTenantId, partyName, amount, paid)>
  +-- addReceipt(description, amount, paidBy, submittedBy, date, category)

Receipt (Entity innerhalb Expense)
  +-- receiptId, description, amount (Amount VO), paidBy, submittedBy, date, category (ExpenseCategory)
  +-- reviewStatus: SUBMITTED | APPROVED | REJECTED

ExpenseCategory: ACCOMMODATION | GROCERIES | RESTAURANT | ACTIVITY | TRANSPORT | FUEL | HEALTH | OTHER
```

### 1.3 Event-Contracts (travelmate-common, unveraendert)

```
Trips publiziert:
  TripCreated, ParticipantJoinedTrip, TripCompleted, StayPeriodUpdated,
  AccommodationPriceSet, InvitationCreated, ExternalUserInvitedToTrip

Expense publiziert:
  ExpenseCreated, ExpenseSettled
```

### 1.4 Architektonische Beobachtung: Import-Pipeline-Muster fehlt

Im gesamten Projekt existiert bisher kein Muster fuer den Import externer Daten. Alle Daten werden manuell eingegeben. Iteration 10 fuehrt erstmals zwei Import-Features ein, die ein **gemeinsames UX-Muster** teilen:

```
Input --> Analyse --> Vorschau --> EDIT --> Bestaetigung --> Speichern
```

Der **EDIT-Schritt** ist entscheidend: Die Vorschau ist ein vollstaendig editierbares, vorausgefuelltes Formular. Der Nutzer MUSS falsch erkannte Daten korrigieren koennen, bevor gespeichert wird.

Dieses Muster wird in Iteration 10 fuer zwei Szenarien implementiert:
1. **Accommodation URL Import** (Trips SCS): URL eingeben -> Webseite scrapen -> Unterkunftsdaten extrahieren -> editierbares Formular
2. **Kassenzettel-Scan** (Expense SCS): Foto hochladen -> OCR -> Belegdaten extrahieren -> editierbares Formular

---

## 2. Priorisierung & Begruendung

### 2.1 Prioritaetskorrektur (vom Nutzer bestaetigt)

| Prio | Feature | Begruendung |
|------|---------|-------------|
| **HOCH** | Accommodation URL Import | Unterkunft ist DAS zentrale Planungselement jeder Gruppenreise. Daraus fliessen: Kostenverteilung, Vorauszahlungen, Zimmerbelegung. URL-Import hat den hoechsten UX-Hebel. |
| **MITTEL** | Kassenzettel-Scan (Receipt OCR) | Belege per Foto erfassen statt manuell eintippen. Eliminiert Eingabeaufwand waehrend der Reise. |
| **NIEDRIG** | Rezept-Import aus URL (Koch-Rezepte) | DEFERRED auf Iteration 11+. Mahlzeitenplanung ist nicht der Kern-Wert. |

### 2.2 Revidierter Iterationsplan

| # | ID | Story | Size | SCS | Begruendung |
|---|---|-------|------|-----|-------------|
| S10-A | US-TRIPS-061 | **Accommodation URL Import** | L | Trips | HEADLINE-Feature. Etabliert Import-Pipeline-Muster + SSRF-Schutz. Hoechster UX-Hebel. |
| S10-B | US-EXP-040 | **Kassenzettel-Scan (Receipt Photo OCR)** | M | Expense | Neues Feature. Nutzt Import-Pipeline-Muster fuer editierbare Vorschau. |
| S10-C | US-EXP-032 | Settlement per Category (Kategorie-Aufschluesselung) | M | Expense | Reines Read Model, kein Cross-SCS Impact. |
| S10-D | US-EXP-033 | Export Settlement as PDF | M | Expense | Download-Button, PDF mit Settlement-Zusammenfassung. |
| S10-E | US-INFRA-042 | Lighthouse CI Integration | S | Infra | PWA-Qualitaetsmessung, nur CI-Pipeline. |

**Gesamtumfang**: 1L + 3M + 1S -- konsistent mit Iteration 7-9 Velocity.

### 2.3 Empfohlene Reihenfolge

```
S10-A (Accommodation URL Import)    -- L, etabliert Import-Pipeline + SSRF-Schutz
  |                                    Umfasst:
  |                                    1. AccommodationImportPort im Domain-Layer
  |                                    2. WebScrapingAccommodationImportAdapter (HTTP + HTML Parsing)
  |                                    3. HtmlFetcher mit SSRF-Schutz (wiederverwendbar)
  |                                    4. AccommodationImportService (Application Service)
  |                                    5. Import-UI: URL-Eingabe -> Analyse -> Editierbares Formular -> Speichern
  |
  v
S10-B (Kassenzettel-Scan)           -- M, neues Feature in Expense SCS
  |                                    Umfasst:
  |                                    1. ReceiptScanPort im Domain-Layer
  |                                    2. OCR-Adapter (tess4j oder Cloud-API)
  |                                    3. Foto-Upload + Preview + editierbares Receipt-Formular
  |                                    4. Nur Gesamtbetrag ist Pflicht (Einzelposten nice-to-have)
  |
  v
S10-C (Category Breakdown)          -- M, reines Read Model, kein Cross-SCS Impact
  |
  v
S10-D (PDF Export)                   -- M, nutzt Category Breakdown + Settlement
  |
  v
S10-E (Lighthouse CI)               -- S, nach UI-Aenderungen am Ende
```

---

## 3. Design-Level EventStorming

### 3.1 Gemeinsames Muster: Import-Pipeline

Beide Import-Features (S10-A und S10-B) folgen demselben UX- und Command-Flow:

```
     Nutzer                   Controller              App-Service              Port/Adapter
       |                         |                        |                        |
       |  1. Input eingeben      |                        |                        |
       |  (URL / Foto)           |                        |                        |
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
       |  Formular anzeigen      |                        |                        |
       |<------------------------|                        |                        |
       |                         |                        |                        |
       |  7. Daten korrigieren   |                        |                        |
       |  + Bestaetigen          |                        |                        |
       |------------------------>|                        |                        |
       |                         |  8. SaveCommand        |                        |
       |                         |  (mit korrigierten     |                        |
       |                         |   Daten)               |                        |
       |                         |----------------------->|                        |
       |                         |                        |  9. Aggregate          |
       |                         |                        |  create/update         |
       |                         |                        |  + repository.save()   |
       |                         |                        |                        |
```

**Entscheidend**: Schritt 6 ist ein EDITIERBARES Formular, kein Read-Only-Preview. Alle extrahierten Felder sind vorausgefuellt, aber der Nutzer kann jedes Feld korrigieren, bevor Schritt 8 die Daten tatsaechlich speichert.

**Technisch**: Schritte 1-6 sind ein normaler GET/POST Roundtrip (kein Aggregate-Zugriff). Erst Schritt 8 erzeugt oder mutiert ein Aggregate.

---

### 3.2 S10-A: Accommodation URL Import (L) -- HEADLINE FEATURE

#### Kontext

Accommodation ist DAS zentrale Planungselement jeder Gruppenreise:
- Daraus fliesst: Kostenverteilung (totalPrice) -> Vorauszahlungen -> Settlement
- Zimmerstruktur -> Zimmerbelegung (Familien -> Zimmer)
- Ausstattung -> Entscheidungsfaktoren (Sauna, Garten, Bergblick)

Der Nutzer kopiert typischerweise die URL einer Ferienwohnung (booking.com, Ferienhausmiete.de, Airbnb, FeWo-direkt) und moechte daraus automatisch Name, Adresse, Preis, Check-in/Check-out, Zimmertypen und Ausstattung extrahieren.

#### Commands, Aggregates, Events

| Command | Actor | Aggregate | Event(s) | Policy |
|---------|-------|-----------|----------|--------|
| AnalyseAccommodationUrl(tenantId, tripId, url) | Organizer | -- (kein Aggregate, nur Port-Aufruf) | -- | Adapter fetcht URL, extrahiert Daten |
| -- (UI zeigt editierbares Formular) | Organizer | -- | -- | Nutzer korrigiert Felder |
| CreateAccommodationFromImport(tenantId, tripId, name, address, url, checkIn, checkOut, totalPrice, rooms) | Organizer | Accommodation | AccommodationPriceSet (wenn Preis > 0) | Normaler Accommodation.create() Flow |
| UpdateAccommodationFromImport(accommodationId, name, address, url, checkIn, checkOut, totalPrice) | Organizer | Accommodation | AccommodationPriceSet (wenn Preis geaendert) | Normaler Accommodation.updateDetails() Flow |

**Wichtig**: Der Import erzeugt keine neuen Domain Events. Das Speichern nutzt die bestehenden Accommodation.create() oder Accommodation.updateDetails() Methoden.

#### Domain Event Timeline

```
                    TRIPS SCS
                    ---------
    Organizer ist auf Trip-Detail-Seite, Abschnitt "Unterkunft"
              |
              v
    [1] Klickt "URL importieren" -> Eingabefeld erscheint (HTMX)
              |
              v
    [2] AnalyseAccommodationUrl(url="https://booking.com/hotel/de/berghuette-xyz")
              |
              v
    AccommodationImportService ruft AccommodationImportPort auf
              |
              v
    WebScrapingAccommodationImportAdapter:
      1. HtmlFetcher.fetch(url) -- SSRF-geschuetzt
      2. HTML parsen (Jsoup)
      3. Extraktionsstrategie (Prioritaetsreihenfolge):

         a) JSON-LD mit @type="LodgingBusiness" / "Hotel" / "House":
            - name, address (streetAddress, addressLocality, postalCode)
            - priceRange oder offers.price
            - checkinTime, checkoutTime
            - numberOfRooms, amenityFeature[]
            - image (URL, nur Preview)

         b) Open Graph Fallback:
            - og:title -> Name
            - og:description -> Beschreibung (fuer manuelle Uebertragung)
            - og:image -> Vorschaubild (nur Preview)
            - og:url -> Canonical URL

         c) HTML Heuristik Fallback:
            - <title> -> Name
            - meta[name="description"] -> Beschreibung
              |
              v
    [3] AccommodationImportResult zurueckgeben:
        Erfolg:
          name: "Berghuette Alpenblick"
          address: "Almweg 12, 83700 Rottach-Egern"
          totalPrice: 2400.00 EUR (wenn extrahierbar)
          checkIn: 2026-07-15 (wenn extrahierbar)
          checkOut: 2026-07-22 (wenn extrahierbar)
          rooms: [Room("Schlafzimmer 1", DOUBLE, 2), Room("Kinderzimmer", QUAD, 4)] (wenn extrahierbar)
          amenities: ["Sauna", "Garten", "Bergblick", "WLAN"] (wenn extrahierbar)
          imageUrl: "https://..." (nur Preview-Anzeige)
          sourceUrl: "https://booking.com/hotel/de/berghuette-xyz"
        ODER:
          Fehler: "Keine verwertbaren Daten auf dieser Seite gefunden"
              |
              v
    [4] Editierbares Formular anzeigen:
        - Alle extrahierten Felder vorausgefuellt
        - Felder die nicht extrahiert werden konnten: leer, vom Nutzer auszufuellen
        - Zimmer: Extrahierte Zimmer als Startpunkt, Nutzer kann hinzufuegen/entfernen/aendern
        - Vorschaubild: Nur zur visuellen Bestaetigung ("Ist das die richtige Unterkunft?")
              |
              v
    [5] Nutzer korrigiert: Preis anpassen, Zimmer umbenennen, fehlende Daten ergaenzen
              |
              v
    [6] CreateAccommodationFromImport / UpdateAccommodationFromImport
              |
              v
    Accommodation.create(tenantId, tripId, name, ..., rooms)
        -> AccommodationPriceSet Event (wenn totalPrice > 0)
        -> repository.save()
```

#### Port-Interface im Domain-Layer

```
AccommodationImportPort (Interface in domain/accommodation/)
  +-- extractAccommodationInfo(url: String) -> AccommodationImportResult

AccommodationImportResult (Record in domain/accommodation/)
  +-- success: boolean
  +-- name: String (nullable)
  +-- address: String (nullable)
  +-- totalPrice: BigDecimal (nullable)
  +-- checkIn: LocalDate (nullable)
  +-- checkOut: LocalDate (nullable)
  +-- rooms: List<ImportedRoom> (kann leer sein)
  +-- amenities: List<String> (kann leer sein)
  +-- imageUrl: String (nullable, nur fuer Preview)
  +-- sourceUrl: String
  +-- errorMessage: String (nullable)

ImportedRoom (Record in domain/accommodation/)
  +-- name: String
  +-- roomType: RoomType (gemappt aus Daten, default OTHER)
  +-- bedCount: int (default 2)
```

#### Adapter-Implementierung

```
WebScrapingAccommodationImportAdapter (in adapters/integration/)
  +-- Implementiert AccommodationImportPort
  +-- Abhaengigkeiten:
      - java.net.http.HttpClient (Java 21, kein externer HTTP Client noetig)
      - Jsoup (HTML Parsing)
      - Jackson (JSON-LD Parsing, bereits vorhanden via Spring)
  +-- @Profile("!test") -- in Tests gemockt
  +-- Timeout: 10 Sekunden
  +-- User-Agent: "Travelmate/0.10.0"

HtmlFetcher (in adapters/integration/, WIEDERVERWENDBAR)
  +-- fetch(url: String) -> FetchResult(body: String, contentType: String, statusCode: int)
  +-- SSRF-Schutz:
      1. Nur HTTPS-URLs akzeptieren
      2. URL-Validierung: kein localhost, keine privaten IPs (10.x, 192.168.x, 172.16-31.x, 169.254.x)
      3. DNS-Rebinding-Schutz: Resolved IP VOR Fetch pruefen
      4. Response-Size-Limit: max 5 MB
      5. Timeout: 10 Sekunden
      6. Redirect-Limit: max 3 Redirects, jede Redirect-URL erneut SSRF-pruefen

JsonLdExtractor (in adapters/integration/)
  +-- extractByType(html: String, type: String) -> Optional<JsonNode>
  +-- Sucht <script type="application/ld+json"> Bloecke
  +-- Filtert nach @type == type (z.B. "LodgingBusiness", "Hotel", "House")

OpenGraphExtractor (in adapters/integration/)
  +-- extract(html: String) -> Map<String, String>
  +-- Extrahiert alle og: Meta-Tags
```

#### Extraktionsstrategie: Welche Websites?

Die grossen Ferienwohnungs-Portale verwenden unterschiedliche Formate:

| Portal | JSON-LD | Open Graph | HTML-Heuristik |
|--------|---------|------------|----------------|
| booking.com | schema.org/Hotel (strukturiert) | Ja | Komplex (SPA) |
| Airbnb | Teilweise (schema.org/Product) | Ja | SPA, schwierig |
| Ferienhausmiete.de | Teilweise | Ja | Gut parsbar |
| FeWo-direkt (VRBO) | schema.org/House | Ja | Gut parsbar |
| Traum-Ferienwohnungen | Teilweise | Ja | Gut parsbar |

**Akzeptanz**: Import ist Best-Effort. Nicht jede Website liefert alle Felder. Das editierbare Formular faengt fehlende Daten auf. Der Hauptwert ist: Name + URL werden mindestens uebernommen, oft auch Adresse und Preis.

#### Invarianten

```
INV-AI1: URL muss gueltiges HTTPS-Format haben
INV-AI2: URL darf nicht auf localhost oder private IPs zeigen (SSRF-Schutz)
INV-AI3: Response-Groesse <= 5 MB
INV-AI4: Mindestens Name muss extrahierbar sein, sonst Fehlermeldung
INV-AI5: Nutzer kann IMMER manuell korrigieren (Import ist Vorschlag, nicht Pflicht)
INV-AI6: Import erzeugt keine neuen Event-Typen -- nutzt bestehende Accommodation-Flows
INV-AI7: Rooms muessen mindestens 1 Bett haben (bestehende Room-Invariante)
```

---

### 3.3 S10-B: Kassenzettel-Scan / Receipt Photo OCR (M)

#### Kontext

Waehrend der Reise kaufen Teilnehmer staendig ein: Supermarkt, Baeckerei, Liftkarte, Restaurantrechnung. Aktuell muessen Beschreibung, Betrag, Datum und Kategorie manuell im Expense-Formular eingetippt werden. Ein Foto des Kassenzettels wuerde diesen Prozess erheblich beschleunigen.

**Wichtig**: Der Gesamtbetrag ist das kritischste Feld. Einzelposten (Artikelzeilen) sind nice-to-have fuer die erste Version. V1 fokussiert auf: Foto -> Gesamtbetrag + Datum + Geschaeftsname erkennen -> editierbares Receipt-Formular.

#### Welches SCS?

Kassenzettel-Scan gehoert vollstaendig in den **Expense SCS**:
- Receipt ist eine Entity innerhalb des Expense Aggregates
- Kein Trips-Kontext noetig (Expense kennt bereits tripId via TripProjection)
- OCR-Ergebnis fuettert direkt `expense.addReceipt(...)`

#### Commands, Aggregates, Events

| Command | Actor | Aggregate | Event(s) | Policy |
|---------|-------|-----------|----------|--------|
| ScanReceipt(tenantId, tripId, imageBytes) | Participant | -- (kein Aggregate, nur Port-Aufruf) | -- | OCR-Adapter analysiert Bild |
| -- (UI zeigt editierbares Receipt-Formular) | Participant | -- | -- | Nutzer korrigiert Felder |
| AddReceiptFromScan(expenseId, description, amount, paidBy, submittedBy, date, category) | Participant | Expense | -- | Normaler Expense.addReceipt() Flow |

#### Domain Event Timeline

```
                    EXPENSE SCS
                    -----------
    Participant ist auf Receipt-Liste-Seite fuer einen Trip
              |
              v
    [1] Klickt "Kassenzettel fotografieren" -> Kamera/Galerie oeffnet sich
        (HTML: <input type="file" accept="image/*" capture="environment">)
              |
              v
    [2] Foto wird hochgeladen (multipart/form-data)
              |
              v
    [3] ScanReceipt(imageBytes, contentType)
              |
              v
    ReceiptScanService ruft ReceiptScanPort auf
              |
              v
    OcrReceiptScanAdapter:
      1. Bild validieren (max 10 MB, JPEG/PNG/HEIC)
      2. Optional: Bild vorverarbeiten (Kontrast, Rotation)
      3. OCR ausfuehren (tess4j / Tesseract)
      4. OCR-Text parsen:
         - Gesamtbetrag suchen (Regex: "SUMME", "TOTAL", "GESAMT", "EUR", letzte Zahl)
         - Datum suchen (Regex: DD.MM.YYYY, DD.MM.YY)
         - Geschaeftsname suchen (erste Zeile oder Header)
         - Einzelposten suchen (Zeilen mit Preis am Ende) -- nice-to-have
              |
              v
    [4] ReceiptScanResult zurueckgeben:
        Erfolg:
          totalAmount: 47.83 EUR
          date: 2026-07-18
          storeName: "EDEKA Markt Mueller"
          lineItems: [("Bio-Milch 1L", 1.89), ("Brot", 3.49), ...] -- optional
          confidence: 0.85 (OCR-Konfidenz)
          rawText: "EDEKA\nMarkt Mueller\n..." (fuer Debug)
        ODER:
          Fehler: "Kassenzettel konnte nicht erkannt werden"
              |
              v
    [5] Editierbares Receipt-Formular anzeigen:
        - Beschreibung: vorausgefuellt mit storeName (z.B. "EDEKA Markt Mueller")
        - Betrag: vorausgefuellt mit totalAmount (z.B. 47.83)
        - Datum: vorausgefuellt mit erkanntem Datum
        - Kategorie: vorgeschlagen basierend auf storeName (EDEKA -> GROCERIES)
        - Bezahlt von: aktueller Nutzer (default)
        - Alle Felder editierbar!
              |
              v
    [6] Participant korrigiert: Betrag anpassen, Kategorie aendern, Beschreibung verfeinern
              |
              v
    [7] AddReceiptFromScan -- technisch identisch mit normalem addReceipt()
              |
              v
    Expense.addReceipt(description, amount, paidBy, submittedBy, date, category)
        -> repository.save()
```

#### Port-Interface im Domain-Layer

```
ReceiptScanPort (Interface in domain/expense/)
  +-- scanReceipt(imageBytes: byte[], contentType: String) -> ReceiptScanResult

ReceiptScanResult (Record in domain/expense/)
  +-- success: boolean
  +-- totalAmount: BigDecimal (nullable)
  +-- date: LocalDate (nullable)
  +-- storeName: String (nullable)
  +-- suggestedCategory: ExpenseCategory (nullable)
  +-- lineItems: List<ScannedLineItem> (kann leer sein)
  +-- confidence: double (0.0 - 1.0)
  +-- rawText: String (nullable, fuer Debug/Transparenz)
  +-- errorMessage: String (nullable)

ScannedLineItem (Record in domain/expense/)
  +-- name: String
  +-- amount: BigDecimal
```

#### Adapter-Implementierung: OCR-Technologie-Entscheidung

**Option A: tess4j (Tesseract, self-hosted)** -- EMPFOHLEN fuer v1
- Vorteile: Keine externe Abhaengigkeit, keine Kosten, DSGVO-konform (Daten bleiben lokal)
- Nachteile: OCR-Qualitaet bei schlechten Fotos maessig, Tesseract-Installation erforderlich
- Maven: `net.sourceforge.tess4j:tess4j:5.13.0`
- Trainierte Sprache: `deu` (Deutsch) + `eng` fuer internationale Belege

**Option B: Google Cloud Vision API**
- Vorteile: Deutlich bessere OCR-Qualitaet, Dokumenten-KI
- Nachteile: Kosten, externe Abhaengigkeit, DSGVO-Bedenken (Belegdaten an Google)

**Option C: Cloud-agnostische Abstraktion mit Tesseract als Default**
- ReceiptScanPort im Domain-Layer bleibt stabil
- TesseractReceiptScanAdapter als Default
- Spaeter austauschbar gegen CloudVisionReceiptScanAdapter

Empfehlung: **Option C** (Design), **Option A** (Implementierung in Iter 10). Port-Abstraktion erlaubt spaetere Optimierung.

#### Kategorie-Erkennung (Heuristik)

```
CategoryGuesser (Domain Service in domain/expense/)
  +-- guess(storeName: String) -> ExpenseCategory

  Mapping-Regeln (konfigurierbar):
    EDEKA, REWE, ALDI, LIDL, SPAR, Penny, Netto    -> GROCERIES
    Baeckerei, Brot, Backhaus                         -> GROCERIES
    Restaurant, Gasthaus, Pizzeria, Trattoria         -> RESTAURANT
    Shell, Aral, Total, OMV, Tankstelle               -> FUEL
    Apotheke, Drogerie, DM, Mueller                   -> HEALTH
    Bergbahn, Seilbahn, Skilift, Therme, Museum       -> ACTIVITY
    DB, Bahn, Bus, Taxi, Uber, Maut                   -> TRANSPORT
    Default                                           -> OTHER
```

#### Invarianten

```
INV-RS1: Bild darf max 10 MB gross sein
INV-RS2: Nur JPEG, PNG, HEIC als Formate akzeptiert
INV-RS3: OCR-Ergebnis ist immer ein VORSCHLAG -- Nutzer muss bestaetigen
INV-RS4: Gesamtbetrag ist das primaere Extraktionsziel
INV-RS5: Wenn OCR fehlschlaegt -> Nutzer faellt auf manuelles Formular zurueck (kein Blocker)
INV-RS6: Keine Persistierung des Originalbildes in v1 (Datenschutz, Speicherplatz)
```

---

### 3.4 S10-C: Settlement per Category (M) -- UNVERAENDERT

#### Commands, Aggregates, Events

| Command | Actor | Aggregate | Event(s) | Policy |
|---------|-------|-----------|----------|--------|
| ViewCategoryBreakdown(expenseId) | Organizer/Participant | Expense (read) | -- | Reines Read Model |

#### Domain Event Timeline

```
                    EXPENSE SCS
                    -----------
    Expense existiert mit Receipts die Kategorien haben
              |
              v
    [1] ViewCategoryBreakdown(expenseId)
              |
              v
    ExpenseService laedt Expense
              |
              v
    [2] Expense.calculateCategoryBreakdown() -> CategoryBreakdown
              |
              v
    CategoryBreakdown:
      +-- totalAmount: BigDecimal
      +-- categories: List<CategoryShare>

    CategoryShare:
      +-- category: ExpenseCategory
      +-- amount: BigDecimal
      +-- percentage: BigDecimal (z.B. 45.2%)
      +-- receiptCount: int

    Beispiel:
      Total: 3000 EUR
      - ACCOMMODATION: 1800 EUR (60.0%, 1 Beleg)
      - GROCERIES: 750 EUR (25.0%, 12 Belege)
      - RESTAURANT: 300 EUR (10.0%, 4 Belege)
      - TRANSPORT: 100 EUR (3.3%, 2 Belege)
      - OTHER: 50 EUR (1.7%, 3 Belege)
```

#### Aggregate-Erweiterung

```
Expense (AggregateRoot) -- ERWEITERT
  +-- calculateCategoryBreakdown() -> CategoryBreakdown  NEU

CategoryBreakdown (Value Object, Record) -- NEU
  +-- totalAmount: BigDecimal
  +-- categories: List<CategoryShare>

CategoryShare (Value Object, Record) -- NEU
  +-- category: ExpenseCategory
  +-- amount: BigDecimal
  +-- percentage: BigDecimal
  +-- receiptCount: int
```

Reines Read Model -- keine Events, keine Persistenz-Aenderung, keine Cross-SCS Kommunikation.

---

### 3.5 S10-D: Export Settlement as PDF (M) -- UNVERAENDERT

#### Commands, Aggregates, Events

| Command | Actor | Aggregate | Event(s) | Policy |
|---------|-------|-----------|----------|--------|
| ExportSettlementPdf(expenseId) | Organizer | Expense (read) | -- | Liest Settlement + Category + Transfers |

#### Domain Event Timeline

```
                    EXPENSE SCS
                    -----------
    Organizer klickt "Als PDF exportieren" auf Settlement-Seite
              |
              v
    [1] ExportSettlementPdf(expenseId)
              |
              v
    SettlementPdfService (Application Service):
      1. Expense laden
      2. calculateBalances() + calculateSettlementPlan()
      3. PartySettlement.aggregateByParty() + calculateTransfers()
      4. calculateCategoryBreakdown() (aus S10-C)
      5. TripProjection laden (Trip-Name, Zeitraum)
              |
              v
    [2] SettlementPdfGenerator (Adapter in adapters/pdf/):
      1. Thymeleaf Template rendern (HTML -> settlement-pdf.html)
      2. HTML -> PDF konvertieren (OpenPDF / Flying Saucer)
      3. PDF als byte[] zurueckgeben
              |
              v
    [3] Controller liefert PDF als Download:
      Content-Type: application/pdf
      Content-Disposition: attachment; filename="Abrechnung-{tripName}.pdf"
```

#### Adapter-Design

```
SettlementPdfPort (Interface in application/)
  +-- generatePdf(data: SettlementPdfData) -> byte[]

SettlementPdfData (Record in application/):
  +-- tripName: String
  +-- dateRange: String (formatiert)
  +-- partyTransfers: List<PartyTransfer>
  +-- categoryBreakdown: CategoryBreakdown
  +-- partyBalances: Map<String, BigDecimal> (partyName -> balance)
  +-- advancePayments: List<AdvancePaymentInfo>
  +-- totalExpenses: BigDecimal
  +-- generatedAt: LocalDate

SettlementPdfAdapter (in adapters/pdf/)
  +-- Implementiert SettlementPdfPort
  +-- Thymeleaf TemplateEngine (standalone, ohne Web-Context)
  +-- Flying Saucer (org.xhtmlrenderer:flying-saucer-pdf) fuer HTML->PDF
  +-- Template: src/main/resources/templates/pdf/settlement-report.html
```

---

### 3.6 S10-E: Lighthouse CI Integration (S) -- UNVERAENDERT

Kein Domain-Event-Flow. Reine CI/CD-Infrastruktur.

```
GitHub Actions Workflow:
  1. Build + Test (bestehend)
  2. Docker Compose up (alle Services)
  3. Lighthouse CI gegen Key Pages:
     - /iam/dashboard
     - /trips/
     - /trips/{id} (Trip Detail)
     - /trips/{id}/shopping-list
  4. Assertions: Performance >= 80, Accessibility >= 90
  5. Report als CI Artifact speichern

Konfiguration: .lighthouserc.json im Root
```

---

## 4. Cross-SCS Event Design

### 4.1 Keine neuen Cross-SCS Events

Iteration 10 fuehrt **keine neuen Event-Contracts** ein:

- S10-A: Trips-internes Import-Feature -> bestehende AccommodationPriceSet Events
- S10-B: Expense-internes Feature -> bestehender addReceipt() Flow
- S10-C/D: Expense-internes Read Model + PDF Export
- S10-E: Infrastructure only

### 4.2 Bestehende Events (unveraendert)

```
AccommodationPriceSet(tenantId, tripId, totalPrice, occurredOn) -- Trips -> Expense
TripCreated, ParticipantJoinedTrip, TripCompleted, StayPeriodUpdated -- alle unveraendert
```

---

## 5. Aggregate Design (Aenderungen)

### 5.1 Accommodation (Trips SCS) -- ERWEITERT um Import-Port

```
Accommodation (AggregateRoot) -- Struktur unveraendert, Methoden unveraendert

AccommodationImportPort (NEU, Interface in domain/accommodation/)
  +-- extractAccommodationInfo(url: String) -> AccommodationImportResult

AccommodationImportResult (NEU, Record in domain/accommodation/)
  +-- success, name, address, totalPrice, checkIn, checkOut
  +-- rooms: List<ImportedRoom>, amenities: List<String>
  +-- imageUrl, sourceUrl, errorMessage

ImportedRoom (NEU, Record in domain/accommodation/)
  +-- name, roomType, bedCount
```

### 5.2 Expense (Expense SCS) -- ERWEITERT um OCR-Port und Category Breakdown

```
Expense (AggregateRoot) -- neue Methode
  +-- calculateCategoryBreakdown() -> CategoryBreakdown  NEU

ReceiptScanPort (NEU, Interface in domain/expense/)
  +-- scanReceipt(imageBytes: byte[], contentType: String) -> ReceiptScanResult

ReceiptScanResult (NEU, Record in domain/expense/)
  +-- success, totalAmount, date, storeName, suggestedCategory
  +-- lineItems: List<ScannedLineItem>, confidence, rawText, errorMessage

ScannedLineItem (NEU, Record in domain/expense/)
  +-- name, amount

CategoryBreakdown (NEU, Record in domain/expense/)
  +-- totalAmount, categories: List<CategoryShare>

CategoryShare (NEU, Record in domain/expense/)
  +-- category, amount, percentage, receiptCount

CategoryGuesser (NEU, Domain Service in domain/expense/)
  +-- guess(storeName: String) -> ExpenseCategory
```

---

## 6. Flyway Migrationen

### Trips SCS

Keine Schema-Aenderung erforderlich. Accommodation-Tabellen existieren bereits. Import-Ports aendern nur die Adapter-Schicht.

### Expense SCS

Keine Schema-Aenderung erforderlich. CategoryBreakdown ist berechnet, nicht persistiert. OCR-Scan erzeugt normale Receipts ueber den bestehenden Flow.

**Kein Bild-Upload persistiert in v1** -- das Foto wird nur transient fuer die OCR-Analyse verwendet und nicht gespeichert.

---

## 7. Zusammenfassung der Code-Aenderungen

### travelmate-common (Shared Kernel)

**Keine Aenderungen.** Kein neues Event, kein neuer Routing Key.

### travelmate-trips (Trips SCS)

**Neue Domain-Typen**:
- `AccommodationImportPort` (Interface in `domain/accommodation/`)
- `AccommodationImportResult` (Record in `domain/accommodation/`)
- `ImportedRoom` (Record in `domain/accommodation/`)

**Neue Adapter** (in `adapters/integration/`):
- `HtmlFetcher` -- HTTP GET mit SSRF-Schutz, Timeout, Size-Limit (WIEDERVERWENDBAR)
- `JsonLdExtractor` -- JSON-LD Extraktion aus HTML
- `OpenGraphExtractor` -- Open Graph Meta-Tag Extraktion
- `WebScrapingAccommodationImportAdapter` -- Implementiert `AccommodationImportPort`

**Neue Application Services**:
- `AccommodationImportService` -- Orchestriert Import-Flow

**Neue Web-Adapter**:
- `AccommodationImportController` (oder Erweiterung von `AccommodationController`) -- Import-Endpoints

**Neue Maven Dependencies**:
- `org.jsoup:jsoup:1.18.3` -- HTML Parsing

**Neue Templates**:
- `accommodation/import.html` oder HTMX-Fragment -- URL-Eingabe + editierbares Import-Formular

**Flyway**: Keine neue Migration.

### travelmate-expense (Expense SCS)

**Neue Domain-Typen**:
- `ReceiptScanPort` (Interface in `domain/expense/`)
- `ReceiptScanResult` (Record in `domain/expense/`)
- `ScannedLineItem` (Record in `domain/expense/`)
- `CategoryGuesser` (Domain Service in `domain/expense/`)
- `CategoryBreakdown` (Record in `domain/expense/`)
- `CategoryShare` (Record in `domain/expense/`)

**Neue Methode auf Expense**:
- `Expense.calculateCategoryBreakdown()` -> `CategoryBreakdown`

**Neue Application Services**:
- `ReceiptScanService` -- Orchestriert Scan-Flow
- `SettlementPdfService` -- Orchestriert PDF-Generierung

**Neue Ports**:
- `SettlementPdfPort` (Interface in `application/`)

**Neue Adapter**:
- `TesseractReceiptScanAdapter` (in `adapters/ocr/`) -- Implementiert `ReceiptScanPort`
- `SettlementPdfAdapter` (in `adapters/pdf/`) -- Thymeleaf + Flying Saucer

**Neue Web-Adapter**:
- `ReceiptScanController` (oder Erweiterung bestehender Controller) -- Upload + Preview Endpoints
- Settlement-Controller erweitert -- GET `/{tripId}/settlement/pdf`
- Settlement-Controller erweitert -- GET `/{tripId}/categories` (HTMX partial)

**Neue Maven Dependencies**:
- `net.sourceforge.tess4j:tess4j:5.13.0` -- OCR
- `org.xhtmlrenderer:flying-saucer-pdf:9.11.3` -- PDF-Generierung

**Neue Templates**:
- `receipt/scan.html` oder HTMX-Fragment -- Foto-Upload + editierbares Receipt-Formular
- `expense/categories.html` -- Kategorie-Aufschluesselung (partial)
- `pdf/settlement-report.html` -- PDF-Template

**Flyway**: Keine neue Migration.

### travelmate-gateway

**Keine Aenderungen.**

### travelmate-e2e

**Neue E2E Tests**:
- Accommodation Import: URL eingeben, Preview/Edit, Speichern, Accommodation vorhanden
- Kassenzettel-Scan: Foto hochladen, Preview/Edit, Speichern, Receipt vorhanden
- Category Breakdown: Sichtbar auf Settlement-Seite
- PDF Export: Download-Link funktioniert (Content-Type pruefen)

**Neue BDD Scenarios** (Cucumber):
- `accommodation-import.feature` -- 3-4 Szenarien (Erfolg, ungueltige URL, SSRF-Block, Fallback manuell)
- `receipt-scan.feature` -- 3 Szenarien (Erfolg, schlechtes Bild, Korrektur)
- `settlement-export.feature` -- 2 Szenarien (PDF Download, Kategorie-Ansicht)

---

## 8. Hot Spots

```
[HS-10-1] SSRF via URL-Import-Endpunkt (HOCH)
  Problem: Nutzer gibt beliebige URLs ein -> Server fetcht diese.
  Mitigation:
    1. Nur HTTPS-URLs akzeptieren
    2. URL-Validierung: kein localhost, keine privaten IP-Bereiche (10.x, 192.168.x, 172.16-31.x, 169.254.x)
    3. DNS-Rebinding-Schutz: Resolved IP VOR Fetch pruefen
    4. Response-Size-Limit: max 5 MB
    5. Redirect-Limit: max 3, jede Redirect-URL erneut SSRF-pruefen
    6. Timeout: 10s
  -> ADR-Kandidat!

[HS-10-2] Website-Verfuegbarkeit und Blocking (MITTEL)
  Problem: Manche Websites blocken Bot-Requests oder haben kein JSON-LD/OG.
  Mitigation: Fallback-Kette (JSON-LD -> OG -> HTML), klare Fehlermeldung, manuelle Eingabe als Backup.
  Akzeptanz: Import ist Best-Effort.

[HS-10-3] OCR-Qualitaet bei Kassenzettel-Fotos (MITTEL)
  Problem: Tesseract hat Schwaechen bei:
    - Schlechter Beleuchtung / unscharfen Fotos
    - Thermodruckern (verblasster Text)
    - Ungewoehnlichen Schriftarten (z.B. Bonrollen)
  Mitigation:
    - Bild-Vorverarbeitung (Kontrast, Schwellwert)
    - Gesamtbetrag hat hoechste Prioritaet (groesste Zahl am Ende)
    - Editierbares Formular als Auffangnetz
    - Confidence-Score anzeigen ("Erkennung: 85% sicher")
  Akzeptanz: ~70% Trefferquote fuer Gesamtbetrag als Ziel fuer v1.

[HS-10-4] Tesseract als native Abhaengigkeit (MITTEL)
  Problem: tess4j benoetigt Tesseract-Bibliothek + Sprachdaten auf dem Server.
  Mitigation:
    - Dockerfile um tesseract-ocr + tessdata erweitern
    - docker-compose testet mit nativem Tesseract
    - Fallback: Wenn Tesseract nicht verfuegbar -> ReceiptScanPort gibt Fehler zurueck
  -> ADR-Kandidat (OCR-Technologiewahl)

[HS-10-5] Bild-Upload Groesse und Format (NIEDRIG)
  Problem: Handy-Fotos koennen 10+ MB sein (HEIC, hohe Aufloesung).
  Mitigation: Max 10 MB, JPEG/PNG/HEIC, serverseitige Konvertierung nach JPEG falls noetig.

[HS-10-6] PDF-Rendering-Qualitaet (NIEDRIG)
  Problem: HTML-zu-PDF-Konvertierung hat Einschraenkungen bei CSS.
  Mitigation: Einfaches Tabellen-Layout, keine komplexen CSS-Features.

[HS-10-7] Datenschutz bei OCR (NIEDRIG)
  Problem: Kassenzettel-Fotos enthalten potenziell personenbezogene Daten.
  Mitigation: Foto wird NICHT persistiert, nur transient verarbeitet. Tesseract laeuft lokal.
```

---

## 9. Sicherheitsbetrachtungen

### 9.1 SSRF-Schutz (S10-A)

Erstmaliges Fetchen externer URLs durch den Server. Vollstaendige SSRF-Mitigation erforderlich:

| Massnahme | Implementierung |
|-----------|----------------|
| HTTPS-Only | URL-Schema-Pruefung vor Fetch |
| Private-IP-Blacklist | InetAddress.getByName() -> isLoopbackAddress(), isSiteLocalAddress(), isLinkLocalAddress() |
| DNS-Rebinding-Schutz | IP vor Connect pruefen, nicht nur Hostname |
| Size-Limit | HttpResponse mit InputStream lesen, bei 5 MB abbrechen |
| Timeout | HttpClient.Builder.connectTimeout(10s), request timeout |
| Redirect-Schutz | Max 3 Redirects, jede URL re-validieren |
| User-Agent | "Travelmate/0.10.0" -- transparent, kein Spoofing |

### 9.2 Bild-Upload-Sicherheit (S10-B)

| Massnahme | Implementierung |
|-----------|----------------|
| Size-Limit | Max 10 MB (Spring: `spring.servlet.multipart.max-file-size`) |
| Content-Type-Pruefung | Magic Bytes pruefen, nicht nur Content-Type Header |
| Kein Persistieren | Bild nur transient im Memory, nach OCR verworfen |
| Kein Server-Rendering | Bild wird nicht zurueck an Browser gesendet (kein XSS via SVG/HTML-in-Image) |

---

## 10. ADR-Kandidaten

| ADR | Thema | Trigger |
|-----|-------|---------|
| ADR-0016 | URL-basierter Import mit SSRF-Schutz -- Import-Pipeline-Muster im Hexagonal Pattern | HS-10-1, S10-A |
| ADR-0017 | OCR-Technologiewahl fuer Kassenzettel-Scan (Tesseract vs. Cloud Vision) | HS-10-3, HS-10-4, S10-B |
| ADR-0018 | PDF-Generierung via Thymeleaf + Flying Saucer | S10-D |

**Hinweis**: ADR-0015 ist das letzte bestehende ADR (Shopping List Aggregate Design). Die Nummerierung beginnt bei ADR-0016.

---

## 11. Deferred (Iteration 11+)

| Story | Begruendung |
|-------|-------------|
| US-TRIPS-041: Recipe Import from URL | NIEDRIGE Prioritaet (Nutzer-Korrektur). Mahlzeitenplanung ist nicht der Kern-Wert. |
| US-TRIPS-062: Accommodation Poll | Eigenes Aggregate, grosse Feature-Iteration |
| US-EXP-022: Custom Splitting per Receipt | Aendert Settlement-Kern, riskant |
| US-TRIPS-055: Bring App Integration | Externe API-Abhaengigkeit |
| US-IAM-051: SMS Notifications | Externe SMS-Gateway-Abhaengigkeit |

---

## 12. Naechste Schritte

1. **ADRs schreiben**: ADR-0016 (Import-Pipeline + SSRF), ADR-0017 (OCR-Technologie), ADR-0018 (PDF)
2. **Story Refinement**: Detaillierte Akzeptanzkriterien fuer S10-A bis S10-E
3. **UX Wireframes**: Accommodation-Import-UI (URL + editierbares Formular), Receipt-Scan-UI (Foto + editierbares Formular)
4. **Spike S10-A**: Jsoup + JSON-LD/OG Parsing gegen booking.com, Ferienhausmiete.de, Airbnb testen
5. **Spike S10-B**: tess4j gegen typische Kassenzettels testen (EDEKA, REWE, Baeckerei)
6. **SSRF-Schutz implementieren**: HtmlFetcher mit IP-Blacklist als erstes (S10-A Grundlage)
7. **TDD fuer Domain**:
   - Trips: AccommodationImportResult (Value Object), ImportedRoom
   - Expense: ReceiptScanResult, CategoryGuesser, calculateCategoryBreakdown()
8. **Docker**: Dockerfile um tesseract-ocr + tessdata-deu erweitern (fuer S10-B)
9. **E2E-Tests**: Import-Flow, Scan-Flow, Category-View, PDF-Download
10. **Lighthouse Baseline**: Aktuelle Scores messen vor CI-Integration
