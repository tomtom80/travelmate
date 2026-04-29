# Roadmap to v1.0.0

**Stand**: 2026-04-27  
**Source baseline**: `v0.18.0` released, repository on `0.19.0-SNAPSHOT`

## Strategic Direction

Travelmate is no longer blocked by missing core domain features. The dominant path to `v1.0.0` is now platform maturity.

This roadmap adopts the consolidated findings from the high-level major-release planning in `reports/continue-with-current-plan-keen-flamingo.md` and promotes them into the canonical backlog.

### Planning Principles

- roughly `60%` non-functional work, `40%` functional work
- every iteration must improve either go-live safety or business completeness
- no major release without verified rollback, backup, observability, and tenant-isolation safeguards
- DDD, SCS, tenant scoping, and asynchronous eventing remain non-negotiable constraints

## Critical Path

1. visibility and diagnosis
2. event integrity and auditability
3. production hardening
4. compliance and pre-release verification
5. major release

## Iteration Overview

| Iteration | Version | Theme | Main Deliverables |
|-----------|---------|-------|-------------------|
| 19 | `v0.19.0` | Visibility and Integrity | Observability, Outbox, Recipe Import, Recipe CRUD, doc corrections |
| 20 | `v0.20.0` | Onboarding and Auditability | External invitation flow completion, password reset, audit logging |
| 21 | `v0.21.0` | Production Hardening I | CSRF, headers, secrets, TLS, backups, multi-stage CI/CD |
| 22 | `v0.22.0` | Production Hardening II | Keycloak hardening, rate limiting, dependency hygiene, automated update flow |
| 23 | `v0.23.0` | Compliance and UX Quality | GDPR paths, accessibility, Lighthouse CI, privacy artefacts |
| 24 | `v0.24.0` | Pre-Release Security and Conformance | tenant-isolation tests, pact/event contracts, pentest, JWT identity hardening |
| 25 | `v1.0.0` | Release Iteration | lasttest baseline, rollback drill, final regression, release sign-off |

## Cross-Cutting Go-Live Gates

### Functional

- complete required v1 stories for onboarding, planning, and settlement
- no dead-end user journeys in sign-up, trip invitation, planning, or completion flows

### Security

- CSRF active again for state-changing browser flows
- production secrets managed without dangerous defaults
- security headers and TLS termination documented and implemented
- audit logging for critical actions
- dependency scanning and container scanning in CI
- tenant isolation verified by dedicated negative-path tests

### Operations

- centralized metrics, logs, and traces
- transactional outbox or equivalent event-integrity protection
- tested backup and restore
- tested rollback by image tag
- documented runbooks for common failure scenarios

### Quality

- full build green
- full E2E green repeatedly without transient failures
- contract tests for priority events
- baseline load test and release smoke tests

## Linked Iteration Plans

- [`iteration-19-plan.md`](./iteration-19-plan.md)
- [`iteration-20-plan.md`](./iteration-20-plan.md)
- [`iteration-21-plan.md`](./iteration-21-plan.md)
- [`iteration-22-plan.md`](./iteration-22-plan.md)
- [`iteration-23-plan.md`](./iteration-23-plan.md)
- [`iteration-24-plan.md`](./iteration-24-plan.md)
- [`iteration-25-plan.md`](./iteration-25-plan.md)
