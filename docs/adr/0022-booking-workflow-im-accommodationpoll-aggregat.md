# ADR-0022: Booking-Workflow im AccommodationPoll-Aggregat

## Status
Vorgeschlagen

## Kontext

Nach der Abstimmung in einer AccommodationPoll muss der Organizer die Unterkunft extern buchen (Telefon, E-Mail, Buchungsplattform). Dabei kann die Buchung fehlschlagen (ausgebucht, Preisaenderung), woraufhin der naechstbeste Kandidat versucht werden soll. Wenn alle Kandidaten erschoepft sind, muss die Abstimmung mit neuen Kandidaten wiederoeffnet werden koennen.

Die zentrale Designfrage ist, ob der Booking-Workflow:
- (A) das bestehende AccommodationPoll-Aggregat erweitert, oder
- (B) als eigenstaendiges BookingWorkflow-Aggregat modelliert wird.

### Anforderungen
1. Buchungsversuch pro Kandidat tracken (Erfolg/Misserfolg/Notizen)
2. Automatischer Fallback zum naechstbesten Kandidaten nach Stimmenzahl
3. Wiederoeffnung der Abstimmung wenn alle Kandidaten erschoepft sind
4. Erfolgreiche Buchung erzeugt das Accommodation-Aggregat

## Entscheidung

**Option A: Erweiterung des AccommodationPoll-Aggregats** um BookingAttempt-Entities und erweiterte Statusmaschine.

### Begruendung

1. **Invarianten-Naehe**: Die Fallback-Logik ("naechster Kandidat nach Stimmenzahl") benoetigt direkten Zugriff auf die Kandidatenliste und die Stimmenverteilung. Ein separates Aggregat muesste diese Daten duplizieren oder referenzieren.

2. **Transaktionale Konsistenz**: Buchungsergebnis (FAILED) und Kandidatenwechsel muessen atomar erfolgen. Innerhalb eines Aggregats ist das garantiert; ueber Aggregatgrenzen hinweg waere ein Saga/Process-Manager noetig.

3. **Lebenszykl-Kohaerenz**: Poll und Booking-Workflow sind Phasen desselben Lebenszyklus (Unterkunftsfindung), nicht unabhaengige Prozesse. Die Poll endet erst, wenn eine Buchung erfolgreich ist oder der Vorgang abgebrochen wird.

4. **Aggregate-Groesse bleibt handhabbar**: BookingAttempts sind wenige (typisch 1-3, maximal = Anzahl Kandidaten). Kein Skalierbarkeitsproblem.

### Erweiterte Statusmaschine

```
OPEN --[confirm]--> BOOKING --[recordSuccess]--> BOOKED
                         |--[recordFailure + next exists]--> BOOKING
                         |--[recordFailure + exhausted]--> REOPENED
                         |--[cancel]--> CANCELLED
OPEN --[cancel]--> CANCELLED
REOPENED --[addCandidate/vote]--> REOPENED
REOPENED --[confirm]--> BOOKING
REOPENED --[cancel]--> CANCELLED
```

### Neue Domain-Elemente
- `BookingAttempt` (Entity): bookingAttemptId, candidateId, outcome (PENDING/SUCCEEDED/FAILED), notes, attemptedAt
- `BookingAttemptId` (Value Object, Record)
- `BookingOutcome` (Enum)
- `AccommodationPollStatus`: OPEN, BOOKING, BOOKED, REOPENED, CANCELLED (ersetzt CONFIRMED durch BOOKING + BOOKED)

### Anpassung des Application Service
- `confirmPoll()` erzeugt kein Accommodation mehr, sondern startet Booking-Phase
- Neues `recordBookingSuccess()` erzeugt Accommodation (ueber bestehenden `applyCandidateAsAccommodation`)
- Neues `recordBookingFailure()` triggert Fallback oder Wiederoeffnung

## Konsequenzen

### Positiv
- Kein neues Aggregat, kein neues Repository, keine neue JPA-Entity-Hierarchie
- Fallback-Logik direkt im Aggregat mit voller Kenntnis der Stimmverteilung
- Atomare Zustandsuebergaenge ohne Saga/Process-Manager
- Backward-kompatible Flyway-Migration (CONFIRMED -> BOOKED)
- Kein neuer Cross-SCS-Event-Contract noetig

### Negativ
- AccommodationPoll-Aggregat wird komplexer (5 statt 3 Status, neue Entity)
- `assertOpen()` wird zu `assertOpenOrReopened()` — bestehende Tests muessen angepasst werden
- REOPENED-Status erfordert UI fuer "Neue Kandidaten hinzufuegen + erneut abstimmen"

## Alternativen

### Option B: Eigenstaendiges BookingWorkflow-Aggregat
- **Vorteile**: Klare Trennung von Abstimmung und Buchung; AccommodationPoll bleibt schlank
- **Nachteile**: Muss Kandidaten + Stimmen duplizieren oder referenzieren; benoetigt Saga fuer Fallback-Koordination; mehr Infrastruktur (Repository, JPA-Entity, Migration); Konsistenzrisiko zwischen zwei Aggregaten

### Option C: Booking-Status am Accommodation-Aggregat
- **Vorteile**: Buchungsdaten direkt an der Unterkunft
- **Nachteile**: Accommodation existiert erst nach erfolgreicher Buchung; wuerde den Lebenszyklus umkehren (erst Accommodation anlegen, dann buchen); widerspricht dem aktuellen Design wo Accommodation das Ergebnis der Buchung ist

## Referenzen
- Vaughn Vernon, "Implementing DDD", Kapitel 10: Aggregates — "Design small aggregates" + "Reference other aggregates by identity"
- ADR-0019: Separate Poll Aggregates (DatePoll vs AccommodationPoll)
- AccommodationPoll Aggregate: `travelmate-trips/src/main/java/de/evia/travelmate/trips/domain/accommodationpoll/`
