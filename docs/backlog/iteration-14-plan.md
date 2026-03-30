# Iteration 14 — Delivery Plan: Collaborative Trip Planning (Date Poll + Accommodation Poll)

**Date**: 2026-03-29
**Target Version**: v0.14.0
**Input**: [iteration-14-stories.md](./iteration-14-stories.md), [iteration-14-scope.md](../design/eventstorming/iteration-14-scope.md)

**Status**: REFINED

---

## Planning Goal

Iteration 14 introduces collaborative decision-making into the trip planning process. Two polling
mechanisms allow travel parties to collectively find a travel period and choose an accommodation:

1. **DatePoll** — Doodle-style multi-select: "Which date ranges work for your party?"
2. **AccommodationPoll** — Single active vote with re-vote: "Which accommodation do you prefer?"

Both polls run within the collaborative planning phase. The organizer retains final decision power,
but a newly created trip is no longer treated as fully scheduled.

The plan optimizes for:

- domain correctness first (separate aggregates with clean invariants)
- visible user value early (DatePoll is usable before AccommodationPoll lands)
- consistent trip lifecycle and planning semantics (ADR-0021)

---

## Architecture Decisions

| ADR | Decision | Rationale |
|-----|----------|-----------|
| ADR-0019 | Separate DatePoll + AccommodationPoll aggregates | Different voting modes, candidate types, and result actions — generic Poll would blur invariants |
| ADR-0021 | Trip starts as planning container; polls determine verbindliche Planungsentscheidungen | Removes the contradiction between trip creation, planning UI, and collaborative polls |

---

## Proposed Delivery Cut

### Must

| ID | Story | Why it is must-have |
|----|-------|---------------------|
| S14-A | Create Date Poll with Options | foundational aggregate — poll must exist before voting |
| S14-B | Vote in Date Poll (Doodle-style) | core collaborative value — date finding |
| S14-C | Confirm Date Poll and Update Trip Period | closes the loop — poll result becomes trip dates |
| S14-D | Propose and Manage Accommodation Candidates | enables collaborative accommodation shortlist |
| S14-E | Vote for Accommodation and Finalize Selection | completes collaborative planning loop |
| S14-G | Refine Trip Lifecycle for Collaborative Planning | removes contradictions between trip creation and poll-driven decisions |

### Should

| ID | Story | Why it is should-have |
|----|-------|-----------------------|
| S14-F | Trip Planning UI Integration | unified planning view — can be minimal in first cut |

### Could / Defer

| Topic | Reason to defer |
|-------|------------------|
| Email notification on poll creation/confirmation | useful but not on critical domain path |
| HTMX live polling for real-time vote updates | nice-to-have, manual refresh is acceptable first |
| Kitchen duty assignment (US-TRIPS-035) | independent feature, defer to Iteration 15 |

---

## Delivery Slices

### Slice 1: DatePoll Aggregate (S14-A + S14-B)

Includes:
- DatePoll aggregate (domain, repository port, persistence adapter)
- DateOption + DateVote entities
- DatePollService (create, add/remove options, cast/change vote)
- DatePollController + Thymeleaf templates
- Flyway V13 (date_poll, date_option, date_vote, date_vote_selection)

User outcome:
- organizer creates a date poll with candidate periods
- participants vote in Doodle-style multi-select
- live result matrix visible to all participants

Engineering outcome:
- DatePoll aggregate established with clean invariants
- Repository + persistence adapter pattern consistent with existing aggregates

### Slice 1.5: Trip Lifecycle Refinement (S14-G)

Includes:
- trip creation without mandatory final date range
- planning state semantics aligned with ADR-0021
- planning dashboard copy aligned with collaborative date and accommodation decisions
- blocking of direct accommodation management before confirmed accommodation decision

User outcome:
- a new trip starts as a planning workspace, not as a fully scheduled trip
- participants understand that both travel period and accommodation are decided collaboratively

Engineering outcome:
- trip lifecycle and poll workflow use one consistent domain model
- groundwork for event and UI migration away from mandatory `Trip.dateRange`

### Slice 2: DatePoll Confirmation (S14-C)

Includes:
- ConfirmDatePoll command → DatePoll.confirm()
- Trip.confirmDateRange(DateRange) as the step that establishes the verbindliche Reisezeit
- StayPeriod reset on date range change
- MealPlan invalidation/regeneration on date range change

User outcome:
- organizer confirms winning date → trip period updates automatically
- participants re-enter stay periods after date change

Engineering outcome:
- Trip aggregate aligned with the lifecycle semantics from ADR-0021
- StayPeriod + MealPlan cascading updates tested

### Slice 3: AccommodationPoll Aggregate (S14-D + S14-E)

Includes:
- AccommodationPoll aggregate (domain, repository port, persistence adapter)
- AccommodationCandidate + AccommodationVote entities
- AccommodationPollService (create, propose, archive, vote, move, select)
- AccommodationPollController + Thymeleaf templates
- Flyway V14 (accommodation_poll, accommodation_candidate, accommodation_vote)
- URL-Import integration: poll-aware routing in AccommodationImportService

User outcome:
- all participants can suggest accommodation candidates
- single-vote with re-vote for fair preference signaling
- organizer finalizes accommodation booking from shortlist
- URL-Import feeds into poll when one is open
- direct accommodation management stays blocked until a decision is confirmed

Engineering outcome:
- AccommodationPoll aggregate with ACTIVE/ARCHIVED/SELECTED candidate lifecycle
- Existing URL-Import pipeline becomes poll-aware

### Slice 4: Planning UI (S14-F)

Includes:
- Trip detail page planning section/tab
- DatePoll: Doodle-like matrix (responsive)
- AccommodationPoll: card layout with vote buttons
- HTMX-driven interactions

User outcome:
- unified planning view for the trip
- both polls accessible from trip detail page

Engineering outcome:
- consistent HTMX patterns across poll UIs
- mobile-responsive layouts (PicoCSS + custom cards)

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S14-A | DatePoll aggregate — establishes the pattern |
| 2 | S14-B | voting model completes the DatePoll value |
| 3 | S14-G | lifecycle refinement before the final workflow is hardened |
| 4 | S14-C | confirmation closes the date planning loop |
| 5 | S14-D | AccommodationPoll aggregate — can overlap with S14-B |
| 6 | S14-E | voting + selection completes accommodation planning |
| 7 | S14-F | unified planning UI integration |

**Parallelism**: S14-A/B/C and S14-D/E are still largely independent tracks. S14-G must align the
trip lifecycle before the final UI and confirmation flows are considered complete. S14-F is
incremental and can grow alongside other stories.

---

## Technical Impact by Story

### S14-A + S14-B: DatePoll

Expected changes:
- New package: `trips/domain/datepoll/` (DatePoll, DatePollId, DateOption, DateOptionId, DateVote, DateVoteId, PollStatus, DatePollRepository)
- New: `trips/application/DatePollService.java` + commands
- New: `trips/adapters/persistence/DatePoll*` (JPA entity, JPA repository, adapter)
- New: `trips/adapters/web/DatePollController.java` + templates
- Flyway V13 migration

Main risk:
- voter identity check (Account vs. Dependent) requires TravelParty lookup

### S14-C: DatePoll Confirmation

Expected changes:
- `Trip.java` — lifecycle refinement so the final date range is not required at creation
- `DatePollService.confirmPoll()` orchestrates DatePoll + Trip updates
- StayPeriod reset logic in Trip aggregate
- MealPlan invalidation handler
- event contract review for `TripCreated` and follow-up confirmation events

Main risk:
- migration from mandatory `Trip.dateRange` to lifecycle-aware planning state
- StayPeriod reset UX — participants must re-enter dates (HS-2)
- MealPlan regeneration must handle existing recipes

### S14-D + S14-E: AccommodationPoll

Expected changes:
- New package: `trips/domain/accommodationpoll/` (AccommodationPoll, AccommodationPollId, AccommodationCandidate, CandidateId, CandidateStatus, AccommodationVote, AccommodationPollRepository)
- New: `trips/application/AccommodationPollService.java` + commands
- Modified: `AccommodationImportService` — poll-aware routing (HS-4)
- New: `trips/adapters/persistence/AccommodationPoll*` + Flyway V14

Main risk:
- URL-Import integration requires conditional routing based on open poll existence
- Candidate archiving must cascade vote removal cleanly

---

## Test Strategy

### Domain Tests

Focus:
- DatePoll creation (min 2 options), voting invariants (account-only, multi-select)
- DatePoll confirmation (selects one option, status transition)
- AccommodationPoll candidate lifecycle (ACTIVE → ARCHIVED → SELECTED)
- AccommodationPoll voting (single vote, move vote, archive cascade)
- trip lifecycle progression toward readiness and StayPeriod reset

Recommended targets:
- `DatePollTest`, `AccommodationPollTest` — comprehensive domain unit tests
- `TripTest` — lifecycle-aware creation and confirmDateRange()

### Application / Integration Tests

Focus:
- DatePollService orchestration (create, vote, confirm → Trip update)
- AccommodationPollService orchestration (propose, vote, select → Accommodation create)
- Voter identity validation via TravelParty
- Poll-aware URL-Import routing
- blocking of direct accommodation management before the poll decision is confirmed

### Web / Controller Tests

Focus:
- DatePollController — create, vote, confirm endpoints
- AccommodationPollController — propose, vote, select endpoints
- Authorization: organizer-only actions vs. participant actions
- Tenant isolation on all endpoints

### E2E Tests

Minimum critical paths:
1. Organizer creates DatePoll with 3 date options
2. Two participants vote in DatePoll (multi-select)
3. Organizer confirms winning date → trip period updated
4. Participant proposes accommodation candidate
5. Participants vote for accommodation
6. Organizer selects accommodation → Accommodation aggregate created
7. Direct accommodation management becomes available only after the decision is confirmed

### BDD Scenarios

German Gherkin scenarios for:
- DatePoll creation and voting
- DatePoll confirmation with trip date update
- AccommodationPoll proposal and voting
- AccommodationPoll selection by organizer

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Lifecycle migration around missing `Trip.dateRange` causes regressions | HIGH | Add focused domain, controller, and E2E coverage for trips without dates at creation |
| StayPeriod reset after date change causes user confusion | HIGH | Toast notification + clear re-entry prompt in UI |
| URL-Import/poll routing becomes fragile | MEDIUM | Conditional check in AccommodationImportService with clear fallback |
| Open polls block Trip.confirm() unexpectedly | MEDIUM | Clear UI guidance showing poll closure as precondition |
| Two new aggregates increase schema complexity | LOW | Clean separation via Flyway migrations V13/V14 |
| Voter identity check adds TravelParty coupling | LOW | Application Service pre-validation, not aggregate coupling |

---

## Exit Criteria

Iteration 14 should be considered successful only if all of the following are true:

1. An organizer can create a DatePoll with multiple candidate date ranges.
2. Account-holding participants can vote in Doodle-style multi-select.
3. Organizer confirmation of a date option updates the trip period.
4. StayPeriods are reset cleanly when the trip period changes.
5. Any account-holding participant can propose accommodation candidates.
6. Participants can cast and move a single accommodation vote.
7. Organizer selection of an accommodation creates/links the Accommodation aggregate.
8. Both polls are only available during PLANNING trip status.
9. All polls are scoped by TenantId (tenant isolation maintained).
10. Dependents cannot vote in either poll type.

---

## Follow-Up After Iteration 14

The next iteration should focus on:

- **US-TRIPS-035**: Kitchen duty assignment for executed meals
- **Email notifications** for poll creation, voting reminders, and confirmation
- **HTMX live polling** for real-time vote updates during active polls
- **Rezept-Import aus URL** (low priority, deferred from earlier iterations)
