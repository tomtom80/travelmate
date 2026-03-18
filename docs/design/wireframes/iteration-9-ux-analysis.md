# Iteration 9 — UX Analysis and Story Recommendations

**Version**: Iteration 9 planning (v0.9.0-SNAPSHOT)
**Date**: 2026-03-18
**Author**: UX Designer Agent
**Scope**: UX impact analysis of candidate stories, mobile/desktop split, dependency mapping, ranked recommendations, wireframe concepts for the Accommodation planning flow and party-level settlement view.

---

## 1. UX Impact Matrix

Ratings: **H** = high impact, **M** = medium impact, **L** = low impact, **—** = not affected

| Story | Anna (Organizer, desktop) | Max (Participant, mobile) | Kai (Co-Organizer, desktop/mobile) | Notes |
|-------|--------------------------|--------------------------|-------------------------------------|-------|
| **US-TRIPS-060** Add Accommodation Details | H | H | M | Core planning gap — no accommodation info anywhere in the app today |
| **US-TRIPS-061** Import Location from URL | M | L | L | Quality-of-life for Anna; requires TRIPS-060 to be useful |
| **US-TRIPS-062** Accommodation Poll | M | M | L | Nice-to-have; pre-supposes TRIPS-060; voting UI is new interaction pattern with high complexity cost |
| **US-EXP-013** Advance Payments | M | M | H | Kai and others need this when one person books in advance; currently invisible in the settlement |
| **US-EXP-022** Custom Splitting | L | L | H | Power-user feature for Kai; low friction gain for Max and Anna in simple use cases |
| **US-EXP-032** Settlement per Category | M | L | H | Useful for accountability reviews; primarily desktop/post-trip |
| **US-EXP-033** Export Settlement as PDF | H | L | H | Anna and Kai need a printable record; Max rarely needs this |
| **US-EXP-042** Re-submit Rejected Receipt | L | M | H | Addresses a real frustration (Kai's documented pain point); small surface area |
| **US-INFRA-041** PWA Manifest | L | H | M | Max (mobile-primary, shopping at store) is the direct beneficiary; install prompt reduces friction for the mobile-heaviest flow |
| **US-INFRA-042** Lighthouse CI | — | — | — | Developer tooling; no user-visible UX change |
| **Recipe Import from URL** | M | L | L | Anna plans meals; saves re-entry; requires external HTTP call — scope risk |
| **Bring App Integration** | L | H | L | Max uses Bring; but this duplicates the shopping list — risk of fragmentation |
| **Accommodation + Room Assignment** | H | M | M | New: Hüttenurlaub use case — assign families to rooms, see capacity, track advance payments |
| **Party-Level Settlement View** | H | M | H | New: group transfers by TravelParty instead of individual; critical for family trips |

---

## 2. Dependency Analysis

### Stories that enhance existing pages

| Story | Target Page | Change Type |
|-------|-------------|-------------|
| US-EXP-013 Advance Payments | `/expense/{tripId}` | New receipt type selector in the "Beleg hinzufügen" dialog |
| US-EXP-022 Custom Splitting | `/expense/{tripId}` | Additional field group in the "Beleg hinzufügen" dialog |
| US-EXP-032 Settlement per Category | `/expense/{tripId}` — SETTLED state | New section below settlement table |
| US-EXP-033 Export Settlement as PDF | `/expense/{tripId}` — SETTLED state | New action button in article header |
| US-EXP-042 Re-submit Rejected Receipt | `/expense/{tripId}` — receipt row (REJECTED state) | Inline edit trigger on existing row |
| US-INFRA-041 PWA Manifest | `<head>` of all pages | No visible page change; adds `manifest.json` and service worker stub |
| Party-Level Settlement View | `/expense/{tripId}` — SETTLED state | New article section grouping transfers by TravelParty |

### Stories that create new sections or pages

| Story | New Surface | Dependency |
|-------|-------------|------------|
| US-TRIPS-060 Accommodation + Room Assignment | New dedicated page `/trips/{id}/accommodation` + new `<article>` card on trip detail | None — standalone addition |
| US-TRIPS-061 Import from URL | New input flow within TRIPS-060 accommodation page | **Requires TRIPS-060** |
| US-TRIPS-062 Accommodation Poll | New `<dialog>` + poll state machine | **Requires TRIPS-060**; new interaction pattern (voting) not yet in design system |
| Recipe Import from URL | `/trips/{id}/recipes/new` — new import mode | Requires Recipe aggregate (done) |
| Bring App Integration | New settings section or shopping list button | Requires external OAuth flow to Bring |

---

## 3. Mobile vs Desktop Analysis

### Primarily mobile benefit

| Story | Rationale |
|-------|-----------|
| US-INFRA-041 PWA Manifest | Max uses the shopping list at the store; "Add to Home Screen" removes browser chrome. |
| US-EXP-042 Re-submit Receipt | Rejection is discovered on-the-go; re-submit should be a one-tap correction. |
| Accommodation read view | Max checks the address and check-in time while driving or at the train station. |

### Primarily desktop benefit

| Story | Rationale |
|-------|-----------|
| US-EXP-033 Export Settlement as PDF | Post-trip settlement review is always done at a desk. |
| US-EXP-032 Settlement per Category | Category breakdown is an analytical view. |
| US-TRIPS-061 Import from URL | URL paste + preview is an organizer task, done at a desk during trip planning. |
| Room assignment / advance payments | Organizer (Anna) sets this up while planning; detailed form work on desktop. |

### Equal benefit

| Story | Rationale |
|-------|-----------|
| US-TRIPS-060 Accommodation | Anna adds it on desktop; Max reads it on mobile the day they arrive. |
| Party-Level Settlement View | Anna reviews on desktop after the trip; Max checks his family's share on mobile. |
| US-EXP-013 Advance Payments | Kai books the hotel on a laptop; the balance is checked by any participant later. |

---

## 4. Recommendation: Top Stories for Iteration 9

Ranked by composite UX value (impact breadth × mobile/desktop fit × implementation risk ratio):

### Rank 1 — Accommodation Page with Room Assignment (new scope)

**Why first**: The Hüttenurlaub (cabin/group house trip) is the primary use case for large-party travel planning. Anna cannot share where they are staying, cannot assign families to rooms, and cannot calculate advance payments per family. This is a page-level gap that makes the app feel incomplete for the most common group trip scenario. Scope: new dedicated page at `/{tripId}/accommodation` with entry from the trip detail card.

### Rank 2 — Party-Level Settlement View (new scope)

**Why second**: The current settlement shows transfers between individuals (e.g., "Max Müller zahlt €45.00 an Anna Bauer"). For a Hüttenurlaub with families, the meaningful unit is the TravelParty ("Familie Müller zahlt €127.50 an Familie Bauer"). This does not require a new page — it is a new `<article>` section on the existing expense detail page. The individual view remains; the party view is additive.

### Rank 3 — US-TRIPS-060: Add Accommodation Details (M)

**Why third**: Even without room assignment, the basic accommodation info (name, address, check-in/out, website) fills the most obvious gap. If the full room assignment scope is too large for one iteration, this is the safe minimum viable version.

### Rank 4 — US-INFRA-041: PWA Manifest (S)

**Why fourth**: Iteration 8 confirmed that the shopping list is the primary mobile-heavy flow. The effort-to-value ratio is the best in the entire backlog. It also sets the foundation for future offline capabilities.

### Rank 5 — US-EXP-042: Re-submit Rejected Receipt (S)

**Why fifth**: Kai's documented pain point. Small surface area on an existing page.

### Rank 6 — US-EXP-013: Advance Payments (M)

**Why sixth**: One person frequently pays for accommodation or transport before the trip starts. Without this, the settlement is inaccurate for most real trips.

### Rank 7 — US-EXP-033: Export Settlement as PDF (M)

**Why seventh**: Anna needs a printable/shareable record after the trip.

### Rank 8 — US-EXP-032: Settlement per Category (M)

**Why eighth**: Extends the SETTLED expense page with a breakdown table. Kai and Anna both value this.

### Rank 9 — US-TRIPS-061: Import Location from URL (M)

**Why ninth**: Quality-of-life improvement for Anna; depends on TRIPS-060/accommodation page being done first.

### Rank 10 — Recipe Import from URL (deferred M)

**Why tenth**: Anna plans meals; eliminating manual recipe entry has real value. Reuses the URL import pattern from TRIPS-061.

### Not recommended for Iteration 9

| Story | Reason |
|-------|--------|
| US-TRIPS-062 Accommodation Poll | High interaction complexity for a feature that only makes sense after the accommodation page is settled. Defer to Iteration 10. |
| US-EXP-022 Custom Splitting | Power-user feature. Defer. |
| US-INFRA-042 Lighthouse CI | No user-visible UX change. |
| Bring App Integration | Fragments the source of truth. Deferred indefinitely. |

---

## 5. Wireframes: Accommodation Planning Flow

---

### 5.1 — Entry Point: Trip Detail Card

The trip detail page gains a new `<article>` card that links to the accommodation page. The card is always visible once a trip has start/end dates (same gate as Meal Plan and Shopping List).

```
┌─── article: Unterkunft ──────────────────────────────────────────┐
│  h2: Unterkunft                                                   │
│                                                                   │
│  (empty state — organizer):                                       │
│  p: "Noch keine Unterkunft eingetragen."                          │
│  [Unterkunft planen]  ← primary button → /{id}/accommodation     │
│                                                                   │
│  (filled state — all users):                                      │
│  Hotel Alpenhof · Dorfstraße 12, 6458 Ischgl                     │
│  Check-in: 12.07.2026 · Check-out: 19.07.2026                    │
│  3 Zimmer · 8 Betten · 2 frei                                    │
│  [Unterkunft anzeigen]  ← /{id}/accommodation                    │
└───────────────────────────────────────────────────────────────────┘
```

The summary line ("3 Zimmer · 8 Betten · 2 frei") is rendered server-side from the Accommodation aggregate. It gives participants a quick read without loading the full page. When no rooms are defined, the bed/capacity line is omitted.

---

### 5.2 — Accommodation Page: Desktop (Organizer view)

**URL**: `/{tripId}/accommodation`
**Layout**: Single column, `<main>` with `<hgroup>`, followed by `<article>` sections

```
╔══════════════════════════════════════════════════════════════════╗
║  nav: Travelmate  Reisen  Rezepte  Abmelden                      ║
╠══════════════════════════════════════════════════════════════════╣
║  hgroup:                                                         ║
║    h1: Unterkunft                                                ║
║    p: ↩ Alpen-Urlaub 2026  ← link to /{tripId}                  ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  ┌─── article: Details ─────────────────────────────────────┐   ║
║  │  header:                                                  │   ║
║  │    h2: Unterkunft                           [Bearbeiten]  │   ║
║  │    [Bearbeiten] only shown to organizer                   │   ║
║  │                                                           │   ║
║  │  dl:                                                      │   ║
║  │    dt: Name          dd: Berghütte Sonnenhang             │   ║
║  │    dt: Adresse       dd: Almweg 3, 5570 Mauterndorf       │   ║
║  │    dt: Check-in      dd: 24.12.2026  14:00 Uhr            │   ║
║  │    dt: Check-out     dd: 31.12.2026  10:00 Uhr            │   ║
║  │    dt: Gesamtpreis   dd: 1.400,00 EUR                     │   ║
║  │    dt: Website       dd: [berghütte-sonnenhang.at ↗]      │   ║
║  │    dt: Notizen       dd: Schlüssel beim Gasthof abholen.  │   ║
║  │                                                           │   ║
║  │  Ausstattung (tags):                                      │   ║
║  │  [Sauna] [Garten] [Bergblick] [Gemeinschaftsküche]        │   ║
║  └───────────────────────────────────────────────────────────┘   ║
║                                                                  ║
║  ┌─── article: Zimmer ──────────────────────────────────────┐   ║
║  │  header:                                                  │   ║
║  │    h2: Zimmer                                 [+ Zimmer]  │   ║
║  │    [+ Zimmer] only shown to organizer                     │   ║
║  │                                                           │   ║
║  │  ┌─ grid (2 columns on desktop) ──────────────────────┐  │   ║
║  │  │                                                     │  │   ║
║  │  │  ┌─ article: room card ──────────────────────────┐ │  │   ║
║  │  │  │  header:                                       │ │  │   ║
║  │  │  │    h3: Zimmer 1 — Familienzimmer          [✎]  │ │  │   ║
║  │  │  │    small: 4 Betten · 140,00 EUR/Nacht          │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  Belegt: ████████░░ 3/4 Betten               │ │  │   ║
║  │  │  │  progress value="3" max="4"                   │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  Belegung:                                    │ │  │   ║
║  │  │  │  Familie Mueller (2 Erw. + 1 Ki.)  ✓ 3 von 4 │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  [Zuweisung ändern]  ← organizer only         │ │  │   ║
║  │  │  └────────────────────────────────────────────────┘ │  │   ║
║  │  │                                                     │  │   ║
║  │  │  ┌─ article: room card ──────────────────────────┐ │  │   ║
║  │  │  │  header:                                       │ │  │   ║
║  │  │  │    h3: Zimmer 2 — Doppelzimmer            [✎]  │ │  │   ║
║  │  │  │    small: 2 Betten · 80,00 EUR/Nacht           │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  Belegt: ░░░░░░░░░░ 0/2 Betten               │ │  │   ║
║  │  │  │  progress value="0" max="2"                   │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  Belegung: — (frei)                           │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  [Zuweisung ändern]                           │ │  │   ║
║  │  │  └────────────────────────────────────────────────┘ │  │   ║
║  │  │                                                     │  │   ║
║  │  │  ┌─ article: room card ──────────────────────────┐ │  │   ║
║  │  │  │  header:                                       │ │  │   ║
║  │  │  │    h3: Matratzenlager                      [✎]  │ │  │   ║
║  │  │  │    small: 6 Betten · inklusive                 │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  Belegt: ██████████ 5/6 Betten               │ │  │   ║
║  │  │  │  progress value="5" max="6"                   │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  Belegung:                                    │ │  │   ║
║  │  │  │  Familie Schmidt (2 Erw. + 3 Ki.)  ✓ 5 von 6 │ │  │   ║
║  │  │  │                                                │ │  │   ║
║  │  │  │  [Zuweisung ändern]                           │ │  │   ║
║  │  │  └────────────────────────────────────────────────┘ │  │   ║
║  │  └─────────────────────────────────────────────────────┘  │   ║
║  │                                                           │   ║
║  │  Nicht zugewiesen:                                        │   ║
║  │  Familie Bauer (1 Erw. + 2 Ki.) ← highlighted warning    │   ║
║  └───────────────────────────────────────────────────────────┘   ║
║                                                                  ║
║  ┌─── article: Vorauszahlungen ─────────────────────────────┐   ║
║  │  header:                                                  │   ║
║  │    h2: Vorauszahlungen                   [Betrag ändern]  │   ║
║  │    p (small): "500,00 EUR pro Partei · 3 × 500 = 1.500"  │   ║
║  │                                                           │   ║
║  │  ┌─ table ──────────────────────────────────────────┐    │   ║
║  │  │ Reisepartei           Betrag       Bezahlt        │    │   ║
║  │  ├──────────────────────────────────────────────────┤    │   ║
║  │  │ Familie Mueller       500,00 EUR   [✓ bezahlt]    │    │   ║
║  │  │ Familie Schmidt       500,00 EUR   [○ offen]      │    │   ║
║  │  │ Familie Bauer         500,00 EUR   [○ offen]      │    │   ║
║  │  └──────────────────────────────────────────────────┘    │   ║
║  │                                                           │   ║
║  │  Eingegangen: 500,00 EUR von 1.500,00 EUR                │   ║
║  │  progress value="500.00" max="1500.00"                   │   ║
║  └───────────────────────────────────────────────────────────┘   ║
║                                                                  ║
║  [← Zurück zur Reise]                                            ║
╚══════════════════════════════════════════════════════════════════╝
```

---

### 5.3 — Accommodation Page: Mobile (Participant read view)

Max checks the accommodation details the day of arrival. The mobile view is read-only, compact, and fast to scan.

```
╔═══════════════════════════════╗
║  ≡  Travelmate                ║
╠═══════════════════════════════╣
║  h1: Unterkunft               ║
║  ↩ Alpen-Urlaub 2026          ║
╠═══════════════════════════════╣
║                               ║
║  ┌─ article ────────────────┐ ║
║  │  h2: Berghütte Sonnenh.  │ ║
║  │  Almweg 3, 5570 Mautern. │ ║
║  │                          │ ║
║  │  Check-in:  24.12. 14:00 │ ║
║  │  Check-out: 31.12. 10:00 │ ║
║  │                          │ ║
║  │  [Sauna] [Bergblick]     │ ║
║  │                          │ ║
║  │  [Website öffnen ↗]      │ ║
║  │  [In Karte zeigen ↗]     │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ article ────────────────┐ ║
║  │  h2: Zimmer              │ ║
║  │                          │ ║
║  │  Zimmer 1 — Familienzi.  │ ║
║  │  4 Betten · 3 belegt     │ ║
║  │  Familie Mueller         │ ║
║  │  ████████░░              │ ║
║  │                          │ ║
║  │  Zimmer 2 — Doppelzi.    │ ║
║  │  2 Betten · frei         │ ║
║  │  ░░░░░░░░░░              │ ║
║  │                          │ ║
║  │  Matratzenlager          │ ║
║  │  6 Betten · 5 belegt     │ ║
║  │  Familie Schmidt         │ ║
║  │  ██████████░             │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ article ────────────────┐ ║
║  │  h2: Vorauszahlungen     │ ║
║  │  500 EUR pro Partei       │ ║
║  │                          │ ║
║  │  Familie Mueller  500,00 │ ║
║  │  [✓ bezahlt]             │ ║
║  │                          │ ║
║  │  Familie Schmidt  500,00 │ ║
║  │  [offen]                 │ ║
║  │                          │ ║
║  │  Familie Bauer    500,00 │ ║
║  │  [offen]                 │ ║
║  └──────────────────────────┘ ║
║                               ║
╚═══════════════════════════════╝
```

On mobile, the room grid collapses to a single column. The `<progress>` bar is retained — it communicates capacity without numbers. "In Karte zeigen" is a `<a href="https://maps.google.com/?q=..." target="_blank">` link — address pre-filled from stored data.

---

### 5.4 — Edit Accommodation Details (HTMX inline swap)

The "Bearbeiten" button on the details article triggers `hx-get="/{id}/accommodation/edit"` which swaps the `<article>` body with the edit form. The same `<article>` outerHTML is returned on save (`hx-post="/{id}/accommodation"`, `hx-swap="outerHTML"`, `hx-target="closest article"`).

```
┌─── article: Unterkunft bearbeiten ───────────────────────────────┐
│                                                                   │
│  form hx-post="/{id}/accommodation"                              │
│       hx-target="closest article"                                │
│       hx-swap="outerHTML":                                       │
│                                                                   │
│  label: Name der Unterkunft                                       │
│  [Berghütte Sonnenhang                                        ]   │
│                                                                   │
│  label: Adresse                                                   │
│  [Almweg 3, 5570 Mauterndorf                                  ]   │
│                                                                   │
│  .grid (2 columns):                                               │
│    label: Check-in Datum    label: Check-in Uhrzeit              │
│    [2026-12-24           ]  [14:00              ]                 │
│                                                                   │
│  .grid (2 columns):                                               │
│    label: Check-out Datum   label: Check-out Uhrzeit             │
│    [2026-12-31           ]  [10:00              ]                 │
│                                                                   │
│  label: Gesamtpreis in EUR (optional)                             │
│  [1400.00                                                     ]   │
│                                                                   │
│  label: Website (optional)                                        │
│  [https://berghuette-sonnenhang.at                            ]   │
│                                                                   │
│  label: Notizen (optional)                                        │
│  <textarea rows="3">                                              │
│  [Schlüssel beim Gasthof abholen.                             ]   │
│                                                                   │
│  fieldset: Ausstattung                                            │
│  <legend>Ausstattung (optional)</legend>                          │
│  [✓] Sauna  [✓] Garten  [ ] Schwimmbad  [✓] Bergblick            │
│  [ ] Einkauf in der Nähe  [✓] Gemeinschaftsküche  [ ] TV          │
│                                                                   │
│  footer:                                                          │
│  [Abbrechen]                                        [Speichern]  │
└───────────────────────────────────────────────────────────────────┘
```

Amenities are a multi-select using `<input type="checkbox">` inside a `<fieldset>`. They POST as a list of strings. PicoCSS auto-styles the fieldset with a border and legend. The "Abbrechen" button triggers `hx-get="/{id}/accommodation"` to swap back the read view without saving.

---

### 5.5 — Add Room Dialog

The "Zimmer hinzufügen" button opens a `<dialog>`. On save, the room card is appended to the rooms grid via HTMX (hx-swap="beforeend" on the grid container).

```
╔══════════════════════════════════════════════════════╗
║  ┌─ dialog ─────────────────────────────────────────┐ ║
║  │  Zimmer hinzufügen                           [X]  │ ║
║  │  ──────────────────────────────────────────────   │ ║
║  │                                                   │ ║
║  │  label: Bezeichnung                               │ ║
║  │  [Zimmer 1                                    ]   │ ║
║  │                                                   │ ║
║  │  label: Zimmertyp                                 │ ║
║  │  <select>                                         │ ║
║  │    [Familienzimmer (4 Betten)             ▼]      │ ║
║  │    options: Einzelzimmer / Doppelzimmer /          │ ║
║  │             Familienzimmer / Matratzenlager /      │ ║
║  │             Sonstiges                             │ ║
║  │                                                   │ ║
║  │  label: Anzahl Betten                             │ ║
║  │  [4                                           ]   │ ║
║  │  small: "Wird automatisch vorausgefüllt"           │ ║
║  │                                                   │ ║
║  │  label: Preis pro Nacht in EUR (optional)          │ ║
║  │  [140.00                                      ]   │ ║
║  │                                                   │ ║
║  │  ──────────────────────────────────────────────   │ ║
║  │  [Abbrechen]                        [Hinzufügen]  │ ║
║  └───────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════╝
```

When a Zimmertyp is selected, the "Anzahl Betten" field is pre-filled with the typical count (Doppelzimmer → 2, Familienzimmer → 4, Matratzenlager → 6) via a small `onchange` JS handler or HTMX `hx-get` with `hx-trigger="change"` returning the field value. The field remains editable — pre-fill is only a convenience default.

---

### 5.6 — Room Assignment Interaction

Room assignment is the core interaction for the Hüttenurlaub use case. The design must work without drag-and-drop on mobile.

#### Desktop — Room Assignment Dialog

Clicking "Zuweisung ändern" on a room card opens a dialog. The organizer uses `<select>` elements to assign or unassign TravelParties from the room.

```
╔══════════════════════════════════════════════════════╗
║  ┌─ dialog ─────────────────────────────────────────┐ ║
║  │  Zimmer 1 — Familienzimmer (4 Betten)        [X]  │ ║
║  │  ──────────────────────────────────────────────   │ ║
║  │                                                   │ ║
║  │  Kapazität: 4 Betten                              │ ║
║  │  progress value="3" max="4"  (3 belegt / 4 frei) │ ║
║  │                                                   │ ║
║  │  Zugewiesene Reisepartei:                         │ ║
║  │  <select name="partyId">                          │ ║
║  │    [ — (frei lassen)                      ▼]      │ ║
║  │    [✓ Familie Mueller (2 Erw. + 1 Ki.)    ]       │ ║
║  │    [   Familie Schmidt (2 Erw. + 3 Ki.)   ]       │ ║
║  │    [   Familie Bauer   (1 Erw. + 2 Ki.)   ]       │ ║
║  │                                                   │ ║
║  │  Wenn belegt:                                     │ ║
║  │  Benötigte Betten: 3 von 4 ✓                      │ ║
║  │  (rendered server-side on change via hx-get)      │ ║
║  │                                                   │ ║
║  │  ──────────────────────────────────────────────   │ ║
║  │  [Abbrechen]                        [Speichern]   │ ║
║  └───────────────────────────────────────────────────┘ ║
╚══════════════════════════════════════════════════════╝
```

One room = one assigned TravelParty (or unassigned). A TravelParty can be in at most one room. The `<select>` shows each party's name plus their size ("2 Erw. + 1 Ki.") so Anna can immediately see the fit vs capacity. Parties already assigned to another room are shown in the dropdown but labelled with that room name — the organizer can move them.

When the organizer selects a party, a server-side fragment is loaded (`hx-get="/{id}/rooms/{roomId}/fit?partyId=..."`, `hx-trigger="change"`, `hx-target="#fit-indicator"`) that shows whether the party fits: "3 von 4 Betten belegt ✓" or "5 von 4 Betten — Zu groß ✗".

#### Mobile — Room Assignment (same dialog, single-column layout)

The dialog on mobile fills the screen width. The `<select>` is the native mobile picker. The capacity progress bar and fit indicator are retained.

```
╔═══════════════════════════════╗
║  ┌─ dialog (full width) ────┐ ║
║  │  Zimmer 1 Familienzi. [X] │ ║
║  │  ──────────────────────── │ ║
║  │  4 Betten                 │ ║
║  │  ████████░░ 3/4           │ ║
║  │                           │ ║
║  │  Reisepartei:             │ ║
║  │  [Familie Mueller      ▼] │ ║
║  │                           │ ║
║  │  3 von 4 Betten ✓         │ ║
║  │                           │ ║
║  │  [Abbrechen] [Speichern]  │ ║
║  └───────────────────────────┘ ║
╚═══════════════════════════════╝
```

No drag-and-drop at any breakpoint. The `<select>` interaction works identically on mobile and desktop — it uses the device's native picker on mobile, which is more reliable than a custom drag-and-drop target on a small touch screen.

#### Unassigned Parties Warning

Below the room grid, a warning `role="alert"` lists TravelParties with no room assignment:

```
┌─── role="alert" class="error" ─────────────────────────────────┐
│  3 Reisepartei(en) ohne Zimmerzuweisung:                        │
│  Familie Bauer (1 Erw. + 2 Ki.)                                 │
│  [Zimmer zuweisen →]  ← button that opens Zimmer 2's dialog    │
└─────────────────────────────────────────────────────────────────┘
```

This alert is only shown to the organizer. It disappears once all parties are assigned. It is rendered server-side in the HTMX fragment returned after any assignment change.

---

## 6. Wireframes: Advance Payment Section

The advance payment section is below the room list. It uses an **equal round amount per party** model: the system auto-suggests a round amount based on accommodation price / party count, the Organizer confirms or adjusts, and the same amount applies to ALL parties.

### Desktop — Advance Payments (Before Confirmation)

The initial state shows the auto-suggestion flow. The Organizer sees the accommodation price, the party count, and the suggested amount. A single editable input allows adjustment.

```
┌─── article: Vorauszahlungen ─────────────────────────────────────┐
│  header:                                                          │
│    h2: Vorauszahlungen                                            │
│                                                                   │
│  p (muted): "Basierend auf 1.400,00 EUR Gesamtpreis              │
│  (Berghütte Sonnenhang) und 3 Reiseparteien."                    │
│                                                                   │
│  ┌─ form: Vorschlag ───────────────────────────────────────┐     │
│  │                                                          │     │
│  │  dl:                                                     │     │
│  │    dt: Unterkunftspreis   dd: 1.400,00 EUR               │     │
│  │    dt: Anzahl Parteien    dd: 3                           │     │
│  │    dt: Vorschlag          dd: 500,00 EUR pro Partei       │     │
│  │                                                          │     │
│  │  small: "Formel: ⌈1.400 / 3 / 50⌉ × 50 = 500 EUR"      │     │
│  │                                                          │     │
│  │  label: Betrag pro Reisepartei (EUR)                      │     │
│  │  [500.00                                             ]    │     │
│  │  small: "Du kannst den Betrag anpassen (z.B. für         │     │
│  │  Puffer). Alle Parteien zahlen denselben Betrag."         │     │
│  │                                                          │     │
│  │  [Vorauszahlungen bestätigen]  ← primary button          │     │
│  └──────────────────────────────────────────────────────────┘     │
└───────────────────────────────────────────────────────────────────┘
```

### Desktop — Advance Payments (After Confirmation)

Once confirmed, the section shows the per-party table with the same amount for each party and a "Bezahlt" toggle to track actual payment receipt.

```
┌─── article: Vorauszahlungen ─────────────────────────────────────┐
│  header:                                                          │
│    h2: Vorauszahlungen                          [Betrag ändern]   │
│                                                                   │
│  p (muted): "500,00 EUR pro Reisepartei ·                        │
│  Gesamt: 1.500,00 EUR (3 × 500,00 EUR)"                          │
│                                                                   │
│  ┌─ table ──────────────────────────────────────────────────┐    │
│  │ Reisepartei            Betrag        Bezahlt              │    │
│  ├──────────────────────────────────────────────────────────┤    │
│  │ Familie Mueller        500,00 EUR    [✓ bezahlt]          │    │
│  │ Familie Schmidt        500,00 EUR    [○ offen]            │    │
│  │ Familie Bauer          500,00 EUR    [○ offen]            │    │
│  ├──────────────────────────────────────────────────────────┤    │
│  │ Gesamt                1.500,00 EUR                        │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                   │
│  Fortschritt:                                                     │
│  progress value="500.00" max="1500.00"                           │
│  small: "500,00 EUR eingegangen von 1.500,00 EUR"                │
└───────────────────────────────────────────────────────────────────┘
```

The "Bezahlt" checkbox column toggles the advance payment status per party. Each checkbox is a small `<form>` posting to `/{id}/accommodation/advance/{partyId}/toggle` via HTMX. The form uses `hx-target="closest tr"` to update only the row and `hx-swap="outerHTML"` to replace the entire row. The progress bar is a separate fragment refreshed via `hx-target="#advance-progress"`.

**Amount model**: Equal round amount for ALL parties. No per-party weighting or headcount calculation. The auto-suggest formula is: `ceil(accommodationPrice / partyCount / 50) × 50`. The Organizer can adjust (e.g., add buffer) before confirming. The "Betrag ändern" button allows re-confirming with a different amount (resets "Bezahlt" status).

**When total price is not entered**: The section renders a prompt instead of the suggestion form:

```
┌─── article: Vorauszahlungen ─────────────────────────────────────┐
│  p: "Trage einen Gesamtpreis bei der Unterkunft ein, um          │
│      Vorauszahlungen automatisch zu berechnen."                  │
│  [Gesamtpreis eintragen]  ← links to the edit form via hx-get   │
└───────────────────────────────────────────────────────────────────┘
```

---

## 7. Wireframes: Party-Level Settlement View

The current settlement in the expense SCS shows individual-level transfers: "Max Müller zahlt 45,00 EUR an Anna Bauer." For a Hüttenurlaub with 11 people across 3 families, this produces up to 6 individual transfer rows that are meaningless in practice. The organizer and participants need the family-level view.

### 7.1 — Current State (Individual View)

The existing `/expense/{tripId}` page in SETTLED state shows:

```
┌─── article: Ausgleichszahlungen ────────────────────────────────┐
│  Von                    →  An                    Betrag          │
│  Max Mueller               Anna Bauer            45,00 EUR       │
│  Sophie Mueller            Anna Bauer            32,50 EUR       │
│  Jonas Schmidt             Anna Bauer            81,00 EUR       │
│  Lena Schmidt              Tobias Bauer          22,00 EUR       │
│  ...                                                             │
└─────────────────────────────────────────────────────────────────┘
```

This is unreadable for family trips. Who tells Max to pay? Anna? Both Anna and the Bauer family?

### 7.2 — Party-Level Summary Section (new article, added below existing)

A new `<article>` section is added below the existing individual settlement plan. It is only shown when the expense is SETTLED. The individual section remains — the party view is additive, not a replacement.

#### Desktop — Party-Level Summary

```
┌─── article: Zusammenfassung nach Reisepartei ─────────────────────┐
│  header:                                                           │
│    h2: Abrechnung nach Reisepartei                                 │
│    small: "Zeigt Salden und Überweisungen auf Parteiebene"         │
│                                                                   │
│  ┌─ table: Party Balances ──────────────────────────────────────┐ │
│  │  Reisepartei      Bezahlt      Anteil        Saldo           │ │
│  ├──────────────────────────────────────────────────────────────┤ │
│  │  Familie Mueller  456,00 EUR   380,00 EUR    +76,00 EUR ✓    │ │
│  │  Familie Schmidt  180,00 EUR   636,00 EUR   -456,00 EUR ↑    │ │
│  │  Familie Bauer    244,00 EUR   264,00 EUR    -20,00 EUR ↑    │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Ausgleichsüberweisungen:                                         │
│  ┌─ dl class="transfers" ────────────────────────────────────┐   │
│  │                                                            │   │
│  │  dt: Familie Schmidt                                       │   │
│  │  dd: zahlt 456,00 EUR an Familie Mueller                   │   │
│  │                                                            │   │
│  │  dt: Familie Bauer                                         │   │
│  │  dd: zahlt 20,00 EUR an Familie Mueller                    │   │
│  │                                                            │   │
│  └────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────┘
```

The party balances table uses the same visual treatment as the existing individual participant summary: positive balance in green (CSS class `balance-positive`), negative in red (CSS class `balance-negative`). The "zahlt … an …" sentence format is the one the user explicitly asked for.

#### Mobile — Party-Level Settlement

On mobile, the table collapses to cards (same pattern as the existing expense SCS):

```
╔═══════════════════════════════╗
║  h2: Abrechnung je Partei     ║
║                               ║
║  ┌─ article (Familie Mueller)┐ ║
║  │  Familie Mueller          │ ║
║  │  Bezahlt:   456,00 EUR    │ ║
║  │  Anteil:    380,00 EUR    │ ║
║  │  Saldo:   +76,00 EUR  ✓   │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ article (Familie Schmidt)┐ ║
║  │  Familie Schmidt          │ ║
║  │  Bezahlt:   180,00 EUR    │ ║
║  │  Anteil:    636,00 EUR    │ ║
║  │  Saldo:  -456,00 EUR  ↑   │ ║
║  │  zahlt an Familie Mueller  │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ article (Familie Bauer) ┐ ║
║  │  Familie Bauer            │ ║
║  │  Bezahlt:   244,00 EUR    │ ║
║  │  Anteil:    264,00 EUR    │ ║
║  │  Saldo:   -20,00 EUR  ↑   │ ║
║  │  zahlt an Familie Mueller  │ ║
║  └──────────────────────────┘ ║
╚═══════════════════════════════╝
```

The arrow symbol (↑) indicates "owes money"; the checkmark (✓) indicates "is owed money". These are CSS classes (`balance-positive`, `balance-negative`) — not inline styles. The plain text sentence "zahlt an Familie Mueller" inside the card replaces the individual transfer row, giving Max one actionable sentence per family.

### 7.3 — Implementation Note: Where Does the Party Data Come From?

The expense SCS currently works with `participantId` values (AccountId or DependentId). To group by TravelParty, the expense SCS needs a mapping from participantId → partyName. This mapping should come from the TripProjection read model, which the expense SCS already maintains. The TripProjection would need a `partyName` field per participant entry (projected from the TravelParty projection in the trips SCS).

The party-level grouping is a **presentation concern** — the underlying settlement algorithm (individual transfers) does not change. The server aggregates individual transfers into party-level totals and renders the new section. No domain model change is required.

---

## 8. Summary Table

| Rank | Story | Size | Personas | Primary Device | Page Surface |
|------|-------|------|----------|----------------|-------------|
| 1 | Accommodation Page + Room Assignment | L | Anna H, Max H, Kai M | Both | New page `/{tripId}/accommodation` |
| 2 | Party-Level Settlement View | M | Anna H, Kai H, Max M | Both | New section on SETTLED expense page |
| 3 | US-TRIPS-060 Accommodation Details (min) | M | Anna H, Max H, Kai M | Both | New section on trip detail |
| 4 | US-INFRA-041 PWA Manifest | S | Max H, Kai M | Mobile | `<head>` + static manifest |
| 5 | US-EXP-042 Re-submit Rejected Receipt | S | Kai H, Max M | Mobile | Existing expense receipt row |
| 6 | US-EXP-013 Advance Payments | M | Kai H, Anna M, Max M | Desktop | Receipt dialog (new type) |
| 7 | US-EXP-033 Export Settlement PDF | M | Anna H, Kai H | Desktop | SETTLED expense page |
| 8 | US-EXP-032 Settlement per Category | M | Kai H, Anna M | Desktop | SETTLED expense page |
| 9 | US-TRIPS-061 Import Location from URL | M | Anna M | Desktop | Accommodation section (after #1/#3) |
| 10 | Recipe Import from URL | M | Anna M | Desktop | Recipe create page |

Stories not recommended: US-TRIPS-062, US-EXP-022, US-INFRA-042, Bring App Integration.

---

## 9. i18n Keys

### Trips SCS — Accommodation

| Key | DE | EN |
|-----|----|----|
| `accommodation.title` | Unterkunft | Accommodation |
| `accommodation.name` | Name der Unterkunft | Accommodation name |
| `accommodation.address` | Adresse | Address |
| `accommodation.checkin` | Check-in | Check-in |
| `accommodation.checkin.time` | Check-in Uhrzeit | Check-in time |
| `accommodation.checkout` | Check-out | Check-out |
| `accommodation.checkout.time` | Check-out Uhrzeit | Check-out time |
| `accommodation.totalPrice` | Gesamtpreis | Total price |
| `accommodation.website` | Website | Website |
| `accommodation.notes` | Notizen | Notes |
| `accommodation.amenities` | Ausstattung | Amenities |
| `accommodation.amenity.SAUNA` | Sauna | Sauna |
| `accommodation.amenity.GARDEN` | Garten | Garden |
| `accommodation.amenity.MOUNTAIN_VIEW` | Bergblick | Mountain view |
| `accommodation.amenity.SHARED_KITCHEN` | Gemeinschaftsküche | Shared kitchen |
| `accommodation.amenity.SHOPPING_NEARBY` | Einkauf in der Nähe | Shopping nearby |
| `accommodation.amenity.TV` | TV | TV |
| `accommodation.empty` | Noch keine Unterkunft eingetragen. | No accommodation details entered yet. |
| `accommodation.edit` | Bearbeiten | Edit |
| `accommodation.plan` | Unterkunft planen | Plan accommodation |
| `accommodation.view` | Unterkunft anzeigen | View accommodation |
| `accommodation.openMap` | In Karte zeigen | Show on map |
| `accommodation.openWebsite` | Website öffnen | Open website |
| `room.title` | Zimmer | Rooms |
| `room.add` | + Zimmer | + Room |
| `room.label` | Bezeichnung | Label |
| `room.type` | Zimmertyp | Room type |
| `room.type.SINGLE` | Einzelzimmer | Single room |
| `room.type.DOUBLE` | Doppelzimmer | Double room |
| `room.type.FAMILY` | Familienzimmer | Family room |
| `room.type.DORMITORY` | Matratzenlager | Dormitory |
| `room.type.OTHER` | Sonstiges | Other |
| `room.bedCount` | Anzahl Betten | Number of beds |
| `room.pricePerNight` | Preis pro Nacht | Price per night |
| `room.assignment` | Zuweisung ändern | Change assignment |
| `room.assignedParty` | Zugewiesene Reisepartei | Assigned travel party |
| `room.free` | frei | free |
| `room.occupied` | belegt | occupied |
| `room.fits` | {0} von {1} Betten belegt ✓ | {0} of {1} beds occupied ✓ |
| `room.tooLarge` | {0} von {1} Betten — Zu groß ✗ | {0} of {1} beds — Too large ✗ |
| `room.unassigned.warning` | {0} Reisepartei(en) ohne Zimmerzuweisung: | {0} travel part(y/ies) without room assignment: |
| `advance.title` | Vorauszahlungen | Advance payments |
| `advance.basis` | Basierend auf {0} EUR Gesamtpreis und {1} Reiseparteien. | Based on {0} EUR total price and {1} travel parties. |
| `advance.suggestion` | Vorschlag: {0} EUR pro Reisepartei | Suggestion: {0} EUR per travel party |
| `advance.formula` | Formel: ⌈{0} / {1} / 50⌉ × 50 = {2} EUR | Formula: ⌈{0} / {1} / 50⌉ × 50 = {2} EUR |
| `advance.amountPerParty` | Betrag pro Reisepartei (EUR) | Amount per travel party (EUR) |
| `advance.adjustHint` | Du kannst den Betrag anpassen (z.B. für Puffer). Alle Parteien zahlen denselben Betrag. | You can adjust the amount (e.g. for buffer). All parties pay the same amount. |
| `advance.confirm` | Vorauszahlungen bestätigen | Confirm advance payments |
| `advance.changeAmount` | Betrag ändern | Change amount |
| `advance.perParty` | {0} EUR pro Partei | {0} EUR per party |
| `advance.totalSummary` | Gesamt: {0} EUR ({1} × {2} EUR) | Total: {0} EUR ({1} × {2} EUR) |
| `advance.party` | Reisepartei | Travel party |
| `advance.amount` | Betrag | Amount |
| `advance.paid` | Bezahlt | Paid |
| `advance.unpaid` | Offen | Unpaid |
| `advance.total` | Gesamt | Total |
| `advance.progress` | {0} EUR eingegangen von {1} EUR | {0} EUR received of {1} EUR |
| `advance.noPricePrompt` | Trage einen Gesamtpreis bei der Unterkunft ein, um Vorauszahlungen automatisch zu berechnen. | Enter a total price for the accommodation to automatically calculate advance payments. |
| `advance.enterPrice` | Gesamtpreis eintragen | Enter total price |

### Expense SCS — Party-Level Settlement

| Key | DE | EN |
|-----|----|----|
| `expense.partySettlement.title` | Abrechnung nach Reisepartei | Settlement by travel party |
| `expense.partySettlement.subtitle` | Zeigt Salden und Überweisungen auf Parteiebene | Shows balances and transfers at party level |
| `expense.partySettlement.party` | Reisepartei | Travel party |
| `expense.partySettlement.totalPaid` | Bezahlt | Paid |
| `expense.partySettlement.fairShare` | Anteil | Fair share |
| `expense.partySettlement.balance` | Saldo | Balance |
| `expense.partySettlement.transfers` | Ausgleichsüberweisungen | Settlement transfers |
| `expense.partySettlement.pays` | zahlt {0} EUR an {1} | pays {0} EUR to {1} |

---

## 10. Design Documents to Create (next steps)

Once the iteration scope is confirmed, the following design documents should be produced before implementation begins:

1. `docs/design/components/accommodation-iteration9.md` — full component spec for accommodation page, room model, assignment dialog, advance payment table; HTMX interaction map
2. `docs/design/components/expense-iteration9.md` — party settlement section, receipt resubmit dialog (EXP-042), advance payment type (EXP-013), category breakdown (EXP-032), PDF export (EXP-033)
3. `docs/design/wireframes/expense-iteration9.md` — updated expense detail page with all new sections and states
4. Update `docs/design/components/trip-detail-page.md` — add accommodation card to the recommended page structure
5. User journey map: `docs/design/journeys/accommodation-iteration9-flows.md` — organizer setup journey, participant arrival day journey
