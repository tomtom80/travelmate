# Architecture Review: Mobile UX Refactoring (Iteration 11)

**Date:** 2026-03-19
**Reviewer:** Architect Agent
**Scope:** All three SCS UI layers (IAM, Trips, Expense) -- mobile responsiveness assessment
**Version:** 0.10.0-SNAPSHOT

---

## 1. Current Architecture Assessment

### 1.1 Template Inventory

| SCS | Template | Tables | Dialogs | HTMX Fragments | Mobile Severity |
|-----|----------|--------|---------|-----------------|-----------------|
| **IAM** | dashboard/index.html | 0 | 0 | 2 (members, companions) | Low |
| **IAM** | dashboard/members.html | 1 (4 cols + actions) | 0 | 1 (memberList) | High |
| **IAM** | dashboard/companions.html | 1 (3 cols + actions) | 0 | 1 (companionList) | Medium |
| **IAM** | signup/form.html | 0 | 0 | 0 | Low |
| **IAM** | landing.html | 0 | 0 | 0 | Low |
| **Trips** | trip/list.html | 1 (5 cols) | 0 | 0 | High |
| **Trips** | trip/detail.html | 1 (4 cols + inline forms) | 2 (invite-member, invite-external) | 1 (invitations) | Critical |
| **Trips** | trip/invitations.html | 1 (3 cols) | 0 | 1 (invitationList) | Medium |
| **Trips** | recipe/list.html | 1 (4 cols) | 0 | 0 | High |
| **Trips** | recipe/form.html | 0 | 0 | 0 | Medium |
| **Trips** | mealplan/overview.html | 1 (4 cols with selects) | 0 | 0 | Critical |
| **Trips** | shoppinglist/overview.html | 2 (6 cols each) | 0 | 3 (itemLists, itemRow, itemActions) | Critical |
| **Trips** | accommodation/overview.html | 1 (5 cols + nested forms) | 2 (add, edit) | 0 | Critical |
| **Expense** | expense/detail.html | 7 tables total | 2 (add-receipt, scan-receipt) | 1 (receipts) | Critical |
| **Expense** | expense/receipts.html | 1 (6-7 cols) | 2 (reject, resubmit per row) | 1 (receiptList) | Critical |
| **Expense** | expense/weightings.html | 1 (3 cols + inline forms) | 0 | 1 (weightingList) | Medium |

**Totals:**
- **53 templates** across 3 SCS (including error pages, email templates, fragments)
- **18 tables** need responsive treatment
- **8 dialog elements** need mobile full-screen behavior
- **12 HTMX fragment endpoints** may need DOM restructuring
- **~25 templates** need actual changes (excluding error pages, email templates, empty fragments)

### 1.2 CSS Framework Analysis

**PicoCSS 2 capabilities currently used:**
- `class="container"` for max-width centering
- `class="grid"` for 2-column form layouts
- `<article>` semantic card styling
- `<dialog>` native modal styling
- `<mark>` for status badges
- `<hgroup>` for title groups
- `<figure><table>` for scrollable table wrapper
- `<nav><ul>` for navigation bar
- `<details><summary>` for collapsible sections
- `<progress>` for percentage bars

**PicoCSS 2 capabilities NOT yet used:**
- `class="overflow-auto"` -- already implicit via `<figure>`, but can be applied more broadly
- Conditional classes: `class="container-fluid"` for full-width on mobile
- PicoCSS built-in responsive breakpoints (576px, 768px, 1024px, 1200px)
- PicoCSS `.grid` already stacks on mobile (<576px) -- this is ALREADY WORKING but tables inside grids are not

**Key insight:** PicoCSS already handles many responsive cases. The grid class stacks columns below 576px. The main problems are:
1. Tables with too many columns
2. Dialogs that don't go full-screen on mobile
3. Navigation that wraps poorly
4. Inline forms inside table cells

### 1.3 HTMX + Responsive Design Interaction

HTMX is structurally neutral regarding responsive design. The `hx-target` and `hx-swap` selectors reference element IDs and CSS selectors, not layout-specific structures. However:

- **Tables-to-cards conversion** will break `hx-target="closest tr"` selectors (used in ShoppingList)
- **Fragment responses** (`th:fragment`) return specific HTML structures -- if the page uses cards on mobile but tables on desktop, the server cannot know which to return
- **Polling** (`hx-trigger="every 5s"`) has mobile battery/data implications

### 1.4 Architecture-Level Changes Assessment

**No architecture-level changes are needed.** This refactoring is entirely within the presentation layer:
- No domain model changes
- No new events or commands
- No new aggregates or value objects
- No changes to application services
- No changes to persistence layer
- No new inter-SCS communication

The changes are scoped to:
1. CSS files (3 SCS + potentially 1 shared)
2. Thymeleaf templates (~25 files)
3. Possibly 1-2 small JS utilities for mobile navigation
4. E2E test additions for mobile viewport

---

## 2. Responsive Strategy with PicoCSS + HTMX

### 2.1 Breakpoint Strategy

PicoCSS 2 uses these built-in breakpoints:
- `< 576px` -- Mobile (phone portrait)
- `576px - 767px` -- Mobile landscape / small tablet
- `768px - 1023px` -- Tablet
- `>= 1024px` -- Desktop

Recommendation: Use **two breakpoints** for simplicity:
- `@media (max-width: 768px)` -- Mobile: card layout, stacked forms, full-screen dialogs
- `@media (min-width: 769px)` -- Desktop: current layout (tables, side-by-side forms, modal dialogs)

### 2.2 Table-to-Card Strategy (CSS-only where possible)

**Option A: CSS `display` switching (RECOMMENDED for simple tables)**

```css
@media (max-width: 768px) {
    .responsive-table thead { display: none; }
    .responsive-table tbody tr {
        display: block;
        margin-bottom: 1rem;
        padding: 0.75rem;
        border: 1px solid var(--pico-muted-border-color);
        border-radius: var(--pico-border-radius);
    }
    .responsive-table tbody td {
        display: flex;
        justify-content: space-between;
        padding: 0.25rem 0;
        border: none;
    }
    .responsive-table tbody td::before {
        content: attr(data-label);
        font-weight: bold;
        margin-right: 0.5rem;
    }
}
```

This approach requires adding `data-label` attributes to `<td>` elements in templates:
```html
<td data-label="Name" th:text="${trip.name()}">...</td>
```

**Pros:** Pure CSS, no server-side mobile detection, no HTMX fragment changes
**Cons:** Requires `data-label` attributes on all `<td>` elements (template changes)

**Option B: Dual-render (server-side)**
Render both a table and a card list, use CSS `display:none` to show only the appropriate one.

**Cons:** Double HTML payload, template duplication, fragment confusion

**Option C: Responsive table wrapper (minimal effort)**
Keep tables but wrap in PicoCSS `<figure>` (already done) and ensure horizontal scroll works well on mobile.

**Cons:** User experience is poor -- horizontal scrolling on a phone is frustrating, especially for the primary use case (shopping list at the store)

**Recommendation:** Option A for all tables. The `data-label` attribute approach is the cleanest:
- No server-side changes beyond adding attributes
- HTMX fragments continue to work because the HTML structure (table/tr/td) is unchanged
- CSS does all the heavy lifting
- `hx-target="closest tr"` still works because the `<tr>` element exists, just displayed as a block

### 2.3 Dialog Full-Screen on Mobile

PicoCSS `<dialog>` is already well-styled but doesn't go full-screen on mobile. CSS fix:

```css
@media (max-width: 768px) {
    dialog {
        width: 100%;
        max-width: 100%;
        min-height: 100vh;
        margin: 0;
        border-radius: 0;
    }
    dialog > article {
        min-height: 100vh;
        border-radius: 0;
        margin: 0;
    }
}
```

This is a pure CSS change. No template modifications needed. All 8 dialogs will benefit automatically.

### 2.4 Navigation Collapse

Current nav structure (PicoCSS `<nav>` with `<ul>`) already has a basic mobile stacking rule in the CSS. However, with 5-6 items (brand, Reisepartei, Reisen, Rezepte, DE/EN, Logout), it wraps awkwardly.

**Option A: CSS-only hamburger (RECOMMENDED)**

Using a checkbox-based CSS-only hamburger pattern:

```html
<nav class="container">
    <ul>
        <li><a href="/iam/dashboard"><strong>Travelmate</strong></a></li>
    </ul>
    <input type="checkbox" id="nav-toggle" class="nav-toggle" aria-label="Toggle menu">
    <label for="nav-toggle" class="nav-toggle-label">
        <span></span>
    </label>
    <ul class="nav-menu">
        <!-- existing nav items -->
    </ul>
</nav>
```

```css
.nav-toggle { display: none; }
.nav-toggle-label { display: none; cursor: pointer; }

@media (max-width: 768px) {
    .nav-toggle-label {
        display: block;
        padding: 0.5rem;
    }
    .nav-toggle-label span,
    .nav-toggle-label span::before,
    .nav-toggle-label span::after {
        display: block;
        background: var(--pico-primary);
        height: 2px;
        width: 1.5rem;
        position: relative;
    }
    .nav-toggle-label span::before { content: ''; top: -6px; position: absolute; }
    .nav-toggle-label span::after { content: ''; top: 6px; position: absolute; }

    .nav-menu {
        display: none;
        flex-direction: column;
        width: 100%;
    }
    .nav-toggle:checked ~ .nav-menu {
        display: flex;
    }
}
```

**Pros:** Zero JavaScript, accessible (checkbox-based), works with PicoCSS
**Cons:** Requires template change in all 3 `layout/default.html` files

**Option B: Keep wrapping, improve spacing** -- Minimal CSS tweaks to the existing wrapping nav. Less disruptive but worse UX.

**Recommendation:** Option A. The template change is identical across all 3 SCS layouts.

### 2.5 Touch-Friendly Button Sizes

Current small action buttons (`padding: 0.15rem 0.5rem; font-size: 0.85rem`) are too small for touch targets. Apple's HIG recommends minimum 44x44px touch targets.

```css
@media (max-width: 768px) {
    button, [role="button"], input[type="submit"] {
        min-height: 44px;
        min-width: 44px;
    }
    /* Override inline small button styles used in shopping list actions */
    .responsive-table button,
    .responsive-table [role="button"] {
        padding: 0.5rem 0.75rem !important;
        font-size: 1rem !important;
    }
}
```

### 2.6 Inline Forms in Table Cells

Several tables embed forms inside cells (participants stay-period, weightings, shopping list add). On mobile card layout:

- **Stay-period form** (trip detail): Currently a horizontal flex row with 2 date inputs + save button. On mobile cards, stack vertically.
- **Weighting form** (expense): Number input + update button inline. Fine as-is in card layout.
- **Add item form** (shopping list tfoot): 3 inputs + button. Stack vertically on mobile.

These work naturally with the CSS card approach since each `<td>` becomes a block-level element.

---

## 3. Shared Layout Considerations

### 3.1 Current State: Three Identical CSS Files

The three `style.css` files are almost identical:
- **IAM** (`travelmate-iam/src/main/resources/static/css/style.css`): 151 lines -- has toast styles, shopping list styles via copy
- **Trips** (`travelmate-trips/src/main/resources/static/css/style.css`): 171 lines -- has toast, shopping list, sr-only
- **Expense** (`travelmate-expense/src/main/resources/static/css/style.css`): 74 lines -- minimal, missing toast/shopping list styles

This is a maintenance burden. Adding responsive CSS to all three is error-prone.

### 3.2 Recommended Approach: Shared CSS via Gateway Static Resources

**Option A: Shared CSS served by Gateway (RECOMMENDED)**

Add a `travelmate-responsive.css` file served by the Gateway at `/css/travelmate-responsive.css`. All SCS layouts reference it:

```html
<link rel="stylesheet" href="/css/travelmate-responsive.css">
<link rel="stylesheet" th:href="@{/css/style.css}">
```

The Gateway already serves static resources (manifest.json, icons). Adding a CSS file is trivial.

**Pros:**
- Single source of truth for responsive styles
- Each SCS keeps its own `style.css` for context-specific overrides
- Gateway already handles static resources
- No build tooling changes

**Cons:**
- Extra HTTP request (mitigated by HTTP/2 multiplexing through Gateway)
- Need to ensure Gateway routes the CSS correctly

**Option B: Consolidate all shared CSS into each SCS `style.css`**

Copy-paste responsive rules into all three files.

**Cons:** Triple maintenance, already proven problematic (IAM and Expense CSS are diverging)

**Option C: Extract `travelmate-common` static resources module**

Not feasible -- `travelmate-common` is a plain JAR with no web resources, and Spring Boot static resource resolution is per-application.

**Recommendation:** Option A. The responsive CSS should be a shared file served by Gateway, while SCS-specific styles remain in their own `style.css`.

### 3.3 Layout Templates (3x default.html)

All three `layout/default.html` files are functionally identical (same nav structure, same scripts, same PicoCSS CDN link). The hamburger menu change needs to be applied to all three.

Consider extracting a Thymeleaf fragment to reduce duplication -- but this is not possible across SCS boundaries since each is a separate Spring Boot application. The three layouts must be synchronized manually.

**Recommendation:** Make the changes in one SCS first (Trips -- highest mobile priority), validate, then copy to IAM and Expense.

---

## 4. Impact on Existing HTMX Patterns

### 4.1 Fragment Restructuring Analysis

| Fragment | hx-target | hx-swap | Table Restructure Impact |
|----------|-----------|---------|--------------------------|
| members (IAM) | `#members` | `innerHTML` | None -- target is wrapper div |
| companions (IAM) | `#companions` | `innerHTML` | None -- target is wrapper div |
| invitationList (Trips) | `#invitations` | `innerHTML` | None -- target is wrapper div |
| itemLists (Shopping) | `#shopping-list-content` | `innerHTML` | None -- target is wrapper div |
| itemRow (Shopping) | `closest tr` | `outerHTML` | **SAFE** -- `<tr>` still exists in DOM, just displayed as block |
| emptyRow (Shopping) | `closest tr` | `outerHTML` | SAFE -- removes the element |
| itemActions (Shopping) | various `closest tr` | `outerHTML` | **SAFE** -- same reason |
| receiptList (Expense) | `#receipts` | `innerHTML` | None -- target is wrapper div |
| weightingList (Expense) | `#weightings` | `innerHTML` | None -- target is wrapper div |

**Key finding:** The CSS-only card approach (Option A from Section 2.2) is specifically chosen because it preserves the HTML structure. All `hx-target` selectors continue to work because `<tr>` elements still exist in the DOM -- they are simply styled as blocks via CSS `display: block`. This is the critical reason to prefer CSS-only over dual-render.

### 4.2 Polling on Mobile

The ShoppingList uses `hx-trigger="every 5s"` for real-time updates. Mobile concerns:

1. **Battery drain:** 5-second polling is aggressive for mobile. Consider:
   - Increasing to 10-15s on mobile (not easily done with pure CSS/HTMX)
   - Using `hx-trigger="every 5s [document.visibilityState === 'visible']"` to stop polling when the tab is backgrounded (HTMX supports this natively)
   - This is a one-line template change with significant battery savings

2. **Data usage:** Each poll returns the full item list HTML. For a 20-item list, this is ~5-10KB per poll = ~60-120KB/minute. Acceptable on modern mobile data plans but worth monitoring.

**Recommendation:** Add visibility check to polling trigger:
```html
hx-trigger="every 5s [document.visibilityState === 'visible']"
```

This is a quick win applicable now regardless of the mobile refactoring.

---

## 5. Testing Strategy

### 5.1 Playwright Viewport Testing

Playwright supports `page.setViewportSize()` and browser context device emulation. Recommended approach:

```java
// Mobile viewport test
page.setViewportSize(375, 812); // iPhone X dimensions
page.navigate(baseUrl + "/trips/");
// Assert card layout is visible, table headers are hidden
assertThat(page.locator("thead")).isHidden();
assertThat(page.locator("tbody tr")).hasCSS("display", "block");
```

**Recommended device viewports to test:**
- **375x812** -- iPhone (most common mobile)
- **768x1024** -- iPad portrait (tablet breakpoint boundary)
- **1280x720** -- Desktop baseline

**Implementation:** Create a `MobileResponsiveTest` base class or parameterized test that runs key flows at all three viewports.

### 5.2 Lighthouse CI Integration

Lighthouse CI was mentioned in the Iteration 10 scope but no `.lighthouserc.js` config exists yet.

Recommended setup:

```json
{
  "ci": {
    "collect": {
      "url": [
        "http://localhost:8080/trips/",
        "http://localhost:8080/iam/dashboard"
      ],
      "settings": {
        "preset": "mobile",
        "onlyCategories": ["performance", "accessibility", "best-practices"]
      }
    },
    "assert": {
      "assertions": {
        "categories:performance": ["warn", { "minScore": 0.7 }],
        "categories:accessibility": ["error", { "minScore": 0.9 }],
        "categories:best-practices": ["warn", { "minScore": 0.9 }]
      }
    }
  }
}
```

**Note:** Running Lighthouse CI requires the full stack to be running (Docker Compose + Gateway + all SCS). This means it cannot run as a unit test -- it should be an E2E-profile task or CI pipeline step.

**Target:** Lighthouse mobile score >= 90 (as specified in the feedback memory).

### 5.3 Visual Regression Testing

For a project of this scope, full visual regression testing (Percy, Chromatic) is overkill. Recommended lighter approach:

- **Playwright screenshot assertions** for critical mobile layouts:
  ```java
  assertThat(page).hasScreenshot("shopping-list-mobile.png",
      new PageAssertions.HasScreenshotOptions().setMaxDiffPixelRatio(0.01));
  ```
- Store baseline screenshots in `travelmate-e2e/src/test/resources/screenshots/`
- Run visual checks as part of E2E suite

---

## 6. Scope Estimation

### 6.1 Per-Page Effort Estimation

| Page | Effort | Notes |
|------|--------|-------|
| **Shared: responsive.css** | M (4h) | Breakpoints, card pattern, dialog full-screen, touch targets |
| **Shared: nav hamburger** | M (3h) | 3x layout/default.html + CSS |
| **Trips: shopping list** | L (6h) | 2 tables (6 cols each), action buttons, add-item form, polling |
| **Trips: trip detail** | L (5h) | Participants table with inline forms, 2 dialogs |
| **Trips: meal plan** | L (5h) | 4-column grid with embedded selects -- most complex table |
| **Trips: trip list** | S (2h) | Simple 5-col table, already has card-style invitations section |
| **Trips: recipe list** | S (2h) | Simple 4-col table |
| **Trips: accommodation** | L (6h) | 5-col table, nested forms, room assignment, 2 dialogs |
| **Expense: detail** | XL (8h) | 7 tables, 2 dialogs, inline forms, advance payments |
| **Expense: receipts** | L (5h) | 6-7 cols, approve/reject dialogs per row |
| **Expense: weightings** | S (2h) | 3 cols with inline form |
| **IAM: dashboard** | M (3h) | Members + companions tables, invite forms |
| **E2E: mobile viewport tests** | L (6h) | Parameterized tests for 3 viewports, key flows |
| **E2E: Lighthouse CI setup** | M (4h) | Config, Docker integration, CI pipeline |

**Total estimated effort: ~59 hours (8-10 dev days)**

### 6.2 Minimum Viable Mobile Experience (MVP)

The primary mobile use case is **shopping at the store** (ShoppingList) and **scanning receipts** (Expense). Focus the MVP on these:

**MVP Scope (Iteration 11a -- ~3-4 days):**
1. Shared responsive CSS file (card pattern, dialog full-screen, touch targets)
2. Navigation hamburger menu (3x layout)
3. Shopping list responsive cards + action buttons
4. Expense: add-receipt dialog + scan-receipt dialog full-screen
5. Polling visibility check (one-line fix)
6. Basic mobile E2E tests (shopping list flow, receipt scan flow)

**Extended Scope (Iteration 11b -- ~4-5 days):**
7. Trip detail (participants, invitations, stay-period forms)
8. Meal plan grid
9. Accommodation page
10. All remaining tables (IAM dashboard, recipe list, expense detail)
11. Lighthouse CI integration
12. Full mobile E2E coverage

### 6.3 Recommended Iteration 11 Scope

Given that this is a purely CSS/HTML iteration with no domain logic, I recommend doing it in two phases within a single iteration:

**Phase 1 (Stories S11-A through S11-D):**
- S11-A: Shared responsive CSS infrastructure (responsive.css via Gateway, hamburger nav)
- S11-B: Shopping List mobile cards (primary mobile use case)
- S11-C: Expense dialogs mobile full-screen (receipt scan + add receipt)
- S11-D: Mobile E2E tests for Phase 1 pages

**Phase 2 (Stories S11-E through S11-H):**
- S11-E: Trip detail + invitations responsive
- S11-F: Meal plan + accommodation responsive
- S11-G: Remaining tables (IAM dashboard, recipe list, expense detail)
- S11-H: Lighthouse CI integration + full mobile E2E

---

## 7. Architecture Conformance Check

| Invariant | Impact | Status |
|-----------|--------|--------|
| Domain layer framework-free | No impact | OK |
| TenantId aggregate isolation | No impact | OK |
| Async SCS communication | No impact | OK |
| Database isolation per SCS | No impact | OK |
| Flyway schema ownership | No impact | OK |
| Server-side rendering (Thymeleaf + HTMX) | Confirmed -- no SPA framework introduced | OK |
| Event publishing pattern | No impact | OK |
| Shared Kernel purity | No impact (CSS is not in common module) | OK |

The mobile UX refactoring is a **pure presentation layer concern** that does not affect any architecture invariants.

---

## 8. Risks

| ID | Risk | Impact | Likelihood | Mitigation |
|----|------|--------|------------|------------|
| R1 | CSS card pattern breaks HTMX `closest tr` selectors | High | Low | CSS-only approach preserves DOM structure; verify with automated tests |
| R2 | PicoCSS `<dialog>` full-screen override conflicts with future PicoCSS updates | Low | Medium | Pin PicoCSS version, scope overrides narrowly |
| R3 | Three-SCS CSS synchronization drift | Medium | High | Shared responsive.css via Gateway; minimize SCS-specific responsive rules |
| R4 | Meal plan table too complex for CSS-only card conversion | Medium | Medium | May need a custom mobile layout (day-by-day accordion) instead of card pattern |
| R5 | E2E tests become brittle with viewport-dependent assertions | Medium | Medium | Use semantic assertions (visibility, count) not pixel-based |
| R6 | Mobile polling drains battery on shopping list page | Low | High | Add visibility check to `hx-trigger` (quick fix) |

---

## 9. Recommendations (Prioritized)

1. **[Quick Win]** Add `document.visibilityState` check to ShoppingList polling -- one-line change, immediate battery savings on mobile
2. **[High]** Create shared `responsive.css` served by Gateway -- foundation for all responsive work
3. **[High]** Implement CSS-only card pattern for tables -- preserves HTMX fragment compatibility
4. **[High]** Prioritize Shopping List and Expense scan dialogs -- primary mobile use cases
5. **[Medium]** Implement CSS-only hamburger nav -- apply to all 3 layout templates
6. **[Medium]** Add `data-label` attributes to all `<td>` elements -- enables CSS card pattern
7. **[Medium]** Set up Playwright mobile viewport E2E tests -- catch regressions early
8. **[Low]** Lighthouse CI integration -- track mobile performance over time
9. **[Low]** Consider dedicated mobile layout for MealPlan -- day-by-day accordion may be more usable than card pattern for a calendar-grid UI

---

## 10. ADR Candidate

**ADR-0019: Mobile-Responsive Strategie (CSS-only mit PicoCSS)**

Key decisions to document:
- CSS-only responsive approach (no media queries on server, no user-agent sniffing)
- Shared `responsive.css` via Gateway
- `data-label` + CSS `display:block` pattern for table-to-card conversion
- Preservation of HTMX fragment compatibility through DOM structure preservation
- PicoCSS dialog override for mobile full-screen sheets

This ADR should be written before implementation begins.
