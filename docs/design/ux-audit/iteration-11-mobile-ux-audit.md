# Iteration 11 — Mobile UX Audit

**Date**: 2026-03-19
**Scope**: All Travelmate UI templates across IAM, Trips, and Expense SCS
**Trigger**: User report — entire UI unusable on mobile (dialogs too wide, tables require horizontal scrolling, popups not mobile-friendly)
**Primary mobile use cases**: Shopping at the store (ShoppingList), scanning receipts (Expense), accepting invitations (Trip list)

---

## 1. Page-by-Page Audit

### 1.1 Summary Table

| Page | Template | Critical Issues | Severity | Proposed Fix | Effort |
|------|----------|-----------------|----------|--------------|--------|
| Navigation (all) | `layout/default.html` | 5+ nav items wrap unpredictably; logout form takes full row | P1 | Hamburger menu or icon-only compact nav | M |
| IAM Dashboard | `dashboard/index.html` | `.grid` invite form splits to 2 cols on 375px — labels overlap; two large `<section>` blocks with no hierarchy on small screens | P1 | Stack grid to 1 col on mobile; collapse add-forms behind `<details>` | M |
| Members table | `dashboard/members.html` | 5-col table (firstName, lastName, email, DOB, delete) — email column alone needs ~200px | P0 | Card layout: name large, email+DOB as metadata rows, delete as trailing action | S |
| Companions table | `dashboard/companions.html` | 4-col table — same overflow issue, less severe | P1 | Card layout: name + DOB, delete trailing | S |
| Trip list | `trip/list.html` | 5-col table (name, from, to, status, action) — date columns cause overflow | P0 | Card layout: trip name as link, dates + status as meta, action button at bottom | S |
| Trip detail | `trip/detail.html` | Participants table has inline stay-period form inside `<td>` — two date inputs + button in a 375px cell: completely broken | P0 | Separate stay-period form into its own row/card below participant name; dialog on mobile | L |
| Invitations table (trip detail) | `trip/invitations.html` | 3-col table with accept/decline grid inside `<td>` — buttons overflow | P0 | Card per invitation: name+status on top, action buttons full-width below | S |
| Trip create form | `trip/form.html` | `.grid` date row works on 375px but cramped; overall form is fine | P2 | Stack date grid to 1 col below 480px | S |
| Meal plan | `mealplan/overview.html` | 4-col table (date + 3 meal types), each cell contains 2–3 form elements — completely unusable on mobile; total width ~700px+ minimum | P0 | Day-by-day accordion: `<details>` per date, one meal type visible at a time with full-width selects | L |
| Shopping list | `shoppinglist/overview.html` | 6-col table (name, qty, unit, assignee, status, actions); action buttons `0.15rem` padding = tiny tap targets; add-form inside `<tfoot>` spans 6 cols | P0 | Card per item; floating "Add item" button; action buttons full-width | L |
| Accommodation | `accommodation/overview.html` | Room table 5 cols with inline edit forms inside `<td>`; add-accommodation `<dialog>` has multi-field form inside dialog — both dialog and room form exceed viewport | P0 | Room cards instead of table; dialog → full-screen form on mobile | L |
| Recipe list | `recipe/list.html` | 4-col table; action buttons (edit + delete) in flex row in `<td>` — cramped | P1 | Card layout: recipe name + servings + ingredient count; action buttons stacked | S |
| Recipe form | `recipe/form.html` | 4-col `.grid` ingredient rows (name, qty, unit, remove) — 4 columns on 375px is broken | P0 | Stack ingredient fields vertically or 2+2 layout; remove button as icon | M |
| Expense detail | `expense/detail.html` | 7 `<article>` sections, each with its own table; category breakdown, daily costs, participant summary, party settlement, transfers — multiple wide tables; `add-receipt-dialog` and `scan-receipt-dialog` not full-screen | P0 | Collapse non-critical tables in `<details>`; dialog → full-screen | L |
| Receipts fragment | `expense/receipts.html` | 6–7 col table depending on review mode; inline `<dialog>` elements per row for reject/resubmit | P0 | Card per receipt; dialog positioning absolute to viewport | M |
| Weightings fragment | `expense/weightings.html` | 3-col table with inline input+button form in `<td>` | P1 | List layout: participant name + inline weight input stacked | S |

### 1.2 Navigation (all layouts)

**Template**: `travelmate-iam/layout/default.html`, `travelmate-trips/layout/default.html`

**Current state**: Two `<ul>` in `<nav class="container">`. Trips nav has 5 items in the right `<ul>`: Reisepartei, Reisen, Rezepte, DE/EN, logout button. The existing CSS at ≤576px stacks items vertically and wraps them — this is a significant improvement over no handling at all, but the result is a multi-row nav bar that consumes ~120px of vertical space on every page.

**Issues**:
- P1: Nav takes 120–150px on mobile — too much space eaten before page content
- P1: Language switcher (DE / EN) is always visible, clutters nav on small screens
- P2: Logout button inside a `<form>` renders as a full-width button in the wrapped layout

**Proposed fix**: Hamburger pattern using pure CSS `<details>`/`<summary>` (no JS required), or a two-tier approach: brand + hamburger on line 1, nav items revealed on tap.

---

## 2. Severity Classification Detail

### P0 — Feature Unusable on Mobile

These pages are the reason the user filed the report. They cannot be used at all on a 375px phone:

1. **Members table** — email column alone forces horizontal scroll; delete button unreachable
2. **Trip list table** — 5 cols with two date columns; on 375px the name column gets ~120px
3. **Trip detail — participants stay-period form** — inline `<td>` form with two date inputs + button; browser renders this ~400px wide inside a table cell
4. **Trip detail — invitations table** — accept/decline grid inside `<td>` forces horizontal scroll
5. **Meal plan table** — 4 columns each containing multiple form elements; minimum rendered width ~700px
6. **Shopping list table** — 6 columns; action buttons have `padding:0.15rem` = ~14px tap target (WCAG minimum 44px)
7. **Accommodation — room table and dialog** — 5-col room table with inline edit; `<dialog>` without max-width constraint renders at its natural content width, which exceeds 375px
8. **Recipe form — ingredient grid** — 4-col `.grid` is 4 equal columns on any viewport
9. **Expense detail** — add-receipt dialog, scan-receipt dialog, and every table section
10. **Receipts fragment** — inline `<dialog>` for reject/resubmit spawns in the DOM but PicoCSS `<dialog>` has no mobile-safe width constraint

### P1 — Major UX Degradation

- Navigation bar vertical collapse works but consumes excessive space
- IAM Dashboard invite forms: `.grid` splits to 2 cols at all viewports (no breakpoint)
- Recipe list: action buttons in `<td>` are reachable but cramped
- Weightings: inline form in `<td>` is narrow but functional

### P2 — Cosmetic / Convenience

- Trip create form: date grid slightly cramped but usable
- Language switcher always visible in nav
- Danger zone section heading colour (red) bleeds on all `section:last-child` — affects mobile visual hierarchy

---

## 3. Mobile-First Redesign Proposals

### 3.1 Table → Card Layout Pattern

**Rule**: Any table with more than 3 columns, or any table containing interactive form elements in cells, must render as cards on mobile (≤ 640px).

**PicoCSS approach**: Use `<article>` as the card container. PicoCSS auto-styles `<article>` with padding, border, and shadow. No custom card class needed.

**CSS pattern** (add to `style.css` in both SCS):

```css
/* Responsive table → card layout */
@media (max-width: 640px) {
    .table-cards table,
    .table-cards thead,
    .table-cards tbody,
    .table-cards th,
    .table-cards td,
    .table-cards tr {
        display: block;
    }

    .table-cards thead {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip: rect(0,0,0,0);
    }

    .table-cards tbody tr {
        margin-bottom: 1rem;
        border: 1px solid var(--pico-muted-border-color);
        border-radius: var(--pico-border-radius);
        padding: 0.75rem;
    }

    .table-cards td {
        padding: 0.25rem 0;
        border: none;
        display: flex;
        align-items: baseline;
        gap: 0.5rem;
    }

    .table-cards td::before {
        content: attr(data-label);
        font-weight: bold;
        min-width: 8rem;
        flex-shrink: 0;
        font-size: 0.85rem;
        color: var(--pico-muted-color);
    }
}
```

Each `<td>` needs a `data-label` attribute (populated server-side via Thymeleaf) for the card approach. Alternatively, use a fully separate mobile fragment — but the CSS approach is simpler and avoids template duplication.

### 3.2 Dialog → Full-Screen on Mobile

**PicoCSS `<dialog>` behaviour**: PicoCSS styles dialog with `max-width: 600px`, centered. On a 375px viewport, PicoCSS will constrain the dialog to viewport width — but this only works if the content inside doesn't force the dialog wider (which happens with multi-column `.grid` layouts and wide form inputs).

**Fix**: Add to `style.css`:

```css
@media (max-width: 640px) {
    dialog {
        max-width: 100vw;
        max-height: 100dvh;
        width: 100%;
        height: 100%;
        margin: 0;
        border-radius: 0;
        overflow-y: auto;
    }

    dialog article {
        min-height: 100%;
        border-radius: 0;
    }

    /* Prevent grid from forcing wide layout inside dialogs */
    dialog .grid {
        grid-template-columns: 1fr;
    }
}
```

This one CSS block fixes all dialogs across all SCS without touching any template.

### 3.3 Navigation Collapse

**Current**: Wrapping nav with `flex-wrap`. Works but eats vertical space.

**Proposed**: Pure-HTML hamburger using `<details>`/`<summary>` — no JavaScript needed.

```html
<!-- layout/default.html (both SCS) -->
<nav class="container">
    <ul>
        <li><a href="/iam/dashboard"><strong th:text="#{nav.brand}">Travelmate</strong></a></li>
    </ul>
    <!-- Desktop: visible nav list | Mobile: hidden behind details -->
    <details class="nav-mobile-toggle" role="list">
        <summary aria-haspopup="listbox" th:text="#{nav.menu}">Menu</summary>
        <ul role="listbox">
            <li><a href="/iam/dashboard" th:text="#{nav.travelParty}">Reisepartei</a></li>
            <li><a href="/trips/" th:text="#{nav.trips}">Reisen</a></li>
            <li><a href="/trips/recipes" th:text="#{nav.recipes}">Rezepte</a></li>
            <li><a th:href="@{''(lang='de')}">DE</a> / <a th:href="@{''(lang='en')}">EN</a></li>
            <li>
                <form method="post" action="/logout" style="margin:0;">
                    <button type="submit" class="outline secondary" th:text="#{nav.logout}">Abmelden</button>
                </form>
            </li>
        </ul>
    </details>
    <!-- Desktop: regular horizontal list (hide on mobile) -->
    <ul class="nav-desktop">
        ...same items...
    </ul>
</nav>
```

CSS:
```css
.nav-desktop { display: flex; }
.nav-mobile-toggle { display: none; }

@media (max-width: 640px) {
    .nav-desktop { display: none; }
    .nav-mobile-toggle { display: block; }
}
```

**Note**: This is a significant template change across both SCS. Consider a shared fragment approach if possible, though SCS isolation makes this harder without a dedicated gateway-level nav.

### 3.4 Touch Optimization

**Shopping list action buttons**: Current `padding:0.15rem 0.5rem` gives ~14px tap targets. WCAG 2.5.8 (AA) requires 24px minimum; Apple HIG recommends 44px.

Fix: Remove inline style overrides from action buttons. Let PicoCSS default button sizing apply (it gives ~40px tap targets by default). The small size was added to avoid table row height expansion — which is no longer a concern in the card layout.

**Form inputs**: PicoCSS default inputs are ~44px tall (comfortable). No change needed.

**Weighting input** (`width:6rem`): Acceptable on mobile.

### 3.5 Progressive Disclosure

Pages where content can be hidden by default on mobile:

| Page | What to collapse | Trigger |
|------|-----------------|---------|
| IAM Dashboard | "Mitglied einladen" form | `<details>` — open by default on desktop, closed on mobile |
| IAM Dashboard | "Mitreisende(n) hinzufuegen" form | Same |
| Accommodation | "Zimmer zuweisen" assignment section | Already `<details>` — good |
| Accommodation | "Zimmer hinzufuegen" form | Already `<details>` — good |
| Expense detail | Daily costs table | `<details>` on mobile — rarely needed during shopping |
| Expense detail | Category breakdown table | `<details>` on mobile |
| Expense detail | Participant summary | `<details>` on mobile |
| Meal plan | Each day's slots | `<details>` per date row — primary mobile pattern |

---

## 4. ASCII Wireframes — 3 Most Critical Pages

### 4.1 Shopping List (Mobile, 375px)

**Current (broken)**:
```
┌─────────────────────────────────────────────────────────────┐
│ Name │ Menge │ Einheit │ Assignee │ Status │ Aktionen       │ ← horizontal scroll required
│ Milk │ 2     │ l       │ —        │ OPEN   │ [Übernehmen]   │
└─────────────────────────────────────────────────────────────┘
```

**Proposed (mobile card)**:
```
┌─────────────────────────┐
│  Aus dem Essensplan     │           [Aktualisieren]
├─────────────────────────┤
│  Milk                   │
│  2 l                    │
│  Status: [Ausstehend]   │
│                         │
│  [  Ich übernehme  ]    │  ← full-width button, 44px+ touch target
├─────────────────────────┤
│  Butter            ✓    │
│  250 g                  │
│  Übernommen von: Du     │
│                         │
│  [Erledigt]  [Abgeben]  │  ← two equal buttons, 44px height
├─────────────────────────┤
│  Manuelle Einträge      │
├─────────────────────────┤
│  Wein                   │
│  1 Flasche              │
│  Status: [Ausstehend]   │
│                         │
│  [  Ich übernehme  ]    │
│  [     Löschen     ]    │  ← secondary action below primary
└─────────────────────────┘

┌─────────────────────────┐
│  + Eintrag hinzufügen   │  ← floating action or bottom-of-page form
│  Name:    [__________]  │
│  Menge:   [____]        │
│  Einheit: [____]        │
│  [Eintrag hinzufügen]   │
└─────────────────────────┘
```

### 4.2 Trip Detail — Participants (Mobile, 375px)

**Current (broken)**:
```
┌──────────┬──────────┬──────────┬─────────────────────────────────┐
│ Mitglied │ Anreise  │ Abreise  │ Aktionen                        │  ← scroll required
│ Max M.   │ 15.03.26 │ 22.03.26 │ [date][date][Speichern]         │  ← 400px+ cell
└──────────┴──────────┴──────────┴─────────────────────────────────┘
```

**Proposed (mobile card)**:
```
┌─────────────────────────┐
│  Teilnehmer             │
├─────────────────────────┤
│  Max Mustermann         │
│  Anreise:  15.03.2026   │
│  Abreise:  22.03.2026   │
│  [Zeitraum bearbeiten]  │  ← opens full-screen form or inline expand
├─────────────────────────┤
│  Anna Schmidt           │
│  Anreise:  —            │
│  Abreise:  —            │
│  [Zeitraum bearbeiten]  │
└─────────────────────────┘

── When "Zeitraum bearbeiten" is tapped ──
┌─────────────────────────┐
│  Max Mustermann         │
│  Anreise:               │
│  [2026-03-15        ]   │  ← full-width date input
│  Abreise:               │
│  [2026-03-22        ]   │  ← full-width date input
│  [    Speichern    ]    │
│  [    Abbrechen    ]    │
└─────────────────────────┘
```

**Implementation**: The stay-period form moves out of the `<td>` and into a toggled section below each participant card. On desktop, the inline `<td>` form is retained.

### 4.3 Expense Detail — Receipt List (Mobile, 375px)

**Current (broken)**:
```
┌───────────┬────────┬──────────┬────────┬──────────┬──────────┐
│ Beschr.   │ Betrag │ Bezahlt  │ Datum  │ Kategorie│ Aktionen │ ← 6 col scroll
└───────────┴────────┴──────────┴────────┴──────────┴──────────┘
```

**Proposed (mobile card)**:
```
┌─────────────────────────┐
│  Supermarkt Rewe        │  ← description (prominent)
│  47,30 EUR              │  ← amount (prominent)
│  Lebensmittel           │  ← category as badge
│  Bezahlt von: Max M.    │
│  12.03.2026             │
│  Status: [Genehmigt]    │
│                         │
│  [Entfernen]            │  ← action only shown if actionable
├─────────────────────────┤
│  Tankstelle Shell       │
│  85,00 EUR              │
│  Treibstoff             │
│  Bezahlt von: Anna S.   │
│  13.03.2026             │
│  Status: [Eingereicht]  │
│                         │
│  [Genehmigen] [Ablehnen]│
└─────────────────────────┘

┌─────────────────────────┐
│  [+ Beleg hinzufügen]   │  ← primary CTA, always visible
│  [📷 Scannen]           │  ← secondary CTA
└─────────────────────────┘
```

---

## 5. Before / After: Table → Card Comparison

### Members Table (IAM Dashboard)

**Before — desktop-only table**:
```html
<figure>
    <table>
        <thead>
            <tr>
                <th>Vorname</th><th>Nachname</th><th>E-Mail</th>
                <th>Geburtsdatum</th><th></th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>Max</td><td>Mustermann</td>
                <td>max@example.com</td><td>1985-04-12</td>
                <td><button>Löschen</button></td>
            </tr>
        </tbody>
    </table>
</figure>
```

**After — responsive with `data-label`**:
```html
<figure class="table-cards">
    <table>
        <thead>
            <tr>
                <th th:text="#{member.firstName}">Vorname</th>
                <th th:text="#{member.lastName}">Nachname</th>
                <th th:text="#{member.email}">E-Mail</th>
                <th th:text="#{common.dateOfBirth}">Geburtsdatum</th>
                <th></th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="m : ${members}">
                <td th:data-label="#{member.firstName}"
                    th:text="${m.firstName()}"></td>
                <td th:data-label="#{member.lastName}"
                    th:text="${m.lastName()}"></td>
                <td th:data-label="#{member.email}"
                    th:text="${m.email()}"></td>
                <td th:data-label="#{common.dateOfBirth}"
                    th:text="${m.dateOfBirth()}"></td>
                <td>
                    <button ...>Löschen</button>
                </td>
            </tr>
        </tbody>
    </table>
</figure>
```

Mobile render (via CSS `display:block` + `::before` pseudo-element):
```
┌─────────────────────────┐
│  Vorname:    Max        │
│  Nachname:   Mustermann │
│  E-Mail:     max@ex...  │
│  Geb.:       1985-04-12 │
│  [Löschen]              │
└─────────────────────────┘
```

This approach requires no template restructuring beyond adding `th:data-label` and the `.table-cards` class wrapper — both are minimal changes.

---

## 6. Responsive Breakpoint Strategy

Travelmate uses PicoCSS 2 which has a single container breakpoint system. PicoCSS uses `min-width` breakpoints:

| PicoCSS breakpoint | Value | Our usage |
|-------------------|-------|-----------|
| Small (default) | any | Mobile-first base styles |
| Medium | ≥576px | Nav stacking (existing CSS) |
| Large | ≥768px | (not currently used) |

**Proposed Travelmate breakpoints** (add to both `style.css` files):

```css
/* Mobile-first breakpoints */
--tm-mobile:   640px;   /* ≤640px: card layouts, full-screen dialogs, single-col grids */
--tm-tablet:   768px;   /* ≤768px: two-col grids, nav collapse threshold */
--tm-desktop: 1024px;   /* >1024px: full table layouts, multi-col grids */
```

**Breakpoint rules**:
- `≤ 640px`: All tables → card layout; all dialogs → full-screen; all `.grid` → 1 column; nav → hamburger
- `641px–768px`: Some tables can stay tabular if ≤ 3 columns with no interactive cells; dialogs → centered with `max-width: 90vw`
- `≥ 769px`: Current desktop layout (tables, dialogs, grids all as-is)

**PicoCSS `.grid` on mobile**: PicoCSS `.grid` creates equal-column CSS Grid with no responsive breakpoint — it stays multi-column regardless of viewport width. Every `.grid` in a form context needs a mobile override:

```css
@media (max-width: 640px) {
    .grid {
        grid-template-columns: 1fr;
    }
}
```

This is the highest-leverage single CSS change — it fixes the invite forms, the date inputs, the ingredient rows, and the room rows all at once.

---

## 7. PicoCSS 2 Responsive Capabilities Evaluation

### What PicoCSS provides out of the box
- `<article>`: auto-styled card (padding, border, shadow) — perfect mobile card base
- `<details>`/`<summary>`: accordion pattern — no custom CSS needed
- `<dialog>`: native modal with backdrop — mobile constraints need custom CSS override
- `<nav>` with `<ul>`: flexbox nav — no hamburger built in
- `<progress>`: indeterminate/determinate bar — mobile-ready
- `aria-busy="true"`: spinner on any element — touch-safe

### What PicoCSS does NOT provide
- Responsive grid (`.grid` does not break to 1-col on mobile)
- Hamburger navigation
- Bottom sheet / drawer pattern
- Sticky action bars
- Swipe gestures (out of scope for server-rendered app)

### What requires custom CSS (additions to `style.css`)
1. `@media (max-width: 640px) { .grid { grid-template-columns: 1fr; } }` — highest priority
2. Dialog full-screen override — one block fixes all dialogs
3. `.table-cards` responsive table-to-card system — reusable across all SCS
4. Hamburger nav toggle — either JS-free (`<details>`) or minimal JS

---

## 8. Mobile User Journeys

### 8.1 Organizer on Phone: Creating a Trip and Adding Accommodation

**Persona**: Organizer, planning from their phone at home
**Device**: iPhone SE (375 × 667px)
**Goal**: Create a new trip, add accommodation via URL import, invite members

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points (Current) | Proposed Fix |
|-------|------------|-----------------|------------|---------|----------------------|--------------|
| 1. Navigate | Taps "Neue Reise planen" | Trip create form | `/trips/new` | Ready | Button is reachable after nav scrolled past | Nav hamburger reduces nav height |
| 2. Fill form | Types trip name, taps date inputs | Native date picker opens | `trip/form.html` | Fine | Date grid side-by-side is cramped but workable | Stack to 1-col |
| 3. Submit | Taps "Reise anlegen" | Redirects to trip detail | Server POST | Satisfied | OK | — |
| 4. Add accommodation | Taps "Unterkunft hinzufügen" | Accommodation page | `/accommodation` | Ready | Fine — link is a button | — |
| 5. Import URL | Taps "URL importieren" in `<details>` | Import form | Accommodation page | Fine | `<details>` already collapses — good UX | — |
| 6. Fill URL | Types or pastes URL | — | Import form | Fine | Input is full-width — OK | — |
| 7. Review import | Server returns pre-filled form | Pre-filled accommodation form | Accommodation page | Satisfied | Room grid (3-col) inside form is broken on mobile | Stack grid to 1-col |
| 8. Save | Taps "Speichern" | Accommodation saved | Server POST | Satisfied | OK | — |
| 9. Invite member | Goes back to trip detail, taps "Mitglied einladen" | Dialog opens | `trip/detail.html` | Frustrated | Dialog wider than viewport — select + buttons overflow | Dialog full-screen override |
| 10. Select member | Taps `<select>` | Native picker | Dialog | Fine | OK if dialog is full-screen | — |
| 11. Send invite | Taps "Einladung senden" | Dialog closes, list updates | HTMX swap | Satisfied | OK | — |

**Current total friction**: Steps 7 and 9 are blockers. Step 7 (room grid) is broken; step 9 (dialog) is broken.
**After fix**: All steps flow without horizontal scroll.

---

### 8.2 Participant at the Store: Shopping List

**Persona**: Participant, shopping at a supermarket
**Device**: Android phone, 390px viewport
**Goal**: Claim items, mark them as purchased, scan a receipt

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points (Current) | Proposed Fix |
|-------|------------|-----------------|------------|---------|----------------------|--------------|
| 1. Open app | Navigates to app, opens trip | Trip detail | `/trips/{id}` | Ready | Trip detail scrolls fine — articles stack | — |
| 2. Open list | Taps "Einkaufsliste anzeigen" | Shopping list page | `/shoppinglist` | Ready | OK | — |
| 3. View items | Sees shopping list | Table renders | Shopping list | Frustrated | 6-col table requires horizontal scroll; items hidden off-screen | Card layout — items immediately visible |
| 4. Claim item | Tries to tap "Ich übernehme" | — | Item row | Very frustrated | Button has 14px tap target — misses on first attempt | Full-width button, 44px height |
| 5. Claim success | Button tap registers | Row updates via HTMX | HTMX swap | Relieved | HTMX swap works — but row update replaces a table row; if table overflows, user loses context | Card layout preserves context |
| 6. Purchase item | Taps "Erledigt" | Row gets strikethrough | HTMX swap | Satisfied | Same tap target issue | Full-width buttons |
| 7. Scan receipt | Navigates to Expense, taps "Kassenzettel scannen" | Scan dialog opens | `expense/detail.html` | Frustrated | Dialog not full-screen; camera input works but form context is wrong size | Dialog full-screen; scan button prominent |
| 8. Take photo | Taps file input (camera) | Native camera app | Browser | Fine | Camera opens OK | — |
| 9. Analyse | Taps "Analysieren" | OCR result returned | Scan dialog | Waiting | Spinner (`htmx-indicator`) is a hidden `<span>` — no visible progress | Replace with `<progress>` bar |
| 10. Review result | Sees pre-filled form | Pre-filled receipt form | Scan dialog | Satisfied | OK if dialog is full-screen | — |
| 11. Save | Taps "Beleg hinzufügen" | Dialog closes, receipt added | HTMX swap | Satisfied | OK | — |

**Critical path**: Steps 3–6 (shopping list table + button tap targets) and step 7 (scan dialog) are P0 blockers.

---

### 8.3 Participant After Trip: Viewing Settlement and Downloading PDF

**Persona**: Participant, checking how much they owe after the trip
**Device**: iPhone 14 (390px)
**Goal**: Understand the settlement, download PDF for records

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points (Current) | Proposed Fix |
|-------|------------|-----------------|------------|---------|----------------------|--------------|
| 1. Open expense | Navigates to expense from trip list | Expense detail page | `/expense/{id}` | Curious | 7 `<article>` blocks — page is long but sections stack well | — |
| 2. Scan totals | Scrolls to "Gesamtbetrag" | Simple `<p>` render | Expense detail | Fine | OK — single paragraph, no table | — |
| 3. Check category | Scrolls to category breakdown | 4-col table | Expense detail | Frustrated | 4 cols (category, amount, progress bar, receipt count) forces scroll; progress bar is rendered but tiny on mobile | Collapse to `<details>` on mobile |
| 4. View transfers | Scrolls to "Ausgleichszahlungen" | 4-col table | Expense detail | Frustrated | 4 cols with party names — may overflow | Stack to card: "A zahlt B: X EUR" as single line |
| 5. Check own balance | Looks for own row in participant summary | 4-col table | Expense detail | Frustrated | 4 cols (name, paid, share, balance) forces horizontal scroll | Card layout — own row first |
| 6. Download PDF | Taps "Abrechnung als PDF exportieren" | Browser downloads file | Browser native | Fine | Button is `role="button"` link — OK | Ensure it renders as full-width button on mobile |

**Key insight**: The expense detail page has many sections that are rarely needed during mobile use. On mobile, the most important information is: total amount + what I personally owe or am owed. Everything else (daily costs, category breakdown, party settlement) should be collapsed into `<details>` on mobile.

---

## 9. Implementation Strategy

### Phase 1 — Critical Mobile Fixes (highest-value / lowest-effort CSS changes)

**Goal**: Make the most-used mobile flows unblocked without template changes.

1. **Global `.grid` → 1-col on mobile** (1 CSS rule, both SCS `style.css`)
   - Fixes: invite forms, date grids, ingredient rows, room rows, all `.grid` inside dialogs
   - Effort: S (1 line per SCS)

2. **Dialog full-screen on mobile** (1 CSS block, both SCS `style.css`)
   - Fixes: add-receipt dialog, scan-receipt dialog, invite-member dialog, invite-external dialog, add-accommodation dialog, edit-accommodation dialog, reject dialog, resubmit dialog
   - Effort: S (8 lines per SCS)

3. **Shopping list action button tap targets** (remove inline style overrides from `shoppinglist/overview.html`)
   - Fixes: "Ich übernehme", "Erledigt", "Abgeben", "Rückgängig" buttons
   - Requires a CSS replacement for the visual compactness those styles provided (a `.btn-compact` class)
   - Effort: S (template touch + 1 CSS class)

4. **Stay-period form out of `<td>`** (`trip/detail.html` template change)
   - This is a structural template change but impacts the most important organizer flow
   - Move stay-period form to a `<details>` below each participant row
   - Effort: M

**Phase 1 estimated delivery**: 3 stories, 1 sprint half

---

### Phase 2 — Table → Card Responsive Layouts

**Goal**: All tables work on mobile without horizontal scroll.

Apply the `.table-cards` CSS pattern + `th:data-label` additions to:

1. **Members table** (`dashboard/members.html`) — Effort: S
2. **Companions table** (`dashboard/companions.html`) — Effort: S
3. **Trip list table** (`trip/list.html`) — Effort: S
4. **Invitations table** (`trip/invitations.html`) — Effort: S
5. **Recipe list table** (`recipe/list.html`) — Effort: S
6. **Weightings table** (`expense/weightings.html`) — Effort: S
7. **Receipts table** (`expense/receipts.html`) — Effort: M (complex action column)
8. **Advance payments table** (`expense/detail.html`) — Effort: S
9. **Settlement / transfers tables** (`expense/detail.html`) — Effort: S each

**Phase 2 estimated delivery**: 1 sprint

---

### Phase 3 — Dialog → Full-Screen Sheets + Form Optimization

**Goal**: Complex forms and workflows fully usable on mobile.

1. **Meal plan — day accordion** (`mealplan/overview.html`)
   - Completely replace table with `<details>` per date row
   - Each day: summary shows date, content shows 3 meal slots as stacked cards
   - Effort: L (template rewrite)

2. **Accommodation — room table → cards** (`accommodation/overview.html`)
   - Room table is complex (inline edit, assignment, capacity indicator)
   - Separate view (read-only cards) from edit (expandable form per room)
   - Effort: L

3. **Recipe form — ingredient layout** (`recipe/form.html`)
   - 4-col `.grid` per ingredient → 2+2 stacked layout or vertical stack
   - Effort: M

4. **Shopping list add-item form** (`shoppinglist/overview.html`)
   - Move out of `<tfoot>` (which breaks on mobile) into a proper form below the list
   - Effort: M

5. **Navigation hamburger** (both `layout/default.html`)
   - Effort: M

**Phase 3 estimated delivery**: 1 sprint

---

### Phase 4 — Polish + Lighthouse Score

**Goal**: Lighthouse mobile score ≥ 90.

1. **Scan receipt UX**: Replace hidden `<span>` spinner with `<progress>` indeterminate bar
2. **Balance colour**: Replace `style="color:green"` / `style="color:red"` with CSS classes (also an accessibility fix — colour alone conveys meaning)
3. **Stay-period labels**: Add visible `<label>` text (currently `.sr-only`) on mobile
4. **Toast position**: On mobile, position at bottom (`bottom: 1rem`) rather than top (top is occluded by nav)
5. **Lighthouse audit**: Run against shopping list, expense detail, and trip detail; address remaining score items (image alt text, meta description, contrast ratios)

**Phase 4 estimated delivery**: 0.5 sprint

---

## 10. Iteration 11 Recommended Scope

Given the scale of work (Phase 1–4), a single iteration should focus on Phase 1 + Phase 2, which delivers:
- All tables readable on mobile (no horizontal scroll required anywhere)
- All dialogs usable on mobile (full-screen)
- Shopping list and receipt scan unblocked (the primary mobile use cases)
- Stay-period form usable on mobile

Phase 3 (meal plan accordion, accommodation room cards) is a larger template rewrite and is best deferred to Iteration 12 unless the meal plan mobile experience is explicitly reported as blocking.

**Recommended Iteration 11 Stories**:

| Story ID | Description | Phase | Effort | Priority |
|----------|-------------|-------|--------|----------|
| S11-A | Global CSS fixes: `.grid` → 1-col + dialog full-screen (both SCS) | 1 | S | P0 |
| S11-B | Shopping list: card layout + touch-friendly action buttons | 1+2 | M | P0 |
| S11-C | Trip detail: stay-period form out of `<td>` | 1 | M | P0 |
| S11-D | Trip detail: invitations table → card layout | 2 | S | P0 |
| S11-E | Receipts fragment: card layout | 2 | M | P0 |
| S11-F | Members + companions tables → card layout | 2 | S | P1 |
| S11-G | Trip list table → card layout | 2 | S | P1 |
| S11-H | Navigation hamburger (both layouts) | 3 | M | P1 |
| S11-I | Expense detail: collapse non-critical sections in `<details>` on mobile | 3 | S | P1 |
| S11-J | Balance colour: replace inline style with CSS classes | 4 | S | P2 |

**Out of scope for Iteration 11 (defer to 12)**:
- Meal plan complete redesign (accordion pattern)
- Accommodation room table → cards (complex inline edit)
- Recipe form ingredient layout
- Lighthouse audit (run after Phase 1+2 ships)

---

## 11. Open Questions for Architect

1. **Shared CSS file**: Both SCS have identical `style.css` files. Should the mobile-critical CSS rules (`.grid` override, dialog override, `.table-cards`) live in a shared asset served by the Gateway, or remain duplicated in each SCS? The SCS isolation principle argues for duplication; the DRY principle argues for a shared asset. This affects how Phase 1 is implemented.

2. **HTMX row-swap + card layout**: The shopping list uses `hx-swap="outerHTML"` on `<tr>` elements to update individual rows. If items become `<article>` cards, the swap target changes from `tr` to `div[id="item-row-X"]`. This is a template + server change, not just a CSS change. The architect should confirm whether the shopping list polling (every 5s) should swap the entire `#shopping-list-content` container or individual cards.

3. **Meal plan template scope**: The meal plan table is a complete mobile rewrite — `<details>` per date row, with three meal slots inside. This changes the model passed to the template (`slotsByDate` map is fine) but the Thymeleaf fragment structure changes significantly. Should this be a new Thymeleaf fragment alongside the existing table fragment (with server-side UA detection to choose) or a single template with CSS-only switching?
