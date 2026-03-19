# Iteration 10 — UX Analysis and Design Specification

**Version**: Iteration 10 planning (v0.10.0-SNAPSHOT) — REPLANNED 2026-03-19
**Author**: UX Designer Agent
**Scope**: Corrected story priorities. S10-A (Accommodation URL Import) is the headline feature. S10-B (Kassenzettel-Scan / Receipt OCR) is the secondary feature. Recipe Import from URL is deferred. S10-C (Category Breakdown) and S10-D (PDF Export) carry over from the previous analysis unchanged.

---

## 1. Starting Point: What Iteration 9 Delivered

Iteration 9 (v0.9.0) shipped six stories:

| Story | What was delivered |
|-------|--------------------|
| S9-A: Accommodation with Room Inventory | Dedicated `/{tripId}/accommodation` page; name, address, URL, check-in/out, total price, rooms table |
| S9-B: Room Assignment | Room grid with capacity indicator; assign TravelParty to room via inline form |
| S9-C: Party-Level Settlement | Party balance table + transfer sentences on SETTLED expense page |
| S9-D: Advance Payments | Equal round amount per party; `Bezahlt` toggle; auto-suggestion from accommodation price |
| S9-E: Re-submit Rejected Receipt | Inline edit + re-submit dialog on REJECTED receipt row |
| S9-F: PWA Manifest | `manifest.json` + install prompt; standalone mode on mobile |

---

## 2. Revised Priority Ranking

| Priority | Story | Rationale |
|----------|-------|-----------|
| HIGH | S10-A: Accommodation URL Import | Accommodation is the central planning hub. Every group trip starts with a URL from booking.com or Ferienhausmiete.de. Import removes the biggest data-entry friction. |
| MEDIUM | S10-B: Kassenzettel-Scan / Receipt OCR | Participants photograph receipts at the store. Mobile-first. Reduces the chance of receipts being lost or forgotten. |
| LOW (deferred) | Recipe Import from URL | Meal planning is secondary value. Defer to Iteration 11+. |
| CARRY-OVER | S10-C: Settlement per Category | Additive section on SETTLED expense page. Low implementation risk. |
| CARRY-OVER | S10-D: Export Settlement as PDF | Anna needs a shareable record after settlement. Low implementation risk. |

---

## 3. Shared UX Pattern: Import Pipeline

All import features in Travelmate follow the same five-step pipeline. This is the load-bearing design principle for Iteration 10.

```
Input → Analyse → Vorschau → EDIT → Speichern
```

Each step:

1. **Input** — user provides a URL or an image. Standard HTML form element, no JavaScript required for submission.
2. **Analyse** — server scrapes URL or runs OCR. HTMX posts to an import endpoint. Loading state via `aria-busy`. This is the only async step.
3. **Vorschau** — server returns a pre-filled, fully editable form as an HTML fragment. HTMX swaps it into the page.
4. **EDIT** — user corrects any wrong values. The form is a standard Thymeleaf form with all fields editable. There is no read-only confirmation step.
5. **Speichern** — user submits the corrected form to the regular create/update endpoint. The import endpoint and the save endpoint are different — import only fills the form, it never saves directly.

Critical design constraint: the system must never save imported data without user review. The EDIT step is mandatory, not optional.

Error path: if import fails (URL unreachable, OCR illegible, no data found), the system renders an inline `role="alert"` fragment. The user can then fill the form manually. Manual entry is always the fallback — import is an enhancement, not a dependency.

---

## 4. S10-A: Accommodation URL Import

### 4.1 User Journey

**Persona**: Organizer (Anna, plans a family cabin trip)
**Goal**: Create the Accommodation record without re-typing everything from the booking page
**Trigger**: Anna has the booking URL open in her browser. She navigates to the trip's Accommodation page.

| Phase | User Action | System Response | Touchpoint | Emotion |
|-------|-------------|-----------------|------------|---------|
| 1. Arrive | Opens Accommodation page; no accommodation exists yet | Empty state with "URL importieren" as primary action and "Manuell erfassen" as secondary | Accommodation page — empty state | Curious, slightly uncertain |
| 2. Input | Pastes URL from booking.com or Ferienhausmiete.de into the import field and clicks "Importieren" | Sends POST to import endpoint; button shows aria-busy spinner | Import field + button | Neutral |
| 3. Analyse | Waits 2–4 seconds | Server fetches URL, parses HTML (og:title, address, pricing schema, room data, check-in/out) | Loading state | Slightly impatient |
| 4. Vorschau / EDIT | Sees pre-filled form with all scraped values; reviews, corrects room types and bed counts as needed | Pre-filled editable form replaces the import field; rooms shown as an editable inline table | Editable form fragment | Relieved, engaged |
| 5. Speichern | Clicks "Unterkunft speichern" | POST to regular accommodation create endpoint; page reloads with new accommodation detail view | Save button | Satisfied |
| Error — no data | Paste a URL that cannot be scraped | Inline alert: "Seite konnte nicht ausgewertet werden. Bitte Felder manuell ausfuellen." The blank form remains open for manual entry | Error fragment | Frustrated but not blocked |
| Error — partial data | URL scraped but only partial data found | Pre-filled form with extracted values; missing fields are empty and highlighted | Partial pre-fill | Cautious |

**Key metrics**:
- Time-to-first-accommodation target: under 3 minutes including the import round-trip
- Error recovery: manual fallback available in all error cases

### 4.2 UX Design Decision: Inline Import Field, Not a Separate Page

The import field lives at the top of the existing accommodation create dialog/section. It does not introduce a new page or wizard step. This keeps the flow within the existing navigation structure.

When accommodation does not yet exist, the empty state shows a prominent import field. The user pastes a URL and clicks import. The server response swaps in the fully populated create form. The user saves directly.

When accommodation already exists, the edit dialog also includes the import field, collapsed inside a `<details>` element. This supports the case where Anna found a different property and wants to replace the existing one.

### 4.3 Desktop Wireframe — Empty State with Import Field

```
+----------------------------------------------------------+
|  [< Zurueck zur Reise]                                   |
|                                                          |
|  Unterkunft                                              |
|  Gruppenreise Alpen 2026                                 |
|                                                          |
|  +------------------------------------------------------+|
|  |  Noch keine Unterkunft hinterlegt.                   ||
|  |                                                      ||
|  |  URL aus Buchungsseite einfuegen:                    ||
|  |  [____________________________________________] [Im-] ||
|  |                                                 [por] ||
|  |                                                 [tie] ||
|  |                                                 [ren] ||
|  |                                                      ||
|  |  ── oder ──                                          ||
|  |                                                      ||
|  |  [Manuell erfassen]                                  ||
|  +------------------------------------------------------+|
+----------------------------------------------------------+
```

### 4.4 Desktop Wireframe — After Import Success: Pre-filled Editable Form

```
+----------------------------------------------------------+
|  Unterkunft                                              |
|  Gruppenreise Alpen 2026                                 |
|                                                          |
|  +------------------------------------------------------+|
|  |  Importiert von: booking.com/de/hotel/berghof ✓     ||
|  |  Daten pruefen und bei Bedarf korrigieren.           ||
|  |                                                      ||
|  |  Name *                                              ||
|  |  [Berghof Saalbach                               ]   ||
|  |                                                      ||
|  |  Adresse                                             ||
|  |  [Dorfstrasse 12, 5753 Saalbach-Hinterglemm     ]   ||
|  |                                                      ||
|  |  Buchungs-URL                                        ||
|  |  [https://www.booking.com/de/hotel/berghof      ]   ||
|  |                                                      ||
|  |  Check-in             Check-out                      ||
|  |  [2026-07-18     ]    [2026-07-25     ]              ||
|  |                                                      ||
|  |  Gesamtpreis (EUR)                                   ||
|  |  [1840.00           ]                                ||
|  |                                                      ||
|  |  Zimmer                                              ||
|  |  +------------------+--------+-------+-----------+  ||
|  |  | Bezeichnung      | Typ    | Betten| Preis/Nacht|  ||
|  |  +------------------+--------+-------+-----------+  ||
|  |  | [Zimmer 1      ] | [QUAD] |  [4] | [120.00  ] |  ||
|  |  | [Zimmer 2      ] | [DOUB] |  [2] | [95.00   ] |  ||
|  |  | [Zimmer 3      ] | [DOUB] |  [2] | [95.00   ] |  ||
|  |  +------------------+--------+-------+-----------+  ||
|  |  [+ Zimmer hinzufuegen]                              ||
|  |                                                      ||
|  |  [Unterkunft speichern]   [Abbrechen]                ||
|  +------------------------------------------------------+|
+----------------------------------------------------------+
```

### 4.5 Mobile Wireframe — Import Field (375px)

```
+---------------------------+
|  Unterkunft               |
|  Gruppenreise Alpen 2026  |
|                           |
|  +-----------------------+|
|  | Noch keine Unterkunft ||
|  | hinterlegt.           ||
|  |                       ||
|  | URL einfuegen:        ||
|  | [___________________] ||
|  |                       ||
|  | [Importieren        ] ||
|  |                       ||
|  | ── oder ──            ||
|  |                       ||
|  | [Manuell erfassen   ] ||
|  +-----------------------+|
+---------------------------+
```

### 4.6 Mobile Wireframe — After Import: Pre-filled Form (stacked, scrollable)

```
+---------------------------+
|  Importiert von           |
|  booking.com ✓            |
|  Pruefen und korrigieren. |
|                           |
|  Name *                   |
|  [Berghof Saalbach      ] |
|                           |
|  Adresse                  |
|  [Dorfstrasse 12, ...   ] |
|                           |
|  Buchungs-URL             |
|  [https://booking.com...] |
|                           |
|  Check-in                 |
|  [2026-07-18           ]  |
|                           |
|  Check-out                |
|  [2026-07-25           ]  |
|                           |
|  Gesamtpreis (EUR)        |
|  [1840.00              ]  |
|                           |
|  Zimmer                   |
|  +-----------------------+|
|  | Zimmer 1 | QUAD | 4  ||
|  | [120.00 EUR/Nacht   ]||
|  | Zimmer 2 | DOUB | 2  ||
|  | [95.00 EUR/Nacht    ]||
|  +-----------------------+|
|  [+ Zimmer hinzufuegen  ] |
|                           |
|  [Unterkunft speichern  ] |
|  [Abbrechen             ] |
+---------------------------+
```

### 4.7 Error State Wireframe

```
+------------------------------------------------------+
|  [role="alert"]                                      |
|  Seite konnte nicht ausgewertet werden.              |
|  Moegliche Ursachen: Seite nicht erreichbar,         |
|  Login erforderlich, oder kein Unterkunftsangebot    |
|  erkannt.                                            |
|                                                      |
|  Bitte Felder manuell ausfuellen.                    |
+------------------------------------------------------+

[blank form fields below — same layout as pre-filled form]
```

### 4.8 Component Spec — Import Field

**Component**: AccommodationImportField
**Page**: `/{tripId}/accommodation` (empty state) and edit dialog
**PicoCSS Base**: `<form>` with `<input type="url">` + `<button>`

#### States
| State | Visual | Trigger |
|-------|--------|---------|
| Default | URL input + "Importieren" button | Page load |
| Loading | Button shows `aria-busy="true"`, input disabled via `hx-disabled-elt` | After form submit |
| Success | Entire import section replaced by pre-filled form fragment | HTMX swap outerHTML |
| Partial success | Pre-filled form with missing fields empty, `aria-invalid="true"` on unfilled required fields | HTMX swap outerHTML |
| Error | `role="alert"` fragment + blank form below | HTMX swap outerHTML |

#### HTMX Interactions

```
<form hx-post="/{tripId}/accommodation/import-url"
      hx-target="#accommodation-import-section"
      hx-swap="outerHTML"
      hx-disabled-elt="find button[type=submit]">
  <label>
    <span th:text="#{accommodation.import.urlLabel}">URL einfuegen</span>
    <input type="url" name="url" required
           th:placeholder="#{accommodation.import.urlPlaceholder}">
  </label>
  <button type="submit" aria-busy="false"
          th:text="#{accommodation.import.action}">Importieren</button>
</form>
```

The server endpoint `POST /{tripId}/accommodation/import-url`:
- Returns the pre-filled form fragment on success (HTTP 200)
- Returns an error alert fragment on failure (HTTP 200 — HTMX processes all 2xx)
- Never saves to the database — it only returns HTML

The pre-filled form submits to `POST /{tripId}/accommodation` — the same endpoint as manual creation.

#### Room Editing in Import Preview

Rooms are rendered as an editable inline table inside the preview form. Each row has inputs for name, roomType (select), bedCount, and pricePerNight. An "add row" button appends a blank row via JavaScript (minimal, progressive enhancement only). On mobile, each room becomes a stacked card.

#### Accessibility
- Import button: `aria-label="Unterkunft von URL importieren"` when no visible context
- Loading state: `aria-live="polite"` region announces "Importiere Daten..." to screen readers
- Error state: `role="alert"` on the error fragment — announced immediately
- Pre-filled form: no special handling needed — standard form labels throughout

#### What the Scraper Should Extract

The server-side scraper targets these signals, in priority order:

| Field | Source signals |
|-------|----------------|
| Name | `og:title`, `schema.org/LodgingBusiness name`, `<h1>` |
| Address | `schema.org/PostalAddress`, `address` meta, visible address text |
| Check-in date | `schema.org/checkinTime`, `reservationFor/checkInDate`, URL query params |
| Check-out date | `schema.org/checkoutTime`, `reservationFor/checkOutDate`, URL query params |
| Total price | `schema.org/priceSpecification/price`, visible price element with currency context |
| Room types | `schema.org/accommodationFloorPlan`, room listing elements on property page |
| Bed counts | Adjacent to room type mentions, `numberOfBedrooms`, visible bed count text |
| URL | The input URL itself (always available) |

Amenities extraction is explicitly out of scope for the MVP import — amenities can be added manually after import.

---

## 5. S10-B: Kassenzettel-Scan / Receipt Photo OCR

### 5.1 User Journey

**Persona**: Participant (Lukas, shops for groceries at Lidl during the trip)
**Goal**: Add the grocery receipt to the shared expense without manual typing
**Trigger**: Lukas has just paid. He opens the app on his phone at the checkout.

| Phase | User Action | System Response | Touchpoint | Emotion |
|-------|-------------|-----------------|------------|---------|
| 1. Open Expense | Navigates to the trip's expense page on mobile | Belege section visible; "Beleg hinzufuegen" button and new "Kassenzettell scannen" button (camera icon) | Expense detail — Belege section | Hurried (at checkout) |
| 2. Capture | Taps "Kassenzettel scannen"; device camera app opens | `<input type="file" accept="image/*" capture="environment">` triggers native file picker or camera | Camera input | Focused |
| 3. Upload | Confirms the photo; upload starts automatically | Upload progress indicator replaces camera button; HTMX posts image to OCR endpoint | Upload progress | Waiting |
| 4. OCR + Vorschau | Waits 3–8 seconds | Server runs OCR, extracts total amount, date, possible store name; returns pre-filled form fragment | Loading state | Slightly impatient |
| 5. EDIT | Reviews pre-filled form; corrects amount if OCR got it wrong; selects paidBy | Pre-filled form with amount, date, description, category pre-selected | Editable form fragment | Relieved |
| 6. Speichern | Taps "Beleg einreichen" | Regular receipt create POST; receipt appears in list | Save | Satisfied, done |
| Error — illegible image | Photo too blurry or dark | "Kassenzettel konnte nicht erkannt werden. Bitte Felder manuell ausfullen." | Error alert | Frustrated |
| Error — no total found | OCR ran but found no price | Partial pre-fill: date if found, rest empty; user fills amount manually | Partial pre-fill | Cautious |

### 5.2 UX Design Decision: Camera Input Before the Add Dialog

The scan entry point is a button next to the existing "Beleg hinzufuegen" button. It does not replace manual entry — both options are always available. On desktop, the camera input opens a file picker. On mobile with `capture="environment"`, it triggers the camera directly.

The flow is:
1. User taps "Kassenzettel scannen"
2. This activates a hidden `<input type="file" accept="image/*" capture="environment">`
3. On file selection, HTMX posts the image to the OCR endpoint
4. The response replaces the add-receipt dialog content with a pre-filled form
5. User saves via the same `POST /{tripId}/receipts` endpoint

This approach works without JavaScript for the submit step. The file input trigger requires a minimal JavaScript click delegation (one line).

### 5.3 Mobile Wireframe — Belege Section with Scan Button (375px)

```
+---------------------------+
|  Belege                   |
|                           |
|  [Beleg hinzufuegen]      |
|  [Kassenzettel scannen]   |
|   (camera icon)           |
|                           |
|  +-- Tabellenzeile ------+|
|  | Rewe, 47,80 EUR ...   ||
|  +------------------------+|
+---------------------------+
```

### 5.4 Mobile Wireframe — Pre-filled Receipt Form After OCR (375px)

```
+---------------------------+
|  Beleg einreichen         |
|                           |
|  Erkannt: Lidl 12.07.2026 |
|  Betrag und Felder pruefen|
|                           |
|  Beschreibung             |
|  [Einkauf Lidl         ]  |
|                           |
|  Betrag (EUR) *           |
|  [47,80                ]  |
|  <- OCR-Ergebnis          |
|                           |
|  Datum *                  |
|  [2026-07-12           ]  |
|                           |
|  Bezahlt von *            |
|  [Lukas Mueller     v  ]  |
|                           |
|  Kategorie                |
|  [Lebensmittel      v  ]  |
|                           |
|  [Beleg einreichen     ]  |
|  [Abbrechen            ]  |
+---------------------------+
```

### 5.5 Desktop Wireframe — Scan Entry and Post-OCR Form

```
+----------------------------------------------------------+
|  Belege                                                  |
|                                                          |
|  [Beleg hinzufuegen]  [Kassenzettel scannen (icon)]      |
|                                                          |
|  -- Nach dem Scannen: Pre-filled Form erscheint --       |
|                                                          |
|  +------------------------------------------------------+|
|  |  Erkannt: Lidl, 12.07.2026. Bitte pruefen.          ||
|  |                                                      ||
|  |  Beschreibung                 Betrag (EUR)           ||
|  |  [Einkauf Lidl            ]   [47.80          ]      ||
|  |                                                      ||
|  |  Datum           Bezahlt von        Kategorie        ||
|  |  [2026-07-12]    [Lukas Mueller v]  [Lebensmittel v] ||
|  |                                                      ||
|  |  [Beleg einreichen]   [Abbrechen]                   ||
|  +------------------------------------------------------+|
+----------------------------------------------------------+
```

### 5.6 Upload Progress State

```
+------------------------------------------------------+
|  [Kassenzettel wird analysiert...]   (aria-busy=true) |
|  [=====                          ]   <progress>       |
+------------------------------------------------------+
```

The `<progress>` element is indeterminate (`<progress>` without `value` attribute) during upload + OCR. It switches to determinate once the response arrives. The loading state is replaced by the pre-filled form on success.

### 5.7 Error State Wireframe

```
+------------------------------------------------------+
|  [role="alert"]                                      |
|  Kassenzettel konnte nicht erkannt werden.           |
|  Moegliche Ursachen: Bild zu unscharf, zu dunkel     |
|  oder Text nicht lesbar.                             |
|                                                      |
|  Bitte Felder manuell ausfuellen oder erneut         |
|  fotografieren.                                      |
|                                                      |
|  [Erneut scannen]   [Manuell ausfuellen]             |
+------------------------------------------------------+
```

### 5.8 Component Spec — Receipt Scan Button

**Component**: ReceiptScanButton
**Page**: `expense/detail.html` — Belege section
**PicoCSS Base**: `<button>` + hidden `<input type="file">`

#### HTMX + File Upload Integration

```html
<!-- Visible trigger button -->
<button type="button"
        class="outline secondary"
        onclick="document.getElementById('scan-file-input').click()"
        th:text="#{expense.receipt.scan}">
  Kassenzettel scannen
</button>

<!-- Hidden file input — triggers camera on mobile -->
<input id="scan-file-input"
       type="file"
       accept="image/*"
       capture="environment"
       style="display:none"
       hx-post="/{tripId}/receipts/scan"
       hx-target="#scan-result-container"
       hx-swap="innerHTML"
       hx-encoding="multipart/form-data"
       hx-indicator="#scan-progress"
       onchange="htmx.trigger(this, 'change')">

<!-- Loading indicator (hidden by default, shown by HTMX during request) -->
<div id="scan-progress" class="htmx-indicator">
  <progress></progress>
  <small th:text="#{expense.receipt.scanning}">Analyse laeuft...</small>
</div>

<!-- Target container for the pre-filled form fragment or error alert -->
<div id="scan-result-container"></div>
```

The server endpoint `POST /{tripId}/receipts/scan`:
- Accepts `multipart/form-data` with the image file
- Runs OCR (Tesseract or cloud OCR API)
- Returns a pre-filled form fragment (same fields as the manual add-receipt dialog)
- On failure returns `role="alert"` error fragment
- Never saves a receipt — it only returns an HTML form

The pre-filled form submits to `POST /{tripId}/receipts` — the same endpoint as manual receipt creation.

#### States
| State | Visual | Trigger |
|-------|--------|---------|
| Default | "Kassenzettel scannen" button, outlined | Page load |
| File picker open | Native OS file picker or camera | Button click |
| Uploading | `aria-busy` on progress container, indeterminate `<progress>` | File selected |
| Success | Pre-filled form replaces scan container | HTMX swap |
| Error | `role="alert"` with retry and manual fallback options | HTMX swap |

#### Category Pre-selection Logic

The OCR endpoint should attempt category inference:
- Keywords suggesting GROCERIES: Rewe, Edeka, Lidl, Aldi, Penny, Kaufland, Netto, Supermarkt, Lebensmittel
- Keywords suggesting RESTAURANT: Restaurant, Gasthaus, Cafe, Bar, Wirtshaus, Speisekarte
- Keywords suggesting FUEL: Tankstelle, Shell, BP, Aral, Total, Liter, l Diesel, l Super
- Default fallback: OTHER

Category is a suggestion — user always overrides via the select field.

#### Accessibility
- Scan button: `aria-label="Kassenzettel fotografieren und Beleg vorausfuellen"` for screen readers
- Progress indicator: `aria-live="polite"` with text "Analyse laeuft..."
- Error alert: `role="alert"` announces immediately
- File input: visually hidden but accessible to keyboard with explicit label

---

## 6. S10-C: Settlement per Category (carry-over)

No changes from the previous analysis. The following is the retained specification.

### 6.1 Design Decision

A new `<article>` is added to the expense detail page in SETTLED state only. It appears below the party settlement section (S9-C). It shows a breakdown of all receipts grouped by expense category with totals and a percentage bar.

### 6.2 Wireframe — Category Breakdown (Desktop)

```
+----------------------------------------------------------+
|  Ausgaben nach Kategorie                                 |
|  +--------+-----------+-------------+------------------+ |
|  | Kateg. | Betrag    | Anteil      | Visualisierung   | |
|  +--------+-----------+-------------+------------------+ |
|  | Unterk.| 1.200 EUR | 52%         | [=====       ]   | |
|  | Lebenm.| 640 EUR   | 28%         | [===          ]   | |
|  | Transp.| 220 EUR   | 10%         | [=            ]   | |
|  | Sonstig| 230 EUR   | 10%         | [=            ]   | |
|  +--------+-----------+-------------+------------------+ |
+----------------------------------------------------------+
```

The `<progress value="52" max="100">` element is used for the bar — PicoCSS styles it automatically.

### 6.3 Mobile Wireframe — Category Cards (375px)

```
+---------------------------+
|  Ausgaben nach Kategorie  |
|                           |
|  +-- Unterkunft ---------+|
|  | 1.200,00 EUR    52%   ||
|  | [=====             ]  ||
|  +-----------------------+|
|  +-- Lebensmittel -------+|
|  | 640,00 EUR      28%   ||
|  | [===               ]  ||
|  +-----------------------+|
+---------------------------+
```

### 6.4 Visibility Rule

The category breakdown article is only rendered when `expense.status() == SETTLED`. Categories with zero receipts are shown at 0,00 EUR — they serve as an intentional audit trail confirming the category was tracked.

---

## 7. S10-D: Export Settlement as PDF (carry-over)

No changes from the previous analysis. The following is the retained specification.

### 7.1 Design Decision

A download link is placed in the header area of the expense detail article when the expense is in SETTLED state. It uses a browser-native download — no HTMX, no dialog.

```html
<a th:href="@{/{id}/expense/export/pdf(id=${tripId})}"
   role="button"
   class="secondary outline"
   download
   th:attr="download='abrechnung-' + ${tripName} + '-' + ${settlementYear} + '.pdf'"
   th:text="#{expense.export.pdf}">
  Als PDF exportieren
</a>
```

`Content-Disposition: attachment; filename="abrechnung-{tripName}-{year}.pdf"` is set server-side.

### 7.2 PDF Content Structure

1. Header: trip name, settlement date, total amount
2. Ausgleichszahlungen nach Reisepartei (party-level transfers)
3. Saldo pro Reisepartei (party balance table)
4. Ausgaben nach Kategorie (category breakdown with amounts)
5. Alle Belege (full receipt list, chronological, with paidBy and category)

### 7.3 Implementation Note

Flying Saucer (XHTML-to-PDF) is recommended over browser `print` CSS, because it produces consistent output independent of the client browser. A dedicated Thymeleaf print template (not PicoCSS) renders the PDF. The PDF endpoint is in the expense SCS, endpoint `GET /{tripId}/expense/export/pdf`.

---

## 8. HTMX Interaction Map — Iteration 10

| Trigger | Endpoint | hx-target | hx-swap | Notes |
|---------|----------|-----------|---------|-------|
| Import URL form submit | `POST /{tripId}/accommodation/import-url` | `#accommodation-import-section` | `outerHTML` | Returns pre-filled form or error alert |
| Pre-filled accommodation form submit | `POST /{tripId}/accommodation` | Full page reload (no HTMX) | n/a | Regular form POST, same as manual create |
| Scan file input change | `POST /{tripId}/receipts/scan` | `#scan-result-container` | `innerHTML` | Multipart upload; returns pre-filled form or error |
| Pre-filled receipt form submit | `POST /{tripId}/receipts` | `#receipts` | `innerHTML` | Same endpoint as manual receipt add |

---

## 9. i18n Keys

### 9.1 Accommodation URL Import (trips SCS)

| Key | DE | EN |
|-----|----|----|
| `accommodation.import.urlLabel` | URL aus Buchungsseite einfuegen | Paste booking URL |
| `accommodation.import.urlPlaceholder` | https://www.booking.com/... | https://www.booking.com/... |
| `accommodation.import.action` | Importieren | Import |
| `accommodation.import.success` | Importiert von {0}. Daten pruefen und speichern. | Imported from {0}. Review and save. |
| `accommodation.import.partial` | Einige Felder konnten nicht erkannt werden. Bitte pruefen. | Some fields could not be extracted. Please review. |
| `accommodation.import.error` | Seite konnte nicht ausgewertet werden. Bitte Felder manuell ausfuellen. | Page could not be parsed. Please fill in manually. |
| `accommodation.import.orManual` | oder | or |
| `accommodation.import.manualAction` | Manuell erfassen | Enter manually |

### 9.2 Receipt OCR Scan (expense SCS)

| Key | DE | EN |
|-----|----|----|
| `expense.receipt.scan` | Kassenzettel scannen | Scan receipt |
| `expense.receipt.scanning` | Analyse laeuft... | Scanning... |
| `expense.receipt.scan.success` | Erkannt: {0}. Bitte Felder pruefen. | Recognized: {0}. Please review. |
| `expense.receipt.scan.error` | Kassenzettel konnte nicht erkannt werden. Bitte Felder manuell ausfuellen. | Receipt could not be recognized. Please fill in manually. |
| `expense.receipt.scan.retry` | Erneut scannen | Scan again |
| `expense.receipt.scan.manualFallback` | Manuell ausfuellen | Fill in manually |

### 9.3 Category Breakdown (expense SCS, carry-over)

| Key | DE | EN |
|-----|----|----|
| `expense.categoryBreakdown` | Ausgaben nach Kategorie | Expenses by Category |
| `expense.categoryBreakdown.share` | Anteil | Share |
| `expense.categoryBreakdown.receiptCount` | Belege | Receipts |

### 9.4 PDF Export (expense SCS, carry-over)

| Key | DE | EN |
|-----|----|----|
| `expense.export.pdf` | Als PDF exportieren | Export as PDF |

---

## 10. Journey Map: Accommodation URL Import

```markdown
## User Journey: Accommodation URL Import
**Persona**: Organizer
**Goal**: Create a complete Accommodation record from a vacation rental URL in under 3 minutes
**Trigger**: Organizer has found a rental property on booking.com or Ferienhausmiete.de

### Journey Phases

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Navigate | Opens trip, clicks "Unterkunft" | Accommodation page — empty state with import field prominent | `/accommodation` empty state | Curious | Unclear what to do first | Make import field the visual hero of the empty state |
| 2. Copy URL | Switches to browser tab with booking.com, copies URL | — | Browser tab | Neutral | Tab-switching on mobile is friction | Note: on mobile the URL may come from share sheet (future) |
| 3. Paste + Submit | Pastes URL into import field, clicks "Importieren" | Loading state; button aria-busy | Import form | Neutral | Wrong URL format gives no early feedback | URL validation (format check) before submit |
| 4. Wait | Waits for scraping (2–5 seconds) | Progress indicator | Loading state | Slightly impatient | Long wait on slow rental sites | Show partial results as soon as name is found (future) |
| 5. Review | Reads pre-filled form; checks name, price, rooms | Editable form with scraped values | Pre-filled form | Relieved | Incorrect room types are not obvious | Highlight fields that required inference (e.g., subtle background tint) |
| 6. Correct | Fixes wrong room type in one room | Inline select change | Room row input | Engaged | Adding a missing room requires clicking "+ Zimmer hinzufuegen" | One-click row addition |
| 7. Save | Clicks "Unterkunft speichern" | Redirect to accommodation detail view with full room list | Accommodation detail | Satisfied | None | — |
```

---

## 11. Journey Map: Kassenzettel-Scan

```markdown
## User Journey: Kassenzettel-Scan
**Persona**: Participant
**Goal**: Submit a grocery receipt to the shared expense while still at the store
**Trigger**: Participant has just paid at the checkout; has the physical receipt

### Journey Phases

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|-------------|-----------------|------------|---------|-------------|---------------|
| 1. Open app | Navigates to the trip expense page | Belege section; "Kassenzettel scannen" button visible | `/expense/{tripId}` | Hurried | Finding the expense link on mobile | PWA install (S9-F) reduces navigation friction |
| 2. Tap Scan | Taps "Kassenzettel scannen" | Native camera or file picker opens | OS camera | Focused | Lighting at checkout is often poor | Show a "Gut beleuchten" tip text before camera opens |
| 3. Photograph | Photographs receipt or selects from gallery | File selected; upload starts | Camera / gallery | Careful | Receipt crumpled or wet → illegible photo | Allow retry without losing context |
| 4. Wait | Waits for OCR (3–8 seconds) | Indeterminate progress bar; "Analyse laeuft..." | Loading indicator | Impatient | Store WiFi may be slow | Compress image client-side before upload (progressive enhancement) |
| 5. Review amount | Sees pre-filled form; OCR total is usually correct for German supermarket receipts | Pre-filled form; "Betrag" field highlighted | Pre-filled form | Relieved | If total wrong, user must correct it carefully | Visually distinguish OCR-sourced vs. empty fields |
| 6. Select paidBy | Selects own name from dropdown | — | paidBy select | Routine | Self-selection is the common case — should default to the current user | Server should default paidBy to the current authenticated participant |
| 7. Submit | Taps "Beleg einreichen" | Receipt appears in list; dialog closes | Expense list | Satisfied | — | — |
```

---

## 12. Design Decisions and Open Questions

### Decision: Import Never Auto-Saves

The import endpoint returns only HTML — it never saves to the database. The user must always submit the save form explicitly. This ensures the EDIT step is mandatory and the user cannot bypass review.

### Decision: Rooms in Import Preview Are Editable Inline

Rather than asking the user to add rooms manually after saving, the import preview form includes an inline editable rooms table. The user can correct room types and bed counts before the first save. This reduces the round-trip count from two (import → save → edit rooms) to one.

### Decision: paidBy Defaults to Current User in Scan Form

The OCR endpoint knows the authenticated user. It should pre-select the current participant in the `paidBy` field. This is the correct default for the participant-at-checkout scenario.

### Decision: Recipe Import from URL is Deferred

Recipe import (Koch-Rezept von URL) is explicitly excluded from Iteration 10. It is low-priority relative to accommodation and receipt import. Scheduled for Iteration 11+ at earliest.

### Open Question: OCR Technology

The receipt OCR implementation approach is not specified in this design doc. Options include Tesseract (self-hosted, Java binding), cloud OCR (Google Vision, AWS Textract), or a structured receipt parser API. The architect must choose during S10-B implementation. The UX design is agnostic to the backend OCR technology.

### Open Question: Accommodation Scraping Anti-Bot

Booking.com and Ferienhausmiete.de may block server-side scraping (Cloudflare, JS rendering). If scraping fails consistently, the fallback is manual entry — no UX change needed. The architect must assess feasibility during S10-A implementation.
