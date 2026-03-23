---
name: EventStorming Iteration 9 (Revised)
description: Iteration 9 revised scope — PartyWeighting refactoring (CRITICAL), Accommodation as own Aggregate, Advance Payment with Party-level calculation, Resubmit UI, PWA
type: project
---

## EventStorming: Iteration 9 Scope — REVISED (2026-03-18)

**Revision reason**: Critical domain knowledge — settlement is per TravelParty (not individual), Accommodation is central planning element with room structure.

**Recommended Stories (v0.9.0)**:
1. S9-D: Resubmit Rejected Receipt UI (S) — domain done, only controller+template
2. S9-A: PartyWeighting Refactoring (L) — CRITICAL: replace per-person with per-party settlement
3. S9-B: Accommodation Basis (M) — own Aggregate with Rooms, AccommodationPriceSet event
4. S9-C: Advance Payment Tracking (M) — Receipt flag + AdvancePaymentCalculation domain service
5. S9-E: PWA Manifest & Install (S) — Gateway manifest.json + minimal Service Worker

**Key Design Decisions (REVISED from initial analysis)**:
- Accommodation as OWN AGGREGATE (revised from Entity-in-Trip) — room structure, assignments, standalone lifecycle
- PartyWeighting replaces ParticipantWeighting — settlement on TravelParty level
- calculateBalances() returns Map<partyId, balance> not Map<participantId, balance>
- SettlementPlan.Transfer is party-to-party
- ParticipantJoinedTrip event extended with participantTenantId field
- New event: AccommodationPriceSet (Trips -> Expense)
- Advance Payment: Receipt with boolean flag + AdvancePaymentCalculation domain service

**Hot Spots**:
- HS-1: Per-Person vs Per-Party Settlement (CRITICAL, decided: Party-level, Option C Hybrid)
- HS-2: Accommodation scope much larger than initially analyzed (decided: own Aggregate, MVP rooms+price)
- HS-3: Advance Payment depends on Party weights + Accommodation price (cross-SCS via AccommodationPriceSet event)
- HS-4: ParticipantJoinedTrip needs participantTenantId for party grouping in Expense
- HS-5: PWA Service Worker scope (decided: minimal, App Shell only)

**Cross-SCS Events**:
- ParticipantJoinedTrip EXTENDED (+participantTenantId)
- AccommodationPriceSet NEW (Trips -> Expense)

**Deferred to Iteration 10**: Category Breakdown, Room Assignments, Amenities, URL Import
**Deferred to Iteration 11+**: Accommodation Poll, Custom Splitting, Lighthouse CI, Bring, PDF Export

**ADR candidates**: ADR-0016 (Accommodation as Aggregate), ADR-0017 (PartyWeighting), ADR-0018 (Advance Payment)

**Why:** The existing per-individual settlement model is fundamentally wrong for the domain. This must be fixed BEFORE adding features that depend on correct party-level cost distribution.

**How to apply:** S9-A (PartyWeighting) is the critical path. All subsequent features (Advance Payment, Category Breakdown) depend on correct party-level settlement. Expect breaking changes to E2E tests.
