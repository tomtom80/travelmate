# Iteration 14 — Refined User Stories: Collaborative Trip Planning (Date Poll + Accommodation Poll)

**Date**: 2026-03-29
**Target Version**: v0.14.0
**Bounded Contexts**: Trips (date poll, accommodation poll, trip lifecycle)

**Implementation Status**: REFINED

---

## Overview

This iteration introduces two collaborative decision-making mechanisms for the trip planning phase.
Both are rooted in the same domain insight: the organizer has final authority, but the group should
have a structured way to contribute before the decision is made and before the trip becomes
verbindlich geplant.

**DatePoll** is a Doodle-style multi-select poll. The organizer defines candidate date ranges, all
account-holding participants mark which options work for them, and the organizer confirms one as the
final trip period. The confirmed option determines the verbindliche Reisedaten.

**AccommodationPoll** is a single active-vote poll. Any account-holding participant can propose
candidates (manually or via URL import), all participants cast exactly one vote and can move it,
and the organizer selects the final accommodation. The selected candidate creates a full
`Accommodation` aggregate.

Both polls are restricted to the collaborative planning phase and may run in parallel. The organizer
does not lose final authority, but direct-edit paths must not undercut the democratic default flow:
exact trip dates are not set at trip creation, and a verbindliche accommodation is not created
before the accommodation decision is confirmed. Dependents (Mitreisende) have no voting right in
either poll.

The two mechanisms are modelled as **two separate aggregate roots** (`DatePoll`,
`AccommodationPoll`) rather than a generic poll, because their voting modes, option types, result
actions, and candidate lifecycles are sufficiently different that a shared abstraction would obscure
the domain rules. See ADR-0019 (candidate).

---

## Dependency Graph

```
S14-A: Create Date Poll with Options
  — foundation for all DatePoll voting and confirmation
  — must exist before any vote can be cast

S14-B: Vote in Date Poll (Doodle-style)
  — depends on S14-A (DatePoll aggregate must exist)
  — provides the multi-select read model used in S14-C

S14-C: Confirm Date Poll and Update Trip Period
  — depends on S14-B (votes must be visible to decide)
  — triggers the final trip date decision; resets StayPeriods if required

S14-D: Propose and Manage Accommodation Candidates
  — independent from DatePoll stories; can start in parallel with S14-A
  — foundation for AccommodationPoll voting in S14-E

S14-E: Vote for Accommodation and Finalize Selection
  — depends on S14-D (AccommodationPoll and candidates must exist)
  — SelectAccommodation creates the verbindliche Accommodation aggregate

S14-F: Trip Planning UI Integration
  — depends on all previous stories being functional
  — assembles both polls into a coherent planning tab/section

S14-G: Refine Trip Lifecycle for Collaborative Planning
  — aligns trip creation, planning dashboard, and poll outcomes
  — removes the mandatory date contradiction from trip creation
```

---

## Recommended Iteration 14 Scope

| ID | Story | Priority | Size | Bounded Context |
|----|-------|----------|------|-----------------|
| S14-A | Create Date Poll with Options | Must | L | Trips |
| S14-B | Vote in Date Poll (Doodle-style) | Must | L | Trips |
| S14-C | Confirm Date Poll and Update Trip Period | Must | M | Trips |
| S14-D | Propose and Manage Accommodation Candidates | Must | L | Trips |
| S14-E | Vote for Accommodation and Finalize Selection | Must | L | Trips |
| S14-F | Trip Planning UI Integration | Should | M | Trips |
| S14-G | Refine Trip Lifecycle for Collaborative Planning | Must | L | Trips |

**Scope rationale:**

S14-A through S14-C form the complete DatePoll lifecycle. They are sequentially dependent and must
be delivered together to provide any user-visible value. Splitting them across iterations would
leave users with a poll they cannot close.

S14-D and S14-E form the complete AccommodationPoll lifecycle. The same argument applies: a poll
without a selection mechanism is not useful. S14-D can be developed in parallel with S14-A/B.

S14-F is a Should story because both poll types have standalone HTMX-driven UIs as part of their
own stories. S14-F adds the integrated planning dashboard view. It can be deferred if scope is
tight, but the overall UX is significantly weaker without it.

S14-G is a Must story because the previous lifecycle assumption (`Trip.dateRange` as mandatory at
creation) conflicts with the core value of collaborative planning and with the intended user
journey.

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S14-A | DatePoll aggregate first — domain model, Flyway V13, persistence, basic controller |
| 2 | S14-D | AccommodationPoll aggregate — can start once S14-A domain patterns are established |
| 3 | S14-B | DatePoll voting on top of the aggregate |
| 4 | S14-E | AccommodationPoll voting + SelectAccommodation |
| 5 | S14-G | Lifecycle refinement before final UI hardening |
| 6 | S14-C | DatePoll confirmation + final date decision + StayPeriod reset |
| 7 | S14-F | Planning UI integration on top of working polls |

---

## Follow-Up After Iteration 14

- **US-TRIPS-035**: Kitchen Duty Assignment — assign participants to executed MealPlan slots; Should,
  M; deferred because the collaborative planning workflow is the higher-value next step.
- **Rezept-Import aus URL** (US-TRIPS-041): low priority; the HtmlAccommodationImportAdapter pattern
  from S10-A applies directly but this feature can wait.
- **Bring-App integration** (US-TRIPS-055): Einkaufsliste to Bring API sync; deferred, no stable API.
- **TravelPartyNameRegistered event**: small story needed for correct party-name propagation into
  Expense; should be scheduled before or within the next Expense-focused iteration.
- **ADR-0019** (separate Poll aggregates) should be written during or immediately after this
  iteration.
- **ADR-0021** should supersede the former ADR-0020 draft and define the lifecycle-aware planning
  process for trips without mandatory dates at creation.

---

## Story S14-A: Create Date Poll with Options

**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As an** organizer, **I want** to create a date poll with at least two candidate date ranges for a
trip in planning, **so that** all account-holding participants can indicate their availability before
a final trip period is set.

### Acceptance Criteria

- **Given** a trip is in PLANNING status and has no open date poll, **When** I open the date poll
  creation form and submit at least two candidate date ranges, **Then** a `DatePoll` is created with
  status `OPEN` and the options are visible to all trip participants.

- **Given** an open `DatePoll` exists for the trip, **When** I try to create a second date poll for
  the same trip, **Then** the system rejects the action and shows an error.

- **Given** I submit fewer than two date options, **When** I confirm creation, **Then** the system
  rejects the action and shows a validation error asking for at least two options.

- **Given** an open `DatePoll` exists, **When** I am the organizer and add an additional date range
  option, **Then** the new option appears in the poll immediately.

- **Given** an open `DatePoll` exists and an option has no votes yet, **When** I remove that option
  as the organizer, **Then** the option is deleted and no longer shown.

- **Given** a trip is in CONFIRMED, IN_PROGRESS, or COMPLETED status, **When** I try to create a
  date poll, **Then** the system rejects the action because polls are only allowed during PLANNING.

### Technical Notes

- Bounded Context: Trips
- New Aggregate: `DatePoll` (Aggregate Root) in `domain/datepoll/`
- Key domain types: `DatePollId`, `DateOption`, `DateOptionId`, `PollStatus` (OPEN / CONFIRMED /
  CANCELLED)
- Commands: `CreateDatePollCommand`, `AddDateOptionCommand`, `RemoveDateOptionCommand`
- Events: `DatePollCreated`, `DateOptionAdded`, `DateOptionRemoved` (BC-internal, not cross-SCS)
- Repository port: `DatePollRepository.findOpenByTripId(TenantId, TripId)` — enforces one open poll
  per trip via query, not DB unique constraint alone
- `DateRange` Value Object reused from `domain/trip/`
- Flyway V13: `date_poll`, `date_option`, `date_vote`, `date_vote_selection` tables
- Controller: `DatePollController` under `adapters/web/`
- UI: form with dynamic add/remove of date range rows, HTMX-driven; accessible from trip detail page

---

## Story S14-B: Vote in Date Poll (Doodle-style)

**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As an** account-holding trip participant, **I want** to mark which candidate date ranges work for
me in a multi-select manner, **so that** the group can see where availability overlaps.

### Acceptance Criteria

- **Given** a `DatePoll` is OPEN and I am an account-holding participant (Mitglied) of the trip,
  **When** I select one or more date options, **Then** my vote is recorded and immediately visible
  in the poll result matrix.

- **Given** I have already voted, **When** I change my selection and resubmit, **Then** my previous
  vote is fully replaced by the new selection.

- **Given** I am a dependent (Mitreisende(r)) without my own account, **When** I open the poll
  page, **Then** I see the current results but have no vote button.

- **Given** multiple participants have voted, **When** any participant opens the poll,
  **Then** they see a Doodle-style matrix: rows are participants, columns are date options, cells
  show each participant's selection, and a summary row shows the total count per option.

- **Given** the `DatePoll` is in CONFIRMED or CANCELLED status, **When** I open the poll,
  **Then** I see the final read-only result but cannot cast or change a vote.

- **Given** I have not voted yet, **When** I open the poll, **Then** my row in the result matrix
  shows an empty or pending state that distinguishes me from participants who have voted.

### Technical Notes

- Bounded Context: Trips
- Commands: `CastDateVoteCommand`, `ChangeDateVoteCommand`
- `DateVote` entity within `DatePoll`: `DateVoteId`, `voterId` (Account UUID), `Set<DateOptionId>`
- Voting right check: `DatePollService.castVote()` validates via `TravelPartyRepository` that the
  voter is a Member (Account-holder), not a Dependent — the aggregate itself only stores UUIDs
- Re-vote: `ChangeDateVoteCommand` replaces the existing `DateVote` for the voter atomically within
  the aggregate
- Read model (RM-1): list of options with per-option vote counts plus per-voter selections for the
  matrix view; derived from `DatePoll` state, no separate projection table needed
- UI: HTMX-driven checkbox matrix; submitting via POST replaces the voter's row fragment
- Voter identity resolved from JWT `email` claim via `TravelPartyRepository`

---

## Story S14-C: Confirm Date Poll and Update Trip Period

**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: M
**As an** organizer, **I want** to confirm one date option as the winner of the date poll, **so
that** the trip period is updated and planning can progress to booking.

### Acceptance Criteria

- **Given** a `DatePoll` is OPEN with at least one option, **When** I confirm one option as the
  winner, **Then** the poll status changes to CONFIRMED, the confirmed option is stored, and
  `Trip.dateRange` is updated to the confirmed date range in the same operation.

- **Given** `Trip.dateRange` is updated by the poll confirmation, **When** existing `StayPeriod`
  records fall outside the new date range, **Then** all affected StayPeriods are reset and
  participants are informed that they need to re-enter their stay dates.

- **Given** I am the organizer, **When** I confirm an option, **Then** I can confirm any option
  regardless of vote count — the organizer is not forced to pick the numerical winner.

- **Given** the `DatePoll` is CONFIRMED, **When** any participant views the poll result,
  **Then** the confirmed date range is highlighted and the historical vote matrix remains visible
  as a read-only record.

- **Given** I want to close the poll without picking a winner, **When** I cancel the poll,
  **Then** the poll status changes to CANCELLED, `Trip.dateRange` is not modified, and no
  StayPeriods are reset.

- **Given** the `DatePoll` is already CONFIRMED or CANCELLED, **When** I try to confirm or cancel
  it again, **Then** the system rejects the action.

### Technical Notes

- Bounded Context: Trips
- Commands: `ConfirmDatePollCommand`, `CancelDatePollCommand`
- Event: `DatePollConfirmed(tenantId, tripId, datePollId, startDate, endDate, occurredOn)` —
  BC-internal; no cross-SCS publishing needed
- Policy P1 (HS-1 resolution): `ConfirmDatePollService` calls `DatePoll.confirm()` then
  `Trip.confirmDateRange(DateRange)` in the same transaction; no separate event listener needed
  because both aggregates live in the same BC
- `Trip.confirmDateRange()`: new method on Trip aggregate; updates `dateRange` field and registers
  a `TripDateRangeUpdated` domain event (BC-internal)
- StayPeriod reset: `Trip.confirmDateRange()` iterates participants and sets affected StayPeriods to
  null; downstream accommodation cost allocation must recompute
- HS-1 resolution: `Trip.dateRange` remains a mandatory field (Pflichtfeld); the organizer sets an
  initial proposed range at trip creation; `DatePoll.confirm()` overwrites it — no breaking change
  to the Trip constructor

---

## Story S14-D: Propose and Manage Accommodation Candidates

**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As a** participant with an account, **I want** to submit accommodation suggestions to a shared
shortlist, **so that** the group builds a collaborative pool of options before the organizer decides.

### Acceptance Criteria

- **Given** a trip is in PLANNING status and an `AccommodationPoll` exists, **When** I am an
  account-holding participant and submit an accommodation candidate with at least a name, **Then**
  the candidate is added to the shortlist with status ACTIVE.

- **Given** no `AccommodationPoll` exists for the trip yet, **When** the organizer creates one,
  **Then** an `AccommodationPoll` is created with status OPEN and the empty shortlist is visible
  to all participants.

- **Given** an ACTIVE candidate exists, **When** a participant submits a booking URL alongside
  the name and optional details, **Then** the URL is stored as the booking link and visible to all
  participants in the shortlist.

- **Given** the URL-import pipeline is used while an `AccommodationPoll` is OPEN, **When** a
  participant imports an accommodation URL, **Then** the import result creates an
  `AccommodationCandidate` in the poll instead of directly creating an `Accommodation` aggregate.

- **Given** I am the organizer and a candidate is ACTIVE, **When** I archive it, **Then** the
  candidate status changes to ARCHIVED, all votes on it are removed, and affected voters can
  re-cast their vote on another candidate.

- **Given** archived candidates exist, **When** any participant views the shortlist, **Then**
  archived candidates are visible in a separate section as fallback context but are visually
  distinguished from ACTIVE candidates.

### Technical Notes

- Bounded Context: Trips
- New Aggregate: `AccommodationPoll` (Aggregate Root) in `domain/accommodationpoll/`
- Key domain types: `AccommodationPollId`, `AccommodationCandidate` (entity), `CandidateId`,
  `CandidateStatus` (ACTIVE / ARCHIVED / SELECTED), `AccommodationVote`, `AccommodationVoteId`,
  `PollStatus` (OPEN / DECIDED / CANCELLED)
- Commands: `CreateAccommodationPollCommand`, `ProposeCandidateCommand`, `ArchiveCandidateCommand`
- Events: `AccommodationPollCreated`, `AccommodationCandidateProposed`,
  `AccommodationCandidateArchived` (BC-internal)
- Policy P4: `AccommodationPoll.archiveCandidate()` removes all votes on the candidate atomically
  within the aggregate before changing status — voters are not notified by event in MVP
- URL-Import integration (HS-4 resolution): `AccommodationImportService` checks for an open
  `AccommodationPoll` via `AccommodationPollRepository.findOpenByTripId()`; if one exists, calls
  `AccommodationPoll.proposeFromImport()` instead of `Accommodation.create()`
- Flyway V14: `accommodation_poll`, `accommodation_candidate`, `accommodation_vote` tables
- Repository port: `AccommodationPollRepository.findOpenByTripId(TenantId, TripId)`
- Controller: `AccommodationPollController` under `adapters/web/`
- Candidate fields: name (required), url (optional), address (optional), estimatedPrice (optional),
  notes (optional), proposedBy (Account UUID), status

---

## Story S14-E: Vote for Accommodation and Finalize Selection

**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As a** participant with an account, **I want** to give exactly one active vote to my preferred
accommodation candidate and move it freely, **so that** the group preference is visible before the
organizer makes the final call.

### Acceptance Criteria

- **Given** an `AccommodationPoll` is OPEN with at least one ACTIVE candidate, **When** I am an
  account-holding participant and cast my vote, **Then** exactly one active vote is linked to my
  chosen candidate and the vote counts update immediately for all participants.

- **Given** I already have an active vote, **When** I choose a different candidate, **Then** my
  vote is removed from the previous candidate and placed on the new one atomically, with no moment
  where I hold two votes.

- **Given** I am a dependent (Mitreisende(r)) without an account, **When** I open the poll,
  **Then** I see the current vote counts per candidate but have no vote button.

- **Given** I am the organizer, **When** I select a candidate as the final accommodation,
  **Then** the poll status changes to DECIDED, the selected candidate status changes to SELECTED,
  all non-selected ACTIVE candidates change to ARCHIVED, and a full `Accommodation` aggregate is
  created from the candidate data.

- **Given** the `AccommodationPoll` transitions to DECIDED, **When** any participant views the
  trip detail, **Then** the Accommodation section shows the newly created `Accommodation` with the
  same name, address, estimated price, and booking URL as the winning candidate.

- **Given** the organizer wants to close the poll without selecting a candidate, **When** they
  cancel the poll, **Then** the poll status changes to CANCELLED, no `Accommodation` aggregate is
  created, and no existing `Accommodation` is deleted.

### Technical Notes

- Bounded Context: Trips
- Commands: `CastAccommodationVoteCommand`, `MoveAccommodationVoteCommand`,
  `SelectAccommodationCommand`, `CancelAccommodationPollCommand`
- Events: `AccommodationVoteCast`, `AccommodationVoteMoved` (BC-internal, aggregate-state only),
  `AccommodationSelected(tenantId, tripId, pollId, candidateId, name, occurredOn)` (BC-internal),
  `AccommodationPollCancelled` (BC-internal)
- Single active vote invariant: `UNIQUE (accommodation_poll_id, voter_id)` in DB + enforced in
  aggregate via `MoveAccommodationVote` replacing the prior row
- Voting right check: same pattern as `DatePoll` — `AccommodationPollService` validates via
  `TravelPartyRepository` that voter is a Member; aggregate stores only UUIDs
- Policy P2: `SelectAccommodationService` calls `AccommodationPoll.select(candidateId)` then
  `AccommodationService.createFromCandidate(candidate)` in the same transaction
- `Accommodation.create()` reuses the existing `SetAccommodationCommand` or a new
  `CreateAccommodationFromCandidateCommand` — decision to be made during implementation
- If an `Accommodation` already exists for the trip (e.g. set before poll was opened), it is
  replaced by the newly created one from the selected candidate
- Read model (RM-2): card layout per ACTIVE candidate showing name, url, estimated price,
  proposed-by, vote count, and the current user's active vote indicator

---

## Story S14-F: Trip Planning UI Integration

**Epic**: E-TRIPS-08
**Priority**: Should
**Size**: M
**As a** trip participant, **I want** a dedicated planning section on the trip detail page that
shows the date poll and the accommodation poll side by side, **so that** I can track the planning
state of the trip in one place without navigating between separate pages.

### Acceptance Criteria

- **Given** I open the trip detail page and the trip is in PLANNING status, **When** at least one
  poll (date or accommodation) is active or has been decided, **Then** a planning section is
  visible on the trip detail page.

- **Given** both a `DatePoll` and an `AccommodationPoll` exist, **When** I view the planning
  section on desktop, **Then** both polls are shown side by side with distinct visual sections.

- **Given** I am on a mobile screen, **When** I view the planning section, **Then** the polls
  stack vertically and each poll is fully usable without horizontal scrolling.

- **Given** the DatePoll is displayed, **When** I view it, **Then** the Doodle-style matrix fits
  the screen responsively and vote interactions are HTMX-driven without full page reloads.

- **Given** the AccommodationPoll is displayed, **When** I view it, **Then** each candidate is
  shown as a card with the vote button, vote count, and booking URL link; voting is HTMX-driven.

- **Given** a poll is in CONFIRMED or DECIDED status, **When** I view it in the planning section,
  **Then** the winner is clearly highlighted and the section shows the decision as final context
  rather than an action-required prompt.

### Technical Notes

- Bounded Context: Trips
- No new aggregate or domain logic — this is a UI composition story
- `TripController.detail()` extended to include `DatePollViewModel` and `AccommodationPollViewModel`
  (optional, present only when polls exist)
- Thymeleaf fragment structure: `trip/planning.html` (partial) included from `trip/detail.html`
- HTMX fragments: date poll matrix row, accommodation candidate card, vote button — each
  replaceable via `hx-swap="outerHTML"` on the relevant container
- DatePoll matrix: `<table>` with `<tr>` per voter and `<th>` per option; aggregated count row at
  the bottom; own-vote row submitted via HTMX form
- AccommodationPoll: `.card-grid` layout (from Iteration 11 shared CSS utilities); vote button
  uses HTMX POST; active vote shown with CSS highlight class
- Mobile: both polls responsive via existing bottom-sheet CSS and `.card-grid` patterns from
  Iteration 11; no additional mobile-specific CSS needed beyond what already exists
- No HTMX polling for this story — full page refresh on user action is acceptable; polling can be
  added in a follow-up if collaborative real-time feel is needed

---

## Open Design Questions

1. **Should URL-Import always create a poll candidate, or only when a poll is open?**
   The EventStorming recommendation (HS-4) is: only when a poll is OPEN does the import pipeline
   route to `proposeFromImport()`; otherwise it creates an `Accommodation` directly as before.
   This keeps the import pipeline simple and avoids forced poll creation.

2. **Should the organizer be able to close a poll without selecting a winner (cancel)?**
   Yes, both polls include `CancelDatePoll` and `CancelAccommodationPoll` commands in the
   EventStorming model. A cancel leaves the trip unaffected — no dates updated, no Accommodation
   created. The organizer retains the ability to edit trip dates and accommodation directly.

3. **Should poll results use HTMX polling for real-time updates?**
   The EventStorming defers this (HS-3 is LOW severity). MVP uses HTMX-driven interactions
   on explicit user action. Background polling (similar to the shopping list at 5-second intervals)
   can be added in a follow-up story if users report stale-data issues during group sessions.

4. **Should there be email notifications when a poll is created or confirmed?**
   Not in this iteration. The existing `InvitationEmailListener` pattern is available for
   extension, but adding poll-specific emails would expand scope significantly. Deferred to a
   dedicated notification story.

5. **What happens to open polls when the trip is cancelled?**
   Policy P3 from the EventStorming: `TripService.cancelTrip()` iterates open polls via
   `DatePollRepository` and `AccommodationPollRepository` and cancels them. This should be
   implemented in S14-A/D as a guard, or in a shared cancellation method called by `TripService`.
