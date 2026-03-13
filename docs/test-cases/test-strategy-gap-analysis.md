# Test Strategy Gap Analysis

**Date**: 2026-03-13
**Context**: 8 bugs found in manual testing, none caught by 265 automated tests

---

## The Honest Answer

The 265 tests are technically sound but cover the wrong things at the wrong level. They verify that individual building blocks work in isolation. They do not verify that the assembled system does what a user actually experiences. The gap is not a failure of test quality — it is a failure of test strategy.

The core problem: **every single one of the 8 bugs lives in the space between components, or in the interaction between the system and its infrastructure.** The test pyramid has a solid base and a missing top.

---

## What the Tests Actually Cover

### Domain tests (unit)
Value object validation, aggregate invariants, business rule enforcement. These are correctly placed and correctly written. They would catch logic bugs inside the domain model.

**What they cannot catch**: anything that involves the UI, the database, external services, configuration, or how components connect.

### Application service tests (Mockito)
Each service method is tested with mocked dependencies. Correct return values are verified, event publishing is verified.

**What they cannot catch**: the interaction between the service and the real infrastructure (Keycloak, mail, database). Mocks always succeed. They model the happy world, not the real world.

### Persistence adapter tests (@SpringBootTest + H2)
Repository methods are tested against an in-memory H2 database.

**What they cannot catch**: PostgreSQL-specific behavior, connection issues, Docker networking. H2 is not PostgreSQL.

### Controller tests (@SpringBootTest + MockMvc)
HTTP request/response cycles are tested with mocked services. Status codes, view names, and model attributes are verified.

**What they cannot catch**: the actual rendered HTML that reaches the browser, HTMX behavior in a real browser, JavaScript execution, CSS layout, whether a button is actually visible and clickable. MockMvc returns a template name, not a rendered page. A `<button>` hidden inside a `<details>` element passes MockMvc tests because MockMvc only checks `view().name("trip/detail")`.

### E2E tests (Playwright, 6 test classes)
`SignUpIT`, `AuthenticationFlowIT`, `LandingPageIT`, `NavigationIT`, `DashboardMemberIT`, `TripLifecycleIT`.

These run against real infrastructure and are the closest to user reality. However:
- `signUpAndLogin()` in `E2ETestBase` contains a known workaround for the verification flow (lines 100-113): it navigates the verification link, then re-enters credentials if still on Keycloak. This **hides bug #1** rather than asserting the correct behavior.
- The `DashboardMemberIT` has a test `dangerZoneShowsDeleteButton()` that only asserts the button is visible. It **does not** click the button and verify what happens. Bug #3 is invisible to this test.
- No E2E test covers the invitation email flow end-to-end (invite external → receive email → click link → complete registration).
- No E2E test asserts that any action produces a success or error feedback message visible to the user.
- No E2E test runs against a Docker Compose environment with the mail service misconfigured.

---

## Bug-by-Bug Breakdown

### Bug 1: Email verification shows two extra Keycloak pages

**Root cause**: Keycloak email verification, by default, redirects to a "verify your email" intermediate page, then a "you may proceed" page, before allowing login. The Keycloak login theme was customized but the `VERIFY_EMAIL` required action flow was not configured to skip these intermediary pages.

**Why tests missed it**: `signUpAndLogin()` in `E2ETestBase` explicitly handles this with a conditional workaround at lines 100-113. Instead of asserting `page.url().contains("/iam/dashboard")` directly after clicking the verification link, the method patches around the Keycloak detour. The E2E test was written to be robust, which accidentally masked the UX problem.

**What would have caught it**: An E2E assertion:
```
After clicking the verification link,
page.url() should immediately contain "/oauth2" or Keycloak login,
NOT contain "login-actions/action-token" or "verify" intermediate pages.
```

### Bug 2: Password reset link lands on dashboard without changing password

**Root cause**: Keycloak's built-in password reset uses `UPDATE_PASSWORD` required action. After the user sets a new password, Keycloak redirects back through the Gateway OIDC flow, which completes login and sends the user to the configured post-login redirect URI (the IAM dashboard). The password was changed, but the user never sees a confirmation, and the flow bypasses their expectation of "I clicked reset, I should now be on a form to type a new password and confirm."

**Why tests missed it**: `AuthenticationFlowIT` tests the "Forgot Password" page navigation and branding but stops at verifying the form fields are present (Order 20-23). No test clicks "Submit" with a real email, retrieves the reset email from Mailpit, clicks the link, and asserts that the user lands on a password-change form (not the dashboard).

**What would have caught it**: A full E2E test of the password reset journey.

### Bug 3: Tenant deletion silently fails

**Root cause**: When `DELETE /dashboard/tenant` is called, the `deleteTenant()` endpoint in `DashboardController` sets `HX-Redirect: /logout`. If `tenantService.deleteTenant()` throws (e.g., a downstream exception from Keycloak or the event publisher), the exception propagates up and the `HX-Redirect` header is never set. The `GlobalExceptionHandler` returns an error fragment, but the dashboard template has `<div id="delete-error">` as the HTMX target. Whether the error fragment actually renders into that `<div>` depends on correct HTMX target resolution. The E2E test `dangerZoneShowsDeleteButton()` never executes the delete.

**Why tests missed it**:
1. `TenantServiceTest.deleteTenantPublishesEvent()` tests the service in isolation with mocks that succeed. It never tests what happens when Keycloak throws.
2. `DashboardControllerTest` has no test for `DELETE /dashboard/tenant` at all.
3. `DashboardMemberIT` only asserts the button exists (`isVisible()`), never clicks it.
4. `GlobalExceptionHandlerTest` tests error handling in isolation but does not test that the error fragment renders correctly in the HTMX context where the delete button lives.

**What would have caught it**: An E2E test that clicks the delete button, confirms the dialog, and then asserts: either the user is redirected to the logout flow, or a visible error message appears in `#delete-error`.

### Bug 4: "Per E-Mail einladen" hidden inside details element, no feedback after submit

**Root cause**: The `trip/detail.html` template has the external invitation form inside a section that can be styled or structured in ways that make it invisible or non-obvious. The "success feedback" JavaScript in `hx-on::after-request` references `document.getElementById('external-invite-feedback')` and `document.getElementById('msg-invite-sent')`, which both exist in the template but the feedback `<p>` has `style="display:none"` and is only shown if `event.detail.successful` is true — if HTMX reports the request as unsuccessful (e.g. due to a server error), the display block is never executed.

**Why tests missed it**:
1. Controller tests use MockMvc, which never executes JavaScript. The `hx-on::after-request` handler is invisible to MockMvc.
2. No E2E test submits the external invitation form and asserts that feedback is shown.
3. No E2E test verifies that the form section is immediately visible (not hidden behind a `<details>` toggle).

**What would have caught it**: An E2E test that navigates to the trip detail, asserts the "Per E-Mail einladen" section is visible without additional clicks, fills the form, submits it, and asserts a success message is visible.

### Bug 5: Stay period save button grows after submit (PicoCSS layout)

**Root cause**: PicoCSS `.grid` treats children as equal-width columns. Inside a `<div style="display:flex; gap:0.5rem; ...">`, the submit button is sized by the flex container, but when PicoCSS grid styles are inherited from parent `<figure>` or `<table>` elements, the button width can expand beyond its content. When the label text wraps, the button grows to fill available space.

**Why tests missed it**: This is a pure CSS/layout bug. No level of testing except visual E2E catches it. The existing `TripLifecycleIT` has `setStayPeriodForOrganizer()` which fills dates and clicks submit, but it does not assert the button's dimensions or that text does not wrap. Layout assertions are absent across all E2E tests.

**What would have caught it**: A visual regression test (screenshot diff), or an E2E assertion checking the computed width/height of the button element, or a simple assertion that the button's `offsetWidth` equals its `scrollWidth` (no wrapping).

### Bug 6: Mail not sending in Docker (wrong hostname)

**Root cause**: IAM `application.yml` configures `spring.mail.host=localhost`. In a Docker Compose network, `localhost` refers to the container itself, not the Mailpit container. The correct value is the Docker service name (e.g. `mailpit`). The application silently swallows `MailConnectException` or logs it without surfacing to the user.

**Why tests missed it**: All tests (including E2E) use `application.yml` values overridden for the test environment. The E2E base class queries Mailpit at `http://localhost:8025` (the host port). No test verifies that the mail is actually sent by the IAM container connecting to Mailpit using the container's DNS name. The E2E tests that call `getVerificationLinkFromMailpit()` do query Mailpit, but they tolerate failures (the method catches all exceptions and returns `null`), so a missing email does not fail the test.

**What would have caught it**: An E2E test that:
1. Submits the invite-member form.
2. Calls `getVerificationLinkFromMailpit(email)` and asserts the result is not null (fail fast if no email arrives within timeout).
3. Follows the link.

The current implementation swallows the null and continues. Asserting `assertThat(verificationLink).isNotNull()` in `signUpAndLogin()` would have caused the test to fail immediately when run inside Docker.

### Bug 7: MailSendException not caught in @TransactionalEventListener (Trips)

**Root cause**: `InvitationEmailListener` (or equivalent) is annotated `@TransactionalEventListener(phase = AFTER_COMMIT)`. If `mailSender.send()` throws `MailSendException`, the exception propagates out of the listener. Spring's `@TransactionalEventListener(AFTER_COMMIT)` runs after the transaction has committed, so there is no transaction to roll back — but the exception still propagates to the caller thread and appears as an error in the response if the listener runs synchronously. The user sees no feedback because the HTTP response has already been returned, but logs fill with stack traces.

**Why tests missed it**: `InvitationServiceTest` mocks all dependencies including the event publisher. It verifies that `publishEvent(InvitationCreated)` is called, but it never tests what happens when the downstream listener throws. The `@TransactionalEventListener` is not active in the `test` profile (the messaging adapter is disabled). No test exercises the integration between the event publication, the listener, and the mail service failure path.

**What would have caught it**: An integration test (not mocked) that starts a real Spring context with a mail server configured to reject connections, invites a participant, and asserts that: the invitation is still saved in the database (the primary transaction committed), and the exception from the mail listener does not bubble up to the user as an unhandled error.

### Bug 8: General pattern - errors swallowed, no user feedback

**Root cause**: The `GlobalExceptionHandler` correctly returns error fragments for HTMX requests. However, several action endpoints catch specific exceptions internally (see `DashboardController.deleteMember()`, `inviteMember()`) and put the error into the model, but the fragment returned must have the correct Thymeleaf fragment that actually renders the `memberError` attribute visibly. No test verifies that `memberError` produces visible text in the browser.

**Why tests missed it**: `DashboardControllerTest.inviteMemberWithDuplicateEmailShowsError()` correctly asserts `model().attributeExists("memberError")`. But `model().attributeExists()` only checks that the model contains the key — it does not check the rendered HTML, and it does not check that the template fragment displays it as visible text to the user. A template bug (wrong Thymeleaf expression, wrong `th:if`, wrong element) would pass this test.

**What would have caught it**: An E2E test that triggers a duplicate invite and asserts `page.locator(".error, [role='alert'], #memberError").isVisible()`.

---

## Summary of Missing Test Types

| Missing Test Type | Bugs it would have caught |
|---|---|
| E2E: Full email verification flow with strict assertions (no workarounds) | #1 |
| E2E: Full password reset flow (submit form → receive email → click link → change password → land on correct page) | #2 |
| E2E: Delete tenant (click button → accept confirm → assert redirect or visible error) | #3 |
| E2E: External invite form (assert section visible without extra clicks → submit → assert feedback message visible) | #4 |
| E2E: Visual/layout assertion on stay period button (no text wrap) | #5 |
| E2E: Email delivery assertion (assertThat(verificationLink).isNotNull()) | #6 |
| Integration: TransactionalEventListener with mail failure (assert graceful degradation) | #7 |
| E2E: Error feedback for all user-facing actions (assert error text is visible in browser) | #8 |

---

## Recommended Test Strategy Improvements

### 1. Fix the E2E base class to assert, not work around

The `signUpAndLogin()` method currently tolerates the broken verification flow. It should assert the flow is correct:
```java
// After clicking the verification link, the browser must land on the Keycloak login page,
// not on an intermediate "verify" page
page.navigate(verificationLink);
page.waitForLoadState();
assertThat(page.url()).doesNotContain("login-actions/action-token");
```

### 2. Add a mandatory email delivery assertion

`getVerificationLinkFromMailpit()` returns `null` on failure and logs a warning. It must throw so that tests fail when mail is not delivered:
```java
assertThat(getVerificationLinkFromMailpit(email))
    .as("Verification email must arrive in Mailpit within timeout")
    .isNotNull();
```

### 3. Write E2E tests for every destructive action

Every button that causes a state change must have an E2E test that:
- Clicks the button
- Asserts the success outcome (redirect, updated UI, success message)
- In a separate test: simulates failure and asserts a visible error message

### 4. Write E2E tests for every form feedback scenario

After every form submit, assert one of:
- A success indicator is visible (toast, updated list, redirected page with expected content)
- Or a validation/error message is visible if the submission was invalid

### 5. Add an integration smoke test for Docker networking

Add a test that verifies the mail host configuration resolves correctly from within the application container. At minimum, add an E2E health-check test that sends an actual email and asserts receipt in Mailpit.

### 6. Cover @TransactionalEventListener failure modes

For every `@TransactionalEventListener`, add an application-level integration test (not mocked) that exercises the failure path: the primary business operation must succeed even if the side-effect (email, RabbitMQ) fails.

---

## What Should Be Prioritized First

The bugs that affect every single user every single time they first use the application:
1. Bug #1 (verification flow) and Bug #6 (mail not sending in Docker) block signup entirely.
2. Bug #3 (silent delete failure) destroys user trust in any action.
3. Bug #8 (general silent failures) is a systematic problem that will keep producing new discovered bugs.

Fix these three categories first. The BDD scenarios in `docs/test-cases/bdd/` document the precise user journey expectations that tests must enforce.
