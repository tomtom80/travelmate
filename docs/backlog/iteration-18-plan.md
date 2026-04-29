# Iteration 18 — Delivery Summary

**Date**: 2026-04-26  
**Target Version**: `v0.18.0`  
**Status**: DONE

## Scope

Iteration 18 consolidated operationally sensitive parts of the current product without opening a new large feature branch. The delivered work focused on trip-local organizer governance, E2E stability, and a production safety guard for the IAM admin endpoint.

## Delivered

### 1. Trip-local multi-organizer support

- additional organizers can be granted on a per-trip basis
- organizer rights can be revoked again
- the aggregate enforces that at least one organizer always remains
- organizer changes publish dedicated domain events for downstream processing

Relevant references:

- [`../adr/0024-organizer-rolle-trip-lokal.md`](../adr/0024-organizer-rolle-trip-lokal.md)
- [`../adr/0025-event-versionierung-und-naming-konvention.md`](../adr/0025-event-versionierung-und-naming-konvention.md)

### 2. E2E stabilization

- Playwright dialog handling was centralized
- HTMX settle waiting was tightened in the shared test base
- navigation waits were hardened to remove known race conditions

Observed result in the delivered iteration:

- `289/289` E2E tests green

### 3. IAM admin hardening

- the IAM `AdminController` is no longer available in production profile
- this keeps destructive tenant-cleanup helpers out of production deployments

## Architectural follow-ups

- ADR-0024 was accepted in this iteration
- ADR-0025 and ADR-0026 were added as proposed follow-ups for event versioning and exception-handler consolidation

## Verification

- Trips unit and integration suite passed during the iteration
- the complete E2E suite passed without the previously known flakes
