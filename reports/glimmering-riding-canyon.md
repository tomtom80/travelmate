# Iteration 17 — Delivery Plan: Trip Lifecycle Completion + Küchendienst

**Datum**: 2026-04-12
**Target Version**: v0.17.0
**Input**: Product Backlog Review, Codebase-Analyse Trip/MealPlan Aggregates, Iteration-16-Retrospektive

**Status**: PLANNING

---

## Planning Goal

Iteration 17 vervollständigt den Trip-Lebenszyklus um die fehlenden CRUD-Operationen (Bearbeiten, Löschen) und führt **Küchendienst** als erstes kollaboratives Feature der Ausführungsphase ein.

Nach dieser Iteration:
- Organisatoren können Reisename und -beschreibung bearbeiten (PLANNING/CONFIRMED)
- Organisatoren können fehlerhaft erstellte Reisen löschen (nur PLANNING), mit sauberer Kaskade über alle verknüpften Aggregate und Downstream-Cleanup in Expense
- Organisatoren können Küchendienst pro Mahlzeit zuweisen — transparente Kochverantwortung
- Product Backlog spiegelt den tatsächlichen Implementierungsstand wider

**Kein neues SCS** — alle Änderungen in Trips (Core Domain) + travelmate-common + Expense (Consumer).

---

## Architekturentscheidungen

| ADR | Entscheidung | Begründung |
|-----|-------------|------------|
| ADR-0023 (NEU) | TripDeleted-Event löst kaskadierende Bereinigung über Aggregate und SCS-Grenzen aus | Löschung einer PLANNING-Reise muss alle verknüpften Daten entfernen (Einladungen, Polls, Essensplan, Einkaufsliste) innerhalb Trips SCS und Ledger in Expense archivieren/löschen. Applikations-Level-Kaskade statt DB-Kaskade bewahrt Aggregate-Grenzen. |

---

## Stories

### S17-A: Reise bearbeiten (US-TRIPS-005)
**Priorität**: MUST | **Size**: S

Organisator kann Name und Beschreibung einer Reise bearbeiten, solange Status PLANNING oder CONFIRMED ist.

**Domain-Änderungen:**
- `Trip.rename(TripName newName)` — Status-Guard (PLANNING oder CONFIRMED)
- `Trip.updateDescription(String newDescription)` — selber Guard
- Neuer Command: `EditTripCommand(UUID tripId, String name, String description)`
- `TripService.editTrip(EditTripCommand)` Orchestrierung

**Controller:**
- `GET /{tripId}/edit` — Edit-Formular rendern (nur PLANNING/CONFIRMED)
- `POST /{tripId}/edit` — Änderungen speichern, Redirect zu Detail

**Templates:**
- Neues `trip/edit.html` (oder Inline-HTMX-Edit auf `trip/detail.html`)
- "Bearbeiten"-Button auf Detail-Seite, bedingt nach Status

**Dateien:**
- `travelmate-trips/.../domain/trip/Trip.java`
- `travelmate-trips/.../application/TripService.java`
- `travelmate-trips/.../application/command/EditTripCommand.java` (neu)
- `travelmate-trips/.../adapters/web/TripController.java`
- `travelmate-trips/.../templates/trip/detail.html`
- `travelmate-trips/.../templates/trip/edit.html` (neu)

---

### S17-B: Reise löschen (US-TRIPS-006)
**Priorität**: MUST | **Size**: M

Organisator kann eine Reise im Status PLANNING löschen. Löschung kaskadiert zu allen verknüpften Aggregaten und publiziert `TripDeleted` für Expense-Cleanup.

**Domain-Änderungen (travelmate-common):**
- Neues Event: `TripDeleted` Record in `common/events/trips/`
- Neuer Routing Key: `RoutingKeys.TRIP_DELETED = "trips.trip-deleted"`

**Domain-Änderungen (Trips):**
- `TripRepository.delete(Trip trip)` — neue Repository-Methode
- Status-Guard: nur PLANNING erlaubt Löschung
- Zusätzlicher Guard: Löschung blockiert, wenn ein Poll im BOOKING-Status ist

**Application Service (Trips):**
- `TripService.deleteTrip(TripId tripId, UUID actorId)` — Kaskade:
  1. Alle Invitations für tripId löschen
  2. MealPlan für tripId löschen (falls vorhanden)
  3. ShoppingList für tripId löschen (falls vorhanden)
  4. DatePoll(s) für tripId löschen
  5. AccommodationPoll(s) für tripId löschen
  6. Accommodation für tripId löschen
  7. Trip löschen
  8. `TripDeleted` Event publizieren

**Cross-SCS (Expense):**
- Neue Queue `expense.trip-deleted` in `RabbitMqConfig`
- Neuer Consumer: `TripEventConsumer.onTripDeleted(TripDeleted event)`
- `ExpenseService.onTripDeleted(TripDeleted)` — Ledger löschen/archivieren (idempotent)

**Neue Repository-Methoden (Trips):**
- `InvitationRepository.deleteAllByTripId(TripId)`
- `MealPlanRepository.deleteByTripId(TripId)`
- `ShoppingListRepository.deleteByTripId(TripId)`
- `DatePollRepository.deleteAllByTripId(TripId)`
- `AccommodationPollRepository.deleteAllByTripId(TripId)`
- `AccommodationRepository.deleteByTripId(TripId)`

**Controller:**
- `POST /{tripId}/delete` — Löschung bestätigen, Redirect zur Trip-Liste

**Templates:**
- "Reise löschen"-Button im Danger-Zone-Bereich (nur PLANNING, nur Organisator)
- Bestätigungs-Dialog

**Dateien:**
- `travelmate-common/.../events/trips/TripDeleted.java` (neu)
- `travelmate-common/.../messaging/RoutingKeys.java`
- `travelmate-trips/.../domain/trip/Trip.java`
- `travelmate-trips/.../domain/trip/TripRepository.java`
- `travelmate-trips/.../application/TripService.java`
- `travelmate-trips/.../adapters/web/TripController.java`
- `travelmate-trips/.../adapters/persistence/Jpa*Repository.java` (mehrere)
- `travelmate-trips/.../templates/trip/detail.html`
- `travelmate-expense/.../adapters/messaging/RabbitMqConfig.java`
- `travelmate-expense/.../adapters/messaging/TripEventConsumer.java`
- `travelmate-expense/.../application/ExpenseService.java`

---

### S17-C: Küchendienst zuweisen (US-TRIPS-035)
**Priorität**: SHOULD | **Size**: M

Organisator kann eine oder mehrere Reiseparteien als Küchendienst für eine ausgeführte Mahlzeit zuweisen. Nur PLANNED-Slots (nicht SKIP/EATING_OUT) akzeptieren Küchendienst.

**Domain-Änderungen:**
- `MealSlot`: neues Feld `List<UUID> kitchenDutyParticipantIds`
- `MealSlot.assignKitchenDuty(List<UUID> participantIds)` — Guard: Status muss PLANNED sein
- `MealSlot.clearKitchenDuty()`
- `MealPlan.assignKitchenDuty(MealSlotId slotId, List<UUID> participantIds)`
- Neuer Command: `AssignKitchenDutyCommand(UUID tripId, UUID mealSlotId, List<UUID> participantIds)`

**Flyway Migration:**
- `V25__kitchen_duty.sql` — JSON-Spalte `kitchen_duty_participant_ids` auf `meal_slot` (folgt dem Pattern aus V21 mit `candidate_rooms_json`)

**Controller:**
- `POST /mealplans/{tripId}/slots/{slotId}/kitchen-duty` — Küchendienst zuweisen

**Templates:**
- Küchendienst-Badge/Indikator auf jeder Mahlzeit im Essensplan-Übersicht
- Multi-Select-Formular (HTMX-Partial) für Organisatoren

**Dateien:**
- `travelmate-trips/.../domain/mealplan/MealSlot.java`
- `travelmate-trips/.../domain/mealplan/MealPlan.java`
- `travelmate-trips/.../application/MealPlanService.java`
- `travelmate-trips/.../application/command/AssignKitchenDutyCommand.java` (neu)
- `travelmate-trips/.../adapters/web/MealPlanController.java`
- `travelmate-trips/.../adapters/persistence/JpaMealPlanRepository.java`
- `travelmate-trips/.../templates/mealplan/overview.html`
- `travelmate-trips/.../db/migration/V25__kitchen_duty.sql` (neu)

---

### S17-D: Backlog-Abgleich
**Priorität**: SHOULD | **Size**: XS

Product Backlog aktualisieren:
- US-TRIPS-014 (Teilnehmer entfernen) als ✅ Done markieren — bereits implementiert
- E-EXP-01 bis E-EXP-05 Status aktualisieren (viel bereits gebaut)
- Iteration-17-Referenzen ergänzen

**Dateien:** `docs/backlog/product-backlog.md`

---

## Implementierungsreihenfolge

| Reihenfolge | Story | Begründung |
|-------------|-------|------------|
| 1 | S17-A (Reise bearbeiten) | Einfachste Story, etabliert Edit-Pattern für Trip-Aggregate |
| 2 | S17-B (Reise löschen) | Baut auf Trip-Lifecycle-Verständnis auf; Cross-SCS-Event ist Key-Deliverable |
| 3 | S17-C (Küchendienst) | Unabhängig von A/B; erweitert MealPlan um neues Konzept |
| 4 | S17-D (Backlog-Abgleich) | Dokumentation, jederzeit parallel möglich |

**Parallelisierung:** S17-C kann parallel zu S17-B starten, sobald S17-A fertig ist.

---

## Test-Strategie

### Domain Tests (TDD — Red-Green-Refactor)
- `Trip.rename()` / `Trip.updateDescription()` mit Status-Guards
- `Trip.rename()` lehnt leeren Namen ab
- Delete-Precondition: nur PLANNING-Status
- Delete blockiert bei Poll im BOOKING-Status
- `MealSlot.assignKitchenDuty()` lehnt SKIP/EATING_OUT-Slots ab
- `MealSlot.assignKitchenDuty()` mit leerer Liste löscht Zuordnung

### Application / Integration Tests
- `TripService.editTrip()` validiert Organizer-Autorisierung
- `TripService.deleteTrip()` kaskadiert zu allen Repositories
- `TripService.deleteTrip()` publiziert `TripDeleted`-Event
- `MealPlanService.assignKitchenDuty()` Roundtrip durch Persistence
- Expense `onTripDeleted` behandelt fehlenden Ledger graceful

### Controller Tests
- Edit-Endpoints: 422 bei ungültigem Status
- Delete-Endpoint: Redirect auf Liste nach Erfolg
- Delete-Endpoint: Ablehnung bei nicht-PLANNING Trips
- Kitchen-Duty-Endpoint: nur Organisator-Zugriff

### BDD Scenarios (Gherkin)
- `edit-trip.feature`: Reise bearbeiten in PLANNING und CONFIRMED
- `delete-trip.feature`: Reise löschen in PLANNING, Ablehnung bei CONFIRMED
- `kitchen-duty.feature`: Küchendienst zuweisen und anzeigen

### E2E Tests (Playwright)
1. Reise erstellen → bearbeiten → Änderungen auf Detail-Seite verifizieren
2. Reise in PLANNING erstellen → löschen → Reise nicht mehr in Liste
3. Essensplan mit PLANNED-Slot → Küchendienst zuweisen → Badge sichtbar
4. CONFIRMED-Reise löschen versuchen → Ablehnung verifizieren

---

## Verifikation

- Unit/Integration Tests: `./mvnw clean verify` (alle Module)
- E2E + BDD: `./mvnw -Pe2e verify` (gegen laufende Docker-Infrastruktur)
- Manuell: Create → Edit → Delete Trip-Flow durchspielen
- Manuell: Küchendienst-Zuordnung im Essensplan
- Cross-SCS: TripDeleted-Event-Fluss via RabbitMQ Management UI verifizieren
- Mobil: Kitchen-Duty-UI bei 375px testen

---

## Risiken & Mitigationen

| Risiko | Mitigation |
|--------|-----------|
| Kaskaden-Löschung vergisst ein Aggregat → verwaiste Daten | Systematische Prüfung aller tripId-FK vor Implementation; E2E-Test prüft DB direkt |
| TripDeleted bei fehlendem Ledger in Expense | Idempotente Consumer-Logik: `findByTripId().ifPresent(delete)` |
| Löschung während Poll im BOOKING-Status | Expliziter Guard: Löschung blockieren, wenn Poll nicht OPEN/CLOSED/BOOKED |
| JSON-Speicherung für Kitchen-Duty-IDs | Folgt bewährtem Pattern aus V21 (candidate_rooms_json) |

---

## Follow-up (Out of Scope)

- Küchendienst-Fairness-Analytik (wer hat wie oft gekocht) — Feature für spätere Iteration
- Multi-Organizer Role Management (E-IAM-05) — braucht erst ADR zur IAM/Trip-Organizer-Abgrenzung
- GlobalExceptionHandler-Deduplizierung — Quality-Iteration
