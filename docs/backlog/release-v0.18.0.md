# Release v0.18.0

**Release Date**: 2026-04-26  
**Release Type**: Minor Release  
**Based On**: `v0.17.0`

## Scope

`v0.18.0` is a consolidation release around trip governance, test reliability, and safer runtime behavior.

- trip-local multi-organizer support was added with explicit grant and revoke flows
- organizer demotion is guarded so a trip cannot lose its last organizer
- dedicated organizer role events and routing keys were introduced for downstream processing
- the E2E suite was stabilized and returned to a clean green state
- the IAM admin cleanup endpoint was fenced off from production profile

## Verification

- Trips module tests passed for the organizer lifecycle changes
- end-to-end verification completed with `289/289` green scenarios in the release iteration

## Included Work Since v0.17.0

- `feat: trip-local multi-organizer support with aggregate guards`
- `feat: organizer role granted and revoked domain events`
- `test: stabilize Playwright dialog handling and HTMX settle waits`
- `fix: protect IAM admin cleanup endpoint from production exposure`
- `docs: add ADR-0024, ADR-0025 and ADR-0026`

## Notes

The repository has already moved on to `0.19.0-SNAPSHOT`. Current work after `v0.18.0` is focused on CI and delivery hardening, including reactor-safe test dependency resolution and documentation updates.
