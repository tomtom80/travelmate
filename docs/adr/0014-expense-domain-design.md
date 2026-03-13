# ADR-0014: Expense Domain Design — Gewichtete Abrechnung mit TripProjection

## Status

Accepted

## Context

Mit Iteration 5 wird das Expense SCS als dritte Fachdomaene eingefuehrt. Die zentrale Anforderung ist die **gewichtete Kostenaufteilung** fuer abgeschlossene Reisen:

1. **Belege erfassen**: Jeder Teilnehmer kann Belege (Einkauf, Restaurant, etc.) mit Betrag und Bezahler erfassen.
2. **Gewichtung pro Teilnehmer**: Erwachsene = 1.0, Kleinkind < 3 Jahre = 0.0, Teilzeitreisende = 0.5 (individuell anpassbar).
3. **Saldo berechnen**: Wer hat wie viel bezahlt vs. was muesste er anteilig zahlen? Positiver Saldo = erhaelt Geld zurueck, negativer Saldo = schuldet Geld.
4. **Abrechnung abschliessen**: Nach Abschluss sind keine Aenderungen mehr moeglich.

Das Expense SCS muss die Teilnehmer einer Reise kennen, hat aber keinen direkten Zugriff auf IAM- oder Trips-Daten.

## Decision

### 1. Aggregate: Expense (Event-Sourced Lifecycle)

Das `Expense`-Aggregat wird durch ein `TripCompleted`-Event automatisch erzeugt. Es enthaelt:

- **Receipts** (Entities): Beleg mit Beschreibung, Betrag, Bezahler und Datum
- **ParticipantWeightings** (Value Objects): Gewichtung pro Teilnehmer (default 1.0)
- **Status**: `OPEN` → `SETTLED` (unidirektional)

```
Expense (AggregateRoot)
├── ExpenseId (VO)
├── TenantId (VO)
├── tripId: UUID
├── status: OPEN | SETTLED
├── receipts: List<Receipt>
│   ├── ReceiptId (VO)
│   ├── description: String
│   ├── Amount (VO, > 0)
│   ├── paidBy: UUID
│   └── date: LocalDate
└── weightings: List<ParticipantWeighting>
    ├── participantId: UUID
    └── weight: BigDecimal (>= 0)
```

**Invarianten**:
- Mindestens eine Gewichtung bei Erstellung
- Bezahler muss Teilnehmer sein (`paidBy` in weightings)
- Belege nur im Status OPEN aenderbar
- Abschluss nur mit mindestens einem Beleg

### 2. Balance-Algorithmus (Proportionale Gewichtung)

```
Fuer jeden Beleg:
  share(participant) = beleg.amount * (participant.weight / totalWeight)

balance(participant) = summe(bezahlt) - summe(anteil)
```

Positiver Saldo → Teilnehmer erhaelt Geld. Negativer Saldo → Teilnehmer schuldet Geld. Rundung: `HALF_UP` mit 2 Nachkommastellen.

### 3. TripProjection (Read Model fuer Cross-SCS-Kontext)

Das Expense SCS unterhalt ein lokales Read Model `TripProjection`, das aus Events aufgebaut wird:

```
TripCreated       → TripProjection anlegen (tripId, tenantId, tripName)
ParticipantJoined → Teilnehmer hinzufuegen (participantId, name)
TripCompleted     → Expense automatisch erstellen mit Default-Gewichtungen
```

**Idempotenz**: Alle Event-Handler pruefen auf Duplikate (`existsByTripId`, `addParticipant` ignoriert bekannte IDs). Ein fehlender TripProjection bei `ParticipantJoined` erstellt einen Stub (Eventual Consistency).

### 4. Identitaetsaufloesung im Expense SCS

Anders als IAM (Email → Account → Tenant) oder Trips (Email → TravelParty → Tenant) loest das Expense SCS die Tenant-Zuordnung **ueber die TripProjection** auf:

```
GET /expense/{tripId}
  → TripProjection laden → TenantId extrahieren
  → ExpenseService mit TenantId aufrufen
```

Dies ist architektonisch valide, da Expense-Zugriff immer im Kontext eines konkreten Trips erfolgt.

**Alternative verworfen**: Eine eigene AccountProjection im Expense SCS haette zusaetzliche Events und Persistenz erfordert, ohne funktionalen Mehrwert.

### 5. Domain Events

Das Expense SCS publiziert zwei Events (in `travelmate-common`):

- `ExpenseCreated(tenantId, tripId, expenseId, occurredOn)` — bei automatischer Erstellung
- `ExpenseSettled(tenantId, tripId, expenseId, occurredOn)` — bei Abschluss

Diese Events ermoeglichen zukuenftige Integrationen (z.B. Benachrichtigungen, Statistiken).

## Consequences

### Positiv

- **Saubere Aggregate-Grenzen**: Expense kapselt alle Abrechnungslogik, keine Business-Logik im Service
- **Event-basierte Kopplung**: Keine synchronen Aufrufe zwischen SCS, nur Eventual Consistency
- **Flexible Gewichtung**: Default 1.0, individuell anpassbar (Kind=0, Teilzeit=0.5)
- **Idempotente Consumer**: Duplikat-Erkennung schuetzt gegen Message-Replay
- **Testbar**: 31 Domain-Tests, 13 Service-Tests, 12 Persistence-Tests, 7 Controller-Tests

### Negativ

- **Eventual Consistency**: Zwischen Trip-Abschluss und Expense-Erstellung kann eine kurze Verzoegerung auftreten
- **Name-Denormalisierung**: Teilnehmernamen werden in TripProjection gespeichert und koennen veralten
- **Kein Undo fuer Settlement**: Nach SETTLED sind keine Aenderungen mehr moeglich (bewusste Design-Entscheidung)

## Related

- ADR-0001: SCS-Architektur
- ADR-0008: DDD + Hexagonale Architektur
- ADR-0011: Ubiquitous Language
- ADR-0012: Trip-Einladung und Event-Choreografie
