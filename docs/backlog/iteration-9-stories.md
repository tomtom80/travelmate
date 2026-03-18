# Iteration 9 — Refined User Stories: Accommodation, Party Settlement, Expense Polish + PWA

**Date**: 2026-03-18 (revised with domain knowledge from user)
**Target Version**: v0.9.0
**Bounded Contexts**: Trips (Accommodation with Room Inventory + Party Assignment), Expense (Party-Level Settlement, Advance Payments, Re-Submit), Infrastructure (PWA Manifest)

---

## Overview

This revision replaces the earlier Iteration 9 scope after the team gained deeper domain knowledge
about how group trips actually work. Two critical insights drove the rewrite:

**Insight 1 — Accommodation is not metadata, it is the planning hub.**
For cabin and house group trips, accommodation is the first and most important planning decision. The
organizer must not only capture where the group is staying but also define a room inventory (room
types, bed counts, price) and assign each travel party (Reisepartei/family) to their rooms. Room
assignment drives cost distribution and is the primary input for advance payments.

**Insight 2 — Settlement is per TravelParty, not per individual.**
All costs — accommodation, groceries, lift tickets, bakery runs — flow into one shared pool. Each
TravelParty's share is proportional to its total weight (adults and children with age-based
weighting). When a participant submits a receipt, it is credited to their TravelParty, not to them
personally. At settlement, the balance per TravelParty is what matters for the "who pays whom"
statement. The current Expense SCS computes per-participant balances; a Party-Level Settlement View
is required to surface the correct family-level picture.

The Accommodation epic (previously one M story) is now two stories: room inventory (L) and party
assignment (M). The advance payment story is refocused around accommodation total as the primary
input. A new Party-Level Settlement View story makes the settlement output match the user's mental
model. Recipe Import and PWA Manifest are unchanged. Re-submit Rejected Receipt moves to a quick
win at the end.

---

## Dependency Graph

```
S9-A: Create Accommodation with Room Inventory
  — new domain model in Trips; foundation for all downstream accommodation work
  — publishes new domain event: AccommodationPriceSet (carries totalPrice)
  └─► S9-B: Assign Travel Parties to Rooms
        — requires Room inventory from S9-A to exist
        — room assignment completes the planning picture visible to all participants

S9-C: Party-Level Settlement View
  — requires TripProjection to carry partyTenantId per participant (new field)
  — can be developed in parallel with S9-A/S9-B once TripProjection change is clear
  — no new events needed: aggregation is a view concern in Expense SCS

S9-D: Advance Payment Tracking (Equal Round Amount per TravelParty)
  — requires S9-C Party-Level Settlement to exist (advance credits flow into party balance)
  — the auto-suggest formula depends on S9-A totalPrice being available
  — single equal amount for ALL parties: ceil(accommodationPrice / partyCount / 50) × 50
  — Organizer confirms or adjusts, then same amount recorded for every party

S9-E: Re-Submit Rejected Receipt
  — independent; pure status transition in Expense aggregate
  — no schema change required; fastest story in the iteration

S9-F: PWA Manifest & Install Prompt
  — fully independent; Gateway static asset + meta tag
  — no SCS changes required; can be merged at any point

--- NOT this iteration ---

US-TRIPS-041 (Recipe Import from URL)
  — deferred to Iteration 10; the accommodation work now fills the L slot
  — RecipeImportAdapter design preserved in agent memory for Iteration 10

US-TRIPS-061 (URL Import for Accommodation)
  — depends on S9-A existing; deferred to Iteration 10

US-TRIPS-062 (Accommodation Poll)
  — new aggregate (LocationPoll); Could priority; deferred

US-EXP-022 (Custom Splitting per Receipt)
  — Could priority; deferred

US-EXP-032 (Settlement Breakdown per Category)
  — Could priority; deferred

US-EXP-033 (Export Settlement as PDF)
  — Could priority; deferred

US-INFRA-042 (Lighthouse CI)
  — Should priority; deferred to CI/CD iteration
```

---

## Recommended Iteration 9 Scope

| ID | Story | Priority | Size | Bounded Context |
|----|-------|----------|------|-----------------|
| S9-A | Create Accommodation with Room Inventory | Must | L | Trips |
| S9-B | Assign Travel Parties to Rooms | Must | M | Trips |
| S9-C | Party-Level Settlement View | Must | M | Expense |
| S9-D | Advance Payment (Equal Round Amount per TravelParty) | Should | M | Expense |
| S9-E | Re-Submit Rejected Receipt | Should | S | Expense |
| S9-F | PWA Manifest & Install Prompt | Should | S | Infrastructure |

**Scope rationale:**
- S9-A and S9-B together close the core accommodation planning gap. Without room assignment,
  accommodation is just a bookmark — it needs to produce the "who sleeps where" list.
- S9-C is the foundational fix for the settlement model mismatch. Without it, the settlement UI
  shows balances per person, which does not match how families think about settling up.
- S9-D depends conceptually on S9-C; the advance payment is a flat credit per party in the
  settlement. It also benefits from S9-A providing the accommodation total as input for the
  auto-suggest formula (ceil(price / partyCount / 50) × 50). The key simplification: all
  parties pay the SAME round amount — no per-party weighting for advances.
- S9-E and S9-F are quick wins that close open gaps from previous iterations.
- Recipe Import (formerly S9-A) is deferred because the accommodation stories are higher priority
  for real-world completeness and would exceed the iteration budget if included.

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S9-F | PWA Manifest — zero dependencies, immediate merge |
| 2 | S9-E | Re-Submit Receipt — no schema change, closes Iteration 6 gap |
| 3 | S9-A | Accommodation + Room Inventory — foundation; publishes AccommodationPriceSet |
| 4 | S9-B | Assign Travel Parties to Rooms — requires S9-A room inventory |
| 5 | S9-C | Party-Level Settlement View — can start after TripProjection partyId design is agreed |
| 6 | S9-D | Advance Payments (equal round amount) — builds on S9-C party model; uses S9-A totalPrice for auto-suggest |

---

## New Domain Model (Iteration 9)

### Accommodation as a First-Class Entity in Trips

The previous design embedded `Accommodation` as a simple Value Object on `Trip`. Given the new
understanding of room inventory and party assignment, `Accommodation` must carry a list of rooms
(Room entities) and a list of assignments (RoomAssignment entities). This is closer to an embedded
aggregate root than a plain VO.

Design decision: `Accommodation` is modelled as an **entity within the Trip aggregate** (not a
separate aggregate root). Trip owns it. Room and RoomAssignment are entities within that structure.
The Trip aggregate root is responsible for all accommodation invariants.

```
Trip (existing)
  └── Accommodation (entity, nullable — trip may not have accommodation yet)
        accommodationId   (UUID)
        name              (String, 1–200 chars)
        address           (String, optional, max 500 chars)
        url               (String, optional, valid HTTP/HTTPS, max 2000 chars)
        checkIn           (LocalDate, optional)
        checkOut          (LocalDate, optional)
        totalPrice        (BigDecimal, optional, >= 0)
        notes             (String, optional, max 2000 chars)
        List<Room>        (1..N when accommodation exists)

        Invariant: checkIn before checkOut when both present
        Invariant: totalPrice = sum of (room.pricePerNight × nights) when rooms have prices,
                   OR entered manually when rooms have no prices (both paths valid)

  Room (entity within Accommodation)
        roomId            (UUID)
        name              (String, 1–100 chars, e.g. "Zimmer 1 — Vierbettzimmer")
        roomType          (enum: SINGLE, DOUBLE, QUAD, DORMITORY, APARTMENT)
        bedCount          (int, >= 1)
        pricePerNight     (BigDecimal, optional, >= 0)
        maxOccupants      (int, derived from bedCount or set explicitly)

  RoomAssignment (entity within Accommodation)
        assignmentId      (UUID)
        roomId            (UUID, FK to Room)
        partyTenantId     (TenantId — the TravelParty being assigned)
        partyName         (String, denormalized for display)
        personCount       (int — number of persons from this party in this room)
        assignedAt        (Instant)
```

### Expense SCS — TripProjection Extension

The TripProjection in the Expense SCS currently maps `participantId → tenantId`. To support
party-level settlement grouping, it must also carry the `partyTenantId` (i.e., the participant's
home TravelParty). Since `ParticipantJoinedTrip` already carries the participant's tenantId
(their TravelParty identifier), this is an additive change.

```
TripProjection (Expense SCS, existing)
  tripId          (UUID)
  tenantId        (TenantId)
  participants    (List<ProjectedParticipant>)

ProjectedParticipant (existing)
  participantId   (UUID)
  participantName (String)
  + partyTenantId (TenantId) ← NEW: the TravelParty this participant belongs to
  + partyName     (String)   ← NEW: denormalized for display
```

The `ParticipantJoinedTrip` event already carries `tenantId` (the participant's Travel Party).
The TripProjection consumer in Expense must now persist this as `partyTenantId`.

### Expense SCS — AdvancePayment (Equal Round Amount per TravelParty)

```
AdvancePayment (entity within Expense aggregate)
  advancePaymentId  (UUID)
  description       (String, 1–200 chars)
  amount            (BigDecimal, > 0, max 2 decimal places)
  paidByPartyId     (TenantId — the TravelParty that paid the advance, NOT a participant UUID)
  paidByPartyName   (String, denormalized for display)
  paidAt            (LocalDate)
  paid              (boolean — has the party actually transferred the money?)
  createdAt         (Instant)

AdvancePaymentSuggestion (Domain Service)
  suggest(accommodationCost, partyCount) → AdvanceSuggestion
  Formula: ceil(accommodationCost / partyCount / 50) × 50

AdvanceSuggestion (Value Object)
  suggestedAmount   (BigDecimal — the same round amount for every party)
  accommodationCost (BigDecimal)
  partyCount        (int)
```

KEY DESIGN CHANGE: Advance payments are a SINGLE EQUAL ROUND AMOUNT per party.
- The system auto-suggests based on accommodation price / party count, rounded up to nearest €50.
- The Organizer confirms or adjusts the suggested amount.
- The confirmed amount is recorded identically for ALL parties (no per-party weighting).
- At settlement, each party receives the same flat credit.
- `paidBy` is a `TenantId` (TravelParty), not a participant UUID — aligns with settlement unit.

---

## Story S9-A: US-TRIPS-060 — Create Accommodation with Room Inventory

**Epic**: E-TRIPS-07
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** to add accommodation details including a room inventory (room names,
types, bed counts, price per night) to a Trip, **so that** all Travel Parties know where they are
staying and the total accommodation cost is established as the basis for advance payment calculation.

### Acceptance Criteria

#### Happy Path — Create Accommodation

- **Given** I am an Organizer viewing a Trip detail page in PLANNING or CONFIRMED status,
  **When** I click "Unterkunft hinzufügen" and fill in at minimum a name and one room,
  **Then** the accommodation is saved and visible to all Trip Participants on the Trip detail page.

- **Given** I fill in the accommodation form with name, address, booking URL, check-in, check-out,
  notes, and at least one room,
  **When** I save,
  **Then** the Trip detail page shows an "Unterkunft" section with all fields and a room list.

- **Given** I enter a booking URL,
  **When** the accommodation is saved,
  **Then** the URL is shown as a clickable "Buchungslink" link that opens in a new tab.

#### Room Inventory

- **Given** I am creating or editing accommodation,
  **When** I click "Zimmer hinzufügen",
  **Then** I can enter a room name, select a room type (Einzelzimmer, Doppelzimmer, Vierbettzimmer,
  Matratzenlager, Apartment), enter the bed count, and optionally enter a price per night.

- **Given** I add multiple rooms, each with a price per night,
  **When** I save,
  **Then** the UI shows the total accommodation price as:
  `totalPrice = sum(room.pricePerNight × nights)` where `nights = checkOut − checkIn`.
  If any room has no price, that room contributes 0 to the sum.

- **Given** I prefer to enter a manual total price instead of per-room pricing,
  **When** I enter a total price directly in the "Gesamtpreis" field and leave per-room prices
  blank,
  **Then** the manual total price is saved as `totalPrice`. Both paths are valid.

  > Design note: the two paths (room-derived total vs. manual total) are UI concerns. The domain
  > stores `totalPrice` as an explicit field. If the organizer uses per-room pricing, the UI
  > computes the sum and writes it to `totalPrice`. If the organizer enters a manual total, it
  > overwrites any room-derived calculation. This keeps the domain model simple.

- **Given** I have at least one room defined,
  **When** I try to delete the last room,
  **Then** I see the error "Eine Unterkunft muss mindestens ein Zimmer haben."

#### Dates and Price

- **Given** I enter check-in and check-out dates,
  **When** I submit,
  **Then** check-in must be strictly before check-out (validated, hard error).

- **Given** I enter check-in/check-out dates that fall outside the Trip's date range,
  **When** I submit,
  **Then** I see the warning "Die eingegebenen Daten liegen außerhalb des Reisezeitraums
  ({startDate}–{endDate}). Trotzdem speichern?" with a confirm/cancel choice. This is a warning,
  not a hard error — organizers sometimes arrive one day early or leave one day late.

- **Given** I enter a total price,
  **When** the accommodation is saved,
  **Then** a domain event `AccommodationPriceSet(tripId, tenantId, totalPrice)` is published.
  The Expense SCS will consume this event to show the accommodation cost in the expense ledger
  as context for advance payment calculation (read-only; it does not auto-create an expense entry).

  > Future: in Iteration 10, this event may trigger an advance payment suggestion. For now, it
  > is published so the Expense SCS can display the accommodation total alongside the ledger.

#### Update and Clear

- **Given** a Trip already has accommodation,
  **When** the Organizer clicks "Unterkunft bearbeiten" and changes any field or room,
  **Then** the accommodation is updated. If the total price changes, `AccommodationPriceSet` is
  re-published with the new value.

- **Given** the Organizer clicks "Unterkunft entfernen" and confirms,
  **Then** the entire accommodation (including all rooms and assignments) is removed. The Trip
  detail page shows no accommodation section.

#### Participant View

- **Given** a Trip has accommodation with rooms,
  **When** a Participant (not Organizer) views the Trip detail page,
  **Then** they see all accommodation fields and the room list in read-only mode. The room
  assignment section (see S9-B) is also shown in read-only mode.

- **Given** a Trip has no accommodation,
  **When** a Participant views the Trip detail page,
  **Then** no accommodation section is shown. Only the Organizer sees the "Unterkunft hinzufügen"
  button.

#### Trip Status Constraints

- **Given** a Trip is PLANNING, CONFIRMED, or IN_PROGRESS,
  **When** the Organizer sets or edits accommodation,
  **Then** the operation is allowed.

- **Given** a Trip is COMPLETED or CANCELLED,
  **When** anyone views the Trip,
  **Then** any previously saved accommodation details are shown in read-only mode for all.
  No changes are permitted.

#### Validation

- **Given** I submit with the name field blank,
  **Then** I see "Name der Unterkunft ist erforderlich."

- **Given** I submit a name longer than 200 characters,
  **Then** I see "Name darf maximal 200 Zeichen haben."

- **Given** I submit a URL that is not a valid HTTP/HTTPS URL,
  **Then** I see "Bitte gib eine gültige URL ein."

- **Given** I enter a bed count of 0 or negative for a room,
  **Then** I see "Anzahl Betten muss mindestens 1 sein."

- **Given** I enter a price per night that is negative,
  **Then** I see "Preis pro Nacht muss 0 oder positiv sein."

- **Given** I submit accommodation with zero rooms,
  **Then** I see "Bitte füge mindestens ein Zimmer hinzu."

#### Authorization

- **Given** I have the `participant` role (not `organizer`),
  **When** I attempt to POST to the accommodation endpoint,
  **Then** I receive HTTP 403 Forbidden.

- **Given** I have the `organizer` role but belong to a different Tenant,
  **When** I attempt to modify accommodation for a Trip I don't own,
  **Then** I receive HTTP 403 Forbidden (TenantId scoping enforced at repository).

#### Multi-Tenancy

- Two Tenants each with accommodation details — only their own data is visible. Cross-tenant
  access is a security violation enforced at the repository layer.

### Technical Notes

- Bounded Context: Trips
- Aggregate: Trip — new embedded `Accommodation` entity with `List<Room>` (entity list)
- `Accommodation` is nullable on Trip (not all trips have accommodation)
- `Room` is an entity within `Accommodation`: `roomId`, `name`, `roomType (enum)`, `bedCount`,
  `pricePerNight (nullable)`, `maxOccupants`
- Commands: `SetAccommodationCommand(tripId, tenantId, name, address, url, checkIn, checkOut,
  totalPrice, notes, List<RoomSpec>)`, `ClearAccommodationCommand(tripId, tenantId)`
- `RoomSpec` is a Record used in the command: `(String name, RoomType type, int bedCount,
  BigDecimal pricePerNight)`
- Application service: `TripService.setAccommodation(...)`, `TripService.clearAccommodation(...)`
- New domain event: `AccommodationPriceSet` in `travelmate-common/events/trips/`
  Fields: `tripId (UUID)`, `tenantId (TenantId)`, `totalPrice (BigDecimal)`
  Published only when `totalPrice` is non-null and non-zero.
- Flyway migration: V11 in Trips — tables `accommodation (id, trip_id, name, address, url,
  check_in, check_out, total_price, notes)` and `accommodation_room (id, accommodation_id,
  name, room_type, bed_count, price_per_night, max_occupants, sort_order)` with FK cascade
- No changes to existing Trip table columns; accommodation moves to a child table
- UI: "Unterkunft" tab or card section on `trip/detail.html`; HTMX form with dynamic room rows
  (hx-post for add room row, hx-delete for remove room row before overall save)
- `RoutingKeys`: add `ACCOMMODATION_PRICE_SET = "trips.accommodation.price_set"`
- BDD scenario: "Als Organisator möchte ich eine Unterkunft mit Zimmern zur Reise hinzufügen"

---

## Story S9-B: US-TRIPS-063 — Assign Travel Parties to Rooms

**Epic**: E-TRIPS-07
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to assign each Travel Party (family) to a room in the accommodation,
**so that** everyone knows their sleeping arrangements and I can verify that all participants are
accommodated.

### Acceptance Criteria

#### Happy Path — Room Assignment

- **Given** an accommodation with at least one room exists on a Trip,
  **When** the Organizer views the accommodation section,
  **Then** they see a room assignment panel showing each room with its capacity (bed count) and
  a list of Travel Parties available to assign.

- **Given** I select a Travel Party from the dropdown for a room,
  **When** I enter the number of persons from that party in this room and click "Zuweisen",
  **Then** the assignment is saved and displayed: "Familie Schmidt — 2 Personen → Zimmer 1".

- **Given** a Travel Party has 4 members (2 adults, 2 children) and is assigned to a room with
  4 beds,
  **When** the assignment is saved,
  **Then** no capacity warning is shown (persons = bed count).

- **Given** I assign a party with a person count that exceeds the room's bed count,
  **When** I save,
  **Then** I see the warning "Diese Belegung überschreitet die Bettenanzahl des Zimmers
  ({bedCount} Betten). Trotzdem speichern?" This is a warning, not a hard error — a family
  may bring a travel cot.

#### Multiple Assignments per Room (Shared Rooms)

- **Given** a dormitory room with 10 beds,
  **When** the Organizer assigns multiple Travel Parties to the same room,
  **Then** each assignment is saved separately. The room shows: "Familie A — 3 Pers., Familie B
  — 4 Pers." The total assigned persons per room is shown vs. the bed count.

#### Edit and Remove Assignments

- **Given** a room assignment exists,
  **When** the Organizer clicks "Zuweisung entfernen" for a specific assignment,
  **Then** the assignment is removed. The room shows as unoccupied (or partially occupied if
  other assignments remain).

- **Given** I update the person count for an existing assignment and save,
  **Then** the updated count is shown.

#### Participant View

- **Given** the Organizer has assigned Travel Parties to rooms,
  **When** a Participant views the Trip detail page,
  **Then** they see the room assignment list in read-only mode: which family is in which room.
  This is visible to all Trip Participants.

- **Given** no assignments have been made yet,
  **When** a Participant views the Trip detail page,
  **Then** the room list shows rooms without assignments. The Organizer sees "Noch nicht belegt"
  per room; Participants see rooms without assignment information.

#### Coverage Check

- **Given** the Organizer views the accommodation section,
  **When** there are Trip Participants whose TravelParty has no room assignment,
  **Then** a banner "Nicht alle Reiseparteien haben eine Zimmeraufteilung" is shown to the
  Organizer. This is informational only — it is not an error.

#### Authorization

- **Given** I have the `participant` role (not `organizer`),
  **When** I attempt to POST a room assignment,
  **Then** I receive HTTP 403 Forbidden.

- **Given** the Trip is COMPLETED or CANCELLED,
  **When** the Organizer tries to modify room assignments,
  **Then** the operation is rejected. Read-only mode applies.

#### Multi-Tenancy

- Room assignments are scoped to TenantId via the Trip aggregate. No cross-tenant access.

### Technical Notes

- Bounded Context: Trips
- New entity: `RoomAssignment` within `Accommodation`
  Fields: `assignmentId (UUID)`, `roomId (UUID)`, `partyTenantId (TenantId)`, `partyName (String)`,
  `personCount (int)`, `assignedAt (Instant)`
- The `partyName` is denormalized from the TravelParty projection at assignment time
- Commands: `AssignPartyToRoomCommand(tripId, tenantId, roomId, partyTenantId, partyName,
  personCount)`, `RemoveRoomAssignmentCommand(tripId, tenantId, assignmentId)`
- Application service: `TripService.assignPartyToRoom(...)`, `TripService.removeRoomAssignment(...)`
- Flyway migration: V12 in Trips — table `room_assignment (id, accommodation_id, room_id,
  party_tenant_id, party_name, person_count, assigned_at)` with FK cascade on accommodation
- UI: Room assignment panel on the accommodation section of `trip/detail.html`
  Each room row has a party selector (dropdown of TravelParties on this Trip) + person count field
  HTMX hx-post for each assignment, hx-delete for removal
- TravelParties available for assignment: derived from the Trip's Participants list, grouped by
  their `partyTenantId`
- Domain Events: none (room assignment is a Trip-internal concern)
- BDD scenario: "Als Organisator möchte ich Reiseparteien den Zimmern zuweisen"

---

## Story S9-C: US-EXP-050 — Party-Level Settlement View

**Epic**: E-EXP-04
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to see the settlement grouped by TravelParty (Reisepartei/family),
**so that** the "who pays whom" statement is expressed at the family level, matching how group trips
actually settle up.

### Background

The current settlement shows per-participant balances. In group travel, families do not settle
individually — the Smith family settles with the Miller family as a unit. A family of 4 has one
check or transfer, not four. This story introduces the Party-Level Settlement as the primary view,
while the per-participant breakdown remains available as a detail view.

### Acceptance Criteria

#### Party-Level Settlement Summary

- **Given** the Trip is COMPLETED and a settlement has been calculated,
  **When** the Organizer views the Settlement page,
  **Then** the primary view shows one row per TravelParty:
  ```
  Familie Schmidt   — share: 480 € — credits: 350 € — balance: +130 € (owes)
  Familie Müller    — share: 320 € — credits: 510 € — balance: -190 € (gets back)
  Familie Weber     — share: 200 € — credits: 140 € — balance:  +60 € (owes)
  ```
  "Credits" = sum of all receipts submitted by any member of the party + advance payments
  credited to the party. "Balance" = share − credits. Positive = owes money; Negative = gets
  money back.

- **Given** the party-level summary is shown,
  **When** the Organizer expands a party row,
  **Then** a detail panel shows the individual contributions: which member submitted which
  receipts and which advance payments were recorded for the party.

#### Transfer Statements

- **Given** the settlement is calculated,
  **When** the Organizer views the "Überweisungen" (transfer) section,
  **Then** the transfer statements are at the party level:
  "Familie Schmidt überweist 130 € an Familie Müller."
  Not: "Max Schmidt überweist 65 € and Lena Schmidt überweist 65 €."

- **Given** all balances net to zero (within rounding tolerance of ±0.01 €),
  **When** the settlement is displayed,
  **Then** the settlement is valid. Total credits across all parties equal total pool.

#### Pool Total Includes Accommodation

- **Given** an `AccommodationPriceSet` event has been consumed for this Trip,
  **When** the settlement is displayed,
  **Then** the pool total shown is: `receipts total + accommodation total`. The accommodation
  amount is shown as a separate line item in the pool breakdown:
  ```
  Kostenpool:
    Belege (eingereicht):    620 €
    Unterkunft (pauschal):   380 €
    ----------------------------
    Gesamtpool:            1.000 €
  ```

  > Design note: the accommodation amount in the Expense SCS comes from the
  > `AccommodationPriceSet` event stored in a new read model field on `TripProjection`
  > (`accommodationTotal`). It does NOT create a Receipt — it is a pool input alongside receipts.

- **Given** no `AccommodationPriceSet` event has been received (trip has no accommodation or
  no total price was set),
  **When** the settlement is displayed,
  **Then** the pool total = receipts total only. No accommodation line is shown.

#### TripProjection Changes (prerequisite)

- **Given** a `ParticipantJoinedTrip` event is consumed by the Expense SCS,
  **When** the TripProjection is updated,
  **Then** each `ProjectedParticipant` now stores `partyTenantId` and `partyName` (populated
  from the event's `tenantId` and a configurable display name).

  > The `ParticipantJoinedTrip` event already carries `tenantId`. The TravelParty display name
  > is not in the event — it must come from the TripCreated event (organizer's party) or be
  > derived from the `tenantId`. Pragmatic approach: use the `tenantId` value as the party key
  > for grouping; display name is built from the first registered participant of each party.

#### Weighting Unchanged

- The weighting model (per-participant weight, e.g. adult = 1.0, child = 0.5, infant = 0.0)
  is unchanged. The party share is the sum of individual member weights divided by the total
  weight of all participants across all parties, times the pool total.

  `partyShare = (sum of weights of party members / total weight) × pool total`

#### Multi-Tenancy

- The settlement is scoped to `tenantId` from the TripProjection. A Trip can span participants
  from multiple TravelParties (that is the point of group trips). All these participants'
  expenses are in the same Expense aggregate, scoped by the organizer's `tenantId` (the trip
  owner). Cross-tenant data access is still enforced at the repository layer.

  > Clarification: The `tenantId` on the Expense aggregate is the trip organizer's party. All
  > participants from other parties join via the Trip. Their receipts are in the same Expense
  > ledger. The `partyTenantId` on each `ProjectedParticipant` identifies which family they
  > belong to for the settlement grouping.

### Technical Notes

- Bounded Context: Expense
- New read model field: `TripProjection.accommodationTotal (BigDecimal nullable)`
  Populated when `AccommodationPriceSet` event is consumed.
- New event consumer: `AccommodationPriceSetConsumer` in `adapters/messaging/`
  Routing key: `trips.accommodation.price_set`
- `ProjectedParticipant` extension: add `partyTenantId (TenantId)` and `partyName (String)`
  Populated from `ParticipantJoinedTrip.tenantId` and a derived name (first member's lastName or
  the party's registered name if available from TenantCreated — pragmatic solution: store
  tenantId as UUID string, display as "Party {last 6 chars of UUID}" until a real name arrives)
- Settlement service: new `PartySettlementPlan` record grouping existing `SettlementPlan` data
  Fields: `partyTenantId`, `partyName`, `partyShare`, `partyCredits`, `partyBalance`,
  `List<ParticipantBalance>` (individual detail)
- `SettlementCalculator` extended: after computing individual balances, aggregate by
  `partyTenantId` to produce `List<PartySettlementPlan>`. Pool total = receipts + accommodationTotal.
- Transfer statements: computed from party balances using the same debt-simplification algorithm
  (net pairs), but operating on `PartySettlementPlan` objects.
- UI: `settlement/detail.html` — primary section shows party balances; collapsible detail per
  party shows individual member contributions. Pool breakdown shows accommodation line if present.
- Flyway migration: V5 in Expense — add `accommodation_total` column to `trip_projection`; add
  `party_tenant_id`, `party_name` columns to `trip_projection_participant` (or equivalent join
  table). Nullable, populated asynchronously from events.
- New `RoutingKeys` constant: `ACCOMMODATION_PRICE_SET` (consumed by Expense)
- BDD scenario: "Als Organisator möchte ich die Abrechnung auf Reisepartei-Ebene sehen"

---

## Story S9-D: US-EXP-013 — Advance Payment Tracking (Equal Round Amount per TravelParty)

**Epic**: E-EXP-02
**Priority**: Should
**Size**: M
**As an** Organizer, **I want** to set a single equal advance payment amount for all TravelParties
— auto-suggested from the accommodation cost — **so that** each family pays the same round amount
before the trip and that amount is credited in the final settlement.

### Background

Before the trip, the Organizer collects a financial advance from each participating Travel Party.
The advance is a **single equal round amount** for ALL parties — not weighted per person or family
size. The system auto-suggests an amount based on: `ceil(accommodationPrice / partyCount / 50) × 50`.
The Organizer confirms or adjusts this amount (e.g., adds a buffer for groceries). Once confirmed,
the same amount is recorded for every party. At settlement, each party's advance is a flat credit.

### Acceptance Criteria

#### Happy Path — Auto-Suggest and Confirm

- **Given** the accommodation price is set and parties are known,
  **When** the Organizer opens the advance payment form,
  **Then** the system suggests: `ceil(accommodationPrice / partyCount / 50) × 50`.
  Example: accommodation = 1400 EUR, 3 parties → ceil(1400/3/50)×50 = ceil(9.33)×50 = 500 EUR.

- **Given** the suggested amount is shown,
  **When** the Organizer adjusts it (e.g., increases to 550 EUR to add a buffer for groceries),
  **Then** the custom amount is accepted. The input is a single editable field.

- **Given** the Organizer confirms the amount,
  **Then** the same amount is recorded as advance for EACH travel party.
  All parties see identical advance amounts (e.g., 3 × 550 EUR = 1650 EUR total).

- **Given** advances are recorded,
  **When** the settlement is calculated (S9-C),
  **Then** each party's advance is credited as a flat amount:
  `partyBalance = partyShare − partyReceiptCredits − advanceAmount`

#### Display — Advance Payment Overview

- **Given** advance payments have been confirmed,
  **When** I view the advance payment section,
  **Then** I see a table with: each party name, the advance amount (same for all), and a
  "Bezahlt" toggle (checkbox) per party to track who has actually transferred the money.

- **Given** the Organizer toggles a party's "Bezahlt" status,
  **Then** the status is updated. A progress bar shows total received vs. total expected.

#### No Accommodation Price Set

- **Given** no accommodation price is set,
  **When** I open the advance payment section,
  **Then** I see a prompt: "Trage einen Gesamtpreis bei der Unterkunft ein, um Vorauszahlungen
  zu berechnen." with a link to the accommodation edit form.

#### Re-Confirm with Different Amount

- **Given** advance payments already exist,
  **When** the Organizer changes the amount and re-confirms,
  **Then** all existing advance payments are replaced with the new amount.
  The "Bezahlt" status is reset (since the amount changed).

#### Validation

- **Given** I submit an amount of 0 or negative,
  **Then** I see "Betrag muss größer als 0 sein."

- **Given** I submit an amount with more than 2 decimal places,
  **Then** I see "Betrag darf maximal 2 Nachkommastellen haben."

#### Authorization

- **Given** I have the `participant` role,
  **When** I attempt to POST an advance payment confirmation,
  **Then** I receive HTTP 403 Forbidden. Only Organizers set advance payments.

#### Trip Status Constraints

- **Given** a Trip is COMPLETED,
  **When** the Organizer tries to set or change advance payments,
  **Then** the operation is rejected. I see "Abgeschlossene Reisen können nicht mehr geändert
  werden."

#### Multi-Tenancy

- The Expense aggregate is scoped by the organizer's TenantId. Advance payments are entities
  within that aggregate. TenantId scoping is enforced at the repository layer.

### Technical Notes

- Bounded Context: Expense
- New entity `AdvancePayment` within the `Expense` aggregate
  Fields: `advancePaymentId (UUID)`, `description (String)`, `amount (BigDecimal)`,
  `paidByPartyId (TenantId)`, `paidByPartyName (String, denormalized)`,
  `paidAt (LocalDate)`, `paid (boolean)`, `createdAt (Instant)`
  KEY CHANGE: `amount` is the SAME for all parties. No per-party weighting.
- New Domain Service: `AdvancePaymentSuggestion`
  Method: `suggest(accommodationCost, partyCount) → AdvanceSuggestion`
  Formula: `ceil(accommodationCost / partyCount / 50) × 50`
- Commands: `ConfirmAdvancePaymentsCommand(expenseId, tenantId, amount, paidAt)` — creates
  one AdvancePayment per party with the same amount.
  `ToggleAdvancePaymentPaidCommand(expenseId, tenantId, advancePaymentId)` — toggles paid status.
  No per-party amount editing — the amount is always uniform.
- `SettlementCalculator` update: each party's advance is a flat credit (same amount) added to
  the party's `partyCredits` in `PartySettlementPlan` (from S9-C)
- Flyway migration: V6 in Expense — table `advance_payment (id, expense_id, description,
  amount, paid_by_party_id, paid_by_party_name, paid_at, paid BOOLEAN DEFAULT FALSE,
  created_at)` FK on `expense`
- UI: "Vorauszahlungen" section on the Accommodation page (Trips SCS) or Expense detail page;
  shows auto-suggestion, single amount input, confirm button, then per-party table with
  paid toggles
- Accommodation total for suggestion: from `TripProjection.accommodationTotal` (via
  AccommodationPriceSet event from S9-A)
- Domain Events: none (advance payments are Expense-internal)
- BDD scenario: "Als Organisator möchte ich einen gleichen Vorauszahlungsbetrag für alle
  Reiseparteien festlegen"

---

## Story S9-E: US-EXP-042 — Re-Submit Rejected Receipt

**Epic**: E-EXP-05
**Priority**: Should
**Size**: S
**As a** Participant, **I want** to correct and re-submit a rejected receipt, **so that** it can be
reviewed again and included in the settlement.

### Acceptance Criteria

#### Happy Path

- **Given** my Receipt is in REJECTED status and I am the original submitter,
  **When** I click "Erneut einreichen",
  **Then** a form opens pre-filled with the current receipt data (description, amount, date,
  category) and the rejection reason is shown as context ("Abgelehnt mit Begründung: ...").

- **Given** I edit the receipt (e.g., correct the amount) and click "Einreichen",
  **Then** the Receipt status transitions from REJECTED to SUBMITTED, the `rejectionReason` is
  cleared, and `reviewedBy` / `reviewedAt` are cleared so a new reviewer can be assigned.

- **Given** the re-submitted receipt is in SUBMITTED status,
  **When** an Organizer reviews it,
  **Then** the normal four-eyes review flow applies (US-EXP-041): the reviewer can Approve or
  Reject again.

#### No Forced Edit Required

- **Given** I believe the receipt was incorrectly rejected,
  **When** I re-submit without changing any field,
  **Then** the system allows re-submission unchanged. The intent is to request a second review.

#### Authorization

- **Given** I am the original submitter,
  **When** I re-submit,
  **Then** the operation is allowed.

- **Given** I am a different Participant (not the submitter) and not an Organizer,
  **When** I try to re-submit,
  **Then** I receive HTTP 403 Forbidden.

- **Given** I am an Organizer (but not the original submitter),
  **When** I access the re-submit action,
  **Then** I can re-submit on behalf of the submitter.

#### Status Constraint

- **Given** a Receipt is in SUBMITTED or APPROVED status,
  **When** I try to trigger re-submission,
  **Then** the "Erneut einreichen" button is not rendered. Only REJECTED receipts show it.

- **Given** a Receipt is REJECTED but the Trip is COMPLETED,
  **When** I try to re-submit,
  **Then** the operation is rejected. I see "Die Reise ist abgeschlossen. Belege können nicht
  mehr eingereicht werden."

#### Rejection History

- Out of scope for this iteration: a full audit trail of rejections and re-submissions.
  The rejection reason from the most recent rejection is shown during re-submission, but prior
  rejection cycles are not preserved.

#### Multi-Tenancy

- TenantId scoping at the repository layer prevents cross-tenant access even if a receipt ID
  is guessed.

### Technical Notes

- Bounded Context: Expense
- Status transition: `REJECTED → SUBMITTED` (with optional field updates)
- Command: `ResubmitReceiptCommand(expenseId, tenantId, receiptId, requestingParticipantId,
  description, amount, date, category)` — same shape as the original submit command
- Method on `Expense` aggregate: `resubmitReceipt(receiptId, requestingParticipantId, ...)`
  Invariant: status must be REJECTED; requestingParticipantId must equal `submittedBy` OR
  requesting user has `organizer` role
- `Receipt.rejectionReason` cleared. `Receipt.reviewedBy` cleared. `Receipt.reviewedAt` cleared.
  `Receipt.submittedAt` updated to the re-submit instant.
- No new Flyway migration: `rejection_reason` column already exists from Iteration 6.
- UI: "Erneut einreichen" button visible only on REJECTED receipts in the submitter's list and
  in the Organizer's review queue. HTMX form submission.
- Domain Events: none
- BDD scenario: "Als Teilnehmer möchte ich einen abgelehnten Beleg korrigieren und erneut einreichen"

---

## Story S9-F: US-INFRA-041 — PWA Manifest & Install Prompt

**Epic**: E-INFRA-05
**Priority**: Should
**Size**: S
**As a** user, **I want** to install Travelmate as a Progressive Web App on my phone's home screen,
**so that** it feels like a native app and launches quickly without browser chrome.

### Acceptance Criteria

#### Manifest

- **Given** I visit any page of Travelmate in a mobile browser (Chrome, Safari),
  **When** the page loads,
  **Then** the browser finds a valid `manifest.json` linked in the `<head>` of all pages and
  the manifest meets PWA installability criteria.

- **Given** the manifest is loaded,
  **Then** it contains:
  - `name`: "Travelmate"
  - `short_name`: "Travelmate"
  - `start_url`: `"/"`
  - `display`: `"standalone"`
  - `background_color`: a colour matching PicoCSS primary background
  - `theme_color`: a colour matching PicoCSS primary accent
  - `icons`: at least one 192×192 and one 512×512 PNG icon
  - `lang`: `"de"`

#### Install Prompt (Android / Chrome)

- **Given** the PWA criteria are met (manifest + HTTPS + served correctly),
  **When** a user visits on Android/Chrome for the first time,
  **Then** the browser shows the native "Add to Home Screen" banner or mini-infobar.
  The application registers a `beforeinstallprompt` event handler but builds no custom UI —
  the browser's native prompt is sufficient for this iteration.

#### Installed App Behaviour

- **Given** the user installs the PWA,
  **When** they open it from the home screen,
  **Then** it launches in standalone mode: no browser address bar, Travelmate icon in the
  task switcher.

#### iOS / Safari

- **Given** I visit Travelmate in Safari on iOS,
  **When** I tap "Share" → "Add to Home Screen",
  **Then** the app installs with the correct icon and name. Safari reads `apple-touch-icon`
  meta tags and the manifest.
  iOS `beforeinstallprompt` is not supported — this path is manual.

#### No Offline Requirement

- A Service Worker is NOT required by this story. Offline caching (US-INFRA-040) is deferred.
  This story delivers installability only.

#### Quality Gate

- **Given** the manifest is deployed,
  **When** a Lighthouse PWA audit is run on the Trip list page,
  **Then** the "Installable" criterion passes. (Manual verification; Lighthouse CI is deferred.)

### Technical Notes

- Bounded Context: Infrastructure / Gateway
- Artefact: `manifest.json` at `travelmate-gateway/src/main/resources/static/manifest.json`
- `<link rel="manifest" href="/manifest.json">` added to the shared Thymeleaf layout fragment
  in each SCS
- Icons: 192×192 and 512×512 PNG at `travelmate-gateway/src/main/resources/static/icons/`
- `apple-touch-icon` link added to the shared layout for iOS support
- No Spring Boot changes required; Gateway static resource serving handles it
- E2E test: Playwright asserts `document.querySelector('link[rel="manifest"]')` is present on
  the Trip list page after login

---

## Cross-SCS Event Flow (New in Iteration 9)

```
Trips SCS                          Expense SCS
─────────────────────────────────────────────────────
Trip.setAccommodation(totalPrice)
  └─► registers AccommodationPriceSet event
  └─► repository.save(trip)
  └─► @TransactionalEventListener → RabbitMQ
                                      AccommodationPriceSetConsumer
                                        └─► TripProjection.accommodationTotal = event.totalPrice()
                                        └─► SettlementCalculator uses accommodationTotal in pool

ParticipantJoinedTrip (existing)   TripProjectionConsumer (existing, extended)
  └─► carries tenantId               └─► ProjectedParticipant.partyTenantId = event.tenantId()
                                        └─► ProjectedParticipant.partyName = derived/denormalized
```

**Routing key**: `trips.accommodation.price_set` (new; add to `RoutingKeys.java` in common)
**Exchange**: `travelmate.events` (existing topic exchange)
**Consumer**: `AccommodationPriceSetConsumer` in `travelmate-expense/adapters/messaging/`

---

## Open Design Questions (to resolve during implementation)

### Q1: TravelParty Name in Expense SCS

The Expense SCS needs to display TravelParty names in the settlement view and the advance payment
dropdown. The `ParticipantJoinedTrip` event carries `tenantId` but no party display name.
Options:
- A) Use `tenantId.value()` as party key; derive a display name from the first participant's last
  name at projection time ("Family {lastName}").
- B) Publish `TravelPartyNameResolved` event from Trips (which has the TravelParty projection)
  when a party first joins a trip.
- C) Accept that party names in Expense are "Family of {memberName}" — acceptable for MVP.

**Recommendation for Iteration 9**: option A (derive from first participant). Proper party name
propagation can be a story in Iteration 10 once the pattern is validated.

### Q2: Accommodation Total in Cost Pool — Does it create a Receipt?

Two approaches:
- A) The accommodation total enters the pool as a meta-input (stored in `TripProjection.
  accommodationTotal`), not as a Receipt. The settlement calculator adds it to the pool total
  but it is not associated with any submitter — it is a shared cost.
- B) The Organizer creates a Receipt of category ACCOMMODATION for the total price manually
  (existing flow). The Expense SCS just shows it in the category breakdown.

**Recommendation**: option A for S9-A/S9-C. The accommodation total is a structural cost, not a
participant-submitted receipt. It reduces the "how much did each family bring in receipts"
calculation but has no submitter. Option B can coexist — the Organizer may still submit a receipt
for accommodation if they want it tracked differently (e.g., the deposit they paid).

### Q3: Advance Payment — Equal amount or weighted?

The advance is a SINGLE EQUAL ROUND AMOUNT per party. No weighting. The Organizer sets one amount
that all parties pay. The system auto-suggests: `ceil(accommodationPrice / partyCount / 50) × 50`.
The Organizer confirms or adjusts. This is a logistical tool ("each family transfers €500 before
the trip"), not a proportional split. Weighting only applies at final settlement.

The advance is collected before the trip and recorded by the Organizer. It is displayed in the
Expense ledger as a flat credit for each party. The UI shows it in a dedicated "Vorauszahlungen"
section with a "Bezahlt" toggle per party to track who has actually transferred the money.

---

## Out-of-Scope for Iteration 9

| Item | Reason |
|------|--------|
| US-TRIPS-041: Recipe Import from URL | Deferred to Iteration 10; accommodation work takes the L slot |
| US-TRIPS-061: Import Accommodation from URL | Depends on S9-A; deferred to Iteration 10 |
| US-TRIPS-062: Accommodation Poll | New aggregate (LocationPoll); Could priority; too much scope |
| US-TRIPS-055: Bring App Integration | Could priority; no stable public API; deferred |
| Accommodation amenities (for poll comparison) | Deferred to Iteration 10 alongside US-TRIPS-062 |
| TravelParty display name propagation event | Deferred; Iteration 9 uses derived names (Q1 above) |
| US-EXP-022: Custom Splitting per Receipt | Could priority; significant UI work; deferred |
| US-EXP-032: Settlement Breakdown per Category | Could priority; deferred |
| US-EXP-033: Export Settlement as PDF | Could priority; introduces PDF library; deferred |
| US-INFRA-040: Service Worker for Offline Caching | Could/XL; deferred from Iteration 8 |
| US-INFRA-042: Lighthouse CI Integration | Should; requires GitHub Actions work; deferred |
| Rejection history / audit trail for receipts | Out of scope for S9-E; future story |
| i18n for error messages in new stories | German default; locale-aware messages are a cross-cutting story |

---

## Ubiquitous Language Compliance

| UI (DE) | UI (EN) | Code | Context |
|---------|---------|------|---------|
| Unterkunft | Accommodation | Accommodation | Trips — entity on Trip |
| Unterkunft hinzufügen | Add Accommodation | SetAccommodationCommand | Trips |
| Zimmer | Room | Room | Trips — entity within Accommodation |
| Zimmertyp | Room Type | RoomType | Trips — enum (SINGLE/DOUBLE/QUAD/DORMITORY/APARTMENT) |
| Vierbettzimmer | Quad Room | QUAD | Trips — RoomType |
| Matratzenlager | Dormitory | DORMITORY | Trips — RoomType |
| Zimmerbelegung | Room Assignment | RoomAssignment | Trips — entity within Accommodation |
| Zuweisung | Assignment | RoomAssignment | Trips |
| Buchungslink | Booking URL | Accommodation.url | Trips |
| Gesamtpreis | Total Price | Accommodation.totalPrice | Trips |
| Preis pro Nacht | Price per Night | Room.pricePerNight | Trips |
| Unterkunftskosten gesetzt | Accommodation Price Set | AccommodationPriceSet | Events (new) |
| Abrechnung je Reisepartei | Party-Level Settlement | PartySettlementPlan | Expense |
| Kostenpool | Cost Pool | pool total | Expense — settlement calculation |
| Überweisung | Transfer | transfer statement | Expense — settlement output |
| Anzahlung | Advance Payment | AdvancePayment | Expense |
| Anzahlung bestätigen | Confirm Advance Payment | ConfirmAdvancePaymentsCommand | Expense |
| Vorschlagsbetrag | Suggested Amount | AdvancePaymentSuggestion | Expense |
| Erneut einreichen | Re-Submit | ResubmitReceiptCommand | Expense |
| Abgelehnt | Rejected | REJECTED | Expense — Receipt status |
| Eingereicht | Submitted | SUBMITTED | Expense — Receipt status |
| Als App installieren | Install App | PWA | Infrastructure |
