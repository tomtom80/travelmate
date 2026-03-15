# UX Design: Expense SCS Integration

**Version**: v0.5.0 scope
**Status**: Design recommendation — no code written

---

## Problem Statement

The Expense SCS (`/expense/{tripId}`) is fully functional but entirely unreachable from the
application's navigation. The user has no way to discover it without already knowing the URL.

---

## Scope of Changes

Three templates in two SCS require modification. No new pages are needed.

| Template | SCS | Change type |
|----------|-----|-------------|
| `trip/list.html` | Trips | Add expense column for COMPLETED rows |
| `trip/detail.html` | Trips | Add expense card section (COMPLETED only) |
| `layout/default.html` (all three SCS) | IAM / Trips / Expense | Add nav link to expense home |

The IAM dashboard (`dashboard/index.html`) should NOT be changed for expense integration.
The dashboard is travel-party management, not trip management. Adding expense links there
would create an incoherent information architecture.

---

## 1. Trip List Page Changes

### Current state

The trip table has four columns: Name, Von, Bis, Status.
Status is shown as a `<mark>` badge. There is no link to expenses anywhere.

### Recommended change: Add an Abrechnung column

**Desktop layout (5 columns):**

```
+------------------+------------+------------+----------------+-------------+
| Name             | Von        | Bis        | Status         | Abrechnung  |
+------------------+------------+------------+----------------+-------------+
| Skiurlaub 2026   | 15.03.2026 | 22.03.2026 | In Planung     |             |
| Sommerurlaub 25  | 01.07.2025 | 14.07.2025 | Abgeschlossen  | Abrechnung →|
+------------------+------------+------------+----------------+-------------+
```

**Rules:**
- The "Abrechnung" cell is empty for all non-COMPLETED trips.
- For COMPLETED trips: render a plain `<a>` link pointing to `/expense/{tripId}`.
- Do NOT use a button — this is a navigation action, not a form submission.
- The link text is the i18n key `nav.expense` (already defined: "Abrechnung" / "Expenses").

**Mobile behavior:**
On narrow screens the table already collapses. The Abrechnung column should be the last and
lowest-priority column — acceptable to hide on very small screens via responsive utility class
if table overflow becomes a problem. The trip name link remains the primary tap target.

### Template diff (description only — no code)

In `trip/list.html`:

1. Add a `<th>` header cell with `th:text="#{nav.expense}"` as the 5th column.
2. In each `<tr th:each="trip : ${trips}">`, add a `<td>` at the end:
   - Condition: `th:if="${trip.status() == 'COMPLETED'}"` — render link to `/expense/{trip.tripId()}`
   - Condition: `th:if="${trip.status() != 'COMPLETED'}"` — render empty `<td>`

**No new i18n keys needed.** `nav.expense` already exists in all three SCS message files.

---

## 2. Trip Detail Page Changes

### Current state

The trip detail page ends with: Info card → Participants card → Invitations card → Back link.
For COMPLETED trips, the lifecycle action buttons are gone but nothing replaces them.

### Recommended change: Expense summary card (COMPLETED trips only)

Add a new `<article>` section after the participants card, visible only when
`trip.status() == 'COMPLETED'`.

**Wireframe:**

```
+-------------------------------------------------------+
|  Abrechnung                                           |
|                                                       |
|  Die Reise ist abgeschlossen. Die Abrechnung kann     |
|  jetzt bearbeitet und abgeschlossen werden.           |
|                                                       |
|  [ Abrechnung anzeigen → ]                            |
+-------------------------------------------------------+
```

**Component specification:**

```
Component: Expense Summary Card on Trip Detail
PicoCSS Base: <article>
Condition: th:if="${trip.status() == 'COMPLETED'}"

Content:
- <h2> with key: trip.expense.title
- <p> with key: trip.expense.hint (contextual description)
- <a role="button"> linking to /expense/{tripId} with key: trip.expense.action
  - Standard primary button style (no class needed — PicoCSS default)
  - Target URL: absolute path /expense/{tripId}

No HTMX. This is a full navigation link.
```

**Placement:** Between the participants card and the invitations card, or after the
invitations card — the invitations section is only relevant for non-completed trips, so
placing the expense card after invitations is acceptable. However, placing it above
invitations makes it more prominent for the primary use case of the organizer wrapping up
the trip. Recommended: place it after participants, before invitations (invitations become
read-only for COMPLETED trips anyway).

**i18n keys needed (new):**

| Key (DE) | Value (DE) | Value (EN) |
|----------|------------|------------|
| `trip.expense.title` | Abrechnung | Expenses |
| `trip.expense.hint` | Die Reise ist abgeschlossen. Jetzt können Belege erfasst und die Abrechnung durchgeführt werden. | The trip is complete. You can now add receipts and settle the expenses. |
| `trip.expense.action` | Abrechnung anzeigen | View Expenses |

These keys belong in `travelmate-trips/src/main/resources/messages_de.properties` and
`messages_en.properties`.

---

## 3. Navigation Bar Changes

### Current state

All three layouts (`iam/layout/default.html`, `trips/layout/default.html`,
`expense/layout/default.html`) define `nav.expense` in their message files but the `<nav>`
element in every layout only shows "Reisepartei" and "Reisen".

### Recommended change: Add expense nav item

Add a third `<li>` entry in the nav `<ul>` (right side):

```
<li><a href="/expense/" th:text="#{nav.expense}">Abrechnung</a></li>
```

**Placement:** After "Reisen", before the language switcher.

**Consideration:** The expense home page (`GET /expense/`) currently renders only a
placeholder message (`expense.home.info`). Once the nav link exists, users will land there
and see an uninformative page. Two options:

- **Option A (recommended for v0.5.0):** Keep the nav link but add a trip list on the
  expense home page showing all the user's expenses (requires the expense controller to
  query by tenant). This gives the nav link a useful destination.

- **Option B (minimal):** Do not add the nav link now. Rely solely on contextual links
  from the trip detail and trip list. Add the nav link only when the expense home page
  has useful content.

Option B is lower risk and keeps the scope focused. The contextual links (changes 1 and 2
above) are sufficient for discoverability without needing a global nav entry.

**Recommendation: Implement Option B for v0.5.0.** Defer the nav link to the same iteration
that improves the expense home page.

---

## 4. Expense Detail Page Changes

### Minor issues to address in the same pass

These are small quality improvements, not new features:

#### 4a. Balance colours: replace inline styles with CSS classes

Current code uses `style="color:green"` and `style="color:red"` for positive/negative
balances. These are not themeable and will break in dark mode.

Replace with:
- `.balance-positive` class → `color: var(--pico-color-green-500)` (or equivalent)
- `.balance-negative` class → `color: var(--pico-color-red-500)` (or equivalent)

Define these in `static/css/style.css` in the expense module.

#### 4b. Settle button: add confirmation

The "Abrechnung abschliessen" action is irreversible (status → SETTLED, no undo).
The form currently has no confirmation prompt.

Add `hx-confirm` or a native `onclick="return confirm(...)"` with the key
`expense.settle.confirm` (new i18n key, DE: "Möchten Sie die Abrechnung wirklich
abschließen? Dies kann nicht rückgängig gemacht werden.", EN: "Are you sure you want to
settle the expenses? This cannot be undone.").

However, the form uses `method="post"` without HTMX, so `hx-confirm` won't fire. Use the
standard HTML pattern already in use for HTMX destructive actions, or wrap the button in
a `<dialog>` confirmation. The simplest correct approach is `onclick="return confirm(...)"`
with a localized message passed via Thymeleaf.

New i18n keys for expense module:

| Key | DE | EN |
|-----|----|----|
| `expense.settle.confirm` | Möchten Sie die Abrechnung wirklich abschliessen? Dies kann nicht rückgängig gemacht werden. | Are you sure you want to settle the expenses? This cannot be undone. |

#### 4c. Back link: already points to `/trips/`

The hardcoded `<a href="/trips/" th:text="#{common.back}">` in expense detail is correct.
No change needed.

---

## Priority Ranking

| Priority | Change | Effort | Impact |
|----------|--------|--------|--------|
| P1 — Critical | Expense card on trip detail (COMPLETED) | Low — 1 article block + 3 i18n keys | Closes the primary navigation dead end |
| P1 — Critical | Expense link in trip list (COMPLETED rows) | Very low — 1 column + no new keys | Allows discovery from overview |
| P2 — High | Balance colour classes (inline style removal) | Very low — 2 CSS lines | Accessibility + dark mode correctness |
| P2 — High | Settle confirmation | Low — 1 i18n key + onclick | Prevents accidental irreversible action |
| P3 — Medium | Nav bar expense link | Very low — 1 li | Only useful after expense home page is improved |
| P3 — Medium | Expense home page: list user's expenses | Medium — requires new service query | Prerequisite for P3 nav link |
| P4 — Low | Weighting hint text | Very low | Reduces confusion about the weighting concept |

---

## What NOT to Change (out of scope)

- **IAM Dashboard**: expense links do not belong here; IAM context is travel party management
- **Expense home page content**: out of scope until a list-all-expenses query is implemented
- **Cross-SCS total amount preview on trip list**: requires event-driven denormalisation or
  client-side composition; out of scope for this iteration
- **Expense navigation deep-linking from IAM dashboard**: violates SCS information architecture
