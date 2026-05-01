# Story Detail — S19-INVITE-EXISTING

**Iteration**: 19 (Demo-Hardening + Phase-0-GTM)
**Estimated size**: M (5–15h)
**Status**: PLANNED
**Authored**: Cross-functional team-planning session 2026-04-30

---

## User Story (RE)

**As an** organizer
**I want** an external trip invitation for an email address that already
belongs to a Travelmate account to always trigger an actionable follow-up
email
**so that** the invitee is never silently dropped and always has a clear
path to accept the invitation.

## Background

The current `ExternalInvitationConsumer.processExternalUserInvited`
short-circuits when `accountRepository.existsByUsernameAcrossTenants` is
true (file: `travelmate-iam/src/main/java/de/evia/travelmate/iam/adapters/messaging/ExternalInvitationConsumer.java:55-58`).
The invitee receives only mail #1 (the trip-invitation email from the
`InvitationEmailListener` in trips-SCS). If the invitee has no Keycloak
password set, or simply forgot it, the trip-invitation link redirects to
the Keycloak login screen — a dead end. Discovered during the
2026-04-30 demo smoke on the Hetzner-hosted demo.

## Acceptance Criteria (RE)

**Scenario 1: New email — existing registration path is preserved**
Given an `ExternalUserInvitedToTrip` event arrives in IAM
And the email address has no existing account
When `ExternalInvitationConsumer` processes the event
Then a registration link is sent as before (existing behavior)
And no error or silent skip occurs

**Scenario 2: Existing account with no Keycloak password credential**
Given an `ExternalUserInvitedToTrip` event arrives in IAM
And the email address matches an existing Account
And the corresponding Keycloak user has no credential of type `password`
When the consumer calls `IdentityProviderService.hasPasswordCredential(userId)`
Then it calls `IdentityProviderService.sendUpdatePasswordEmail(userId)` with
   action `UPDATE_PASSWORD` and `redirect_uri` pointing to
   `${TRAVELMATE_PUBLIC_URL}/trips/invitations/{invitationId}`
And a log line at INFO reads `Password setup link sent to {email}`
And the consumer does not silently return

**Scenario 3: Existing account with a password credential set**
Given an `ExternalUserInvitedToTrip` event arrives in IAM
And the email address matches an existing Account
And the Keycloak user already has a credential of type `password`
When the consumer processes the event
Then it sends a re-login notice email via `ReLoginNoticeEmailService`
And the email body contains the trip name, the inviter name, and the
   login URL `${TRAVELMATE_PUBLIC_URL}`
And a log line at INFO reads `Re-login notice sent to {email} for trip {tripName}`

**Scenario 4: Both mail #1 and mail #2 arrive independently**
Given the Trips SCS sends mail #1 (`InvitationEmailListener`) for the same invitation
And IAM sends mail #2 (password-setup or re-login notice) via the consumer
When both listeners process the same `ExternalUserInvitedToTrip` event
Then both emails are delivered independently
And replaying the event a second time does not produce duplicate
   account-creation side effects (idempotent)

**Scenario 5: Password-setup branch completes — user can accept invitation**
Given the invitee received the Keycloak password-setup email
When the invitee follows the link, sets a password, and is redirected
   to `/trips/invitations/{invitationId}`
Then the page loads with a valid JWT session
And the invitee can accept the invitation successfully

## Edge Cases / Error Scenarios (RE)

- **Keycloak Admin API timeout** when calling `GET .../credentials` or
  `PUT .../execute-actions-email`: consumer must catch the exception, log
  at WARN with the email and invitation ID, and re-throw or NACK the
  message so RabbitMQ DLQ handles retry. Do not swallow and silently drop.
- **Idempotency on event replay**: redelivered messages must not trigger
  a second Keycloak email. Mitigation options: (a) DB-backed
  `external_invite_followups` marker table, or (b) Caffeine in-memory
  cache keyed by `(email, tripId)` with 1-hour TTL.
- **Race between mail #1 and mail #2**: mail #1 may arrive first; the
  trip-invitation email copy must include a note that a second email with
  login credentials is on its way.
- **Keycloak SMTP not configured at realm level**: `execute-actions-email`
  returns HTTP 500. Consumer must log ERROR and not ack.
- **Redirect URI not on Keycloak allow-list**: silent failure post-set.
  Integration test must verify `${APP_BASE_URL}/*` is in the client's
  valid redirect URIs in the realm template.
- **Existing account belongs to a different tenant**: the consumer must
  not join the account to the inviting tenant. Tenant assignment remains
  a separate flow.

---

## DDD / Architecture Notes (Architect)

- **Aggregate impact**: None. The fix lives entirely in the **messaging
  adapter** of IAM. No state on `Tenant`, `Account`, or `Dependent`
  aggregates changes. The branch logic is a pure routing decision based
  on a Keycloak credential lookup; it does not produce or consume domain
  events of its own.
- **New events**: none. The trigger remains the existing cross-context
  event `de.evia.travelmate.common.events.trips.ExternalUserInvitedToTrip`.
- **New ports / adapters**:
  - Extend the existing **port** `IdentityProviderService` with two methods:
    `boolean hasPasswordCredential(KeycloakUserId)` and
    `void sendUpdatePasswordEmail(KeycloakUserId, URI redirectUri, String clientId)`.
    Implement them in `KeycloakIdentityProviderAdapter` (file:
    `travelmate-iam/.../adapters/keycloak/KeycloakIdentityProviderAdapter.java`)
    using `realmResource().users().get(id).credentials()` and
    `executeActionsEmail(List.of("UPDATE_PASSWORD"), redirectUri, clientId)`.
    Same pattern as the existing `sendVerificationEmail` (line ~92).
  - New collaborator class `ExistingAccountInviteRouter` in
    `travelmate-iam/.../application/`. Pure application service, no Spring
    annotations beyond `@Component`. Takes `IdentityProviderService` +
    `RegistrationEmailService` + new `ReLoginNoticeEmailService`. Exposes
    `route(email, firstName, tripName, inviterName)`.
  - New email template: `templates/email/re-login-notice.html` alongside
    existing `templates/email/member-invitation.html`.
  - New service `ReLoginNoticeEmailService` mirrors `RegistrationEmailService`
    (one method, same MimeMessage/Thymeleaf wiring).
- **Hexagonal conformity check**: PASS. The new method on
  `IdentityProviderService` keeps the port in `domain/account/`; the adapter
  remains the only Keycloak-aware class. `ExistingAccountInviteRouter` is
  in `application/`, not `adapters/`. No domain entities gain framework deps.
- **Reuse opportunities**:
  - `KeycloakIdentityProviderAdapter.sendVerificationEmail` — same
    `executeActionsEmail` API, copy-shape for `UPDATE_PASSWORD`.
  - `RegistrationEmailService` (lines 35–62) — clone for
    `ReLoginNoticeEmailService`; reuse the
    `MailAuthenticationException` / `MailSendException` catch blocks verbatim.
  - `ExternalInvitationConsumer.processExternalUserInvited` — replace the
    early-return at line 56 with a call to
    `existingAccountInviteRouter.route(...)`. Keep the
    `externalUserInvitedTimer` Micrometer wrapping intact so the new
    branches stay observable.
- **Risk + mitigation**: **Replay non-idempotency in the password-setup
  branch.** Each event re-delivery would trigger a fresh
  `execute-actions-email`, spamming the invitee. Mitigation: gate the
  `UPDATE_PASSWORD` send behind a short-TTL idempotency check via either
  `external_invite_followups` table (Flyway V8) or Caffeine cache keyed by
  `(email, tripId)` with 1-hour TTL. Outbox/replay design from later
  iterations must respect this gate.

---

## UX Wireframes / Journey (UX Designer)

### Journey: Invitee with existing account receives password-setup email

| Step | Touchpoint | Emotion | Pain / Opportunity |
|---|---|---|---|
| 1. Receives two emails | Inbox: Mail #1 (trip invite) + Mail #2 (password setup) | Confused — two emails at once | Mail #2 subject must name the trip so it looks related, not like spam |
| 2. Opens Mail #2, clicks CTA | Keycloak password-set form | Uncertain — unfamiliar Keycloak page | Keycloak form has Travelmate theme; no extra UX control here |
| 3. Sets password, submits | Keycloak processes, redirects to `redirect_uri` | Relieved | Redirect lands directly on `/trips/invitations/{id}` — no extra login step |
| 4. Trip invitation acceptance page | `/trips/invitations/{id}` | Ready | Existing page behavior unchanged; user sees trip name and Accept/Decline |

### Mail #2 — Passwort einrichten (Keycloak-triggered via `execute-actions-email`)

Sent by Keycloak's own template engine. Spec for the Keycloak realm email
template (`travelmate` realm → Email → Theme: `travelmate`). Visually
mirrors the existing `member-invitation.html` layout.

```
Subject (DE): Travelmate – Passwort festlegen und Einladung annehmen
Subject (EN): Travelmate – Set your password to accept the invitation

Preheader (DE): Dein Konto ist bereits vorhanden. Lege jetzt ein Passwort fest.
Preheader (EN): Your account already exists. Set a password to continue.

Body (DE):
  Hallo [Vorname],

  Du wurdest zu einer Reise auf Travelmate eingeladen. Dein Konto
  existiert bereits — du musst nur noch ein Passwort festlegen,
  um dich einzuloggen.

  [CTA-Button: Passwort festlegen]

  Nach dem Festlegen des Passworts wirst du direkt zur Einladung
  weitergeleitet.

  Hinweis: Dieser Link ist 72 Stunden gültig.

  Falls du keine Einladung erwartet hast, kannst du diese E-Mail
  ignorieren — ohne Aktion passiert nichts.

CTA button label (DE): Passwort festlegen
CTA button label (EN): Set password
```

HTML structure: reuse `member-invitation.html` pattern — header band with
inline SVG logo + wordmark, headline block, body paragraph, single CTA
button, notice box (blue left-border), fallback link, footer. Eyebrow:
`EINLADUNG ANNEHMEN · ACCEPT INVITATION`.

### Mail #3 — Erneut anmelden (Re-login Notice)

New template: `email/re-login-notice.html`. Sent by
`ReLoginNoticeEmailService` when existing Keycloak user already has a
password. Model variables: `firstName`, `tripName`, `inviterName`,
`loginUrl`.

```
Subject (DE): Du wurdest zu „{tripName}" eingeladen – jetzt anmelden
Subject (EN): You've been invited to "{tripName}" – sign in to accept

Body (DE):
  Hallo [Vorname],

  [Einladender] hat dich zur Reise „[Reisename]" eingeladen.
  Du hast bereits ein Travelmate-Konto. Melde dich einfach mit
  deinen bestehenden Zugangsdaten an — danach findest du die
  Einladung in deinem Dashboard.

  [CTA-Button: Jetzt anmelden]

CTA button label (DE): Jetzt anmelden
loginUrl: ${TRAVELMATE_PUBLIC_URL}/oauth2/authorization/keycloak
```

Footer note: `Versehentlich erhalten? Diese E-Mail ignorieren — ohne Anmeldung passiert nichts.`

The trip name in the subject line is the primary disambiguation signal
when both Mail #1 and Mail #3 land in the inbox simultaneously.

---

## BDD Scenarios / Test Plan (QA)

### Test pyramid distribution

- **Unit tests (5)**:
  - `ExistingAccountInviteRouterTest` — three routing branches (new
    account, existing-no-password, existing-with-password)
  - `ReLoginNoticeEmailServiceTest` — renders correct template
  - `IdentityProviderService` port contract guard (null-safety of new methods)
- **Integration / Slice tests (2)**:
  - `KeycloakIdentityProviderAdapterTest` extended with `hasPasswordCredential`
    and `sendUpdatePasswordEmail` using Mockito on `UserResource` /
    `CredentialRepresentation` (existing pattern)
  - `ExternalInvitationConsumerRouterIT` (`@SpringBootTest` +
    `@ActiveProfiles("test")` + `@MockitoBean IdentityProviderService`)
    covering all three dispatch paths
- **E2E (1)**: `ExternalInviteExistingAccountIT` uses Mailpit to assert the
  password-setup email arrives and its link lands on the Keycloak
  `UPDATE_PASSWORD` action page

### Feature file: `existing-account-invite-routing.feature`

Location: `travelmate-iam/src/test/resources/features/`

```gherkin
Feature: External invite routing for existing accounts
  As the IAM messaging layer
  I want to route ExternalUserInvitedToTrip events correctly
  So that existing members receive the right follow-up action

  Background:
    Given a trips organizer sends an external invite to "invitee@example.com"

  @happy-path
  Scenario: New email — creates account and sends registration email
    Given no account exists for "invitee@example.com"
    When the ExternalInvitationConsumer processes the event
    Then a new Tenant and Account are created
    And a registration email with a setup-token link is sent

  @routing
  Scenario: Existing account with no password credential — sends UPDATE_PASSWORD email
    Given an account already exists for "invitee@example.com"
    And the Keycloak user has no password credential
    When the ExternalInvitationConsumer processes the event
    Then no new Tenant or Account is created
    And IdentityProviderService.sendUpdatePasswordEmail is called for that user

  @routing
  Scenario: Existing account with password — sends re-login notice email
    Given an account already exists for "invitee@example.com"
    And the Keycloak user has a password credential
    When the ExternalInvitationConsumer processes the event
    Then no new Tenant or Account is created
    And ReLoginNoticeEmailService sends a re-login notice email

  @negative
  Scenario: Keycloak user lookup fails — event processing throws and is re-queued
    Given an account already exists for "invitee@example.com"
    And the Keycloak admin API returns a 500 error
    When the ExternalInvitationConsumer processes the event
    Then an IdentityProviderException is propagated
    And the message is not acknowledged (DLQ backoff applies)

  @boundary
  Scenario: Duplicate event delivery — idempotent
    Given the same ExternalUserInvitedToTrip event is delivered twice
    When the consumer processes the first event
    Then account creation succeeds
    When the consumer processes the second event
    Then the existing-account routing branch is taken without duplicate side effects
```

### Test-doubles / mocking strategy

- `IdentityProviderService` (Keycloak): Mockito mock at port boundary in
  unit/integration; real Keycloak in Docker for E2E
- `RegistrationEmailService` / `ReLoginNoticeEmailService`: Mockito mock
  in unit; Mailpit SMTP capture in E2E
- `AccountRepository`: H2 in test profile
- `SignUpService`: `@MockitoBean` in `ExternalInvitationConsumerRouterIT`

### Files to create

- `travelmate-iam/src/test/java/.../application/ExistingAccountInviteRouterTest.java`
- `travelmate-iam/src/test/java/.../adapters/messaging/ExternalInvitationConsumerRouterIT.java`
- `travelmate-iam/src/test/java/.../adapters/keycloak/KeycloakIdentityProviderAdapterTest.java` (extend existing)
- `travelmate-iam/src/test/resources/features/existing-account-invite-routing.feature`
- `travelmate-e2e/src/test/java/.../ExternalInviteExistingAccountIT.java`

---

## Threat Model (Security — STRIDE)

| STRIDE | Threat | Mitigation |
|---|---|---|
| **Spoofing** | Attacker triggers Keycloak `execute-actions-email` (UPDATE_PASSWORD) against arbitrary victim emails using a self-issued external invitation → victim receives a phishing-pretext-friendly password-reset email | Only invoke admin API when `ExternalInvitationConsumer` resolves an existing Keycloak user whose email exactly matches the invitation target email AND the invitation was emitted by an authenticated organizer. Never call admin API for unknown emails — fall back to standard registration. Use a dedicated Keycloak service account `travelmate-admin-bot` with only `manage-users` on realm `travelmate`, no `realm-admin`. |
| **Tampering** | `redirect_uri` parameter is manipulated to point to attacker-controlled host (open redirect → credential theft) | Hardcode `redirect_uri` in `ExternalInvitationConsumer` from `@ConfigurationProperties("app.gateway.base-url")`. Add the exact pattern `${APP_BASE_URL}/trips/invitations/*` to Keycloak client `travelmate-gateway` Valid Redirect URIs allow-list. Never accept redirect from event payload. |
| **Repudiation** | Organizer denies sending invite; admin denies sending the password-reset email | Persist `ExternalInvitationDispatched(invitationId, targetEmail, dispatchedBy, dispatchedAt, keycloakAction)` event in Trips. Audit-log the IAM-side admin-API call with username, target user-id, action, and correlation-id (MDC). |
| **Information Disclosure** | Keycloak admin-API errors leak whether an email is registered (account enumeration via timing or response body) | Catch `WebClientResponseException` in `KeycloakAdminClient`. Log only correlation-id + status code. Return generic `dispatched()` to caller regardless of Keycloak outcome. Never echo Keycloak error body to user-facing flash. |
| **Denial of Service** | Email-bombing: replaying `ExternalInvitationCreated` events triggers N password-reset emails to same victim → SMTP relay abuse + reputation damage | (a) Idempotency: nullable `ExternalInvitation.dispatchedAt` field; consumer short-circuits if non-null. (b) Rate-limit per-email (max 1 dispatch per email per 10 min) using `Account.lastInviteDispatchedAt`. (c) RabbitMQ DLQ already in place — ensure consumer is non-requeue on business errors. |
| **Elevation of Privilege** | Compromised SCS leaks the Keycloak admin-bot client-secret → full realm takeover | Store `keycloak.admin.client-secret` in env-only, never committed. Restrict service-account roles to `manage-users` + `view-users` on `travelmate` realm only — explicitly deny `realm-admin`, `manage-clients`, `manage-realm`. Verify in `KeycloakSetupTest`. |

### DSGVO / Privacy Notes

- Data: target email, Keycloak user-id, invitation-id, organizer-account-id.
  Lawful basis: Art. 6(1)(f) legitimate interest (organizing travel) + Art. 6(1)(b)
  pre-contractual measure if invitee accepts.
- Retention: dispatch-audit log 12 months, then purge with the invitation aggregate.
- No 3rd-country transfer (Keycloak self-hosted EU; Strato SMTP relay is EU).
- Update Datenschutzhinweis: mention "wir senden in Ihrem Namen Einladungs-/Passwort-Setup-Mails über Keycloak".

### Concrete Action Items for the Sprint

1. Add allow-list entry `${APP_BASE_URL}/trips/invitations/*` to Keycloak
   realm export — Security AC fails until verified by E2E.
2. Implement `ExternalInvitation.markDispatched()` + Flyway V8
   (column `dispatched_at`); consumer reads-and-checks before admin call.
3. New `IdentityProviderService.sendUpdatePasswordEmail(userId, redirectUri, clientId)`
   — `redirectUri` parameter is internal only, built from
   `@ConfigurationProperties`; no caller can override.
4. Provision dedicated client `travelmate-admin-bot` (service-account flow);
   reject startup if `realm-admin` role detected — add to `KeycloakHealthIndicator`.
5. Unit test: replaying same `ExternalInvitationCreated` triggers
   `executeActionsEmail` exactly once (Mockito `verify(..., times(1))`).
6. Pen-test step (manual): submit a forged event for `victim@example.org`
   with no Account → assert no admin-API call (verify RabbitMQ trace +
   Keycloak admin-events log empty).

---

## Deployment Notes (DevOps)

### New Environment Variables

None. `KEYCLOAK_URL`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, and
Strato SMTP vars are already wired in `docker-compose.demo.yml`.

### Docker / Infrastructure Changes

None. The `re-login-notice.html` template lives inside the IAM JAR
(classpath resource). No volume mount needed. Keycloak `execute-actions-email`
is called container-internally at `http://keycloak:8080`.

### Database Migrations

If idempotency is DB-backed (recommended over Caffeine for replay-safety
across container restarts):
- `travelmate-iam/src/main/resources/db/migration/V8__external_invite_followups.sql`

Current highest IAM migration: `V7__trip_participation.sql`. Next free: V8.

### CI/CD Pipeline Changes

None. The IAM image is already in the build matrix. No new GitHub
Actions secrets.

### Container Restart Strategy

`docker compose up -d --remove-orphans` (existing `deploy-demo.sh`)
performs rolling replacement when the image digest changes. Only the
`iam` container is replaced. Flyway runs on startup and applies V8
automatically. No `--force-recreate` needed.

### External-Service Setup Steps

None. The Keycloak Admin API is internal. Verify the Keycloak admin
credentials in `DEMO_ENV_FILE` have sufficient realm-admin permissions
(true by default for the bootstrap admin account).

### Monitoring / Metrics

Add Micrometer counters to the routing layer:

- `travelmate.iam.invite.relogin_notice.sent` — incremented on successful
  re-login email send
- `travelmate.iam.invite.relogin_notice.failed` — incremented on Keycloak
  API error or SMTP error
- `travelmate.iam.invite.password_setup.sent` — incremented on successful
  Keycloak `execute-actions-email` call
- `travelmate.iam.invite.password_setup.failed` — incremented on
  Keycloak API error

Tag with `result=success|failure`. Verify delivery volume + detect silent
Keycloak Admin API breakage.

### Rollback

1. Revert the commit; pipeline re-builds and re-deploys the previous IAM
   image on the next push to main.
2. If V8 migration was applied: run
   `DELETE FROM flyway_schema_history WHERE version = '8'` and
   `DROP TABLE external_invite_followups` on `postgres-iam` before
   redeploying the rollback image (Flyway `validate` rejects schema
   ahead of code expectations).
3. No Keycloak data change — `execute-actions-email` is stateless on
   Keycloak side.

---

## Out of Scope

- Reworking the Trip-Invitation-Landing handler at `/trips/invitations/{id}`
  to be partially-anonymous (e.g. show trip teaser before login). Separate
  UX-improvement story for later iteration.
- Bulk-invite UI. This story only fixes the single-invite happy path.
- Generic Keycloak rate-limiting on `execute-actions-email` beyond the
  per-email idempotency check above. If abuse becomes systemic, that's
  a separate iter-21 hardening item.
