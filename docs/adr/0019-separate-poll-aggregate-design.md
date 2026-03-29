# ADR-0019: Separate Poll-Aggregate (DatePoll + AccommodationPoll) statt generischem Poll

## Status

Vorgeschlagen

## Context

Mit Iteration 14 werden kollaborative Abstimmungsmechanismen in die Reiseplanung eingefuehrt. Zwei Abstimmungstypen bedienen den Planungsprozess:

1. **DatePoll** -- Terminabstimmung (Doodle-artig): Mehrfachauswahl ueber moegliche Reisezeitraeume
2. **AccommodationPoll** -- Unterkunftsabstimmung: Einfachauswahl mit Re-Vote ueber vorgeschlagene Unterkuenfte

Beide folgen demselben Grundmuster (Optionen sammeln, abstimmen, Organisator entscheidet), unterscheiden sich aber in Abstimmungsmodus, Kandidaten-Typ, Ergebnis-Aktion und Kandidaten-Herkunft.

Die zentrale Designfrage: Ein generisches `Poll<T>`-Aggregat oder zwei eigenstaendige Aggregate?

**Decision Drivers**:

1. Die zwei Abstimmungstypen haben unterschiedliche Invarianten und Abstimmungsmodi
2. Die Kandidaten-Typen unterscheiden sich fundamental (Value Object vs. Entity mit reichem Zustand)
3. Die Ergebnis-Aktionen betreffen unterschiedliche Aggregate (Trip vs. Accommodation)
4. DDD bevorzugt fachlich korrekte Modelle gegenueber technischer Wiederverwendung

## Decision

### Zwei separate Aggregate (DatePoll, AccommodationPoll) statt eines generischen Poll-Aggregats

Die Unterschiede ueberwiegen die Gemeinsamkeiten entlang fuenf Dimensionen:

| Aspekt | DatePoll | AccommodationPoll |
|--------|----------|-------------------|
| Abstimmungsmodus | Multi-Select (Doodle) | Single Active Vote mit Re-Vote |
| Optionen-Typ | DateRange (Value Object) | AccommodationCandidate (Entity mit URL, Preis, Zimmern) |
| Ergebnis-Aktion | Trip.confirmDateRange() | Accommodation.create() |
| Kandidaten-Quelle | Nur Organizer definiert Optionen | Alle Teilnehmer koennen Kandidaten vorschlagen |
| Kandidaten-Lebenszyklus | Einfach (hinzufuegen/entfernen) | Komplex (ACTIVE/ARCHIVED/SELECTED) |

### 1. Unterschiedliche Abstimmungsmodi erzwingen unterschiedliche Invarianten

DatePoll erlaubt Multi-Select: Jeder Teilnehmer kann mehrere Zeitraeume als "passend" markieren (Doodle-Muster). Die Stimme wird als `Set<DateOptionId>` modelliert. AccommodationPoll erlaubt genau eine aktive Stimme pro Teilnehmer (Single Vote). Ein Re-Vote (MoveVote) entfernt die alte Stimme atomar und setzt eine neue. Diese beiden Modi erfordern grundlegend verschiedene Validierungslogik im Aggregat -- ein generisches `Poll<T>` muesste den Modus per Enum oder Flag unterscheiden, was die Invarianten-Pruefung mit bedingter Logik durchsetzt.

### 2. Unterschiedliche Kandidaten-Typen

DatePoll-Optionen sind einfache DateRange Value Objects (startDate, endDate) -- unveraenderlich und zustandslos. AccommodationCandidate ist eine Entity mit reichem Zustand: Name, URL, Adresse, geschaetzter Preis, Notizen, proposedBy-Referenz und eigenem Status-Lifecycle (ACTIVE → ARCHIVED → SELECTED). Ein generisches `Poll<T>` muesste T so abstrakt halten, dass weder DateRange noch AccommodationCandidate sinnvoll getypt waeren.

### 3. Unterschiedliche Ergebnis-Aktionen

Die Bestaetigung eines DatePolls setzt `Trip.confirmDateRange(DateRange)` -- eine Zustandsaenderung auf einem bestehenden Aggregat. Die Auswahl in einem AccommodationPoll erstellt ein neues `Accommodation`-Aggregat aus den Kandidaten-Daten (oder verknuepft mit einem bestehenden, wenn via URL-Import erstellt). Diese voellig unterschiedlichen Seiteneffekte lassen sich nicht sinnvoll in einer generischen Confirm/Select-Operation abstrahieren.

### 4. Unterschiedliche Kandidaten-Quellen

Bei DatePoll definiert ausschliesslich der Organizer die Zeitraum-Optionen. Bei AccommodationPoll koennen alle Account-haltenden Teilnehmer Kandidaten vorschlagen (einschliesslich URL-Import als Kandidat). Diese unterschiedlichen Berechtigungsmodelle fuer die Kandidaten-Verwaltung wuerden in einem generischen Aggregat zu verschachtelter Berechtigungslogik fuehren.

### 5. Unterschiedlicher Kandidaten-Lebenszyklus

DateOptions sind einfach: hinzufuegen oder entfernen, kein eigener Status. AccommodationCandidates durchlaufen einen Lebenszyklus (ACTIVE → ARCHIVED → SELECTED), wobei das Archivieren eines Kandidaten Stimmen auf diesen Kandidaten entfernt (Policy P4 im EventStorming). Dieser Seiteneffekt ist spezifisch fuer AccommodationPoll und hat kein Aequivalent bei DatePoll.

### 6. Generisches Poll wuerde Domain-Sprache verduennen

Ein generisches `Poll<T>` mit `VotingMode`-Enum wuerde fachliche Konzepte hinter technischen Abstraktionen verbergen. "DatePoll" und "AccommodationPoll" sind eigenstaendige Begriffe in der Domaene -- Benutzer sprechen von "Terminabstimmung" und "Unterkunftsabstimmung", nicht von "Poll vom Typ Datum" und "Poll vom Typ Unterkunft". DDD fordert, dass das Modell die Fachsprache widerspiegelt.

### 7. DDD bevorzugt fachlich korrekte Modelle gegenueber technischer Wiederverwendung

Das DDD-Prinzip der strategischen Modellierung empfiehlt, Aggregate entlang fachlicher Grenzen zu schneiden, nicht entlang technischer Aehnlichkeiten. Die scheinbare Strukturaehnlichkeit (Optionen + Stimmen + Status) ist ein technisches Artefakt. Die fachlichen Regeln (wer darf was, wie wird abgestimmt, was passiert bei Bestaetigung) sind grundverschieden. Code-Duplikation zwischen den beiden Aggregaten ist akzeptabel und sogar erwuenscht, da sie unabhaengige Evolution ermoeglicht.

### Aggregate-Strukturen

```
DatePoll (Aggregate Root)
├── DatePollId          : Value Object (UUID)
├── TenantId            : Value Object (aus common)
├── TripId              : Value Object (Referenz auf Trip)
├── PollStatus          : Enum (OPEN, CONFIRMED, CANCELLED)
├── List<DateOption>    : Entity
│   ├── DateOptionId    : Value Object (UUID)
│   └── DateRange       : Value Object (startDate, endDate)
├── List<DateVote>      : Entity
│   ├── DateVoteId      : Value Object (UUID)
│   ├── voterId         : UUID (Account-ID)
│   └── Set<DateOptionId> : ausgewaehlte Optionen (Multi-Select)
└── confirmedOptionId   : DateOptionId? (null bis ConfirmDatePoll)

AccommodationPoll (Aggregate Root)
├── AccommodationPollId     : Value Object (UUID)
├── TenantId                : Value Object (aus common)
├── TripId                  : Value Object (Referenz auf Trip)
├── PollStatus              : Enum (OPEN, DECIDED, CANCELLED)
├── List<AccommodationCandidate> : Entity
│   ├── CandidateId         : Value Object (UUID)
│   ├── name, url, address, estimatedPrice, notes
│   ├── proposedBy          : UUID (Account-ID)
│   └── CandidateStatus     : Enum (ACTIVE, ARCHIVED, SELECTED)
├── List<AccommodationVote> : Entity
│   ├── VoteId              : Value Object (UUID)
│   ├── voterId             : UUID (Account-ID)
│   └── candidateId         : CandidateId
└── selectedCandidateId     : CandidateId? (null bis SelectAccommodation)
```

### Repository-Interfaces (Ports)

```java
// domain/datepoll/DatePollRepository.java
public interface DatePollRepository {
    DatePoll save(DatePoll datePoll);
    Optional<DatePoll> findById(DatePollId id);
    Optional<DatePoll> findOpenByTripId(TenantId tenantId, TripId tripId);
}

// domain/accommodationpoll/AccommodationPollRepository.java
public interface AccommodationPollRepository {
    AccommodationPoll save(AccommodationPoll poll);
    Optional<AccommodationPoll> findById(AccommodationPollId id);
    Optional<AccommodationPoll> findOpenByTripId(TenantId tenantId, TripId tripId);
}
```

## Consequences

### Positiv

- **Klare Invarianten**: Jedes Aggregat erzwingt nur seine eigenen fachlichen Regeln ohne bedingte Logik fuer unterschiedliche Modi
- **Saubere Domain-Sprache**: DatePoll und AccommodationPoll sind eigenstaendige fachliche Konzepte, keine technischen Varianten
- **Unabhaengige Evolution**: Aenderungen an der Terminabstimmung (z.B. Praeferenz-Gewichtung) beruehren die Unterkunftsabstimmung nicht und umgekehrt
- **BC-interne Events**: Keine neuen Cross-SCS-Event-Vertraege in travelmate-common noetig -- alle Poll-Events bleiben im Trips-Bounded-Context
- **TenantId-Scoping**: Beide Aggregate folgen dem etablierten Muster der Tenant-Isolation
- **Aehnliche Repository-Patterns**: Trotz getrennter Aggregate bleiben die Repository-Interfaces konsistent und vorhersagbar

### Negativ

- **Zwei Aggregate statt einem**: Hoehere Anzahl an Klassen (Aggregate Roots, Entities, Value Objects, Repositories, JPA-Entities, Controller, Services)
- **Strukturelle Duplikation**: PollStatus-Enum, Voter-Pruefung und aehnliche Muster werden in beiden Aggregaten separat implementiert
- **Zwei Flyway-Migrationen**: V13 (date_poll) und V14 (accommodation_poll) statt einer einzelnen Migration
- **Lernkurve**: Entwickler muessen beide Aggregate verstehen, auch wenn sie strukturell aehnlich sind

## Alternatives

### Option 2: Generisches Poll<T>-Aggregat mit VotingMode-Enum

Ein einzelnes `Poll<T>`-Aggregat parametrisiert ueber den Kandidaten-Typ (DateRange vs. AccommodationCandidate) mit einem `VotingMode`-Enum (MULTI_SELECT, SINGLE_VOTE) zur Steuerung der Abstimmungslogik.

- **Vorteile**: Weniger Klassen, gemeinsame Infrastruktur (eine Flyway-Migration, ein Controller, ein Service)
- **Nachteile**: Bedingte Invarianten (`if (mode == MULTI_SELECT) ...` in jeder Validierung), Typparameter T muss so abstrakt sein, dass AccommodationCandidate-spezifische Operationen (Archivieren, Status-Lifecycle) nicht typsicher ausdrueckbar sind, Domain-Sprache wird verduennt ("Poll vom Typ X" statt "Terminabstimmung"), unterschiedliche Ergebnis-Aktionen erfordern Strategy-Pattern oder aehnliche Indirektion
- **Bewertung**: Abgelehnt -- die Invarianten unterscheiden sich zu stark, die generische Abstraktion wuerde die fachliche Korrektheit opfern

### Option 3: Poll als Teil des Trip-Aggregats

DatePoll und AccommodationPoll als Entities innerhalb des Trip-Aggregats modellieren.

- **Vorteile**: Keine separate Transaktionsgrenze, direkter Zugriff auf Trip-Zustand
- **Nachteile**: Trip-Aggregat wuerde massiv ueberladen (Single Responsibility Violation), unterschiedliche Lifecycles erzwingen unnatuerliche Kopplung, jede Stimmabgabe wuerde das gesamte Trip-Aggregat sperren (Concurrency-Problem bei parallelen Abstimmungen), widerspricht dem Prinzip kleiner Aggregate
- **Bewertung**: Abgelehnt -- unterschiedliche Consistency Boundaries und Lifecycles erfordern separate Aggregate

### Option 4: Externer Polling-Service (eigenes SCS)

Abstimmungen in ein eigenes Self-Contained System auslagern.

- **Vorteile**: Unabhaengige Skalierung, Wiederverwendbarkeit fuer andere Abstimmungstypen
- **Nachteile**: Abstimmungen sind domaenen-kritisch fuer die Reiseplanung und gehoeren fachlich in den Trips-Bounded-Context, Cross-SCS-Kommunikation (Events, eventual consistency) fuer ein Feature das eng an Trip-Lifecycle gebunden ist, zusaetzliche Infrastruktur (Datenbank, Deployment) fuer ein Feature mit begrenzter Komplexitaet -- unverhaehltnismaessiger Aufwand
- **Bewertung**: Abgelehnt -- YAGNI, die Abstimmungen sind integraler Bestandteil der Reiseplanung

## Related

- ADR-0001: SCS-Architektur
- ADR-0008: DDD + Hexagonale Architektur
- ADR-0011: Ubiquitous Language
- ADR-0015: Shopping List Aggregate Design (Vorbild fuer Aggregate-Design-ADR)
- EventStorming: `docs/design/eventstorming/iteration-14-scope.md`
