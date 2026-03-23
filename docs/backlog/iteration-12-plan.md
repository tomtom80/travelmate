# Iteration 12 — Delivery Plan: Shared Trip Account and Party-Centric Trip Management

**Date**: 2026-03-19
**Target Version**: v0.12.0
**Input**: [iteration-12-stories.md](./iteration-12-stories.md), [iteration-12-scope.md](../design/eventstorming/iteration-12-scope.md)

**Status**: DONE / released as `v0.12.0` on 2026-03-23

## Release Verification

- `docker compose up --build -d` completed successfully against the current workspace state
- full end-to-end verification passed with `./mvnw -Pe2e -pl travelmate-e2e clean verify -DskipTests=false`
- final result: `Tests run: 222, Failures: 0, Errors: 0, Skipped: 32`

## Delivered Outcome

- party members can add/remove their own trip participants and maintain same-party stay periods
- trip organizers can grant additional organizers, but only to account-bound participants
- the expense area is available during the current trip, not only after completion
- the main expense view is now the party account with running account entries and party balances
- accommodation uses `stay period x weighting`
- age-based weighting defaults are visible and organizer-overridable
- advance payments and receipts are visible inside the same party-account model

---

## Planning Goal

Iteration 12 is not a cosmetic release. It changes two important domain assumptions:

1. **Trip management becomes party-self-service**
2. **Expense becomes a live shared trip account with party balances**

The plan therefore optimizes for:

- domain correctness first
- visible user value early
- minimal architectural backtracking

---

## Proposed Delivery Cut

### Must

| ID | Story | Why it is must-have |
|----|-------|---------------------|
| S12-A | Manage Own Party Participants on Trip | foundational domain permission change |
| S12-B | Maintain Stay Periods with Party Scope + Organizer Override | direct input to cost model |
| S12-D | Open Shared Trip Account for Current Trip | required lifecycle correction |
| S12-E | Party Account View as Primary Expense Screen | required visible UX outcome |
| S12-F | Allocate Accommodation Cost from Shared Pool | core accounting rule |
| S12-H | Shopping Receipts as Shared Pool Debit + Party Credit | core live-trip workflow |

### Should

| ID | Story | Why it is should-have |
|----|-------|-----------------------|
| S12-G | Equal Advance Payments per Party | important, but can land after core account model exists |
| S12-C | Grant Additional Trip Organizers | useful governance, but not on cost-critical path |

### Could / Defer if Needed

| Topic | Reason to defer |
|-------|------------------|
| personal-detail subview under party account | nice follow-up, not main domain shift |
| IAM synchronization of trip organizer rights | avoid blocking on cross-context auth refinement |
| advanced review queue UX | review is explicitly not the primary concern |
| final account closing workflow polish | can be minimal in first cut if current-state model works |

### Delivered in Addition

| Topic | Why it mattered |
|-------|------------------|
| UI hardening for trip participant actions | stable desktop/mobile table behavior with overflow actions |
| DOB propagation from IAM to Expense | required for age-band weighting defaults |
| explicit party-account timeline in UI and PDF | makes the live account auditable for end users |

---

## Important Next Step After Iteration 12

After the current party-management and shared-account slice lands, the next major planning step should move back to the beginning of the trip lifecycle:

1. Create and run a date poll with multiple candidate trip periods.
2. Let adult party members with accounts vote in a doodle-like way and inspect the live result at any time.
3. Allow the organizer to confirm the final trip period from the poll outcome.
4. Collect accommodation candidates collaboratively.
5. Run a transparent accommodation vote with exactly one active vote per participant account and allow re-voting.
6. Let the organizer finalize the accommodation booking while keeping losing options archived as fallback.
7. Extend meal planning with kitchen-duty assignment per executed meal.

Backlog anchor:
- `US-TRIPS-080` to `US-TRIPS-084`
- `US-TRIPS-035`

Reasoning:
- This is the next unresolved domain bottleneck at trip start.
- It complements Iteration 12 instead of diluting it.
- It creates cleaner inputs for later accommodation and meal-plan execution.

---

## Delivery Slices

### Slice 1: Party Self-Management in Trips

Includes:
- S12-A
- S12-B
- minimal foundations for S12-C

User outcome:
- a travel party can manage its own trip participation
- stay periods become trustworthy and current

Engineering outcome:
- Trips authorization model distinguishes:
  - own party permissions
  - organizer override permissions

### Slice 2: Shared Trip Account Lifecycle

Includes:
- S12-D
- minimal data plumbing needed for S12-E/F/H

User outcome:
- the current trip already has an expense/account area before completion

Engineering outcome:
- Expense lifecycle no longer depends on trip completion for first usefulness

### Slice 3: Party Account as Main Expense View

Includes:
- S12-E
- minimum data needed to render party-centric balance

User outcome:
- users stop thinking in person-to-person transfers
- they see their own party account directly

Engineering outcome:
- `PartyAccountView` becomes the primary read model contract

### Slice 4: Shared Pool Accounting Rules

Includes:
- S12-F
- S12-H
- S12-G if capacity permits

User outcome:
- accommodation and receipts are reflected in the same party balance model
- advances remain simple and communicable

Engineering outcome:
- one coherent accounting model instead of separate ad hoc flows

---

## Technical Impact by Story

### S12-A: Manage Own Party Participants on Trip

Expected changes:
- Trips domain/service authorization logic
- trip participant UI/actions
- possible command/API split for own-party management
- event publication for add/remove remains important

Likely files/modules:
- `travelmate-trips` domain `trip`
- `travelmate-trips` application services and commands
- trip detail templates and related controller endpoints
- E2E trip lifecycle coverage

Main risk:
- party ownership checks must be explicit and testable

### S12-B: Maintain Stay Periods with Party Scope + Organizer Override

Expected changes:
- Trips authorization rules for stay-period updates
- trip detail UI for editability by same-party actors
- downstream event contracts stay relevant

Likely files/modules:
- `travelmate-trips` domain `trip`
- `StayPeriodUpdated` producers
- existing stay-period UI and tests

Main risk:
- permission drift between UI and server-side enforcement

### S12-C: Grant Additional Trip Organizers

Expected changes:
- organizer membership model in Trips
- trip UI for organizer management
- possibly security helpers / authorization checks

Likely files/modules:
- `travelmate-trips` domain `trip`
- trip representation + controller
- optional event contract if mirrored outward later

Main risk:
- avoid coupling first implementation to IAM realm-role propagation

### S12-D: Open Shared Trip Account for Current Trip

Expected changes:
- Expense lifecycle trigger changes
- consumer behavior for trip events
- navigation/linking from current trip to expense view

Likely files/modules:
- `travelmate-expense` application service lifecycle hooks
- `TripProjection` consumer logic
- `travelmate-trips` links/navigation

Main risk:
- lifecycle migration from current `TripCompleted` assumptions

### S12-E: Party Account View as Primary Expense Screen

Expected changes:
- new or expanded read model / representation
- expense detail template restructuring
- reduced emphasis on transfer tables

Likely files/modules:
- `ExpenseRepresentation`
- `expense/detail.html`
- view-layer tests and E2E checks

Main risk:
- avoid mixing old transfer semantics into the new main view

### S12-F: Allocate Accommodation Cost from Shared Pool

Expected changes:
- Expense domain/accounting logic
- shared-pool booking logic
- weighted allocation using stay periods and participant weights

Likely files/modules:
- `travelmate-expense` domain `expense`
- `TripProjection` participant/stay-period data
- tests around weighted allocation

Main risk:
- allocation formula and rounding need to be stable and explainable

### S12-G: Equal Advance Payments per Party

Expected changes:
- advance payment setup and account projection
- party-level target + paid credit representation

Likely files/modules:
- `travelmate-expense` domain `expense`
- existing advance payment logic / templates

Main risk:
- keep this deliberately simpler than accommodation allocation

### S12-H: Shopping Receipts as Shared Pool Debit + Party Credit

Expected changes:
- receipt booking semantics
- read model distinction between shared-pool effect and party credit
- party account rendering of receipt credits

Likely files/modules:
- `travelmate-expense` domain `expense`
- `ExpenseService`
- receipt-related templates and controller

Main risk:
- prevent double counting between shared-pool debit and party credit

---

## Architecture Decisions to Lock Early

These decisions should be made before implementation starts in earnest:

1. **Trip organizer scope**
   - Recommended: organizer membership is modeled in Trips first, not blocked on IAM sync.

2. **Trip account opening trigger**
   - Recommended: open on `TripCreated` unless there is a strong product reason to delay to `TripConfirmed`.

3. **Shared cost allocation formula**
   - Recommended baseline: weighted person-nights per party divided by weighted person-nights total.

4. **Receipt booking timing**
   - Recommended baseline: show `pending receipts` separately from booked credits if review is still active.

5. **Primary expense UI contract**
   - Recommended: `PartyAccountView` is the main screen; transfer tables become secondary or disappear.

---

## Test Strategy

### Domain Tests

Focus:
- party ownership permissions in Trips
- organizer override behavior
- weighted accommodation allocation
- equal advance distribution
- receipt booking as shared debit + party credit
- party balance recomputation and rounding

Recommended targets:
- Trips domain/service tests
- Expense domain/accounting tests

### Application / Integration Tests

Focus:
- event-driven propagation from Trips to Expense
- trip account opening before completion
- stay-period update triggers recomputation
- expense view returns party account representation

Recommended targets:
- service tests in `travelmate-trips`
- service tests in `travelmate-expense`
- persistence tests for new/changed account projections

### Web / Controller Tests

Focus:
- same-party actor can add/remove own participants
- different-party actor gets rejected
- organizer can edit all stay periods
- current trip expense page accessible before completion
- party account screen renders correct sections

### E2E Tests

Minimum critical paths:
1. Party member adds own dependent to trip and sets stay period.
2. Organizer edits another party's stay period.
3. Current trip shows expense/account link before trip completion.
4. Accommodation price is set, party account shows allocated shared cost.
5. Shopping receipt is submitted and appears as party credit.
6. Equal advance is configured and appears on every party account.

---

## Recommended Team Sequence

### Track A: Trips

Order:
1. S12-A
2. S12-B
3. S12-C

Reason:
- Trips must produce correct participation and stay-period data before Expense can allocate correctly.

### Track B: Expense

Order:
1. S12-D
2. S12-E
3. S12-F
4. S12-H
5. S12-G

Reason:
- lifecycle first, then main view, then accounting rules, then simplification layer for advances.

### Parallelism Recommendation

Parallel work is safe once these boundaries are respected:
- Trips team owns participant/organizer/stay-period changes.
- Expense team owns trip account lifecycle, party account view, and accounting rules.
- Shared contract changes on trip event payloads should be agreed before both tracks diverge.

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| authorization rules become inconsistent between UI and backend | high | enforce rules in service/domain tests first |
| weighted allocation formula remains ambiguous | high | lock formula in ADR or explicit iteration note before coding |
| old transfer-based expense view leaks into new UX | medium | treat `PartyAccountView` as the only primary representation |
| lifecycle migration leaves existing expense flows half-working | high | implement account-opening tests before UI refactor |
| receipt credits get double-counted | high | add explicit accounting tests for pool debit vs party credit |

---

## Exit Criteria

Iteration 12 should be considered successful only if all of the following are true:

1. A travel party can manage its own trip participants and stay periods.
2. Organizers can still act across all parties.
3. The current trip exposes a live expense account before trip completion.
4. The main expense screen is party-centric, not transfer-centric.
5. Accommodation is allocated from the shared pool using weighted participant/stay-period data.
6. Shopping receipts affect both the shared account and the performing party balance correctly.
7. Advances, if included, remain equal per party and clearly distinct from weighted allocation.

---

## Recommendation

Plan the iteration around the `Must` slice first and treat `Should` as capacity-controlled.
If the team hits ambiguity on formula or lifecycle, cut `S12-C` before cutting any of the core
shared-account stories.
