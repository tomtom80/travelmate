---
name: EventStorming Iteration 14
description: Iteration 14 scope — DatePoll + AccommodationPoll aggregates for collaborative trip planning (E-TRIPS-08, US-TRIPS-080 to 084). Two separate aggregates, BC-internal events only, polls only in PLANNING status.
type: project
---

## EventStorming: Iteration 14 -- Collaborative Trip Planning (2026-03-29)

**Key Design Decisions**:
- Two separate aggregates (DatePoll, AccommodationPoll) instead of generic Poll — voting modes differ (multi-select vs single-vote), candidate types differ, result actions differ
- BC-internal events only — no new Cross-SCS event contracts in travelmate-common
- Polls only allowed in Trip PLANNING status; Trip.confirm() requires all polls closed
- Parallel polls allowed (date and accommodation can overlap)
- Voting right per Account (not Dependents), validated in Application Service
- URL-Import becomes poll-aware: creates AccommodationCandidate when poll is open
- Trip.dateRange stays mandatory; DatePoll overwrites initial suggestion on confirmation
- StayPeriods reset when dateRange changes via DatePollConfirmed

**New Aggregates**:
- DatePoll (domain/datepoll/): DatePollId, PollStatus(OPEN/CONFIRMED/CANCELLED), List<DateOption>, List<DateVote>, confirmedOptionId
- AccommodationPoll (domain/accommodationpoll/): AccommodationPollId, PollStatus(OPEN/DECIDED/CANCELLED), List<AccommodationCandidate>, List<AccommodationVote>, selectedCandidateId

**Stories**: S14-A (DatePoll create) -> S14-B (DatePoll voting) -> S14-C (Confirm + Trip link) -> S14-D (AccommodationPoll create) -> S14-E (AccommodationPoll voting + select) -> S14-F (UI integration)

**Hot Spots**:
- HS-1 (HIGH): Trip.dateRange update mechanism — pragmatic: keep as mandatory, overwrite on confirm
- HS-2 (MEDIUM): StayPeriod invalidation on date range change
- HS-4 (MEDIUM): URL-Import + AccommodationCandidate interaction
- HS-7 (MEDIUM): Trip lifecycle coupling (polls must close before confirm)

**ADR candidates**: ADR-0019 (separate poll aggregates), ADR-0020 (Trip.dateRange handling)

**Why:** E-TRIPS-08 is the next domain bottleneck after party self-management (Iter 12) and invitation hardening (Iter 13). Date/accommodation discovery happens before booking.

**How to apply:** Full document at docs/design/eventstorming/iteration-14-scope.md. S14-A and S14-D can start in parallel.
