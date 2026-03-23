# Release v0.12.0

**Release Date**: 2026-03-23
**Release Type**: Iteration 12

## Scope

`v0.12.0` delivers the shift from post-trip settlement to a live, party-centric trip account.

- party members can manage their own trip participants
- stay periods are maintained per participant with organizer override
- additional trip organizers can be granted to account-bound participants
- expense is visible during the current trip
- `PartyAccount` is the main accounting view
- accommodation allocation uses `stay period x weighting`
- weighting defaults follow age bands and remain organizer-overridable
- advance payments and receipt credits are integrated into the same running account
- participant-table actions and trip feature cards were hardened for desktop and mobile use

## Verification

- compose stack rebuilt and started with `docker compose up --build -d`
- full E2E and BDD verification passed with `./mvnw -Pe2e -pl travelmate-e2e clean verify -DskipTests=false`
- final suite result: `Tests run: 222, Failures: 0, Errors: 0, Skipped: 32`

## Known Follow-Up

- Mockito-based unit tests in some modules still need an environment-level cleanup for the JDK/inline-mock setup
- the next product step after this release is collaborative trip initialization:
  date poll, accommodation voting, and kitchen-duty planning
