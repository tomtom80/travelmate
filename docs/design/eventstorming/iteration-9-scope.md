# Design-Level EventStorming: Iteration 9 -- Revidierte Scope-Analyse & Planung

**Datum**: 2026-03-18 (Revision)
**Scope**: Cross-SCS Analyse -- Accommodation, TravelParty-Level Settlement, Advance Payments
**Methode**: Design-Level EventStorming (Alberto Brandolini)
**Bounded Contexts**: Trips (Core), Expense (Generic), IAM (Supporting)
**Anlass der Revision**: Neue Domaen-Erkenntnisse -- Abrechnung erfolgt pro Reisepartei, nicht pro Person. Unterkunft ist zentrales Planungselement mit Zimmerbelegung.

---

## 1. Bestandsaufnahme (Ist-Zustand nach Iteration 8)

### 1.1 Trips SCS -- Aggregate und Events

```
Trip (AggregateRoot)
  +-- TripId, TenantId
  +-- name: TripName, description: String
  +-- dateRange: DateRange(startDate, endDate)
  +-- organizerId: UUID
  +-- status: TripStatus (PLANNING | CONFIRMED | IN_PROGRESS | COMPLETED | CANCELLED)
  +-- participants: List<Participant(participantId, firstName, lastName, stayPeriod)>

Invitation (AggregateRoot)
  +-- InvitationId, TripId
  +-- type: MEMBER | EXTERNAL
  +-- status: PENDING | ACCEPTED | DECLINED | AWAITING_REGISTRATION

TravelParty (Projection/ReadModel)
  +-- TenantId, members: List<Member(memberId, email, firstName, lastName)>,
  +-- dependents: List<TravelPartyDependent(dependentId, guardianMemberId, firstName, lastName)>

Recipe (AggregateRoot), MealPlan (AggregateRoot), ShoppingList (AggregateRoot)
  ... (unveraendert)
```

### 1.2 Expense SCS -- Aggregate und Events

```
Expense (AggregateRoot)
  +-- ExpenseId, TenantId, tripId: UUID
  +-- status: ExpenseStatus (OPEN | READY_FOR_SETTLEMENT | SETTLED)
  +-- reviewRequired: boolean
  +-- receipts: List<Receipt>
        +-- ReceiptId, description, amount: Amount, paidBy: UUID, submittedBy: UUID
        +-- date, category: ExpenseCategory, reviewStatus: ReviewStatus
        +-- reviewerId?, rejectionReason?
  +-- weightings: List<ParticipantWeighting(participantId: UUID, weight: BigDecimal)>

TripProjection (ReadModel)
  +-- tripId, tenantId, tripName, startDate, endDate
  +-- participants: List<TripParticipant(participantId, name, arrivalDate?, departureDate?)>
```

### 1.3 Event-Contracts (travelmate-common)

```
IAM publiziert:
  TenantCreated, AccountRegistered, MemberAddedToTenant, DependentAddedToTenant,
  DependentRemovedFromTenant, MemberRemovedFromTenant, TenantDeleted,
  RoleAssignedToUser, RoleUnassignedFromUser

Trips publiziert:
  TripCreated, ParticipantJoinedTrip(tenantId, tripId, participantId, username, occurredOn)
  TripCompleted, StayPeriodUpdated

Expense publiziert:
  ExpenseCreated, ExpenseSettled
```

### 1.4 KRITISCHE GAP-ANALYSE: Abrechnungseinheit

Die folgenden Erkenntnisse stammen aus der Domaenenanalyse mit dem Nutzer:

**Mentales Modell des Nutzers (Soll)**:
- Abrechnung erfolgt pro **Reisepartei** (TravelParty/Tenant), NICHT pro Einzelperson
- Jede Reisepartei = 1 Familie (z.B. 2 Erwachsene + 2 Kinder)
- Gewichtung: Erwachsene = 1.0, Kinder nach Alter (0.0 bis 0.5)
- Alle Kosten fliessen in einen **gemeinsamen Kostenpool** (Unterkunft + Belege)
- Anteil einer Partei = (Partei-Gesamtgewicht / Gesamt-Gewicht) x Pool-Summe
- Belege sind **Gutschriften** fuer die einreichende Partei gegen den Pool
- Vorauszahlungen sind ebenfalls Gutschriften fuer die zahlende Partei

**Ist-Zustand der Implementierung (Defizite)**:

| Aspekt | Soll (Nutzer-Modell) | Ist (Code) | Gap |
|--------|---------------------|------------|-----|
| Abrechnungseinheit | TravelParty (Familie) | Einzelperson (UUID) | KRITISCH |
| ParticipantWeighting | Pro TravelParty (summiert ueber Members+Dependents) | Pro participantId (immer 1.0) | KRITISCH |
| calculateBalances() | Saldo pro TravelParty | Saldo pro participantId | KRITISCH |
| SettlementPlan.Transfer | Party-to-Party | Person-to-Person | KRITISCH |
| Receipt.paidBy | Person bezahlt, Gutschrift fuer deren Party | Person bezahlt, Gutschrift fuer diese Person | OK (aggregierbar) |
| TripProjection | Kennt TravelParty-Zugehoerigkeit | Nur flache participantId + name | LUECKE |
| ParticipantJoinedTrip Event | Muesste tenantId des Teilnehmers mitliefern | Liefert nur participantId + username | LUECKE |

**Kernproblem**: Das Expense SCS hat KEIN Konzept von "TravelParty". Teilnehmer sind flache UUIDs ohne Gruppierung. Der Settlement-Algorithmus verteilt Kosten auf Individuen, nicht auf Familien. Die `ParticipantWeighting` wird pauschal mit 1.0 angelegt (Zeile 94 in ExpenseService).

---

## 2. Hot Spots (Red Stickies)

### HS-1: Abrechnungseinheit -- Per-Person vs. Per-TravelParty (KRITISCH)

**Frage**: Wie ueberbruecken wir die Luecke zwischen individuellem Settlement und dem Partei-basierten mentalen Modell?

**Option A: Presentation-Layer Aggregation (minimal, schnell)**
- `calculateBalances()` bleibt per-Person
- Neues Read Model `PartyProjection` im Expense SCS mit Mapping participantId -> partyId
- `calculatePartyBalances()` aggregiert individuelle Balancen zu Party-Balancen
- `SettlementPlan` generiert Party-to-Party Transfers
- Pro: Bestehende Domain-Logik unveraendert, nur Darstellungsschicht
- Contra: Gewichtung ist immer noch pro Person, nicht pro Party; "Partei zahlt 1/3" ergibt sich nur indirekt

**Option B: Party-Aware Settlement (korrekt, groesser)**
- `ParticipantWeighting` wird ersetzt durch `PartyWeighting(partyId: UUID, weight: BigDecimal)`
- Oder: `ParticipantWeighting` erhaelt zusaetzlich `partyId: UUID`
- `calculateBalances()` arbeitet nativ auf Party-Ebene
- `Receipt.paidBy` bleibt Person-UUID, wird bei Berechnung der Party zugeordnet
- Pro: Domain bildet reale Welt korrekt ab
- Contra: Umbau von Expense-Kern, Migration bestehender Daten

**Option C: Hybrid -- PartyWeighting + Person-Tracking (empfohlen)**
- Neues Konzept `PartyWeighting(partyId: UUID, partyName: String, totalWeight: BigDecimal, memberIds: List<UUID>)`
- `calculateBalances()` berechnet auf Party-Ebene
- `Receipt.paidBy` (Person-UUID) wird via `memberIds` der richtigen Party zugeordnet
- `SettlementPlan.Transfer(fromPartyId, toPartyId, amount)` -- Party-to-Party
- UI zeigt: "Familie Mueller schuldet Familie Schmidt 45.00 EUR"
- Pro: Korrekte Domaene, klare Zuordnung, zukunftssicher
- Contra: Groesserer Umbau als Option A

**Entscheidung**: **Option C -- Hybrid mit PartyWeighting** fuer Iteration 9.

**Begruendung**: Option A ist ein Workaround, der die Domain falsch modelliert. Die Gewichtung MUSS auf Party-Ebene sein (Kind gehoert zur Familie, nicht zum Trip), weil nur so der Nutzer die Gewichte sinnvoll setzen kann ("Familie Mueller: 2 Erw. + 2 Kinder = 2.5"). Option B ist zu invasiv ohne Mehrwert gegenueber C. Option C modelliert die reale Welt korrekt und ist erweiterbar.

### HS-2: Accommodation -- Nicht nur Info-Feld, sondern Planungszentrale (HOCH)

**Revision gegenueber Erstanalyse**: Accommodation ist KEIN einfaches Value Object mit Name/Adresse/URL. Es ist das zentrale Planungselement mit:

- **Zimmerbelegung**: 4-Bett-Zimmer, 2-Bett-Zimmer, Matratzenlager -- Familien werden Zimmern zugeordnet
- **Preis**: Gesamtpreis der Unterkunft treibt Vorauszahlungen
- **Ausstattung**: Sauna, Garten, Bergblick, Einkaufsmoeglichkeit, TV, Gemeinschaftsraeume, Gemeinschaftskueche -- spaeter fuer Abstimmungen relevant
- **Zimmer-Zuweisung**: Welche Reisepartei in welchem Zimmer

**Designfrage**: Entity in Trip vs. eigenes Aggregate?

**Revidierte Empfehlung**: **Eigenes Aggregate `Accommodation`** (Revision der Erstanalyse!).

Begruendung:
1. Accommodation hat einen eigenstaendigen Lebenszyklus (wird vor der Reise geplant, unabhaengig von Trip-Status)
2. Zimmer-Zuweisungen sind eine eigene Invariante (Bettenzahl >= zugewiesene Personen)
3. Das Aggregate wuerde in Trip zu gross werden (Participants + Rooms + Amenities + Assignments)
4. Spaeter kommt AccommodationPoll dazu -- dann referenziert Poll -> Accommodation(s)
5. Zimmerbelegung referenziert TravelParty-IDs (tenantIds) -- Cross-Aggregate Referenz

**Aber fuer MVP-Scope in Iteration 9**: Nur die Basisstruktur (Rooms, Pricing). Zimmer-Zuweisungen und Amenities werden in Iteration 10+ ergaenzt.

### HS-3: Vorauszahlung -- Gleicher Rundbetrag pro Reisepartei (HOCH)

**Revision (2. Revision)**: Eine Vorauszahlung ist ein GLEICHER RUNDBETRAG pro Reisepartei. KEINE gewichtete Verteilung, KEINE individuelle Berechnung pro Partei. Der Flow ist:

1. Unterkunftspreis steht fest (z.B. 3000 EUR fuer 3 Parteien)
2. System schlaegt vor: ceil(Unterkunftspreis / Anzahl Parteien / 50) x 50 = Rundbetrag
   Beispiel: ceil(3000 / 3 / 50) x 50 = ceil(20) x 50 = 1000 EUR pro Partei
3. Organizer kann den Betrag anpassen (z.B. 1100 EUR fuer Puffer)
4. ALLE Parteien zahlen DENSELBEN Betrag (z.B. jede Familie 1100 EUR)
5. Bei Abrechnung: Vorauszahlung = Flat Credit pro Partei

**Kernprinzip**: Einfachheit. Keine Gewichtung bei Vorauszahlungen. Gewichtung ist nur fuer das Settlement relevant. Die Vorauszahlung ist ein logistisches Werkzeug -- "Jede Familie ueberweist 1000 EUR vorab" -- kein anteilsmaessiges.

**Problem**: Die Auto-Suggestion erfordert:
- Unterkunftspreis (aus Accommodation, Trips SCS)
- Anzahl teilnehmender Parteien (aus TripProjection, Expense SCS)

Das ist ein Cross-SCS Workflow. Es gibt zwei Ausfuehrungsoptionen:

**Option A**: Berechnung im Expense SCS (Accommodation-Preis per Event uebertragen)
- Trips publiziert `AccommodationPriceSet(tripId, totalPrice)` -> Expense speichert Preis
- Expense kann dann: ceil(totalPrice / partyCount / 50) x 50 = Vorschlagsbetrag
- Organizer bestaetigt oder passt an
- Pro: Expense kennt die Parteien, kann den Vorschlag komplett selbst machen
- Contra: Accommodation-Preis muss per Event synchronisiert werden

**Option B**: Berechnung im Frontend (Gateway/Controller aggregiert)
- Organizer sieht Unterkunftspreis + Partei-Anzahl und rechnet manuell
- System zeigt nur den berechneten Vorschlag als Hilfe
- Pro: Kein Cross-SCS Event noetig
- Contra: Fehleranfaellig, Business-Logik im UI

**Entscheidung**: **Option A** -- `AccommodationPriceSet` Event von Trips -> Expense.
Der Vorschlagsbetrag wird vom Expense SCS berechnet: ceil(accommodationPrice / partyCount / 50) x 50. Der Organizer bestaetigt oder passt den Betrag an. Der bestaetigte Betrag gilt GLEICH fuer ALLE Parteien.

### HS-4: ParticipantJoinedTrip Event -- fehlende Party-Information (HOCH)

**Problem**: Das aktuelle Event `ParticipantJoinedTrip(tenantId, tripId, participantId, username)` liefert die `tenantId` des TRIPS (die den Trip besitzt), NICHT die `tenantId` der TravelParty des Teilnehmers. Das Expense SCS kann den Teilnehmer keiner Party zuordnen.

**Loesung**: Neues Event oder erweitertes Event:
- `ParticipantJoinedTrip` erhaelt ein zusaetzliches Feld `participantTenantId: UUID`
- ODER: Neues Event `PartyJoinedTrip(tripTenantId, tripId, partyTenantId, partyName, memberIds: List<UUID>)`

**Empfehlung**: Event erweitern um `participantTenantId`. In Trips ist der Participant mit seinem `memberId` gespeichert, und die TravelParty-Projektion kennt das Mapping memberId -> tenantId. Der TripService kann dieses Feld beim Publizieren befuellen.

### HS-5: PWA -- Service Worker Scope bei Multi-SCS Gateway (MEDIUM)

(Unveraendert aus Erstanalyse)

**Entscheidung**: Minimaler Service Worker im Gateway:
1. App Shell Caching (manifest.json, Offline-Fallback-Seite)
2. Kein Data Caching (HTMX-Content immer vom Server)
3. Offline-Fallback: Statische Seite "Keine Verbindung"

---

## 3. Aggregate Design

### 3.1 Accommodation (Neues Aggregate im Trips SCS)

```
Accommodation (AggregateRoot) -- NEU
  +-- AccommodationId: UUID
  +-- TenantId: TenantId (Tenant des Organizers)
  +-- tripId: UUID (Referenz auf Trip)
  +-- name: AccommodationName (String, nicht leer, max 200)
  +-- address: String (nullable, max 500)
  +-- url: String (nullable, URL-Format)
  +-- checkInDate: LocalDate (nullable)
  +-- checkOutDate: LocalDate (nullable)
  +-- totalPrice: BigDecimal (nullable, >= 0)
  +-- currency: String (default "EUR", max 3)
  +-- rooms: List<Room> (0..n)
  +-- amenities: List<Amenity> (0..n) -- DEFERRED zu Iteration 10+

Room (Entity innerhalb Accommodation)
  +-- RoomId: UUID
  +-- name: String (z.B. "Zimmer 1", "Matratzenlager")
  +-- roomType: RoomType (SINGLE | DOUBLE | TRIPLE | QUAD | DORMITORY)
  +-- bedCount: int (> 0)
  +-- pricePerNight: BigDecimal (nullable, >= 0)
  +-- assignedPartyId: UUID (nullable) -- TenantId der zugewiesenen Party -- DEFERRED

RoomType (Enum)
  SINGLE(1), DOUBLE(2), TRIPLE(3), QUAD(4), DORMITORY(n)
  +-- defaultBedCount: int

Methoden auf Accommodation:
  create(tenantId, tripId, name) -> Accommodation (Factory)
  updateDetails(name, address?, url?, checkIn?, checkOut?)
  setTotalPrice(totalPrice, currency)
  addRoom(name, roomType, bedCount, pricePerNight?)
  removeRoom(roomId)
  totalPrice() -> BigDecimal (manuell gesetzt ODER summiert aus rooms x nights)

Invarianten:
  INV-A1: name darf nicht leer sein
  INV-A2: checkInDate <= checkOutDate (wenn beide gesetzt)
  INV-A3: bedCount > 0 pro Room
  INV-A4: 0..1 Accommodation pro Trip (erzwungen via unique constraint tripId)

Events:
  AccommodationCreated(tenantId, tripId, accommodationId, name)
  AccommodationPriceSet(tenantId, tripId, totalPrice, currency)  -- Cross-SCS relevant!
```

**Flyway**: V11 -- Neuer Table `accommodation` + `accommodation_room`

**Iteration 9 Scope**: Basisstruktur (name, address, url, rooms, totalPrice). DEFERRED: Amenities, Room-Assignments, Room-Preis-Kalkulation.

### 3.2 Expense -- Refactoring auf PartyWeighting

```
PartyWeighting (Value Object, ersetzt ParticipantWeighting) -- NEU
  +-- partyId: UUID (= TenantId der TravelParty)
  +-- partyName: String
  +-- totalWeight: BigDecimal (>= 0, Summe der Mitgliedergewichte)
  +-- memberIds: List<UUID> (participantIds die zu dieser Party gehoeren)

Expense (AggregateRoot) -- REFACTORED
  +-- ... (bestehende Felder)
  +-- partyWeightings: List<PartyWeighting>   (ERSETZT weightings: List<ParticipantWeighting>)
  +-- accommodationCost: BigDecimal (nullable) -- aus AccommodationPriceSet Event

Aenderungen an Expense:
  create(tenantId, tripId, partyWeightings) -> Expense
  updatePartyWeighting(partyId, newWeight)
  setAccommodationCost(totalPrice)
  addReceipt(...) -- paidBy bleibt Person-UUID
  calculateBalances() -> Map<UUID, BigDecimal> -- Schluessel ist partyId, nicht participantId
    1. poolTotal = sum(receipts.amount) + accommodationCost
    2. pro Party: share = poolTotal x (partyWeight / totalWeight)
    3. pro Party: credits = sum(receipts where paidBy in party.memberIds) -- Person-Belege der Party
    4. pro Party: balance = credits - share  (positiv = bekommt zurueck, negativ = schuldet)
  calculateSettlementPlan() -> SettlementPlan (Party-to-Party Transfers)

  addAdvancePayment(description, amount, partyId, date) -- AdvancePayment-Entity erstellen
  confirmAdvancePayments(amount) -- Gleichen Betrag fuer ALLE Parteien erfassen
  removeAdvancePayments() -- Alle Vorauszahlungen entfernen (vor Neubestaetigung)

AdvancePayment (Entity innerhalb Expense) -- NEU
  +-- advancePaymentId: UUID
  +-- description: String
  +-- amount: BigDecimal (> 0, gleich fuer alle Parteien)
  +-- paidByPartyId: UUID (TenantId der zahlenden Partei)
  +-- paidByPartyName: String (denormalisiert)
  +-- paidAt: LocalDate
  +-- paid: boolean (default false, Toggle durch Organizer)
  +-- createdAt: Instant

Neue Invarianten:
  INV-PW1: Jeder Teilnehmer (memberIds) muss genau einer Party zugeordnet sein
  INV-PW2: paidBy in addReceipt muss in einer der Party.memberIds enthalten sein
  INV-AP1: Alle AdvancePayments innerhalb eines Expense haben denselben amount (gleicher Rundbetrag)
  INV-AP2: Pro Party existiert maximal ein AdvancePayment
```

**Migration**: Flyway V4 --
1. ALTER TABLE `participant_weighting` -> Rename/refactor zu `party_weighting` (partyId, partyName, totalWeight)
2. Neue Junction Table `party_weighting_member` (partyWeightingId, memberId)
3. Neue Table `advance_payment (id, expense_id, description, amount, paid_by_party_id, paid_by_party_name, paid_at, paid BOOLEAN DEFAULT FALSE, created_at)` FK auf `expense`
4. ADD COLUMN `accommodation_cost NUMERIC` auf `expense`
5. Datenmigration: Bestehende Einzel-Gewichtungen zu 1-Person-Parties umwandeln

### 3.3 PartyProjection (Neues Read Model im Expense SCS)

```
PartyProjection (Read Model) -- NEU, ersetzt die flache Participant-Liste
  Wird aufgebaut aus:
    - ParticipantJoinedTrip (erweitert um participantTenantId)
    - TravelParty-Info aus IAM Events (TenantCreated, MemberAddedToTenant, DependentAddedToTenant)

  +-- tripId: UUID
  +-- parties: List<PartyInfo>

PartyInfo:
  +-- partyId: UUID (= tenantId der TravelParty)
  +-- partyName: String
  +-- memberIds: List<UUID>
```

**Alternative (einfacher fuer MVP)**: Expense SCS konsumiert kein separates PartyProjection, sondern das erweiterte `ParticipantJoinedTrip`-Event liefert genug Information. Beim `TripCompleted` werden die Parties aus den gesammelten `ParticipantJoinedTrip`-Events mit `participantTenantId` gruppiert und als `PartyWeighting` angelegt.

### 3.4 SettlementPlan -- Party-to-Party Transfers

```
SettlementPlan (Value Object) -- REFACTORED
  +-- transfers: List<Transfer>

Transfer (Value Object) -- REFACTORED
  +-- fromPartyId: UUID (war: from: UUID -- Einzelperson)
  +-- toPartyId: UUID (war: to: UUID -- Einzelperson)
  +-- amount: BigDecimal
```

### 3.5 AdvancePaymentSuggestion (Domain Service im Expense SCS)

```
AdvancePaymentSuggestion (Domain Service) -- NEU
  suggest(accommodationCost, partyCount) -> AdvanceSuggestion

AdvanceSuggestion (Value Object):
  +-- suggestedAmount: BigDecimal  (gleicher Rundbetrag pro Partei)
  +-- accommodationCost: BigDecimal
  +-- partyCount: int
  +-- formula: "ceil(accommodationCost / partyCount / 50) × 50"

Berechnung:
  suggestedAmount = ceil(accommodationCost / partyCount / 50) x 50
  Beispiele:
    3000 / 3 Parteien = 1000 -> ceil(1000/50)*50 = 1000 EUR
    1400 / 3 Parteien = 466.67 -> ceil(466.67/50)*50 = 500 EUR
    2500 / 4 Parteien = 625 -> ceil(625/50)*50 = 650 EUR

Organizer kann den vorgeschlagenen Betrag anpassen (z.B. 550 statt 500 EUR fuer Puffer).
Der bestaetigte Betrag gilt GLEICH fuer ALLE Parteien.
```

Dieser Service wird NICHT persistiert -- er ist eine reine Berechnungshilfe, die der Organizer auf der Vorauszahlungs-Seite sieht. Der Organizer bestaetigt oder passt den Betrag an, danach wird derselbe Betrag fuer JEDE Partei als AdvancePayment erfasst.

---

## 4. Cross-SCS Event Design

### 4.1 Erweiterte Events

```
ParticipantJoinedTrip (ERWEITERT)
  +-- tenantId: UUID          (Trip-Tenant, wie bisher)
  +-- tripId: UUID
  +-- participantId: UUID     (memberId der Person)
  +-- participantTenantId: UUID  -- NEU: TenantId der TravelParty des Teilnehmers
  +-- username: String
  +-- occurredOn: LocalDate
```

**Wo kommt participantTenantId her?** Im Trips SCS: Der TripService kennt den Participant (memberId). Die TravelParty-Projektion kennt das Mapping memberId -> tenantId. Beim Publizieren von ParticipantJoinedTrip wird das TenantId des Teilnehmers aufgeloest.

### 4.2 Neue Events

```
AccommodationPriceSet (NEU, Trips -> Expense)
  +-- tenantId: UUID
  +-- tripId: UUID
  +-- totalPrice: BigDecimal
  +-- currency: String
  +-- occurredOn: LocalDate

  Publiziert wenn: Organizer setzt/aendert den Gesamtpreis der Unterkunft
  Konsumiert von: Expense SCS -> setzt accommodationCost auf Expense
  RoutingKey: trips.accommodation.price.set
```

### 4.3 Uebersicht Cross-SCS Event Flow

```
TRIPS SCS                              EXPENSE SCS
---------                              -----------
Trip geplant
  |
  v
TripCreated ----------------------->  TripProjection erstellt
  |
Participant tritt bei
  |
  v
ParticipantJoinedTrip(+tenantId) --->  TripProjection.addParticipant()
                                       (mit Party-Zuordnung gespeichert)
  |
Accommodation erstellt
  |
  v
AccommodationPriceSet -------------->  Expense.setAccommodationCost()
  |
Trip abgeschlossen
  |
  v
TripCompleted ---------------------->  Expense.create(partyWeightings)
                                       (PartyWeightings aus TripProjection gruppiert)

                                       Belege erfasst (Receipt mit paidBy: Person-UUID)
                                       |
                                       v
                                       calculateBalances() -- auf Party-Ebene
                                       |
                                       v
                                       SettlementPlan (Party-to-Party Transfers)
```

---

## 5. Domain Event Timeline

### 5.1 Accommodation anlegen + Preis setzen

```
                    TRIPS SCS
                    ---------
    Organizer oeffnet Trip-Detailseite
              |
              v
    [1] CreateAccommodation(tenantId, tripId, name, address?, url?)
              |
              v
    Accommodation.create(tenantId, tripId, name) -> AccommodationCreated
              |
              v
    [2] AddRoom(accommodationId, name, roomType, bedCount, pricePerNight?)
              |
              v
    Accommodation.addRoom(...) -- kein Cross-SCS Event
              |
              v
    [3] SetTotalPrice(accommodationId, totalPrice, currency)
              |
              v
    Accommodation.setTotalPrice(3000, "EUR") -> AccommodationPriceSet  ----> EXPENSE SCS
```

### 5.2 Expense mit PartyWeighting erstellen (bei TripCompleted)

```
                    EXPENSE SCS
                    -----------
    TripCompleted empfangen
              |
              v
    [1] TripProjection laden -- Participants mit participantTenantId
              |
              v
    [2] Participants nach participantTenantId gruppieren
        -> Party A (tenantId=X): [memberId1, memberId2, dependentId1]
        -> Party B (tenantId=Y): [memberId3]
              |
              v
    [3] PartyWeighting erstellen (Default: totalWeight = Anzahl Members + 0 fuer Dependents)
        -> PartyWeighting(partyId=X, name="Mueller", weight=2.0, memberIds=[m1,m2,d1])
        -> PartyWeighting(partyId=Y, name="Schmidt", weight=2.0, memberIds=[m3])
              |
              v
    [4] Expense.create(tenantId, tripId, partyWeightings)
              |
              v
    Organizer passt Gewichtungen an:
        -> updatePartyWeighting(partyId=X, weight=2.5) -- 2 Erw. + Kind halb
              |
              v
    [5] Belege werden erfasst (paidBy = Person-UUID, wie bisher)
              |
              v
    [6] settle() -> calculateBalances() auf Party-Ebene
              |
              v
    SettlementPlan: [Transfer(fromPartyId=Y, toPartyId=X, amount=45.00)]
    -> "Familie Schmidt schuldet Familie Mueller 45.00 EUR"
```

### 5.3 Vorauszahlung berechnen + erfassen (Gleicher Rundbetrag pro Partei)

```
                    EXPENSE SCS
                    -----------
    AccommodationPriceSet empfangen (3000 EUR)
              |
              v
    Expense.setAccommodationCost(3000)
              |
              v
    Organizer oeffnet Vorauszahlungs-Ansicht
              |
              v
    [1] AdvancePaymentSuggestion.suggest(
          accommodationCost=3000,
          partyCount=3
        )
        -> AdvanceSuggestion(suggestedAmount=1000.00)
        Formel: ceil(3000 / 3 / 50) x 50 = 1000 EUR pro Partei
              |
              v
    Organizer sieht Vorschlag: "1.000 EUR pro Reisepartei"
    Organizer passt an: 1.100 EUR (fuer Puffer)
    Organizer bestaetigt den Betrag
              |
              v
    [2] confirmAdvancePayment(amount=1100.00)
        -> fuer JEDE Partei wird derselbe Betrag erfasst:
              |
              v
    [3] addAdvancePayment("Vorauszahlung Unterkunft", 1100.00, partyId=X, date=2026-03-01)
    [4] addAdvancePayment("Vorauszahlung Unterkunft", 1100.00, partyId=Y, date=2026-03-01)
    [5] addAdvancePayment("Vorauszahlung Unterkunft", 1100.00, partyId=Z, date=2026-03-01)
        -> Alle 3 Parteien: gleicher Betrag (1.100 EUR)
              |
              v
    [6] Bei Settlement: jede Partei erhaelt einen Flat Credit von 1.100 EUR
        partyBalance = partyShare - partyReceiptCredits - 1100.00
```

### 5.4 Resubmit Rejected Receipt (US-EXP-042)

```
                    EXPENSE SCS
                    -----------
    Beleg wurde REJECTED (bestehendes Szenario)
              |
              v
    Einreicher sieht abgelehnten Beleg mit Begruendung
              |
              v
    [1] ResubmitReceipt (bestehendes Command)
              |
              v
    Expense.resubmitReceipt(receiptId, description, amount, date, category)
              |
              v
    Receipt.resubmit() --> status: REJECTED --> SUBMITTED
```

(Domain + Service bereits implementiert. Nur Controller + Template fehlen.)

### 5.5 PWA Manifest (US-INFRA-041)

```
    Kein Domain-Event-Flow. Reine Infrastruktur:

    [1] manifest.json erstellen (Gateway static resources)
    [2] Service Worker registrieren (App Shell Caching)
    [3] Meta-Tags in Layout-Templates aller SCS
    [4] Install Prompt Banner (JavaScript)
```

---

## 6. Bewertungsmatrix (revidiert)

| # | Story | Size | Value | Risk | Cross-SCS | Domain-Aenderung |
|---|-------|------|-------|------|-----------|------------------|
| US-EXP-050 | PartyWeighting Refactoring | L | CRITICAL | HIGH | Event-Erweiterung + neues Event | Expense-Kern umbauen |
| US-TRIPS-060 | Accommodation (Basis) | M | HIGH | MEDIUM | Neues Event AccommodationPriceSet | Neues Aggregate |
| US-EXP-013 | Advance Payment | M | HIGH | MEDIUM | Abhaengig von Accommodation-Preis | Receipt erweitern + Calc Service |
| US-EXP-042 | Resubmit Receipt (UI) | S | HIGH | LOW | Keine | Keine (nur Adapter) |
| US-INFRA-041 | PWA Manifest | S | HIGH | MEDIUM | Layout-Templates | Keine |
| US-EXP-032 | Category Breakdown | M | MEDIUM | LOW | Keine | Read Model |

---

## 7. Empfohlener Iterationsplan

### ITERATION 9 (v0.9.0) -- "Party-Level Settlement + Accommodation Basis"

**Leitmotiv**: Die Abrechnung auf das korrekte Domaenenmodell (Reisepartei-Ebene) umstellen und die Unterkunft als zentrales Planungselement einfuehren.

| # | ID | Story | Size | SCS | Begruendung |
|---|---|-------|------|-----|-------------|
| S9-A | US-EXP-050 | PartyWeighting Refactoring | L | Expense + Common | MUSS ZUERST: Korrektes Domaenenmodell ist Voraussetzung fuer alles andere |
| S9-B | US-TRIPS-060 | Accommodation Basis (Name, Rooms, Preis) | M | Trips + Common | Liefert AccommodationPriceSet Event fuer Vorauszahlungen |
| S9-C | US-EXP-013 | Advance Payment Tracking | M | Expense | Baut auf PartyWeighting + AccommodationCost auf |
| S9-D | US-EXP-042 | Resubmit Rejected Receipt (UI) | S | Expense | Quick Win, Domain fertig, schliesst Feature-Luecke |
| S9-E | US-INFRA-041 | PWA Manifest & Install Prompt | S | Gateway + alle | ADR-0005 umsetzen, groesster Mobile-UX-Gewinn |

### Empfohlene Reihenfolge

```
S9-D (Resubmit UI)              -- S, schneller Quick Win zum Warmwerden
  |
  v
S9-A (PartyWeighting Refactor)  -- L, Fundament fuer korrektes Settlement
  |                                 Umfasst:
  |                                 1. ParticipantJoinedTrip Event erweitern (+participantTenantId)
  |                                 2. TripProjection erweitern (Party-Zuordnung)
  |                                 3. PartyWeighting VO + Expense Refactoring
  |                                 4. calculateBalances() auf Party-Ebene
  |                                 5. SettlementPlan Party-to-Party
  |                                 6. UI-Anpassung (Party-Namen statt Personen)
  |                                 7. Flyway V4 Migration (Expense)
  |
  v
S9-B (Accommodation Basis)      -- M, neues Aggregate, AccommodationPriceSet Event
  |
  v
S9-C (Advance Payment)          -- M, baut auf S9-A (PartyWeighting) + S9-B (Preis) auf
  |
  v
S9-E (PWA Manifest)             -- S, Querschnitt, am Ende wenn UI stabil
```

### Begruendung der Auswahl

1. **S9-A ist das Fundament**: Ohne korrektes Party-Level Settlement sind Vorauszahlungen und Kategorie-Aufschluesselung auf dem falschen Abstraktionsniveau. Dies ist eine technische Schuld, die jetzt behoben werden MUSS, bevor weitere Features darauf aufbauen.

2. **S9-B liefert den Preis-Input**: Accommodation mit Gesamtpreis ist Voraussetzung fuer die automatische Vorauszahlungsberechnung (S9-C).

3. **S9-C verbindet die Teile**: Advance Payment nutzt PartyWeighting (S9-A) + AccommodationCost (S9-B) fuer die Berechnung und Receipt+Flag fuer die Erfassung.

4. **S9-D ist schneller Einstieg**: Domain + Service existieren, nur Controller + Template fehlen. Innerhalb eines halben Tages machbar.

5. **S9-E schliesst ADR-0005**: PWA-Manifest + minimaler Service Worker. Kein Domain-Impact.

6. **Category Breakdown (US-EXP-032) verschoben**: Muss zuerst auf Party-Level Settlement aufgebaut werden. Besser in Iteration 10 nach stabilem Fundament.

### Risikobewertung

| Risiko | Schwere | Mitigation |
|--------|---------|------------|
| PartyWeighting Refactoring bricht bestehende E2E-Tests | HOCH | Alle 189 E2E-Tests nach S9-A ausfuehren, Settlement-Tests gezielt anpassen |
| AccommodationPriceSet Event Timing (Preis gesetzt bevor Expense existiert) | MITTEL | Expense SCS cached den Preis in TripProjection, setzt accommodationCost bei create() |
| Flyway V4 Migration bei bestehenden Expense-Daten | MITTEL | Datenmigration: Bestehende Einzel-Gewichtungen -> 1-Person-Parties. Idempotent ausfuehren. |
| ParticipantJoinedTrip Event-Erweiterung bricht Expense Consumer | MITTEL | Feld ist additiv (neues optionales Feld). Alte Events ohne participantTenantId -> Fallback auf 1-Person-Party |

### ITERATION 10 (v0.10.0) -- Vorschau

| Story | Begruendung |
|-------|-------------|
| US-EXP-032: Category Breakdown | Jetzt auf Party-Level, nach stabilem Settlement |
| US-TRIPS-060b: Accommodation Zimmer-Zuweisung | Room -> Party Mapping, Bettenzahl-Invariante |
| US-TRIPS-060c: Accommodation Amenities | Fuer spaetere Polls, rein informativ |
| US-TRIPS-061: URL Import (Accommodation) | OpenGraph/Schema.org Extraktion |
| US-EXP-033: PDF Export | SettlementPlan als PDF |

### Spaeter (Iteration 11+)

| Story | Begruendung |
|-------|-------------|
| US-TRIPS-062: Accommodation Poll | Eigenes Aggregate, grosse Feature-Iteration |
| US-EXP-022: Custom Splitting per Receipt | Aendert Settlement-Kern, erst nach Stabilisierung |
| US-INFRA-042: Lighthouse CI | Nach PWA-Stabilisierung |
| US-TRIPS-055: Bring Integration | Externe API-Abhaengigkeit |
| Recipe Import from URL | Gemeinsam mit URL Import Infrastruktur |

---

## 8. Zusammenfassung der Domain-Aenderungen

### travelmate-common (Shared Kernel)

**Erweitert**:
- `ParticipantJoinedTrip` + Feld `participantTenantId: UUID`

**Neu**:
- `AccommodationPriceSet(tenantId, tripId, totalPrice, currency, occurredOn)` in `events/trips/`
- Neuer `RoutingKeys`-Eintrag: `TRIPS_ACCOMMODATION_PRICE_SET`

### travelmate-trips (Trips SCS)

**Neues Aggregate**:
- `Accommodation` (AccommodationId, TenantId, tripId, name, address, url, checkInDate, checkOutDate, totalPrice, currency, rooms)
- `Room` Entity (RoomId, name, roomType, bedCount, pricePerNight)
- `AccommodationRepository` Port + JPA-Implementierung
- `AccommodationService` Application Service
- `AccommodationController` Web Adapter

**Erweitert**:
- `TripService` / `DomainEventPublisher`: `participantTenantId` in `ParticipantJoinedTrip` befuellen (aus TravelParty Lookup)

**Flyway**: V11 -- `accommodation` + `accommodation_room` Tables

### travelmate-expense (Expense SCS)

**Refactored**:
- `ParticipantWeighting` -> `PartyWeighting(partyId, partyName, totalWeight, memberIds)`
- `Expense.weightings` -> `Expense.partyWeightings`
- `Expense.calculateBalances()` -> Party-Level Berechnung
- `SettlementPlan.Transfer(from, to)` -> `Transfer(fromPartyId, toPartyId, amount)`
- `ExpenseService.onTripCompleted()` -> Participants nach `participantTenantId` gruppieren
- `ExpenseService.onParticipantJoined()` -> `participantTenantId` in TripProjection speichern

**Neu**:
- `Expense.accommodationCost: BigDecimal` Feld
- `Expense.setAccommodationCost(totalPrice)` Methode
- `AdvancePayment` Entity innerhalb Expense (advancePaymentId, description, amount, paidByPartyId, paidByPartyName, paidAt, paid, createdAt)
- `Expense.addAdvancePayment()` / `confirmAdvancePayments(amount)` / `removeAdvancePayments()` Methoden
- `AdvancePaymentSuggestion` Domain Service (suggest: ceil(price / partyCount / 50) x 50)
- `AdvanceSuggestion` Value Object
- `AccommodationPriceSetConsumer` Messaging Adapter
- Resubmit Controller-Endpoint + Thymeleaf-Template (S9-D)

**Flyway**: V4 -- party_weighting refactoring + advance_payment column + accommodation_cost column

### travelmate-gateway

**Neu (PWA)**:
- `manifest.json` in static resources
- Service Worker (App Shell Cache + Offline Fallback)
- Meta-Tags in Layout-Templates

---

## 9. ADR-Kandidaten

| ADR | Thema | Trigger |
|-----|-------|---------|
| ADR-0016 | Accommodation als eigenes Aggregate in Trips (Revision der Erstanalyse) | HS-2 |
| ADR-0017 | PartyWeighting statt ParticipantWeighting -- Abrechnung auf Reisepartei-Ebene | HS-1 |
| ADR-0018 | Advance Payment als gleicher Rundbetrag pro Partei + AdvancePaymentSuggestion Domain Service | HS-3 |

---

## 10. Naechste Schritte

1. **ADRs schreiben**: ADR-0016, ADR-0017, ADR-0018
2. **Story Refinement**: Detaillierte Akzeptanzkriterien fuer S9-A bis S9-E
3. **UX Wireframes**: Party-Level Settlement UI, Accommodation-Sektion, Vorauszahlungs-Ansicht, Resubmit-Modal
4. **Spike S9-A**: ParticipantJoinedTrip Event-Erweiterung + Expense calculateBalances() auf Party-Level -- als Proof of Concept
5. **TDD fuer Domain**:
   - Expense: PartyWeighting, calculateBalances() mit Parties, addAdvancePayment(), AdvancePaymentSuggestion (ceil-Formel)
   - Trips: Accommodation.create(), addRoom(), setTotalPrice(), AccommodationPriceSet Event
6. **E2E-Tests**: Bestehende Settlement-E2E-Tests nach S9-A anpassen (Party-Level UI)
7. **PWA-Spike**: Service Worker Scope-Test mit Gateway + SCS Routing
