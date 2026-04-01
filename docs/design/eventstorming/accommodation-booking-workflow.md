# EventStorming: Accommodation Poll Booking Workflow

**Date**: 2026-03-31
**Level**: Design-Level (single aggregate)
**Aggregate**: AccommodationPoll (Trips BC)
**ADR**: ADR-0022

## Domain Events (chronological, happy path)

1. AccommodationPollCreated
2. AccommodationCandidateAdded (x N)
3. AccommodationVoteCast (x N)
4. AccommodationPollConfirmed (winner selected, booking phase starts)
5. BookingAttemptStarted (PENDING attempt for selected candidate)
6. BookingSucceeded (organizer records external booking confirmation)
7. AccommodationCreated (Accommodation aggregate materialised)

## Domain Events (fallback path)

1. ... (steps 1-5 as above)
2. BookingFailed (organizer records unavailability)
3. BookingAttemptStarted (auto-fallback to next candidate by vote rank)
4. BookingSucceeded OR BookingFailed
5. ... (repeat until success or exhaustion)

## Domain Events (reopened path)

1. ... (all candidates exhausted)
2. AccommodationPollReopened (status -> REOPENED)
3. AccommodationCandidateAdded (new candidates)
4. AccommodationVoteCast (re-voting on all candidates)
5. AccommodationPollConfirmed (new winner)
6. BookingAttemptStarted
7. ...

## Command-Event Mapping

| Command | Actor | Aggregate | Pre-condition | Event(s) | Policy |
|---------|-------|-----------|---------------|----------|--------|
| CreateAccommodationPoll | Organizer | AccommodationPoll | Trip PLANNING, no open poll | AccommodationPollCreated | -- |
| AddAccommodationCandidate | Organizer/Participant | AccommodationPoll | OPEN or REOPENED | AccommodationCandidateAdded | -- |
| CastAccommodationVote | Participant (Account) | AccommodationPoll | OPEN or REOPENED | AccommodationVoteCast | -- |
| ConfirmAccommodationPoll | Organizer | AccommodationPoll | OPEN or REOPENED, candidate exists | AccommodationPollConfirmed + BookingAttemptStarted | -- |
| RecordBookingSuccess | Organizer | AccommodationPoll | BOOKING, PENDING attempt | BookingSucceeded | Create Accommodation aggregate |
| RecordBookingFailure | Organizer | AccommodationPoll | BOOKING, PENDING attempt | BookingFailed + (BookingAttemptStarted OR AccommodationPollReopened) | Auto-fallback or reopen |
| CancelAccommodationPoll | Organizer | AccommodationPoll | OPEN, BOOKING, or REOPENED | AccommodationPollCancelled | -- |

## Status Machine

```
OPEN ──confirm()──> BOOKING ──recordSuccess()──> BOOKED
  │                    │                            │
  │ cancel()           │ recordFailure()            └──> Accommodation.create()
  │                    │   (next candidate exists)
  ▼                    ▼
CANCELLED          BOOKING (auto-selected next candidate)
                       │
                       │ recordFailure()
                       │   (all exhausted)
                       ▼
                   REOPENED ──confirm()──> BOOKING
                       │                      │
                       │ cancel()             │ ... (same cycle)
                       ▼                      ▼
                   CANCELLED              BOOKED / REOPENED
```

## Invariants

1. Only one PENDING BookingAttempt at a time per poll
2. Fallback candidate selection: highest vote count among untried candidates
3. REOPENED allows voting and candidate addition (same rules as OPEN)
4. BOOKED is terminal -- no further state changes
5. CANCELLED is terminal from OPEN, BOOKING, or REOPENED
6. Accommodation aggregate is created ONLY on BOOKED (not on BOOKING)

## Hot Spots

- **HS-1 (LOW)**: BookingAttempt notes length -- 2000 chars sufficient? Could contain booking reference numbers, price quotes, correspondence summaries.
- **HS-2 (MEDIUM)**: Should REOPENED clear existing votes or keep them? **Decision**: Keep existing votes. New candidates start at 0 votes. Voters can change votes to new candidates if desired.
- **HS-3 (LOW)**: Notification when booking fails and fallback starts? Currently no notification system for poll events (BC-internal only). Could add email notification later via policy.
- **HS-4 (MEDIUM)**: Can organizer manually pick a candidate to try in BOOKING (skip vote ranking)? **Decision**: No. Confirm always starts with the selected candidateId. Auto-fallback follows vote ranking. This keeps the aggregate simple and the process fair.

## Aggregate Design (BookingAttempt entity)

```
BookingAttempt (entity within AccommodationPoll)
├── bookingAttemptId: BookingAttemptId (UUID)
├── candidateId: AccommodationCandidateId
├── outcome: BookingOutcome {PENDING, SUCCEEDED, FAILED}
├── notes: String (nullable, max 2000)
└── attemptedAt: LocalDateTime

Invariants:
- Only PENDING can transition to SUCCEEDED or FAILED
- SUCCEEDED/FAILED are terminal for an individual attempt
- At most one PENDING attempt per poll at any time
```
