# Iteration 12 — Refined User Stories: Shared Trip Account, Party Self-Management, Live Party Balances

**Date**: 2026-03-19
**Target Version**: v0.12.0
**Bounded Contexts**: Trips (participant management, stay periods, trip organizers), Expense (shared trip account, party balances)

**Implementation Status**: DONE on 2026-03-23

## Delivery Status

| ID | Status | Notes |
|----|--------|-------|
| S12-A | Done | own-party participant add/remove is implemented end to end |
| S12-B | Done | same-party stay periods plus organizer override are active |
| S12-C | Done | additional trip organizers are supported for account-bound participants only |
| S12-D | Done | expense is opened for the current trip lifecycle |
| S12-E | Done | `PartyAccount` is the primary expense view |
| S12-F | Done | accommodation allocation uses stay period and weighting |
| S12-G | Done | equal advance payments per party are integrated into the party account |
| S12-H | Done | receipts appear as shared-pool debit plus party credit in the running account |

## Final Verification

- `docker compose up --build -d`
- `./mvnw -Pe2e -pl travelmate-e2e clean verify -DskipTests=false`
- result: full browser IT and Cucumber suite green before release

---

## Overview

This iteration translates the latest domain clarification into implementable stories.

Three insights drive the scope:

1. **Trip participation is party-driven, not organizer-driven.**
   Members of a travel party must be able to add/remove their own members and dependents to the
   trip and manage their own stay periods. Organizers retain cross-party override rights.

2. **Expense is a shared trip account, not a post-trip transfer calculator.**
   The account must exist for the current trip, not only after completion. Participants need to
   see the current state during planning and while travelling.

3. **The primary accounting unit is the Travel Party.**
   Accommodation cost and shopping receipts flow into a shared trip account. Party balances are
   derived from that pool. Only advance payments are intentionally simplified to equal round
   amounts per party.

---

## Dependency Graph

```
S12-A: Manage Own Party Participants on Trip
  — foundation for realistic party-based trip participation
  — enables members to add/remove own members and dependents from a trip

S12-B: Maintain Stay Periods with Party Scope + Organizer Override
  — depends on S12-A participant management
  — provides the core input for later cost allocation

S12-C: Grant Additional Trip Organizers
  — independent from expense logic
  — strengthens trip governance without blocking accounting

S12-D: Open Shared Trip Account for Current Trip
  — foundation for all Expense stories in this iteration
  — replaces "expense only after trip completion" as the primary lifecycle

S12-E: Party Account View as Primary Expense UI
  — depends on S12-D
  — replaces the mental model of person-to-person transfers

S12-F: Shared Cost Allocation for Accommodation
  — depends on S12-B stay periods and S12-D shared account
  — allocates accommodation from shared pool to party accounts

S12-G: Equal Advance Payments per Party
  — depends on S12-E party account view
  — intentionally simpler than weighted allocation

S12-H: Shopping Receipts as Shared Pool Debit + Party Credit
  — depends on S12-D
  — produces the visible positive credit on the performing party account
```

---

## Recommended Iteration 12 Scope

| ID | Story | Priority | Size | Bounded Context |
|----|-------|----------|------|-----------------|
| S12-A | Manage Own Party Participants on Trip | Must | L | Trips |
| S12-B | Maintain Stay Periods with Party Scope + Organizer Override | Must | M | Trips |
| S12-C | Grant Additional Trip Organizers | Should | M | Trips |
| S12-D | Open Shared Trip Account for Current Trip | Must | L | Expense |
| S12-E | Party Account View as Primary Expense Screen | Must | M | Expense |
| S12-F | Allocate Accommodation Cost from Shared Pool | Must | L | Expense |
| S12-G | Equal Advance Payments per Party | Should | M | Expense |
| S12-H | Shopping Receipts as Shared Pool Debit + Party Credit | Must | M | Expense |

**Scope rationale:**
- S12-A and S12-B are essential because the domain rules explicitly move trip participant
  management from organizer-only to party-self-management.
- S12-D is the key lifecycle correction: the trip account must be visible and usable before the
  trip ends.
- S12-E is the UI consequence of the new model: the party account becomes the main accounting view.
- S12-F and S12-H encode the most important accounting rules from the workshop.
- S12-G is important but intentionally simpler and can follow after the shared-account base exists.
- S12-C is useful governance but not on the critical path for cost correctness.

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S12-A | participant model first |
| 2 | S12-B | stay periods are input to allocation |
| 3 | S12-D | shared account lifecycle must exist before expense UI/rules |
| 4 | S12-E | exposes the new mental model early |
| 5 | S12-F | main cost allocation rule |
| 6 | S12-H | live shopping receipts during the trip |
| 7 | S12-G | equal advances on top of party accounts |
| 8 | S12-C | organizer expansion can merge anytime after trip governance design is clear |

---

## Follow-Up After Iteration 12

The most important next Trips slice after this iteration is not deeper accounting refinement, but collaborative trip initialization:

- date poll with multiple candidate trip periods
- doodle-like availability voting by adult party members with accounts
- transparent result view for all trip participants
- organizer confirmation of the final trip period
- collaborative accommodation shortlist plus one active vote per participant account
- organizer final booking decision with archive/fallback handling for losing options
- kitchen duty assignment for executed meals in the meal plan

Reference backlog stories:
- `US-TRIPS-080` to `US-TRIPS-084` in `product-backlog.md`
- `US-TRIPS-035` for kitchen duty under meal planning

Why this follows Iteration 12:
- party self-management is the prerequisite for fair collaborative planning
- live trip accounting should not overshadow the unresolved planning-start workflow
- the next domain bottleneck is reaching a shared date and accommodation decision early

---

## Story S12-A: Manage Own Party Participants on Trip

**Epic**: E-TRIPS-02 / E-TRIPS-03 extension
**Priority**: Must
**Size**: L
**As a** member of a travel party, **I want** to add or remove members and dependents of my own
party to/from a trip, **so that** my family can manage its own participation without waiting for
the trip organizer.

### Acceptance Criteria

- **Given** I am a participant of a trip and belong to a travel party, **When** I open the trip
  participant management, **Then** I can add members or dependents from my own travel party to the trip.

- **Given** I add a member or dependent from my own travel party, **When** I confirm, **Then** the
  participant appears on the trip participant list.

- **Given** I try to add a person who is not part of my own travel party, **When** I confirm,
  **Then** the system rejects the action.

- **Given** a participant from my own travel party is already on the trip, **When** I remove them,
  **Then** they are removed from the trip and no longer count toward stay-period and cost allocation.

- **Given** I try to remove a participant from another travel party, **When** I confirm,
  **Then** the system rejects the action.

### Technical Notes

- Trips SCS
- Extends trip participant management permissions
- Requires clear party ownership check: actor party == participant party
- Publishes `ParticipantAddedToTrip` / `ParticipantRemovedFromTrip`

---

## Story S12-B: Maintain Stay Periods with Party Scope + Organizer Override

**Epic**: E-TRIPS-03
**Priority**: Must
**Size**: M
**As a** party member, **I want** to maintain the stay periods of participants from my own party,
**so that** our arrival and departure dates are always current. As an organizer, I want to edit
stay periods for all participants.

### Acceptance Criteria

- **Given** I belong to a travel party on the trip, **When** I edit arrival or departure dates for
  a participant of my own party, **Then** the stay period is saved.

- **Given** I try to edit the stay period of a participant from another party, **When** I save,
  **Then** the system rejects the action.

- **Given** I am a trip organizer, **When** I edit any participant's stay period, **Then** the
  stay period is saved regardless of party affiliation.

- **Given** a stay period lies outside the trip date range, **When** I save, **Then** I see a validation error.

- **Given** a stay period changes, **When** the update is stored, **Then** downstream cost
  allocation can be recomputed from the new dates.

### Technical Notes

- Trips SCS
- Existing `StayPeriodUpdated` event remains central
- Authorization now depends on either same-party membership or organizer role

---

## Story S12-C: Grant Additional Trip Organizers

**Epic**: New trip-governance slice
**Priority**: Should
**Size**: M
**As a** trip organizer, **I want** to grant organizer rights to another participant of the trip,
**so that** trip administration can be shared.

### Acceptance Criteria

- **Given** I am an organizer of the trip, **When** I select another participant and grant organizer rights,
  **Then** that participant becomes a trip organizer.

- **Given** a participant is not part of the trip, **When** I try to grant organizer rights,
  **Then** the system rejects the action.

- **Given** a participant is already an organizer, **When** I grant organizer rights again,
  **Then** the action is idempotent.

- **Given** a new organizer was granted, **When** they open the trip, **Then** they can perform
  organizer-only trip actions including cross-party stay-period management.

### Technical Notes

- Prefer modelling organizer membership in Trips first
- Avoid making IAM realm role synchronization a hard prerequisite

---

## Story S12-D: Open Shared Trip Account for Current Trip

**Epic**: Expense lifecycle refactoring
**Priority**: Must
**Size**: L
**As a** trip participant, **I want** the expense account of the current trip to exist before the
trip ends, **so that** costs can be recorded and viewed during planning and travel.

### Acceptance Criteria

- **Given** a trip exists, **When** the expense lifecycle is initialized for that trip, **Then** a
  shared trip account exists and is reachable from the trip while the trip is still active.

- **Given** the shared trip account exists but no costs were recorded yet, **When** I open it,
  **Then** I see an empty-state account view rather than "no settlement yet".

- **Given** I am a participant of the current trip, **When** I open the expense area,
  **Then** I can access the shared trip account before trip completion.

- **Given** the trip account is still open, **When** costs or receipts are added,
  **Then** the account is updated without requiring the trip to be completed first.

### Technical Notes

- Expense SCS
- Refactors the lifecycle away from `TripCompleted -> first visibility`
- May create/open account on `TripCreated` or `TripConfirmed` depending final design choice

---

## Story S12-E: Party Account View as Primary Expense Screen

**Epic**: Expense view redesign
**Priority**: Must
**Size**: M
**As a** participant, **I want** the primary expense screen to show my travel party account,
**so that** I understand my family's current balance in the shared trip account.

### Acceptance Criteria

- **Given** I open the trip expense area, **When** the shared trip account is shown,
  **Then** the primary view is a party-account view, not a transfer list between participants.

- **Given** I belong to a travel party, **When** I view the party account,
  **Then** I see at least:
  accommodation-related allocated shared cost, advance target, paid advance, receipt credits, and current balance.

- **Given** the party account has a positive result in favor of the party, **When** I view it,
  **Then** it is shown as credit/guthaben.

- **Given** the party account has a negative result or still open contribution, **When** I view it,
  **Then** it is shown as an open amount/offener Anteil.

- **Given** legacy transfer data exists, **When** I open the party account screen,
  **Then** participant-to-participant transfers are not the primary representation.

### Technical Notes

- Expense SCS
- Main read model: `PartyAccountView`
- Existing per-person settlement tables become secondary or are removed from primary flow

---

## Story S12-F: Allocate Accommodation Cost from Shared Pool

**Epic**: Expense allocation rules
**Priority**: Must
**Size**: L
**As a** participant, **I want** accommodation cost to be booked into the shared trip account and
then allocated fairly to party accounts based on weighted participants and stay periods, **so that**
the accommodation share reflects who was actually there.

### Acceptance Criteria

- **Given** accommodation total price is defined for the trip, **When** it is transferred into the
  expense domain, **Then** it is posted as a cost block to the shared trip account.

- **Given** participants have different stay periods and weights, **When** party shares are computed,
  **Then** the accommodation cost is allocated using the weighted overall participant model.

- **Given** a party has more weighted participant-nights than another party, **When** allocation is computed,
  **Then** that party receives a larger accommodation share.

- **Given** stay periods change, **When** the allocation is recomputed,
  **Then** the allocated shared cost per party is updated.

- **Given** a party account is displayed, **When** I inspect it,
  **Then** I see the allocated accommodation-related shared cost as part of the party balance.

### Technical Notes

- Expense SCS
- Shared pool first, party allocation second
- Formula candidate: weighted person-nights per party / weighted person-nights total

---

## Story S12-G: Equal Advance Payments per Party

**Epic**: Expense advance payment model
**Priority**: Should
**Size**: M
**As an** organizer, **I want** to define one equal round advance amount for every travel party,
**so that** prepayments remain simple and easy to communicate.

### Acceptance Criteria

- **Given** parties exist on the trip, **When** I open the advance-payment setup,
  **Then** I can define one amount that applies equally to all parties.

- **Given** the amount is confirmed, **When** party accounts are recalculated,
  **Then** each party gets the same advance target.

- **Given** a party has paid its advance, **When** the payment is marked as received,
  **Then** the party account shows the payment as positive credit.

- **Given** parties have different participant counts, weights, or stay periods, **When** advance
  targets are computed, **Then** the amount is still equal for all parties.

### Technical Notes

- Expense SCS
- Deliberately different from accommodation allocation rule
- Round-number communication is more important than strict fairness here

---

## Story S12-H: Shopping Receipts as Shared Pool Debit + Party Credit

**Epic**: Expense live operations
**Priority**: Must
**Size**: M
**As a** participant, **I want** shopping receipts to reduce the shared trip pool and credit the
performing travel party, **so that** grocery runs and similar purchases are reflected correctly.

### Acceptance Criteria

- **Given** I submit a shopping or similar shared-trip receipt, **When** it is booked,
  **Then** the amount is posted as a negative amount into the shared trip account.

- **Given** the receipt was paid by a member of my travel party, **When** it is booked,
  **Then** my travel party receives a positive credit on its party account.

- **Given** review is active, **When** a receipt is pending review,
  **Then** the UI can distinguish pending/open receipts from booked credits.

- **Given** the receipt is approved or considered booked, **When** I view the party account,
  **Then** the receipt credit is visible in the current balance.

- **Given** multiple receipts are submitted by different parties, **When** party balances are recomputed,
  **Then** each party only receives credit for its own receipts.

### Technical Notes

- Expense SCS
- Domain rule: one booking affects both the shared pool and the performing party account
- Review remains a lightweight four-eyes rule: `reviewer != submitter`

---

## Open Design Questions

1. Should the shared trip account open on `TripCreated` or on `TripConfirmed`?
2. Should shopping receipts produce provisional party credit immediately or only after approval?
3. Is trip-organizer membership purely local to Trips, or should it also be synchronized outward?
4. Should the party account screen also include a secondary "my personal detail" section, or only the party view?
5. Should removing a participant from a trip be blocked once cost-relevant receipts already exist?

---

## Recommendation

The iteration should be treated as a domain shift, not just an expense UI enhancement. The key
deliverable is a coherent model where trip parties can manage their own participation, the shared
trip account exists during the trip, and party balances become the main accounting language.
