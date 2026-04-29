# Iteration 19 — Visibility and Integrity

**Target Version**: `v0.19.0`  
**Status**: PLANNED

## Goal

Iteration 19 establishes the operational baseline for all later go-live work. The focus is to make the platform observable, reduce silent event-loss risk, and close the remaining obvious recipe-management gap.

## Planned Scope

### Functional stories

- recipe edit flow finalized and verified end to end
- recipe delete flow finalized and verified end to end
- recipe import from URL with SSRF-aware adapter behavior

### Non-functional stories

- observability baseline with Micrometer and Prometheus-compatible endpoints
- centralized roadmap decision for logs, metrics, and traces
- transactional outbox design and first implementation slice
- event versioning and naming conventions made executable through tests
- documentation drift corrections where roadmap findings contradict older assumptions

## Planned Deliverables

- recipe CRUD considered functionally complete for v1 baseline
- first production-grade observability slice in place
- reduced risk of silent post-commit event loss
- updated canonical docs for roadmap and delivery planning

## Acceptance

- recipe import covers valid URL, no-data-found, and invalid URL paths
- metrics endpoints and scrape strategy are documented and buildable
- at least one SCS has working outbox mechanics or a committed implementation path validated in code
- roadmap-driven DDD and security documentation no longer contradict the current codebase knowingly
