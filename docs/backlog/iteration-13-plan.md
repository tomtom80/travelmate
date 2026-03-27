# Iteration 13 — Delivery Plan: Invitation Hardening, Lifecycle-Safe Removal, and Projection Consistency

**Date**: 2026-03-27
**Target Version**: unreleased
**Input**: collaborative invitation/deletion refinement after `v0.12.3`

**Status**: IN PROGRESS

## Planning Goal

Iteration 13 hardens the collaboration boundaries that are now critical for real family-and-friends trip planning:

- trip invitations become party-aware instead of member-centric
- invited parties can grow later and add new members/dependents to a joined trip without duplicates
- participant removal becomes lifecycle-safe and event-driven
- cross-SCS projections must stay current for renamed travel parties and participant changes
- travel-party member invitation onboarding remains a separate IAM invitation flow with password setup and login verification

## Key Changes

### 1. Party-centric trip invitation model

- keep trip invitations inside Trips
- enforce one active invitation per `trip x target travel party`
- resolve an already registered target party from invited member email
- store invited representative member plus `targetPartyTenantId`
- reject duplicate invites for another member of the same party on the same trip
- reject invite when that party already participates in the trip
- keep external invitation flow email-bound until registration links it to a real party
- invited account holder joins first; later same-party participants are added explicitly, not automatically

### 2. Late party growth after joining

- once Party 2 has joined, later-created members and dependents appear in Trips via party projections
- same-party account holders can add those people to the trip exactly once
- duplicate participant adds are rejected cleanly at trip level

### 3. Participant removal and deletion rules

- `ParticipantRemovedFromTrip` is a real published event
- participant removal from a `COMPLETED` trip is forbidden
- active operational references such as open shopping assignments are cleared automatically
- financial and executed history is preserved rather than cascaded away
- IAM blocks member/dependent deletion while the person still participates in any active trip
- normal end-user trip hard delete stays out of scope; `cancel` is the safe user path

### 4. Cross-SCS event consistency

- Expense consumes `TenantRenamed`
- Expense consumes participant-removal events
- read models update party names by `partyTenantId`
- UI and reports must render current party names from projections instead of stale copied strings
- downstream cleanup and display rules are driven by domain events, not browser-side repair logic

### 5. Travel-party invitation onboarding

- inviting a member into a travel party is treated as a distinct IAM invitation flow, not as a trip invitation
- the invite email must lead to onboarding where the invited member sets a password
- after onboarding, the invited member can log in independently
- this flow must not auto-join any trip; later trip invitations are handled separately in Trips

## Verification Focus

- domain and application tests for duplicate-party invitation prevention
- tests for late-added same-party members/dependents joining trips without duplicates
- tests for participant-removal event publication and completed-trip guardrails
- tests for IAM deletion blocking while trip participation exists
- tests for `TenantRenamed` propagation into Expense projections
- Mailpit-backed BDD/E2E for:
  - registered-party trip invitations
  - external-party trip invitations
  - travel-party member invitation onboarding with password setup and login

## E2E Regression Coverage

- Playwright suites now verify the full deletion lifecycle (invite → trip add → blocked IAM deletion → removal → cleanup → IAM deletion allowed) via `ParticipantDeletionLifecycleIT`.
- Invitation lifecycle (`InvitationLifecycleIT`) and rename-projection regression (`TripConsistencyIT`) continue to pass after the IAM rebuild that surfaced deletion errors through localized toasts.

## Delivered Code Slice

- common event/routing support for `ParticipantRemovedFromTrip`
- party-aware invitation persistence and service invariants in Trips
- shopping-list assignment cleanup on participant removal
- participant-removal publication from Trips
- IAM trip-participation projection for deletion prechecks
- Expense consumers and projection updates for participant removal and `TenantRenamed`

## Follow-up Risks

- full end-to-end automation still depends on stable multi-browser and Mailpit infrastructure
- historical financial references currently stay preserved by keeping participants in Expense once receipts exist; if stronger former-participant labelling is needed, add an explicit status/snapshot model next
