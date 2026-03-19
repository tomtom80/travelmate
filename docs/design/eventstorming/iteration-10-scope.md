# Design-Level EventStorming: Iteration 10 -- Scope-Analyse & Planung

**Datum**: 2026-03-19
**Scope**: Cross-SCS Analyse -- Recipe Import, URL Metadata Extraction, Settlement PDF, Accommodation Room Assignments
**Methode**: Design-Level EventStorming (Alberto Brandolini)
**Bounded Contexts**: Trips (Core), Expense (Generic)
**Ausgangslage**: v0.9.0 -- Accommodation Aggregate, Party-Level Settlement, Advance Payments, Room Assignments, PWA Manifest

---

## 1. Bestandsaufnahme (Ist-Zustand nach Iteration 9)

### 1.1 Trips SCS -- Aggregate und Events

```
Trip (AggregateRoot)
  +-- TripId, TenantId, name, description, dateRange, organizerId
  +-- status: PLANNING | CONFIRMED | IN_PROGRESS | COMPLETED | CANCELLED
  +-- participants: List<Participant(participantId, firstName, lastName, stayPeriod)>

Invitation (AggregateRoot) -- MEMBER | EXTERNAL

TravelParty (Projection/ReadModel)
  +-- TenantId, members, dependents

Recipe (AggregateRoot)
  +-- RecipeId, TenantId, name: RecipeName, servings: Servings
  +-- ingredients: List<Ingredient(name, quantity, unit)>

MealPlan (AggregateRoot) -- MealSlots with PLANNED/SKIP/EATING_OUT

ShoppingList (AggregateRoot) -- ShoppingItems with OPEN/ASSIGNED/PURCHASED

Accommodation (AggregateRoot)
  +-- AccommodationId, TenantId, TripId
  +-- name, address, url, checkIn, checkOut, totalPrice
  +-- rooms: List<Room(roomId, name, roomType, bedCount, pricePerNight)>
  +-- assignments: List<RoomAssignment(assignmentId, roomId, partyTenantId, partyName, personCount)>
```

### 1.2 Expense SCS -- Aggregate und Events

```
Expense (AggregateRoot)
  +-- ExpenseId, TenantId, tripId, status, reviewRequired
  +-- receipts: List<Receipt(receiptId, description, amount, paidBy, submittedBy, date, category, reviewStatus)>
  +-- weightings: List<ParticipantWeighting(participantId, weight)>
  +-- advancePayments: List<AdvancePayment(advancePaymentId, partyTenantId, partyName, amount, paid)>

PartySettlement (Domain Service)
  +-- aggregateByParty(individualBalances, participantToParty) -> Map<partyId, balance>
  +-- calculateTransfers(partyBalances) -> List<PartyTransfer>

TripProjection (ReadModel)
  +-- tripId, tenantId, tripName, startDate, endDate
  +-- accommodationTotalPrice
  +-- participants: List<TripParticipant(participantId, name, arrivalDate, departureDate, partyTenantId, partyName)>

AdvancePaymentSuggestion (Domain Service)
  +-- suggest(accommodationCost, partyCount) -> BigDecimal (ceil/50 Formel)
```

### 1.3 Event-Contracts (travelmate-common)

```
IAM publiziert:
  TenantCreated, AccountRegistered, MemberAddedToTenant, DependentAddedToTenant,
  DependentRemovedFromTenant, MemberRemovedFromTenant, TenantDeleted,
  RoleAssignedToUser, RoleUnassignedFromUser

Trips publiziert:
  TripCreated, ParticipantJoinedTrip(+participantTenantId, +partyName),
  TripCompleted, StayPeriodUpdated, AccommodationPriceSet,
  InvitationCreated, ExternalUserInvitedToTrip

Expense publiziert:
  ExpenseCreated, ExpenseSettled
```

### 1.4 Architektonische Beobachtung: Import-Adapter-Muster fehlt

Im Trips SCS existieren bisher keine Integration-Adapter fuer externe Datenquellen. Alle Daten werden manuell eingegeben. Iteration 10 fuehrt erstmals das Muster **"URL-basierter Import externer Daten"** ein, das fuer zwei Features benoetigt wird:

1. **Recipe Import**: schema.org/Recipe JSON-LD aus beliebigen Koch-Websites
2. **Accommodation Import**: Open Graph + schema.org/LodgingBusiness Metadaten aus Buchungsseiten

Beide Features teilen dieselbe technische Grundlage: HTTP Fetch + HTML/JSON-LD Parsing. Das legt einen gemeinsamen **MetadataExtraction-Port** im Domain-Layer nahe, mit konkreten Adapter-Implementierungen.

---

## 2. Kandidatenanalyse & Abhaengigkeitsgraph

### 2.1 Kandidaten aus dem Backlog

| # | Story-ID | Titel | BC | Prio (Backlog) | Size | 2x deferred? |
|---|----------|-------|----|----------------|------|------------|
| 1 | US-TRIPS-041 | Recipe Import from URL | Trips | Should | L | Ja (Iter 7 + 9) |
| 2 | US-TRIPS-061 | Import Accommodation Info from URL | Trips | Could | M | Nein |
| 3 | US-TRIPS-062 | Accommodation Poll | Trips | Could | L | Nein |
| 4 | US-TRIPS-055 | Bring App Integration | Trips | Could | L | Nein |
| 5 | US-EXP-022 | Custom Splitting per Receipt | Expense | Could | M | Nein |
| 6 | US-EXP-032 | Settlement per Category | Expense | Could | M | Nein |
| 7 | US-EXP-033 | Export Settlement as PDF | Expense | Could | M | Nein |
| 8 | US-INFRA-042 | Lighthouse CI | Infra | Should | M | Nein |
| 9 | US-IAM-051 | SMS Notifications | IAM | Could | M | Nein |
| 10 | US-IAM-052 | Notification Preferences | IAM | Could | S | Nein |

### 2.2 Abhaengigkeitsgraph

```
US-TRIPS-041 (Recipe Import)
  |
  +--- etabliert MetadataExtraction-Port + HTML-Parsing-Infrastruktur
  |
  v
US-TRIPS-061 (Accommodation Import) -- NUTZT dieselbe Infrastruktur

US-EXP-032 (Category Breakdown)
  |
  +--- unabhaengig, reines Read Model auf bestehenden Receipts

US-EXP-033 (PDF Export)
  |
  +--- abhaengig von korrektem Settlement (erledigt in Iter 9)
  +--- profitiert von Category Breakdown (US-EXP-032)

US-INFRA-042 (Lighthouse CI) -- unabhaengig, erfordert laufende Infrastruktur

US-TRIPS-062 (Poll) -- eigenes Aggregate, gross, unabhaengig
US-TRIPS-055 (Bring) -- externe API, gross, unabhaengig
US-EXP-022 (Custom Splitting) -- aendert Settlement-Kern, riskant
US-IAM-051 (SMS) -- externe SMS-Gateway-Abhaengigkeit
```

### 2.3 Bewertungsmatrix

| Story | Value | Risk | Synergie | Technische Schuld | Empfehlung |
|-------|-------|------|----------|--------------------|------------|
| US-TRIPS-041 | HOCH (2x deferred, User-Wunsch) | MITTEL (neues Adapter-Muster) | Etabliert Import-Infra | 0 | MUSS |
| US-TRIPS-061 | HOCH (nutzt Import-Infra) | NIEDRIG (wenn 041 zuerst) | Nutzt MetadataExtraction | 0 | SOLL |
| US-EXP-032 | MITTEL (Nice-to-have) | NIEDRIG (reines Read Model) | 0 | 0 | SOLL |
| US-EXP-033 | MITTEL (Sharing-Feature) | NIEDRIG (Template-Rendering) | Profitiert von 032 | 0 | SOLL |
| US-INFRA-042 | MITTEL (CI-Qualitaet) | NIEDRIG | 0 | 0 | KANN |
| US-TRIPS-062 | MITTEL | HOCH (neues Aggregate) | 0 | 0 | DEFERRED |
| US-TRIPS-055 | NIEDRIG | HOCH (externe API) | 0 | 0 | DEFERRED |
| US-EXP-022 | NIEDRIG | HOCH (Settlement-Kern) | 0 | 0 | DEFERRED |

---

## 3. Empfohlener Iterationsplan

### ITERATION 10 (v0.10.0) -- "Import-Infrastruktur + Settlement-Polish"

**Leitmotiv**: Erstmals externe Datenquellen anbinden (Recipe + Accommodation URL Import), die geteilte Import-Infrastruktur etablieren, und das Settlement mit Kategorie-Aufschluesselung und PDF-Export abrunden.

| # | ID | Story | Size | SCS | Begruendung |
|---|---|-------|------|-----|-------------|
| S10-A | US-TRIPS-041 | Recipe Import from URL (schema.org/Recipe) | L | Trips | 2x deferred, hoechste Prioritaet. Etabliert MetadataExtraction-Port. |
| S10-B | US-TRIPS-061 | Import Accommodation Info from URL (Open Graph) | M | Trips | Nutzt S10-A Infrastruktur. Erweitert Accommodation. |
| S10-C | US-EXP-032 | Settlement per Category (Kategorie-Aufschluesselung) | M | Expense | Party-Level Settlement steht, jetzt Aufschluesselung moeglich. |
| S10-D | US-EXP-033 | Export Settlement as PDF | M | Expense | Profitiert von S10-C. Schliesst Sharing-Luecke. |
| S10-E | US-INFRA-042 | Lighthouse CI Integration | S* | Infra | PWA steht, jetzt messen. *Reduziert auf S: nur CI-Pipeline, keine Fixes.* |

**Gesamtumfang**: 1L + 3M + 1S -- konsistent mit Iteration 7-9 Velocity.

### Empfohlene Reihenfolge

```
S10-A (Recipe Import)            -- L, etabliert MetadataExtraction-Port + Adapter
  |                                  Umfasst:
  |                                  1. MetadataExtractionPort im Domain-Layer
  |                                  2. RecipeImportAdapter (HTML Fetch + JSON-LD Parse)
  |                                  3. RecipeImportService Application Service
  |                                  4. Import-UI mit URL-Eingabe + Preview + Uebernahme
  |                                  5. Fallback auf manuelle Eingabe
  |
  v
S10-B (Accommodation URL Import) -- M, nutzt dieselbe Infrastruktur
  |                                  Umfasst:
  |                                  1. AccommodationImportAdapter (Open Graph + schema.org)
  |                                  2. AccommodationImportService
  |                                  3. Import-UI auf Accommodation-Seite
  |
  v
S10-C (Category Breakdown)      -- M, reines Read Model, kein Cross-SCS Impact
  |
  v
S10-D (PDF Export)              -- M, nutzt Category Breakdown + Settlement
  |
  v
S10-E (Lighthouse CI)           -- S, nach UI-Aenderungen am Ende
```

### Begruendung

1. **S10-A ist ueberfaellig**: Recipe Import wurde zweimal verschoben (Iteration 7, 9). Der Nutzer wuenscht dieses Feature explizit. Es etabliert zudem die technische Grundlage (MetadataExtraction) fuer S10-B.

2. **S10-B nutzt Synergie**: Accommodation URL Import verwendet denselben HTTP-Fetch + Parsing-Mechanismus. Nach S10-A ist der Aufwand deutlich reduziert -- lediglich ein neuer Parser fuer Open Graph / schema.org/LodgingBusiness.

3. **S10-C und S10-D runden Expense ab**: Nach der grossen Party-Level-Umstellung in Iteration 9 sind Kategorie-Aufschluesselung und PDF-Export logische Ergaenzungen. Beide erfordern keine Domain-Umbauten.

4. **S10-E ist minimaler CI-Aufwand**: Lighthouse CI als GitHub Action einrichten, keine Page-Optimierungen in dieser Iteration.

---

## 4. Design-Level EventStorming

### 4.1 S10-A: Recipe Import from URL

#### Commands, Aggregates, Events

| Command | Actor | Aggregate | Event(s) | Policy |
|---------|-------|-----------|----------|--------|
| ImportRecipeFromUrl(tenantId, url) | Organizer/Participant | -- (kein Aggregate, nur Port) | -- | Adapter fetcht URL, parst JSON-LD |
| PreviewImportedRecipe(tenantId, extractedData) | Organizer/Participant | -- (UI-Zwischenschritt) | -- | Nutzer prueft und korrigiert |
| ConfirmRecipeImport(tenantId, name, servings, ingredients) | Organizer/Participant | Recipe | (kein neues Event noetig) | Normaler Recipe.create() |

#### Domain Event Timeline

```
                    TRIPS SCS
                    ---------
    Nutzer oeffnet Rezept-Import-Seite
              |
              v
    [1] ImportRecipeFromUrl(url="https://chefkoch.de/rezepte/123")
              |
              v
    RecipeImportService ruft MetadataExtractionPort auf
              |
              v
    RecipeImportAdapter:
      1. HTTP GET url
      2. HTML parsen
      3. <script type="application/ld+json"> suchen
      4. JSON-LD mit @type="Recipe" filtern
      5. Felder extrahieren: name, recipeYield (servings),
         recipeIngredient[] (Ingredient-Strings)
      6. Ingredient-Strings parsen: "500 g Mehl" -> Ingredient("Mehl", 500, "g")
              |
              v
    [2] RecipeImportResult zurueckgeben:
        - name: "Apfelkuchen"
        - servings: 4
        - ingredients: [Ingredient("Mehl", 500, "g"), Ingredient("Zucker", 200, "g"), ...]
        ODER:
        - ImportFailure("Keine schema.org/Recipe-Daten gefunden")
              |
              v
    [3] Nutzer sieht Preview: Name, Portionen, Zutatenliste
        Nutzer kann Felder korrigieren (Name aendern, Zutat entfernen, hinzufuegen)
              |
              v
    [4] ConfirmRecipeImport
              |
              v
    Recipe.create(tenantId, name, servings, ingredients)
        -> Normaler Persistenz-Flow (kein neues Domain Event)
```

#### Neues Port-Interface im Domain-Layer

```
RecipeImportPort (Interface in domain/recipe/)
  +-- extractRecipe(url: String) -> RecipeImportResult

RecipeImportResult (Value Object in domain/recipe/)
  +-- success: boolean
  +-- name: String (nullable)
  +-- servings: Integer (nullable)
  +-- ingredients: List<ParsedIngredient> (kann leer sein)
  +-- errorMessage: String (nullable)

ParsedIngredient (Value Object in domain/recipe/)
  +-- name: String
  +-- quantity: BigDecimal (nullable, wenn nicht parsbar)
  +-- unit: String (nullable, wenn nicht parsbar)
  +-- rawText: String (Original-String fuer Fallback-Anzeige)
```

#### Adapter-Implementierung

```
RecipeImportAdapter (in adapters/integration/)
  +-- Implementiert RecipeImportPort
  +-- Abhaengigkeiten:
      - java.net.http.HttpClient (Java 21, kein externer HTTP Client noetig)
      - Jsoup (HTML Parsing, schema.org JSON-LD Extraktion)
  +-- @Profile("!test") -- in Tests gemockt
  +-- Timeout: 10 Sekunden
  +-- User-Agent: "Travelmate/0.10.0 RecipeImport"
```

#### Ingredient-Parsing-Strategie

schema.org/Recipe liefert Zutaten als `recipeIngredient: ["500 g Mehl", "200 g Zucker", "3 Eier"]`. Diese Strings muessen in strukturierte Ingredients geparst werden.

```
IngredientParser (Domain Service in domain/recipe/)
  +-- parse(rawIngredient: String) -> ParsedIngredient

  Strategie:
  1. Regex-basiertes Parsing: "(\d+[\.,]?\d*)\s*(g|kg|ml|l|Stueck|EL|TL|Prise|Bund|Packung)\s+(.+)"
  2. Fallback: Wenn Regex nicht matcht -> rawText uebernehmen, quantity=null, unit=null
  3. Nutzer kann im Preview manuell korrigieren

  Beispiele:
    "500 g Mehl"       -> ParsedIngredient("Mehl", 500, "g", "500 g Mehl")
    "3 Eier"           -> ParsedIngredient("Eier", 3, "Stueck", "3 Eier")
    "etwas Salz"       -> ParsedIngredient("etwas Salz", null, null, "etwas Salz")
    "1,5 kg Kartoffeln" -> ParsedIngredient("Kartoffeln", 1.5, "kg", "1,5 kg Kartoffeln")
```

#### Hot Spots

```
[HS-10-1] Ingredient-Parsing Zuverlaessigkeit (MEDIUM)
  Problem: schema.org Ingredient-Strings sind nicht standardisiert.
  Verschiedene Websites formatieren anders: "500g Mehl" vs "500 g Mehl" vs "Mehl (500 g)".
  Mitigation: Robuste Regex + Fallback auf rawText. Preview erlaubt manuelle Korrektur.
  Risiko: Parser deckt ~80% der gaengigen Formate ab. Restliche 20% manuell.

[HS-10-2] Website-Verfuegbarkeit und Blocking (MEDIUM)
  Problem: Manche Websites blocken Bot-Requests oder haben keinen JSON-LD.
  Mitigation: User-Agent Header, Timeout, klare Fehlermeldung.
  Akzeptanz: Import ist Best-Effort, kein garantiertes Feature.

[HS-10-3] Security: Server-Side Request Forgery (SSRF) (HOCH)
  Problem: Nutzer gibt beliebige URLs ein -> Server fetcht diese.
  Mitigation:
  1. Nur HTTPS-URLs akzeptieren
  2. URL-Validierung: kein localhost, keine privaten IP-Bereiche (10.x, 192.168.x, 172.16-31.x)
  3. DNS-Rebinding-Schutz: Resolved IP vor Fetch pruefen
  4. Response-Size-Limit (max 2 MB)
  5. Timeout 10s
  -> ADR-Kandidat!
```

#### Invarianten

```
INV-RI1: URL muss gueltiges HTTPS-Format haben
INV-RI2: URL darf nicht auf localhost oder private IPs zeigen (SSRF-Schutz)
INV-RI3: Response-Groesse <= 2 MB
INV-RI4: Mindestens Name muss extrahierbar sein, sonst Fehler
INV-RI5: Nutzer kann IMMER manuell korrigieren (Import ist Vorschlag, nicht Pflicht)
```

### 4.2 S10-B: Import Accommodation Info from URL

#### Commands, Aggregates, Events

| Command | Actor | Aggregate | Event(s) | Policy |
|---------|-------|-----------|----------|--------|
| ImportAccommodationFromUrl(accommodationId, url) | Organizer | -- (Port-Aufruf) | -- | Adapter fetcht URL, parst OG+schema.org |
| PreviewImportedAccommodation(extractedData) | Organizer | -- (UI-Zwischenschritt) | -- | Nutzer prueft |
| ApplyImportedAccommodationData(accommodationId, name, address, url, ...) | Organizer | Accommodation | AccommodationPriceSet (wenn Preis extrahiert) | Accommodation.updateDetails() |

#### Domain Event Timeline

```
                    TRIPS SCS
                    ---------
    Organizer ist auf Accommodation-Detail-Seite
              |
              v
    [1] ImportAccommodationFromUrl(url="https://booking.com/hotel/de/berghuette-xyz")
              |
              v
    AccommodationImportService ruft MetadataExtractionPort auf
              |
              v
    AccommodationImportAdapter:
      1. HTTP GET url
      2. HTML parsen
      3. Open Graph Tags extrahieren:
         - og:title -> Name
         - og:description -> Beschreibung
         - og:image -> Vorschaubild-URL (nur anzeigen, nicht speichern)
         - og:url -> Canonical URL
      4. schema.org/LodgingBusiness oder Hotel suchen:
         - name, address, description
         - priceRange (wenn verfuegbar)
         - geo (Koordinaten, optional)
      5. Fallback: Nur <title> + meta description
              |
              v
    [2] AccommodationImportResult zurueckgeben:
        - name: "Berghuette XYZ"
        - address: "Musterstrasse 1, 83700 Rottach-Egern"
        - description: "Gemutliche Huette mit Bergblick..."
        - imageUrl: "https://..."  (nur Anzeige im Preview)
        - url: "https://booking.com/hotel/de/berghuette-xyz"
        ODER:
        - ImportFailure("Keine verwertbaren Metadaten gefunden")
              |
              v
    [3] Organizer sieht Preview: Name, Adresse, Beschreibung, Vorschaubild
        Organizer kann Felder korrigieren
              |
              v
    [4] ApplyImportedAccommodationData
              |
              v
    Accommodation.updateDetails(name, address, url, checkIn, checkOut, totalPrice)
        -> AccommodationPriceSet Event nur wenn Preis aus Import (selten)
        -> Normalerweise nur Name + Adresse + URL uebernommen
```

#### Neues Port-Interface im Domain-Layer

```
AccommodationImportPort (Interface in domain/accommodation/)
  +-- extractAccommodationInfo(url: String) -> AccommodationImportResult

AccommodationImportResult (Value Object in domain/accommodation/)
  +-- success: boolean
  +-- name: String (nullable)
  +-- address: String (nullable)
  +-- description: String (nullable)
  +-- imageUrl: String (nullable, nur fuer Preview)
  +-- sourceUrl: String
  +-- errorMessage: String (nullable)
```

#### Geteilte Infrastruktur mit S10-A

```
Shared Utilities (in adapters/integration/):

  HtmlFetcher -- HTTP GET mit SSRF-Schutz + Timeout + Size-Limit
    +-- fetch(url: String) -> FetchResult(body: String, statusCode: int)
    +-- Wiederverwendet von RecipeImportAdapter UND AccommodationImportAdapter

  JsonLdExtractor -- Extrahiert JSON-LD <script> Bloecke aus HTML
    +-- extract(html: String) -> List<JsonNode>
    +-- Wiederverwendet von beiden Adaptern

  OpenGraphExtractor -- Extrahiert og: Meta-Tags
    +-- extract(html: String) -> Map<String, String>
    +-- Nur von AccommodationImportAdapter genutzt
```

#### Hot Spots

```
[HS-10-4] Open Graph Vollstaendigkeit (NIEDRIG)
  Problem: Nicht alle Buchungsseiten haben og: Tags oder schema.org/LodgingBusiness.
  Mitigation: Fallback-Kette: schema.org -> Open Graph -> <title> + meta description.
  Akzeptanz: Best-Effort. Hauptsaechlich fuer grosse Plattformen (Booking, Airbnb, FeWo-direkt).
```

### 4.3 S10-C: Settlement per Category (Kategorie-Aufschluesselung)

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

CategoryBreakdown (Value Object) -- NEU
  +-- totalAmount: BigDecimal
  +-- categories: List<CategoryShare>

CategoryShare (Value Object) -- NEU
  +-- category: ExpenseCategory
  +-- amount: BigDecimal
  +-- percentage: BigDecimal
  +-- receiptCount: int
```

Dies ist ein reines Read Model -- keine Events, keine Persistenz-Aenderung, keine Cross-SCS Kommunikation.

### 4.4 S10-D: Export Settlement as PDF

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

#### Abhaengigkeiten (neue Maven Dependencies)

```xml
<!-- travelmate-expense/pom.xml -->
<dependency>
    <groupId>org.xhtmlrenderer</groupId>
    <artifactId>flying-saucer-pdf</artifactId>
    <version>9.11.3</version>
</dependency>
```

#### Hot Spots

```
[HS-10-5] PDF-Rendering-Qualitaet (NIEDRIG)
  Problem: HTML-zu-PDF-Konvertierung hat Einschraenkungen bei CSS-Features.
  Mitigation: Einfaches Tabellen-basiertes Layout, keine komplexen CSS-Features.
  Standard: Lesbar, korrekt, nicht "designt".

[HS-10-6] Thymeleaf Standalone vs. Web (NIEDRIG)
  Problem: PDF-Template darf nicht mit Spring MVC Context gekoppelt sein.
  Loesung: Standalone TemplateEngine konfigurieren, Template im classpath.
```

### 4.5 S10-E: Lighthouse CI Integration

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

## 5. Cross-SCS Event Design

### 5.1 Keine neuen Cross-SCS Events

Iteration 10 fuehrt **keine neuen Event-Contracts** ein. Alle Features arbeiten innerhalb ihrer jeweiligen Bounded Contexts:

- S10-A/B: Trips-internes Import-Feature (kein Event nach aussen)
- S10-C/D: Expense-internes Read Model + PDF Export
- S10-E: Infrastructure only

Dies ist ein Indikator fuer eine gut ausbalancierte Iteration: keine Cross-SCS-Kopplungen, keine Contract-Aenderungen, somit geringeres Integrationsrisiko.

### 5.2 Bestehende Events (unveraendert)

```
ParticipantJoinedTrip(tenantId, tripId, participantId, username, participantTenantId, partyName, occurredOn)
AccommodationPriceSet(tenantId, tripId, totalPrice, occurredOn)
TripCreated, TripCompleted, StayPeriodUpdated -- alle unveraendert
```

---

## 6. Aggregate Design (Aenderungen)

### 6.1 Recipe (Trips SCS) -- ERWEITERT

```
Recipe (AggregateRoot) -- unveraendert
  +-- RecipeId, TenantId, name, servings, ingredients

RecipeImportPort (NEU, Interface in domain/recipe/)
  +-- extractRecipe(url: String) -> RecipeImportResult

RecipeImportResult (NEU, Record in domain/recipe/)
  +-- success: boolean
  +-- name: String?
  +-- servings: Integer?
  +-- ingredients: List<ParsedIngredient>
  +-- errorMessage: String?

ParsedIngredient (NEU, Record in domain/recipe/)
  +-- name: String
  +-- quantity: BigDecimal?
  +-- unit: String?
  +-- rawText: String

IngredientParser (NEU, Domain Service in domain/recipe/)
  +-- parse(rawIngredient: String) -> ParsedIngredient
  +-- parseAll(rawIngredients: List<String>) -> List<ParsedIngredient>
```

### 6.2 Accommodation (Trips SCS) -- ERWEITERT

```
Accommodation (AggregateRoot) -- unveraendert in Struktur

AccommodationImportPort (NEU, Interface in domain/accommodation/)
  +-- extractAccommodationInfo(url: String) -> AccommodationImportResult

AccommodationImportResult (NEU, Record in domain/accommodation/)
  +-- success: boolean
  +-- name: String?
  +-- address: String?
  +-- description: String?
  +-- imageUrl: String?
  +-- sourceUrl: String
  +-- errorMessage: String?
```

### 6.3 Expense (Expense SCS) -- ERWEITERT

```
Expense (AggregateRoot) -- neue Methode
  +-- calculateCategoryBreakdown() -> CategoryBreakdown  NEU

CategoryBreakdown (NEU, Record in domain/expense/)
  +-- totalAmount: BigDecimal
  +-- categories: List<CategoryShare>

CategoryShare (NEU, Record in domain/expense/)
  +-- category: ExpenseCategory
  +-- amount: BigDecimal
  +-- percentage: BigDecimal
  +-- receiptCount: int
```

---

## 7. Flyway Migrationen

### Trips SCS

Keine Schema-Aenderung erforderlich. Recipe und Accommodation Tables existieren bereits. Die Import-Ports aendern nur die Adapter-Schicht.

### Expense SCS

Keine Schema-Aenderung erforderlich. CategoryBreakdown ist ein berechnetes Read Model, kein persistiertes Feld. PDF wird on-the-fly generiert.

---

## 8. Zusammenfassung der Code-Aenderungen

### travelmate-common (Shared Kernel)

**Keine Aenderungen.** Kein neues Event, kein neuer Routing Key.

### travelmate-trips (Trips SCS)

**Neue Domain-Typen**:
- `RecipeImportPort` (Interface in `domain/recipe/`)
- `RecipeImportResult` (Record in `domain/recipe/`)
- `ParsedIngredient` (Record in `domain/recipe/`)
- `IngredientParser` (Domain Service in `domain/recipe/`)
- `AccommodationImportPort` (Interface in `domain/accommodation/`)
- `AccommodationImportResult` (Record in `domain/accommodation/`)

**Neue Adapter** (in `adapters/integration/`):
- `HtmlFetcher` -- HTTP GET mit SSRF-Schutz, Timeout, Size-Limit
- `JsonLdExtractor` -- JSON-LD Extraktion aus HTML
- `OpenGraphExtractor` -- Open Graph Meta-Tag Extraktion
- `RecipeImportAdapter` -- Implementiert `RecipeImportPort`
- `AccommodationImportAdapter` -- Implementiert `AccommodationImportPort`

**Neue Application Services**:
- `RecipeImportService` -- Orchestriert Import-Flow
- `AccommodationImportService` -- Orchestriert Import-Flow

**Neue Web-Adapter**:
- `RecipeImportController` -- Import-UI Endpoints (GET form, POST import, POST confirm)
- `AccommodationController` erweitert -- Import-Endpoint

**Neue Maven Dependencies**:
- `org.jsoup:jsoup:1.18.3` -- HTML Parsing

**Neue Templates**:
- `recipe/import.html` -- URL-Eingabe + Preview + Uebernahme
- `accommodation/import-preview.html` (HTMX fragment) -- Import-Vorschau

**Flyway**: Keine neue Migration.

### travelmate-expense (Expense SCS)

**Neue Domain-Typen**:
- `CategoryBreakdown` (Record in `domain/expense/`)
- `CategoryShare` (Record in `domain/expense/`)

**Neue Methode auf Expense**:
- `Expense.calculateCategoryBreakdown()` -> `CategoryBreakdown`

**Neue Application Services**:
- `SettlementPdfService` -- Orchestriert PDF-Generierung

**Neue Ports**:
- `SettlementPdfPort` (Interface in `application/`)

**Neue Adapter**:
- `SettlementPdfAdapter` (in `adapters/pdf/`) -- Thymeleaf + Flying Saucer

**Neue Web-Adapter**:
- `SettlementController` erweitert -- GET `/expense/{tripId}/settlement/pdf`
- `SettlementController` erweitert -- GET `/expense/{tripId}/categories` (HTMX partial)

**Neue Maven Dependencies**:
- `org.xhtmlrenderer:flying-saucer-pdf:9.11.3`

**Neue Templates**:
- `expense/categories.html` -- Kategorie-Aufschluesselung (partial)
- `pdf/settlement-report.html` -- PDF-Template

**Flyway**: Keine neue Migration.

### travelmate-gateway

**Keine Aenderungen.**

### travelmate-e2e

**Neue E2E Tests**:
- Recipe Import: URL eingeben, Preview pruefen, bestaetigen, Rezept vorhanden
- Accommodation Import: URL auf Accommodation-Seite, Preview, Uebernahme
- Category Breakdown: Sichtbar auf Settlement-Seite
- PDF Export: Download-Link funktioniert (Content-Type pruefen)

**Neue BDD Scenarios** (Cucumber):
- `recipe-import.feature` -- 3-4 Szenarien (Erfolg, Fehler-URL, Fallback manuell)
- `settlement-export.feature` -- 2 Szenarien (PDF Download, Kategorie-Ansicht)

---

## 9. Risikobewertung

| Risiko | Schwere | Wahrscheinlichkeit | Mitigation |
|--------|---------|---------------------|------------|
| SSRF ueber URL-Import-Endpunkt | HOCH | NIEDRIG | URL-Validierung, IP-Blacklist, HTTPS-only, Size-Limit, Timeout |
| Ingredient-Parser deckt Randformate nicht ab | MITTEL | MITTEL | Fallback auf rawText + manuelle Korrektur im Preview |
| Jsoup-Abhaengigkeit bringt Sicherheitsrisiken | MITTEL | NIEDRIG | Aktuelle Version, OWASP-Check, nur Server-seitig |
| PDF-Rendering hat Layout-Probleme | NIEDRIG | MITTEL | Einfaches Tabellen-Layout, manuelle Tests |
| Lighthouse Scores unter Threshold | NIEDRIG | MITTEL | Keine Build-Failure in Iter 10, nur Reporting |

---

## 10. ADR-Kandidaten

| ADR | Thema | Trigger |
|-----|-------|---------|
| ADR-0016 | URL-basierter Import mit SSRF-Schutz -- MetadataExtraction-Port im Hexagonal Pattern | HS-10-3, S10-A/B |
| ADR-0017 | PDF-Generierung via Thymeleaf + Flying Saucer (Alternative: iText, Apache FOP) | S10-D |

**Hinweis**: ADR-0016 war in Iteration 9 fuer "Accommodation als eigenes Aggregate" reserviert. Wenn dieses ADR bereits geschrieben wurde, beginnt die Nummerierung hier bei ADR-0019. Die korrekte Nummer muss vor dem Schreiben geprueft werden.

---

## 11. Deferred (Iteration 11+)

| Story | Begruendung |
|-------|-------------|
| US-TRIPS-062: Accommodation Poll | Eigenes Aggregate (LocationPoll), grosse Feature-Iteration |
| US-EXP-022: Custom Splitting per Receipt | Aendert Settlement-Kern, erst nach Stabilisierung |
| US-TRIPS-055: Bring App Integration | Externe API-Abhaengigkeit, separate Iteration |
| US-IAM-051: SMS Notifications | Externe SMS-Gateway-Abhaengigkeit (Twilio) |
| US-IAM-052: Notification Preferences | Abhaengig von SMS (US-IAM-051) |
| US-INFRA-055: Transactional Outbox | XL, eigene Infrastruktur-Iteration |

---

## 12. Naechste Schritte

1. **ADRs schreiben**: ADR fuer URL-Import mit SSRF-Schutz, ADR fuer PDF-Generierung
2. **Story Refinement**: Detaillierte Akzeptanzkriterien fuer S10-A bis S10-E
3. **UX Wireframes**: Recipe-Import-Seite (URL + Preview), Accommodation-Import, Kategorie-Ansicht, PDF-Layout
4. **Spike S10-A**: Jsoup + JSON-LD Parsing gegen chefkoch.de, eatsmarter.de, lecker.de testen
5. **SSRF-Schutz implementieren**: URL-Validator mit IP-Blacklist als erstes
6. **TDD fuer Domain**:
   - Trips: IngredientParser (Regex-Matching), RecipeImportResult (Value Object)
   - Expense: calculateCategoryBreakdown() (Aggregation, Prozent-Berechnung)
7. **E2E-Tests**: Import-Flow, Category-View, PDF-Download
8. **Lighthouse Baseline**: Aktuelle Scores messen vor CI-Integration
