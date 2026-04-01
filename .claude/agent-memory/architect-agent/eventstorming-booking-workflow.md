---
name: EventStorming Booking Workflow
description: Design-level EventStorming for AccommodationPoll booking workflow extension — OPEN->BOOKING->BOOKED cycle, BookingAttempt entity, auto-fallback by vote rank, REOPENED status for exhausted candidates.
type: project
---

## Booking Workflow Extension (2026-03-31)

AccommodationPoll aggregate extended (not new aggregate) per ADR-0022. Key changes:
- Status machine: OPEN, BOOKING, BOOKED, REOPENED, CANCELLED (replaces old OPEN/CONFIRMED/CANCELLED)
- BookingAttempt entity: tracks external booking attempts per candidate
- Auto-fallback: on failure, next untried candidate by vote rank
- REOPENED: all candidates exhausted, allows new candidates + re-voting
- Accommodation.create() triggers on BOOKED (not on confirm)
- Flyway V20: booking_attempt table + booked_candidate_id column + CONFIRMED->BOOKED migration
- No new cross-SCS events; BC-internal only

**Why:** Organizer books externally (phone/email/platform). First candidate may be unavailable. System needs to track attempts and support fallback without losing vote history.

**How to apply:** Full EventStorming at docs/design/eventstorming/accommodation-booking-workflow.md. ADR at docs/adr/0022-booking-workflow-im-accommodationpoll-aggregat.md.
