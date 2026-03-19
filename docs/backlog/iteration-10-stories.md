# Iteration 10 — Refined User Stories: Recipe Import, Settlement Polish, TravelParty Names + Lighthouse CI

**Date**: 2026-03-19
**Target Version**: v0.10.0
**Bounded Contexts**: Trips (Recipe Import from URL), Expense (Custom Receipt Splitting, Settlement per Category), Infrastructure (Lighthouse CI)

---

## Overview

Iteration 10 delivers the long-deferred Recipe Import from URL as the anchor story, three medium
stories that close meaningful gaps in the Expense domain, and one infrastructure story that
establishes automated quality monitoring.

**US-TRIPS-041 (Recipe Import from URL)** has been deferred from Iteration 7 and Iteration 9. It
establishes the URL-import adapter pattern that will later apply to accommodation imports (US-TRIPS-061).
It has the highest user-visible impact of any remaining Trips story and is therefore the L story
for this iteration.

**US-EXP-022 (Custom Receipt Splitting)** closes a real-world gap: not every group expense is
shared equally. Activity receipts (ski rental for adults only, entry fee for one family) need to be
assigned to a subset of participants. This is the most frequently requested Expense enhancement.

**US-EXP-032 (Settlement per Category)** complements the party-level settlement view from
Iteration 9 with a category breakdown. It is pure read-side work — no new aggregates, no schema
changes.

**US-INFRA-042 (Lighthouse CI)** has been deferred twice. The PWA manifest is now in place
(Iteration 9), making Lighthouse installability checks meaningful. Adding automated quality gates
closes the loop on mobile performance and accessibility.

**S10-E (TravelParty Display Name Propagation)** is a new story arising from the Iteration 9
open design question Q1. The Expense SCS currently derives party names from the first participant's
last name. This story publishes a proper `TravelPartyNameRegistered` event and consumes it in
Expense, giving the settlement view correct party names.

---

## Dependency Graph

```
S10-A: Recipe Import from URL (US-TRIPS-041)
  — standalone; requires no new events or schema in other SCS
  — establishes RecipeImportPort + JsonLdRecipeImportAdapter in Trips
  — Recipe aggregate (domain) unchanged; new secondary port only
  — foundation for future US-TRIPS-061 (Accommodation URL Import)

S10-B: Custom Receipt Splitting (US-EXP-022)
  — adds SplitMode (WEIGHTED_ALL / CUSTOM_PARTICIPANTS) to Receipt entity
  — new command: UpdateReceiptSplitCommand
  — SettlementCalculator must respect per-receipt participant filter
  — builds on existing Expense aggregate; no new event contracts
  — Flyway V10 in Expense

S10-C: Settlement per Category (US-EXP-032)
  — pure read-side: derives category totals from existing Receipt data
  — no new aggregates, no Flyway migration
  — can be developed independently; renders as a new section on the
    settlement detail page

S10-D: Lighthouse CI (US-INFRA-042)
  — GitHub Actions workflow addition; no SCS changes
  — depends on PWA manifest (done in Iteration 9)
  — can be implemented at any point in the iteration

S10-E: TravelParty Display Name Propagation (NEW — from Iter 9 Q1)
  — new event: TravelPartyNameRegistered in travelmate-common/events/trips/
  — published by Trips SCS when a TravelParty joins a Trip for the first time
  — consumed by Expense SCS to update partyName in TripProjection
  — Flyway V10 already covers the party_name column added in V8; no new migration needed
  — should be implemented before S10-B to ensure correct party names in custom split UI

--- NOT this iteration ---

US-TRIPS-061 (Accommodation URL Import)
  — Could priority; deferred; wait until S10-A establishes the adapter pattern

US-TRIPS-062 (Accommodation Poll)
  — Could priority; new LocationPoll aggregate; out of scope

US-EXP-033 (Export Settlement as PDF)
  — Could priority; introduces new PDF library dependency; out of scope

US-INFRA-040 (Service Worker / Offline)
  — Could/XL; deferred

US-TRIPS-055 (Bring App Integration)
  — Could priority; no stable public API; deferred
```

---

## Recommended Iteration 10 Scope

| ID | Story | Priority | Size | Bounded Context |
|----|-------|----------|------|-----------------|
| S10-A | Recipe Import from URL | Should | L | Trips |
| S10-B | Custom Receipt Splitting per Receipt | Could | M | Expense |
| S10-C | Settlement per Category | Could | M | Expense |
| S10-D | Lighthouse CI | Should | M | Infrastructure |
| S10-E | TravelParty Display Name Propagation | Should | S | Trips + Expense |

**Scope rationale:**
- S10-A is the mandatory L story — deferred twice, explicitly prioritised by the user. It
  establishes the URL-import adapter pattern for the codebase.
- S10-E is a small but high-leverage story. The Iteration 9 party settlement view works but
  shows derived names ("Family of Schmidt"). A proper event closes that gap and makes all
  settlement UIs coherent. It should be done before S10-B since the custom split UI shows
  party names in the participant selector.
- S10-B adds meaningful value for real group trips where not every expense is shared by
  everyone (ski rental, activity, children's programme). It is the most requested Expense
  feature after settlement.
- S10-C is pure read-side work with no schema changes — very low risk, visible improvement.
- S10-D closes the automated quality monitoring gap now that the PWA manifest is in place.

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S10-D | Lighthouse CI — purely additive GitHub Actions change, no SCS impact |
| 2 | S10-E | Party names — fixes Iter 9 Q1 before S10-B needs party names in UI |
| 3 | S10-A | Recipe Import — standalone, high value, establishes adapter pattern |
| 4 | S10-C | Settlement per Category — read-side only, safe to merge at any time |
| 5 | S10-B | Custom Splitting — builds on S10-E (party names) for correct UI display |

---

## New Domain Model (Iteration 10)

### Trips SCS — RecipeImportPort (Secondary Port)

Recipe Import from URL introduces a new secondary port in the domain layer and a new adapter in
`adapters/integration/`. The Recipe aggregate itself is not changed.

```
RecipeImportPort (interface in domain/recipe/)
  Optional<RecipeImportResult> importFromUrl(String url)

RecipeImportResult (Record in domain/recipe/)
  String name
  int servings
  List<ImportedIngredient> ingredients

ImportedIngredient (Record in domain/recipe/)
  String name
  BigDecimal quantity
  String unit

JsonLdRecipeImportAdapter (implements RecipeImportPort, in adapters/integration/)
  — fetches URL with timeout (5 seconds max)
  — parses HTML for <script type="application/ld+json"> blocks
  — extracts schema.org/Recipe: name, recipeYield, recipeIngredient[]
  — SSRF protection: blocks private IP ranges and localhost
  — returns Optional.empty() if no schema.org/Recipe found or on error
```

The adapter is wired via Spring `@Bean` in a `@Configuration` class. The domain only knows the
port. Tests mock the port to avoid real HTTP calls.

### Trips SCS — New Event: TravelPartyNameRegistered (S10-E)

Published by Trips SCS when a TravelParty first becomes visible in a Trip context (i.e., when
a participant from that party joins a Trip). This gives the Expense SCS a reliable party name.

```
TravelPartyNameRegistered (Record in travelmate-common/events/trips/)
  UUID tripId
  TenantId partyTenantId   — the TravelParty's tenant
  String  partyName         — e.g. "Familie Schmidt"
  Instant occurredOn
```

The TravelParty projection in Trips SCS already has the party name (it is the Tenant name from the
IAM SCS). When the first participant of a party joins a Trip, Trips publishes this event. Expense
consumes it and updates `party_name` in `trip_projection_participant`.

Routing key: `trips.party.name_registered` (new; add to `RoutingKeys.java` in common).

### Expense SCS — SplitMode on Receipt (S10-B)

```
SplitMode (enum in domain/expense/)
  WEIGHTED_ALL    — default: all participants share the cost, weighted
  CUSTOM          — only listed participants share the cost, weighted among themselves

Receipt (existing entity — extended)
  + SplitMode splitMode   (default: WEIGHTED_ALL)
  + List<UUID> splitParticipantIds  (empty = all; only relevant when splitMode = CUSTOM)
```

The `SettlementCalculator` is extended: for each Receipt, if `splitMode == CUSTOM`, only the
listed participantIds are included in the denominator when computing shares for that receipt.
Party-level grouping from Iteration 9 still applies — custom splitting just changes which
participants contribute to the pool for that specific receipt.

---

## Story S10-A: US-TRIPS-041 — Recipe Import from URL

**Epic**: E-TRIPS-05
**Priority**: Should
**Size**: L
**As a** Member, **I want** to import a Recipe from a URL (e.g., chefkoch.de, allrecipes.com),
**so that** I don't have to type ingredients manually when a recipe already exists online.

### Background

Most commonly used recipe sites embed structured data as `schema.org/Recipe` in JSON-LD format
within a `<script type="application/ld+json">` tag. The adapter fetches the page, parses that
JSON-LD, and pre-fills the Recipe form. If no structured data is found, the user falls back to
manual entry. This is the same adapter pattern that will later apply to accommodation URL imports
(US-TRIPS-061).

### Acceptance Criteria

#### Happy Path — Successful Import

- **Given** I am on the Recipe list page,
  **When** I click "Rezept aus URL importieren",
  **Then** a form opens with a URL input field and an "Importieren" button.

- **Given** I enter a URL for a recipe page that contains `schema.org/Recipe` JSON-LD
  (e.g., a chefkoch.de recipe),
  **When** I click "Importieren",
  **Then** the recipe form is pre-filled with: the extracted recipe name, servings count,
  and ingredient list (name, quantity, unit parsed from the ingredient strings).
  The form is editable — I can correct any field before saving.

- **Given** the form is pre-filled and I click "Speichern",
  **Then** the Recipe is created exactly as if I had entered it manually (same aggregate
  path, same `Recipe.create(...)` factory method).

- **Given** the URL contains multiple JSON-LD blocks,
  **When** the adapter parses,
  **Then** the first block of type `schema.org/Recipe` is used.

#### Partial Import — Missing Fields

- **Given** the URL has a `schema.org/Recipe` block but `recipeYield` is missing,
  **When** the form is pre-filled,
  **Then** the servings field is left empty (defaulting to 1 in the form's placeholder).
  All other extracted fields are shown.

- **Given** the URL has a `schema.org/Recipe` block but some ingredient strings cannot be
  parsed into (quantity, unit, name) triplets,
  **When** the form is pre-filled,
  **Then** those ingredients are shown as name-only entries with empty quantity and unit.
  The user can complete them manually.

#### Import Failure — No Structured Data

- **Given** I enter a URL for a page that exists but does not contain `schema.org/Recipe`
  JSON-LD,
  **When** I click "Importieren",
  **Then** I see the message: "Auf dieser Seite wurde kein Rezept gefunden. Bitte gib die
  Zutaten manuell ein." The empty Recipe form is displayed so I can proceed manually.

#### Import Failure — HTTP Error

- **Given** I enter a URL that returns HTTP 404, 500, or any non-200 response,
  **When** I click "Importieren",
  **Then** I see the message: "Die URL konnte nicht geladen werden (Status {statusCode}).
  Bitte prüfe den Link oder gib das Rezept manuell ein."

- **Given** the URL request times out (takes longer than 5 seconds),
  **When** I click "Importieren",
  **Then** I see the message: "Die Verbindung hat zu lange gedauert. Bitte versuche es erneut
  oder gib das Rezept manuell ein."

#### SSRF Protection

- **Given** I enter a URL pointing to a private IP range (10.x.x.x, 192.168.x.x, 172.16-31.x.x)
  or localhost,
  **When** I click "Importieren",
  **Then** the request is blocked and I see: "Diese URL ist nicht erreichbar." No network
  request is made.

- **Given** I enter a URL with a non-HTTP/HTTPS scheme (e.g., `file://`, `ftp://`),
  **When** I click "Importieren",
  **Then** I see the validation error: "Bitte gib eine gültige HTTP- oder HTTPS-URL ein."

#### Concurrent Use — Preview and Edit

- **Given** the import succeeds and the form is pre-filled,
  **When** I add, remove, or modify any ingredient before saving,
  **Then** the final Recipe contains my edited version, not the original import. The import
  result is a suggestion only.

- **Given** I submit the pre-filled form without changes,
  **Then** the Recipe is saved with exactly the imported data.

#### Authorization

- **Given** I have the `participant` role,
  **When** I access the import form or POST the import URL,
  **Then** the operation is allowed. Recipe creation is open to all roles (same as manual
  creation).

#### Multi-Tenancy

- The created Recipe is scoped to the requesting user's TenantId, resolved from the JWT
  email claim via TravelPartyRepository. No cross-tenant access.

### Technical Notes

- Bounded Context: Trips
- New secondary port: `RecipeImportPort` in `domain/recipe/`
  ```java
  public interface RecipeImportPort {
      Optional<RecipeImportResult> importFromUrl(String url);
  }
  ```
- New Records in `domain/recipe/`:
  - `RecipeImportResult(String name, int servings, List<ImportedIngredient> ingredients)`
  - `ImportedIngredient(String name, BigDecimal quantity, String unit)`
- New adapter: `JsonLdRecipeImportAdapter` in `adapters/integration/`
  - HTTP client: `java.net.http.HttpClient` with 5-second connection + read timeout
  - JSON-LD parser: Jackson `ObjectMapper` (already on classpath); find
    `<script type="application/ld+json">` via Jsoup (new dependency) or regex fallback
  - Preferred library: **Jsoup 1.18.x** for HTML parsing (add to `travelmate-trips/pom.xml`)
  - SSRF check: resolve URL's host to IP, reject if in RFC 1918 / loopback ranges
  - Ingredient string parsing: split on first number + unit pattern (e.g., "200 g Mehl" →
    quantity=200, unit="g", name="Mehl"). Use a simple regex:
    `^(\d+[\.,]?\d*)\s*([a-zA-ZäöüÄÖÜß]+)\s+(.+)$` — if no match, treat full string as name
  - If JSON-LD is an array `[{...}]`, iterate to find `@type: "Recipe"`
  - schema.org field mapping:
    - `name` → RecipeImportResult.name
    - `recipeYield` → servings (may be a string "4 Portionen" — extract leading integer)
    - `recipeIngredient[]` → each string parsed via ingredient regex above
- New Controller endpoint in `RecipeController`:
  - `GET /recipes/import` → renders `recipe/import-form.html`
  - `POST /recipes/import` → calls `RecipeImportPort.importFromUrl(url)`, then
    renders `recipe/form.html` pre-populated with the result (or error message)
  - The import step does NOT save anything — it just pre-fills the form.
    The actual save is the existing `POST /recipes`.
- No new domain event; no Flyway migration (no schema changes)
- Test strategy:
  - Unit tests for `JsonLdRecipeImportAdapter` with mocked HTTP responses (use
    `MockWebServer` from OkHttp or WireMock)
  - Unit tests for SSRF IP range validation
  - Unit tests for the ingredient string parser
  - Controller test: mock `RecipeImportPort` → verify form pre-fill + error message paths
  - E2E test: use WireMock stub in `travelmate-e2e` to serve a fixture HTML page with
    schema.org/Recipe JSON-LD; verify the form is pre-filled correctly
- Ingredient string parsing corner cases:
  - Fractions: "½ TL Salz" → quantity=0.5, unit="TL", name="Salz"
  - Range: "2-3 EL Öl" → quantity=2 (take lower bound), unit="EL", name="Öl"
  - No quantity: "Salz nach Geschmack" → name="Salz nach Geschmack", quantity=null, unit=""
  - All no-match cases: full string as name, quantity=null, unit=""
- BDD scenario: "Als Mitglied möchte ich ein Rezept per URL importieren"
- `RoutingKeys`: no changes needed

---

## Story S10-B: US-EXP-022 — Custom Receipt Splitting per Receipt

**Epic**: E-EXP-03
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to restrict a specific receipt to a subset of participants (e.g.,
ski rental for adults only), **so that** not every expense is borne by the entire group and the
settlement reflects actual consumption.

### Background

In group trips, many expenses are not shared by everyone: ski rental applies only to adults
who ski, a children's activity applies only to families with children under 12, a special
dinner may apply only to one Travel Party. The default `WEIGHTED_ALL` split works for shared
costs (groceries, accommodation). Custom splits are the exception, not the rule — the UI must
make the default easy and custom selection discoverable but not dominant.

The custom split operates at the participant level: the Organizer selects which individual
participants share the cost. The weighting of the selected participants relative to each other
determines their shares — the same algorithm as `WEIGHTED_ALL` but with a filtered participant
list.

### Acceptance Criteria

#### Default Mode — WEIGHTED_ALL

- **Given** I submit a receipt without changing the split mode,
  **When** it is saved,
  **Then** `splitMode = WEIGHTED_ALL` and `splitParticipantIds` is empty. The settlement
  includes all participants in the denominator for this receipt. This is the existing behaviour
  and must remain unchanged.

#### Custom Split — Setting the Split

- **Given** I am creating or editing a receipt,
  **When** I click "Benutzerdefinierte Aufteilung" (collapsed by default),
  **Then** a participant multi-select opens, showing all participants of the Trip with their
  name and weighting.

- **Given** the multi-select is open and I select a subset of participants (e.g., 3 of 8),
  **When** I save the receipt,
  **Then** `splitMode = CUSTOM` and `splitParticipantIds` contains the selected IDs.

- **Given** I select all participants,
  **When** I save,
  **Then** the result is equivalent to `WEIGHTED_ALL`. The system accepts it; no error.

- **Given** I select zero participants,
  **When** I try to save with `CUSTOM` mode,
  **Then** I see the error: "Bitte wähle mindestens einen Teilnehmer aus."

#### Custom Split — View

- **Given** a receipt has `splitMode = CUSTOM`,
  **When** I view the receipt list,
  **Then** the receipt shows a badge "Teilaufteilung" with a tooltip listing the selected
  participant names.

- **Given** I view the settlement calculation,
  **When** a receipt has a custom split,
  **Then** only the selected participants bear the cost of that receipt (with their weights).
  All other receipts continue to use `WEIGHTED_ALL`.

#### Settlement Calculation Impact

- **Given** a Trip has 4 participants (A: 1.0, B: 1.0, C: 0.5, D: 0.0) and a receipt of
  100 EUR with custom split selecting only A and B (weights 1.0 each),
  **When** the settlement runs,
  **Then** A bears 50 EUR and B bears 50 EUR. C and D bear 0 EUR for this receipt.
  The party-level settlement (from Iter 9) groups these correctly.

- **Given** a receipt has `splitMode = WEIGHTED_ALL`,
  **When** the settlement runs,
  **Then** the calculation is identical to the existing algorithm. No regression.

- **Given** a custom split selects participants from one TravelParty only,
  **When** the settlement runs,
  **Then** that receipt is attributed entirely to that party's share.

#### Edit and Remove Custom Split

- **Given** a receipt has a custom split,
  **When** I edit the receipt and switch back to "Alle Teilnehmer" (clear the custom selection),
  **Then** `splitMode` reverts to `WEIGHTED_ALL` and `splitParticipantIds` is cleared.

#### Authorization

- **Given** I have the `participant` role,
  **When** I submit a receipt,
  **Then** I can set a custom split for my own receipt (same as creating a receipt).

- **Given** I have the `organizer` role,
  **When** I edit another participant's receipt's split (e.g., from the review queue),
  **Then** the split is updated. Organizers may adjust split mode as part of receipt management.

#### Multi-Tenancy

- Participant IDs in `splitParticipantIds` must belong to the same Trip. The settlement
  service validates this when computing. Cross-trip or cross-tenant participant IDs are rejected.

### Technical Notes

- Bounded Context: Expense
- `Receipt` entity extension:
  - Add `SplitMode splitMode` (enum: `WEIGHTED_ALL`, `CUSTOM`) — default `WEIGHTED_ALL`
  - Add `List<UUID> splitParticipantIds` (empty list = all)
- `SplitMode` enum: new enum in `domain/expense/` (or as inner class of `Receipt` — team decision)
- Commands:
  - Extend `SubmitReceiptCommand` and `EditReceiptCommand` with:
    `SplitMode splitMode`, `List<UUID> splitParticipantIds`
- `Expense.submitReceipt(...)` and `Expense.editReceipt(...)`: accept and store split fields
  - Invariant: if `splitMode == CUSTOM` and `splitParticipantIds.isEmpty()`, throw
    `IllegalArgumentException("A custom split must include at least one participant.")`
- `SettlementCalculator` update:
  - For each receipt, determine the effective participant set:
    - `WEIGHTED_ALL`: all participants
    - `CUSTOM`: only those in `splitParticipantIds` (intersected with actual participants)
  - Compute the denominator (total weight) from the effective set only
  - Distribute the receipt amount proportionally among the effective set
  - Accumulate into the per-participant (and per-party) balance as before
- Flyway migration: V10 in Expense — add `split_mode VARCHAR(20) NOT NULL DEFAULT
  'WEIGHTED_ALL'` and `split_participant_ids TEXT NULL` (stored as comma-separated UUIDs or
  JSON array) to the `receipt` table
- UI: receipt form (`expense/receipt-form.html`) — collapsible "Benutzerdefinierte Aufteilung"
  section with participant checkboxes. Collapsed by default; opens on click.
  HTMX: no partial update needed; standard form submission.
  Receipt list row: small "Teilaufteilung" badge when `splitMode == CUSTOM`.
- Participant list for the multi-select: retrieved from `TripProjection.participants` (existing
  read model in Expense SCS)
- Domain Events: none
- BDD scenario: "Als Organisator möchte ich einen Beleg nur bestimmten Teilnehmern zuordnen"

---

## Story S10-C: US-EXP-032 — Settlement per Category

**Epic**: E-EXP-04
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to see the settlement broken down by expense category (Unterkunft,
Lebensmittel, Restaurant, Aktivitäten, Transport, Sonstiges), **so that** everyone understands
where the shared money was spent.

### Background

The party-level settlement view (Iteration 9) answers "who owes whom". The category breakdown
answers "what did we spend it on". Both views are derived from the same `Receipt` data — no new
aggregate or schema is needed. The category breakdown is a secondary section on the existing
settlement page.

### Acceptance Criteria

#### Category Breakdown Section

- **Given** a Trip is COMPLETED and has an approved expense ledger,
  **When** the Organizer views the Settlement page,
  **Then** below the party balance table, a "Kostenübersicht nach Kategorie" section is shown.

- **Given** the category section is rendered,
  **Then** it shows one row per category that has at least one approved receipt:
  ```
  Kategorie        Betrag    Anteil
  Unterkunft       380 €      38%
  Lebensmittel     290 €      29%
  Restaurant       180 €      18%
  Aktivitäten       90 €       9%
  Transport         60 €       6%
  Sonstiges          0 €       0%   ← not shown if zero
  ─────────────────────────
  Gesamt         1.000 €     100%
  ```
  Only categories with amounts > 0 are shown. Categories with 0 EUR are omitted.

- **Given** the accommodation total from `TripProjection.accommodationTotal` is set,
  **When** the category section is rendered,
  **Then** it is included in the "Unterkunft" row alongside any ACCOMMODATION-category receipts.
  The Unterkunft total = `accommodationTotal + sum(receipts with category=ACCOMMODATION)`.

- **Given** a receipt has no category set (null / legacy data),
  **When** the category section is rendered,
  **Then** it is counted under "Sonstiges".

- **Given** there are no approved receipts,
  **When** the settlement page loads,
  **Then** the category section is not shown (empty state: no receipts to categorise).

#### Participant View

- **Given** a Participant (not Organizer) views the settlement page,
  **When** the category section is shown,
  **Then** the same breakdown is visible. All participants can see the category summary.

- **Given** I view the category breakdown,
  **When** I click on a category row,
  **Then** a detail panel shows the individual receipts in that category (receipt description,
  date, submitter, amount). This is an HTMX-powered collapsible expansion.

#### Export Consideration

- The category breakdown is rendered on the settlement page only. PDF export (US-EXP-033)
  is deferred — it will include this breakdown when implemented.

#### Multi-Tenancy

- Category totals are derived from the `Expense` aggregate, which is scoped to the
  organizer's `tenantId`. No cross-tenant data exposure.

### Technical Notes

- Bounded Context: Expense
- No new aggregate, entity, or event
- No Flyway migration
- New Read Model / Representation: `CategoryBreakdown` record
  ```java
  record CategoryBreakdown(
      ExpenseCategory category,
      BigDecimal total,
      BigDecimal percentage,
      List<ReceiptSummary> receipts
  )
  ```
  Computed in `SettlementService` (or a new `CategoryBreakdownService`) from
  `Expense.receipts()` filtered to `status == APPROVED`.
  Accommodation total added to the `ACCOMMODATION` bucket from `TripProjection.accommodationTotal`.
- `SettlementService`: add `List<CategoryBreakdown> computeCategoryBreakdown(Expense expense,
  TripProjection projection)` — pure computation, no side effects
- UI: `settlement/detail.html` — new collapsible section below the party balance table.
  Each row is expandable via HTMX `hx-get` to load receipt detail for that category.
  New fragment: `settlement/category-breakdown.html`
- Receipt category enum: `ExpenseCategory` already exists (from Iteration 6: GROCERIES,
  ACCOMMODATION, RESTAURANT, ACTIVITIES, TRANSPORT, OTHER). Use as-is.
- BDD scenario: "Als Organisator möchte ich die Gesamtkosten nach Kategorie aufgeschlüsselt sehen"

---

## Story S10-D: US-INFRA-042 — Lighthouse CI Integration

**Epic**: E-INFRA-05
**Priority**: Should
**Size**: M
**As a** developer, **I want** Lighthouse CI to automatically audit key Travelmate pages on every
pull request, **so that** mobile performance, PWA installability, and accessibility regressions
are caught before they reach main.

### Background

The PWA manifest is deployed (Iteration 9). Lighthouse can now audit PWA installability. The
existing E2E test suite runs against the full stack in CI (via Docker Compose). Lighthouse CI
plugs into the same infrastructure, running after the E2E suite completes.

### Acceptance Criteria

#### CI Integration

- **Given** a pull request is opened or updated,
  **When** the GitHub Actions pipeline runs,
  **Then** after the E2E tests pass, a Lighthouse CI step runs and audits at least the
  following pages (authenticated via session cookie from a test login):
  1. Trip list page (`/trips`)
  2. Shopping list page for a test trip
  3. Settlement page for a completed test trip

- **Given** Lighthouse runs on the Trip list page,
  **When** the audit completes,
  **Then** the following score thresholds must pass:
  - Performance: >= 80
  - Accessibility: >= 90
  - Best Practices: >= 90
  - PWA (Installable): pass

- **Given** any threshold is breached,
  **When** the CI step completes,
  **Then** the pipeline is marked as failed and the PR cannot be merged (branch protection rule).
  The Lighthouse HTML report is uploaded as a CI artifact.

- **Given** all thresholds pass,
  **When** the CI step completes,
  **Then** the pipeline step passes and the Lighthouse HTML report is available as a build
  artifact for review (retained for 7 days).

#### Audit Configuration

- **Given** the Lighthouse config is defined,
  **Then** the following Lighthouse categories are audited:
  `performance`, `accessibility`, `best-practices`, `pwa`.
  The `seo` category is excluded (internal app, not public-facing).

- **Given** pages require authentication,
  **When** Lighthouse runs,
  **Then** a pre-script performs a headless login via the test user credentials
  (`testuser` / `testpassword`) and passes the session to Lighthouse.
  Alternatively: Lighthouse runs in the same Playwright browser context after login.

- **Given** the infrastructure is not running (CI cold start),
  **When** the Lighthouse step begins,
  **Then** it waits for the health checks of Gateway and all SCS to pass before auditing
  (same wait logic as existing E2E tests).

#### Reporting

- **Given** the CI run is complete,
  **When** a developer views the PR checks,
  **Then** they see a Lighthouse CI summary comment on the PR with score badges for
  Performance, Accessibility, Best Practices, and PWA, and links to the full report.

#### Scope Limitation

- The `seo` category is explicitly excluded.
- Lighthouse is not run on POST/mutation endpoints.
- Mobile simulation: Lighthouse runs with the "Mobile" Lighthouse preset (throttled 4G,
  mobile viewport 375px). Desktop is not audited in this iteration.

### Technical Notes

- Bounded Context: Infrastructure / CI
- Tool: `@lhci/cli` (Lighthouse CI) via `npm`
- GitHub Actions: new job `lighthouse` in `.github/workflows/ci.yml` (or a new
  `lighthouse.yml` workflow)
  - Runs after the `e2e` job succeeds (uses `needs: [e2e]`)
  - Uses `actions/upload-artifact` to store HTML reports
  - Uses `github-actions-lighthouse-ci` action or direct `lhci autorun`
- Config file: `.lighthouserc.json` at repository root
  ```json
  {
    "ci": {
      "collect": {
        "url": [
          "http://localhost:8080/trips",
          "http://localhost:8080/trips/{tripId}/shopping-list",
          "http://localhost:8080/expense/{tripId}/settlement"
        ],
        "numberOfRuns": 1,
        "settings": {
          "preset": "perf",
          "formFactor": "mobile",
          "screenEmulation": { "mobile": true }
        }
      },
      "assert": {
        "assertions": {
          "categories:performance": ["error", {"minScore": 0.8}],
          "categories:accessibility": ["error", {"minScore": 0.9}],
          "categories:best-practices": ["error", {"minScore": 0.9}],
          "installable-manifest": ["error", {"minScore": 1}]
        }
      },
      "upload": { "target": "filesystem", "outputDir": ".lighthouseci" }
    }
  }
  ```
- Authentication: the CI pipeline already has Keycloak test users. A setup step performs
  a Playwright login and extracts the session cookie (or uses the same Docker test context
  as E2E tests). See existing E2E test infrastructure in `travelmate-e2e`.
- Trip/settlement URLs: CI creates a test trip (same pattern as E2E test setup) and
  substitutes its ID into the URL list.
- E2E test addition: Playwright assertion that `link[rel="manifest"]` returns HTTP 200 (sanity
  check that the manifest file is served).
- No SCS code changes required.
- BDD scenario: none (CI infrastructure story)

---

## Story S10-E: TravelParty Display Name Propagation

**Epic**: E-TRIPS-02
**Priority**: Should
**Size**: S
**As an** Organizer, **I want** the correct TravelParty name (e.g., "Familie Schmidt") to appear
in the settlement view and advance payment overview, **so that** the party-level settlement is
readable without needing to know internal party IDs.

### Background

Iteration 9 added party-level settlement (S9-C) and advance payments (S9-D). The Expense SCS
derives party names from the first participant's last name: "Family {lastName}". This is a
workaround for the fact that the `ParticipantJoinedTrip` event does not carry the TravelParty's
display name (Tenant name). This story closes that gap with a proper event that carries the
party name and is consumed by the Expense SCS.

The Trips SCS already has the correct party name in the `TravelParty` projection (it mirrors
the `Tenant.name` from the IAM SCS via the `TenantCreated` event). When a participant joins a
Trip, the Trips SCS can look up their TravelParty and publish the party name.

### Acceptance Criteria

#### Event Publishing (Trips SCS)

- **Given** a participant joins a Trip (MEMBER invitation accepted or organizer auto-join),
  **When** the `ParticipantJoinedTrip` event is processed,
  **Then** the Trips SCS also publishes `TravelPartyNameRegistered(tripId, partyTenantId,
  partyName)` if this is the first participant from that TravelParty to join the Trip.

  > "First participant from that party" means: at the time of the join, no other participant
  > with the same `tenantId` is already listed in the Trip's participant list. If a second
  > member of the same family joins, no new event is published (the name is already known).

- **Given** the TravelParty name changes in the IAM SCS (US-IAM-012, not yet implemented),
  **Then** updating the party name in the settlement view is deferred. This story handles
  the initial propagation only.

#### Event Consumption (Expense SCS)

- **Given** a `TravelPartyNameRegistered` event is received,
  **When** the Expense SCS processes it,
  **Then** all `ProjectedParticipant` rows in `trip_projection_participant` for the given
  `tripId` and `partyTenantId` have their `party_name` column updated to the event's
  `partyName` value.

- **Given** the `party_name` is updated,
  **When** the settlement page renders,
  **Then** the party row shows the actual name (e.g., "Familie Schmidt") instead of the
  derived fallback ("Family of Schmidt").

#### Advance Payment View

- **Given** advance payments are configured (S9-D),
  **When** the advance payment overview is shown,
  **Then** each party is listed by its actual name ("Familie Schmidt — 500 € — nicht bezahlt"),
  not by a derived name.

#### Fallback Behaviour

- **Given** the `TravelPartyNameRegistered` event has not yet been received for a party
  (e.g., event processing is delayed),
  **When** the settlement renders,
  **Then** the derived fallback ("Family of {lastName}") is shown. No blank names.
  The event is idempotent: if received multiple times, `party_name` is simply overwritten
  with the same value.

#### Multi-Tenancy

- The event carries `partyTenantId` and `tripId`. The Expense SCS consumer looks up the
  `TripProjection` by `tripId` (which is already scoped to the organizer's tenantId). No
  cross-tenant access.

### Technical Notes

- Bounded Context: Trips (publishes) + Expense (consumes)
- New event contract in `travelmate-common/events/trips/`:
  ```java
  public record TravelPartyNameRegistered(
      UUID tripId,
      TenantId partyTenantId,
      String partyName,
      Instant occurredOn
  ) implements DomainEvent {}
  ```
- New routing key: `trips.party.name_registered` in `RoutingKeys.java`
- Trips SCS — event publishing:
  - In `TripService.joinTrip(...)` (or the method called when an invitation is accepted):
    after publishing `ParticipantJoinedTrip`, check if this is the first participant
    from `partyTenantId` in the Trip's current participant list.
    If yes: look up the TravelParty name from `TravelPartyRepository.findByTenantId(partyTenantId)`
    and `registerEvent(new TravelPartyNameRegistered(tripId, partyTenantId, party.name(), Instant.now()))`.
  - The auto-join path (organizer creates Trip) also publishes this event.
- Expense SCS — new consumer: `TravelPartyNameRegisteredConsumer` in `adapters/messaging/`
  - Finds `TripProjection` by `tripId`
  - Updates all `ProjectedParticipant` entries where `partyTenantId` matches: sets `partyName`
  - Saves and commits
  - Idempotent: repeated messages are safe (same `party_name` is written)
- No Flyway migration: `party_name` column on `trip_projection_participant` already exists
  (added in V8 for Iteration 9)
- Domain Events produced: `TravelPartyNameRegistered`
- BDD scenario: "Als Organisator möchte ich den korrekten Namen der Reisepartei in der Abrechnung sehen"

---

## Cross-SCS Event Flow (New in Iteration 10)

```
Trips SCS                                Expense SCS
────────────────────────────────────────────────────────────────
TripService.joinTrip(participantId, ...)
  └─► Trip.addParticipant(...)
  └─► registerEvent(ParticipantJoinedTrip)           → existing
  └─► if first from party: registerEvent(TravelPartyNameRegistered)  ← NEW
  └─► repository.save(trip)
  └─► @TransactionalEventListener → RabbitMQ
                                         TravelPartyNameRegisteredConsumer ← NEW
                                           └─► TripProjection.updatePartyName(partyTenantId, name)
                                           └─► Renders as "Familie Schmidt" in settlement UI
```

**Routing key**: `trips.party.name_registered` (new; add to `RoutingKeys.java` in common)
**Exchange**: `travelmate.events` (existing topic exchange)
**Consumer**: `TravelPartyNameRegisteredConsumer` in `travelmate-expense/adapters/messaging/`

---

## Out-of-Scope for Iteration 10

| Item | Reason |
|------|--------|
| US-TRIPS-061: Import Accommodation from URL | Could priority; depends on S10-A adapter pattern being proven first; deferred to Iteration 11 |
| US-TRIPS-062: Accommodation Poll | Could/L; new LocationPoll aggregate; substantial scope |
| US-EXP-033: Export Settlement as PDF | Could; introduces PDF library; deferred |
| US-INFRA-040: Service Worker / Offline Caching | Could/XL; deferred |
| US-TRIPS-055: Bring App Integration | Could; no stable public API documented; deferred |
| US-IAM-012: Edit Travel Party Name | Could/S; low demand; deferred |
| US-IAM-040/041: Multi-Organizer Role Management | Should; Keycloak Admin API work; deferred to a dedicated IAM iteration |
| Rejection history / audit trail for receipts | Out of scope from S9-E; future story |
| US-INFRA-055: Transactional Outbox Pattern | Could/XL; significant infrastructure change; deferred |
| TravelParty name update propagation (if name changes) | Depends on US-IAM-012; deferred |

---

## Flyway Migration Summary

| SCS | Version | Content |
|-----|---------|---------|
| Trips | none new | No schema changes in Iteration 10 |
| Expense | V10 | Add `split_mode VARCHAR(20) NOT NULL DEFAULT 'WEIGHTED_ALL'` and `split_participant_ids TEXT NULL` to `receipt` table |
| Common | — | New event record `TravelPartyNameRegistered.java`; new routing key constant |

> Note: Expense V8 (`trip_participant_party_info`) already added `party_name` to
> `trip_projection_participant`. No migration needed for S10-E.

---

## Ubiquitous Language Compliance

| UI (DE) | UI (EN) | Code | Context |
|---------|---------|------|---------|
| Rezept aus URL importieren | Import Recipe from URL | RecipeImportPort | Trips |
| Importieren | Import | POST /recipes/import | Trips |
| Strukturierte Daten | Structured Data | schema.org/Recipe JSON-LD | Trips |
| Zutatenzeile | Ingredient String | ImportedIngredient | Trips |
| Benutzerdefinierte Aufteilung | Custom Split | SplitMode.CUSTOM | Expense |
| Alle Teilnehmer | All Participants | SplitMode.WEIGHTED_ALL | Expense |
| Teilaufteilung | Partial Split | CUSTOM split badge | Expense |
| Kostenübersicht nach Kategorie | Settlement per Category | CategoryBreakdown | Expense |
| Reiseparteiname | Travel Party Name | TravelPartyNameRegistered | Events (new) |
| Qualitätsprüfung | Quality Gate | Lighthouse CI | Infrastructure |
| Barrierefreiheit | Accessibility | Lighthouse `accessibility` category | Infrastructure |

---

## Dependency Map for Team Assignment

```
S10-D (Lighthouse CI)     → no code dependencies on other stories; start first
S10-E (Party Names)       → depends on common (new event); implement before S10-B
S10-A (Recipe Import)     → depends on Jsoup added to pom.xml; standalone otherwise
S10-C (Category Breakdown)→ read-only; no code dependencies; implement any time
S10-B (Custom Split)      → depends on S10-E for correct party name display in UI
```

If two developers work in parallel:
- Dev 1: S10-D → S10-A
- Dev 2: S10-E → S10-C → S10-B
