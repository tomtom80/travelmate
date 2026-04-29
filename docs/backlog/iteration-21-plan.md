# Iteration 21 — Production Hardening I

**Target Version**: `v0.21.0`  
**Status**: PLANNED

## Goal

Iteration 21 moves Travelmate from demo-grade delivery toward first production-grade operating discipline.

## Planned Scope

### Functional stories

- receipt editing and related expense-flow polish where still required for v1 readiness

### Non-functional stories

- CSRF strategy corrected for browser-based state changes
- security headers defined and enforced
- production secrets strategy introduced
- TLS termination and deployment topology documented and implemented for the chosen target
- backup and restore path documented and practically verifiable
- CI/CD extended beyond basic verify and demo deploy toward environment promotion

## Planned Deliverables

- first real production hardening bundle across application and operations
- reduced exposure from browser and deployment misconfiguration risks
- explicit path for recovery from failed deployments or lost data

## Acceptance

- state-changing browser requests are protected consistently
- no dangerous secret defaults remain acceptable for production operation
- rollback and restore procedures exist and are testable
- CI/CD architecture is documented beyond the current single demo path
