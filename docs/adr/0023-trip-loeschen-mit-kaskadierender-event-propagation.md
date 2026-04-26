# ADR-0023: Reise loeschen mit kaskadierender Event-Propagation

## Status
Akzeptiert

## Kontext

Organisatoren benoetigen die Moeglichkeit, eine fehlerhaft erstellte Reise wieder zu entfernen. Eine Reise ist innerhalb des Trips-SCS mit zahlreichen weiteren Aggregaten verknuepft (Invitations, MealPlan, ShoppingList, DatePoll, AccommodationPoll, Accommodation) und das nachgelagerte Expense-SCS haelt eine TripProjection sowie ggf. ein Expense-Ledger zur selben tripId.

Eine Loeschung muss:
1. innerhalb des Trips-SCS alle verknuepften Aggregate konsistent entfernen,
2. ueber die SCS-Grenze hinweg im Expense-SCS die TripProjection und das Expense-Ledger entfernen,
3. waehrend eines aktiven Buchungsversuchs (AccommodationPoll im Status `AWAITING_BOOKING`) abgewiesen werden, da sonst Buchungsdaten in inkonsistentem Zustand zurueckblieben,
4. nur in Status `PLANNING` erlaubt sein, um keine bereits bestaetigten oder begonnenen Reisen retroaktiv zu loeschen.

## Entscheidung

**Loeschung kaskadiert auf Application-Service-Ebene innerhalb des Trips-SCS und propagiert via `TripDeleted`-Domain-Event ueber RabbitMQ ans Expense-SCS.** Es wird KEINE Datenbank-Kaskade (`ON DELETE CASCADE`) eingefuehrt, weder innerhalb eines SCS noch ueber SCS-Grenzen hinweg.

### Konkrete Sequenz in `TripService.deleteTrip(TripId)`

1. Trip laden, Statusguard `PLANNING` pruefen
2. Guard: AccommodationPoll im Status `AWAITING_BOOKING` blockiert die Loeschung
3. Kaskade in fester Reihenfolge:
   - `invitationRepository.deleteByTripId(tripId)`
   - `mealPlanRepository.deleteByTripId(tripId)`
   - `shoppingListRepository.findByTripIdAndTenantId(...)` &rarr; `delete`
   - `datePollRepository.findLatestByTripId(...)` &rarr; `delete`
   - `accommodationPollRepository.findLatestByTripId(...)` &rarr; `delete`
   - `accommodationRepository.deleteByTripId(tripId)`
   - `tripRepository.delete(trip)`
4. `TripDeleted(tenantId, tripId, occurredOn)`-Event via Spring `ApplicationEventPublisher` publizieren
5. `@TransactionalEventListener(AFTER_COMMIT)` in `DomainEventPublisher` versendet das Event an Routing Key `trips.trip-deleted`
6. Expense-SCS konsumiert via `expense.trip-deleted`-Queue und loescht idempotent `Expense` + `TripProjection` ueber `expenseRepository.deleteByTripId` + `tripProjectionRepository.deleteByTripId`

### Begruendung

1. **Aggregat-Grenzen werden gewahrt**: Application-Service-Kaskade ruft Aggregat-eigene Repository-Methoden, statt Foreign-Key-Konstrukte ueber Aggregat-Grenzen aufzubauen.
2. **Cross-SCS-Konsistenz folgt dem etablierten Event-Choreographie-Pattern**: gleiche Mechanik wie `TripCreated`, `ParticipantJoinedTrip`, `TripCompleted`.
3. **Idempotente Consumer-Logik**: `deleteByTripId` mit 0 Treffern ist erfolgreich &rarr; passt zur At-least-once-Garantie von RabbitMQ.
4. **Domaen-Logik verbleibt im Trips-Bounded-Context**: Expense haelt nur Lese-Projektionen, die als Folgeartefakte mit dem Lebenszyklus der Reise zu pflegen sind.
5. **AWAITING_BOOKING-Guard verhindert Datenmuell**: Aktive Buchungsversuche signalisieren externe, nicht-rollback-faehige Aktionen (E-Mail/Telefon/Buchungsplattform). Eine Loeschung waehrend dieses Zustands zerstoerte den Audit-Trail.

## Konsequenzen

### Positiv
- Loeschung ist idempotent und resilient gegen Wiederholungs-Lieferungen
- Klare Verantwortlichkeit: Trips-SCS ist Owner der Daten, Expense-SCS reagiert auf Events
- Keine Schemakopplung zwischen Trips- und Expense-Datenbanken
- Audit-Trail bleibt ueber `TripDeleted`-Event (mit `occurredOn`) erhalten

### Negativ
- Eventuelles Konsumenten-Lag erzeugt kurzes Inkonsistenzfenster (Expense-Ledger noch sichtbar nach Trip-Loeschung)
- Fehler im Expense-Consumer landen in der DLQ und muessen manuell oder via Retry geloest werden
- Erfordert Pflege der Repository-`deleteByTripId`-Methoden in beiden SCS

### Risiken
- **Vergessenes Aggregat in der Kaskade** &rarr; verwaiste Daten. Mitigation: Service-Test verifiziert alle Repository-Aufrufe explizit; ArchUnit kann zukuenftig pruefen, dass jedes neue tripId-FK-Aggregat eine `deleteByTripId`-Methode definiert.
- **Race Condition Loeschung vs. paralleler Booking-Start** &rarr; durch Statusguard und transaktionalen Aufruf abgedeckt.

## Alternativen

- **Datenbank-CASCADE**: verworfen wegen Verletzung der Aggregat-Grenzen und Unmoeglichkeit der Cross-SCS-Anwendung.
- **Soft-Delete-Flag**: verworfen, da PLANNING-only Loeschung explizit das Entfernen aller Spuren bezweckt; Trips in `CONFIRMED+` werden stattdessen abgesagt (`cancel`).
- **Saga / Process Manager**: Overkill fuer eine einfache Loeschoperation; Event-Choreographie ist ausreichend.
