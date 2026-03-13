# Plan: Invitation Flow Rework, Email Communication & Birthday Requirement

## Context

The current Travelmate invitation and email communication has significant gaps:

1. **Trip invitations send NO email** — invitees must manually navigate to the trip page to discover they were invited
2. **No way to invite non-platform users to a trip** — only existing TravelParty members can be invited
3. **Birthday (DateOfBirth) is optional** — not collected at sign-up, nullable in DB
4. **Keycloak member invitation email is generic** — says "Action required" without explaining WHY the user was invited
5. **Missing events** — `InvitationCreated` doesn't exist; `TripCreated` defined but never published
6. **Test gaps** — no E2E tests for trip invitations, no unit tests for `AccountService.inviteMember()`, no email testing

This plan replaces the current Iteration 4 scope (ArchUnit, DLQ, Security Tests → moved to Iteration 5) with a focused user journey improvement.

**Design decisions made:**
- Auto-join on registration (no manual accept for invited new users)
- External invite form lives on Trip detail page (Trips SCS, event-driven to IAM)
- Trips SCS sends trip emails directly via Spring Mail (SCS self-containment)
- Current Iteration 4 infra stories move to Iteration 5

---

## Phase 1: Birthday Required

### 1.1 Domain Changes (travelmate-iam)

**DateOfBirth validation** — `DateOfBirth.java`:
- Add validation: reject future dates, reject dates > 150 years ago
- Keep existing `argumentIsNotNull` check

**Account.java**:
- Remove the `register(...)` overload without `dateOfBirth`
- Make `dateOfBirth` mandatory in remaining `register()` factory method
- Add `argumentIsNotNull(dateOfBirth, "dateOfBirth")`

**Dependent.java**:
- Remove the `add(...)` overload without `dateOfBirth`
- Make `dateOfBirth` mandatory

**SignUpCommand.java** — add `LocalDate dateOfBirth` field:
```java
public record SignUpCommand(String tenantName, String firstName, String lastName,
                            String email, String password, LocalDate dateOfBirth) {}
```

### 1.2 Application Service Changes

**SignUpService.java** — pass `dateOfBirth` from command to `Account.register()`
**AccountService.inviteMember()** — enforce non-null `dateOfBirth`
**AccountService.addDependent()** — enforce non-null `dateOfBirth`

### 1.3 UI Changes

**signup/form.html** — add required `<input type="date" name="dateOfBirth">` between name row and email row
**dashboard/index.html** — change `dateOfBirth` from `required=false` to `required` for both member invitation and companion forms
**SignUpController.java** — accept `dateOfBirth` parameter, pass to `SignUpCommand`
**DashboardController.java** — remove `required = false` from `@RequestParam` for `dateOfBirth`

### 1.4 Database Migration

**V4__make_date_of_birth_required.sql** (travelmate-iam):
```sql
UPDATE account SET date_of_birth = '1900-01-01' WHERE date_of_birth IS NULL;
ALTER TABLE account ALTER COLUMN date_of_birth SET NOT NULL;
UPDATE dependent SET date_of_birth = '1900-01-01' WHERE date_of_birth IS NULL;
ALTER TABLE dependent ALTER COLUMN date_of_birth SET NOT NULL;
```

### 1.5 Tests (TDD — write first)

| Layer | Test | File |
|-------|------|------|
| Domain | `DateOfBirthTest.rejectsFutureDate()` | DateOfBirthTest.java |
| Domain | `DateOfBirthTest.rejectsDateMoreThan150YearsAgo()` | DateOfBirthTest.java |
| Domain | `DateOfBirthTest.acceptsTodayAsDateOfBirth()` | DateOfBirthTest.java |
| Domain | `AccountTest.registerRequiresDateOfBirth()` | AccountTest.java |
| Domain | `DependentTest.addRequiresDateOfBirth()` | DependentTest.java |
| Application | `SignUpServiceTest.signUpPassesDateOfBirthToAccount()` | SignUpServiceTest.java |
| Controller | `SignUpControllerTest.signUpWithoutDateOfBirthFails()` | SignUpControllerTest.java |
| E2E | `SignUpWithBirthdayIT.signUpFormShowsBirthdayField()` | SignUpWithBirthdayIT.java |
| E2E | `SignUpWithBirthdayIT.signUpWithBirthdaySucceeds()` | SignUpWithBirthdayIT.java |

### Critical files:
- `travelmate-iam/.../domain/account/DateOfBirth.java`
- `travelmate-iam/.../domain/account/Account.java`
- `travelmate-iam/.../domain/dependent/Dependent.java`
- `travelmate-iam/.../application/command/SignUpCommand.java`
- `travelmate-iam/.../application/SignUpService.java`
- `travelmate-iam/.../adapters/web/SignUpController.java`
- `travelmate-iam/src/main/resources/templates/signup/form.html`
- `travelmate-iam/src/main/resources/templates/dashboard/index.html`
- `travelmate-iam/src/main/resources/db/migration/V4__make_date_of_birth_required.sql`

---

## Phase 2: Trip Invitation Email for Existing Members

### 2.1 New Event Contract (travelmate-common)

**InvitationCreated.java** in `events/trips/`:
```java
record InvitationCreated(UUID tenantId, UUID tripId, UUID invitationId,
    String inviteeEmail, String inviteeFirstName,
    String tripName, LocalDate tripStartDate, LocalDate tripEndDate,
    String inviterFirstName, String inviterLastName,
    String invitationType, LocalDate occurredOn) implements DomainEvent
```

**RoutingKeys.java** — add:
```java
public static final String INVITATION_CREATED = "trips.invitation-created";
```

### 2.2 Domain Changes (travelmate-trips)

**Invitation.java** — register `InvitationCreated` domain event in `create()` factory method.
The event carries trip context (name, dates, inviter name) so the email adapter can compose a meaningful email. The application service must enrich the event with trip/member data before publishing.

**InvitationService.invite()** — after creating invitation:
1. Load trip details (name, dates) from `Trip` aggregate
2. Resolve inviter name and invitee email from `TravelParty`
3. Publish `InvitationCreated` event via `ApplicationEventPublisher`

### 2.3 Email Infrastructure (travelmate-trips)

**New dependency** in `travelmate-trips/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

**Mail configuration** in `application.yml`:
```yaml
spring.mail:
  host: ${MAIL_HOST:localhost}
  port: ${MAIL_PORT:1025}
```

**New adapter** — `adapters/mail/InvitationEmailListener.java`:
- `@Component` + `@Profile("!test")`
- `@TransactionalEventListener(phase = AFTER_COMMIT)` listening to `InvitationCreated`
- Uses `JavaMailSender` + Thymeleaf `TemplateEngine` to render and send HTML email
- Follows the same pattern as `DomainEventPublisher` in IAM

**New email templates** in `templates/email/`:
- `invitation-member.html` — "Du wurdest zur Reise {tripName} eingeladen" with trip details + link to trip page
- `invitation-external.html` — "Du wurdest zu Travelmate eingeladen" with registration link (Phase 3)

**Test adapter** — `adapters/mail/NoOpInvitationEmailListener.java` with `@Profile("test")` (or simply no bean loaded)

### 2.4 UI Changes (travelmate-trips)

**trip/list.html** — add "Offene Einladungen" section at the top:
- Shows pending invitations for the current user with Accept/Decline buttons
- Hidden when no pending invitations exist
- Uses `<article>` cards with trip name, dates, inviter name
- HTMX accept/decline inline

**TripController.java** — on GET `/` (trip list):
- Query `invitationRepository.findByInviteeIdAndStatus(currentMemberId, PENDING)`
- Add `pendingInvitations` to model

**Navigation badge** — in Trips layout, add pending invitation count next to "Reisen" nav link

### 2.5 Tests

| Layer | Test | File |
|-------|------|------|
| Domain | `InvitationTest.createRegistersInvitationCreatedEvent()` | InvitationTest.java |
| Application | `InvitationServiceTest.invitePublishesInvitationCreatedEvent()` | InvitationServiceTest.java |
| Application | `InvitationServiceTest.inviteRejectsInvitationToSelf()` | InvitationServiceTest.java |
| Controller | `TripControllerTest.tripListShowsPendingInvitations()` | TripControllerTest.java |
| E2E | `TripInvitationIT.inviteMemberAndVerifyEmail()` | TripInvitationIT.java (Mailpit) |
| E2E | `TripInvitationIT.acceptInvitationViaUi()` | TripInvitationIT.java |

### Critical files:
- `travelmate-common/.../events/trips/InvitationCreated.java` (NEW)
- `travelmate-common/.../messaging/RoutingKeys.java`
- `travelmate-trips/.../domain/invitation/Invitation.java`
- `travelmate-trips/.../application/InvitationService.java`
- `travelmate-trips/.../adapters/mail/InvitationEmailListener.java` (NEW)
- `travelmate-trips/.../adapters/web/TripController.java`
- `travelmate-trips/src/main/resources/templates/email/invitation-member.html` (NEW)
- `travelmate-trips/src/main/resources/templates/trip/list.html`

---

## Phase 3: External Trip Invitation (New Users)

### 3.1 Domain Model Extension

**InvitationType enum** (NEW) in `domain/invitation/`:
```java
public enum InvitationType { MEMBER, EXTERNAL }
```

**InvitationStatus** — add `AWAITING_REGISTRATION`:
```java
AWAITING_REGISTRATION, PENDING, ACCEPTED, DECLINED
```

**Invitation.java** — extend aggregate:
- Add fields: `inviteeEmail` (String), `invitationType` (InvitationType)
- Make `inviteeId` nullable (null for EXTERNAL until user registers)
- New factory method: `inviteExternal(tenantId, tripId, inviteeEmail, invitedBy)` → status = AWAITING_REGISTRATION, type = EXTERNAL
- New method: `linkToMember(UUID inviteeId)` → asserts AWAITING_REGISTRATION, sets inviteeId, transitions to PENDING, auto-accept (registers event)
- Rename existing `create()` to `inviteMember(tenantId, tripId, inviteeId, inviteeEmail, invitedBy)` → status = PENDING, type = MEMBER

**InvitationRepository** — add:
- `findByInviteeEmailAndStatus(String email, InvitationStatus status)`
- `existsByTripIdAndInviteeEmail(TripId, String email)`

### 3.2 New Event Contract (travelmate-common)

**ExternalUserInvitedToTrip.java** in `events/trips/`:
```java
record ExternalUserInvitedToTrip(UUID tenantId, UUID tripId, UUID invitationId,
    String email, String firstName, String lastName, LocalDate dateOfBirth,
    LocalDate occurredOn) implements DomainEvent
```

**RoutingKeys.java** — add:
```java
public static final String EXTERNAL_USER_INVITED = "trips.external-user-invited";
```

### 3.3 Cross-Context Event Flow

```
Organizer fills "Invite by email" form on Trip detail
    │
    ▼
TripController.inviteExternal(email, firstName, lastName, dateOfBirth)
    │
    ▼
InvitationService.inviteExternal(InviteExternalCommand)
  → Creates Invitation.inviteExternal(...) [AWAITING_REGISTRATION]
  → Publishes InvitationCreated (for email to new user)
  → Publishes ExternalUserInvitedToTrip (for IAM user creation)
    │
    ├──▶ [Trips - InvitationEmailListener]
    │      Sends "You're invited to trip X — register to join!" email
    │      Link: /iam/signup?invitationToken={token}
    │
    └──▶ [IAM - ExternalInvitationConsumer] (NEW)
           Consumes ExternalUserInvitedToTrip
           Calls AccountService.inviteMember() to create Keycloak user + Account
           Publishes AccountRegistered event
              │
              ▼
           [Trips - IamEventConsumer.onAccountRegistered()]
             Updates TravelParty (existing)
             Finds AWAITING_REGISTRATION invitation by email
             Calls invitation.linkToMember(newMemberId)
             Auto-accepts → participant added to trip
```

### 3.4 IAM Changes

**ExternalInvitationConsumer.java** (NEW) in `adapters/messaging/`:
- `@Profile("!test")`
- Consumes `ExternalUserInvitedToTrip` from queue `iam.external-user-invited`
- Calls `AccountService.inviteMember()` with data from event
- Idempotency: check if account with email already exists before creating

### 3.5 Trips Changes

**IamEventConsumer.onAccountRegistered()** — extend:
- After updating TravelParty, query `invitationRepository.findByInviteeEmailAndStatus(email, AWAITING_REGISTRATION)`
- For each found invitation: call `invitation.linkToMember(newMemberId)` (auto-joins trip)
- Save invitation + update trip participant list

**InvitationService.inviteExternal()** (NEW):
- Validates email not already invited to this trip
- Creates `Invitation.inviteExternal(...)`
- Loads trip context (name, dates) for event
- Publishes `InvitationCreated` + `ExternalUserInvitedToTrip`

### 3.6 UI Changes

**trip/detail.html** — add "Neue Person einladen" form next to existing member dropdown:
```
[Existing member dropdown] [Senden]    [email] [firstName] [lastName] [dateOfBirth] [Per E-Mail einladen]
```
Both forms in a PicoCSS `grid` layout. The external form uses HTMX POST to `/{tripId}/invitations/external`.

**trip/invitations.html** — show invitation type indicator:
- MEMBER invitations: show member name
- EXTERNAL invitations: show email + "(extern)" badge + status AWAITING_REGISTRATION

### 3.7 Database Migration

**V5__invitation_external_support.sql** (travelmate-trips):
```sql
ALTER TABLE invitation ADD COLUMN invitee_email VARCHAR(255);
ALTER TABLE invitation ADD COLUMN invitation_type VARCHAR(20) NOT NULL DEFAULT 'MEMBER';
ALTER TABLE invitation ALTER COLUMN invitee_id DROP NOT NULL;
-- Backfill existing invitations with email from travel_party_member
UPDATE invitation i SET invitee_email = (
    SELECT tpm.email FROM travel_party_member tpm WHERE tpm.member_id = i.invitee_id
) WHERE i.invitee_email IS NULL;
ALTER TABLE invitation ALTER COLUMN invitee_email SET NOT NULL;
```

### 3.8 Tests

| Layer | Test | File |
|-------|------|------|
| Domain | `InvitationTest.inviteExternalCreatesAwaitingRegistration()` | InvitationTest.java |
| Domain | `InvitationTest.linkToMemberTransitionsToPendingAndAutoAccepts()` | InvitationTest.java |
| Domain | `InvitationTest.linkToMemberRejectsNonAwaitingStatus()` | InvitationTest.java |
| Application | `InvitationServiceTest.inviteExternalPublishesBothEvents()` | InvitationServiceTest.java |
| Application | `InvitationServiceTest.inviteExternalRejectsDuplicateEmail()` | InvitationServiceTest.java |
| Application | `TravelPartyServiceTest.onAccountRegisteredLinksAwaitingInvitations()` | TravelPartyServiceTest.java |
| Controller | `TripControllerTest.inviteExternalReturnsUpdatedFragment()` | TripControllerTest.java |
| E2E | `PlatformInvitationIT.inviteNewUserAndVerifyRegistrationEmail()` | PlatformInvitationIT.java |
| E2E | `PlatformInvitationIT.newUserRegistersAndAutoJoinsTrip()` | PlatformInvitationIT.java |

### Critical files:
- `travelmate-trips/.../domain/invitation/Invitation.java` — extend with inviteeEmail, invitationType, linkToMember()
- `travelmate-trips/.../domain/invitation/InvitationType.java` (NEW)
- `travelmate-trips/.../domain/invitation/InvitationStatus.java` — add AWAITING_REGISTRATION
- `travelmate-trips/.../application/InvitationService.java` — add inviteExternal()
- `travelmate-trips/.../adapters/messaging/IamEventConsumer.java` — extend onAccountRegistered()
- `travelmate-common/.../events/trips/ExternalUserInvitedToTrip.java` (NEW)
- `travelmate-iam/.../adapters/messaging/ExternalInvitationConsumer.java` (NEW)
- `travelmate-trips/src/main/resources/db/migration/V5__invitation_external_support.sql` (NEW)
- `travelmate-trips/src/main/resources/templates/trip/detail.html`
- `travelmate-trips/src/main/resources/templates/email/invitation-external.html` (NEW)

---

## Phase 4: Keycloak Email Improvement

### 4.1 Update executeActions Email Template

**docker/keycloak/themes/travelmate/email/messages/messages_de.properties**:
```properties
executeActionsSubject=Travelmate: Willkommen! Richte dein Konto ein
executeActionsBodyHtml=<h2>Willkommen bei Travelmate!</h2>\
<p>Du wurdest als Mitglied einer Reisepartei zu Travelmate eingeladen.</p>\
<p>Bitte richte dein Konto ein, indem du ein Passwort vergibst und deine E-Mail bestaetigst:</p>
```

**docker/keycloak/themes/travelmate/email/messages/messages_en.properties**:
```properties
executeActionsSubject=Travelmate: Welcome! Set up your account
executeActionsBodyHtml=<h2>Welcome to Travelmate!</h2>\
<p>You've been invited to join a travel party on Travelmate.</p>\
<p>Please set up your account by creating a password and verifying your email:</p>
```

### Critical files:
- `docker/keycloak/themes/travelmate/email/messages/messages_de.properties`
- `docker/keycloak/themes/travelmate/email/messages/messages_en.properties`
- `docker/keycloak/themes/travelmate/email/html/executeActions.ftl`

---

## Phase 5: Existing Test Gap Closure

Fill gaps identified during exploration (independent of new features):

| Test | File | Gap |
|------|------|-----|
| `AccountServiceTest.inviteMemberCreatesKeycloakUserAndAccount()` | AccountServiceTest.java (NEW) | Zero tests for inviteMember() |
| `AccountServiceTest.inviteMemberRejectsDuplicateEmail()` | AccountServiceTest.java | Missing negative test |
| `AccountServiceTest.inviteMemberRollsBackOnFailure()` | AccountServiceTest.java | Rollback not tested |
| `AccountServiceTest.inviteMemberPublishesAccountRegisteredEvent()` | AccountServiceTest.java | Event not verified |
| `InvitationRepositoryAdapterTest` (NEW) | InvitationRepositoryAdapterTest.java | No persistence tests |
| `DashboardControllerTest.inviteMemberPostReturnsFragment()` | DashboardControllerTest.java | No controller tests |

---

## Phase 6: Documentation & Backlog Updates

### 6.1 Product Backlog Updates

**New User Stories** (replace US-INFRA-020/021/022/030 in Iteration 4):

| ID | Title | Priority | Size |
|----|-------|----------|------|
| US-IAM-070 | Birthday Required for All Users | Must | M |
| US-TRIPS-070 | Trip Invitation Email Notification | Must | M |
| US-TRIPS-071 | Pending Invitations on Trip List Page | Must | S |
| US-TRIPS-072 | External Trip Invitation (Invite by Email) | Must | L |
| US-TRIPS-073 | Auto-Join Trip on Registration via Invitation | Must | M |
| US-IAM-071 | Improve Keycloak Invitation Email | Should | S |
| US-INFRA-060 | Close Existing Test Coverage Gaps | Should | M |

**Move to Iteration 5**: US-INFRA-020 (ArchUnit), US-INFRA-021 (JaCoCo), US-INFRA-022 (DLQ), US-INFRA-030 (Tenant Isolation Tests)

### 6.2 ADR

**ADR-0012: Trip-Einladung per E-Mail und Plattform-Einladung** (MADR, German):
- Decision: Trips SCS sends invitation emails via Spring Mail
- Decision: External invitation creates user via event to IAM, auto-joins trip
- Decision: Birthday is required for all users
- Alternatives considered: Notification SCS, manual accept after registration

### 6.3 Arc42 Updates

- **Section 5 (Bausteinsicht)**: Add email adapter in Trips SCS, ExternalInvitationConsumer in IAM
- **Section 6 (Laufzeitsicht)**: Add sequence diagrams for both invitation paths
- **Section 8 (Querschnittliche Konzepte)**: Email communication strategy
- **Section 10 (Qualitaetsanforderungen)**: Update QS-U02 (Ease of Use) with email notification

### 6.4 i18n Keys (summary)

New keys needed in Trips `messages_de.properties` / `messages_en.properties`:
- `invitation.pending.title`, `invitation.pending.invitedBy`, `invitation.pending.empty`
- `invitation.inviteByEmail`, `invitation.sendEmailInvite`, `invitation.existingMember`
- `invitation.sentSuccess`, `invitation.statusExternal`, `invitation.statusAwaitingRegistration`
- `email.tripInvitation.subject`, `email.platformInvitation.subject`

New keys in IAM:
- `signup.dateOfBirth`, `signup.invitationContext`

---

## Implementation Order (TDD)

```
Phase 1: Birthday Required
  ├── Domain tests → Domain changes → Migration
  ├── Application tests → Service changes
  ├── Controller tests → UI changes
  └── E2E tests

Phase 2: Trip Invitation Email (existing members)
  ├── Event contract (travelmate-common)
  ├── Domain tests → Invitation event registration
  ├── Application tests → InvitationService email publishing
  ├── Email adapter (Spring Mail + Thymeleaf templates)
  ├── Controller tests → Pending invitations on trip list
  └── E2E tests (Mailpit verification)

Phase 3: External Trip Invitation (new users)
  ├── Domain tests → InvitationType, AWAITING_REGISTRATION, linkToMember
  ├── Event contract (ExternalUserInvitedToTrip)
  ├── Application tests → inviteExternal(), auto-join on registration
  ├── IAM consumer (ExternalInvitationConsumer)
  ├── Migration V5
  ├── Controller + UI changes
  └── E2E tests (full cross-SCS flow)

Phase 4: Keycloak Email Improvement (standalone)

Phase 5: Test Gap Closure (parallelizable)

Phase 6: Documentation & Backlog Updates
```

---

## Verification

### Manual Testing
1. Start infrastructure: `docker compose up -d`
2. Start all SCS: gateway (:8080), iam (:8081), trips (:8082)
3. Sign up with birthday → verify birthday stored and shown on dashboard
4. Create trip → invite existing member → check Mailpit (:8025) for email
5. Accept invitation → verify participant added
6. Invite by email (non-platform user) → check Mailpit for registration email
7. Register via invitation link → verify auto-join to trip

### Automated Testing
```bash
# Unit + integration tests (all modules)
./mvnw clean verify

# E2E tests (requires docker compose infrastructure)
./mvnw -Pe2e verify
```

### Test Count Estimate
- Current: ~185 tests
- After: ~250 tests (+65 new/extended)
