# Iteration 20 — Onboarding and Auditability

**Target Version**: `v0.20.0`  
**Status**: PLANNED

## Goal

Iteration 20 completes key onboarding gaps and introduces auditability that later security and compliance work can rely on.

## Planned Scope

### Functional stories

- external invitation flow completed as a first-class onboarding channel
- password reset via Keycloak enabled and integrated into the user journey
- **Recipe edit flow finalized and verified end to end** (moved from iter-19 — solid feature add, not demo-critical, no direct cash-flow impact)
- **Recipe delete flow finalized and verified end to end** (moved from iter-19 — same rationale as recipe edit)
- **S20-NAVBAR-USER-DISPLAY** — show the currently logged-in user in the
  navbar, with a dropdown linking to profile and logout. See story detail below.
- **S20-PAYMENT-INTEGRATION** — Stripe Checkout integration for Pay-per-Trip
  (€4.99 one-shot per trip pass) and Pro subscription (€4.99/month or
  €39/year). Includes Stripe webhook handler, subscription/trip-pass
  domain entities in IAM, and Stripe Tax for DACH VAT. Stub only;
  full ACs at sprint planning. Foundational for the GTM Phase 2
  monetization (`docs/business/business-model-and-strategy.md` §7).

### Non-functional stories

- audit logging for security-relevant and business-critical actions
- explicit documentation updates for security-relevant code/documentation mismatches
- release and operations docs aligned to the new onboarding and audit flows
- **Observability baseline with Micrometer and Prometheus-compatible endpoints** (moved from iter-19 — important for scaling but not critical at <10 users)
- **Centralized roadmap decision for logs, metrics, and traces** (moved from iter-19 — companion to the observability baseline)

## Planned Deliverables

- no production-significant onboarding dead end remains in invitation and account recovery flows
- audit events exist for critical actions such as organizer changes and destructive operations
- architectural documentation explains the real current behavior instead of historic assumptions

## Acceptance

- external invitation works for the main user journey and fails safely on invalid or expired paths
- password reset is demonstrable in a realistic environment
- audit events are emitted and documented for agreed critical paths
- S20-NAVBAR-USER-DISPLAY acceptance criteria below are met end to end

---

## Story Detail: S20-NAVBAR-USER-DISPLAY

### User Story

**As an** authenticated user,
**I want** my name (or username) prominently shown in the top navbar,
**so that** I always see which account I'm currently logged in as — important
when juggling demo accounts or shared workstations.

### Background

The shared `default.html` layout in each SCS
(`travelmate-iam/src/main/resources/templates/layout/default.html:19-51`,
plus the analogous files in `travelmate-trips` and `travelmate-expense`)
renders the logo, nav links, language switcher, and a logout form, but
**no user indicator**. Users discovering this issue during the
2026-04-30 demo smoke had to mentally track which Keycloak session was
active — error-prone for demos and shared environments.

Spring Security exposes the principal via Thymeleaf-Security tags
(`sec:authentication`); most controllers also already pass a
`ResolvedIdentity` (`memberId`, `tenantId`, `email`) to the model.
Either path is suitable.

### Acceptance Criteria

1. **Navbar element**: each `default.html` layout shows the logged-in
   user's display name (preferred: `firstName lastName`; fallback:
   `username` / `email`) in the navbar, positioned next to the language
   switcher. Renders only on authenticated pages.

2. **Public pages**: on `Sign In` / `Sign Up` (no JWT), the navbar shows
   the existing "Sign In" / "Sign Up" links instead — never an empty slot.

3. **Dropdown menu**: clicking the user-name opens a small dropdown with:
   - "Mein Profil" (links to `/iam/profile` — page itself ships with S23,
     so this story can render the link as inactive/disabled or pointing
     to a placeholder until S23 lands)
   - "Logout" (existing logout form, moved into the dropdown)

4. **Cross-SCS consistency**: all three SCS layouts (`iam`, `trips`,
   `expense`) display the user element identically — same position,
   same dropdown content, same i18n keys.

5. **Keycloak login theme**: unchanged. The Keycloak login page never
   shows an authenticated user — different surface area, out of scope.

6. **Tests**:
   - One unit test per SCS verifying the navbar partial renders the user
     name when a `ResolvedIdentity` is in the model.
   - One E2E scenario in `travelmate-e2e` that logs in, navigates between
     IAM/Trips/Expense, and asserts the user name is shown consistently.

### Technical Notes

- **Implementation A (preferred)**: add the
  `org.thymeleaf.extras:thymeleaf-extras-springsecurity6` dependency to the
  three SCS POMs, then use `sec:authentication="principal.username"` directly
  in the Thymeleaf template.
- **Implementation B (no new dep)**: add a `@ControllerAdvice` per SCS that
  exposes `currentUser` (resolved via existing `IdentityResolver` or similar)
  to every model. Templates render `${currentUser?.firstName}`.
- HTMX-friendly dropdown: use a plain `<details>` element (semantic HTML,
  PicoCSS already styles it) — no JS.
- The "Mein Profil" link should be present even before S23 lands, so users
  experience the navigation hint; clicking it would 404 until S23 lands —
  acceptable for an in-progress iteration.

### Out of Scope

- Avatar / profile photo upload — separate future story.
- Switch-tenant function for users who belong to multiple tenants — part
  of larger Multi-Organizer discussion (Iter-22+).
- Role indicator (organizer vs participant) next to the name — deferred.
