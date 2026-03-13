# Iteration 5 Plan: User Management & Communication Architecture Overhaul

## Context

The current user management flows suffer from multiple UX problems caused by delegating user-facing
flows to Keycloak's `executeActionsEmail()` mechanism. Users get stuck in Keycloak screens, can't set
passwords, see session conflicts, and have no way back to the application. Additionally, the external
invitation creates accounts in the wrong tenant, own travel party members aren't auto-joined to trips,
error handling is ad-hoc, and the landing page doesn't detect auth state.

This iteration addresses all 5 priorities as a cohesive architectural improvement.

## Implementation Order

```
P5 (Landing page auth) → P4 (Error handling) → P3 (Auto-join) → P1 (Custom registration) → P2 (External tenant)
```

Rationale: P5 is trivial. P4 establishes error handling infrastructure used by all subsequent work.
P3 is isolated to Trips. P1 is the core change. P2 builds on P1's InvitationToken infrastructure.

---

## ADR-0013: Custom Registration Flow (replaces Keycloak executeActionsEmail)

Covers P1, P2, and the Keycloak interaction model change. Written before implementation begins.

---

## P5: Landing Page Auth-State Detection

**Scope:** IAM SCS only. ~30 min.

### Design
- `HomeController` already redirects authenticated users to `/dashboard`
- For defense-in-depth: pass `isAuthenticated` boolean to model
- `landing.html` conditionally renders nav buttons via `th:if`

### TDD Steps

| Step | Layer | Test | Implementation |
|------|-------|------|---------------|
| 1 | Controller | NEW `HomeControllerTest`: GET `/` without JWT → model has `isAuthenticated=false` | Modify `HomeController.home()`: add `Model` param, `model.addAttribute("isAuthenticated", jwt != null)` |
| 2 | Template | Manual verification | Modify `landing.html`: `th:if="${isAuthenticated}"` on nav/CTA buttons |

### Files
- MODIFY: `travelmate-iam/.../adapters/web/HomeController.java` — add Model param + isAuthenticated attribute
- MODIFY: `travelmate-iam/.../resources/templates/landing.html` — conditional nav rendering
- NEW: `travelmate-iam/src/test/.../adapters/web/HomeControllerTest.java`

### BDD Scenario
```gherkin
Feature: Landing Page Auth Detection
  Scenario: Unauthenticated user sees login and register buttons
    Given I am not logged in
    When I visit the landing page
    Then I see "Registrieren" and "Anmelden" buttons
    And I do not see "Dashboard" or "Logout" buttons

  Scenario: Authenticated user sees dashboard and logout buttons
    Given I am logged in
    When I visit the landing page
    Then I see "Dashboard" and "Logout" buttons
    And I do not see "Registrieren" or "Anmelden" buttons
```

---

## P4: Global Error Handling + ProblemDetail

**Scope:** travelmate-common + IAM + Trips. ~2-3 hours.

### Design
- Custom domain exceptions in `travelmate-common/domain/`: `EntityNotFoundException`, `DuplicateEntityException`, `BusinessRuleViolationException`
- `@ControllerAdvice` (named `GlobalExceptionHandler`) in each SCS's `adapters/web/`
- For HTMX requests (`HX-Request` header present): return error toast/fragment
- For browser requests: return Thymeleaf error page
- Enable `spring.mvc.problemdetails.enabled=true`
- Error templates: `error/404.html`, `error/403.html`, `error/error.html` (generic)
- HTMX error fragment: `fragments/error.html :: errorToast`

### TDD Steps

| Step | Layer | Test | Implementation |
|------|-------|------|---------------|
| 1 | Domain | NEW `EntityNotFoundExceptionTest` in common | 3 exception classes in `common/domain/` |
| 2 | Controller | NEW `GlobalExceptionHandlerTest` (IAM) — throw `EntityNotFoundException` → expect 404 | `GlobalExceptionHandler` in IAM `adapters/web/` |
| 3 | Template | — | Error templates in IAM |
| 4 | Refactor | Update existing tests (exception types change) | Replace `IllegalArgumentException` with typed exceptions in IAM services |
| 5 | Controller | NEW `GlobalExceptionHandlerTest` (Trips) | `GlobalExceptionHandler` in Trips `adapters/web/` |
| 6 | Refactor | Update existing Trips tests | Replace exceptions in Trips services |

### Exception Mapping

| Exception | HTTP Status | Use Case |
|-----------|------------|----------|
| `EntityNotFoundException(entityType, id)` | 404 | Account/Trip/Invitation not found |
| `DuplicateEntityException(messageKey)` | 409 | Duplicate member, duplicate invitation |
| `BusinessRuleViolationException(messageKey)` | 422 | Last member deletion, status transition |
| `IllegalArgumentException` (validation) | 400 | Value object validation failures |
| `IllegalStateException` | 409 | Invalid state transitions |
| `AccessDeniedException` (Spring Security) | 403 | Unauthorized access |

### Files
- NEW: `travelmate-common/.../domain/EntityNotFoundException.java`
- NEW: `travelmate-common/.../domain/DuplicateEntityException.java`
- NEW: `travelmate-common/.../domain/BusinessRuleViolationException.java`
- NEW: `travelmate-iam/.../adapters/web/GlobalExceptionHandler.java`
- NEW: `travelmate-trips/.../adapters/web/GlobalExceptionHandler.java`
- NEW: Error templates in both SCS (`error/404.html`, `error/403.html`, `error/error.html`, `fragments/error.html`)
- MODIFY: `AccountService.java`, `SignUpService.java`, `TenantService.java` — use typed exceptions
- MODIFY: `TripService.java`, `InvitationService.java`, `TripController.java` — use typed exceptions
- MODIFY: `DashboardController.java` — remove inline try/catch, let GlobalExceptionHandler handle
- MODIFY: IAM + Trips `application.yml` — add `spring.mvc.problemdetails.enabled=true`
- MODIFY: i18n files — add error page message keys

### BDD Scenario
```gherkin
Feature: User-Friendly Error Handling
  Scenario: User sees friendly 404 page
    Given I am logged in
    When I navigate to a non-existent trip
    Then I see a user-friendly "not found" error page
    And the page shows a link back to the dashboard

  Scenario: HTMX request shows inline error
    Given I am on the dashboard
    When I try to invite an already-existing member via HTMX
    Then I see an inline error message "Mitglied existiert bereits"
    And the page does not reload
```

---

## P3: Auto-Join Own Travel Party on Trip Creation

**Scope:** Trips SCS only. ~1-2 hours.

### Design
- Modify `Trip.plan()` to accept `List<UUID> participantIds` (all members + dependents)
- `TripService.createTrip()` collects ALL member IDs + dependent IDs from TravelParty
- The organizer is always the first participant (and the organizerId)
- No invitation needed for own Reisepartei members — they are direct participants
- The "invite" section on trip detail should only show for inviting OTHER Reiseparteis (external)
- `invitableMembers` list becomes empty since all own members are already participants

### TDD Steps

| Step | Layer | Test | Implementation |
|------|-------|------|---------------|
| 1 | Domain | MODIFY `TripTest`: `planAddsAllProvidedParticipants` | Modify `Trip.plan()` signature to accept `List<UUID> participantIds` |
| 2 | Application | MODIFY `TripServiceTest`: `createTripAddsAllTravelPartyMembers` | Modify `TripService.createTrip()` to collect member+dependent IDs |
| 3 | Controller | MODIFY `TripControllerTest`: verify `invitableMembers` is empty for own party | — (behavior follows from service change) |

### Files
- MODIFY: `travelmate-trips/.../domain/trip/Trip.java` — `plan()` accepts `List<UUID> participantIds`
- MODIFY: `travelmate-trips/.../application/TripService.java` — collect all party member/dependent IDs
- MODIFY: existing tests

### BDD Scenario
```gherkin
Feature: Auto-Join Travel Party on Trip Creation
  Scenario: All travel party members auto-join trip
    Given I have a travel party with 2 members and 1 companion
    When I create a new trip
    Then all 2 members and 1 companion are automatically added as participants
    And the invitable members list is empty

  Scenario: External users still require invitation
    Given I created a trip with my travel party auto-joined
    When I want to invite someone outside my travel party
    Then I can use the external invitation form
```

---

## P1: Custom Invited-User Registration Page

**Scope:** IAM SCS (primary) + Gateway config. ~4-6 hours. This is the core change.

### Design: Replace `executeActionsEmail` with Custom Token-Based Registration

**New domain concept:** `InvitationToken` — a time-limited, single-use token for registration completion.

**Revised invitation flow:**
```
1. AccountService.inviteMember() creates Keycloak user (random temp password, NO required actions, emailVerified=false)
2. RegistrationService.generateToken() creates InvitationToken (UUID token, 72h expiry)
3. RegistrationEmailService sends custom HTML email with link: /iam/register?token=<token>
4. User clicks link → RegisterController GET /register?token=<token> → form (name pre-filled, set password)
5. User submits → RegisterController POST /register → RegistrationService.completeRegistration()
   → setPassword in Keycloak (Admin API) → setEmailVerified=true → mark token used
6. Redirect to /oauth2/authorization/keycloak → auto-login → /iam/dashboard
```

**Keycloak changes:**
- `createInvitedUser()` now sets a random temp password + NO required actions (instead of UPDATE_PASSWORD + VERIFY_EMAIL)
- New methods on `IdentityProviderService`: `setPassword(KeycloakUserId, Password)`, `setEmailVerified(KeycloakUserId, boolean)`
- Remove `sendActionsEmail()` from `IdentityProviderService` (deprecated, no longer needed)

**Sign-up flow impact:**
- Self sign-up keeps `sendVerificationEmail()` for now (user already has password, only needs email verification)
- Future: could also replace with custom verification, but defer for now

### TDD Steps

| Step | Layer | Test File | Test Cases | Implementation |
|------|-------|-----------|-----------|---------------|
| 1 | Domain | NEW `InvitationTokenTest` | `generateSetsExpiryTo72Hours`, `useMarksTokenAsUsed`, `cannotUseExpiredToken`, `cannotUseAlreadyUsedToken`, `cannotUseNullToken` | NEW `InvitationToken.java` in `domain/registration/` |
| 2 | Domain | NEW `InvitationTokenRepository` interface | — (port, no test) | NEW `InvitationTokenRepository.java` in `domain/registration/` |
| 3 | Domain | MODIFY `IdentityProviderService` | — (port, add methods) | Add `setPassword()`, `setEmailVerified()`, remove `sendActionsEmail()` |
| 4 | Adapter | `KeycloakIdentityProviderAdapter` | Tested via integration in production | Implement `setPassword()`, `setEmailVerified()`, modify `createInvitedUser()` |
| 5 | Application | NEW `RegistrationServiceTest` | `generateTokenCreatesAndSaves`, `completeRegistrationSetsPasswordAndVerifies`, `completeRegistrationInvalidatesToken`, `rejectsExpiredToken`, `rejectsUsedToken`, `rejectsUnknownToken` | NEW `RegistrationService.java` |
| 6 | Application | MODIFY `AccountServiceTest` | Update `inviteMember*` tests: verify `generateToken()` called, `sendActionsEmail()` NOT called | MODIFY `AccountService.inviteMember()` |
| 7 | Persistence | NEW `InvitationTokenRepositoryAdapterTest` | `savesAndFindsToken`, `findsNothingForUnknownToken` | NEW JPA entity + adapter + Flyway V5 |
| 8 | Adapter/Mail | NEW `RegistrationEmailServiceTest` | `sendsEmailWithCorrectLink`, `emailContainsRecipientName` | NEW `RegistrationEmailService.java` in `adapters/mail/` |
| 9 | Controller | NEW `RegisterControllerTest` | `showsFormWithPrefilledData`, `completesRegistrationAndRedirects`, `rejectsExpiredToken`, `rejectsInvalidPassword` | NEW `RegisterController.java` |
| 10 | Config | Integration | Verify Gateway routes | SecurityConfig + Gateway changes |
| 11 | Template | Manual | — | NEW `register/form.html`, `register/success.html`, `email/member-invitation.html` |
| 12 | i18n | — | — | Add keys to messages*.properties |

### New Files
- `travelmate-iam/.../domain/registration/InvitationToken.java`
- `travelmate-iam/.../domain/registration/InvitationTokenRepository.java`
- `travelmate-iam/.../application/RegistrationService.java`
- `travelmate-iam/.../application/command/CompleteRegistrationCommand.java`
- `travelmate-iam/.../adapters/persistence/InvitationTokenJpaEntity.java`
- `travelmate-iam/.../adapters/persistence/InvitationTokenJpaRepository.java`
- `travelmate-iam/.../adapters/persistence/InvitationTokenRepositoryAdapter.java`
- `travelmate-iam/.../adapters/web/RegisterController.java`
- `travelmate-iam/.../adapters/mail/RegistrationEmailService.java`
- `travelmate-iam/src/main/resources/db/migration/V5__add_invitation_token.sql`
- `travelmate-iam/src/main/resources/templates/register/form.html`
- `travelmate-iam/src/main/resources/templates/email/member-invitation.html`
- 6 test files

### Modified Files
- `travelmate-iam/.../domain/account/IdentityProviderService.java` — add `setPassword()`, `setEmailVerified()`; remove `sendActionsEmail()`
- `travelmate-iam/.../adapters/keycloak/KeycloakIdentityProviderAdapter.java` — implement new methods, modify `createInvitedUser()`
- `travelmate-iam/.../application/AccountService.java` — inject `RegistrationService`, replace `sendActionsEmail` with token+email
- `travelmate-iam/.../adapters/web/SecurityConfig.java` — add `/register`, `/register/**` to permitAll
- `travelmate-iam/pom.xml` — add `spring-boot-starter-mail`
- `travelmate-iam/.../resources/application.yml` — add mail config (Mailpit defaults)
- `travelmate-gateway/.../SecurityConfig.java` — add `/iam/register`, `/iam/register/**` to permitAll
- `travelmate-gateway/.../resources/application.yml` — add `/iam/register**` to iam-public route
- `travelmate-iam/src/test/.../TestIdentityProviderConfig.java` — update mock to include new methods
- i18n messages*.properties in IAM

### BDD Scenarios
```gherkin
Feature: Custom Registration for Invited Users
  Scenario: Invited member receives registration email
    Given I am an organizer
    When I invite a new member "lisa@example.com" to my travel party
    Then Lisa receives an email with a registration link
    And the email contains the link to /iam/register?token=<token>

  Scenario: Invited user completes registration
    Given I received a registration email with a valid token
    When I click the registration link
    Then I see a form with my name pre-filled
    And I can set my password
    When I submit the form with a valid password
    Then I am redirected to the login page
    And I can log in with my new password

  Scenario: Expired token shows error
    Given I have a registration token that expired 73 hours ago
    When I click the registration link
    Then I see an error message "Einladungslink abgelaufen"

  Scenario: Already-used token shows error
    Given I already completed my registration
    When I click the registration link again
    Then I see an error message "Einladungslink bereits verwendet"
```

---

## P2: External Invitation Creates New Tenant

**Scope:** IAM SCS + Trips SCS. ~3-4 hours. Most architecturally complex.

### Design Decision: Cross-Tenant Trip Participation

**Problem:** Currently trips are scoped by tenantId. If external users get their own tenant, they
can't see trips from the inviter's tenant. We need a cross-tenant participation model.

**Solution: Participant-based trip access**
- `TripRepository.findAllByParticipantId(UUID)` — finds trips across tenants by participant
- `TripController.list()` merges: trips by tenantId UNION trips by participantId
- `TripController.detail()` access: allowed if participant OR tenant member
- Denormalize participant name onto `Participant` entity (since cross-tenant TravelParty won't have the name)

**External invitation flow (revised):**
```
1. Trips: InvitationService.inviteExternal() → publishes ExternalUserInvitedToTrip (unchanged)
2. IAM: ExternalInvitationConsumer receives event
   → NEW: Creates NEW Tenant (name = "Reisepartei {firstName} {lastName}")
   → Creates Keycloak user (no password, no required actions)
   → Creates Account in NEW tenant
   → Generates InvitationToken (from P1)
   → Sends registration email (from P1)
   → Publishes TenantCreated + AccountRegistered (with NEW tenantId)
3. Trips: TravelPartyService creates TravelParty for NEW tenant
   → linkAwaitingInvitations matches by email → auto-joins trip as participant
4. External user clicks registration link → completes registration (P1 flow)
   → Logs in → sees the trip in their trip list (via participantId query)
```

### TDD Steps

| Step | Layer | Test File | Test Cases | Implementation |
|------|-------|-----------|-----------|---------------|
| 1 | Domain | NEW/MODIFY `ParticipantTest` | — | Add `firstName`, `lastName` to `Participant` |
| 2 | Domain | MODIFY `TripTest` | `addParticipantWithName` | Modify `Trip.addParticipant()` to accept name |
| 3 | Persistence | Trips Flyway V6 | — | Add `first_name`, `last_name` to `trip_participant` |
| 4 | Application (IAM) | NEW `ExternalUserRegistrationTest` | `createsNewTenantAndAccount`, `generatesInvitationToken` | New method on `SignUpService` or new service |
| 5 | Adapter (IAM) | MODIFY `ExternalInvitationConsumer` test | Verify new tenant creation | MODIFY `ExternalInvitationConsumer` |
| 6 | Persistence (Trips) | MODIFY `TripRepositoryAdapterTest` | `findAllByParticipantId` | Add JPA query |
| 7 | Application (Trips) | MODIFY `TripServiceTest` | `findTripsForParticipant` | New service method |
| 8 | Controller (Trips) | MODIFY `TripControllerTest` | `listShowsCrossTenantTrips`, `detailAllowsParticipantAccess` | Modify access model |

### New/Modified Files

**IAM:**
- NEW: `application/command/RegisterExternalUserCommand.java`
- MODIFY: `ExternalInvitationConsumer.java` — call new registration method instead of `inviteMember()`
- MODIFY: `SignUpService.java` or NEW service — `registerExternalUser()` method

**Trips:**
- MODIFY: `Participant.java` — add `firstName`, `lastName` fields
- MODIFY: `Trip.java` — modify `addParticipant()` signature
- MODIFY: `TripRepository.java` — add `findAllByParticipantId(UUID)`
- MODIFY: `TripRepositoryAdapter.java` + `TripJpaRepository.java` — implement query
- MODIFY: `TripService.java` — add `findAllByParticipantId()` method
- MODIFY: `TripController.java` — merge trip lists, relax tenant access check
- MODIFY: `InvitationService.java` — pass names when adding participants
- NEW: `V6__participant_name_denormalization.sql`
- MODIFY: `TripJpaEntity` / `ParticipantJpaEntity` — add name columns

### BDD Scenarios
```gherkin
Feature: External Invitation Creates New Travel Party
  Scenario: External user gets their own travel party
    Given I am an organizer with a trip "Sommerurlaub"
    When I invite external user "lisa@example.com" to the trip
    Then a new travel party "Reisepartei Lisa Müller" is created
    And Lisa receives a registration email
    And Lisa is NOT a member of my travel party

  Scenario: External user sees invited trip after registration
    Given I was invited externally to trip "Sommerurlaub"
    And I completed my registration
    When I log in and view my trips
    Then I see "Sommerurlaub" in my trip list
    And I am listed as a participant

  Scenario: External user can manage their own travel party
    Given I registered through an external invitation
    When I go to my dashboard
    Then I can add companions to my own travel party
    And I cannot see the inviter's travel party data
```

---

## Verification Plan

### Unit Tests (per priority)
```bash
# P5
./mvnw -pl travelmate-iam test -Dtest=HomeControllerTest

# P4
./mvnw -pl travelmate-common test -Dtest=EntityNotFoundExceptionTest
./mvnw -pl travelmate-iam test -Dtest=GlobalExceptionHandlerTest
./mvnw -pl travelmate-trips test -Dtest=GlobalExceptionHandlerTest

# P3
./mvnw -pl travelmate-trips test -Dtest=TripTest
./mvnw -pl travelmate-trips test -Dtest=TripServiceTest

# P1
./mvnw -pl travelmate-iam test -Dtest=InvitationTokenTest
./mvnw -pl travelmate-iam test -Dtest=RegistrationServiceTest
./mvnw -pl travelmate-iam test -Dtest=RegisterControllerTest
./mvnw -pl travelmate-iam test -Dtest=InvitationTokenRepositoryAdapterTest

# P2
./mvnw -pl travelmate-trips test -Dtest=TripTest
./mvnw -pl travelmate-trips test -Dtest=TripControllerTest
```

### Full Build
```bash
./mvnw clean verify
```

### Manual E2E Testing (with docker compose)
1. Start infrastructure: `docker compose up -d`
2. Start SCS services
3. Test flows:
   - Register new user → verify email flow
   - Invite member → check email in Mailpit (localhost:8025)
   - Click registration link → set password → login
   - Create trip → verify all party members auto-joined
   - Invite external user → verify new tenant created
   - External user completes registration → verify trip visible
   - Navigate to landing page while logged in → verify auth-aware nav
   - Trigger errors → verify user-friendly error pages

### Automated E2E (Playwright)
```bash
./mvnw -Pe2e verify
```

---

## Documentation Deliverables

1. **ADR-0013**: Eigene Registrierungsseite statt Keycloak executeActionsEmail (P1 + P2)
2. **ADR-0014**: Cross-Tenant Trip-Teilnahme (P2)
3. **Arc42 Updates**: Section 5 (Building Blocks), Section 6 (Runtime Views), Section 8 (Crosscutting)
4. **BDD Scenarios**: As listed above per priority
