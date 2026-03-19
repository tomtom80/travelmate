# Iteration 10 — UX Analysis and Story Recommendations

**Version**: Iteration 10 planning (v0.10.0-SNAPSHOT)
**Date**: 2026-03-19
**Author**: UX Designer Agent
**Scope**: UX impact analysis of Iteration 10+ backlog candidates, impact/effort matrix, ranked recommendations, wireframes for Recipe URL Import, Accommodation URL Import, Settlement per Category, and Export Settlement as PDF.

---

## 1. Starting Point: What Iteration 9 Delivered

Iteration 9 (v0.9.0) shipped six stories. These are now baseline — the UX analysis for Iteration 10 builds on them:

| Story | What was delivered |
|-------|--------------------|
| S9-A: Accommodation with Room Inventory | Dedicated `/{tripId}/accommodation` page with name, address, check-in/out times, amenities, total price |
| S9-B: Room Assignment | Room grid with capacity progress bars; assign TravelParty to room via `<dialog>` + `<select>` |
| S9-C: Party-Level Settlement | New `<article>` on SETTLED expense page: party balance table + transfer sentences |
| S9-D: Advance Payments | Equal round amount per party; `Bezahlt` toggle; auto-suggestion from accommodation price |
| S9-E: Re-submit Rejected Receipt | Inline edit + re-submit on REJECTED receipt row |
| S9-F: PWA Manifest | `manifest.json` + install prompt; standalone mode on mobile |

---

## 2. Impact/Effort Matrix

Candidates are the remaining Iteration 10+ backlog items plus the four stories highlighted in the task brief.

Effort ratings: **S** = small (1–2 days), **M** = medium (3–5 days), **L** = large (1–2 weeks), **XL** = very large (>2 weeks).
Impact ratings: **H** = high impact across most users, **M** = medium impact, **L** = low impact or narrow audience.

| Story | ID | Size | Effort | UX Impact | Net Priority | Notes |
|-------|----|------|--------|-----------|--------------|-------|
| Recipe Import from URL | US-TRIPS-041 | L | M | M | **High** | Builds on existing Recipe aggregate; reuses URL import pattern established with accommodation URL import |
| Accommodation URL Import | US-TRIPS-061 | M | M | M | **High** | Adds URL import to the existing accommodation page; depends on S9-A (done); saves re-entry for Anna |
| Settlement per Category | US-EXP-032 | M | M | M | **High** | Additive section on already-built SETTLED page; Kai and Anna need post-trip financial accountability |
| Export Settlement as PDF | US-EXP-033 | M | M | H | **High** | Anna needs a shareable/printable record; all post-trip accounting depends on this |
| Accommodation Poll | US-TRIPS-062 | L | L | M | **Defer** | Requires US-TRIPS-060 (done), but poll state machine is a new interaction pattern; best deferred |
| Bring App Integration | US-TRIPS-055 | L | XL | L | **Defer** | Fragments the source of truth; infinite maintenance cost |
| Custom Splitting per Receipt | US-EXP-022 | M | M | L | **Defer** | Power-user only; most users never need it |
| Service Worker / Offline | US-INFRA-040 | XL | XL | M | **Defer** | High effort for SSR+HTMX; not justified until adoption grows |
| Lighthouse CI | US-INFRA-042 | M | M | — | **Low** | No user-visible UX change; purely dev tooling |
| SMS Notifications | US-IAM-051 | M | M | L | **Defer** | Requires third-party SMS gateway; low adoption payoff |
| Participant Schedule Grid | US-TRIPS-021 | M | M | M | **Consider** | Day-by-day grid of who is present; useful for planning but not in the original Iter 10 shortlist |
| Per-Day Cost View | US-EXP-011 | M | M | M | **Consider** | Budget monitoring; natural companion to category breakdown |

---

## 3. Dependency Analysis

### Stories that modify existing pages (additive)

| Story | Target Page | Change Type |
|-------|-------------|-------------|
| US-EXP-032 Settlement per Category | `/expense/{tripId}` — SETTLED state | New `<article>` section below party settlement |
| US-EXP-033 Export Settlement as PDF | `/expense/{tripId}` — SETTLED state | Download button in article header, server-side PDF response |
| US-TRIPS-061 Accommodation URL Import | `/{tripId}/accommodation` — edit/create form | New optional `<details>` URL import block in the details edit form |

### Stories that create a new flow within an existing page

| Story | Target | Dependency |
|-------|--------|------------|
| US-TRIPS-041 Recipe Import from URL | `/trips/recipes/new` — new "Import" mode | Recipe aggregate exists (Iteration 7). No blocker. |

### Dependency chain

```
S9-A (Accommodation page — DONE) → US-TRIPS-061 (URL Import into accommodation form)
Recipe aggregate (DONE) → US-TRIPS-041 (URL import into recipe create form)
Party settlement section (DONE) → US-EXP-032 (Category breakdown below it)
Party settlement + Category breakdown → US-EXP-033 (PDF export covers all sections)
```

PDF export should come last (or in parallel with EXP-032) because a complete PDF is more valuable than one with only transfers.

---

## 4. Mobile vs Desktop Analysis

### Primarily desktop benefit

| Story | Rationale |
|-------|-----------|
| US-TRIPS-041 Recipe Import | Organizer pastes URL from a recipe site, typically on desktop during meal planning. |
| US-TRIPS-061 Accommodation URL Import | Anna copies the booking confirmation URL at her desk. |
| US-EXP-032 Settlement per Category | Post-trip analytical view; Max is unlikely to use this on mobile. |
| US-EXP-033 Export as PDF | PDF is opened on desktop and sent by email. |

### Equal benefit

| Story | Rationale |
|-------|-----------|
| US-EXP-032 Category chart | Kai checks the breakdown from any device as the trip runs. |

All four recommended stories are organizer/desktop-primary, which is appropriate at this stage: the mobile experience (shopping list, accommodation read view, party settlement) was addressed in Iterations 8 and 9.

---

## 5. Recommended Iteration 10 Stories

Ranked by composite UX value (impact breadth × dependency readiness × implementation risk):

### Rank 1 — US-TRIPS-061: Accommodation URL Import (S10-A)

**Why first**: The accommodation page is now live. Anna has to manually type the accommodation name, address, website — data that is already on the booking page she has open. The `og:title`, `og:description`, and `og:image` tags are present on nearly every booking platform (Airbnb, Booking.com, Hüttendorf sites). The scope is small: a new optional URL field at the top of the accommodation edit form; on submit, the server fetches and parses Open Graph/schema.org metadata; the user reviews the pre-filled values and saves. Fallback to manual is automatic. No new page required.

### Rank 2 — US-TRIPS-041: Recipe Import from URL (S10-B)

**Why second**: The Recipe aggregate and create-form exist. Anna spends 10–15 minutes re-typing ingredients from Chefkoch every time she plans a recipe. schema.org/Recipe JSON-LD is supported by Chefkoch.de, Essen-und-Trinken.de, and most major recipe sites. The import is a new optional tab/mode on the existing recipe create page — same URL, different initial state. The implementation is a new `RecipeImportAdapter` (HTTP fetch + JSON-LD parse). Effort is medium; the pattern mirrors the accommodation URL import.

### Rank 3 — US-EXP-032: Settlement per Category (S10-C)

**Why third**: The settlement page already shows individual and party-level transfers (Iteration 9). The next natural question is "where did the money go?" — a category breakdown table is a direct answer. The data is already present (receipts have categories since Iteration 6). This is a computed view rendered on the server, no new aggregate or events needed. Small to medium effort; high perceived value in post-trip reviews.

### Rank 4 — US-EXP-033: Export Settlement as PDF (S10-D)

**Why fourth**: After Ranks 1–3 are complete, the settlement page has everything it needs to produce a complete record: individual transfers, party-level summary (Iteration 9), and category breakdown (Rank 3). A PDF download button on the SETTLED page generates the complete record. Kai and Anna need this to share with participants who don't use the app and for personal record-keeping. Effort is medium (iText or Thymeleaf-to-PDF). Should be sequenced after US-EXP-032 so the PDF includes category data.

### Not recommended for Iteration 10

| Story | Reason |
|-------|--------|
| US-TRIPS-062 Accommodation Poll | High interaction complexity; voting state machine is a new pattern in the design system; defer to Iteration 11 |
| US-EXP-022 Custom Splitting | Power-user only; defer |
| US-TRIPS-055 Bring Integration | Fragments source of truth; deferred indefinitely |
| US-INFRA-040 Offline / Service Worker | XL effort; SSR + HTMX architecture makes this complex; defer |
| US-INFRA-042 Lighthouse CI | Dev tooling; no user-visible UX change |

---

## 6. Wireframes: Accommodation URL Import (S10-A)

The accommodation URL import is an optional block that appears inside the existing accommodation details edit form. It does not require a new page.

### 6.1 — Desktop: Edit Form with URL Import Block

The "Bearbeiten" button on the accommodation details article triggers `hx-get="/{id}/accommodation/edit"`, returning the same form as before — with one addition: a collapsed `<details>` element at the top for URL import.

```
┌─── article: Unterkunft bearbeiten ───────────────────────────────┐
│                                                                   │
│  ┌─ details (initially open on new accommodation, closed on edit)┐│
│  │  <summary>Aus URL importieren (optional)</summary>            ││
│  │                                                               ││
│  │  label: Buchungs-URL oder Website                             ││
│  │  [https://www.airbnb.de/rooms/12345678                    ]   ││
│  │  small: "Unterstützt: Airbnb, Booking.com, Hüttendorf-Sites"  ││
│  │                                                               ││
│  │  [Importieren]  ← hx-post="/{id}/accommodation/import-url"   ││
│  │                    hx-target="#accommodation-import-result"   ││
│  │                    hx-swap="innerHTML"                        ││
│  │                    hx-indicator="#import-spinner"             ││
│  │                                                               ││
│  │  div id="accommodation-import-result":                        ││
│  │    (empty initially; filled by HTMX response)                 ││
│  │    ─────────────────────────────────────────                  ││
│  │    Gefunden:                                                   ││
│  │    Berghütte Sonnenhang — Almweg 3, 5570 Mauterndorf           ││
│  │    [Felder ausfüllen]  ← hx-post fills the form fields below  ││
│  └───────────────────────────────────────────────────────────────┘│
│                                                                   │
│  label: Name der Unterkunft                                       │
│  [Berghütte Sonnenhang                                        ]   │
│                                                                   │
│  label: Adresse                                                   │
│  [Almweg 3, 5570 Mauterndorf                                  ]   │
│                                                                   │
│  .grid (2 columns):                                               │
│    label: Check-in Datum      label: Check-in Uhrzeit            │
│    [2026-12-24             ]  [14:00              ]               │
│                                                                   │
│  .grid (2 columns):                                               │
│    label: Check-out Datum     label: Check-out Uhrzeit           │
│    [2026-12-31             ]  [10:00              ]               │
│                                                                   │
│  label: Website (optional)                                        │
│  [https://berghuette-sonnenhang.at                            ]   │
│                                                                   │
│  label: Gesamtpreis in EUR (optional)                             │
│  [1400.00                                                     ]   │
│                                                                   │
│  label: Notizen (optional)                                        │
│  <textarea rows="3">Schlüssel beim Gasthof abholen.</textarea>    │
│                                                                   │
│  fieldset: Ausstattung (optional)                                 │
│  [✓] Sauna  [✓] Garten  [ ] Schwimmbad  [✓] Bergblick            │
│  [✓] Gemeinschaftsküche  [ ] Einkauf in der Nähe  [ ] TV         │
│                                                                   │
│  footer:                                                          │
│  [Abbrechen]                                        [Speichern]  │
└───────────────────────────────────────────────────────────────────┘
```

### 6.2 — Import Result States

**Success state** (URL contained extractable data):

```
div id="accommodation-import-result":
┌─ role="status" ─────────────────────────────────────────────────┐
│  Gefunden: Berghütte Sonnenhang                                  │
│  Adresse: Almweg 3, 5570 Mauterndorf                            │
│  Website: https://berghuette-sonnenhang.at                       │
│                                                                  │
│  [Felder ausfüllen]  ← secondary button; JS-fills form fields   │
│                          + scrolls to Name field                 │
└──────────────────────────────────────────────────────────────────┘
```

**Failure state** (URL had no parseable metadata):

```
div id="accommodation-import-result":
┌─ role="alert" aria-live="polite" ──────────────────────────────┐
│  Aus dieser URL konnten keine Daten gelesen werden.             │
│  Bitte trage die Details manuell ein.                           │
└─────────────────────────────────────────────────────────────────┘
```

**Loading state** (during fetch):

```
button[aria-busy="true"]:  Importiert ...   (PicoCSS aria-busy spinner)
```

### 6.3 — Mobile: URL Import Block (collapsed by default)

On mobile, the `<details>` is closed by default to keep the form scannable. The user taps the summary to expand.

```
╔═══════════════════════════════╗
║  h1: Unterkunft bearbeiten    ║
╠═══════════════════════════════╣
║  ┌─ details ────────────────┐ ║
║  │  ▶ Aus URL importieren   │ ║
║  │    (optional)            │ ║
║  └──────────────────────────┘ ║
║                               ║
║  label: Name der Unterkunft   ║
║  [Berghütte Sonnenhang    ]   ║
║                               ║
║  label: Adresse               ║
║  [Almweg 3, 5570 Mautern.]   ║
║                               ║
║  label: Check-in Datum        ║
║  [2026-12-24             ]   ║
║                               ║
║  label: Check-in Uhrzeit      ║
║  [14:00                  ]   ║
║                               ║
║  label: Check-out Datum       ║
║  [2026-12-31             ]   ║
║                               ║
║  label: Check-out Uhrzeit     ║
║  [10:00                  ]   ║
║                               ║
║  label: Website (optional)    ║
║  [https://...            ]   ║
║                               ║
║  [Abbrechen]   [Speichern]   ║
╚═══════════════════════════════╝
```

### 6.4 — HTMX Interaction Flow (Accommodation URL Import)

```
User pastes URL → clicks [Importieren]
  ↓
hx-post="/{tripId}/accommodation/import-url"
  body: url=https://...
  hx-indicator="#import-spinner" (button shows aria-busy)
  hx-target="#accommodation-import-result"
  hx-swap="innerHTML"
  ↓
Server: AccommodationImportAdapter fetches URL, parses og:title / og:description / og:site_name / schema.org/LodgingBusiness
  → returns fragment: import-result partial (success or failure)
  ↓
Fragment injected into #accommodation-import-result
  ↓
User clicks [Felder ausfüllen]
  → small inline JS: fills name/address/website input values from data attributes on the button
  → no additional server round-trip needed
  → user reviews and edits, then clicks [Speichern]
```

The "Felder ausfüllen" approach keeps the import result and the field filling separate. The server returns the parsed data embedded as `data-name`, `data-address`, `data-website` attributes on the button. A minimal inline script (or `hx-vals` / `hx-on`) writes the values into the form inputs. This avoids a second server round-trip and keeps the form editable by the user before saving.

---

## 7. Wireframes: Recipe Import from URL (S10-B)

Recipe import adds a second mode to the existing recipe create page. The user can either enter manually (default) or switch to URL import mode. Both modes share the same page — `GET /trips/recipes/new` renders the manual form; `GET /trips/recipes/new?mode=import` renders the import flow.

### 7.1 — Desktop: Recipe Create Page with Import Mode

The page header gains a mode toggle. In manual mode (default), the existing create form is shown. In import mode, a URL field is shown first, followed by the pre-filled (and editable) recipe fields.

```
╔══════════════════════════════════════════════════════════════════╗
║  nav: Travelmate  Reisen  Rezepte  Abmelden                      ║
╠══════════════════════════════════════════════════════════════════╣
║  hgroup:                                                         ║
║    h1: Neues Rezept                                              ║
║    p: ↩ Rezepte                                                  ║
╠══════════════════════════════════════════════════════════════════╣
║                                                                  ║
║  ┌─ article ────────────────────────────────────────────────┐    ║
║  │  nav (mode toggle):                                       │    ║
║  │    [Manuell eingeben]  [Aus URL importieren ▼ aktiv]      │    ║
║  │                                                           │    ║
║  │  ─── Import-Block ─────────────────────────────────────── │    ║
║  │                                                           │    ║
║  │  label: Rezept-URL                                        │    ║
║  │  [https://www.chefkoch.de/rezepte/123456/...          ]   │    ║
║  │  small: "Unterstützt: Chefkoch.de, Essen-und-Trinken.de,  │    ║
║  │          BBC Good Food, und andere Seiten mit             │    ║
║  │          strukturierten Rezeptdaten (schema.org/Recipe)"  │    ║
║  │                                                           │    ║
║  │  [Rezept importieren]  aria-busy while loading            │    ║
║  │  hx-post="/trips/recipes/import-url"                      │    ║
║  │  hx-target="#recipe-import-result"                        │    ║
║  │  hx-swap="outerHTML"                                      │    ║
║  │                                                           │    ║
║  │  div id="recipe-import-result" (empty initially)          │    ║
║  └───────────────────────────────────────────────────────────┘    ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝
```

After a successful import, the `#recipe-import-result` div is replaced with the pre-filled create form:

```
╔══════════════════════════════════════════════════════════════════╗
║  ┌─ article ────────────────────────────────────────────────┐    ║
║  │  nav:  [Manuell eingeben]  [Aus URL importieren]          │    ║
║  │                                                           │    ║
║  │  role="status":                                           │    ║
║  │    Rezept gefunden: "Spaghetti Carbonara (Chefkoch)"       │    ║
║  │    Überprüfe und passe die Felder an:                     │    ║
║  │                                                           │    ║
║  │  form hx-post="/trips/recipes"                            │    ║
║  │       hx-target="body" hx-swap="outerHTML":               │    ║
║  │                                                           │    ║
║  │  label: Rezeptname                                        │    ║
║  │  [Spaghetti Carbonara                                 ]   │    ║
║  │                                                           │    ║
║  │  label: Beschreibung (optional)                           │    ║
║  │  <textarea rows="2">Ein klassisches römisches Pastagericht.</textarea>│
║  │                                                           │    ║
║  │  label: Portionen                                         │    ║
║  │  [4                                                   ]   │    ║
║  │                                                           │    ║
║  │  ─── Zutaten ──────────────────────────────────────────── │    ║
║  │                                                           │    ║
║  │  ┌─ table ────────────────────────────────────────────┐   │    ║
║  │  │ # │ Zutat            │ Menge │ Einheit │ [Löschen]  │   │    ║
║  │  ├──────────────────────────────────────────────────── │   │    ║
║  │  │ 1 │ [Spaghetti     ] │ [400] │ [g     ] │ [x]       │   │    ║
║  │  │ 2 │ [Guanciale     ] │ [150] │ [g     ] │ [x]       │   │    ║
║  │  │ 3 │ [Pecorino      ] │ [100] │ [g     ] │ [x]       │   │    ║
║  │  │ 4 │ [Eier          ] │ [4  ] │ [Stk.  ] │ [x]       │   │    ║
║  │  │ 5 │ [Pfeffer       ] │ [1  ] │ [TL    ] │ [x]       │   │    ║
║  │  └────────────────────────────────────────────────────┘   │    ║
║  │                                                           │    ║
║  │  [+ Zutat hinzufügen]                                     │    ║
║  │                                                           │    ║
║  │  footer:                                                  │    ║
║  │  [Abbrechen]                             [Rezept speichern] │  ║
║  └───────────────────────────────────────────────────────────┘    ║
╚══════════════════════════════════════════════════════════════════╝
```

### 7.2 — Import Failure State

```
div id="recipe-import-result":
┌─ role="alert" ──────────────────────────────────────────────────┐
│  Aus dieser URL konnten keine Rezeptdaten gelesen werden.        │
│  Mögliche Gründe:                                               │
│  - Die Seite enthält keine strukturierten Rezeptdaten            │
│  - Die Seite ist nicht öffentlich erreichbar                    │
│                                                                  │
│  [Manuell eingeben]  ← switches to manual mode (link to ?mode=manual) │
└──────────────────────────────────────────────────────────────────┘
```

### 7.3 — Mobile: Recipe Import (compact flow)

```
╔═══════════════════════════════╗
║  h1: Neues Rezept             ║
║  ↩ Rezepte                    ║
╠═══════════════════════════════╣
║  ┌─ article ────────────────┐ ║
║  │  [Manuell] [▼ Importieren]│ ║
║  │                           │ ║
║  │  label: Rezept-URL        │ ║
║  │  [https://chefkoch.de/...] │ ║
║  │  small: Chefkoch, BBC     │ ║
║  │  Good Food, u.a.          │ ║
║  │                           │ ║
║  │  [Rezept importieren]     │ ║
║  └──────────────────────────┘ ║
║                               ║
║  (after import — same form   ║
║   as desktop, single-column) ║
╚═══════════════════════════════╝
```

On mobile, the ingredient table renders as a stacked list (one row = one fieldset-row of inputs, stacked vertically). This is the same pattern as the existing manual recipe create form.

### 7.4 — HTMX Interaction Flow (Recipe Import)

```
User pastes URL → clicks [Rezept importieren]
  ↓
hx-post="/trips/recipes/import-url"
  body: url=https://...
  hx-target="#recipe-import-result"
  hx-swap="outerHTML"
  ↓
Server: RecipeImportAdapter fetches URL, parses JSON-LD for @type="Recipe"
  Extracts: name, description, recipeYield, recipeIngredient[]
  Parses ingredient strings: "400g Spaghetti" → {name:"Spaghetti", quantity:400, unit:"g"}
  → returns fragment: pre-filled recipe form (as #recipe-import-result replacement)
  OR: error fragment if parsing fails
  ↓
User reviews and edits all fields (name, description, servings, each ingredient)
User clicks [Rezept speichern]
  → standard hx-post="/trips/recipes"
  → redirect to recipe list on success
```

The key design decision: after a successful import, the server returns the complete pre-filled form (not just the data). This means the user is always editing a server-rendered form — consistent with the no-SPA constraint and progressive enhancement. If JS is disabled, the import button simply submits as a full page POST and the returned page contains the pre-filled form.

---

## 8. Wireframes: Settlement per Category (S10-C)

The category breakdown is a new `<article>` section on the `/expense/{tripId}` page, SETTLED state only. It appears below the party-level settlement section (added in Iteration 9).

### 8.1 — Desktop: Category Breakdown Section

```
╔══════════════════════════════════════════════════════════════════╗
║  ...                                                             ║
║  ┌─── article: Abrechnung nach Reisepartei ───────────────────┐  ║
║  │  (Iteration 9 content — party balance table + transfers)    │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                  ║
║  ┌─── article: Ausgaben nach Kategorie ──────────────────────┐   ║
║  │  header:                                                   │   ║
║  │    h2: Ausgaben nach Kategorie                              │   ║
║  │    small: "Alle genehmigten Belege · Gesamt: 880,00 EUR"   │   ║
║  │                                                            │   ║
║  │  ┌─ table ──────────────────────────────────────────────┐  │   ║
║  │  │  Kategorie          Betrag       Anteil am Gesamt    │  │   ║
║  │  ├──────────────────────────────────────────────────────┤  │   ║
║  │  │  Unterkunft         420,00 EUR   47,7%               │  │   ║
║  │  │  Lebensmittel       235,00 EUR   26,7%               │  │   ║
║  │  │  Restaurant          80,00 EUR    9,1%               │  │   ║
║  │  │  Aktivität           95,00 EUR   10,8%               │  │   ║
║  │  │  Transport           50,00 EUR    5,7%               │  │   ║
║  │  │  Sonstiges            0,00 EUR    0,0%               │  │   ║
║  │  ├──────────────────────────────────────────────────────┤  │   ║
║  │  │  Gesamt             880,00 EUR  100,0%               │  │   ║
║  │  └──────────────────────────────────────────────────────┘  │   ║
║  │                                                            │   ║
║  │  Visuelle Aufschlüsselung (progress bars, one per row):   │   ║
║  │                                                            │   ║
║  │  Unterkunft    ████████████████████████████░░░░░  47,7%  │   ║
║  │  Lebensmittel  ██████████████░░░░░░░░░░░░░░░░░░  26,7%  │   ║
║  │  Restaurant    ████░░░░░░░░░░░░░░░░░░░░░░░░░░░░   9,1%  │   ║
║  │  Aktivität     █████░░░░░░░░░░░░░░░░░░░░░░░░░░░  10,8%  │   ║
║  │  Transport     ██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░   5,7%  │   ║
║  └────────────────────────────────────────────────────────────┘   ║
╚══════════════════════════════════════════════════════════════════╝
```

The `<progress>` bars are native HTML `<progress value="420" max="880">` elements, styled by PicoCSS. Each row in the visual breakdown is a `<dl>` or `<div>` with `<dt>` (category name) and `<dd>` containing the `<progress>` and the percentage. This is accessible and works without JavaScript.

Categories not present in the expense ledger (no approved receipts) are shown with `0,00 EUR` and a `0%` bar. They are shown because the user should see that the category was considered.

### 8.2 — Mobile: Category Breakdown (card list)

On mobile, the table collapses to a list of category cards. The progress bar is retained as it communicates proportion effectively without needing the number column.

```
╔═══════════════════════════════╗
║  h2: Ausgaben je Kategorie    ║
║  Gesamt: 880,00 EUR           ║
║                               ║
║  ┌─ Unterkunft ─────────────┐ ║
║  │  420,00 EUR · 47,7%      │ ║
║  │  ████████████████░░░░░░  │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ Lebensmittel ───────────┐ ║
║  │  235,00 EUR · 26,7%      │ ║
║  │  █████████░░░░░░░░░░░░░  │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ Restaurant ─────────────┐ ║
║  │  80,00 EUR · 9,1%        │ ║
║  │  ████░░░░░░░░░░░░░░░░░░  │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ Aktivität ──────────────┐ ║
║  │  95,00 EUR · 10,8%       │ ║
║  │  █████░░░░░░░░░░░░░░░░░  │ ║
║  └──────────────────────────┘ ║
║                               ║
║  ┌─ Transport ──────────────┐ ║
║  │  50,00 EUR · 5,7%        │ ║
║  │  ██░░░░░░░░░░░░░░░░░░░░  │ ║
║  └──────────────────────────┘ ║
╚═══════════════════════════════╝
```

### 8.3 — Implementation Notes

The category breakdown is computed entirely server-side from the approved receipts associated with the Expense. The expense SCS already has `ExpenseCategory` on each `Receipt` (added in Iteration 6). The controller groups receipts by category, sums amounts, computes percentage of total, and passes the list to the Thymeleaf fragment. No new aggregate, event, or domain logic is needed.

The category breakdown is **only shown in SETTLED state**, consistent with the party settlement view. In OPEN state the data exists but showing it would be premature (pending receipts distort the picture).

---

## 9. Wireframes: Export Settlement as PDF (S10-D)

The PDF export is a server-side generated document. The trigger is a download button on the SETTLED expense page. No dialog or configuration is required — the PDF is generated deterministically from the settled expense data.

### 9.1 — Entry Point: Download Button on Settled Expense Page

```
┌─── article: Abrechnung ──────────────────────────────────────────┐
│  header:                                                          │
│    h2: Abrechnung                                    [PDF laden]  │
│    small: "Abgerechnet am 20.12.2026"                             │
│                                                                   │
│    [PDF laden] is an <a> link, not a button:                      │
│    <a href="/{tripId}/expense/export/pdf"                         │
│       role="button"                                               │
│       class="secondary outline"                                   │
│       download="abrechnung-alpenurlaub-2026.pdf">                 │
│      PDF laden                                                    │
│    </a>                                                           │
└───────────────────────────────────────────────────────────────────┘
```

Using `<a href="..." download>` means the browser handles the file download natively. No HTMX is required. The server responds to `GET /{tripId}/expense/export/pdf` with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename="abrechnung-{tripName}-{year}.pdf"`.

### 9.2 — PDF Document Structure

The PDF is a clean single-page-per-section document. It mirrors the page structure the user already knows from the web app.

```
┌──────────────────────────────────────────────────────────────────┐
│  TRAVELMATE — REISEABRECHNUNG                                     │
│  Alpen-Urlaub 2026 · 24.12.2026 – 31.12.2026                    │
│  Erstellt: 20.12.2026                                             │
│                                                                   │
│  ══════════════════════════════════════════════════════          │
│  1. ZUSAMMENFASSUNG                                               │
│                                                                   │
│  Gesamtausgaben:     880,00 EUR                                   │
│  Teilnehmer:         11 Personen (3 Reiseparteien)               │
│  Belege gesamt:      14 (alle genehmigt)                          │
│                                                                   │
│  ══════════════════════════════════════════════════════          │
│  2. AUSGLEICHSÜBERWEISUNGEN (NACH REISEPARTEI)                    │
│                                                                   │
│  Familie Schmidt  →  Familie Mueller      456,00 EUR              │
│  Familie Bauer    →  Familie Mueller       20,00 EUR              │
│                                                                   │
│  ══════════════════════════════════════════════════════          │
│  3. SALDEN NACH REISEPARTEI                                       │
│                                                                   │
│  Reisepartei         Bezahlt      Anteil        Saldo            │
│  Familie Mueller     456,00 EUR   380,00 EUR   +76,00 EUR        │
│  Familie Schmidt     180,00 EUR   636,00 EUR  -456,00 EUR        │
│  Familie Bauer       244,00 EUR   264,00 EUR   -20,00 EUR        │
│                                                                   │
│  ══════════════════════════════════════════════════════          │
│  4. AUSGABEN NACH KATEGORIE                                       │
│                                                                   │
│  Unterkunft          420,00 EUR   47,7%                           │
│  Lebensmittel        235,00 EUR   26,7%                           │
│  Restaurant           80,00 EUR    9,1%                           │
│  Aktivität            95,00 EUR   10,8%                           │
│  Transport            50,00 EUR    5,7%                           │
│                                                                   │
│  ══════════════════════════════════════════════════════          │
│  5. ALLE BELEGE                                                   │
│                                                                   │
│  Datum      Beschreibung       Eingereicht von    Betrag   Kat.  │
│  24.12.2026 Mautgebühr         Anna Mueller       45,00    Trans │
│  25.12.2026 Lebensmittel       Max Schmidt       120,00    Lebm  │
│  ...                                                              │
│                                                                   │
│  ── Erstellt mit Travelmate ─────────────────────────────────── │
└──────────────────────────────────────────────────────────────────┘
```

### 9.3 — Technical Implementation Note

PDF generation in the JVM ecosystem has three practical options in the context of this project:

1. **Flying Saucer (XHTML to PDF)**: Render a simplified Thymeleaf template to HTML, convert with Flying Saucer (`org.xhtmlrenderer:flying-saucer-pdf`). Pros: reuses existing template skills, simple CSS-driven layout. Cons: requires XHTML-compliant markup, limited CSS support.

2. **iText 7 (community)**: Programmatic PDF construction. Pros: full control, table support, no HTML rendering dependency. Cons: verbose code, license constraints for iText 8 (AGPL).

3. **Apache PDFBox**: Low-level drawing API. Pros: Apache license, no external dependencies. Cons: very verbose for formatted output.

**Recommended**: Flying Saucer. It allows a dedicated `expense/export/pdf.html` Thymeleaf template that mirrors the PDF structure above. The controller renders the template to a String, converts to PDF bytes, and returns as a byte-array response. PicoCSS is NOT included in the PDF template — a minimal, print-targeted CSS is used instead.

### 9.4 — Mobile: Download Trigger

On mobile, the `[PDF laden]` button appears in the page header. Tapping it triggers the browser's native download flow. The PDF opens in the device's PDF viewer. No special mobile handling is needed.

```
╔═══════════════════════════════╗
║  h2: Abrechnung               ║
║  Abgerechnet 20.12.2026       ║
║  [PDF laden]                  ║
╠═══════════════════════════════╣
║  ... (transfer sentences)     ║
╚═══════════════════════════════╝
```

---

## 10. User Journeys

### 10.1 — Organizer Imports Accommodation from URL (S10-A)

**Persona**: Anna (Organizer, desktop)
**Goal**: Populate the accommodation page with data from her Airbnb booking confirmation, without retyping.
**Trigger**: Anna opens the accommodation page; it shows basic data from the manual entry; she realises she can update it with the booking link.

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Navigate | Clicks "Bearbeiten" on accommodation article | Returns edit form with `<details>` URL import block | `/{tripId}/accommodation/edit` partial | Neutral | Edit form has many fields | Import block is collapsed — not overwhelming |
| 2. Discover import | Spots "Aus URL importieren" details block | — | Edit form | Curious | May not notice it if collapsed | Auto-expand on new accommodation (no pre-existing data) |
| 3. Paste URL | Opens details, pastes Airbnb URL | Shows URL field, Import button | `<details>` block | Hopeful | Has to copy URL from another tab | URL field accepts paste; no format validation needed upfront |
| 4. Import | Clicks [Importieren] | Aria-busy spinner; server fetches + parses og:title, og:description | Import block | Waiting | Fetch may take 1–3s | Progress indicator is immediate (aria-busy) |
| 5. Review | Sees success status with name + address | "Felder ausfüllen" button | Import result div | Relieved | Name may have the host name prefix (e.g., "Room in…") | Editable fields: user can fix before saving |
| 6. Fill + review | Clicks [Felder ausfüllen]; reviews all pre-filled values | JS fills name, address, website inputs | Form fields | Satisfied | Check-in/out times NOT imported (no structured data) | Time fields remain empty — user fills them |
| 7. Save | Clicks [Speichern] | Returns to read view `<article>` | Accommodation article | Done | — | — |

**Key Metrics**:
| Metric | Target |
|--------|--------|
| Import success rate (URL with og:title) | >70% of all booking URLs tested |
| Time to populate accommodation vs manual | 60% faster |
| Form abandonment after failed import | <30% (fallback to manual is clear) |

---

### 10.2 — Organizer Imports Recipe from URL (S10-B)

**Persona**: Anna (Organizer, desktop)
**Goal**: Add a Chefkoch.de recipe to the recipe library without manual re-entry.
**Trigger**: Anna has the recipe page open in another tab while planning the meal plan.

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Navigate | Opens "Neues Rezept" from recipe list | Renders recipe create page with mode toggle | `/trips/recipes/new` | Neutral | Default is manual mode | Auto-switch to import mode if URL in clipboard (JS enhancement, optional) |
| 2. Switch mode | Clicks [Aus URL importieren] | Page re-renders with import block active | Same page (`?mode=import`) | Intentional | Extra click | Mode state survives page refresh (query param) |
| 3. Paste URL | Pastes Chefkoch URL | Shows URL field | Import form | Focused | Chefkoch URLs can be long | URL field is wide; paste works |
| 4. Import | Clicks [Rezept importieren] | Fetches JSON-LD, parses ingredients | Server fetch | Waiting | Ingredient parsing is heuristic (quantity + unit in string) | Show what was extracted transparently |
| 5. Review | Sees pre-filled form with name, servings, ingredient table | — | Pre-filled create form | Surprised (positively) | Some ingredients may parse incorrectly ("1 Prise Salz" → quantity=1, unit="Prise") | All fields editable; user can fix |
| 6. Edit | Corrects wrong ingredient quantities or units | Live form editing | Ingredient table | Engaged | Adding missing ingredients requires clicking "+ Zutat" | Standard add-row pattern |
| 7. Save | Clicks [Rezept speichern] | Saves recipe, redirects to list | Recipe list | Satisfied | — | — |

**Key Metrics**:
| Metric | Target |
|--------|--------|
| Chefkoch.de import success rate | >90% (schema.org/Recipe is well-supported) |
| Average ingredients parsed correctly | >80% |
| Recipe save rate after import vs manual | Higher (pre-filled form reduces abandonment) |

---

### 10.3 — Post-Trip Category Review (S10-C)

**Persona**: Kai (Co-Organizer, desktop and mobile)
**Goal**: Review where the group's money went, cross-check against pre-trip budget.
**Trigger**: Trip is COMPLETED, expense is SETTLED. Kai opens the expense page.

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Open expense | Navigates to `/expense/{tripId}` | Shows SETTLED expense page: receipt list, individual transfers, party transfers (Iter 9) | Expense page | Reflective | Lots of data on one page | Category section at bottom gives the "big picture" view |
| 2. Scroll to breakdown | Scrolls past transfer table to category section | Category table + progress bars visible | `#category-breakdown` article | Interested | "Unterkunft" dominates — was that expected? | Progress bars give instant proportion signal |
| 3. Cross-check | Compares category totals against mental budget | — | — | Analytical | No budget was pre-defined in the app | Future: optional budget targets per category |
| 4. Share | Copies total from category table, pastes into WhatsApp | — | External | Done | PDF export (S10-D) will make this easier | — |

---

### 10.4 — Organizer Exports PDF and Shares (S10-D)

**Persona**: Anna (Organizer, desktop)
**Goal**: Send a settlement summary to Kai who doesn't check the app after the trip.
**Trigger**: Trip COMPLETED, expense SETTLED. Anna wants to close the trip financially.

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Settle | (Expense is already settled — Iteration 9 covered this) | — | — | — | — | — |
| 2. Download | Clicks [PDF laden] in expense article header | Browser triggers PDF download; no page navigation | Download link | Satisfied | PDF generation may take 1–2 seconds | No spinner needed — browser shows native download progress |
| 3. Review | Opens PDF in Preview / Acrobat | Sees: summary, transfers, party balances, category breakdown, full receipt list | PDF viewer | Confident | PDF layout may differ from web layout | Mirror familiar structure; no surprises |
| 4. Share | Attaches PDF to email / WhatsApp | — | External | Done | — | PDF filename includes trip name + year for easy identification |

---

## 11. Component Specifications

### 11.1 — AccommodationUrlImportBlock

**Page**: `/{tripId}/accommodation/edit` (part of the edit form article)
**PicoCSS Base**: `<details>` + `<summary>` + `<form>`

#### States

| State | Visual | Trigger |
|-------|--------|---------|
| Collapsed | `<details>` closed; just the summary line visible | Default on edit; open on new accommodation |
| Expanded | URL input + [Importieren] button visible | User clicks summary / auto-opens on new |
| Loading | Button `aria-busy="true"`, spinner on button text | hx-indicator fires |
| Success | Injected success fragment: name, address, [Felder ausfüllen] button | HTMX response |
| Failure | Injected error fragment with role="alert" | HTMX response |

#### HTMX Interactions

- `hx-post="/{tripId}/accommodation/import-url"` — body: `url=...`
- `hx-target="#accommodation-import-result"` — replaces content of the result div
- `hx-swap="innerHTML"`
- `hx-indicator="#import-spinner"` — targets the button itself (aria-busy pattern)

#### Accessibility

- `<details>` / `<summary>` are keyboard-accessible natively
- Error fragment uses `role="alert"` so screen readers announce it without focus management
- Success fragment uses `role="status"` (polite; does not interrupt)
- The [Felder ausfüllen] button uses `aria-label="Importierte Daten in das Formular übernehmen"`

---

### 11.2 — RecipeImportBlock

**Page**: `/trips/recipes/new?mode=import`
**PicoCSS Base**: `<article>` with mode-toggle `<nav>` + URL form + pre-filled create form (after import)

#### States

| State | Visual | Trigger |
|-------|--------|---------|
| Manual mode | Standard recipe create form (no import block) | Default / `?mode=manual` |
| Import mode (empty) | URL input + [Rezept importieren] button | `?mode=import` / mode toggle click |
| Import loading | Button aria-busy | hx-indicator |
| Import success | Full pre-filled recipe form replaces #recipe-import-result | HTMX response (outerHTML swap) |
| Import failure | Error fragment with role="alert" + [Manuell eingeben] link | HTMX response |

#### HTMX Interactions

- `hx-post="/trips/recipes/import-url"` — body: `url=...`
- `hx-target="#recipe-import-result"` — replaces the import result div (initially empty)
- `hx-swap="outerHTML"` — the success response replaces the entire container with the pre-filled form

#### Accessibility

- Mode toggle: `<nav aria-label="Eingabemodus">` with `aria-current="page"` on the active mode
- Error fragment: `role="alert"` for immediate screen reader announcement
- Success: `role="status"` above the pre-filled form to announce what was found

---

### 11.3 — CategoryBreakdownSection

**Page**: `/expense/{tripId}` — SETTLED state only
**PicoCSS Base**: `<article>` with `<table>` (desktop) + `<article>` list (mobile)

#### States

| State | Visual | Trigger |
|-------|--------|---------|
| SETTLED with categories | Table + progress bars | Always when SETTLED |
| SETTLED, no categories | Explanatory `<p>` note: "Belege wurden ohne Kategorie eingereicht." | All receipts have category=OTHER or no category |
| OPEN | Not rendered | Server-side conditional |

#### Layout

- **Desktop**: Full-width `<table>` with four columns (Kategorie, Betrag, Anteil am Gesamt, visual `<progress>`), followed by a totals row. The progress column uses `<progress value="420" max="880">` with PicoCSS default styling.
- **Mobile**: `<article>` per category (same card pattern as party settlement). Category name as `<h3>`, amount + percentage as `<dl>`, `<progress>` bar below.

#### Accessibility

- `<table>` has `<caption>` for screen readers: "Ausgaben nach Kategorie"
- `<progress>` bars have `aria-label="{category}: {amount} EUR ({percent}%)"` to announce both value and context
- Category enum values must be rendered via i18n keys — never as raw code strings

---

### 11.4 — PdfExportLink

**Page**: `/expense/{tripId}` — SETTLED state
**PicoCSS Base**: `<a role="button" class="secondary outline">`

#### States

| State | Visual | Trigger |
|-------|--------|---------|
| Default | "PDF laden" button in expense article header | Always shown in SETTLED state |
| Clicked | Browser native download indicator | Browser-native |

#### Implementation

```html
<a th:href="@{/{tripId}/expense/export/pdf(tripId=${tripId})}"
   role="button"
   class="secondary outline"
   th:download="|abrechnung-${tripName}-${tripYear}.pdf|"
   th:text="#{expense.export.pdf}">
    PDF laden
</a>
```

No HTMX needed. The `download` attribute triggers a browser download. The `<a>` with `role="button"` ensures PicoCSS button styling while maintaining correct semantics (it IS a link — it navigates to a resource).

The controller at `GET /{tripId}/expense/export/pdf` returns:
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="abrechnung-{tripName}-{year}.pdf"`

---

## 12. HTMX Interaction Map — Iteration 10

```
Accommodation URL Import
  POST /{tripId}/accommodation/import-url
    ← form: url field in details block
    → #accommodation-import-result innerHTML
       [success] → name/address/website in role="status" + [Felder ausfüllen]
       [failure] → role="alert" with manual fallback link

Recipe Import
  POST /trips/recipes/import-url
    ← form: url field in #recipe-import-result container
    → #recipe-import-result outerHTML
       [success] → pre-filled recipe create form (complete replacement)
       [failure] → role="alert" with [Manuell eingeben] link

Category Breakdown
  (no HTMX — server-rendered, static on page load in SETTLED state)

PDF Export
  (no HTMX — plain <a download> link, handled by browser natively)
```

The two import flows use different swap strategies for a deliberate reason:

- **Accommodation import** uses `innerHTML` because the result div is inside a stable edit form. The form fields below remain intact while only the result div updates.
- **Recipe import** uses `outerHTML` because the entire container is replaced with the pre-filled create form — the import block disappears and is replaced by the actual editing experience.

---

## 13. i18n Key Table

### Trips SCS — Accommodation URL Import (S10-A)

| Key | DE | EN |
|-----|----|----|
| `accommodation.import.toggle` | Aus URL importieren (optional) | Import from URL (optional) |
| `accommodation.import.url.label` | Buchungs-URL oder Website | Booking URL or website |
| `accommodation.import.url.hint` | Unterstützt: Airbnb, Booking.com, und andere Seiten | Supported: Airbnb, Booking.com, and other booking sites |
| `accommodation.import.button` | Importieren | Import |
| `accommodation.import.loading` | Importiert ... | Importing ... |
| `accommodation.import.success.label` | Gefunden | Found |
| `accommodation.import.success.fill` | Felder ausfüllen | Fill in fields |
| `accommodation.import.failure` | Aus dieser URL konnten keine Daten gelesen werden. Bitte trage die Details manuell ein. | Could not extract data from this URL. Please enter the details manually. |

### Trips SCS — Recipe URL Import (S10-B)

| Key | DE | EN |
|-----|----|----|
| `recipe.mode.manual` | Manuell eingeben | Enter manually |
| `recipe.mode.import` | Aus URL importieren | Import from URL |
| `recipe.import.url.label` | Rezept-URL | Recipe URL |
| `recipe.import.url.hint` | Unterstützt: Chefkoch.de, Essen-und-Trinken.de, BBC Good Food, und andere Seiten mit strukturierten Rezeptdaten (schema.org/Recipe) | Supported: Chefkoch.de, Essen-und-Trinken.de, BBC Good Food, and other sites with structured recipe data (schema.org/Recipe) |
| `recipe.import.button` | Rezept importieren | Import recipe |
| `recipe.import.loading` | Wird geladen ... | Loading ... |
| `recipe.import.found` | Rezept gefunden: "{0}" — Überprüfe und passe die Felder an: | Recipe found: "{0}" — Review and adjust the fields: |
| `recipe.import.failure` | Aus dieser URL konnten keine Rezeptdaten gelesen werden. | Could not extract recipe data from this URL. |
| `recipe.import.failure.hint` | Mögliche Gründe: Seite enthält keine strukturierten Rezeptdaten, oder die Seite ist nicht öffentlich erreichbar. | Possible reasons: the page contains no structured recipe data, or the page is not publicly accessible. |
| `recipe.import.failure.manual` | Manuell eingeben | Enter manually |

### Expense SCS — Settlement per Category (S10-C)

| Key | DE | EN |
|-----|----|----|
| `expense.category.title` | Ausgaben nach Kategorie | Expenses by category |
| `expense.category.total` | Alle genehmigten Belege · Gesamt: {0} EUR | All approved receipts · Total: {0} EUR |
| `expense.category.category` | Kategorie | Category |
| `expense.category.amount` | Betrag | Amount |
| `expense.category.share` | Anteil am Gesamt | Share of total |
| `expense.category.totalRow` | Gesamt | Total |
| `expense.category.noData` | Belege wurden ohne Kategorie eingereicht. | Receipts were submitted without a category. |
| `expense.category.ACCOMMODATION` | Unterkunft | Accommodation |
| `expense.category.GROCERIES` | Lebensmittel | Groceries |
| `expense.category.RESTAURANT` | Restaurant | Restaurant |
| `expense.category.ACTIVITY` | Aktivität | Activity |
| `expense.category.TRANSPORT` | Transport | Transport |
| `expense.category.OTHER` | Sonstiges | Other |

### Expense SCS — PDF Export (S10-D)

| Key | DE | EN |
|-----|----|----|
| `expense.export.pdf` | PDF laden | Download PDF |
| `expense.export.pdf.title` | REISEABRECHNUNG | TRAVEL SETTLEMENT |
| `expense.export.pdf.generated` | Erstellt am {0} mit Travelmate | Generated on {0} with Travelmate |
| `expense.export.pdf.section.summary` | ZUSAMMENFASSUNG | SUMMARY |
| `expense.export.pdf.section.transfers` | AUSGLEICHSÜBERWEISUNGEN | SETTLEMENT TRANSFERS |
| `expense.export.pdf.section.partyBalance` | SALDEN NACH REISEPARTEI | BALANCES BY TRAVEL PARTY |
| `expense.export.pdf.section.categories` | AUSGABEN NACH KATEGORIE | EXPENSES BY CATEGORY |
| `expense.export.pdf.section.receipts` | ALLE BELEGE | ALL RECEIPTS |
| `expense.export.pdf.total` | Gesamtausgaben | Total expenses |
| `expense.export.pdf.participants` | Teilnehmer | Participants |
| `expense.export.pdf.receipts.count` | Belege gesamt | Total receipts |

---

## 14. Summary Table

| Rank | Story | ID | Size | Personas | Primary Device | Page Surface | Dependency |
|------|-------|----|------|----------|----------------|--------------|------------|
| 1 | Accommodation URL Import | US-TRIPS-061 | M | Anna H | Desktop | Accommodation edit form | S9-A (done) |
| 2 | Recipe Import from URL | US-TRIPS-041 | L | Anna M | Desktop | Recipe create page | Recipe aggregate (done) |
| 3 | Settlement per Category | US-EXP-032 | M | Kai H, Anna M | Desktop + Mobile | SETTLED expense page | Categories (done since Iter 6) |
| 4 | Export Settlement as PDF | US-EXP-033 | M | Anna H, Kai H | Desktop | SETTLED expense page | EXP-032 recommended first |

Stories deferred: US-TRIPS-062 (Poll), US-EXP-022 (Custom Split), US-TRIPS-055 (Bring), US-INFRA-040 (Offline), US-INFRA-042 (Lighthouse CI).

---

## 15. Design Documents to Create (next steps)

Before implementation begins, the following component specification documents should be produced:

1. `docs/design/components/import-url-iteration10.md` — full component spec for `AccommodationUrlImportBlock` and `RecipeImportBlock`; fallback states; server-side adapter contract; i18n keys
2. `docs/design/components/expense-iteration10.md` — full component spec for `CategoryBreakdownSection` and `PdfExportLink`; PDF template structure; Flying Saucer implementation notes
3. `docs/design/journeys/import-url-iteration10-flows.md` — detailed organizer journeys for both URL import flows (happy path + failure recovery)
