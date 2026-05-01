# Story Detail — S19-LANDING-WAITLIST

**Iteration**: 19 (Demo-Hardening + Phase-0-GTM)
**Estimated size**: M (10–20h)
**Status**: PLANNED
**Authored**: Cross-functional team-planning session 2026-04-30

---

## User Story (RE)

**As a** potential user visiting travelmate-demo.de
**I want** to enter my email address and opt into a wait-list
**so that** I am notified when Travelmate opens for public access, and the
team can validate early demand before go-live.

## Background

Drives GTM Phase 0 from `docs/business/business-model-and-strategy.md` §8.
Without a public landing page with email capture, there is no scalable
funnel for the customer-interview pipeline (described in
`docs/business/customer-interview-guide.md`). Currently `travelmate-demo.de`
redirects to `/iam/` (Sign-In page) — visitors who are not yet ready to
sign up have no entry point to leave their address.

## Acceptance Criteria (RE)

**Scenario 1: Valid submission with DSGVO consent**
Given the visitor is on `/iam/landing`
And the page renders a headline, product description, email input,
   explicit opt-in checkbox, and a privacy-policy link
When the visitor enters a valid email, checks the opt-in checkbox,
   and submits the form
Then the server POSTs the signup to the Mailerlite API
And Mailerlite triggers its double-opt-in confirmation email
And Plausible records a `Waitlist Signup` custom event
And the page displays an HTMX-swapped confirmation fragment without
   a full page reload

**Scenario 2: Missing DSGVO consent — rejected**
Given the visitor entered a valid email but did not check the opt-in box
When the visitor submits the form
Then the form does not submit to Mailerlite
And a validation message informs the visitor that consent is required
And no Plausible event is recorded

**Scenario 3: Invalid email format — rejected**
Given the visitor typed a string that is not a valid email
When the visitor submits the form
Then the server returns an HTMX error fragment ("Please enter a valid email address")
And no Mailerlite call is made
And no Plausible event is recorded

**Scenario 4: Duplicate email — enumeration-defense**
Given the visitor's email is already subscribed in Mailerlite
When the visitor submits the form again
Then the response shows the same confirmation message as Scenario 1
   (do not reveal whether the email is already registered — GDPR
   enumeration defense)

**Scenario 5: Mailerlite API unavailable**
Given Mailerlite returns a 5xx or times out (>5 s)
When the form is submitted
Then the server logs the error at ERROR level with the response body
And the visitor sees a user-friendly error message
And the form remains available for retry
And no Plausible event is recorded

**Scenario 6: GDPR deletion request**
Given a subscriber requests removal of their email
When the admin calls the Mailerlite `DELETE /subscribers/{id}` endpoint
Then the subscriber is removed from the wait-list group
And there is no other persistent store of the email in the Travelmate codebase

## Edge Cases / Error Scenarios (RE)

- **No JavaScript / bot submission**: form must function as plain HTML POST
  (progressive enhancement). Server-side validation mandatory regardless
  of client state.
- **Plausible script blocked by ad-blocker**: Plausible event silently
  not fired. Acceptable — Plausible is analytics only, not a gate.
- **Mailerlite API key leaked in client bundle**: API call must be
  server-side only. API key only via env var, never in HTML/JS.
- **Double-opt-in email ends up in spam**: outside system control, but the
  Mailerlite sender domain must have SPF/DKIM aligned with Strato.
- **CSRF on the public form**: this is a public page — the form endpoint
  must explicitly handle CSRF (either exempted with documented reason,
  or use synchronizer token).
- **Privacy policy link broken**: privacy page (`/datenschutz`) must
  exist at deploy time, even as a stub returning 200 with minimal DSGVO content.
- **Concurrent duplicate submissions** (double-click): Mailerlite upsert
  semantics handle this; HTMX `hx-disabled-elt="this"` on submit
  button prevents double POST.

---

## DDD / Architecture Notes (Architect)

- **Aggregate impact**: None in existing SCS. The wait-list is **not a
  domain concept** of IAM (it predates a tenant/account) and not of
  Trips/Expense. Treat it as a **pre-IAM, marketing-only subdomain** —
  Generic, not Core. Do **not** model `WaitlistEntry` as an IAM
  aggregate; that would couple marketing analytics to the multi-tenant
  billing-relevant boundary.
- **Architectural decision**: **Do not create a new `travelmate-landing`
  SCS.** Reasons: (a) no DB of its own — Mailerlite owns the list;
  (b) new SCS implies new PostgreSQL, Flyway, RabbitMQ wiring, CI deploy
  step — violates 1–3h/week pace constraint; (c) IAM already has public
  sign-up infrastructure (`@Profile("!test")` SecurityConfig with
  `permitAll` on `/iam/signup`, Thymeleaf, PicoCSS).
  **Decision: host inside IAM at `/iam/landing`.** Replace the gateway's
  current root `RedirectTo=302, /iam/` (application.yml line 19) with
  `/iam/landing` and add `/iam/landing*` to the `iam-public` route
  predicate list (line 23) and to the gateway `permitAll` list
  (`SecurityConfig.java:33`).
- **New events**: none. Mailerlite double-opt-in is fully handled by
  Mailerlite — no internal `WaitlistSignupRequested` event needed.
- **New ports / adapters**:
  - **Port** `WaitlistSubscriber` in `travelmate-iam/.../application/marketing/`
    — domain-free interface with `subscribe(EmailAddress, ConsentMarker)`.
  - **Adapter** `MailerliteSubscriberAdapter` in
    `travelmate-iam/.../adapters/mailerlite/` — uses `RestClient` to POST
    to Mailerlite's `/api/subscribers`. API key from env
    `MAILERLITE_API_KEY`. `@Profile("!test")` so tests don't hit the network.
  - **Controller** `LandingController` in `travelmate-iam/.../adapters/web/`
    — GET `/landing` (renders Thymeleaf), POST `/landing/waitlist` (HTMX
    form-submit, returns swap fragment).
  - **Plausible**: browser-side script tag in template, no backend integration.
- **Hexagonal conformity check**: PASS, with one caveat. Watch for
  **leakage of marketing concerns into the IAM domain** —
  `WaitlistSubscriber` must NOT live in `domain/`, only in `application/`.
  The wait-list email is not a Tenant, not an Account, never persists
  in IAM's DB. If a future story requires storing wait-list signups
  locally, revisit and likely promote to a new BC.
- **Reuse opportunities**:
  - SCS public-route pattern from gateway `application.yml:20-23`
    (`iam-public` route) — extend with `/iam/landing*`.
  - PicoCSS + Thymeleaf hero-layout pattern from existing
    `templates/signup.html` — use the same `section-card` wrapper from v0.15.x.
  - DSGVO-compliant explicit-opt-in checkbox pattern from
    `templates/signup.html`.
- **Risk + mitigation**: **DSGVO data-flow risk** — Mailerlite is
  US-hosted (Schrems-II). Mitigation: (a) require explicit checkbox
  with consent text mentioning "Mailerlite (USA)" + privacy-policy link;
  (b) document in arc42 §3 (Context & Scope) the new external system +
  Standard Contractual Clauses; (c) document deletion path. Architectural
  mitigation: keep Mailerlite calls behind `WaitlistSubscriber` port so
  a future EU-hosted alternative (Brevo, Listmonk) can be swapped in.

---

## UX Wireframes / Journey (UX Designer)

### Mobile wireframe (375px)

```
+------------------------------------------+
|  [Logo] Travelmate              [Menu]   |
+------------------------------------------+
|                                          |
|  [SVG logo — 48px centered]              |
|                                          |
|  Schluss mit WhatsApp-Chaos.             |  ← h1, 28px, bold
|                                          |
|  Travelmate bringt Reiseplanung,         |  ← p, 16px, muted
|  Abrechnung und Unterkunfts-             |
|  koordination an einen Ort —             |
|  ohne Gruppenchat-Wahnsinn.              |
|                                          |
|  +----- waitlist-form-card ────────+     |
|  | Frühzugang sichern              |     |  ← <article> card
|  |                                 |     |
|  | [E-Mail-Adresse eingeben      ] |     |  ← <input type=email>
|  |                                 |     |
|  | [ ] Ich bin einverstanden, dass |     |  ← <label> with checkbox
|  |   Travelmate mich per E-Mail    |     |
|  |   kontaktiert. Datenschutz-     |     |
|  |   hinweis →                     |     |
|  |                                 |     |
|  | [  Frühzugang anfordern  ]      |     |  ← <button> full-width
|  +----------------------------------+    |
|                                          |
|  ✦ Demo verfügbar: travelmate-demo.de   |  ← <small>, centered, muted
|                                          |
|  --- features section ---                |
|  (unchanged from existing landing.html)  |
+------------------------------------------+
|  Impressum · Datenschutz                |  ← footer <small>
+------------------------------------------+
```

### Desktop wireframe (1024px+)

```
+------------------------------------------------------------------+
| [Logo] Travelmate                            [Registrieren] [DE] |
+------------------------------------------------------------------+
|                                                                  |
|  [large SVG logo — 64px]                                         |
|                                                                  |
|  Schluss mit WhatsApp-Chaos.                                     |
|  Travelmate bringt Reiseplanung, Abrechnung und Unterkunfts-     |
|  koordination an einen Ort — ohne Gruppenchat-Wahnsinn.          |
|                                                                  |
|  +--- waitlist-form-card (max-width: 480px, centered) ---------+ |
|  |  Frühzugang sichern                                         | |
|  |  [E-Mail-Adresse eingeben                              ]    | |
|  |  [ ] Ich bin einverstanden ... Datenschutzhinweis →         | |
|  |              [ Frühzugang anfordern ]                       | |
|  +--------------------------------------------------------------+ |
|                                                                  |
|  ✦ Demo verfügbar unter travelmate-demo.de                       |
|                                                                  |
|  [feature grid — 3 columns, unchanged]                           |
+------------------------------------------------------------------+
| Impressum · Datenschutz · © Travelmate                           |
+------------------------------------------------------------------+
```

### Copy — German-first with English translation

| Element | DE | EN |
|---|---|---|
| `<h1>` | Schluss mit WhatsApp-Chaos. | No more WhatsApp chaos. |
| `<p>` hero | Travelmate bringt Reiseplanung, Abrechnung und Unterkunftskoordination an einen Ort — ohne Gruppenchat-Wahnsinn. | Travelmate brings trip planning, expenses, and accommodation into one place — no group-chat chaos. |
| Card header | Frühzugang sichern | Get early access |
| `<input>` placeholder | E-Mail-Adresse | Email address |
| Opt-in label | Ich bin einverstanden, dass Travelmate mich per E-Mail über den Start des Dienstes informiert. Mailerlite (USA) verarbeitet meine E-Mail im Auftrag von Travelmate. | I agree that Travelmate may contact me by email about the service launch. Mailerlite (USA) processes my email on Travelmate's behalf. |
| Privacy link | Datenschutzhinweis | Privacy notice |
| Button | Frühzugang anfordern | Request early access |
| Social proof | Demo verfügbar unter travelmate-demo.de | Demo available at travelmate-demo.de |
| `landing.waitlist.success` | Danke! Wir melden uns, wenn Travelmate startet. | Thanks! We'll be in touch when Travelmate launches. |
| `landing.waitlist.error` | Etwas ist schiefgelaufen. Bitte versuche es erneut. | Something went wrong. Please try again. |
| `landing.waitlist.duplicate` | Bereits eingetragen. Wir melden uns. | Already registered. We'll be in touch. |

### Form states and HTMX spec

```html
<form id="waitlist-form"
      hx-post="/iam/landing/waitlist"
      hx-target="#waitlist-result"
      hx-swap="innerHTML"
      hx-disabled-elt="button[type=submit]">

  <input type="email" name="email" required
         placeholder="#{landing.waitlist.emailPlaceholder}"
         autocomplete="email" />

  <label>
    <input type="checkbox" name="consentGiven" required />
    <span th:text="#{landing.waitlist.consent}">
      Ich bin einverstanden …
    </span>
    <a th:href="@{/datenschutz}" th:text="#{landing.waitlist.privacyLink}"
       target="_blank" rel="noopener">
      Datenschutzhinweis
    </a>
  </label>

  <!-- honeypot field, see Security STRIDE -->
  <input type="text" name="website" tabindex="-1" autocomplete="off"
         style="position:absolute;left:-9999px;" />

  <button type="submit" th:text="#{landing.waitlist.submit}">
    Frühzugang anfordern
  </button>

  <div id="waitlist-result" role="status" aria-live="polite"></div>
</form>
```

**State transitions**:

- **Idle**: form visible, button enabled.
- **Loading**: `hx-disabled-elt` disables button; add `aria-busy="true"`
  on the button if request latency may exceed 800ms.
- **Success**: server returns success fragment; HTMX swaps `#waitlist-result`
  innerHTML. Form inputs remain visible (user can share with another email).
- **Error**: server returns error fragment with `role="alert"`; same swap target.
- **Duplicate email**: server returns the `landing.waitlist.duplicate`
  variant — same visual treatment as success (enumeration defense).

### i18n keys to add (IAM `messages*.properties`)

```
landing.waitlist.title
landing.waitlist.emailPlaceholder
landing.waitlist.consent
landing.waitlist.privacyLink
landing.waitlist.submit
landing.waitlist.success
landing.waitlist.error
landing.waitlist.duplicate
```

---

## BDD Scenarios / Test Plan (QA)

### Test pyramid distribution

- **Unit tests (2)**:
  - `MailerliteSubscriberAdapterTest` — WireMock stub for Mailerlite REST API
  - `WaitlistCommandValidationTest` — value object rejects blank email,
    missing consent
- **Integration / Slice tests (2)**:
  - `LandingControllerTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` +
    `@ActiveProfiles("test")` + `@MockitoBean MailerliteSubscriberAdapter`):
    GET renders form, POST success swap, POST without consent rejects,
    POST duplicate enumeration-safe
  - `MailerliteSubscriberAdapterWireMockIT` (`@SpringBootTest` + WireMock
    rule stubbing `POST /api/subscribers`)
- **E2E (1)**: `WaitlistIT` fills form on `/iam/landing`, submits, asserts
  HTMX success swap

### Feature file: `waitlist-signup.feature`

Location: `travelmate-iam/src/test/resources/features/`

```gherkin
Feature: Waitlist sign-up on landing page
  As a visitor
  I want to register my interest via the waitlist form
  So that I am notified when Travelmate opens publicly

  Background:
    Given the visitor is on "/iam/landing"

  @happy-path
  Scenario: Valid submission with DSGVO consent
    Given the visitor enters email "new@example.com"
    And the visitor checks the DSGVO opt-in checkbox
    When the visitor submits the waitlist form
    Then the HTMX response swaps in a success confirmation fragment
    And Mailerlite receives a subscriber POST for "new@example.com"
    And no full page reload occurs

  @validation
  Scenario: Missing DSGVO consent — rejected
    Given the visitor enters email "no-consent@example.com"
    And the visitor does NOT check the DSGVO opt-in checkbox
    When the visitor submits the waitlist form
    Then the response status is 400
    And the HTMX swap shows a consent-required error message
    And Mailerlite is not called

  @validation
  Scenario: Blank email — rejected
    Given the visitor leaves the email field empty
    And the visitor checks the DSGVO opt-in checkbox
    When the visitor submits the waitlist form
    Then the response status is 400
    And an email-required error is shown

  @security
  Scenario: Duplicate email — enumeration-defense
    Given "existing@example.com" is already subscribed in Mailerlite
    And the visitor enters email "existing@example.com"
    And the visitor checks the DSGVO opt-in checkbox
    When the visitor submits the waitlist form
    Then the HTMX response shows the same success treatment as a new subscriber
    And the response does NOT reveal that the email already exists

  @negative
  Scenario: Mailerlite API unavailable
    Given the Mailerlite API returns a 503
    And the visitor enters email "retry@example.com"
    And the visitor checks the DSGVO opt-in checkbox
    When the visitor submits the waitlist form
    Then the HTMX swap shows a generic "try again later" message
    And the HTTP status is 200 (HTMX error fragment, not a 5xx)

  @security
  Scenario: Honeypot field filled — silently dropped
    Given the visitor enters email "bot@example.com"
    And the visitor fills the hidden "website" honeypot field
    When the visitor submits the waitlist form
    Then the HTMX response shows the success fragment
    And Mailerlite is not called

  @security
  Scenario: Public access without authentication
    Given the visitor is not logged in
    When the visitor navigates to "/iam/landing"
    Then the page returns HTTP 200
    And the waitlist form is rendered
```

### Test-doubles / mocking strategy

- `MailerliteSubscriberAdapter`: `@MockitoBean` in `LandingControllerTest`;
  WireMock stub in `MailerliteSubscriberAdapterWireMockIT`
- No RabbitMQ involvement — test profile already disables it

### Files to create

- `travelmate-iam/src/test/java/.../adapters/web/LandingControllerTest.java`
- `travelmate-iam/src/test/java/.../adapters/mailerlite/MailerliteSubscriberAdapterWireMockIT.java`
- `travelmate-iam/src/test/resources/features/waitlist-signup.feature`
- `travelmate-e2e/src/test/java/.../WaitlistIT.java`

---

## Threat Model (Security — STRIDE)

| STRIDE | Threat | Mitigation |
|---|---|---|
| **Spoofing** | Botnet submits 100k forged emails to pollute waitlist + exhaust Mailerlite quota / damage sender reputation | (1) hCaptcha (EU-hosted) or Friendly Captcha on the form, validated server-side. (2) Honeypot field `<input name="website" hidden>` — non-empty submission → silently 200, no API call. (3) Mailerlite double-opt-in: forged addresses self-purge if confirmation never arrives. |
| **Tampering** | Mailerlite list-id / API key manipulated via configuration injection → emails routed to attacker list | Bind `mailerlite.api-key` and `mailerlite.list-id` via `@ConfigurationProperties("mailerlite")` with `@NotBlank` validation. Log list-id at startup (not the key). Pin API base URL `https://connect.mailerlite.com/api/`. |
| **Repudiation** | "I never signed up" — user denies subscribing, or insider claims user did | Mailerlite double-opt-in covers consent proof (Art. 7 DSGVO). Persist `WaitlistSignupAttempted(emailHash, ipHash, userAgentHash, timestamp)` in IAM (hash with rotating salt, GDPR-minimized) for 90 days for abuse forensics. |
| **Information Disclosure** | (a) Mailerlite breach exposes waitlist. (b) Plausible script leaks visitor IPs to non-EU CDN. (c) Server error reveals "email already on waitlist" → enumeration | (a) Treat Mailerlite as low-trust; collect only email + locale, no name/IP. (b) Self-host Plausible script via `/iam/p.js` proxy or use EU-hosted Plausible install. (c) Always respond `200 OK` with generic success message regardless of duplicate / API failure. |
| **Denial of Service** | Form endpoint hit at 1000 req/s exhausts Mailerlite rate-limit (120 req/min) + IAM thread pool | Rate-limit at gateway: per-IP `5 req / 10 min` on `POST /iam/landing/waitlist`. Use Spring Cloud Gateway `RequestRateLimiter` (Redis or in-memory bucket4j filter). Circuit-breaker on Mailerlite client (Resilience4j) with 3s timeout. |
| **Elevation of Privilege** | Landing endpoint accidentally exposed under `/iam/**` permitAll matcher and inherits CSRF-disabled config — malicious site auto-POSTs visitor's email to grow list | Either re-enable CSRF for the public form path specifically, or enforce: (i) `Origin`/`Referer` header check matching gateway host; (ii) `Content-Type: application/x-www-form-urlencoded` only; (iii) hCaptcha already provides anti-CSRF guarantee per request. |

### DSGVO / Privacy Notes

- **Data**: email + locale + (hashed) IP for abuse log. Lawful basis:
  Art. 6(1)(a) consent via double-opt-in.
- **Mailerlite is US-hosted** → 3rd country transfer per Art. 44ff. Required:
  - **DPA / AVV** signed with Mailerlite Inc. (Mailerlite publishes a
    standard SCC-2021/914-compliant DPA — link in `docs/security/dpa/`).
  - Datenschutzhinweis section "Newsletter / Warteliste" listing
    Mailerlite + EU→US transfer + SCCs + revocation link.
- **Plausible**: EU-hosted (Hetzner DE/FI), no cookies, no PII →
  Art. 6(1)(f) legitimate interest, no consent banner needed if confirmed cookieless.
- **Deletion**: Mailerlite-managed token + admin-side deletion via
  Mailerlite UI (Auskunfts-/Löschanfrage SLA 30 days, Art. 17).
- **Add to** `docs/security/dsgvo-verarbeitungsverzeichnis.md` (Art. 30
  records of processing) — new entry "Waitlist Pre-Launch Marketing".

### Concrete Action Items for the Sprint

1. `LandingController.submitWaitlist(@Valid WaitlistForm form)` with
   `@Email` + hCaptcha verifier; controller test with `@MockitoBean`.
2. New `MailerliteSubscriberAdapter` using `RestClient` with 3s timeout +
   Resilience4j circuit-breaker; secret from `MAILERLITE_API_KEY` env.
3. Update gateway `SecurityConfig`: permit `/iam/landing`,
   `/iam/landing/waitlist` but **add Origin-header check filter** for POST.
4. Add hCaptcha sitekey/secret to `application-prod.yml` template
   (env-overridable); skip in `@Profile("test")`.
5. Sign DPA with Mailerlite + archive in `docs/security/dpa/`. Hard
   prerequisite — owner: legal.
6. Update `docs/legal/datenschutz.html` (Thymeleaf fragment) before merge —
   Mailerlite + Plausible sections.
7. E2E: submit form → 200 OK with success fragment; submit twice with
   same email → still 200 OK (no enumeration); honeypot fill → 200 OK
   but `MailerliteSubscriberAdapter` not invoked.

---

## Deployment Notes (DevOps)

### New Environment Variables

| Var name | Purpose | Sample value | Secret? |
|---|---|---|---|
| `MAILERLITE_API_KEY` | Mailerlite API v2 token for subscriber creation | `eyJ...` (64-char token) | YES — add to GitHub `demo` env secret `DEMO_ENV_FILE` |
| `MAILERLITE_GROUP_ID` | Mailerlite group/segment to subscribe into | `12345678` | No (optional, non-sensitive) |

### Docker / Infrastructure Changes

No new containers. Both vars injected into the `iam` service environment
block in `docker-compose.demo.yml`:

```yaml
iam:
  environment:
    MAILERLITE_API_KEY: ${MAILERLITE_API_KEY}
    MAILERLITE_GROUP_ID: ${MAILERLITE_GROUP_ID:-}
```

`LandingController` at `/iam/landing` is public (no auth). Caddy routes
all `/iam/*` traffic to gateway → IAM. No Caddy rule change needed.
Plausible is browser-side only.

### Database Migrations

None. Wait-list signup is fire-and-forget outbound API call. No local
persistence of subscriber email. (Hashed abuse log — if implemented per
Security Repudiation mitigation — would be Flyway V8 in IAM, parallel to
S19-INVITE-EXISTING's V8; coordinate during implementation.)

### CI/CD Pipeline Changes

- Add `MAILERLITE_API_KEY` to GitHub Actions `demo` environment secrets.
- Append to `.env.demo.travelmate-demo.example`:

```
# Mailerlite waitlist
MAILERLITE_API_KEY=replace-with-mailerlite-api-key
MAILERLITE_GROUP_ID=
```

- Re-paste updated `DEMO_ENV_FILE` secret. No workflow YAML change needed.

### Container Restart Strategy

`docker compose up -d` suffices. Only `iam` image changes; new env vars
require container recreation, which compose handles automatically when
env file changes. No `--force-recreate` flag needed.

### External-Service Setup Steps

**Mailerlite onboarding (one-time manual, before first deploy):**

1. Create free Mailerlite account at mailerlite.com using
   `noreply@travelmate-demo.de` sender address.
2. Verify sender domain `travelmate-demo.de` in Mailerlite Settings →
   Domains (add SPF/DKIM records to Strato DNS).
3. Create group `Travelmate Waitlist` via Mailerlite dashboard →
   Subscribers → Groups → Add Group.
4. Note the numeric group ID from the URL.
5. Generate API token: Mailerlite → Integrations → API → Create token
   with `subscribers:write` scope.
6. Store token as `MAILERLITE_API_KEY` GitHub Actions secret.
7. Store group ID as `MAILERLITE_GROUP_ID` in env file.

**Plausible Analytics — register custom event:**

1. Plausible dashboard for travelmate-demo.de → Goals → Add Goal →
   Custom Event.
2. Event name: `Waitlist Signup` (frontend JS calls `plausible('Waitlist Signup')`
   on successful form submission).
3. No server-side change needed.

### Monitoring / Metrics

Add to `MailerliteSubscriberAdapter`:

- `travelmate.iam.waitlist.subscribe.success` — counter on HTTP 200/201
- `travelmate.iam.waitlist.subscribe.failure` — counter with tag
  `reason=api_error|timeout|duplicate`

Expose `management.endpoint.health` indicator that fails-fast at startup
if `MAILERLITE_API_KEY` is missing.

### Rollback

1. Revert commit; pipeline redeploys previous IAM image. The
   `MAILERLITE_API_KEY` env var will be present but ignored — no harm.
2. Mailerlite subscriber records created before rollback remain in
   Mailerlite (no cleanup needed — they are marketing opt-ins).
3. No DB migration to revert.

---

## Out of Scope

- Storing subscriber emails locally in IAM DB. Mailerlite is the
  source-of-truth. Future story if compliance audit requires local copy.
- Multi-language landing page rotation beyond DE/EN.
- A/B-testing different headlines (would need analytics infra; out for
  iter-19).
- SEO-optimized variants of the landing page for specific keywords. That
  is `pSEO` work for a later iteration.
- Automatic Mailerlite-to-Notion sync of waitlist for further analytics.
