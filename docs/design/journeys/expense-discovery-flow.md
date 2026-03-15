# User Journey: Expense Discovery and Settlement

**Persona**: Organizer (primary) / Participant (secondary)
**Goal**: Find and use the expense settlement for a completed trip
**Trigger**: A trip transitions to COMPLETED status, creating an Expense automatically

---

## Context: The Discovery Problem

The Expense SCS at `/expense/{tripId}` is fully functional but unreachable from the rest of the
application. After login, a user sees the IAM Dashboard and then the Trip List and Trip Detail.
None of these pages contain any reference to `/expense`. The user must already know the URL
structure to access expense data. This is a complete navigation dead end.

---

## Journey Phase Analysis

### Phase 1 — Trip Completion (Organizer)

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1a. Lifecycle | Clicks "Abschliessen" on trip detail | Trip status → COMPLETED; Expense auto-created by event consumer | Trip detail `/trips/{id}` | Satisfied | No indication an expense was created | After-completion redirect or inline hint |
| 1b. Redirect | Page reloads with COMPLETED status | Status badge updated, lifecycle buttons gone | Trip detail | Neutral | User sees a trip marked complete — no next step | Add "Abrechnung anzeigen" link/button |

**Critical gap**: The moment a trip completes is the highest-intent moment for expense access.
Currently nothing happens that points the user forward.

---

### Phase 2 — Return Visit (Organizer or Participant)

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 2a. Login | Authenticates | Redirected to IAM Dashboard | `/iam/dashboard` | Neutral | Dashboard shows travel party info only — no trip history | Not the right place for expense entry points |
| 2b. Navigate | Clicks "Reisen" in nav | Trip list loads | `/trips/` | Neutral | Trip list table has no expense column or link | Add total + link for COMPLETED trips |
| 2c. Select trip | Clicks trip name link | Trip detail loads | `/trips/{id}` | Neutral | Trip detail has no expense section | Add expense summary card at bottom |
| 2d. Dead end | User scans page | Nothing links to `/expense` | Trip detail | Frustrated | User gives up or guesses URL | Primary fix point |

---

### Phase 3 — Expense Usage (Organizer adds receipts)

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 3a. Arrive | Reaches `/expense/{tripId}` | Full expense detail loads | Expense detail | Relieved | No back-navigation context (hardcoded `/trips/`) | Keep hardcoded back link — it already works |
| 3b. Add receipts | Opens "Beleg hinzufügen" dialog | HTMX partial updates receipt list | Expense detail | Focused | Receipt dialog has no feedback on duplicate entries | Existing toast feedback is adequate |
| 3c. Adjust weights | Updates participant weightings | HTMX partial updates weighting list | Expense detail | Focused | No explanation of what "Gewichtung" means | Consider tooltip or hint text |
| 3d. Settle | Clicks "Abrechnung abschliessen" | Full page redirect to settled view | Expense detail | Satisfied | Settle button style (`.contrast`) is visually loud for an irreversible action | Use `.outline.secondary` or add confirmation |

---

### Phase 4 — Viewing Settled Expense (Participant)

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 4a. Navigate | Finds COMPLETED trip in list | Trip detail loads | `/trips/{id}` | Neutral | Same dead end as Phase 2d | Same fix — link to expense |
| 4b. View balances | Reaches expense page | Sees balance table | Expense detail | Interested | Positive balance = green inline style; negative = red inline style — hardcoded, not themeable | Replace with CSS classes |

---

## Key Metrics

| Metric | Target | Current State |
|--------|--------|---------------|
| Steps from login to expense | ≤ 3 | Impossible (no path) |
| Steps from trip detail to expense | ≤ 1 | Impossible (no link) |
| Steps from trip list to expense | ≤ 2 | Impossible |

---

## Root Cause Summary

There are exactly three places where a user has trip context and could be pointed to the
expense SCS:

1. **Trip list row** — a COMPLETED trip row has no expense column, no link
2. **Trip detail page** — a COMPLETED trip has no expense card, no link
3. **After the "Abschliessen" action** — the page simply reloads; no contextual prompt

The IAM Dashboard is NOT an appropriate expense entry point. The dashboard is travel-party
management (IAM context). Expenses are trip-scoped (Trips/Expense context). Placing expense
links there would violate the SCS boundary in the UI and confuse the information architecture.

---

## Improvement Opportunities (prioritised)

1. **[P1 — Critical] Trip detail: expense card for COMPLETED trips**
   Add a new `<article>` section at the bottom of trip detail when status is COMPLETED.
   Shows total amount and a prominent link to `/expense/{tripId}`.

2. **[P1 — Critical] Trip list: expense link column for COMPLETED trips**
   Add an "Abrechnung" link cell (or icon) in the trip table row when status is COMPLETED.

3. **[P2 — High] Trip list: total amount preview**
   The trip list table could show the total expense amount for COMPLETED trips.
   Requires a cross-SCS data call — this has architectural implications (see notes below).

4. **[P2 — High] Nav bar: add "Abrechnung" entry**
   All three layouts already define `nav.expense` in messages but never render it in `<nav>`.
   Adding it creates a general entry point, though without trip context it lands on the expense
   home page which only shows a placeholder message.

5. **[P3 — Low] Expense home page (`/expense/`)**
   Currently renders a placeholder with `expense.home.info`. If navigation to this page
   becomes possible, consider listing all expenses for the user's trips here instead.

---

## Architectural Note: Cross-SCS Data

Showing total amounts on the trip list page (Trips SCS) requires data from the Expense SCS.
Two approaches are possible:

- **Client-side composition**: Trip list renders, then HTMX fires a separate request to
  `/expense/{tripId}/summary` for each COMPLETED trip row. This keeps SCS boundaries clean
  but adds per-row HTTP requests.
- **Server-side denormalisation**: When Expense events are published (ExpenseSettled),
  Trips consumes them and stores a `totalAmount` projection locally. This avoids runtime
  coupling but requires a new event consumption path.

For v0.5.0 scope, the simplest safe approach is: **direct link only on trip detail, no total
on trip list**. This requires zero cross-SCS calls and fixes the primary navigation gap
immediately. The expense total preview is a follow-up concern.
