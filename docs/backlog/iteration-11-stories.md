# Iteration 11 — Refined User Stories: Mobile UX Refactoring

**Date**: 2026-03-19
**Target Version**: v0.11.0
**Bounded Contexts**: All SCS (IAM, Trips, Expense), Gateway/Infrastructure
**Driver**: User feedback — the entire UI is unusable on mobile. Dialogs are too wide, tables require
horizontal scrolling, no responsive card layouts. The primary mobile use cases (shopping at the store,
receipt scanning, viewing settlement) are the highest-priority targets.

---

## Overview

Travelmate is used on phones. The evidence is clear:

- The Shopping List (`shoppinglist/overview.html`) is THE primary in-store mobile workflow — participants
  use it live while shopping. It is currently a table with six columns and tiny button targets.
- The Receipt Scan workflow (`expense/detail.html` → scan dialog → `expense/scan-result.html`) requires
  camera access; the dialog that wraps it is a desktop-sized overlay.
- The Settlement page (`expense/detail.html`) has seven separate `<table>` elements, none of which are
  readable on a 360 px viewport.
- Every `<dialog>` in the application (`invite-member-dialog`, `invite-external-dialog`,
  `add-receipt-dialog`, `scan-receipt-dialog`) renders at its natural width and overflows mobile viewports.

This iteration is a CSS-first, template-refactoring iteration. No new domain logic, no new aggregates,
no Flyway migrations. The work is confined to:
1. `travelmate-*/src/main/resources/static/css/style.css` (each SCS has its own)
2. `travelmate-*/src/main/resources/templates/**/*.html`
3. `travelmate-gateway/src/main/resources/static/css/style.css` (if shared styles are promoted)
4. One GitHub Actions CI step for Lighthouse mobile auditing

### Technical Baseline

The stack already supports mobile refactoring without new dependencies:
- **PicoCSS 2** — semantic CSS reset, already used; supports `@media` queries natively
- **Thymeleaf fragments** — used throughout; responsive fragments can replace table fragments cleanly
- **HTMX 2.0** — the shopping list HTMX patterns (`hx-target`, `hx-swap="outerHTML"`) work identically
  with card layouts as with table rows when the fragment markup is updated consistently
- **Native `<dialog>`** — already used for all modals; CSS `@media` transforms to bottom sheet

### What Changes and What Stays the Same

What changes:
- CSS `style.css` in each SCS — new utility classes, `@media (max-width: 768px)` rules
- Template HTML structure for the specific pages listed per story
- The shared `layout/default.html` nav in all three SCS (S11-A)

What does NOT change:
- Controller logic, service layer, domain model, application services
- URL routes, form action paths, HTMX endpoint paths
- i18n message keys — all existing `th:text="#{...}"` keys stay the same
- HTMX polling behavior (`hx-trigger="every 5s"`) — only the rendered HTML structure changes
- Keycloak, RabbitMQ, PostgreSQL, event publishing

### HTMX Fragment Compatibility Note

The Shopping List (S11-D) uses HTMX fragments: `itemRow` is swapped via `hx-swap="outerHTML"` on
individual table rows. When converting to cards, the fragment `itemRow` must produce `<article>` (or
equivalent card element) instead of `<tr>`. The `hx-target` in the action forms must be updated from
`closest tr` to `closest article` (or a wrapper `div`). This is the most technically load-bearing
change in the iteration — both the full-page render and the HTMX partial update path must produce
the same card structure.

---

## Dependency Graph

```
S11-A: Responsive Navigation
  — no dependencies; affects all three layout/default.html files
  — delivers the shared CSS utility classes used by other stories

S11-B: Trip List → Mobile Cards
  — depends on S11-A shared CSS utilities
  — touches trip/list.html only

S11-C: Trip Detail → Mobile Layout
  — depends on S11-A
  — touches trip/detail.html; does NOT touch accommodation/overview.html (separate page)

S11-D: Shopping List Mobile-First
  — depends on S11-A
  — HTMX fragment compatibility requirement: itemRow fragment must be card-shaped
  — highest user value in the iteration

S11-E: Receipt Scan Mobile-First
  — depends on S11-F (dialogs → sheets) for the scan dialog treatment
  — touches expense/detail.html scan dialog + expense/scan-result.html

S11-F: Dialogs → Mobile Sheets
  — depends on S11-A (CSS utilities)
  — purely CSS; no template structural changes
  — should be implemented before S11-E and S11-C invite dialogs

S11-G: Expense Settlement Mobile
  — depends on S11-A + S11-F
  — touches expense/detail.html (tables only; dialog already covered by S11-F)

S11-H: Participant/Member Tables → Cards
  — depends on S11-A
  — touches trip/detail.html (participants table), trip/invitations.html,
    dashboard/members.html, dashboard/companions.html

S11-I: Lighthouse Mobile Score >= 90
  — depends on S11-A through S11-H being complete
  — CI step validates the sum of all refactoring work
```

---

## Recommended Iteration 11 Scope

| ID | Story | Priority | Size | Bounded Context |
|----|-------|----------|------|-----------------|
| S11-A | Responsive Navigation | Must | S | All SCS (IAM, Trips, Expense) |
| S11-F | Dialogs → Mobile Sheets | Must | M | All SCS |
| S11-D | Shopping List Mobile-First | Must | M | Trips |
| S11-E | Receipt Scan Mobile-First | Must | M | Expense |
| S11-B | Trip List → Mobile Cards | Should | S | Trips |
| S11-C | Trip Detail → Mobile Layout | Should | M | Trips |
| S11-G | Expense Settlement Mobile | Should | M | Expense |
| S11-H | Participant/Member Tables → Cards | Should | S | IAM + Trips |
| S11-I | Lighthouse Mobile Score >= 90 | Could | S | Infrastructure |

**Scope rationale:**

S11-A and S11-F are Must because every other story depends on them. Navigation that wraps poorly
makes the entire app feel broken; dialogs that overflow the viewport make all modal flows unusable.

S11-D is Must because the Shopping List is the highest-frequency mobile workflow — a participant at
the supermarket is the canonical use case that gave rise to this iteration.

S11-E is Must because receipt scanning is the second primary mobile use case and was implemented
specifically for mobile (camera capture), yet the wrapping dialog defeats the purpose.

S11-B, S11-C, S11-G, S11-H are Should: important for overall mobile usability but less
time-critical than the shopping and receipt flows.

S11-I (Lighthouse) is Could: it validates the work already done. Given S10-E (Lighthouse CI) is
already in scope for Iteration 10, this story raises the mobile performance threshold.

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S11-A | Foundation: shared CSS utilities + navigation fix; all other stories build on this |
| 2 | S11-F | Dialogs → sheets: purely CSS, no structural changes; unblocks S11-E and S11-C dialogs |
| 3 | S11-D | Shopping List: highest user value; most complex (HTMX fragment refactor) |
| 4 | S11-E | Receipt Scan: dialog fixed by S11-F; camera form and scan-result page refactoring |
| 5 | S11-H | Member/Companion/Invitation tables → cards: small scope, quick wins |
| 6 | S11-B | Trip list → cards: small scope, safe change |
| 7 | S11-C | Trip Detail accordion: medium scope, needs care around existing HTMX update targets |
| 8 | S11-G | Settlement mobile: many tables, methodical but predictable |
| 9 | S11-I | Lighthouse: validates the entire iteration's output |

---

## Shared CSS Strategy (applies to all stories)

All three SCS (`travelmate-iam`, `travelmate-trips`, `travelmate-expense`) maintain separate
`style.css` files. The responsive utility classes introduced in S11-A should be added to all
three files (copy-paste initially; promotion to a shared CSS artifact is a separate story if needed).

### CSS utility classes to introduce (S11-A defines, all SCS adopt)

```css
/* Mobile-first card grid */
.card-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 1rem;
}
@media (min-width: 768px) {
    .card-grid {
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    }
}

/* Large tap target buttons (min 44px touch target per WCAG 2.5.5) */
.btn-touch {
    min-height: 44px;
    min-width: 44px;
    padding: 0.75rem 1.25rem;
    font-size: 1rem;
}

/* Responsive table: hide on mobile, cards replace it */
@media (max-width: 767px) {
    .mobile-hidden { display: none !important; }
    .mobile-full-width { width: 100%; }
}

/* Bottom sheet dialog on mobile */
@media (max-width: 767px) {
    dialog[open] {
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        top: auto;
        margin: 0;
        width: 100%;
        max-width: 100%;
        border-radius: 1rem 1rem 0 0;
        max-height: 90dvh;
        overflow-y: auto;
    }
    dialog::backdrop {
        background: rgba(0, 0, 0, 0.5);
    }
}

/* Filter pill nav */
.filter-pills {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
    overflow-x: auto;
    padding-bottom: 0.25rem;
    scrollbar-width: none;
}
.filter-pills a {
    white-space: nowrap;
    padding: 0.4rem 1rem;
    border-radius: 2rem;
    border: 1px solid var(--pico-primary);
    font-size: 0.9rem;
    text-decoration: none;
}
.filter-pills a.contrast {
    background: var(--pico-primary);
    color: var(--pico-primary-inverse);
}
```

The `bottom sheet` pattern is CSS-only — no JavaScript required. The native `<dialog>` element
already handles focus trapping, backdrop, and close-on-Escape.

---

## Story S11-A: US-INFRA-060 — Responsive Navigation

**Epic**: E-INFRA-06
**Priority**: Must
**Size**: S
**As a** user on a phone, **I want** the navigation bar to be usable without scrolling, **so that**
I can reach any section without pinching and panning.

### Background

The current `layout/default.html` in all three SCS renders a single `<nav class="container">` with
two `<ul>` elements: brand on the left, five links on the right. On a 360 px viewport these links
wrap into two rows and push the main content down. The brand link overlaps with the nav list.
PicoCSS 2's `<nav>` component supports a hamburger-style collapsible pattern without JavaScript
when used with `<details>` and `<summary>`.

### Acceptance Criteria

- **Given** I am using a phone (viewport <= 767px), **When** any page loads,
  **Then** the navigation renders as a top bar with the Travelmate brand name on the left and a
  hamburger menu icon on the right. No nav items are visible by default.

- **Given** the navigation is collapsed, **When** I tap the hamburger icon,
  **Then** the menu expands to show all nav items (Reisepartei, Reisen, Rezepte, DE/EN, Abmelden)
  as a vertical list. Each item is a full-width tap target of at least 44px height.

- **Given** the menu is open, **When** I tap a nav item,
  **Then** the page navigates and the menu collapses.

- **Given** I am using a tablet or desktop (viewport >= 768px), **When** any page loads,
  **Then** the navigation renders as before — horizontal nav bar with all items visible. No hamburger.

- **Given** I am on the IAM dashboard, **When** I view the nav,
  **Then** the same responsive behavior applies (IAM has its own `layout/default.html`).

- **Given** I am on the Expense page, **When** I view the nav,
  **Then** the same responsive behavior applies (Expense has its own `layout/default.html`).

### Technical Notes

- Bounded Context: All SCS (IAM, Trips, Expense) — three separate `layout/default.html` files
- Implementation: PicoCSS `<nav>` with `<details>/<summary>` pattern (no JavaScript)
  ```html
  <nav class="container">
    <ul>
      <li><a href="/iam/dashboard"><strong>Travelmate</strong></a></li>
    </ul>
    <ul class="mobile-hidden"><!-- desktop items --></ul>
    <details class="mobile-nav-dropdown">
      <summary aria-label="Menu"><!-- hamburger icon or text --></summary>
      <ul><!-- same nav items, vertical layout --></ul>
    </details>
  </nav>
  ```
- Introduce the shared CSS utility classes (`.mobile-hidden`, `.btn-touch`, `.card-grid`,
  `.filter-pills`, dialog bottom-sheet rules) in all three `style.css` files
- No controller changes, no i18n changes
- Test strategy: Playwright viewport test at 375px width — assert hamburger visible, items hidden;
  click hamburger, assert items visible. Repeat at 1024px — assert hamburger hidden, items visible.
- BDD scenario: "Als mobiler Nutzer möchte ich eine bedienbare Navigation"

---

## Story S11-F: US-INFRA-061 — Dialogs → Mobile Bottom Sheets

**Epic**: E-INFRA-06
**Priority**: Must
**Size**: M
**As a** user on a phone, **I want** modal dialogs to open from the bottom of the screen (bottom sheet
style), **so that** forms are fully visible without needing to scroll a floating overlay.

### Background

The following `<dialog>` elements exist in the application:

| Dialog ID | Page | SCS |
|-----------|------|-----|
| `invite-member-dialog` | `trip/detail.html` | Trips |
| `invite-external-dialog` | `trip/detail.html` | Trips |
| `add-receipt-dialog` | `expense/detail.html` | Expense |
| `scan-receipt-dialog` | `expense/detail.html` | Expense |
| Room add/edit dialogs | `accommodation/overview.html` | Trips |
| Receipt review/reject dialog | `expense/receipts.html` | Expense |

On desktop, these dialogs render centered in the viewport — correct behavior. On mobile (375 px),
they render at their natural content width which typically overflows the screen horizontally, or
they center at a width that leaves no margin for touch. The PicoCSS dialog styles do not apply
`max-width: 100%` on mobile.

The bottom sheet pattern is already described in the shared CSS strategy (S11-A). This story
applies and verifies it across all dialogs and ensures dialog content is scrollable when it
exceeds viewport height.

### Acceptance Criteria

#### Add Receipt Dialog (Expense)

- **Given** I am on the Expense page on a phone, **When** I tap "Beleg hinzufügen",
  **Then** the dialog slides up from the bottom of the screen, covering the lower 90% of the
  viewport. The header ("Beleg hinzufügen"), all form fields, and the Cancel/Save buttons are
  visible without horizontal scrolling.

- **Given** the dialog is open and the form is taller than the viewport (e.g., on a small phone),
  **When** I scroll within the dialog,
  **Then** the dialog content scrolls while the backdrop stays fixed.

- **Given** I tap the backdrop (outside the dialog), **When** the touch registers,
  **Then** the dialog closes (PicoCSS/native behavior — no change needed here; just ensure CSS
  doesn't block the click).

#### Scan Receipt Dialog (Expense)

- **Given** I am on the Expense page on a phone, **When** I tap "Kassenzettel scannen",
  **Then** the scan dialog appears as a bottom sheet. The file input with `capture="environment"`
  is clearly visible and tap-accessible (min 44px tap target on the "Analysieren" button).

#### Invite Member Dialog (Trips)

- **Given** I am on a Trip detail page on a phone, **When** I tap "Mitglied einladen",
  **Then** the invite dialog appears as a bottom sheet. The member `<select>` dropdown and the
  Cancel/Send buttons are fully accessible without horizontal scrolling.

#### Invite External Dialog (Trips)

- **Given** I tap "Per E-Mail einladen" on a phone,
  **Then** the external invite dialog appears as a bottom sheet. The two-column `<div class="grid">`
  layout for name fields and the email/date-of-birth fields reflow to single-column on mobile.

#### Reject Receipt Dialog (Expense — receipts list)

- **Given** an Organizer taps the reject button for a receipt on a phone,
  **Then** the reject dialog appears as a bottom sheet with the rejection reason text area
  and the Reject/Cancel buttons accessible.

#### Desktop Unchanged

- **Given** I am on a desktop (viewport >= 768px), **When** I open any dialog,
  **Then** the dialog remains centered in the viewport, unchanged from current behavior.

#### No JavaScript Required

- **Then** the bottom sheet transformation is achieved purely via CSS `@media (max-width: 767px)`
  rules on the native `<dialog>` element. No additional JavaScript is added.

### Technical Notes

- Bounded Context: Trips (`trip/detail.html`, `accommodation/overview.html`), Expense
  (`expense/detail.html`, `expense/receipts.html`)
- CSS changes only: the `@media (max-width: 767px) dialog[open] { ... }` rule from S11-A is
  sufficient for all dialogs. This story is about verification + the `grid` → single-column
  reflow for dialog-internal form layouts.
- Two-column `<div class="grid">` inside dialogs must stack to single column on mobile. Add:
  ```css
  @media (max-width: 767px) {
      dialog .grid {
          grid-template-columns: 1fr;
      }
  }
  ```
- Animation (optional): add a slide-up transition via CSS `@starting-style` (supported in modern
  browsers; degrades gracefully to instant open on older browsers):
  ```css
  @media (max-width: 767px) {
      dialog[open] {
          animation: slideUp 0.25s ease-out;
      }
      @keyframes slideUp {
          from { transform: translateY(100%); }
          to   { transform: translateY(0); }
      }
  }
  ```
- No template structural changes needed for the dialog elements themselves — only CSS
- Test strategy: Playwright viewport test at 375px — open each dialog, assert no horizontal
  overflow (`scrollWidth <= clientWidth`), assert Cancel + Submit buttons visible in viewport.
- BDD scenario: "Als mobiler Nutzer möchte ich Dialoge als Bottom-Sheet sehen"

---

## Story S11-D: US-TRIPS-080 — Shopping List Mobile-First

**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: M
**As a** Participant shopping at the store, **I want** the Shopping List to be easy to use with one
hand on my phone, **so that** I can assign and check off items quickly while walking through the aisles.

### Background

The current Shopping List (`shoppinglist/overview.html`) is a six-column table (Name, Menge,
Einheit, Assignee, Status, Aktionen). On a 360 px phone this requires horizontal scrolling to
reach the action buttons. The action buttons themselves are styled with `padding: 0.15rem 0.5rem`
and `font-size: 0.85rem` — far below the 44px touch target minimum.

The HTMX polling (`hx-trigger="every 5s"`) swaps the `#shopping-list-content` div. The `itemRow`
fragment is swapped via `hx-swap="outerHTML"` on individual rows. Both paths must produce
consistent card markup. This is the primary structural constraint.

The filter nav (`Alle / Ausstehend / Übernommen / Erledigt`) is a `<nav><ul>` — on mobile this
wraps badly. It should become a horizontally scrollable pill row.

### Acceptance Criteria

#### Card Layout (replaces table rows on all viewports)

- **Given** the Shopping List has items, **When** I view it on a phone (375px),
  **Then** each item is displayed as a card with:
  - Item name prominently displayed (`font-size: 1.1rem; font-weight: bold`)
  - Quantity and unit on the same line below the name (e.g., "500 g")
  - Assignee name below that (or "—" if unassigned)
  - Status badge (Ausstehend / Übernommen / Erledigt)
  - Action buttons as large, full-width (or half-width side-by-side) tap targets (min 44px height)

- **Given** I am on a desktop (>= 768px), **When** I view the Shopping List,
  **Then** the table layout is still rendered (or an equivalent card grid). There is no requirement
  to keep the table on desktop — a card grid at ≥ 2 columns is acceptable and simpler to maintain.

#### Filter Pills

- **Given** the Shopping List has items, **When** I view the filter nav on a phone,
  **Then** the four filter options (Alle, Ausstehend, Übernommen, Erledigt) are shown as a
  horizontally scrollable pill row. The active filter pill is highlighted (filled background).

- **Given** I tap a filter pill, **When** the navigation occurs,
  **Then** the list is filtered and the correct pill is highlighted. (This is existing behavior;
  only the visual treatment changes.)

#### Action Button Tap Targets

- **Given** an item is OPEN, **When** I view its card on a phone,
  **Then** the "Ich übernehme" button has a minimum height of 44px and width of at least 120px.
  It is visually clear and easy to tap without hitting adjacent items.

- **Given** an item is ASSIGNED to me, **When** I view its card,
  **Then** "Abgeben" and "Erledigt" buttons are shown side by side or stacked, each at least
  44px tall. "Erledigt" uses a primary (filled) style; "Abgeben" uses outline/secondary style.

- **Given** an item is PURCHASED, **When** I view its card,
  **Then** the card is visually de-emphasized (opacity or strikethrough on name) and a smaller
  "Rückgängig" button is available. It remains >= 44px touch target.

#### HTMX Fragment Compatibility

- **Given** I tap "Ich übernehme" on an item, **When** the HTMX request completes,
  **Then** the card is updated in place (same position in the list) without a full page reload.
  The updated card shows my name as assignee and the "Abgeben" / "Erledigt" buttons.

- **Given** I tap "Erledigt", **When** the HTMX request completes,
  **Then** the card updates to show PURCHASED state. If the active filter is "Ausstehend" or
  "Übernommen", the card disappears from the list (existing behavior via `hx-swap="outerHTML"`
  returning empty).

- **Given** HTMX polling fires (every 5s), **When** the `/items` fragment is returned,
  **Then** the entire `#shopping-list-content` is replaced with the up-to-date card list.
  The card structure in the polled response is identical to the initial render.

#### Add Manual Item Form

- **Given** I want to add a manual item on a phone, **When** I view the manual items section,
  **Then** the add form is laid out vertically (name field full-width, quantity and unit on
  the same row, add button full-width below). No horizontal scrolling is needed.

- **Given** I submit the add form, **When** HTMX processes the response,
  **Then** the new item appears as a card in the manual items section.

#### Purchased Items De-emphasis

- **Given** the filter is "Alle" and some items are PURCHASED, **When** I view the list,
  **Then** purchased items appear at the bottom of each section with reduced opacity (0.6) or
  strikethrough on the item name, making open and assigned items visually prominent.

### Technical Notes

- Bounded Context: Trips
- Affected template: `shoppinglist/overview.html`
- CRITICAL — HTMX fragment update: the `th:fragment="itemRow"` fragment currently produces `<tr>`.
  It must be changed to produce an `<article class="shopping-item-card">` (or equivalent). The
  forms within the fragment must update `hx-target` from `closest tr` to `closest article`
  (or `closest .shopping-item-card`):
  ```html
  <!-- BEFORE -->
  <form ... hx-target="closest tr" hx-swap="outerHTML" ...>
  <!-- AFTER -->
  <form ... hx-target="closest article" hx-swap="outerHTML" ...>
  ```
  This change propagates through ALL action forms: assign, unassign, purchase, undo-purchase, delete.
- The `itemLists` fragment (used for polling) must produce the same card structure as the full
  page render. Both rendering paths go through the same fragment — no duplication.
- New CSS classes for shopping items:
  ```css
  .shopping-item-card { ... }
  .shopping-item-card.item-purchased { opacity: 0.55; }
  .shopping-item-card .item-name { font-size: 1.1rem; font-weight: 600; }
  .shopping-item-card .item-qty  { color: var(--pico-muted-color); font-size: 0.9rem; }
  .shopping-item-card .item-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.75rem; }
  .shopping-item-card .item-actions button { min-height: 44px; flex: 1; }
  ```
- The two sections ("Aus dem Essensplan" / "Manuelle Einträge") remain as separate `<article>`
  containers with section headings — only the item rows inside become cards.
- Regenerate button remains in the section header — no change to its position.
- Test strategy:
  - Unit tests: no new unit tests (template change only)
  - Controller tests: existing tests verify model attributes; no structural test changes needed
  - E2E test (Playwright at 375px viewport): load shopping list, assert no horizontal overflow,
    assert "Ich übernehme" button >= 44px height, tap button, assert HTMX update produces card
    with assignee name, tap "Erledigt", assert card shows PURCHASED state.
  - E2E test: add manual item via the vertical form, assert card appears.
- BDD scenario: "Als Teilnehmer im Supermarkt möchte ich Einkaufsartikel schnell abhaken"

---

## Story S11-E: US-EXP-070 — Receipt Scan Mobile-First

**Epic**: E-EXP-01
**Priority**: Must
**Size**: M
**As a** Participant at a till, **I want** the receipt scanning flow to work smoothly on my phone, **so that**
I can photograph a receipt and submit the expense in under 30 seconds.

### Background

The receipt scan flow implemented in Iteration 10 uses a `<dialog id="scan-receipt-dialog">` in
`expense/detail.html`. On mobile this dialog suffers from the same overflow problem described in
S11-F. Additionally, the scan result page (`expense/scan-result.html`) renders a pre-filled form —
on mobile this form needs large inputs and a clearly visible submit button.

The file input uses `accept="image/*" capture="environment"` which opens the camera directly on
Android/iOS — the correct behavior. The issue is that the wrapping dialog is unusable before the
camera even opens.

### Acceptance Criteria

#### Scan Dialog (mobile)

- **Given** I am on the Expense page on a phone, **When** I tap "Kassenzettel scannen",
  **Then** the scan dialog appears as a bottom sheet (handled by S11-F). Inside the dialog:
  - A short explanatory text is visible without scrolling
  - The file input (camera button) is styled as a large tap target (min 44px, full width)
  - The "Analysieren" button is full-width, min 44px height, primary style
  - The HTMX loading spinner is visible inside the dialog while OCR runs

- **Given** I tap the file input, **When** the browser opens the camera or gallery,
  **Then** the dialog remains in the background; after I select the image, I return to the
  dialog with the selected file shown (filename or thumbnail preview).

#### Scan Result Page / Pre-filled Form

- **Given** OCR completes and the server returns the pre-filled form (`expense/scan-result.html`),
  **When** the result is shown inside the dialog (via `hx-target="#scan-result-container"`),
  **Then**:
  - The extracted amount and date fields are large and clearly editable (font-size >= 1rem)
  - Any OCR confidence hint ("Betrag konnte nicht erkannt werden — bitte manuell eingeben") is
    shown in a colored notice box above the fields, not below small-print
  - The receipt photo thumbnail is shown above the form fields (not below, where it may be
    missed)
  - The "Beleg einreichen" button is full-width, min 44px height, and the primary color

- **Given** I am on a phone and the scan result form is taller than the visible dialog area,
  **When** I scroll within the dialog,
  **Then** I can reach the submit button without closing the dialog.

#### Camera Access (no change to logic)

- **Given** I am on a phone, **When** I tap the file input,
  **Then** the camera opens directly (existing `capture="environment"` attribute — no change).
  This acceptance criterion documents existing behavior that must not regress.

#### Desktop Unchanged

- **Given** I am on a desktop, **When** I open the scan dialog,
  **Then** the file picker opens (no camera). The form layout uses the existing two-column grid.

### Technical Notes

- Bounded Context: Expense
- Affected templates: `expense/detail.html` (scan dialog content), `expense/scan-result.html`
- The HTMX target `#scan-result-container` is inside the dialog — the result form renders inside
  the bottom sheet, keeping the user in context. No navigation to a separate page.
- CSS changes for scan dialog interior:
  ```css
  @media (max-width: 767px) {
      #scan-receipt-dialog label input[type="file"] {
          min-height: 56px;
          width: 100%;
          display: block;
      }
      #scan-receipt-dialog footer button[type="submit"] {
          width: 100%;
          min-height: 44px;
      }
  }
  ```
- `expense/scan-result.html` changes: ensure the thumbnail `<img>` is `width: 100%; max-height: 200px; object-fit: contain` on mobile; form fields are full-width; submit button is full-width.
- No controller changes, no domain changes, no new routes.
- Test strategy:
  - E2E test (Playwright at 375px): open scan dialog, assert no horizontal overflow, assert
    "Analysieren" button >= 44px height. Mock OCR endpoint to return a pre-filled form; assert
    the form is visible and submit button is tappable.
  - Regression: existing E2E scan tests must still pass.
- BDD scenario: "Als Teilnehmer möchte ich einen Kassenzettel per Handy fotografieren und einreichen"

---

## Story S11-B: US-TRIPS-081 — Trip List → Mobile Cards

**Epic**: E-TRIPS-01
**Priority**: Should
**Size**: S
**As a** user on a phone, **I want** the Trip list to be displayed as cards, **so that** I can scan
my trips at a glance without horizontal table scrolling.

### Background

`trip/list.html` renders a `<table>` with five columns (Name, Von, Bis, Status, action). On mobile
this wraps or overflows. The pending invitations section at the top already uses a `<div class="grid">`
card layout — consistent with what the trip list should become.

### Acceptance Criteria

- **Given** I am on the Trip list page on a phone, **When** the page loads,
  **Then** each Trip is displayed as a card containing:
  - Trip name as a clickable link (large, primary color)
  - Date range below: "15.03.2026 – 22.03.2026"
  - Status badge (`<mark>`)
  - "Zur Abrechnung" link for COMPLETED trips (if applicable)

- **Given** I am on a desktop, **When** the page loads,
  **Then** trips are shown as a two- or three-column card grid (not a table). The table is removed
  entirely. The card grid renders with at least two cards per row on desktop.

- **Given** there are no trips, **When** the page loads,
  **Then** the empty state message and the "Neue Reise planen" button are displayed as before.

- **Given** I have pending invitations, **When** the page loads,
  **Then** the pending invitation cards above the trip list are unchanged in appearance.

### Technical Notes

- Bounded Context: Trips
- Affected template: `trip/list.html`
- Replace `<figure><table>...</table></figure>` with a `<div class="card-grid">` of `<article>`
  elements. The card grid class is defined in S11-A.
- No HTMX interactions on the trip list (static render) — no fragment compatibility concern.
- The pending invitations section is already card-based (`<div class="grid" th:each="...">`) —
  no change needed.
- Test strategy: E2E test at 375px — assert no horizontal scrollbar, assert trip name link visible.

---

## Story S11-C: US-TRIPS-082 — Trip Detail → Mobile Layout

**Epic**: E-TRIPS-01
**Priority**: Should
**Size**: M
**As a** user on a phone, **I want** the Trip detail page to be organized into collapsible sections,
**so that** I can navigate to the specific section I need (participants, shopping list, accommodation)
without scrolling through everything.

### Background

`trip/detail.html` currently renders eight `<article>` sections sequentially:
1. Trip Info Card (dates, status, action buttons)
2. Participants table
3. Invitations
4. Accommodation link
5. Meal Plan link
6. Shopping List link
7. Expense link (COMPLETED only)

On a phone, the user scrolls through all of these to reach the section they want. Trip info and
quick-action buttons (Bestätigen, Starten, etc.) should remain visible at the top. Everything else
can be collapsed by default on mobile.

### Acceptance Criteria

#### Mobile Layout — Collapsible Sections

- **Given** I am on a Trip detail page on a phone (viewport <= 767px), **When** the page loads,
  **Then** the Trip Info Card (name, dates, status, action buttons) is fully visible at the top.
  The Participants, Invitations, Accommodation, Meal Plan, Shopping List, and Expense sections
  are rendered as `<details>/<summary>` accordions, collapsed by default.

- **Given** a section is collapsed, **When** I tap its summary heading,
  **Then** the section expands to show its content (participants table/cards, invitation list, etc.).

- **Given** I expand the Participants section, **When** it opens,
  **Then** the participants are shown as cards (per S11-H), not a table.

- **Given** the Trip has a pending action (e.g., an open invitation awaiting my response),
  **When** the Invitations section is collapsed,
  **Then** a badge or count indicator on the summary heading shows the number of pending items
  (e.g., "Einladungen (2 offen)").

#### Desktop Layout — All Sections Visible

- **Given** I am on a desktop (viewport >= 768px), **When** the page loads,
  **Then** all sections are visible without accordion — same as current behavior.
  The `<details>` elements render with `open` attribute on desktop via CSS:
  ```css
  @media (min-width: 768px) {
      .mobile-accordion { display: contents; }
      .mobile-accordion > summary { display: none; }
  }
  ```

#### Invite Dialogs — Unchanged

- **Given** I expand the Invitations section and tap "Mitglied einladen",
  **Then** the invite dialog opens as a bottom sheet (handled by S11-F). The `<dialog>` elements
  remain in `trip/detail.html` — no structural change needed.

### Technical Notes

- Bounded Context: Trips
- Affected template: `trip/detail.html`
- Wrap the collapsible sections in `<details class="mobile-accordion">` with `<summary>` headings
- On mobile: `details.mobile-accordion` is a standard `<details>` — collapsed by default.
- On desktop: CSS hides the `<summary>` and shows the `<details>` content unconditionally.
- The HTMX target `#invitations` (used by the invite forms) remains inside the Invitations section.
  Because the `<details>` wrapper does not affect the `id` attribute or the HTMX swap targets,
  the invitation HTMX update works without change.
- Pending invitation badge: compute `pendingInvitationCount` in the model and pass it to the view.
  Render as `<summary>Einladungen <th:block th:if="${pendingInvitationCount > 0}">(<span
  th:text="${pendingInvitationCount}"></span> offen)</th:block></summary>`.
- New model attribute: `pendingInvitationCount` (Integer) in `TripController.detail()` — count of
  PENDING invitations for the current user's tenant. This is the only controller change in the story.
- Test strategy: E2E at 375px — load trip detail, assert only Trip Info Card visible by default,
  expand Participants, assert participant cards visible; expand Invitations, tap invite button,
  assert dialog opens. At 1024px, assert all sections visible.

---

## Story S11-G: US-EXP-071 — Expense Settlement Mobile

**Epic**: E-EXP-04
**Priority**: Should
**Size**: M
**As a** Participant checking the settlement on my phone, **I want** to see who owes whom without
having to scroll sideways through tables, **so that** I can quickly find my balance and any transfers
I need to make.

### Background

`expense/detail.html` is the most table-heavy page in the application. It contains:
1. Category Breakdown table (4 columns)
2. Daily Costs table (3 columns)
3. Receipts list (via `expense/receipts.html` fragment — separate story if needed)
4. Participant Summary table (4 columns)
5. Advance Payments table (4 columns)
6. Party Settlement table (5 columns)
7. Party-Level Transfers table (4 columns)
8. Settlement Plan (individual transfers) table (4 columns)

The Party Settlement (5 columns) and Party Transfers are the primary mobile use case: after the
trip, everyone wants to know "what do I owe" — this is the first thing looked up on a phone.

### Acceptance Criteria

#### Party Settlement — Cards (primary use case)

- **Given** I am on the Expense page on a phone, **When** I scroll to the Party Settlement section,
  **Then** each Travel Party's settlement is shown as a card:
  ```
  ┌────────────────────────────────────┐
  │  Familie Schmidt                   │
  │  Mitglieder: Max, Lisa, Tom        │
  │  Bezahlt: 320,00 EUR               │
  │  Anteil:  450,00 EUR               │
  │  Saldo:  -130,00 EUR  (rot)        │
  └────────────────────────────────────┘
  ```
  Positive saldo (owes nothing / is owed) is shown in green; negative in red.

#### Party Transfers — Cards

- **Given** I am on the Expense page on a phone, **When** I view the Ausgleichszahlungen section,
  **Then** each transfer is shown as a statement card:
  ```
  ┌────────────────────────────────────┐
  │  Familie Schmidt                   │
  │  zahlt an Familie Müller           │
  │  130,00 EUR                        │
  └────────────────────────────────────┘
  ```

#### Category Breakdown — Compact Cards or Bar List

- **Given** I am on the Expense page on a phone, **When** I view the Category Breakdown section,
  **Then** each category is shown as a two-line summary with a progress bar:
  ```
  Lebensmittel          290,00 EUR
  [============================--]   29 %
  ```
  The existing `<progress>` element and percentage are already in the template — only the table
  wrapper is replaced with a `<div>` list layout.

#### Daily Costs — Compact List

- **Given** I am on the Expense page on a phone, **When** I view the Daily Costs section,
  **Then** each day is shown as a single-line entry: `"15.03.2026 — 120,00 EUR (3 Belege)"`
  — a definition list (`<dl>/<dt>/<dd>`) or a simple list replaces the table.

#### Advance Payments — Cards

- **Given** I am on the Expense page on a phone, **When** I view the Advance Payments section,
  **Then** each party's advance payment is a card with party name, amount, and paid/unpaid status
  badge. The "Bezahlt" toggle button is full-width, min 44px.

#### Desktop Unchanged

- **Given** I am on a desktop, **When** I view the Expense page,
  **Then** all sections retain their current table layouts. The card/list layouts are
  `@media (max-width: 767px)` only.

### Technical Notes

- Bounded Context: Expense
- Affected template: `expense/detail.html`
- Pattern: add `display: none` to `<figure>` on mobile, show a `<div class="card-grid mobile-only">`
  sibling with the same data. Alternatively, use CSS-only `<table>` transformation (stack cells):
  ```css
  @media (max-width: 767px) {
      .responsive-table thead { display: none; }
      .responsive-table tr { display: block; margin-bottom: 1rem; border: 1px solid var(--pico-muted-border-color); border-radius: 0.5rem; padding: 0.75rem; }
      .responsive-table td { display: flex; justify-content: space-between; padding: 0.25rem 0; }
      .responsive-table td::before { content: attr(data-label); font-weight: bold; }
  }
  ```
  The CSS-only `td::before` approach requires adding `data-label` attributes to each `<td>`. This
  is the preferred approach for the settlement tables — it avoids duplicating the Thymeleaf data
  iteration and keeps a single template for both mobile and desktop.
- For the category breakdown: the existing `<progress>` + percentage layout works well as a list.
  Remove the `<table>` wrapper and use `<div>` rows directly.
- The receipts list (`expense/receipts.html` fragment) is left for a future polishing story —
  it is complex (HTMX, reject dialog, status badges) and is lower priority than settlement.
- Test strategy: E2E at 375px — load a settled expense, assert Party Settlement cards visible,
  assert no horizontal scrollbar on any section. Assert party transfer statement readable.

---

## Story S11-H: US-IAM-080 — Participant/Member Tables → Cards

**Epic**: E-IAM-03 / E-TRIPS-02
**Priority**: Should
**Size**: S
**As a** user on a phone, **I want** to see Members, Companions, and Trip Participants as cards
instead of tables, **so that** I can read the list without horizontal scrolling.

### Background

Tables with more than three columns on mobile require horizontal scrolling. The affected tables:

| Table | Columns | Template |
|-------|---------|----------|
| Members list | Name, Email, Actions | `dashboard/members.html` |
| Companions list | Name, Date of Birth, Actions | `dashboard/companions.html` |
| Participants table | Name, Arrival, Departure, Actions | `trip/detail.html` |
| Invitation list | Member, Status, Actions | `trip/invitations.html` |

All of these are moderate-data tables (typically 2–8 rows) where a card layout is natural and
more readable than a table on mobile.

### Acceptance Criteria

#### Members List (IAM Dashboard)

- **Given** I am on the IAM Dashboard on a phone, **When** I view the Members section,
  **Then** each Member is shown as a card:
  - Full name as the card heading
  - Email address below
  - "Entfernen" button as full-width, min 44px (organizer only)

#### Companions List (IAM Dashboard)

- **Given** I am on the IAM Dashboard on a phone, **When** I view the Companions section,
  **Then** each Companion is shown as a card:
  - Full name as the card heading
  - Date of birth below (formatted: dd.MM.yyyy)
  - "Entfernen" button as full-width, min 44px

#### Participants Table (Trip Detail)

- **Given** I am on a Trip detail page on a phone and the Participants section is expanded
  (per S11-C), **When** I view the participants,
  **Then** each participant is shown as a card:
  - Full name as the card heading
  - Arrival / Departure dates below
  - Stay period date pickers stacked vertically (not inline) with a "Speichern" button below

- **Given** I am the current user (or Organizer), **When** I view my own participant card,
  **Then** the date picker inputs are visible and large enough to tap (input height >= 44px).

#### Invitation List (Trip Detail / Invitations fragment)

- **Given** I view the Invitations section on a phone, **When** the invitation list renders,
  **Then** each invitation is shown as a card:
  - Invitee name
  - Status badge (Ausstehend / Angenommen / Abgelehnt)
  - "Accept" / "Decline" buttons for PENDING invitations belonging to the current user

#### Desktop Unchanged

- **Given** I am on a desktop, **When** I view any of these sections,
  **Then** the table layout is retained OR a multi-column card grid is shown — either is acceptable.
  The priority is mobile correctness.

### Technical Notes

- Bounded Context: IAM (`dashboard/members.html`, `dashboard/companions.html`), Trips
  (`trip/detail.html` participants section, `trip/invitations.html`)
- Affected templates: `dashboard/members.html`, `dashboard/companions.html`,
  `trip/invitations.html`, and the participants section within `trip/detail.html`
- For the participant stay-period form: the date inputs and save button within a table cell are
  currently laid out inline with `display:flex`. Convert to vertical stack on mobile:
  ```css
  @media (max-width: 767px) {
      .stay-period-form { flex-direction: column; align-items: stretch; }
      .stay-period-form input[type="date"] { width: 100%; }
      .stay-period-form button { width: 100%; min-height: 44px; }
  }
  ```
- The delete/remove HTMX patterns (`hx-delete`, `hx-target="closest tr"`) in member and
  companion lists must be updated to `hx-target="closest article"` when converting to cards —
  the same HTMX fragment compatibility concern as S11-D.
- `dashboard/members.html` and `dashboard/companions.html` contain HTMX-rendered fragments.
  The fragment structure (member cards, companion cards) must match between the initial render
  and the HTMX swap response.
- Test strategy: E2E at 375px on IAM dashboard — assert members rendered as cards (no table),
  assert email visible on card, assert delete button >= 44px. E2E at 375px on trip detail —
  expand participants, assert card layout, assert date inputs accessible.

---

## Story S11-I: US-INFRA-062 — Lighthouse Mobile Score >= 90

**Epic**: E-INFRA-05
**Priority**: Could
**Size**: S
**As a** developer, **I want** Lighthouse CI to audit key pages at a mobile viewport and enforce a
score of >= 90, **so that** the mobile refactoring gains are continuously protected.

### Background

S10-E (Iteration 10) introduced Lighthouse CI with thresholds for Performance >= 80 and
Accessibility >= 90 at the default (desktop-ish) preset. This story raises the bar: the mobile
preset (375px viewport, throttled 4G) is used and the Performance threshold is raised to 90 to
reflect the completed mobile refactoring.

### Acceptance Criteria

- **Given** a pull request is opened, **When** Lighthouse CI runs after E2E tests,
  **Then** the Shopping List page (`/trips/{tripId}/shoppinglist`) is audited at a mobile preset
  (375px viewport, throttled 4G) in addition to the existing Trip list page.

- **Given** the Lighthouse audit runs on the Shopping List page,
  **Then** the following thresholds must pass:
  - Performance: >= 90 (raised from 80)
  - Accessibility: >= 90 (unchanged)
  - Best Practices: >= 90 (unchanged)

- **Given** the Lighthouse audit runs on the Trip list page,
  **Then** the existing thresholds apply (Performance >= 80, Accessibility >= 90 — updated to
  Performance >= 90 if the refactoring achieves it, confirmed post-implementation).

- **Given** any threshold is breached, **When** the CI step completes,
  **Then** the pipeline is marked as failed and the Lighthouse HTML report is uploaded as an artifact.

### Technical Notes

- Bounded Context: Infrastructure / CI
- Changes: `.lighthouserc.json` — update `settings.preset` to `"mobile"`, add Shopping List URL
  to the `url` array, raise Performance threshold.
- The mobile preset requires authentication (same session cookie approach as S10-E).
- No SCS code changes.
- BDD scenario: none (CI infrastructure story).

---

## Cross-SCS Impact Summary

| Template | SCS | Story |
|----------|-----|-------|
| `layout/default.html` | IAM, Trips, Expense | S11-A |
| `style.css` | IAM, Trips, Expense | S11-A (defines), all others consume |
| `trip/list.html` | Trips | S11-B |
| `trip/detail.html` | Trips | S11-C, S11-H (participants) |
| `trip/invitations.html` | Trips | S11-H |
| `shoppinglist/overview.html` | Trips | S11-D |
| `expense/detail.html` | Expense | S11-F (dialogs), S11-E (scan dialog), S11-G (tables) |
| `expense/scan-result.html` | Expense | S11-E |
| `dashboard/members.html` | IAM | S11-H |
| `dashboard/companions.html` | IAM | S11-H |
| `accommodation/overview.html` | Trips | S11-F (room add/edit dialogs — CSS only) |
| `expense/receipts.html` | Expense | S11-F (reject dialog — CSS only) |

---

## Out-of-Scope for Iteration 11

| Item | Reason |
|------|--------|
| US-TRIPS-041: Recipe Import from URL | Low priority; deferred to Iteration 12+ |
| Expense receipts list mobile refactoring | Complex (HTMX, reject dialog, review queue); separate story |
| MealPlan grid mobile layout | Lower priority than shopping + settlement; deferred |
| Accommodation overview mobile layout | Deferred; fewer users interact with it in the field |
| Service Worker / Offline | Could/XL; separate epic |
| US-EXP-022: Custom Receipt Splitting | Deferred from Iter 10 |
| Multi-Organizer Role Management | Deferred to dedicated IAM iteration |

---

## No New Domain Model

This iteration introduces no new aggregates, entities, value objects, domain events, application
services, or Flyway migrations. The only new Java code is:

- `TripController.detail()`: one new model attribute `pendingInvitationCount` (Integer) for S11-C.
  This is a count derived from the already-loaded invitation list — no new query, no new service method.

All other changes are confined to Thymeleaf templates and CSS files.

---

## Ubiquitous Language Compliance

No new terms are introduced. All UI changes retain existing i18n message keys. CSS class names
are not user-visible and follow no ubiquitous language constraint.

| UI (DE) | UI (EN) | Code | Context |
|---------|---------|------|---------|
| (Alle existing terms apply — no new terms in this iteration) | | | |

Responsive layout patterns introduced:
- "Bottom Sheet" (Dialog) — no German UI term needed; it is a visual pattern only
- "Akkordeon" — used for `<details>` sections in S11-C; no UI label needed

---

## Flyway Migration Summary

None. This iteration contains no database schema changes.
