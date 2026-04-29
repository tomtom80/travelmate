# Iteration 24 — Pre-Release Security and Conformance

**Target Version**: `v0.24.0`  
**Status**: PLANNED

## Goal

Iteration 24 is the pre-release proof iteration. It validates that the designed safeguards really hold under test and review.

## Planned Scope

- tenant-isolation negative-path test slices across critical endpoints
- event contract tests for priority publisher/consumer pairs
- JWT identity hardening away from brittle assumptions where required
- external pentest execution and remediation of critical findings
- go-live risk review against architecture, security, and QA gates

## Planned Deliverables

- confidence that the platform enforces its central trust assumptions
- contractual protection against breaking event evolution
- pre-release evidence for security sign-off

## Acceptance

- no unresolved critical pentest findings
- tenant-isolation tests cover the agreed critical surface
- event contracts fail the build when broken
