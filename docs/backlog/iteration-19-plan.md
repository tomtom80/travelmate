# Iteration 19 — Demo-Hardening + Phase-0-GTM

**Target Version**: `v0.20.0`
**Status**: PLANNED
**Sprint Duration**: ~3 months at 1–3h/week solo pace (~12–36h capacity)
**In-Scope Total**: 3 stories (~17–39h estimated)

> **Theme refresh from team-planning 2026-04-30**: Original scope was
> "Visibility & Integrity" with ~10 stories (Recipe CRUD, Observability,
> Outbox, Event-Versioning). After cross-functional planning with Architect,
> Requirements Engineer, UX Designer, QA Engineer, DevOps, and Security,
> scope was cut to 3 stories that directly address (a) demo-blockers
> from the 2026-04-30 demo smoke and (b) the GTM-Phase-0 trigger from
> `docs/business/business-model-and-strategy.md`. Recipe CRUD,
> Observability, Outbox, Event-Versioning moved to iter-20 / 21 / 22.

## In-Scope Stories

Each story has its own detail file under `iter-19/` with the full
team-session outputs (RE / Architect / UX / QA / Security / DevOps).

| # | Story | Size | Detail |
|---|---|---|---|
| 1 | **S19-INVITE-EXISTING** | M (5–15h) | [iter-19/S19-INVITE-EXISTING.md](iter-19/S19-INVITE-EXISTING.md) — external invite for an existing account no longer silently skips: send password-setup email if Keycloak credentials missing, otherwise re-login notice |
| 2 | **S19-LANDING-WAITLIST** | M (10–20h) | [iter-19/S19-LANDING-WAITLIST.md](iter-19/S19-LANDING-WAITLIST.md) — public landing page on `/iam/landing` with email wait-list signup via Mailerlite + Plausible event tracking |
| 3 | **S19-UI-POLISH-DEMO-FEEDBACK** | S (2–4h) | [iter-19/S19-UI-POLISH-DEMO-FEEDBACK.md](iter-19/S19-UI-POLISH-DEMO-FEEDBACK.md) — login logo BG (already hotfixed), accommodation poll i18n EN keys, trip detail edit/cancel button alignment |

## Out-of-Scope (moved to later iterations)

| Story | Moved to | Why |
|---|---|---|
| Recipe edit flow finalized | iter-20 | Solid feature add, not demo-critical, no direct cash-flow impact |
| Recipe delete flow finalized | iter-20 | Same as above |
| Recipe import from URL (SSRF-aware) | iter-21 | SSRF-adapter pattern is its own threat-model exercise — needs Security-led design |
| Observability baseline (Micrometer / Prometheus) | iter-20 | Important for scaling but not critical at <10 users |
| Centralized roadmap decision for logs/metrics/traces | iter-20 | Companion to observability story |
| Transactional outbox first slice | iter-22 | Significant architectural addition — production-hardening territory |
| Event versioning + naming conventions executable through tests | iter-22 | Engineering excellence, no direct user value at this stage |
| Documentation drift corrections | iter-19 (continuous) | Lightweight bookkeeping running alongside the 3 main stories |

## Sprint Acceptance

- [ ] **S19-INVITE-EXISTING**: external invitations to existing accounts always
  produce one of two follow-up emails (password-setup or re-login notice);
  no silent skip; idempotent on event replay; full E2E flow verified
  against real Keycloak in Mailpit.
- [ ] **S19-LANDING-WAITLIST**: `/iam/landing` is publicly reachable, accepts
  email + DSGVO opt-in, subscribes to Mailerlite, fires Plausible
  `Waitlist Signup` event, GDPR enumeration-defense on duplicate, hCaptcha
  or Origin-header anti-bot check, DPA signed with Mailerlite.
- [ ] **S19-UI-POLISH-DEMO-FEEDBACK**: accommodation-poll create page renders
  fully in EN locale; trip-detail Edit/Cancel buttons share PicoCSS class;
  `MessageBundleParityTest` enforces DE-EN key parity for trips bundle.
- [ ] All three stories' BDD feature files exist and pass.
- [ ] STRIDE threat models documented per story; Security action items closed.
- [ ] Demo on travelmate-demo.de visibly improved (cosmetic + waitlist + invitation flow).

## Sprint Operational Notes

- **No new infrastructure components** required (no new container, no new DB,
  no new RabbitMQ queue). Story 1 may add a single Flyway migration in IAM
  (V8 if the idempotency marker is DB-backed; Caffeine cache alternative
  avoids it).
- **One new external service**: Mailerlite (US-hosted) for waitlist. DPA
  signature is a hard prerequisite — see `iter-19/S19-LANDING-WAITLIST.md`
  Security section.
- **One new GitHub Environment secret**: `MAILERLITE_API_KEY` to be added
  before iter-19's first deploy.
- **Demo-VM impact**: minimal. Container redeploy via existing
  `Demo Deploy` workflow handles all three stories.

## How This Iteration Was Planned

Cross-functional team-planning session (2026-04-30) used six specialised
agents in three sequential rounds:

1. **Architect + Requirements Engineer** (parallel): DDD-impact analysis,
   INVEST-compliant user stories, Gherkin acceptance criteria.
2. **UX Designer** (sequential): journey maps, mail mockups, ASCII
   wireframes, copy suggestions in DE/EN.
3. **QA Engineer** (sequential): test-pyramid distribution, Gherkin feature
   files, E2E skeletons, mocking strategy.
4. **DevOps + Security** (parallel): STRIDE threat models, DSGVO notes,
   deployment + monitoring + rollback strategy per story.

Total agent-time: ~6 hours of focused planning produced full per-story
specs ready for sprint execution. The detail files (`iter-19/*.md`) are
the source of truth; this top-level file is the navigable index.
