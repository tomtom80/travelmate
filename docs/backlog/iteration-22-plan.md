# Iteration 22 — Production Hardening II

**Target Version**: `v0.22.0`  
**Status**: PLANNED

## Goal

Iteration 22 continues the production track by addressing authentication hardening, abuse protection, and dependency hygiene.

## Planned Scope

- rate limiting for critical anonymous and authenticated routes
- Keycloak hardening for brute-force protection, sessions, and password policy
- dependency and container vulnerability scanning in CI
- automated update workflow for supported dependency streams
- follow-up platform fixes from iteration 21 findings

## Planned Deliverables

- reduced abuse and brute-force surface
- better dependency freshness and CVE visibility
- clearer production-readiness bar for authn/authz and platform maintenance

## Acceptance

- security hardening settings are documented and reproducible
- CI fails on agreed critical dependency thresholds
- operational responsibility for auth hardening is no longer implicit
