# Travelmate 0.12.4 — Release Notes
**Date:** 2026-03-27

## Highlights
- Hardened trip invitations so they are party-aware, reject duplicate target parties, and resolve existing travel parties before creating a new invite.
- Added lifecycle-safe participant removal with published `ParticipantRemovedFromTrip`, IAM deletion guards, and shopping-list cleanup when operational references remain.
- Propagated travel-party renames and participant updates consistently across Trips, Expense, and IAM projections, including real UI coverage for name updates and deletion flows.
- Automated the full e2e / BDD suite (Playwright + Cucumber) covering invitations, participant deletion, recipe/shopping/expense flows, and navigation to keep the new guardrails verified.

## Testing
- `./mvnw -Pe2e -pl travelmate-e2e test-compile failsafe:integration-test failsafe:verify -Dfailsafe.failIfNoSpecifiedTests=false`
- `./mvnw -DskipTests package`
