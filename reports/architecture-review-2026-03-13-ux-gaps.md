# Architecture Review: UX Failure Root Cause Analysis

**Date**: 2026-03-13
**Scope**: Cross-cutting UX concerns across all SCS (IAM, Trips, Gateway)
**Trigger**: User complaint: "Users click buttons but nothing happens, no feedback"

## 1. Review Scope

This review evaluates the architectural gaps that manifest as user-facing bugs,
specifically the absence of feedback patterns, fragile event-driven side effects,
and confusing UI information architecture on the trip detail page.

Files analyzed:
- `travelmate-iam/src/main/java/.../adapters/web/GlobalExceptionHandler.java`
- `travelmate-trips/src/main/java/.../adapters/web/GlobalExceptionHandler.java`
- `travelmate-trips/src/main/java/.../adapters/messaging/DomainEventPublisher.java`
- `travelmate-iam/src/main/java/.../adapters/messaging/DomainEventPublisher.java`
- `travelmate-trips/src/main/java/.../adapters/mail/InvitationEmailListener.java`
- `travelmate-iam/src/main/java/.../adapters/mail/RegistrationEmailService.java`
- `travelmate-trips/src/main/resources/templates/trip/detail.html`
- `travelmate-trips/src/main/resources/templates/trip/invitations.html`
- `travelmate-trips/src/main/resources/templates/layout/default.html`
- `travelmate-trips/src/main/resources/templates/fragments/error.html`
- `travelmate-gateway/src/main/java/.../SecurityConfig.java`
- `docker-compose.yml`
- All `application.yml` files

## 2. Findings

### Finding 1: No HTMX Response Protocol (CRITICAL)

**Problem**: The architecture chose HTMX for partial updates (ADR-0004) but never
designed a cross-cutting protocol for how HTMX responses communicate success/failure
to the user.

**Evidence**:
- `layout/default.html` has no toast container or notification area
- `fragments/error.html` returns a bare `<div>` with no positioning or auto-dismiss
- HTMX forms define `hx-target` for content areas, but error fragments replace that content
- No success notification mechanism exists anywhere in the codebase
- External invite form uses inline JavaScript (`hx-on::after-request`) for feedback

**Impact**: Users perform actions with no visible result. The "did it work?" uncertainty
is the primary source of frustration.

**Root Cause**: The architecture was designed domain-inward (aggregates, events, contexts)
but the user-outward direction (response protocol, feedback UX) was never formalized
as an architectural concern.

### Finding 2: Unprotected Event Publishers (HIGH)

**Problem**: `DomainEventPublisher` classes in both IAM and Trips have no exception
handling. Since Spring 6+, `@TransactionalEventListener(AFTER_COMMIT)` propagates
exceptions back to the caller.

**Evidence**:
- IAM `DomainEventPublisher`: 7 `@TransactionalEventListener` methods, 0 try-catch blocks
- Trips `DomainEventPublisher`: 4 `@TransactionalEventListener` methods, 0 try-catch blocks
- Trips `InvitationEmailListener`: HAS try-catch (correctly)
- IAM `RegistrationEmailService`: HAS try-catch (correctly)

**Impact**: If RabbitMQ is temporarily unreachable, users see a 500 error for operations
that actually succeeded (data is committed). This is the "click button, see error,
but the data was saved" bug. If the user retries, they may get a `DuplicateEntityException`.

**Root Cause**: The event publishing pattern was designed for data consistency (documented
in arc42 section 11 as "Event Loss on Publish" risk) but the UX impact was not considered.

### Finding 3: Duplicated Exception Handlers (MEDIUM)

**Problem**: `GlobalExceptionHandler` in IAM and Trips are 77-line identical copies.

**Evidence**:
- Both handle: `EntityNotFoundException`, `DuplicateEntityException`,
  `BusinessRuleViolationException`, `RuntimeException`
- Neither logs exceptions
- Both pass `ex.getMessage()` raw to the user (no i18n, leaks implementation details)
- Neither handles `ResponseStatusException` (used by `TripController` for 403)

**Root Cause**: Error handling was not designed as a cross-cutting concern. Each SCS
independently discovered the need and built the same solution.

### Finding 4: Trip Detail Page Information Architecture (MEDIUM)

**Problem**: The trip detail page presents trip info, participants, invitation list,
member invite form, and external invite form as a single vertical stream with no
visual hierarchy.

**Evidence** (detail.html):
- Three `<section>` elements stacked vertically
- Invite forms always visible even when no members are invitable
- External invite form has 4 fields (email, firstName, lastName, dateOfBirth) always visible
- No `<article>`, `<fieldset>`, or `<details>` grouping for visual separation
- PicoCSS relies on semantic elements for styling, but forms without wrappers blend in

### Finding 5: No Observability in Error Paths (MEDIUM)

**Problem**: Exceptions that reach the `GlobalExceptionHandler` are converted to user-facing
pages/toasts but never logged. Backend failures are invisible to operators.

**Evidence**: Zero `LOG.error()` or `LOG.warn()` calls in either `GlobalExceptionHandler`.

## 3. Conformance Check

| Invariant | Status | Notes |
|-----------|--------|-------|
| Domain layer framework-free | PASS | Verified in both SCS |
| TenantId aggregate isolation | PASS | All queries scoped by TenantId |
| Async SCS communication | PASS | RabbitMQ only, no direct HTTP |
| Database isolation per SCS | PASS | 4 separate PostgreSQL instances |
| Flyway schema ownership | PASS | ddl-auto=validate in all SCS |
| Server-side rendering | PASS | Thymeleaf + HTMX, no SPA |
| Event publishing pattern | PARTIAL FAIL | Pattern correct but no error resilience |
| Shared Kernel purity | PASS | Only primitives and event contracts |

## 4. Risk Assessment

| ID | Risk | Impact | Likelihood | Severity |
|----|------|--------|------------|----------|
| R1 | HTMX requests fail silently (no user feedback) | High | Very Likely | CRITICAL |
| R2 | Event publisher exceptions crash caller | High | Likely | HIGH |
| R3 | No logging in error handlers | Medium | Very Likely | HIGH |
| R4 | Raw exception messages shown to users | Medium | Likely | MEDIUM |
| R5 | Trip detail page usability | Medium | Very Likely | MEDIUM |
| R6 | Code duplication in exception handlers | Low | Very Likely | LOW |

## 5. Recommendations (prioritized)

1. **[CRITICAL] Implement HTMX feedback architecture** (ADR-0013)
   - Toast container in layout
   - HX-Trigger for success notifications
   - hx-swap-oob for error notifications
   - CSS loading indicators

2. **[HIGH] Add try-catch to all DomainEventPublisher methods**
   - Immediate fix, 30 minutes of work
   - Prevents false 500 errors for successful operations

3. **[HIGH] Add logging to GlobalExceptionHandler**
   - LOG.error for 5xx, LOG.warn for 4xx
   - Make backend failures visible

4. **[MEDIUM] Replace raw exception messages with i18n keys**
   - Map exception types to message keys
   - Never show `ex.getMessage()` to users

5. **[MEDIUM] Restructure trip detail page**
   - Wrap sections in `<article>` elements
   - Collapse invite forms behind action buttons
   - Use PicoCSS `<dialog>` or `<details>` for invite forms

6. **[LOW] Handle ResponseStatusException in GlobalExceptionHandler**
   - Add dedicated handler for proper 403/404 rendering

## 6. EventStorming: Trip Invitation Flow (Process-Level)

This flow concentrates most of the UX pain points.

### Happy Path

| # | Sticky | Type | Notes |
|---|--------|------|-------|
| 1 | Organizer issues InviteParticipant | Command (Blue) | via HTMX POST |
| 2 | Trip aggregate validates invitation | Aggregate (Yellow) | |
| 3 | InvitationCreated | Event (Orange) | registered in aggregate |
| 4 | Invitation saved to DB | | repository.save() |
| 5 | **Policy**: Send invitation email | Policy (Purple) | InvitationEmailListener (AFTER_COMMIT) |
| 6 | **Policy**: Publish to RabbitMQ | Policy (Purple) | DomainEventPublisher (AFTER_COMMIT) |
| 7 | HTMX response updates invitation list | Read Model (Green) | invitations fragment |

### Hot Spots (Red)

| # | Hot Spot | Description |
|---|----------|-------------|
| H1 | Between step 4 and 7 | NO SUCCESS FEEDBACK to user after invitation saved |
| H2 | Step 5 failure | If mail fails, exception caught (OK). But user has no idea email failed. |
| H3 | Step 6 failure | If RabbitMQ fails, exception NOT caught. User sees 500 error. Data IS saved. |
| H4 | Step 7 rendering | Error toast replaces invitation list content if HTMX error occurs |
| H5 | External invite form | 4 required fields always visible, no progressive disclosure |

### Missing Events

| Event | Why Missing | Impact |
|-------|-------------|--------|
| InvitationEmailFailed | No event for mail failure | User and organizer unaware email did not arrive |
| EventPublishFailed | No event for RabbitMQ failure | Event lost silently (or crashes caller) |

## 7. Quality Storming: Usability

### Quality Scenario QS-U01: Action Feedback
- **Quality**: Usability -- Action Feedback
- **Stimulus**: User submits an HTMX form (invite, create trip, accept invitation)
- **Environment**: Normal operation
- **Response**: User sees visible confirmation within 500ms (success toast or error toast)
- **Current State**: FAILING -- no success feedback, error feedback replaces content

### Quality Scenario QS-U02: Error Recovery
- **Quality**: Usability -- Error Recovery
- **Stimulus**: Backend operation fails (DB, Keycloak, RabbitMQ)
- **Environment**: Partial infrastructure failure
- **Response**: User sees clear error message, original page state preserved, can retry
- **Current State**: PARTIALLY FAILING -- error shown but replaces content, retries may cause duplicates

### Quality Scenario QS-U03: Loading State
- **Quality**: Usability -- System Status Visibility
- **Stimulus**: User submits form with network latency > 200ms
- **Environment**: Slow network
- **Response**: Submit button shows loading state, prevents double-click
- **Current State**: FAILING -- no loading indicators anywhere

## 8. Action Items

| Priority | Item | Effort | ADR |
|----------|------|--------|-----|
| P0 | Try-catch in DomainEventPublisher (both SCS) | 1h | ADR-0013 |
| P0 | Toast container + minimal JS in layouts | 2h | ADR-0013 |
| P1 | Logging in GlobalExceptionHandler | 1h | ADR-0013 |
| P1 | HX-Trigger success headers in controllers | 3h | ADR-0013 |
| P1 | OOB error swap pattern | 2h | ADR-0013 |
| P2 | i18n error message mapping | 2h | ADR-0013 |
| P2 | Trip detail page restructuring | 3h | - |
| P2 | CSS loading indicators | 1h | ADR-0013 |
| P3 | ResponseStatusException handler | 30min | - |
