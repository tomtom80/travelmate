# Iteration 20 — Onboarding and Auditability

**Target Version**: `v0.20.0`  
**Status**: PLANNED

## Goal

Iteration 20 completes key onboarding gaps and introduces auditability that later security and compliance work can rely on.

## Planned Scope

### Functional stories

- external invitation flow completed as a first-class onboarding channel
- password reset via Keycloak enabled and integrated into the user journey

### Non-functional stories

- audit logging for security-relevant and business-critical actions
- explicit documentation updates for security-relevant code/documentation mismatches
- release and operations docs aligned to the new onboarding and audit flows

## Planned Deliverables

- no production-significant onboarding dead end remains in invitation and account recovery flows
- audit events exist for critical actions such as organizer changes and destructive operations
- architectural documentation explains the real current behavior instead of historic assumptions

## Acceptance

- external invitation works for the main user journey and fails safely on invalid or expired paths
- password reset is demonstrable in a realistic environment
- audit events are emitted and documented for agreed critical paths
