# Component Specifications: Expense SCS — Iteration 6

**Version**: Iteration 6 design (v0.7.0)
**Status**: Design specification — no code written
**Companion documents**:
- User journey maps: `docs/design/journeys/expense-iteration6-flows.md`
- Feedback system: `docs/design/components/feedback-system.md`

---

## 1. Receipt Status Badges

**Page**: Expense detail — receipt list rows
**PicoCSS Base**: `<mark>` element (styled via CSS class modifier)

### Status Values and Visual Treatment

| Status (code) | Label (DE) | Label (EN) | Badge style | Colour intent |
|---------------|-----------|------------|-------------|---------------|
| `DRAFT` | Entwurf | Draft | `<mark class="status-draft">` | Neutral grey |
| `SUBMITTED` | Eingereicht | Submitted | `<mark class="status-submitted">` | Blue (action required) |
| `APPROVED` | Genehmigt | Approved | `<mark class="status-approved">` | Green |
| `REJECTED` | Abgelehnt | Rejected | `<mark class="status-rejected">` | Red |

**Rule**: Do not rely on colour alone. Each badge uses a text prefix that is meaningful without colour:
- "Eingereicht" and "Genehmigt" / "Abgelehnt" are self-describing words.
- On the rejection badge, a `(!)` indicator is appended in the DOM for screen readers: `<mark class="status-rejected">Abgelehnt <span aria-hidden="true">(!)</span></mark>`.

### CSS additions (`static/css/style.css` in expense module)

```css
/* Receipt status badges — build on PicoCSS <mark> defaults */
.status-draft      { background: var(--pico-muted-background); color: var(--pico-muted-color); }
.status-submitted  { background: var(--pico-primary-background); color: var(--pico-primary); }
.status-approved   { background: #dcfce7; color: #166534; }
.status-rejected   { background: #fee2e2; color: #991b1b; }

/* Category badges */
.category-badge    { background: var(--pico-muted-background); color: var(--pico-muted-color); font-size: 0.8em; }
```

---

## 2. Receipt List with Review State

**Page**: Expense detail — `#receipts` fragment (`expense/receipts.html`)
**PicoCSS Base**: `<table>` with inline action rows

### Layout (desktop — 7 columns)

```
+----------------+---------+-----------+----------+--------------+--------+----------+
| Beschreibung   | Betrag  | Kategorie | Datum    | Bezahlt von  | Status | Aktionen |
+----------------+---------+-----------+----------+--------------+--------+----------+
| Supermarkt     | 42,50 € | Lebensm.  | 14.07.   | Alice        | GENEH. | —        |
| Hotel Alpin    | 320,00€ | Unterkunft| 14.07.   | Bob          | EINGR. | [✓] [✗]  |
| Gondel         | 18,00 € | Aktivität | 15.07.   | Alice        | ABGEL. | [Bearbei]|
+----------------+---------+-----------+----------+--------------+--------+----------+
```

**Column rules:**
- "Aktionen" column renders conditionally based on `expense.status()` AND receipt status AND current user role/identity:
  - Expense OPEN, receipt SUBMITTED, current user is organizer AND not submitter → show "Genehmigen" and "Ablehnen" buttons
  - Expense OPEN, receipt REJECTED, current user is submitter → show "Bearbeiten" button
  - Expense OPEN, receipt DRAFT, current user is submitter → show "Einreichen" and "Entfernen" buttons
  - Expense SETTLED → no action column
  - Receipt APPROVED → no actions (immutable)
- "Aktionen" column header is hidden on SETTLED expense (`th:if` removed)

### Review action buttons (inline in Aktionen `<td>`)

```
[ Genehmigen ]  [ Ablehnen ]
```

Both are `<button>` elements using HTMX:
- "Genehmigen": `hx-post="/{tid}/receipts/{rid}/approve"` → target `#receipts` → swap `innerHTML`
- "Ablehnen": opens inline rejection form (see Section 3 below)
- Both buttons: `class="outline"`, small size via `style="padding:0.25rem 0.75rem"`

### Rejection reason row (inline expansion)

When the organizer taps "Ablehnen", the receipt row expands below it to show a rejection reason form. This avoids a full dialog and keeps the user in context.

```
+--------------------------------------------------------------------+
| [Ablehnungsgrund eingeben                               ]          |
| [Grund: Beleg bereits vorhanden ▾] [optional shortcut]            |
| [ Ablehnen bestätigen ]  [ Abbrechen ]                             |
+--------------------------------------------------------------------+
```

**Pattern**: The "Ablehnen" button triggers `hx-get="/{tid}/receipts/{rid}/reject-form"` → target `#reject-row-{rid}` → swap `innerHTML`. The server returns a `<tr id="reject-row-{rid}">` containing the reason form. This is an "inline expansion" pattern — no dialog needed.

The rejection reason form then `hx-post="/{tid}/receipts/{rid}/reject"` → target `#receipts` → swap `innerHTML` (full receipts refresh).

### Mobile layout

Below 640px, the table collapses to a card list. Each receipt becomes an `<article>`:

```
+-----------------------------------------------+
| Supermarkt                   [ Eingereicht ]  |
| 42,50 EUR · Lebensmittel · 14.07.2026          |
| Bezahlt von: Alice                             |
+-----------------------------------------------+
| Hotel Alpin                  [ Eingereicht ]  |
| 320,00 EUR · Unterkunft · 14.07.2026           |
| Bezahlt von: Bob                               |
| [ Genehmigen ]  [ Ablehnen ]                   |
+-----------------------------------------------+
```

This is achieved with responsive CSS on the table (hide table chrome, display rows as blocks below breakpoint) — the same technique used in the trips list.

### Empty state

```html
<p th:if="${receipts.isEmpty()}" th:text="#{expense.receipt.empty}">
    Noch keine Belege vorhanden.
</p>
```

### Review queue indicator (section header)

When one or more receipts are SUBMITTED, the section header shows a count badge:

```
Belege  [ 2 zu prüfen ]
```

Markup: `<h2>#{expense.receipts} <mark class="status-submitted" th:if="${submittedCount > 0}">...</mark></h2>`

---

## 3. Add / Edit Receipt Dialog

**Page**: Expense detail — `<dialog id="add-receipt-dialog">`
**PicoCSS Base**: `<dialog>` → `<article>` inside

### Form fields (OPEN expense only)

```
+--------------------------------------------------+
|  Beleg hinzufügen                            [X] |
+--------------------------------------------------+
|  Beschreibung                                    |
|  [ Hotelrechnung                              ]  |
|                                                  |
|  Kategorie                                       |
|  [ Unterkunft                              ▼ ]  |
|                                                  |
|  [ Betrag          ]   [ Datum              ]    |
|  [ 320.00          ]   [ 2026-07-14         ]    |
|                                                  |
|  Bezahlt von                                     |
|  [ Alice Müller                            ▼ ]  |
|                                                  |
|  -- Unterkunft-Aufteilung --                     |  (conditional: category = Unterkunft)
|  Gesamtkosten werden nach Übernachtungen         |
|  aufgeteilt. Vorschau:                           |
|                                                  |
|  Alice Müller    5 Nächte   160,00 EUR           |
|  Bob Meier       5 Nächte   160,00 EUR           |
|                                                  |
|             [ Abbrechen ]  [ Beleg hinzufügen ]  |
+--------------------------------------------------+
```

### Category `<select>` options

Order follows typical trip expense frequency:

| Value | DE label | EN label |
|-------|----------|----------|
| `GROCERIES` | Lebensmittel | Groceries |
| `RESTAURANT` | Restaurant | Restaurant |
| `TRANSPORT` | Transport | Transport |
| `ACCOMMODATION` | Unterkunft | Accommodation |
| `ACTIVITY` | Aktivität | Activity |
| `FUEL` | Kraftstoff | Fuel |
| `HEALTH` | Gesundheit | Health |
| `OTHER` | Sonstiges | Other |

### Accommodation sub-form (conditional section)

Appears below the date field when category = `ACCOMMODATION`. Triggered by:

```html
<select name="category" hx-get="/{tid}/receipts/accommodation-preview"
        hx-target="#accommodation-section"
        hx-swap="innerHTML"
        hx-trigger="change[this.value == 'ACCOMMODATION']">
```

On any other category selection, `hx-trigger="change[this.value != 'ACCOMMODATION']"` on the same select clears the section.

The `#accommodation-section` div is empty by default. The server returns a preview fragment when `ACCOMMODATION` is selected, containing the per-participant nights and calculated share. If stay periods are missing, it returns an error fragment instead.

**Accommodation preview fragment** (`expense/accommodation-preview.html`):

```
+--------------------------------------------+
| Aufteilung nach Übernachtungen             |
| Betrag:  [ 320.00 ] EUR (live preview)     |
|                                            |
| Name           Nächte   Anteil    Betrag   |
| Alice Müller   5        50 %      160,00 € |
| Bob Meier      5        50 %      160,00 € |
+--------------------------------------------+
```

The preview table re-fetches when the amount field changes: `hx-trigger="input delay:500ms"` on the amount field, using the same endpoint.

**Error state — missing stay periods:**

```
+--------------------------------------------+
|  [!] Fehler: Aufenthaltszeiträume fehlen  |
|  für folgende Teilnehmer: Anna, Klaus.     |
|  Bitte zuerst die Aufenthaltszeiten        |
|  im Reise-Detail eintragen.               |
+--------------------------------------------+
```

Markup: `<p role="alert" class="error-notice">`. Submit button is disabled when this error is shown (`hx-disabled-elt` or server-side validation on POST).

### HTMX pattern for dialog form

```
hx-post="/{id}/receipts"
hx-target="#receipts"
hx-swap="innerHTML"
hx-disabled-elt="find [type=submit]"
hx-on::after-request="if(event.detail.successful){ this.reset(); document.getElementById('add-receipt-dialog').close(); }"
```

Inline feedback appears at the top of the `#receipts` fragment on error (server returns the fragment with an error notice at top). Success: dialog closes, receipt list refreshes with new receipt visible.

---

## 4. Settlement Summary Section

**Page**: Expense detail — new section, renders only when `expense.status() == 'SETTLED'`
**PicoCSS Base**: `<article>` with inner `<figure>` tables

### Overall layout

```
+=========================================================+
|  ABRECHNUNG ABGESCHLOSSEN                               |
|                                                         |
|  Gesamtausgaben: 820,50 EUR                             |
|                                                         |
|  -- Ausgaben nach Kategorie --                          |
|  Unterkunft        320,00 EUR   39 %                    |
|  Lebensmittel      180,00 EUR   22 %                    |
|  Restaurant        145,50 EUR   18 %                    |
|  Transport         175,00 EUR   21 %                    |
|                                                         |
|  -- Saldo pro Teilnehmer --                             |
|  Alice Müller      +245,00 EUR  (erhält)                |
|  Bob Meier         −120,00 EUR  (schuldet)              |
|  Clara Schmidt     −125,00 EUR  (schuldet)              |
|                                                         |
|  -- Überweisungen --                                    |
|  Bob Meier zahlt Alice Müller       120,00 EUR          |
|  Clara Schmidt zahlt Alice Müller   125,00 EUR          |
|                                                         |
+=========================================================+
```

### Sub-components

#### 4a. Category Breakdown Table

`<figure>` with `<table>`:

| Column | Thymeleaf binding |
|--------|------------------|
| Kategorie | `#{expense.category.{category}}` |
| Betrag | `#numbers.formatDecimal(amount, 1, 2)` + " EUR" |
| Anteil | `#numbers.formatDecimal(pct, 1, 0)` + " %" |

Totals row: `<tfoot>` with class `total-row`.

#### 4b. Balance Table

Same structure as current balance section, with improved visual treatment:

- Positive balances: `class="balance-positive"` (green, defined in style.css — replacing current `style="color:green"`)
- Negative balances: `class="balance-negative"` (red)
- Zero balance: `class="balance-zero"` (muted)

Screen-reader prefix added inside balance cell:
```html
<span class="sr-only">erhält</span> +245,00 EUR
```

#### 4c. Transfer List (Debt Simplification)

The transfer list uses **plain language** to make instructions unambiguous for non-technical users.

PicoCSS Base: `<ul>` with custom class `.transfer-list`

```html
<ul class="transfer-list">
    <li th:each="transfer : ${transfers}">
        <strong th:text="${transfer.debtorName}">Bob Meier</strong>
        <span th:text="#{expense.transfer.pays}">zahlt</span>
        <strong th:text="${transfer.creditorName}">Alice Müller</strong>
        <span th:text="${#numbers.formatDecimal(transfer.amount, 1, 2)}">120,00</span> EUR
    </li>
</ul>
```

CSS:
```css
.transfer-list li {
    padding: 0.5rem 0;
    border-bottom: 1px solid var(--pico-muted-border-color);
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
    align-items: baseline;
}
```

**Debt simplification algorithm**: The server pre-calculates a minimal transfer set (greedy creditor-debtor matching). This is a presentation-layer concern for the controller/service, not the domain aggregate. Design note: the `Expense` aggregate already provides `calculateBalances()`. The settlement summary representation calculates transfers from the balance map.

---

## 5. Settle Confirmation Dialog

**Page**: Expense detail — replaces current `onclick="return confirm(...)"`
**PicoCSS Base**: `<dialog>`

### Trigger rule

The settle button is only enabled when:
1. Expense status is `OPEN`
2. At least one receipt exists
3. No receipts in `SUBMITTED` state (all are `APPROVED` or `REJECTED`)

If condition 3 is not met, the button is replaced by a warning:

```
+--------------------------------------------------+
|  [!] 2 Belege sind noch nicht geprüft.           |
|  Bitte alle Belege prüfen, bevor die             |
|  Abrechnung abgeschlossen wird.                  |
+--------------------------------------------------+
```

### Confirmation dialog wireframe

```
+--------------------------------------------------+
|  Abrechnung abschliessen?                    [X] |
+--------------------------------------------------+
|  Möchten Sie die Abrechnung wirklich             |
|  abschliessen? Dies kann nicht rückgängig        |
|  gemacht werden.                                 |
|                                                  |
|  Belege: 8   Gesamtbetrag: 820,50 EUR            |
|                                                  |
|        [ Abbrechen ]   [ Abschliessen ]          |
+--------------------------------------------------+
```

**HTMX pattern**: The settle form uses `hx-post="/{id}/settle"` with a full page redirect via `HX-Redirect` response header (since settle changes the entire page state). No partial swap — the whole detail page reloads in settled state.

The dialog is opened by the button via `onclick="document.getElementById('settle-confirm-dialog').showModal()"`. The form inside the dialog submits normally.

---

## 6. HTMX Interaction Map

### Summary of all HTMX endpoints (new for Iteration 6)

| User action | Trigger | HTMX method + path | Target | Swap |
|-------------|---------|-------------------|--------|------|
| Submit receipt for review | Form submit in dialog | `hx-post /{tid}/receipts` | `#receipts` | `innerHTML` |
| Approve receipt | Button click | `hx-post /{tid}/receipts/{rid}/approve` | `#receipts` | `innerHTML` |
| Open rejection form | Button click | `hx-get /{tid}/receipts/{rid}/reject-form` | `#reject-row-{rid}` | `innerHTML` |
| Confirm rejection | Form submit inline | `hx-post /{tid}/receipts/{rid}/reject` | `#receipts` | `innerHTML` |
| Edit rejected receipt | Button click | `hx-get /{tid}/receipts/{rid}/edit-form` | (opens dialog) | `outerHTML` on dialog |
| Re-submit edited receipt | Form submit in dialog | `hx-put /{tid}/receipts/{rid}` | `#receipts` | `innerHTML` |
| Select ACCOMMODATION category | `change` event on select | `hx-get /{tid}/receipts/accommodation-preview` | `#accommodation-section` | `innerHTML` |
| Amount field change (accomm.) | `input delay:500ms` | `hx-get /{tid}/receipts/accommodation-preview` | `#accommodation-section` | `innerHTML` |
| Remove receipt (DRAFT only) | Button click | `hx-delete /{tid}/receipts/{rid}` | `#receipts` | `innerHTML` |
| Load expense detail (initial) | Full page load | `GET /{tid}` | Full page | Full |
| Settle (confirmed) | Form submit in dialog | `hx-post /{tid}/settle` | Full page via `HX-Redirect` | Redirect |

### Loading states

All HTMX write actions use:
```html
hx-disabled-elt="find [type=submit]"
```
on the parent form. The submit button shows `aria-busy="true"` while the request is in flight (set via `htmx:beforeRequest` / `htmx:afterRequest` event handlers, or via PicoCSS `aria-busy` support on the button element directly).

---

## 7. i18n Key Reference

### New keys required — Expense SCS (`messages_de.properties` / `messages_en.properties`)

**Receipt status**

| Key | DE | EN |
|-----|----|----|
| `expense.receipt.status.DRAFT` | Entwurf | Draft |
| `expense.receipt.status.SUBMITTED` | Eingereicht | Submitted |
| `expense.receipt.status.APPROVED` | Genehmigt | Approved |
| `expense.receipt.status.REJECTED` | Abgelehnt | Rejected |
| `expense.receipt.reviewPending` | {0} zu prüfen | {0} to review |
| `expense.receipt.ownReceipt` | Eigener Beleg | Own receipt |

**Receipt review actions**

| Key | DE | EN |
|-----|----|----|
| `expense.receipt.approve` | Genehmigen | Approve |
| `expense.receipt.reject` | Ablehnen | Reject |
| `expense.receipt.rejectReason` | Ablehnungsgrund | Rejection reason |
| `expense.receipt.rejectReason.placeholder` | z.B. Betrag stimmt nicht | e.g. Amount is incorrect |
| `expense.receipt.rejectConfirm` | Ablehnen bestätigen | Confirm rejection |
| `expense.receipt.edit` | Bearbeiten | Edit |
| `expense.receipt.resubmit` | Erneut einreichen | Resubmit |
| `expense.receipt.approvedSuccess` | Beleg genehmigt. | Receipt approved. |
| `expense.receipt.rejectedSuccess` | Beleg abgelehnt. | Receipt rejected. |
| `expense.receipt.resubmittedSuccess` | Beleg erneut eingereicht. | Receipt resubmitted. |
| `expense.receipt.cannotReviewOwnReceipt` | Sie können Ihren eigenen Beleg nicht prüfen. | You cannot review your own receipt. |

**Categories**

| Key | DE | EN |
|-----|----|----|
| `expense.receipt.category` | Kategorie | Category |
| `expense.category.GROCERIES` | Lebensmittel | Groceries |
| `expense.category.RESTAURANT` | Restaurant | Restaurant |
| `expense.category.TRANSPORT` | Transport | Transport |
| `expense.category.ACCOMMODATION` | Unterkunft | Accommodation |
| `expense.category.ACTIVITY` | Aktivität | Activity |
| `expense.category.FUEL` | Kraftstoff | Fuel |
| `expense.category.HEALTH` | Gesundheit | Health |
| `expense.category.OTHER` | Sonstiges | Other |

**Accommodation split**

| Key | DE | EN |
|-----|----|----|
| `expense.accommodation.title` | Unterkunft — Aufteilung nach Übernachtungen | Accommodation — Split by Nights |
| `expense.accommodation.preview` | Vorschau der Kostenaufteilung | Cost split preview |
| `expense.accommodation.nights` | Nächte | Nights |
| `expense.accommodation.share` | Anteil | Share |
| `expense.accommodation.missingStayPeriods` | Aufenthaltszeiträume fehlen für: {0}. Bitte im Reise-Detail eintragen. | Stay periods missing for: {0}. Please set them in the trip detail. |

**Settlement summary**

| Key | DE | EN |
|-----|----|----|
| `expense.settle.confirm.title` | Abrechnung abschliessen? | Settle Expenses? |
| `expense.settle.confirm.message` | Möchten Sie die Abrechnung wirklich abschliessen? Dies kann nicht rückgängig gemacht werden. | Are you sure you want to settle the expenses? This cannot be undone. |
| `expense.settle.confirm.receiptCount` | Belege: {0} | Receipts: {0} |
| `expense.settle.pendingReview` | {0} Beleg(e) noch nicht geprüft. Bitte alle Belege prüfen, bevor die Abrechnung abgeschlossen wird. | {0} receipt(s) not yet reviewed. Please review all receipts before settling. |
| `expense.categoryBreakdown` | Ausgaben nach Kategorie | Expenses by Category |
| `expense.categoryBreakdown.share` | Anteil | Share |
| `expense.transfer.title` | Überweisungen | Transfers |
| `expense.transfer.pays` | zahlt | pays |
| `expense.transfer.empty` | Keine Überweisungen notwendig. | No transfers needed. |

---

## 8. Edge Cases and Error States

### Empty states

| Situation | Display | i18n key |
|-----------|---------|----------|
| No receipts | `<p>` notice in receipt section | `expense.receipt.empty` (existing) |
| No transfers needed (all balances zero) | `<p>` in transfer list section | `expense.transfer.empty` |
| Stay periods missing for accommodation | `<p role="alert" class="error-notice">` in accommodation section | `expense.accommodation.missingStayPeriods` |
| All receipts rejected, none approved | Warning before settle button | new key |

### Permission errors (server-enforced, UI reflects)

| Situation | UI behaviour |
|-----------|-------------|
| Participant tries to approve own receipt | "Genehmigen" / "Ablehnen" buttons not rendered; "Eigener Beleg" label shown |
| Non-organizer tries to approve | Server returns 403; GlobalExceptionHandler returns error fragment into `#receipts` |
| Receipt not in SUBMITTED state when approving | Server returns 400; error fragment with `role="alert"` |

### Rejected receipt feedback for submitter

The receipt row for a REJECTED receipt expands below the status badge to show the reason:

```
+--------------------------------------------------------------+
| Restaurant Alpin  38,50 €  Restaurant  15.07.  Bob  ABGEL.  |
| Ablehnungsgrund: Betrag stimmt nicht überein.   [ Bearbeiten ]|
+--------------------------------------------------------------+
```

The reason text is shown in a `<small>` element below the status badge in the actions column or as an extra row beneath on mobile card layout.

### Receipt in non-editable state

Once APPROVED, receipts cannot be edited or removed. The "Aktionen" cell is empty or omitted for APPROVED receipts. This is enforced server-side; the UI simply never renders the action buttons for APPROVED receipts.
