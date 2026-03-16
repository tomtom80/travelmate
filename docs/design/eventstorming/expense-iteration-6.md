# Design-Level EventStorming: Expense SCS -- Iteration 6

**Datum**: 2026-03-16
**Scope**: Tactical DDD -- Receipt Review, Settlement Calculation, Expense Categories, Accommodation Splitting
**Methode**: Design-Level EventStorming (Alberto Brandolini)
**Bounded Context**: Expense (Generic Subdomain)

---

## 1. Bestandsaufnahme (Ist-Zustand)

### Bestehende Aggregate und Events

```
Expense (AggregateRoot)
  +-- ExpenseId, TenantId, tripId
  +-- status: OPEN | SETTLED
  +-- receipts: List<Receipt>
  |     +-- ReceiptId, description, Amount, paidBy, date
  +-- weightings: List<ParticipantWeighting>
        +-- participantId, weight (BigDecimal)

TripProjection (Read Model)
  +-- tripId, tenantId, tripName
  +-- participants: List<TripParticipant(participantId, name)>

Events (published):  ExpenseCreated, ExpenseSettled
Events (consumed):   TripCreated, ParticipantJoinedTrip, TripCompleted
```

---

## 2. Zeitleiste der Domain Events (Orange Stickies)

Die Events sind chronologisch nach Feature-Cluster geordnet.

### 2.1 Expense Lifecycle (bestehend + erweitert)

```
TripCompleted (extern)
  --> ExpenseCreated
        --> ReceiptSubmitted  [NEU]
              --> ReceiptApproved [NEU]  --oder--  ReceiptRejected [NEU]
                    --> (alle Belege approved?) --> ExpenseReadyForSettlement [NEU]
                          --> ExpenseSettled (bestehend)
```

### 2.2 Receipt Review Workflow (Four-Eyes)

```
1. ReceiptSubmitted        -- Beleg wurde eingereicht (status SUBMITTED)
2. ReceiptApproved         -- Beleg wurde von zweiter Person genehmigt
3. ReceiptRejected         -- Beleg wurde abgelehnt (mit Begruendung)
4. ReceiptResubmitted      -- Abgelehnter Beleg erneut eingereicht (nach Korrektur)
```

### 2.3 Settlement Calculation

```
5. ExpenseReadyForSettlement  -- Alle Belege approved, Settlement moeglich
6. SettlementCalculated       -- Optimierte Zahlungsmatrix berechnet (debt simplification)
7. ExpenseSettled              -- Abrechnung finalisiert (bestehend, erweitert)
```

### 2.4 Expense Categories

```
8. ReceiptCategorized        -- Beleg wurde einer Kategorie zugewiesen
   (wird Teil von ReceiptSubmitted, kein eigenstaendiges Event)
```

### 2.5 Accommodation Splitting

```
9. StayPeriodUpdated (extern, NEU)  -- Trips SCS publiziert Aufenthaltszeitraum-Aenderung
10. AccommodationReceiptSubmitted   -- Spezieller Beleg mit Nachtsplitting
```

### Vollstaendige Zeitleiste

```
                    EXTERNAL                          EXPENSE SCS
                    --------                          -----------
              [1] TripCreated  --------->  TripProjection angelegt
              [2] ParticipantJoinedTrip ->  TripProjection aktualisiert
              [3] StayPeriodUpdated  ---->  TripProjection aktualisiert (StayPeriod)  [NEU]
              [4] TripCompleted  -------->  Expense erstellt (ExpenseCreated)
                                           |
                                    [5] ReceiptSubmitted (mit Kategorie)
                                           |
                                    [6] ReceiptApproved  --oder--  ReceiptRejected
                                           |                            |
                                           |                     [7] ReceiptResubmitted
                                           |                            |
                                           +<---------------------------+
                                           |
                                    [8] ExpenseReadyForSettlement (Policy)
                                           |
                                    [9] SettlementCalculated (Schuldenoptimierung)
                                           |
                                   [10] ExpenseSettled
```

---

## 3. Commands (Blue Stickies)

| # | Command | Akteur | Aggregate | Vorbedingung |
|---|---------|--------|-----------|-------------|
| C1 | SubmitReceipt | Participant | Expense | Expense OPEN |
| C2 | ApproveReceipt | Participant (Reviewer) | Expense | Receipt SUBMITTED, reviewer != submitter |
| C3 | RejectReceipt | Participant (Reviewer) | Expense | Receipt SUBMITTED, reviewer != submitter |
| C4 | ResubmitReceipt | Participant (original submitter) | Expense | Receipt REJECTED |
| C5 | SettleExpense | Organizer | Expense | Expense OPEN, alle Receipts APPROVED |
| C6 | UpdateWeighting | Organizer | Expense | Expense OPEN (bestehend) |
| C7 | RemoveReceipt | Submitter | Expense | Receipt SUBMITTED oder REJECTED, Expense OPEN |

### Command-Signatur-Entwuerfe

```
SubmitReceipt(tripId, description, amount, paidBy, date, category, submittedBy)
  -- submittedBy ist der einreichende Participant (kann != paidBy sein)

SubmitAccommodationReceipt(tripId, description, totalAmount, paidBy, date, submittedBy)
  -- Splitting wird automatisch anhand StayPeriod berechnet

ApproveReceipt(tripId, receiptId, reviewerId)

RejectReceipt(tripId, receiptId, reviewerId, reason)

ResubmitReceipt(tripId, receiptId, description, amount, date, category, submittedBy)
  -- Aktualisiert den Beleg und setzt Status zurueck auf SUBMITTED

SettleExpense(tripId, settledBy)
```

---

## 4. Command-Event-Mapping

| Command | Aggregate | Event(s) | Invariant | Policy |
|---------|-----------|----------|-----------|--------|
| SubmitReceipt | Expense | ReceiptSubmitted | paidBy muss Participant sein; Expense OPEN | -- |
| SubmitAccommodationReceipt | Expense | ReceiptSubmitted | wie oben, StayPeriod muss vorliegen | Berechne Nacht-Anteile aus TripProjection |
| ApproveReceipt | Expense | ReceiptApproved | reviewer != submitter; Receipt SUBMITTED | Wenn alle Receipts APPROVED -> ExpenseReadyForSettlement |
| RejectReceipt | Expense | ReceiptRejected | reviewer != submitter; Receipt SUBMITTED | -- |
| ResubmitReceipt | Expense | ReceiptResubmitted | nur original Submitter; Receipt REJECTED | -- |
| RemoveReceipt | Expense | (ReceiptRemoved -- intern) | Receipt SUBMITTED/REJECTED; Expense OPEN | -- |
| SettleExpense | Expense | SettlementCalculated, ExpenseSettled | alle Receipts APPROVED; >= 1 Receipt | Berechne debt-simplified Zahlungsmatrix |

---

## 5. Aggregates (Yellow Stickies)

### 5.1 Expense Aggregate (erweitert)

**Entscheidung**: Receipt bleibt Entity innerhalb von Expense. Settlement bleibt kein eigenes Aggregate, sondern wird als Value Object (`SettlementPlan`) innerhalb von Expense modelliert.

**Begruendung**: Expense ist die natuerliche Transaktionsgrenze. Alle Belege einer Reise werden gemeinsam abgerechnet. Ein separates Settlement-Aggregate wuerde die Konsistenz zwischen Receipts und Settlement ohne Not aufbrechen.

```
Expense (AggregateRoot) -- ERWEITERT
  +-- ExpenseId (VO)
  +-- TenantId (VO)
  +-- tripId: UUID
  +-- status: OPEN | READY_FOR_SETTLEMENT | SETTLED     [erweitert]
  +-- receipts: List<Receipt>
  |     +-- ReceiptId (VO)
  |     +-- description: String
  |     +-- Amount (VO, > 0)
  |     +-- paidBy: UUID
  |     +-- submittedBy: UUID                             [NEU]
  |     +-- date: LocalDate
  |     +-- category: ExpenseCategory (VO/Enum)           [NEU]
  |     +-- reviewStatus: ReviewStatus                    [NEU]
  |     +-- reviewerId: UUID (nullable)                   [NEU]
  |     +-- rejectionReason: String (nullable)            [NEU]
  +-- weightings: List<ParticipantWeighting> (bestehend)
  +-- settlementPlan: SettlementPlan (nullable)           [NEU]
        +-- transfers: List<Transfer>
              +-- from: UUID
              +-- to: UUID
              +-- amount: BigDecimal
```

### 5.2 Neue Value Objects

```
ExpenseCategory (Enum)
  ACCOMMODATION, GROCERIES, RESTAURANT, ACTIVITY, TRANSPORT, FUEL, HEALTH, OTHER

ReviewStatus (Enum)
  SUBMITTED, APPROVED, REJECTED

SettlementPlan (Record)
  +-- transfers: List<Transfer>
  +-- calculatedAt: LocalDate

Transfer (Record)
  +-- from: UUID (Schuldner)
  +-- to: UUID (Glaeubiger)
  +-- amount: Amount
```

### 5.3 ExpenseStatus (erweitert)

```
ExpenseStatus (Enum)
  OPEN                    -- Belege werden eingereicht/reviewed
  READY_FOR_SETTLEMENT    -- Alle Belege approved (automatisch via Policy)
  SETTLED                 -- Abrechnung finalisiert

  OPEN -> READY_FOR_SETTLEMENT (automatisch, wenn alle Receipts APPROVED)
  READY_FOR_SETTLEMENT -> OPEN (automatisch, wenn neuer Receipt eingereicht
                                oder bestehender rejected wird)
  READY_FOR_SETTLEMENT -> SETTLED (manuell durch Organizer)
  SETTLED -> (terminal, keine Rueckkehr)
```

### 5.4 TripProjection (erweitert)

```
TripProjection -- ERWEITERT
  +-- tripId, tenantId, tripName
  +-- startDate: LocalDate                                [NEU, aus TripCreated]
  +-- endDate: LocalDate                                  [NEU, aus TripCreated]
  +-- participants: List<TripParticipant>
        +-- participantId, name
        +-- arrivalDate: LocalDate (nullable)             [NEU, aus StayPeriodUpdated]
        +-- departureDate: LocalDate (nullable)           [NEU, aus StayPeriodUpdated]
```

---

## 6. Invarianten

### Expense Aggregate

| ID | Invariante | Durchgesetzt in |
|----|-----------|-----------------|
| INV-1 | Bezahler (paidBy) muss Participant sein (in weightings) | `Expense.submitReceipt()` |
| INV-2 | Reviewer darf nicht der Submitter sein (Four-Eyes) | `Expense.approveReceipt()`, `Expense.rejectReceipt()` |
| INV-3 | Nur Receipts im Status SUBMITTED koennen approved/rejected werden | `Expense.approveReceipt()`, `Expense.rejectReceipt()` |
| INV-4 | Nur Receipts im Status REJECTED koennen resubmitted werden | `Expense.resubmitReceipt()` |
| INV-5 | Settlement nur wenn ALLE Receipts APPROVED und >= 1 Receipt | `Expense.settle()` |
| INV-6 | Keine Aenderungen nach SETTLED | `assertNotSettled()` |
| INV-7 | Submitter kann sein eigenes Receipt entfernen (SUBMITTED/REJECTED) | `Expense.removeReceipt()` |
| INV-8 | Accommodation-Split: Summe der Nacht-Anteile = Gesamtbetrag | `Expense.submitAccommodationReceipt()` |

### Four-Eyes Detail

```
submittedBy: Der Participant, der den Beleg einreicht.
  -- submittedBy kann gleich paidBy sein (Normalfall: "Ich habe bezahlt und reiche ein")
  -- submittedBy kann ungleich paidBy sein ("Anna hat bezahlt, ich reiche fuer sie ein")

reviewerId: Der Participant, der den Beleg prueft.
  -- reviewerId != submittedBy (IMMER, das ist die Four-Eyes-Regel)
  -- reviewerId kann gleich paidBy sein (erlaubt: "Ich habe bezahlt, jemand anders hat
     eingereicht, ich darf das Review machen")
```

---

## 7. Policies / Reaktionen (Lilac Stickies)

| # | Policy | Trigger (Event) | Aktion | Typ |
|---|--------|-----------------|--------|-----|
| P1 | Auto-Status-Transition | ReceiptApproved | Wenn ALLE Receipts APPROVED -> Status = READY_FOR_SETTLEMENT, publiziere ExpenseReadyForSettlement | Aggregate-intern |
| P2 | Status-Ruecksetzung | ReceiptSubmitted, ReceiptResubmitted | Wenn Status READY_FOR_SETTLEMENT und neuer/aktualisierter Receipt -> Status = OPEN | Aggregate-intern |
| P3 | Auto-Expense-Creation | TripCompleted | Expense erstellen mit Default-Gewichtungen (bestehend) | Application Service |
| P4 | StayPeriod-Projektion | StayPeriodUpdated | TripProjection mit Aufenthaltszeitraum aktualisieren | Application Service |
| P5 | Settlement-Berechnung | SettleExpense-Command | Debt-simplified Zahlungsmatrix berechnen, als SettlementPlan speichern | Aggregate-intern |

### Policy P1 Detail: Auto-Status-Transition

```
Nach jedem ReceiptApproved:
  1. Pruefe: Haben ALLE Receipts den Status APPROVED?
  2. Wenn ja: status = READY_FOR_SETTLEMENT, registerEvent(ExpenseReadyForSettlement)
  3. Wenn nein: status bleibt OPEN

Dieses Pattern ist aggregate-intern, kein externer Policy-Handler noetig.
```

### Policy P5 Detail: Debt Simplification

Der bestehende `calculateBalances()` berechnet bereits Netto-Salden pro Participant.
Die Debt Simplification reduziert die Anzahl der Transfers:

```
Eingabe: Salden {A: +50, B: -30, C: -20}
  (A hat 50 EUR Guthaben, B schuldet 30, C schuldet 20)

Algorithmus (Greedy):
  1. Sortiere Glaeubiger absteigend, Schuldner aufsteigend
  2. Verrechne groessten Glaeubiger mit groesstem Schuldner
  3. Wiederhole bis alle Salden 0

Ergebnis: [B -> A: 30 EUR, C -> A: 20 EUR]
  statt moeglicherweise: [B -> A: 15, B -> C: 15, C -> A: 35] (ohne Optimierung)
```

---

## 8. Read Models (Green Stickies)

| # | Read Model | Datenquelle | Verwendet von | Neu/Bestehend |
|---|-----------|-------------|---------------|---------------|
| RM1 | TripProjection | TripCreated, ParticipantJoinedTrip, StayPeriodUpdated | ExpenseService, Accommodation-Split | Erweitert |
| RM2 | ExpenseOverview | Expense Aggregate | Expense-Detailseite | Bestehend (erweitert) |
| RM3 | ReceiptReviewList | Expense.receipts (filtered by SUBMITTED) | Review-UI, zeigt offene Belege | NEU |
| RM4 | SettlementSummary | Expense.settlementPlan + Expense.balances | Settlement-Seite, "Wer zahlt wem" | NEU |
| RM5 | CategoryBreakdown | Expense.receipts (grouped by category) | Statistik/Uebersicht pro Kategorie | NEU |

### RM3: ReceiptReviewList

```
ReceiptReviewList
  +-- expenseId, tripId, tripName
  +-- pendingReceipts: List<ReceiptForReview>
        +-- receiptId, description, amount, paidBy, paidByName,
            submittedBy, submittedByName, date, category
  -- Filtert: nur Receipts mit reviewStatus == SUBMITTED
  -- Filtert: nur Receipts wo currentUser != submittedBy (zeigt nur reviewbare)
```

### RM4: SettlementSummary

```
SettlementSummary
  +-- expenseId, tripId, tripName, status
  +-- totalAmount: BigDecimal
  +-- balances: Map<ParticipantInfo, BigDecimal>
  +-- transfers: List<TransferView>  (nur bei SETTLED)
        +-- fromName, toName, amount
  +-- categoryTotals: Map<ExpenseCategory, BigDecimal>
```

---

## 9. External Systems (Pink Stickies)

| System | Interaktion | Richtung | Mechanismus |
|--------|-------------|----------|-------------|
| Trips SCS | TripCreated, ParticipantJoinedTrip, TripCompleted | Trips -> Expense | RabbitMQ (bestehend) |
| Trips SCS | StayPeriodUpdated | Trips -> Expense | RabbitMQ (NEU) |
| IAM SCS | (indirekt via TripProjection Teilnehmernamen) | -- | Kein direkter Kontakt |

### Neues Event: StayPeriodUpdated

Dieses Event muss vom Trips SCS publiziert werden, wenn ein Organizer den Aufenthaltszeitraum eines Participants setzt oder aendert.

```
StayPeriodUpdated (neuer Event-Contract in travelmate-common)
  +-- tenantId: UUID
  +-- tripId: UUID
  +-- participantId: UUID
  +-- arrivalDate: LocalDate
  +-- departureDate: LocalDate
  +-- occurredOn: LocalDate
```

**Routing Key**: `trips.stay-period-updated`

---

## 10. Hot Spots (Red Stickies)

### HS-1: Receipt Status vs. Expense Complexity (MEDIUM)
**Frage**: Wird Receipt durch die ReviewStatus-Erweiterung zu komplex als Entity innerhalb von Expense?
**Position**: Nein. Receipt hat keinen eigenen Lifecycle unabhaengig von Expense. Alle Receipt-Operationen gehen durch die Expense-Aggregate-Boundary. Das ist korrekt nach DDD -- Entities innerhalb eines Aggregates duerfen Zustand haben.
**Risiko**: Expense.java wird gross. Mitigation: Klare Methoden-Aufspaltung, Receipt-Logik in Receipt-Entity kapseln.

### HS-2: READY_FOR_SETTLEMENT als Zwischenstatus (LOW)
**Frage**: Braucht man den Zwischenstatus READY_FOR_SETTLEMENT oder reicht die Pruefung "alle approved?" beim Settle-Command?
**Position**: Der Zwischenstatus ist wertvoll fuer die UI (zeigt an: "Abrechnung kann jetzt abgeschlossen werden") und vermeidet wiederholte Pruefungen. Er ist reversibel (zurueck zu OPEN bei neuem Receipt).
**Entscheidung**: Einfuehren. Geringer Aufwand, hoher UX-Wert.

### HS-3: Accommodation Splitting -- Woher kommen die StayPeriods? (HIGH)
**Frage**: StayPeriod existiert nur im Trips SCS. Wie bekommt Expense diese Daten?
**Option A**: Neues Event `StayPeriodUpdated` von Trips -> Expense projiziert in TripProjection.
**Option B**: StayPeriod-Daten im `ParticipantJoinedTrip`-Event mitliefern (erfordert Event-Erweiterung).
**Option C**: Synchroner API-Call an Trips SCS (verletzt SCS-Prinzip, verworfen).
**Position**: Option A bevorzugt. Eigenes Event ist sauber, unabhaengig, und StayPeriod aendert sich unabhaengig vom Beitritt. Option B ist problematisch, weil StayPeriod oft erst nach dem Beitritt gesetzt wird.
**Entscheidung**: Option A -- neues `StayPeriodUpdated` Event.

### HS-4: Accommodation-Split-Berechnung (MEDIUM)
**Frage**: Wie genau wird ein Accommodation-Receipt auf Naechte aufgeteilt?
**Beispiel**: Ferienhaus 700 EUR fuer 7 Naechte. Teilnehmer A: 7 Naechte, B: 5 Naechte, C: 3 Naechte.
```
  Kosten pro Nacht: 700 / 7 = 100 EUR/Nacht
  FALSCH: 700 / (7+5+3) = 46.67 pro Personennacht
```
**Klarstellung**: Die Gesamtkosten der Unterkunft sind fix (700 EUR). Die Aufteilung erfolgt nach Personennaechten:
```
  Total Personennaechte: 7 + 5 + 3 = 15
  A anteil: 7/15 * 700 = 326.67 EUR
  B anteil: 5/15 * 700 = 233.33 EUR
  C anteil: 3/15 * 700 = 140.00 EUR
```
**Position**: Accommodation-Receipts verwenden einen separaten Split-Algorithmus, der NICHT die globalen ParticipantWeightings nutzt, sondern Naechte-basiert aufteilt. Dies betrifft NUR die Anteilsberechnung fuer diesen einen Receipt, nicht die globale Gewichtung.
**Entscheidung**: Receipt bekommt ein optionales `SplitStrategy`-Konzept. Default = WEIGHTED (bestehend), Alternative = BY_NIGHTS (fuer Accommodation).

### HS-5: SplitStrategy als Value Object oder Receipt-Subtyp? (MEDIUM)
**Frage**: Modellieren wir die unterschiedliche Aufteilungslogik als Strategy-Pattern, als Receipt-Subtyp, oder als separate Felder?
**Option A**: `SplitStrategy` Enum auf Receipt (WEIGHTED, BY_NIGHTS) + optionale NightAllocation-Daten.
**Option B**: Zwei Receipt-Subklassen (StandardReceipt, AccommodationReceipt).
**Option C**: `AccommodationReceipt` als eigener Entity-Typ innerhalb Expense.
**Position**: Option A bevorzugt. Ein Enum mit optionalen Zusatzdaten ist einfacher als Vererbung in JPA und haelt Receipt als einzelne Entity. Die NightAllocation-Daten (Map<UUID, Integer> participantNights) werden beim Erstellen des Accommodation-Receipts aus der TripProjection berechnet und dann im Receipt gespeichert (nicht bei jedem calculateBalances neu berechnet).
**Entscheidung**: Option A -- SplitStrategy Enum + NightAllocation VO.

### HS-6: Wer darf Receipts einreichen? (LOW)
**Frage**: Nur der Bezahler oder jeder Participant?
**Position**: Jeder Participant darf Receipts einreichen (submittedBy). Das paidBy-Feld gibt an, wer tatsaechlich bezahlt hat. Use Case: "Mein Partner hat bezahlt, ich reiche den Beleg ein."
**Entscheidung**: submittedBy != paidBy ist explizit erlaubt. Four-Eyes-Regel prueft gegen submittedBy, nicht gegen paidBy.

### HS-7: Receipts ohne Review bei kleinen Gruppen (RESOLVED)
**Frage**: Ist Four-Eyes bei Solo-Organizer-Reisen sinnvoll?
**Problem**: Solo-Organizer kann eigene Belege nicht reviewen -> Deadlock.
**Entscheidung (2026-03-16)**: **Zwei-Modus-Ansatz** (automatische Erkennung):
- **Solo-Organizer**: Belege werden direkt eingeschlossen (kein Review-Gate). Settlement sofort moeglich.
- **Multi-Organizer**: Four-Eyes-Review aktiv (SUBMITTED -> APPROVED/REJECTED).
- Modus wird automatisch anhand der Anzahl Organizer auf der Reise bestimmt.
- Begruendung: Solo-Organizer ist die primaere Persona (Familien-Reise mit 1 Organisator).

### HS-8: ParticipantJoinedTrip -- Companions/Dependents (LOW)
**Frage**: Koennen Dependents (Mitreisende ohne Login) Receipts einreichen oder reviewen?
**Position**: Nein. Dependents haben keinen Login und koennen daher keine Aktionen im System ausfuehren. Sie erscheinen nur in Weightings (z.B. Gewichtung 0.5 fuer Kinder). Receipts werden nur von Accounts (Mitgliedern mit Login) eingereicht und reviewed.
**Entscheidung**: submittedBy und reviewerId muessen Account-IDs sein (haben Login). Dependent-IDs erscheinen nur in Weightings und paidBy (theoretisch -- aber praktisch unueblich).

---

## 11. Aggregate Design-Entscheidungen

### E1: Receipt bleibt Entity innerhalb Expense

**Begruendung**:
- Receipt hat keinen eigenstaendigen Lifecycle (existiert nur im Kontext einer Expense)
- Alle Invarianten (Four-Eyes, Status-Transitionen) beziehen sich auf den Expense-Kontext
- Kein externer Zugriff auf einzelne Receipts ohne Expense
- Konsistenz zwischen Receipt-Status und Expense-Status muss transaktional sein

### E2: SettlementPlan als Value Object innerhalb Expense

**Begruendung**:
- Settlement ist eine Berechnung UEBER alle Receipts und Weightings
- Kein eigener Lifecycle -- wird genau einmal berechnet bei `settle()`
- Immutable nach Berechnung (Expense ist SETTLED, keine Aenderungen mehr)
- Settlement ohne Expense-Kontext ist bedeutungslos

### E3: ExpenseCategory als Enum (nicht als eigenes Aggregate)

**Begruendung**:
- Feste, vom System vorgegebene Kategorien
- Kein Tenant-spezifisches Customizing in Iteration 6
- Kann spaeter zu einem eigenen Konzept erweitert werden (Custom Categories)
- Enum ist die einfachste Loesung mit geringstem Coupling

### E4: NightAllocation wird bei Receipt-Erstellung eingefroren

**Begruendung**:
- Die Nacht-Aufteilung basiert auf StayPeriod-Daten zum Zeitpunkt der Receipt-Erstellung
- Spaetere StayPeriod-Aenderungen betreffen nur NEUE Accommodation-Receipts
- Vermeidet retroaktive Neuberechnungen und ueberraschende Saldoaenderungen
- Explizite Entscheidung: Konsistenz zum Erstellungszeitpunkt > Eventual Correctness

---

## 12. Neue Event-Contracts (fuer travelmate-common)

### Events publiziert von Expense SCS

```java
// Bestehend (unveraendert)
ExpenseCreated(tenantId, tripId, expenseId, occurredOn)
ExpenseSettled(tenantId, tripId, expenseId, occurredOn)

// NEU
ReceiptSubmitted(tenantId, tripId, expenseId, receiptId, submittedBy,
                 amount, category, occurredOn)

ReceiptApproved(tenantId, tripId, expenseId, receiptId, reviewerId, occurredOn)

ReceiptRejected(tenantId, tripId, expenseId, receiptId, reviewerId,
                reason, occurredOn)

ExpenseReadyForSettlement(tenantId, tripId, expenseId, occurredOn)
```

**Anmerkung**: ReceiptSubmitted, ReceiptApproved, ReceiptRejected sind zunaechst nur interne Domain Events (registriert im Aggregate, publiziert via ApplicationEventPublisher). Ob sie auf RabbitMQ publiziert werden, haengt davon ab, ob andere SCS sie konsumieren muessen. Aktuell: NEIN -- kein anderes SCS braucht Receipt-Status-Informationen.

**Entscheidung**: ExpenseReadyForSettlement wird ebenfalls nur intern publiziert. Kein externer Consumer.

### Events publiziert von Trips SCS (NEU)

```java
StayPeriodUpdated(tenantId, tripId, participantId,
                  arrivalDate, departureDate, occurredOn)
```

**Routing Key**: `trips.stay-period-updated` (in RoutingKeys.java)

---

## 13. Erweitertes Receipt-Modell

```
Receipt (Entity innerhalb Expense) -- FINAL DESIGN
  +-- receiptId: ReceiptId
  +-- description: String
  +-- amount: Amount
  +-- paidBy: UUID
  +-- submittedBy: UUID                         [NEU]
  +-- date: LocalDate
  +-- category: ExpenseCategory                 [NEU]
  +-- reviewStatus: ReviewStatus                [NEU, default SUBMITTED]
  +-- reviewerId: UUID (nullable)               [NEU]
  +-- rejectionReason: String (nullable)        [NEU]
  +-- splitStrategy: SplitStrategy              [NEU, default WEIGHTED]
  +-- nightAllocation: NightAllocation (nullable) [NEU, nur bei BY_NIGHTS]

ReviewStatus
  SUBMITTED -> APPROVED
  SUBMITTED -> REJECTED
  REJECTED  -> SUBMITTED (via resubmit, reset)

SplitStrategy (Enum)
  WEIGHTED     -- Standard: globale ParticipantWeighting
  BY_NIGHTS    -- Accommodation: Aufteilung nach Personennaechten

NightAllocation (Record/VO)
  +-- participantNights: Map<UUID, Integer>  (participantId -> Anzahl Naechte)
  -- Invariant: alle participantIds muessen in Expense.weightings vorhanden sein
  -- Invariant: mindestens ein Eintrag mit nights > 0
```

---

## 14. Erweiterter Balance-Algorithmus

Der bestehende `calculateBalances()` muss um die SplitStrategy erweitert werden:

```
Fuer jeden Receipt:
  WENN splitStrategy == WEIGHTED:
    share(p) = receipt.amount * (p.weight / totalWeight)    [bestehend]

  WENN splitStrategy == BY_NIGHTS:
    totalNights = sum(nightAllocation.values())
    share(p) = receipt.amount * (nightAllocation.get(p) / totalNights)
    -- Participants ohne Eintrag in nightAllocation: share = 0

balance(p) = sum(paid by p) - sum(shares across all receipts)
```

---

## 15. Zusammenfassung der Aenderungen

### Neue Value Objects
- `ExpenseCategory` (Enum: ACCOMMODATION, GROCERIES, RESTAURANT, ACTIVITY, TRANSPORT, FUEL, HEALTH, OTHER)
- `ReviewStatus` (Enum: SUBMITTED, APPROVED, REJECTED)
- `SplitStrategy` (Enum: WEIGHTED, BY_NIGHTS)
- `NightAllocation` (Record: Map<UUID, Integer> participantNights)
- `SettlementPlan` (Record: List<Transfer>, LocalDate calculatedAt)
- `Transfer` (Record: UUID from, UUID to, Amount amount)

### Erweiterte Entities
- `Receipt`: +submittedBy, +category, +reviewStatus, +reviewerId, +rejectionReason, +splitStrategy, +nightAllocation
- `ExpenseStatus`: +READY_FOR_SETTLEMENT

### Neue Event-Contracts (travelmate-common)
- `ReceiptSubmitted`, `ReceiptApproved`, `ReceiptRejected` (intern)
- `ExpenseReadyForSettlement` (intern)
- `StayPeriodUpdated` (extern, Trips -> Expense)

### Neue Commands
- `SubmitReceiptCommand`, `SubmitAccommodationReceiptCommand`
- `ApproveReceiptCommand`, `RejectReceiptCommand`, `ResubmitReceiptCommand`

### Erweiterte Read Models
- `TripProjection`: +startDate, +endDate, +arrivalDate/departureDate pro Participant
- Neue Representations: `ReceiptReviewList`, `SettlementSummary`, `CategoryBreakdown`

### Neue RoutingKey
- `trips.stay-period-updated`

### Flyway Migrations (voraussichtlich)
- V4: receipt_review_columns (submittedBy, reviewStatus, reviewerId, rejectionReason, category, splitStrategy)
- V5: night_allocation table (receipt_id, participant_id, nights)
- V6: settlement_plan + transfer tables
- V7: trip_projection erweiterung (startDate, endDate, arrivalDate, departureDate)

---

## 16. Naechste Schritte

1. **ADR-0015 schreiben**: Receipt Review Workflow und Four-Eyes-Prinzip
2. **ADR-0016 schreiben**: Accommodation Splitting und SplitStrategy-Design
3. **Event-Contract implementieren**: StayPeriodUpdated in travelmate-common
4. **TDD Red-Green-Refactor**: Domain-Tests zuerst (Receipt Status-Transitionen, Four-Eyes-Invariante, Debt Simplification, Night-Split-Algorithmus)
5. **ArchUnit erweitern**: Neue Invarianten-Tests fuer Review-Workflow
