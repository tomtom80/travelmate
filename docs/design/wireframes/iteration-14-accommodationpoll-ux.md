# Iteration 14 — Accommodation Poll UX Redesign Specification

**Version**: 2026-03-31
**Author**: UX Designer Agent
**Scope**: Full redesign of `accommodationpoll/create.html` and `accommodationpoll/overview.html`, including domain model constraint for amenities, booking workflow, vote visualisation, and address display.

---

## 1. Current UX Problems — Root Cause Analysis

### 1.1 Create Form: Layout Overflow

The room row template uses `display:flex` with four `<label>` elements side by side. PicoCSS 2 styles `<label>` as `display:block; width:100%` by default. Four `width:100%` elements in a flex row each try to claim full container width, producing horizontal overflow at every viewport size.

The fix is to stop using `<label>` as the outer flex child. The label text and input should stay together, but the flex container must be a plain `<div>` or a CSS grid row, not a wrapper `<label>`.

### 1.2 Create Form: No Visual Hierarchy

All fields (candidate header, optional URL, rooms) are the same visual weight. When two or three candidates are open simultaneously the form becomes an undifferentiated wall of inputs. Users cannot easily tell where one candidate ends and the next begins.

### 1.3 Overview: Candidate Cell Dump

The `<td>` for the candidate name contains name + raw URL text + description + room text concatenated as strings. Raw URL text (booking.com/de/.../ferienwohnung-meerblick-...) is unreadable at table column width. The room text `Zimmer1 (3 Betten)` carries no visual meaning. There is no hierarchy to help the user compare candidates.

### 1.4 Overview: Monochrome Chart

All bars use the same `var(--pico-primary)` gradient. The winner bar switches to `var(--pico-success)` but only after the organiser confirms. During an open poll every candidate looks identical in the chart. Users have to read percentages rather than seeing differences at a glance.

### 1.5 Domain Model: Amenities on Rooms

The `CandidateRoom` record carries a `features: String` field. In real accommodation listings, amenities (WiFi, pool, kitchen, parking) belong to the property as a whole. Putting them on rooms makes the data entry confusing — users must repeat "WiFi" on every room — and makes the comparison table hard to read. Rooms should describe sleeping capacity and optionally a bed type; amenities should move to the candidate level.

### 1.6 No Booking Workflow

After the organiser confirms a candidate, there is no state for "booking attempted but failed". The poll goes directly from OPEN to CONFIRMED. In reality the organiser must attempt the booking first, and the accommodation may no longer be available. There is no fallback path in the UI.

---

## 2. Domain Model Change Recommendation

Before specifying the UI, one domain model change is required to unblock the correct UI design.

### 2.1 Move amenities from `CandidateRoom` to `AccommodationCandidate`

**Current `CandidateRoom`**: `name`, `bedCount`, `pricePerNight`, `features (String)`

**Proposed `CandidateRoom`**: `name`, `bedCount`, `bedType (String, optional)`, `pricePerNight (optional)`
- Remove `features` field entirely from `CandidateRoom`
- `features` is optional free text for the type of beds (e.g. "1 Doppelbett", "2 Einzelbetten")

**Add `amenities: List<String>` to `AccommodationCandidate`**
- Amenity values are free-form tags (not an enum) to avoid i18n complexity in the domain
- Recommended values the UI should suggest: `wifi`, `pool`, `kitchen`, `parking`, `breakfast`, `pets`, `balcony`, `washer`
- The UI renders these as localised labels using i18n message keys
- Maximum 12 amenities per candidate (enforced in the form, not the domain)

This is an architectural recommendation; the architect makes the final call. All UI specs below assume this change is accepted. The `features` text field in rooms is treated as the new `bedType` field.

### 2.2 Add `BOOKING_FAILED` status to `AccommodationPollStatus`

**Current states**: `OPEN` → `CONFIRMED` | `CANCELLED`

**Proposed states**: `OPEN` → `BOOKING_PENDING` → `CONFIRMED` | `BOOKING_FAILED` → `OPEN` (re-opens for next attempt)

The architect may prefer a separate `bookingAttempts` list on the aggregate rather than a status change. Either approach is compatible with the UI design below. The key requirement is that the UI can render a "booking failed" state for a specific candidate and prompt the organiser to try the next one.

---

## 3. Component: Accommodation Candidate Card

Used in the voting table as the content of the first column, replacing the current `poll-row-copy` div that dumps all candidate data as raw text.

### 3.1 Information Hierarchy

Tier 1 (always visible, primary scan target): Accommodation name
Tier 2 (secondary info, readable in table context): address fragment or city if available; description as one line (clamped)
Tier 3 (expandable or visible on wider viewports): room summary chips; amenity icons
Tier 4 (link, never raw URL text): "booking.com" as a short link label

### 3.2 Desktop Layout (≥768px)

```
┌─────────────────────────────────────────────────────────────┐
│  Hotel Seeblick                                             │
│  Bodensee, Deutschland · booking.com ↗                      │
│  Schöne Ferienwohnung mit Seeblick, ideal für Familien...   │
│  ──────────────────────────────────────────────────────     │
│  🛏 Zimmer 1 · 4 Betten    🛏 Zimmer 2 · 2 Betten           │
│  [wifi] [pool] [kitchen] [parking]                          │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Mobile Layout (≤767px): card mode via `.table-cards`

The `.table-cards` responsive pattern already in style.css converts each `<tr>` into a card. The candidate cell should render in full within that card — all tiers visible stacked vertically.

### 3.4 Thymeleaf Fragment Specification

```html
<!-- Candidate cell content — replaces current poll-row-copy div -->
<div class="candidate-card">
  <div class="candidate-card__name">
    <strong th:text="${candidate.name()}">Hotel Name</strong>
    <a th:if="${candidate.url()}"
       th:href="${candidate.url()}"
       class="candidate-card__link"
       target="_blank" rel="noopener noreferrer">
      <span class="candidate-card__link-label"
            th:text="${#strings.abbreviate(
              #strings.replace(
                #strings.replace(candidate.url(), 'https://', ''),
                'http://', ''),
              32)}">booking.com</span>
      <!-- External link icon (inline SVG, 12x12) -->
      <svg aria-hidden="true" viewBox="0 0 12 12" width="12" height="12">
        <path d="M5 2H2a1 1 0 00-1 1v7a1 1 0 001 1h7a1 1 0 001-1V7M7 1h4m0 0v4m0-4L5 7"
              stroke="currentColor" stroke-width="1.5" fill="none"
              stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </a>
  </div>
  <p th:if="${candidate.description()}"
     class="candidate-card__desc"
     th:text="${candidate.description()}">Beschreibung</p>
  <div th:if="${candidate.rooms().size() > 0}" class="candidate-card__rooms">
    <span th:each="room : ${candidate.rooms()}"
          class="candidate-card__room-chip">
      <!-- Bed SVG icon 14x14 -->
      <svg aria-hidden="true" viewBox="0 0 14 14" width="14" height="14">
        <path d="M1 7V3a1 1 0 011-1h10a1 1 0 011 1v4M1 7h12M1 7v4h12V7"
              stroke="currentColor" stroke-width="1.5" fill="none"
              stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span th:text="${room.name() + ' · ' + room.bedCount()}">Zimmer 1 · 3</span>
    </span>
  </div>
  <!-- Amenity pills — only if domain model change is accepted -->
  <div th:if="${candidate.amenities() != null and !candidate.amenities().isEmpty()}"
       class="candidate-card__amenities">
    <span th:each="amenity : ${candidate.amenities()}"
          class="candidate-card__amenity-pill"
          th:text="#{${'accommodationpoll.amenity.' + amenity}}">wifi</span>
  </div>
</div>
```

### 3.5 New CSS Classes for style.css

```css
/* ── Candidate Card (voting table cell) ─────────────── */
.candidate-card {
    display: grid;
    gap: 0.3rem;
}
.candidate-card__name {
    display: flex;
    align-items: baseline;
    gap: 0.5rem;
    flex-wrap: wrap;
}
.candidate-card__link {
    display: inline-flex;
    align-items: center;
    gap: 0.2rem;
    font-size: 0.8rem;
    color: var(--pico-primary);
    text-decoration: none;
    white-space: nowrap;
}
.candidate-card__link:hover {
    text-decoration: underline;
}
.candidate-card__desc {
    margin: 0;
    font-size: 0.875rem;
    color: var(--tm-text-soft);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
.candidate-card__rooms {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
    margin-top: 0.1rem;
}
.candidate-card__room-chip {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
    font-size: 0.78rem;
    padding: 0.2rem 0.5rem;
    background: var(--tm-surface-muted);
    border: 1px solid var(--tm-border-strong);
    border-radius: 1rem;
    color: var(--tm-text-soft);
    white-space: nowrap;
}
.candidate-card__room-chip svg {
    stroke: var(--tm-text-soft);
    flex-shrink: 0;
}
.candidate-card__amenities {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
    margin-top: 0.1rem;
}
.candidate-card__amenity-pill {
    font-size: 0.75rem;
    padding: 0.15rem 0.4rem;
    background: var(--pico-primary-background, #eef2ff);
    color: var(--pico-primary);
    border-radius: 0.75rem;
    border: 1px solid var(--pico-primary-border, #c7d2fe);
}
```

---

## 4. Component: Vote Visualisation (Coloured Bar Chart)

### 4.1 Problem with Current Implementation

All bars are the same colour. The winner gets green only after confirmation. The colour shift is purely status-driven, not identity-driven. With 4–6 candidates, users cannot visually map a legend entry to its bar.

### 4.2 Design: Per-Candidate Colour Assignment

Assign a deterministic colour index to each candidate based on its position in the list (0-indexed). Use a palette of 6 hues that are:
- Distinguishable for common colour-vision deficiencies (deuteranopia / protanopia)
- Compatible with PicoCSS default blue primary
- Readable on both light and dark backgrounds

Palette (CSS custom properties added to style.css):
```
--tm-poll-c0: #3366CC;  /* blue — same as --tm-primary */
--tm-poll-c1: #E07B00;  /* amber/orange */
--tm-poll-c2: #1E9E5A;  /* green */
--tm-poll-c3: #CC3333;  /* red */
--tm-poll-c4: #7B3FCC;  /* purple */
--tm-poll-c5: #009999;  /* teal */
```

The colour index wraps for polls with more than 6 candidates (rare but possible).

### 4.3 Thymeleaf: Assign Index to Each Bar

PicoCSS and Thymeleaf do not provide a direct index-to-CSS-var mechanism, but Thymeleaf's `th:each` exposes `iterStat.index`. Use inline style with a CSS variable per bar:

```html
<div th:each="candidate, iterStat : ${accommodationPoll.candidates()}"
     class="poll-chart__row"
     th:classappend="${accommodationPoll.selectedCandidateId() != null
         and accommodationPoll.selectedCandidateId().equals(candidate.candidateId())} ? 'winner' : ''">
  <span class="poll-chart__label" th:text="${candidate.name()}">Hotel</span>
  <div class="poll-chart__track">
    <div class="poll-chart__bar"
         th:style="|width:${total > 0 ? (candidate.voteCount() * 100.0 / total) : 4}%;
                    background: var(--tm-poll-c${iterStat.index % 6});|">
    </div>
  </div>
  <span class="poll-chart__value"
        th:text="${total > 0 ? T(java.lang.Math).round(candidate.voteCount() * 100.0 / total) : 0} + '%'">0%</span>
</div>
```

Matching legend dots get the same `background: var(--tm-poll-c${iterStat.index % 6})` inline style. This makes label ↔ bar ↔ legend dot visually connected by hue.

### 4.4 Winner Highlighting

When `selectedCandidateId` is set (after confirmation), the winning bar receives:
- A star/trophy SVG icon before the label (14x14)
- The bar gains `font-weight: 600` on the label
- The bar track gains a subtle `box-shadow` ring in the bar colour
- A small `<mark>` badge (using PicoCSS `<mark>`) after the percentage: "(Bestätigt)"

The monochrome winner-override (`.poll-chart__row.winner .poll-chart__bar { background: var(--pico-success) }`) should be removed from style.css. The candidate keeps its assigned colour; the star icon provides the winner signal without destroying the colour identity.

### 4.5 Empty State (0 votes)

All bars at 4% minimum width is already implemented. Add a centred hint below the chart when `total == 0`:

```html
<p th:if="${total == 0}" class="poll-chart__no-votes"
   th:text="#{accommodationpoll.chart.noVotes}">Noch keine Stimmen abgegeben.</p>
```

### 4.6 Dashboard Summary Strip (new reference consolidation)

The provided survey dashboard reference adds a useful framing pattern before the chart itself: a
compact row of KPI tiles. For Travelmate, this should sit between the page hero and the vote chart.

Recommended four tiles:

- `Kandidaten` — count of active candidates
- `Stimmen` — total submitted votes
- `Fuehrender Kandidat` — current leader name or `Noch offen`
- `Status` — `Offen`, `Buchung ausstehend`, `Gebucht`, `Abgebrochen`

Why this pattern fits:

- participants understand the overall state before parsing individual cards
- organiser-only states such as `AWAITING_BOOKING` become explicit
- the page gains a dashboard character without requiring client-side chart libraries

HTML sketch:

```html
<section class="poll-kpi-grid" aria-label="Poll summary">
  <article class="poll-kpi">
    <span class="poll-kpi__value" th:text="${activeCandidateCount}">3</span>
    <span class="poll-kpi__label" th:text="#{accommodationpoll.kpi.candidates}">Kandidaten</span>
  </article>
  <article class="poll-kpi">
    <span class="poll-kpi__value" th:text="${totalVotes}">5</span>
    <span class="poll-kpi__label" th:text="#{accommodationpoll.kpi.votes}">Stimmen</span>
  </article>
  <article class="poll-kpi">
    <span class="poll-kpi__value"
          th:text="${leadingCandidateName != null ? leadingCandidateName : #messages.msg('accommodationpoll.kpi.open')}">Hotel Seeblick</span>
    <span class="poll-kpi__label" th:text="#{accommodationpoll.kpi.leading}">Fuehrender Kandidat</span>
  </article>
  <article class="poll-kpi">
    <span class="poll-kpi__value" th:text="#{${'accommodationpoll.status.' + accommodationPoll.status()}}">Offen</span>
    <span class="poll-kpi__label" th:text="#{accommodationpoll.kpi.status}">Status</span>
  </article>
</section>
```

CSS sketch:

```css
.poll-kpi-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
    gap: 0.85rem;
    margin: 1rem 0 1.25rem;
}
.poll-kpi {
    display: grid;
    gap: 0.15rem;
    padding: 1rem 1.1rem;
    border: 1px solid var(--tm-border-strong);
    border-radius: 1rem;
    background: var(--tm-surface, #fff);
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}
.poll-kpi__value {
    font-size: 1.55rem;
    font-weight: 700;
    line-height: 1.1;
}
.poll-kpi__label {
    color: var(--tm-text-soft);
    font-size: 0.85rem;
}
```

Decision note:

- keep the horizontal ranked bars as the primary comparison view
- do not add a radar chart to the main poll page; radar visuals look attractive in survey tools but
  are weaker for precise accommodation comparison and voting decisions

---

## 5. Create / Edit Form Redesign

### 5.1 Layout Fix: Room Rows

The root cause is `<label>` as the flex child. Replace the four-`<label>` flex row with a CSS grid row inside a plain wrapper div:

```html
<template id="room-row-template">
  <div class="room-row">
    <div class="room-row__grid">
      <div class="room-row__field">
        <label>
          <span th:text="#{accommodationpoll.room.name}">Zimmer</span>
          <input type="text" name="roomName" required>
        </label>
      </div>
      <div class="room-row__field room-row__field--narrow">
        <label>
          <span th:text="#{accommodationpoll.room.bedCount}">Betten</span>
          <input type="number" min="1" name="roomBedCount" required>
        </label>
      </div>
      <div class="room-row__field room-row__field--narrow">
        <label>
          <span th:text="#{accommodationpoll.room.pricePerNight}">Preis/Nacht</span>
          <input type="number" step="0.01" min="0" name="roomPricePerNight">
        </label>
      </div>
      <div class="room-row__field room-row__field--narrow">
        <label>
          <span th:text="#{accommodationpoll.room.bedType}">Betttyp</span>
          <input type="text" name="roomBedType" placeholder="z.B. Doppelbett">
        </label>
      </div>
    </div>
    <button type="button" class="btn-icon btn-icon--danger remove-room-btn"
            th:aria-label="#{common.remove}">
      <svg viewBox="0 0 24 24">
        <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
      </svg>
    </button>
  </div>
</template>
```

CSS for room rows (add to style.css):

```css
/* ── Room Row Grid ───────────────────────────────────── */
.room-row {
    display: flex;
    align-items: flex-end;
    gap: 0.5rem;
    margin-top: 0.5rem;
}
.room-row__grid {
    display: grid;
    grid-template-columns: 2fr 1fr 1fr 1.5fr;
    gap: 0.5rem;
    flex: 1;
    min-width: 0;
}
.room-row__field {
    min-width: 0;
}
.room-row__field label {
    margin-bottom: 0;
}
.room-row__field input {
    margin-bottom: 0;
}

@media (max-width: 767px) {
    .room-row {
        flex-direction: column;
        align-items: stretch;
    }
    .room-row__grid {
        grid-template-columns: 1fr 1fr;
    }
    .room-row .btn-icon {
        align-self: flex-end;
    }
}
```

### 5.2 Candidate Entry Visual Hierarchy

Each candidate block should read as a card with a clear header (candidate number + remove button) and a body (fields). The current flat div with a flex row at the top is not enough.

Redesigned candidate entry structure:

```html
<div class="candidate-entry">
  <!-- Card header: title + remove -->
  <div class="candidate-entry__header">
    <h4 class="candidate-entry__title">
      <!-- Number filled by JS: "Unterkunft 1" -->
      <span class="candidate-entry__number"></span>
    </h4>
    <button type="button" class="btn-icon btn-icon--danger remove-candidate-btn"
            th:aria-label="#{common.remove}">
      <svg viewBox="0 0 24 24">
        <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
      </svg>
    </button>
  </div>

  <!-- Core fields -->
  <input type="hidden" name="candidateRoomsJson" class="candidate-rooms-data" value="[]">
  <label>
    <span th:text="#{accommodationpoll.candidateName}">Name *</span>
    <input type="text" name="candidateName" required>
  </label>
  <div class="grid">
    <label>
      <span th:text="#{accommodationpoll.candidateUrl}">URL</span>
      <input type="url" name="candidateUrl">
    </label>
    <label>
      <span th:text="#{accommodationpoll.candidateDescription}">Kurzbeschreibung</span>
      <input type="text" name="candidateDescription">
    </label>
  </div>

  <!-- Amenity checkboxes (if domain model change accepted) -->
  <fieldset class="candidate-entry__amenities">
    <legend th:text="#{accommodationpoll.amenities}">Ausstattung</legend>
    <div class="candidate-entry__amenity-grid">
      <!-- Rendered for each known amenity value -->
      <label class="candidate-entry__amenity-check">
        <input type="checkbox" name="candidateAmenities" value="wifi">
        <!-- WiFi SVG icon 16x16 -->
        <svg aria-hidden="true" viewBox="0 0 16 16" width="16" height="16">
          <path d="M1 6a10 10 0 0114 0M4 9a5 5 0 018 0M7 12a1 1 0 002 0"
                stroke="currentColor" stroke-width="1.5" fill="none"
                stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span th:text="#{accommodationpoll.amenity.wifi}">WLAN</span>
      </label>
      <!-- … repeat for pool, kitchen, parking, breakfast, pets, balcony, washer … -->
    </div>
  </fieldset>

  <!-- Rooms sub-section -->
  <div class="candidate-entry__rooms-section">
    <div class="candidate-entry__rooms-header">
      <strong th:text="#{accommodationpoll.rooms}">Zimmer</strong>
    </div>
    <div class="room-rows"></div>
    <button type="button" class="outline secondary add-room-btn"
            style="margin-top:0.5rem; font-size:0.875rem;">
      + <span th:text="#{accommodationpoll.addRoom}">Zimmer hinzufügen</span>
    </button>
  </div>
</div>
```

CSS additions for candidate entry:

```css
/* ── Candidate Entry Card ───────────────────────────── */
.candidate-entry {
    margin-bottom: 1.25rem;
    padding: 1rem;
    border: 1px solid var(--tm-border-strong);
    border-radius: var(--pico-border-radius);
    background: var(--tm-surface-muted);
}
.candidate-entry__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.75rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid var(--tm-border-strong);
}
.candidate-entry__title {
    margin: 0;
    font-size: 1rem;
}
.candidate-entry__rooms-section {
    margin-top: 0.75rem;
    padding-top: 0.75rem;
    border-top: 1px solid var(--tm-border-strong);
}
.candidate-entry__rooms-header {
    font-size: 0.875rem;
    color: var(--tm-text-soft);
    margin-bottom: 0.25rem;
}

/* ── Amenity Checkbox Grid ──────────────────────────── */
.candidate-entry__amenities {
    border: none;
    padding: 0;
    margin: 0.5rem 0;
}
.candidate-entry__amenities legend {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--tm-text-soft);
    margin-bottom: 0.4rem;
    padding: 0;
}
.candidate-entry__amenity-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem;
}
.candidate-entry__amenity-check {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.25rem 0.6rem;
    border: 1px solid var(--tm-border-strong);
    border-radius: 1rem;
    cursor: pointer;
    font-size: 0.8rem;
    background: #fff;
    transition: background 0.1s, border-color 0.1s;
    /* Override PicoCSS label 100% width */
    width: auto !important;
    margin: 0;
}
.candidate-entry__amenity-check:has(input:checked) {
    background: var(--pico-primary-background, #eef2ff);
    border-color: var(--pico-primary);
    color: var(--pico-primary);
}
.candidate-entry__amenity-check input[type="checkbox"] {
    /* Visually hidden but accessible */
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
}
.candidate-entry__amenity-check svg {
    stroke: currentColor;
    fill: none;
    stroke-width: 1.5;
    stroke-linecap: round;
    stroke-linejoin: round;
    flex-shrink: 0;
}
```

Note on the checkbox pattern: the `:has(input:checked)` CSS selector (supported in all modern browsers) handles the visual toggle without JavaScript. For older Travelmate supported browsers this degrades gracefully — the label stays unstyled but remains functional.

### 5.3 Numbering Candidates via JavaScript

Add one line to the existing JS to update `candidate-entry__number` text when candidates are added or removed:

```javascript
function updateCandidateNumbers() {
    document.querySelectorAll('.candidate-entry__number').forEach((el, idx) => {
        el.textContent = `Unterkunft ${idx + 1}`;
    });
}
// Call updateCandidateNumbers() after addCandidateEntry() and after remove-candidate-btn click
```

### 5.4 Validation: Minimum 2 Candidates

The domain already enforces this (`proposals.size() >= 2`). The form should also show a client-side hint when the user tries to submit with only 1 candidate — but since this is a progressive enhancement project, the server-side error returned via full-page reload is acceptable. No new JavaScript is needed here.

---

## 6. Booking Workflow UI (Post-Confirmation State)

This section specifies the UI states for the booking attempt flow. The backend state machine must be implemented by the architect (see domain model recommendation in section 2.2).

### 6.1 State Machine (UI perspective)

```
[OPEN]
  ↓ organiser confirms candidate
[BOOKING_PENDING]   — organiser sees booking instructions, marks outcome
  ↓ success             ↓ failure
[CONFIRMED]         [BOOKING_FAILED]
                        ↓ organiser marks as failed
                    [OPEN again] — next candidate highlighted as suggested
```

### 6.2 Winner Banner — BOOKING_PENDING State

Replaces the current `winner-banner` article when the status is `BOOKING_PENDING`:

```
┌──────────────────────────────────────────────────────────────┐
│  NAECHSTER SCHRITT                                           │
│  Unterkunft buchen                                           │
│                                                              │
│  Hotel Seeblick wurde ausgewaehlt. Bitte buche die Unterkunft│
│  und markiere das Ergebnis hier.                             │
│                                                              │
│  [Buchung erfolgreich — Bestaetigen]  [Buchung fehlgeschlagen]│
└──────────────────────────────────────────────────────────────┘
```

HTML structure:

```html
<article class="booking-pending-banner"
         th:if="${accommodationPoll.status() == 'BOOKING_PENDING'}">
  <p class="poll-hero__eyebrow" th:text="#{accommodationpoll.booking.nextStep}">Naechster Schritt</p>
  <h3 th:text="#{accommodationpoll.booking.title}">Unterkunft buchen</h3>
  <p>
    <strong th:text="${winner.name()}">Hotel Seeblick</strong>
    <span th:text="#{accommodationpoll.booking.hint}">
      wurde ausgewaehlt. Bitte buche die Unterkunft und markiere das Ergebnis hier.
    </span>
  </p>
  <a th:if="${winner.url()}"
     th:href="${winner.url()}"
     role="button"
     class="outline secondary"
     target="_blank" rel="noopener noreferrer"
     th:text="#{accommodationpoll.booking.openListing}">Inserat oeffnen ↗</a>
  <div class="booking-pending-banner__actions">
    <form method="post"
          th:action="@{/{tid}/accommodationpoll/{pid}/booking/confirm(tid=${trip.tripId()},pid=${accommodationPoll.accommodationPollId()})}">
      <button type="submit" th:text="#{accommodationpoll.booking.success}">Buchung erfolgreich — Bestaetigen</button>
    </form>
    <form method="post"
          th:action="@{/{tid}/accommodationpoll/{pid}/booking/fail(tid=${trip.tripId()},pid=${accommodationPoll.accommodationPollId()})}">
      <button type="submit" class="outline secondary"
              th:text="#{accommodationpoll.booking.fail}">Buchung fehlgeschlagen</button>
    </form>
  </div>
</article>
```

CSS:

```css
.booking-pending-banner {
    background: linear-gradient(135deg, #fffbeb, #fef9ec);
    border: 1px solid #f59e0b;
}
.booking-pending-banner .poll-hero__eyebrow {
    color: #b45309;
}
.booking-pending-banner__actions {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
    margin-top: 1rem;
}
.booking-pending-banner__actions form {
    margin: 0;
}
@media (max-width: 767px) {
    .booking-pending-banner__actions {
        flex-direction: column;
    }
    .booking-pending-banner__actions button {
        width: 100%;
    }
}
```

### 6.3 BOOKING_FAILED State

When the organiser marks a booking as failed, the poll re-opens. The overview renders a prominent warning banner above the vote table:

```
┌──────────────────────────────────────────────────────────────┐
│  ⚠  Hotel Seeblick konnte nicht gebucht werden.             │
│     Waehle eine andere Unterkunft oder fuege neue Vorschlaege│
│     hinzu.                                                   │
│     [Neue Unterkunft hinzufuegen ↓]                         │
└──────────────────────────────────────────────────────────────┘
```

HTML:

```html
<div role="alert" class="alert alert-warning"
     th:if="${accommodationPoll.lastFailedCandidateName != null}">
  <strong>
    <span th:text="${accommodationPoll.lastFailedCandidateName}">Hotel Seeblick</span>
    <span th:text="#{accommodationpoll.booking.failedHint}">konnte nicht gebucht werden.</span>
  </strong>
  <p th:text="#{accommodationpoll.booking.failedAction}">
    Waehle eine andere Unterkunft oder fuege neue Vorschlaege hinzu.
  </p>
  <a href="#add-candidate-panel" role="button" class="outline secondary"
     th:text="#{accommodationpoll.addCandidate}">Neue Unterkunft hinzufuegen ↓</a>
</div>
```

In the vote table, the row for the failed candidate gets a visual mark (red border-left or strikethrough) to indicate it is no longer available, even if it still appears in the list for transparency.

### 6.4 CONFIRMED State (Booking Success)

The existing `winner-banner` is already close to correct. Two improvements:

1. Show the "open listing" link prominently in the banner — the organiser needs to share the booking URL with participants.
2. Replace the raw room text with the room chips component from section 3.

---

## 7. Address Display

### 7.1 Current State

`AccommodationCandidate` has no address field. The only location data is the URL. Participants have no way to know where the accommodation is without clicking the link.

### 7.2 Short-term Recommendation (no new domain field)

When an accommodation URL is available, show a "Auf Karte anzeigen" link that opens Google Maps / OpenStreetMap with the candidate name as the search query. This requires no new domain field and no server-side change:

```html
<a th:if="${candidate.name()}"
   th:href="${'https://www.openstreetmap.org/search?query=' + #uris.encodePath(candidate.name())}"
   class="candidate-card__map-link"
   target="_blank" rel="noopener noreferrer">
  <!-- Map pin SVG 14x14 -->
  <svg aria-hidden="true" viewBox="0 0 14 14" width="14" height="14">
    <path d="M7 1C4.79 1 3 2.79 3 5c0 3.25 4 8 4 8s4-4.75 4-8c0-2.21-1.79-4-4-4z"
          stroke="currentColor" stroke-width="1.5" fill="none"
          stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="7" cy="5" r="1.5" stroke="currentColor" stroke-width="1.5" fill="none"/>
  </svg>
  <span th:text="#{accommodationpoll.showOnMap}">Auf Karte anzeigen</span>
</a>
```

### 7.3 Medium-term Recommendation (with address field)

If the domain adds an `address: String` field to `AccommodationCandidate` (which the URL import pipeline already extracts from `schema.org/PostalAddress`), the candidate card should show the address in Tier 2 instead of "on map" link:

```
Oberdorfstrasse 12, 78315 Radolfzell · booking.com ↗  [map icon link]
```

The map link URL becomes more accurate:
`https://www.openstreetmap.org/search?query=<encoded address>`

No embedded `<iframe>` map is recommended — embedded maps require third-party script loading, increase page weight, and break the progressive enhancement principle. An `<a>` link to OpenStreetMap is sufficient and opens in a new tab.

### 7.4 Listing-style placement of location and room detail

The accommodation listing reference suggests a stronger structure for the candidate card than the
current "description first, everything else later" ordering. The Travelmate card should therefore
use this order:

1. Name and external listing link
2. Address/location line with adjacent map action
3. Amenity chip row
4. Room/sleeping section
5. Optional description excerpt

This ordering works better for voting because:

- users first ask "where is it?"
- then "does it have what we need?"
- then "can everyone sleep there?"

Compact HTML sketch:

```html
<div class="candidate-card__location-row"
     th:if="${candidate.address() != null or candidate.name() != null}">
  <span class="candidate-card__location"
        th:text="${candidate.address() != null ? candidate.address() : candidate.name()}">
    Oberdorfstrasse 12, 78315 Radolfzell
  </span>
  <a th:href="${candidate.address() != null
        ? 'https://maps.google.com/?q=' + #uris.encodeQuery(candidate.address())
        : 'https://maps.google.com/?q=' + #uris.encodeQuery(candidate.name())}"
     class="candidate-card__map-link"
     target="_blank" rel="noopener noreferrer"
     th:text="#{accommodationpoll.showOnMap}">Auf Karte anzeigen</a>
</div>
```

This keeps the card close to a familiar booking/listing mental model without introducing embedded
maps or image-heavy layouts.

---

## 8. Full Page Wireframe: Overview (Redesigned)

### 8.1 Desktop (≥768px) — OPEN Status, 3 candidates, 5 votes cast

```
┌─────────────────────────────────────────────────────────────────────┐
│ [hgroup] Unterkunftsabstimmung  ← Mallorca Familienurlaub           │
├─────────────────────────────────────────────────────────────────────┤
│ [article.poll-hero]                                          [SVG]  │
│   SCHRITT 2                                                         │
│   Unterkunftsabstimmung       [badge: Offen]                        │
│   Die Abstimmung laeuft — waehle deine bevorzugte Unterkunft.       │
│   [3 Kandidaten]  [5 Stimmen]                                       │
├─────────────────────────────────────────────────────────────────────┤
│ [article.poll-chart]                                                │
│   Stimmenverteilung                                                 │
│   ─────────────────────────────────────────────────────            │
│   Hotel Seeblick   [████████████░░░░░░░░░░░░░░░░░░░]   60%  ●blue  │
│   Ferienwohnung A  [████████░░░░░░░░░░░░░░░░░░░░░░░]   40%  ●amber │
│   Hostel Zentrum   [░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]    0%  ●green │
│   ─────────────────────────────────────────────────────            │
│   Noch keine Stimme für Hostel Zentrum.                             │
├─────────────────────────────────────────────────────────────────────┤
│ [article] Abstimmen                                                 │
│ Die Abstimmung laeuft — stimme fuer deine bevorzugte Unterkunft.   │
│                                                                     │
│ [form] ┌──────────────────────────────────────────┬───┬──────────┐ │
│        │ Unterkunft                               │Stm│Deine Stm │ │
│        ├──────────────────────────────────────────┼───┼──────────┤ │
│        │ Hotel Seeblick                           │ 3 │  (●)     │ │
│        │ bodensee-hotel.de ↗  [map pin]           │   │          │ │
│        │ Tolle Lage, Seeblick von allen Zimmern  │   │          │ │
│        │ [🛏 Zimmer A · 4] [🛏 Zimmer B · 2]      │   │          │ │
│        │ [wifi][pool][breakfast]                  │   │          │ │
│        ├──────────────────────────────────────────┼───┼──────────┤ │
│        │ Ferienwohnung Sauer                      │ 2 │  ( )     │ │
│        │ ferienwohnungen.de ↗  [map pin]          │   │          │ │
│        │ [🛏 Hauptzimmer · 5] [🛏 Studio · 2]     │   │          │ │
│        │ [kitchen][parking][balcony]              │   │          │ │
│        ├──────────────────────────────────────────┼───┼──────────┤ │
│        │ Hostel Zentrum                           │ 0 │  ( )     │ │
│        │ hostelworld.com ↗                        │   │          │ │
│        │ [🛏 Schlafsaal · 8]                      │   │          │ │
│        └──────────────────────────────────────────┴───┴──────────┘ │
│  [button: Abstimmen]                                                │
├─────────────────────────────────────────────────────────────────────┤
│ [article] Aktionen  [only visible to organiser]                     │
│ ┌──────────────────────┐┌──────────────────────┐┌────────────────┐ │
│ │ Unterkunft hinzufügen ││ URL importieren       ││ Bestaetigen    │ │
│ │ [Name]                ││ [URL input]           ││ [dropdown]     │ │
│ │ [URL]                 ││ [Importieren button]  ││ [Bestaetigen]  │ │
│ │ [Beschreibung]        │└──────────────────────┘└────────────────┘ │
│ │ [Ausstattung pills]   │                                           │
│ │ + Zimmer hinzufügen   │                                           │
│ │ [Hinzufuegen]         │                                           │
│ └──────────────────────┘                                           │
│                                                                     │
│ [button outline secondary: Abstimmung abbrechen]                    │
├─────────────────────────────────────────────────────────────────────┤
│ [← Zurueck zur Reise]                                               │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Mobile (≤767px) — OPEN Status

```
┌───────────────────────────┐
│ Unterkunftsabstimmung     │
│ ← Mallorca Familienurlaub │
├───────────────────────────┤
│ SCHRITT 2                 │
│ Unterkunftsabstimmung     │
│ [Offen]                   │
│ Die Abstimmung laeuft...  │
│ [3 Kandidaten][5 Stimmen] │
├───────────────────────────┤
│ Stimmenverteilung         │
│ Hotel Seeblick    60% ●   │
│ [████████████░░░░░░░]     │
│ Ferienwohnung A   40% ●   │
│ [████████░░░░░░░░░░░]     │
│ Hostel Zentrum     0% ●   │
│ [░░░░░░░░░░░░░░░░░░░]     │
├───────────────────────────┤
│ Abstimmen                 │
│                           │
│ ┌─────────────────────┐   │
│ │ Unterkunft          │   │
│ │ Hotel Seeblick      │   │
│ │ bodensee-hotel.de ↗ │   │
│ │ [wifi][pool]        │   │
│ │ [🛏 Zimmer A · 4]   │   │
│ │                     │   │
│ │ Stimmen: 3          │   │
│ │ Deine Stimme: (●)   │   │
│ └─────────────────────┘   │
│ ┌─────────────────────┐   │
│ │ Unterkunft          │   │
│ │ Ferienwohnung Sauer │   │
│ │ ...                 │   │
│ │ Stimmen: 2          │   │
│ │ Deine Stimme: ( )   │   │
│ └─────────────────────┘   │
│ [Abstimmen] (full-width)  │
└───────────────────────────┘
```

---

## 9. Full Page Wireframe: Create Form (Redesigned)

### 9.1 Desktop (≥768px)

```
┌─────────────────────────────────────────────────────────────────────┐
│ Unterkunftsabstimmung erstellen                                     │
│ ← Mallorca Familienurlaub                                           │
├─────────────────────────────────────────────────────────────────────┤
│ [article]                                                           │
│   [form]                                                            │
│                                                                     │
│ ┌────────── Unterkunft 1 ──────────────────────────────── [✕] ──┐  │
│ │ Name *  [                                              ]       │  │
│ │ URL     [                        ]  Beschreibung [         ]   │  │
│ │                                                                │  │
│ │ Ausstattung                                                    │  │
│ │ [wifi ○] [pool ○] [kitchen ○] [parking ○] [breakfast ○]...    │  │
│ │                                                                │  │
│ │ Zimmer ──────────────────────────────────────────────────      │  │
│ │ ┌──────────────┬──────┬──────────┬────────────────┐           │  │
│ │ │ Zimmer       │Betten│ Preis/Nt │ Betttyp         │  [✕]     │  │
│ │ │ [          ] │[   ] │ [      ] │ [             ] │           │  │
│ │ └──────────────┴──────┴──────────┴────────────────┘           │  │
│ │ + Zimmer hinzufügen                                            │  │
│ └────────────────────────────────────────────────────────────┘  │  │
│                                                                     │
│ ┌────────── Unterkunft 2 ──────────────────────────────── [✕] ──┐  │
│ │  ... (same structure)                                          │  │
│ └────────────────────────────────────────────────────────────┘  │  │
│                                                                     │
│ [+ Unterkunft hinzufügen]                                          │
│                                                                     │
│ [Erstellen]  [Abbrechen]                                           │
│ Mindestens 2 Unterkünfte erforderlich.                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 9.2 Mobile (≤767px)

The grid columns in `.room-row__grid` collapse to 2-column at ≤767px (name + bed count on row 1, price + bed type on row 2). The amenity chip grid wraps naturally. No additional CSS is needed beyond what is already specified.

---

## 10. Journey Map: Organiser Plans Accommodation

```markdown
## User Journey: Accommodation Poll — Organiser Workflow
**Persona**: Organiser
**Goal**: Agree on accommodation with the travel party before booking
**Trigger**: Trip is in PLANNING status; organiser opens the Planning page

### Journey Phases

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Discover | Opens Planning page, sees empty Step 2 card | Empty state with CTA | planning/index | Curious | No guidance on what to prepare | Hint text: "Füge 2+ Vorschläge hinzu" |
| 2. Create Poll | Clicks "Unterkunftsabstimmung erstellen" | create.html with 2 pre-populated empty candidates | create.html | Engaged | Room row overflow breaks on narrow screen (P0) | CSS grid fix eliminates overflow |
| 3. Fill Candidates | Pastes booking URLs, imports data | URL import fills name/rooms/price automatically | create.html | Satisfied | Manual data entry is tedious without import | Prominent import field |
| 4. Submit | Clicks "Erstellen" | Redirects to overview.html, poll is OPEN | overview.html | Accomplished | No visual confirmation of what was saved | Inline success alert on redirect |
| 5. Wait for Votes | Shares overview link with party | Live vote counts visible | overview.html | Anticipating | No notification when votes come in | Future: push/email on new vote |
| 6. Review Results | Sees vote chart stabilise | Coloured bars show clear winner | overview.html | Decisive | Monochrome bars make comparison hard (current bug) | Per-candidate colour assignment |
| 7. Confirm Winner | Selects winner in confirm dropdown | Status → BOOKING_PENDING; booking banner appears | overview.html | Determined | No connection between "confirm" and "book" step | BOOKING_PENDING state + open-listing button |
| 8. Book Externally | Clicks "Inserat öffnen", books on third-party site | External booking site opens in new tab | Third-party | Tense | No way to report back if booking fails | "Booking failed" button closes the loop |
| 9a. Success | Returns, clicks "Buchung erfolgreich" | Status → CONFIRMED; winner banner in green | overview.html | Relieved | Success state looks same as "just selected" | Distinct CONFIRMED visual + accommodation URL shared |
| 9b. Failure | Returns, clicks "Buchung fehlgeschlagen" | Status → OPEN again; failed banner shown | overview.html | Frustrated | No path forward in current UI | BOOKING_FAILED state + "add new candidate" prompt |
| 10. Retry (9b path) | Adds new candidate or selects next | Poll re-enters confirmation flow | overview.html | Resilient | Must start over from Aktionen panel | Suggest "next best" candidate automatically |

### Key Metrics
| Metric | Target | Measurement |
|--------|--------|-------------|
| Poll creation completion rate | >90% | Funnel analytics |
| Time from create to first vote | <24h | Timestamps |
| Booking failure rate | <20% | booking/fail POST count |
```

---

## 11. New i18n Keys Required

All keys needed in `messages.properties`, `messages_de.properties`, and `messages_en.properties`:

```
# Amenities
accommodationpoll.amenities=Ausstattung | Amenities
accommodationpoll.amenity.wifi=WLAN | WiFi
accommodationpoll.amenity.pool=Pool | Pool
accommodationpoll.amenity.kitchen=Kueche | Kitchen
accommodationpoll.amenity.parking=Parkplatz | Parking
accommodationpoll.amenity.breakfast=Fruehstueck | Breakfast
accommodationpoll.amenity.pets=Haustiere erlaubt | Pets allowed
accommodationpoll.amenity.balcony=Balkon | Balcony
accommodationpoll.amenity.washer=Waschmaschine | Washing machine

# Room fields (renamed from features)
accommodationpoll.room.name=Zimmer | Room
accommodationpoll.room.bedCount=Betten | Beds
accommodationpoll.room.pricePerNight=Preis/Nacht | Price/night
accommodationpoll.room.bedType=Betttyp (optional) | Bed type (optional)

# Address / Map
accommodationpoll.showOnMap=Auf Karte anzeigen | Show on map

# Chart
accommodationpoll.chart.noVotes=Noch keine Stimmen abgegeben. | No votes yet.

# KPI tiles
accommodationpoll.kpi.candidates=Kandidaten | Candidates
accommodationpoll.kpi.votes=Stimmen | Votes
accommodationpoll.kpi.leading=Fuehrender Kandidat | Leading candidate
accommodationpoll.kpi.status=Status | Status
accommodationpoll.kpi.open=Noch offen | Still open

# Booking workflow
accommodationpoll.booking.nextStep=Naechster Schritt | Next Step
accommodationpoll.booking.title=Unterkunft buchen | Book accommodation
accommodationpoll.booking.hint=wurde ausgewaehlt. Bitte buche die Unterkunft und markiere das Ergebnis hier. | was selected. Please book the accommodation and mark the outcome here.
accommodationpoll.booking.openListing=Inserat oeffnen | Open listing
accommodationpoll.booking.success=Buchung erfolgreich — Bestaetigen | Booking successful — Confirm
accommodationpoll.booking.fail=Buchung fehlgeschlagen | Booking failed
accommodationpoll.booking.failedHint=konnte nicht gebucht werden. | could not be booked.
accommodationpoll.booking.failedAction=Waehle eine andere Unterkunft oder fuege neue Vorschlaege hinzu. | Select a different accommodation or add new candidates.

# Existing key — updated text recommended
accommodationpoll.winnerTitle=Bestaetigt | Confirmed
```

---

## 12. HTMX Interaction Map

| Action | Trigger | Method | Endpoint | Target | Swap |
|--------|---------|--------|----------|--------|------|
| Cast/change vote | form submit | POST | `/{tid}/accommodationpoll/{pid}/vote` | `body` (full redirect) | — |
| Add candidate (OPEN poll) | form submit | POST | `/{tid}/accommodationpoll/{pid}/candidates/add` | full redirect | — |
| Import from URL | form submit | POST | `/{tid}/accommodationpoll/{pid}/import` | full redirect | — |
| Remove candidate | icon button → form submit | POST | `/{tid}/accommodationpoll/{pid}/candidates/{cid}/remove` | full redirect | — |
| Confirm winner | form submit | POST | `/{tid}/accommodationpoll/{pid}/confirm` | full redirect | — |
| Mark booking success | form submit | POST | `/{tid}/accommodationpoll/{pid}/booking/confirm` | full redirect | — |
| Mark booking failed | form submit | POST | `/{tid}/accommodationpoll/{pid}/booking/fail` | full redirect | — |
| Cancel poll | form submit | POST | `/{tid}/accommodationpoll/{pid}/cancel` | full redirect | — |

All interactions use full page redirect (PRG pattern). No HTMX partial updates are needed on this page because: (a) the vote table needs to re-render completely after any mutation, and (b) the page is not frequently auto-refreshed.

One optional HTMX enhancement: the "Import from URL" action could swap only the "add candidate" panel with the pre-filled form (following the Import Pipeline pattern from Iteration 10), replacing the current full redirect. This is recommended but not required for the base redesign.

---

## 13. Open Questions for Architect

1. **Domain model change approval**: Is the move of `amenities` from `CandidateRoom` to `AccommodationCandidate` acceptable? Does it require a Flyway migration to `accommodation_poll` schema?

2. **`BOOKING_PENDING` vs. booking attempts list**: Does the team prefer a new `BOOKING_FAILED` status (simple state machine) or a `List<BookingAttempt>` value object on the aggregate (richer audit trail)?

3. **Re-open mechanic**: When a booking fails, should the poll revert to `OPEN` fully (any candidate can be selected again) or enter a new `AWAITING_REBOOKING` state where only non-attempted candidates are shown?

4. **Amenity validation**: Should the domain validate amenity strings against an allow-list, or accept any string with UI-side suggestions only?

5. **Address field scope**: Should `address` be added to `AccommodationCandidate` now (populated by URL import) or deferred until the booking workflow is in place?
