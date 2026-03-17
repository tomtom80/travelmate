# Wireframes: Shopping List — Iteration 8

**Version**: Iteration 8 design (v0.9.0-SNAPSHOT)
**Status**: Design specification — no code written
**Companion documents**:
- Component specs: `docs/design/components/shopping-list-iteration8.md`
- User journeys: `docs/design/journeys/shopping-list-iteration8-flows.md`

---

## Navigation and Integration Point

The Shopping List is a **dedicated page** at `/trips/{id}/shoppinglist`, following the same pattern as the Meal Plan page (`/trips/{id}/mealplan`). It is reachable from the Trip detail page via a new `<article>` card section, visible whenever a meal plan exists for the trip.

### New section on Trip detail page

```
┌─── article: Einkaufsliste ─────────────────────────────────────────┐
│  h2: Einkaufsliste                                                  │
│  p: "Einkaufsliste aus dem Essensplan generieren oder manuell       │
│      Einträge hinzufügen."                                          │
│                                                                     │
│  [Einkaufsliste anzeigen]   ← always shown when meal plan exists   │
│                                                                     │
│  Note: Link is visible to all trip participants                     │
└─────────────────────────────────────────────────────────────────────┘
```

The shopping list page is accessible to all trip participants regardless of trip status. There is no status gate — a list can be built during planning and used at the store during `IN_PROGRESS`.

---

## Page: Shopping List (Desktop)

**URL**: `/trips/{id}/shoppinglist`
**Layout**: Single column, `<main>` with `<hgroup>`, followed by `<article>` sections

```
╔══════════════════════════════════════════════════════════════════╗
║  nav: Travelmate  Reisen  Rezepte  Abmelden                      ║
╠══════════════════════════════════════════════════════════════════╣
║  hgroup:                                                         ║
║    h1: Einkaufsliste                                             ║
║    p: ↩ Alpen-Urlaub 2026   ← link back to trip detail          ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  ┌─── article: Aus dem Essensplan ──────────────────────────┐   ║
║  │  header:                                                  │   ║
║  │    h2: Aus dem Essensplan                                 │   ║
║  │    [Aktualisieren]  ← outline button, right              │   ║
║  │                                                           │   ║
║  │  p: "12 Einträge aus 7 Rezepten (4 Personen)"            │   ║
║  │                                                           │   ║
║  │  ┌─ table ──────────────────────────────────────────┐    │   ║
║  │  │ Eintrag        Menge  Einheit  Wer?     Status   │    │   ║
║  │  ├──────────────────────────────────────────────────┤    │   ║
║  │  │ Haferflocken   400    g        —        [OPEN]   │    │   ║
║  │  │ Milch          2      l        Anna     [OVER.]  │    │   ║
║  │  │ Eier           12     Stk.     —        [OPEN]   │    │   ║
║  │  │ Brot           2      Stk.     Max      [DONE]   │    │   ║
║  │  └──────────────────────────────────────────────────┘    │   ║
║  └───────────────────────────────────────────────────────────┘   ║
║                                                                  ║
║  ┌─── article: Manuelle Einträge ───────────────────────────┐   ║
║  │  header:                                                  │   ║
║  │    h2: Manuelle Einträge                                  │   ║
║  │                                                           │   ║
║  │  ┌─ table ──────────────────────────────────────────┐    │   ║
║  │  │ Eintrag        Menge  Einheit  Wer?     Status   │    │   ║
║  │  ├──────────────────────────────────────────────────┤    │   ║
║  │  │ Sonnencrème    1      Stk.     —        [OPEN]   │    │   ║
║  │  └──────────────────────────────────────────────────┘    │   ║
║  │                                                           │   ║
║  │  ── Eintrag hinzufügen ─────────────────────────────     │   ║
║  │  ┌─ inline form ──────────────────────────────────┐      │   ║
║  │  │  [Name                ] [Menge] [Einheit] [+]  │      │   ║
║  │  └────────────────────────────────────────────────┘      │   ║
║  └───────────────────────────────────────────────────────────┘   ║
║                                                                  ║
║  [← Zurück zur Reise]                                            ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## Item Row: States

Each shopping list item row renders differently based on its status. The status determines available actions.

### OPEN (unassigned)

```
┌──────────────────────────────────────────────────────────────────┐
│  Haferflocken    400   g     —        [OPEN]   [Ich übernehme]  │
└──────────────────────────────────────────────────────────────────┘
```

- Name column: normal weight
- Status cell: `<mark class="status-open">` (neutral, muted)
- Action cell: "Ich übernehme" button — `class="outline"`, small
- Assignee: em dash (—)

### ASSIGNED (by current user)

```
┌──────────────────────────────────────────────────────────────────┐
│  Haferflocken    400   g     Du       [ÜBER.]  [Abgeben] [Erledigt] │
└──────────────────────────────────────────────────────────────────┘
```

- Assignee: "Du" (current user) or full name for other participants
- Status cell: `<mark class="status-assigned">` (blue, indicating action in progress)
- Actions: "Abgeben" (unassign self — `class="outline secondary"`) + "Erledigt" (mark done — `class="outline"`)
- "Abgeben" only shown when assignee is current user
- "Erledigt" only shown when assignee is current user

### ASSIGNED (by another participant)

```
┌──────────────────────────────────────────────────────────────────┐
│  Haferflocken    400   g     Anna     [ÜBER.]  —                 │
└──────────────────────────────────────────────────────────────────┘
```

- No action buttons — other participants cannot unassign or complete someone else's item
- Assignee shown by first name

### PURCHASED (done)

```
┌──────────────────────────────────────────────────────────────────┐
│  ~~Brot~~        2   Stk.   Max      [ERLEDIGT]   —             │
└──────────────────────────────────────────────────────────────────┘
```

- Name column: `<del>` or `class="item-purchased"` with CSS `text-decoration: line-through; opacity: 0.6`
- Status cell: `<mark class="status-purchased">` (green)
- No action buttons — purchased items are done

---

## Item Row: Source Differentiation

Recipe-generated items have a source indicator that is visible but not prominent.

```
Desktop (table row — additional context column):
┌─────────────────────────────────────────────────────────────────────┐
│  Haferflocken    400   g    Guten-Morgen-Müsli (×2)   —    [OPEN] │
│                             ↑ small, muted text: recipe name       │
└─────────────────────────────────────────────────────────────────────┘
```

Recipe-sourced quantities are scaled: "400 g" = 200g × 2 servings. The tooltip or `<small>` label shows the recipe name and multiplier. Manual items have no source label.

---

## Scaling Info Panel (desktop only)

Above the recipe items table, a muted info bar explains the scaling:

```
┌─────────────────────────────────────────────────────────────────────┐
│  ℹ  Mengen skaliert auf 4 Personen — Rezepte für je 2 Portionen.   │
│     Angepasst an: Essensplan vom 12.07.–19.07.2026                 │
└─────────────────────────────────────────────────────────────────────┘
```

This is a `<p class="notice-info">` inside the article header. It does not appear on mobile (hidden via `@media (max-width: 640px)`).

---

## Filter / Status Group Tabs (desktop)

Above the combined item table (or between the two article sections), a filter bar allows filtering by status. This is rendered server-side as query parameters:

```
[Alle (15)]  [Ausstehend (9)]  [Übernommen (3)]  [Erledigt (3)]
```

Implemented as `<nav>` with `<ul>` links containing `?filter=open`, `?filter=assigned`, `?filter=purchased`. The active filter item gets `aria-current="page"`. Full page reload on filter change — no HTMX needed here.

---

## Page: Shopping List (Mobile — participant at the store)

Mobile is the **primary use case** for participants. The layout prioritises:
1. Item name and quantity — large tap target
2. Action button — primary action per item status
3. Minimal chrome — no desktop sidebars, no info bars

```
╔═══════════════════════════════╗
║  ≡  Travelmate                ║
╠═══════════════════════════════╣
║  h1: Einkaufsliste            ║
║  ↩ Alpen-Urlaub 2026          ║
╠═══════════════════════════════╣
║  [Alle] [Ausstehend] [Erledigt]║
╠═══════════════════════════════╣
║                               ║
║  ┌─ article (card) ─────────┐ ║
║  │  Haferflocken             │ ║
║  │  400 g                    │ ║
║  │  Aus: Guten-Morgen-Müsli  │ ║
║  │  ─────────────────────── │ ║
║  │  [  Ich übernehme  ]      │ ║
║  └───────────────────────────┘ ║
║                               ║
║  ┌─ article (card) ─────────┐ ║
║  │  Milch                    │ ║
║  │  2 l        [ÜBERNOMMEN]  │ ║
║  │  Anna Müller              │ ║
║  └───────────────────────────┘ ║
║                               ║
║  ┌─ article (card) ─────────┐ ║
║  │  ~~Brot~~                 │ ║
║  │  ~~2 Stk.~~   [ERLEDIGT]  │ ║
║  │  Max Meier                │ ║
║  └───────────────────────────┘ ║
║                               ║
║  ┌─ article (card, my item) ┐ ║
║  │  Eier                     │ ║
║  │  12 Stk.    [ÜBERNOMMEN]  │ ║
║  │  Du                       │ ║
║  │  ─────────────────────── │ ║
║  │  [Abgeben] [✓ Erledigt]   │ ║
║  └───────────────────────────┘ ║
║                               ║
║  + Manuell hinzufügen         ║
║                               ║
╚═══════════════════════════════╝
```

**Mobile card implementation**: The table is hidden on mobile (`@media (max-width: 640px) { table { display: none; } }`). A separate fragment renders mobile cards with the same HTMX behaviour. Alternatively, use CSS-only responsive table → card transformation (as done in the expense SCS).

**"+ Manuell hinzufügen"** on mobile opens a `<dialog>` with the add form, rather than the inline desktop row.

---

## Dialog: Add Manual Item (Mobile)

```
╔═══════════════════════════════╗
║  ┌─ dialog ─────────────────┐ ║
║  │  Eintrag hinzufügen   [X] │ ║
║  │  ───────────────────────  │ ║
║  │  Name                     │ ║
║  │  [Sonnencrème           ] │ ║
║  │                           │ ║
║  │  Menge          Einheit   │ ║
║  │  [1           ] [Stk.   ] │ ║
║  │                           │ ║
║  │  [Abbrechen]  [Hinzufügen] │ ║
║  └───────────────────────────┘ ║
╚═══════════════════════════════╝
```

On desktop, the "add item" form is inline below the manual items table. On mobile, a button "Eintrag hinzufügen" opens this dialog. Both submit to the same HTMX endpoint.

---

## Inline Add Form (Desktop)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Neuer Eintrag                                                       │
│  ┌──────────────────────────┐  ┌────────┐  ┌──────────┐  ┌──────┐ │
│  │  Name                    │  │ Menge  │  │ Einheit  │  │  +   │ │
│  └──────────────────────────┘  └────────┘  └──────────┘  └──────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

Uses PicoCSS `.grid` with 4 columns. The "+" submit button uses `aria-label="Eintrag hinzufügen"`. After successful submission, the manual items table body is swapped via HTMX and the form resets.

---

## Polling Indicator (HTMX polling)

The shopping list container polls every 5 seconds for updates when other participants are active. The polling is subtle — no visible spinner. The container uses `hx-trigger="every 5s"` on the item list div.

When an update is applied (content differs from current), HTMX applies the swap with a brief CSS transition on the changed row:

```css
/* Applied by server via hx-reswap="innerHTML transition:true" */
@keyframes highlightUpdate {
    from { background-color: var(--pico-primary-background); }
    to   { background-color: transparent; }
}
.item-updated {
    animation: highlightUpdate 1.5s ease-out forwards;
}
```

The server applies `class="item-updated"` to rows that changed since the last poll. This gives a brief yellow-to-transparent flash without jarring reflows.

---

## Email: Trip Invitation (US-IAM-050)

The invitation email is an HTML email rendered via Thymeleaf template. Layout uses inline CSS for email client compatibility.

```
╔══════════════════════════════════════════════════╗
║                                                  ║
║   TRAVELMATE                                     ║
║   ──────────────────────────────────────────     ║
║                                                  ║
║   Hallo [Vorname],                               ║
║                                                  ║
║   [Einladender Name] hat dich zu einer           ║
║   Reise eingeladen.                              ║
║                                                  ║
║   ┌──────────────────────────────────────┐       ║
║   │  Alpen-Urlaub 2026                   │       ║
║   │  12.07.2026 – 19.07.2026             │       ║
║   └──────────────────────────────────────┘       ║
║                                                  ║
║   ┌──────────────────────────────────────┐       ║
║   │        Einladung annehmen            │       ║
║   └──────────────────────────────────────┘       ║
║                                                  ║
║   Oder klicke auf diesen Link:                   ║
║   https://travelmate.example.com/trips/accept/…  ║
║                                                  ║
║   Diese Einladung läuft in 7 Tagen ab.           ║
║                                                  ║
║   ──────────────────────────────────────────     ║
║   Travelmate — Gemeinsam planen.                 ║
║   Diese E-Mail wurde automatisch versandt.       ║
║                                                  ║
╚══════════════════════════════════════════════════╝
```

**Mobile email** (max-width: 600px): Single-column, button full-width, font sizes scale down proportionally. The trip info box collapses to single column.
