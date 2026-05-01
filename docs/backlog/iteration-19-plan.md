# Iteration 19 — Visibility and Integrity

**Target Version**: `v0.20.0`  
**Status**: PLANNED

> Note: The originally planned v0.19.0 release was reused as a demo-readiness
> release (theme refresh, mail hardening, env-driven Keycloak realm bootstrap,
> automated GitHub-Actions demo deployment). The iteration-19 scope below
> moves to v0.20.0 unchanged.

## Goal

Iteration 19 establishes the operational baseline for all later go-live work. The focus is to make the platform observable, reduce silent event-loss risk, and close the remaining obvious recipe-management gap.

## Planned Scope

### Functional stories

- recipe edit flow finalized and verified end to end
- recipe delete flow finalized and verified end to end
- recipe import from URL with SSRF-aware adapter behavior
- **S19-INVITE-EXISTING** — external invite for existing account no longer
  silently skips: send password-setup link if Keycloak credentials missing,
  otherwise send a re-login notice. See story detail below.
- **S19-UI-POLISH-DEMO-FEEDBACK** — three cosmetic UI bugs surfaced during
  the 2026-04-30 demo smoke (Keycloak-login logo background, accommodation
  poll mixed locale, edit/cancel button sizing). See story detail below.
- **S19-LANDING-WAITLIST** — public landing page on travelmate-demo.de
  with email wait-list signup + Plausible Analytics + Mailerlite
  integration. Stub only; full ACs to be defined at sprint planning.
  Drives the GTM Phase 0 validation step. See `docs/business/business-model-and-strategy.md` §8.

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
- S19-INVITE-EXISTING acceptance criteria below are met end to end
- S19-UI-POLISH-DEMO-FEEDBACK acceptance criteria below are met end to end

---

## Story Detail: S19-INVITE-EXISTING

### User Story

**As an** organizer  
**I want** an external invitation for an email that already has a Travelmate account to result in an actionable email for the invitee (instead of being silently skipped),  
**so that** invitees never end up at the Keycloak login screen with no path forward.

### Background

The current `ExternalInvitationConsumer` in IAM short-circuits if the email is already known:

```java
// travelmate-iam/.../adapters/messaging/ExternalInvitationConsumer.java:55-58
if (accountRepository.existsByUsernameAcrossTenants(new Username(event.email()))) {
    LOG.info("Account already exists for email {}, skipping creation", event.email());
    return;
}
```

Effect: the trip-invitation email (mail #1) is sent, but mail #2 (registration link)
never goes out. If the existing user has no Keycloak password set, or simply forgot
it, the trip-invitation link redirects to Keycloak login — a dead end.

Discovered while smoking the Hetzner demo on 2026-04-30: an externally invited
email that had been used in a prior demo signup attempt was stuck.

### Acceptance Criteria

1. **Given** an `ExternalUserInvitedToTrip` event arrives in IAM and the email
   already has an account, **when** the consumer detects this, **then** it must
   not return silently — it always emits one of two follow-up emails.

2. **Given** the existing Keycloak user has no credential of type `password`
   (e.g. account was created via a previous external-invite that never completed),
   **when** the consumer processes the event, **then** the consumer triggers
   Keycloak's `execute-actions-email` admin endpoint with `actions=["UPDATE_PASSWORD"]`
   so the invitee receives a password-setup email signed by Keycloak.
   - Log line: `Password setup link sent to {email}` at INFO level.

3. **Given** the existing Keycloak user already has a password credential set,
   **when** the consumer processes the event, **then** the consumer triggers a
   re-login notice email via `RegistrationEmailService` (or new
   `ReLoginNoticeEmailService`) telling the invitee that they were invited
   to trip X and should log in at `${TRAVELMATE_PUBLIC_URL}` with their existing
   credentials. The trip name and inviter must appear in the email.
   - Log line: `Re-login notice sent to {email} for trip {tripName}` at INFO level.

4. **Given** any branch above runs, **when** mail #1 (the InvitationEmailListener
   path in trips) also runs, **then** both emails arrive independently. Order
   between mail #1 and mail #2 is not guaranteed, both must be idempotent on
   replay.

5. **The trip invitation accept page** at `/trips/invitations/{id}` retains its
   current behavior — auth required, JWT subject must equal `inviteeId`. No
   change there. (The fix is upstream in mail flow, not in the landing route.)

6. **Tests**:
   - Three new unit tests in `ExternalInvitationConsumerTest`:
     a. New account → existing behavior preserved (regression guard)
     b. Existing account, no Keycloak password → password-setup branch
     c. Existing account, password set → re-login-notice branch
   - One new E2E scenario in `travelmate-e2e` that drives the entire flow
     against a real Keycloak (not mocked) — verify the user ends up logged in
     and able to accept a trip invitation after the password-setup branch.

7. **Migration concern**: existing skipped invitations from before the fix
   are not retroactively re-emailed. The behavior change applies only to
   newly arriving events.

### Technical Notes

- New collaborator: `KeycloakAdminClient.hasPasswordCredential(userId)` —
  inspects `GET /admin/realms/{realm}/users/{userId}/credentials` for any
  entry with `type=password`. The Keycloak admin token is already plumbed
  via the existing `KeycloakService` in IAM.
- New collaborator: `KeycloakAdminClient.sendUpdatePasswordEmail(userId)` —
  wraps `PUT /admin/realms/{realm}/users/{userId}/execute-actions-email`
  with payload `["UPDATE_PASSWORD"]`. Optional query param
  `redirect_uri=${TRAVELMATE_PUBLIC_URL}/trips/invitations/{invitationId}`
  so that after the password is set, Keycloak redirects the user back to the
  trip invitation acceptance page.
- New email template: `email/re-login-notice.html` (i18n keys in
  `messages_de.properties` and `messages_en.properties`).
- Refactor: extract a small `ExistingAccountInviteRouter` from
  `ExternalInvitationConsumer` so the branch logic is unit-testable without
  the full RabbitMQ wiring.

### Risks

- Keycloak `execute-actions-email` requires SMTP to be configured at the
  realm level. We already do this via `init-smtp-from-env.sh`, so this risk
  is low — but the test harness must mock or assume Mailpit/Strato as
  available.
- The `redirect_uri` for the password-setup email must be on the Valid
  Redirect URIs list of the `travelmate-gateway` Keycloak client. The
  current realm template has `${APP_BASE_URL}/*` which already covers it.

### Out of Scope

- Reworking the Trip-Invitation-Landing handler at `/trips/invitations/{id}`
  to be partially-anonymous (e.g. show the trip teaser before login). That's
  a separate UX-improvement story for a later iteration.
- Bulk-invite UI. This story only fixes the single-invite happy path.

---

## Story Detail: S19-UI-POLISH-DEMO-FEEDBACK

### User Story

**As a** demo viewer or first-time user,
**I want** the UI to feel consistent and polished — no off-color backgrounds,
no mixed-language labels, no awkward button-size mismatches —
**so that** the application feels production-ready, not draft.

### Background

Three cosmetic issues surfaced during the 2026-04-30 demo smoke on the
Hetzner-hosted demo at `https://travelmate-demo.de`:

1. **Login logo darker square** — the Keycloak login page rendered a faintly
   tinted square around the circular logo. Root cause: `.kc-card` had
   `background: rgba(255, 255, 255, 0.95)` (5% transparency), and the logo
   SVG has a 1px transparent ring around the blue circle (viewBox 40×40,
   circle r=19), so the page gradient bled through that ring more visibly
   than through the rest of the card.
   - **Already fixed in hotfix commit** (CSS: `.kc-card` set to opaque
     `#ffffff`) — this story documents the change for iteration tracking.

2. **Accommodation Poll mixed locale** — the create page at
   `/trips/{tripId}/accommodationpoll/create` shows the title in English
   (proper i18n key) but several sub-labels and helper texts are still in
   German. Root cause: `messages_en.properties` is missing translations for
   some `accommodationpoll.*` keys, so Thymeleaf falls back to the German
   default in the template.

3. **Edit Trip vs Cancel button size mismatch** — on the Trip Detail page,
   the "Edit Trip" link uses PicoCSS class `secondary outline`, while the
   "Cancel" button uses `contrast`. PicoCSS renders these with different
   padding/sizing, so the two side-by-side buttons look unbalanced.

### Acceptance Criteria

1. **Login logo**: in DevTools, `.kc-card` shows `background: #ffffff` (opaque),
   no visible darker square around the logo when viewed against the
   page-gradient backdrop. Visual regression: hotfix commit
   `docker/keycloak/themes/travelmate/login/resources/css/travelmate.css:141`.

2. **Accommodation Poll i18n**:
   - Audit every `#{accommodationpoll.*}` key in
     `travelmate-trips/src/main/resources/templates/accommodationpoll/create.html`
     (and related templates)
   - For each missing English translation, add the key to
     `travelmate-trips/src/main/resources/messages_en.properties`
   - Manual probe in the EN locale: every visible label is English; no
     German fallback text remains in the rendered DOM.

3. **Trip Detail buttons**: `Edit Trip` and `Cancel` (in `templates/trip/detail.html:34,50`)
   share the same height and padding. Either:
   - Both use the same PicoCSS class combination (e.g., both `secondary`), or
   - Custom CSS rule in `style.css` aligns padding/font-size for the two
     specific buttons.
   Visual probe: side-by-side rendering shows no perceptible size difference.

### Technical Notes

- The login-logo fix landed before this story is implemented; the AC is
  satisfied retrospectively. No further code change for that part — only
  documentation in this plan.
- Accommodation Poll audit should also check `messages.properties` (default,
  fallback to DE per Travelmate convention) for completeness, even though
  the visible bug is in the EN path.
- The Trip Detail button mismatch may exist in other detail pages too
  (Recipe-Detail, Accommodation-Detail). Story scope is **Trip Detail
  only** for now; other pages can be a follow-up if discovered.

### Out of Scope

- A repository-wide audit of all PicoCSS-class uses for size consistency.
  This story is reactive (3 specific findings), not proactive design-system
  work.
- Theme refresh or color-token changes. Existing tokens are kept.
