# Iteration 15 — Delivery Plan: Accommodation Poll Redesign and Booking Recovery

**Date**: 2026-03-31
**Target Version**: v0.15.0
**Input**: [iteration-15-stories.md](./iteration-15-stories.md), [iteration-14-accommodationpoll-ux.md](../design/wireframes/iteration-14-accommodationpoll-ux.md), additional visual references for survey-style KPI/chart layout and listing-style accommodation detail presentation

**Status**: REFINED

---

## Planning Goal

Iteration 15 turns the first functional accommodation poll from Iteration 14 into a resilient
planning workflow that survives real booking failures and is easier to understand on desktop and
mobile.

The plan focuses on four outcomes:

1. amenities move to the correct level of the model so accommodation candidates can be compared
   consistently
2. organiser confirmation no longer implies a successful external booking
3. failed booking attempts re-open the decision flow without losing transparency
4. the UI adopts clearer comparison patterns inspired by dashboard-style poll visualisation and
   listing-style accommodation detail cards

---

## UX Input Consolidation

Two external reference directions are now explicitly in scope for the redesign:

### 1. Survey-style result visualisation

The provided survey dashboard reference contributes two useful patterns:

- compact KPI tiles above the main content to explain poll state at a glance
- horizontal comparison bars with strong colour separation and obvious ranking

For Travelmate, this translates to:

- KPI strip for `Kandidaten`, `Stimmen`, `Fuehrender Kandidat`, `Status`
- horizontal bars as the primary result visualisation
- no radar chart in the primary decision flow; radar is harder to compare precisely and should not
  replace the ranking bars for booking decisions

### 2. Listing-style accommodation presentation

The referenced Airbnb listing informs the detail density of each candidate card:

- address/location context should be visible without opening the external provider
- amenities should be rendered as scannable chips/icons near the top of the card
- rooms/sleeping capacity need their own compact section instead of raw concatenated text
- a map action should sit next to the location data, not as a hidden secondary affordance

Travelmate keeps progressive enhancement boundaries:

- no embedded third-party map iframe
- no dependency on remote scripts
- external listing and map open in a new tab

---

## Architecture Decisions

| ADR | Decision | Rationale |
|-----|----------|-----------|
| ADR-0021 | Trip remains a planning container until collaborative decisions are complete | Accommodation poll still belongs to planning, not to confirmed execution |
| ADR-0022 | Booking workflow stays inside `AccommodationPoll` until booking succeeds | Lets the organiser recover from provider-side failure without creating a premature `Accommodation` aggregate |

Additional implementation decisions for this iteration:

- `Amenity` becomes a structured enum on `AccommodationCandidate`
- `CandidateRoom` keeps only room-specific data (`name`, `bedCount`, optional `bedDescription`, optional `pricePerNight`)
- `CONFIRMED` is replaced in practice by the two-step flow `AWAITING_BOOKING` -> `BOOKED`
- failed booking attempts remain visible as history but are excluded from active voting

---

## Proposed Delivery Cut

### Must

| ID | Story | Why it is must-have |
|----|-------|---------------------|
| S15-A | Amenity model correction | foundational schema and UI cleanup; all other redesign work depends on it |
| S15-B | Booking confirmation after vote | removes the incorrect "selected means booked" behaviour |
| S15-C | Booking failure fallback | makes the new booking step operationally safe |
| S15-D1 | Poll overview redesign with KPI strip and ranked bars | delivers visible usability value and exposes the new booking states clearly |

### Should

| ID | Story | Why it is should-have |
|----|-------|-----------------------|
| S15-D2 | Candidate create/add form redesign | fixes overflow and aligns data entry with the new amenities model |
| S15-D3 | Candidate card enrichment with rooms, amenities, address/map action | applies the listing-style reference and improves comparison quality |

### Could / Defer

| Topic | Reason to defer |
|-------|------------------|
| Candidate thumbnail/hero image import | attractive but blocked by current import model and increases layout complexity |
| Secondary radar-style compare visual | lower decision value than KPI tiles + ranked bars |
| Live auto-refresh of vote counts | useful later; PRG and manual refresh remain acceptable for v0.15.0 |

---

## Delivery Slices

### Slice 1: Candidate Model Correction (S15-A)

Includes:

- `Amenity` enum in `domain/accommodationpoll/`
- candidate-level amenity persistence and representation mapping
- room model cleanup (`bedDescription` replaces overloaded features text)
- Flyway migration for amenities and legacy field removal
- controller/form contract update for candidate create/add flows

User outcome:
- candidates can be compared by consistent accommodation features
- rooms describe sleeping arrangements instead of duplicating property-wide details

Engineering outcome:
- cleaner aggregate boundaries
- stable base for UI chips/icons and filterable rendering later

### Slice 2: Booking Pending State (S15-B)

Includes:

- `AWAITING_BOOKING` and `BOOKED` workflow
- `BookingAttempt` entity and persistence
- organiser-only booking success action
- delayed creation of the `Accommodation` aggregate until booking success

User outcome:
- the group can distinguish "winner selected" from "actually booked"
- the organiser has an explicit handoff from poll decision to provider booking

Engineering outcome:
- workflow matches reality
- no premature downstream accommodation data

### Slice 3: Booking Failure Recovery (S15-C)

Includes:

- booking failure command with optional note
- candidate transition to `BOOKING_FAILED`
- poll reopening with remaining active candidates
- failed-attempt history section in the UI

User outcome:
- the group can continue planning immediately after a provider-side rejection
- failed options remain transparent without polluting the active choice set

Engineering outcome:
- aggregate has a complete retry loop
- organiser actions become auditable

### Slice 4: Poll Overview Redesign (S15-D1)

Includes:

- top-level KPI strip using four compact tiles
- primary results panel with colour-coded horizontal ranking bars
- redesigned candidate cards in `.card-grid`
- explicit booking-state banner for `AWAITING_BOOKING`
- collapsible failed-booking section

User outcome:
- poll status is understandable within seconds on desktop and mobile
- voting results are comparable without reading dense tables

Engineering outcome:
- replaces the current mixed table/string dump with reusable card fragments
- gives the controller/template layer a stable structure for future HTMX partial refreshes

### Slice 5: Candidate Entry and Listing Detail Polish (S15-D2 + S15-D3)

Includes:

- room fieldsets without overflow
- amenity checkbox grid with icon labels
- candidate card subsections for location, amenities, rooms, and actions
- map link based on address when present, otherwise candidate name fallback

User outcome:
- organisers can enter complex accommodation proposals without layout breakage
- participants can inspect location and sleeping capacity before voting

Engineering outcome:
- create and overview templates speak the same visual language
- future import enrichment has clear output slots in the card layout

---

## Recommended Implementation Order

| Order | Story / Slice | Rationale |
|-------|---------------|-----------|
| 1 | S15-A / Slice 1 | model correction first; avoids rework in forms and cards |
| 2 | S15-B / Slice 2 | establishes the new booking boundary |
| 3 | S15-C / Slice 3 | completes the safety loop before UI polish finalises the workflow |
| 4 | S15-D1 / Slice 4 | highest-value UI slice once states and representations are stable |
| 5 | S15-D2 + S15-D3 / Slice 5 | form/detail polish after the main page contract is settled |

Parallelism:

- Slice 4 UX/template work can start once the representation contract for S15-A/B/C is fixed
- Slice 5 form polish can overlap late with Slice 4 if controller parameter changes are already decided

---

## Technical Impact by Story

### S15-A: Amenity Model

Expected changes:

- `trips/domain/accommodationpoll/`:
  `Amenity`, updated `AccommodationCandidate`, updated `CandidateRoom`
- `AccommodationPollService` commands and mapping
- persistence adapter and Flyway migration for candidate amenities
- create/add forms and representations

Main risk:

- legacy `features` removal touches domain, persistence, and form serialisation at once

### S15-B + S15-C: Booking Workflow

Expected changes:

- `AccommodationPollStatus`, `CandidateStatus`, `BookingAttempt`, `BookingAttemptStatus`
- `AccommodationPollService` orchestration split into select / book-success / book-failure
- controller endpoint changes away from the old direct confirm semantics
- delayed `Accommodation` creation policy

Main risk:

- existing selection logic may assume immediate `Accommodation` creation
- status migration from Iteration 14 data must remain backward-safe

### S15-D: UX Redesign

Expected changes:

- `templates/accommodationpoll/overview.html`
- `templates/accommodationpoll/create.html`
- new partial `templates/accommodationpoll/candidate-card.html`
- Trips `style.css` extensions for KPI tiles, candidate cards, amenity grid, status banners, and
  result bars

Main risk:

- controller/template contract for dynamic room inputs must stay simple enough for server-side
  validation and controller tests

---

## Test Strategy

### Domain Tests

Focus:

- amenity persistence in candidate lifecycle
- room invariants after `features` removal
- `OPEN -> AWAITING_BOOKING -> BOOKED`
- `AWAITING_BOOKING -> OPEN` with failed candidate history
- archived/failed candidates excluded from active selection

### Application / Integration Tests

Focus:

- `AccommodationPollService` mapping of amenities and rooms
- successful booking creates `Accommodation` only after success command
- failed booking reopens the poll and preserves attempt notes
- migration safety for existing polls/candidates

### Web / Controller Tests

Focus:

- organiser-only select / book / fail endpoints
- form binding for amenity lists and repeated room fields
- presence of KPI cards, booking banner, and failed section in the model/view states
- tenant isolation and participant authorization

### E2E Tests

Minimum critical paths:

1. Organiser creates a poll with amenities and multiple rooms.
2. Participants vote and see ranked bars plus card-based candidate details.
3. Organiser selects a winner and sees `AWAITING_BOOKING`.
4. Organiser marks booking failure; poll reopens and failed candidate moves to history.
5. Organiser selects another candidate and marks booking success.
6. Booked accommodation becomes visible as the confirmed outcome.

---

## Release Readiness Criteria

- all Trips tests green, including migration-sensitive persistence coverage
- mobile layout verified at `<=767px` for create and overview pages
- no horizontal overflow in room entry
- no path remains where poll selection auto-creates booked accommodation
- copy and i18n updated for amenities, booking pending, booking failure, and map action

---

## Follow-up Risks

- address quality depends on import quality; map links need graceful fallback when only a name is known
- if many candidates are added, colour reuse in bar charts may reduce distinction; cap or group views
  may be needed later
- thumbnail/image support is still absent, so the listing-style card relies on text hierarchy rather
  than photography for now
