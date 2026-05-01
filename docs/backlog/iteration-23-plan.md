# Iteration 23 — Compliance and UX Quality

**Target Version**: `v0.23.0`  
**Status**: PLANNED

## Goal

Iteration 23 prepares Travelmate for go-live from a compliance and user-quality perspective.

## Planned Scope

- GDPR-relevant data export and deletion pathways planned or implemented for the MVP compliance bar
- privacy notice and processing-register documentation
- accessibility checks integrated into the quality workflow
- Lighthouse CI and mobile-quality gates introduced
- documentation and release artefacts updated to reflect the compliance state
- **S23-USER-PROFILE-LOCALE-TZ-CURRENCY** — locale-aware date/time
  formatting plus timezone and preferred-currency settings in the user
  profile. See story detail below.

## Planned Deliverables

- first coherent compliance baseline for personal and financial travel data
- measurable UX quality gates for mobile and accessibility
- clearer operational understanding of data lifecycle responsibilities

## Acceptance

- compliance artefacts exist in canonical documentation, not only in reports
- accessibility and performance checks are automated at least at baseline level
- unresolved GDPR risks are explicitly documented if they remain open
- S23-USER-PROFILE-LOCALE-TZ-CURRENCY acceptance criteria below are met end to end

---

## Story Detail: S23-USER-PROFILE-LOCALE-TZ-CURRENCY

### User Story

**As a** user with a specific locale, timezone, or preferred currency,
**I want** Travelmate to display dates, times, and amounts in my local
conventions and let me edit these preferences in my user profile,
**so that** the app feels native and I don't mentally translate
ISO-formatted dates or EUR-only amounts.

### Background

Surfaced during the 2026-04-30 demo smoke as: *"Datum/Uhrzeit im
jeweiligen (deutschem) Format darstellen je nach Sprache. Vielleicht
auch Zeitzonen und Währungseinstellung im User-Profil."*

Current state:

- Date formatting in controllers is hardcoded:
  `LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))`
  (e.g. `travelmate-expense/.../adapters/web/ExpenseController.java:367`)
  — no locale binding.
- The `Account` aggregate
  (`travelmate-iam/src/main/java/de/evia/travelmate/iam/domain/account/Account.java:13-44`)
  has no fields for `locale`, `timezone`, `preferredCurrency`.
- The navbar language switcher only sets the Thymeleaf locale context,
  not persistent at the user level.
- Default currency is implicitly EUR everywhere; not configurable.

### Acceptance Criteria

1. **Account aggregate extended** with optional fields `locale` (default
   `de_DE`), `timezone` (default `Europe/Berlin`), `preferredCurrency`
   (default `EUR`). Flyway migration in IAM (V8 or higher), backfilling
   defaults for existing accounts.

2. **User-Profile page** at `/iam/profile` with a form to edit these
   three fields:
   - Locale as a dropdown of supported values (initially DE, EN; future-proof
     for FR/IT/ES)
   - Timezone as a dropdown of common European zones plus a
     "show all"-option (full TZ list)
   - Currency as an ISO-4217 selector (EUR, USD, CHF, GBP at minimum)

3. **Date display**: every visible date (Trip dates, Trip-Detail headers,
   PDF exports, mail templates) uses Thymeleaf `#temporals.format(...)`
   with the user's locale rather than hardcoded `ofPattern("yyyy-MM-dd")`.

4. **Time display**: dates that include time (Trip stay-periods, poll
   deadlines, expense timestamps) render in the user's timezone, not UTC.

5. **Currency display**: amounts in the Expense overview and PDF export
   format in the user's currency. For demo purposes only EUR/USD/CHF
   are actually converted (mock rate); other currencies render in their
   original currency with a footnote.

6. **i18n migration**: existing users with no preferences set default
   to `de_DE / Europe/Berlin / EUR`. No data loss.

7. **Tests**:
   - 3 unit tests in `AccountTest` for the new setters/validation
   - 1 integration test in a controller verifying that a user's
     `locale` is propagated through Thymeleaf
   - 1 E2E scenario in `travelmate-e2e` that edits the profile, switches
     locale, and verifies a date renders in the new format

### Technical Notes

- Domain modeling: `Locale` and `ZoneId` should be wrapped as Value
  Objects (records) similar to `TenantId` if validation is added.
  `Currency` from `java.util.Currency` can be used directly or wrapped.
- Spring provides `LocaleResolver` and a timezone-aware
  `LocaleContextResolver` — these should be used to bind the user's
  preference to the request context, not passed through manually.
- **Architectural choice**: extend `Account` directly, or introduce a
  `UserPreferences` aggregate? If we expect more profile fields
  (notification settings, theme, etc.), splitting out `UserPreferences`
  is cleaner. Decision deferred to slicing.

### Risks

- Currency conversion with stale rates is misleading. Either no
  conversion (label original currency clearly) or live FX rates.
  This story chooses **no live conversion**, only display formatting —
  conversion is its own story.
- Migration backfill for existing accounts must be safe (NULL
  defaults vs hardcoded `de_DE` etc.). Define explicitly in the V8
  migration.

### Out of Scope

- True currency conversion using live FX rates — separate larger story
  ("FX integration").
- Adding new languages beyond DE/EN (FR, IT, ES are tracked
  separately in the i18n roadmap).
- Historization of locale/timezone changes (audit trail).
- Per-trip currency override — users may attend a trip in a different
  currency than their personal preference; that's a follow-up story.
