# UX Designer Agent Memory

## Design System
- PicoCSS 2 via CDN — semantic HTML auto-styled
- HTMX 2.0.8 via CDN — progressive enhancement
- Thymeleaf server-side rendering
- Color scheme: PicoCSS default blue (not teal)
- Responsive CSS theme implemented (5a9107b)
- Custom CSS in `static/css/style.css` (both iam and trips) — `.error`, section spacing, danger zone, footer

## Existing Pages
- Landing page, Sign-up, Login (Keycloak)
- Dashboard (`/iam/dashboard`) — members table, companions table, danger zone; all HTMX partial updates
- Trip list (`/trips/`) — pending invitation cards at top, trip table below
- Trip detail (`/trips/{id}`) — flat sections: metadata, participants table, invitations section with inline forms
- Trip create form (`/trips/new`) — simple name/description/dates form
- Invitation management — inline in trip detail and trip list

## Known UX Problems (documented from user feedback, v0.4.0)
See [project memory on UX issues](../../../.claude/projects/-Users-t-klingler-repos-privat-travelmate/memory/MEMORY.md) for history.

### Critical gaps (not addressed by architect's proposals)
1. Companions (Dependents) cannot set stay periods on trip detail — no row for them in participants table
2. The participant stay-period form is inline in a `<td>` — breaks on mobile, button label wraps (reported bug)

### Identified in code review
- External invite feedback uses hidden `<span>` toggled by raw JS + `style="color:green"` — not design system, not accessible
- All enum values (PLANNING, CONFIRMED, etc.) are displayed raw to users — need localised labels
- Stay-period date inputs have no associated `<label>` elements — accessibility violation
- Invitation section uses `<h3>` headings to introduce always-visible forms — not intuitive

## Feedback Pattern Decision (documented)
- Toast notifications are NOT the right primary feedback mechanism for this app
- Inline fragment feedback (server-rendered `role="alert"` in HTMX swap target) is the correct pattern
- Toast is acceptable only as additive/secondary channel for non-critical transient success
- See `/docs/design/components/feedback-system.md` for full rationale

## Iteration 9 UX Analysis (2026-03-18) — SHIPPED in v0.9.0
- Full analysis at `/docs/design/wireframes/iteration-9-ux-analysis.md`
- All 6 stories shipped: Accommodation page + room assignment (S9-A/B), Party-level settlement (S9-C), Advance payments (S9-D), Re-submit rejected receipt (S9-E), PWA manifest (S9-F)

## Iteration 10 UX Analysis (2026-03-19) — REPLANNED
- Full analysis at `/docs/design/wireframes/iteration-10-ux-analysis.md`
- CORRECTED priority: S10-A (Accommodation URL Import) > S10-B (Kassenzettel-Scan / Receipt OCR) > S10-C (Category Breakdown) > S10-D (PDF Export)
- Recipe Import from URL is DEFERRED (not in Iteration 10 — low priority per user)

### Shared Import Pipeline Pattern
All import features follow: Input → Analyse → Vorschau → EDIT → Speichern
- EDIT step is mandatory — server never auto-saves imported data
- Import endpoint returns HTML fragment only; save endpoint is the regular create endpoint
- Error path always offers manual entry fallback

### Accommodation URL Import (S10-A — US-TRIPS-061) — HIGH priority
- Import field in empty state of accommodation page (prominent); in `<details>` in edit dialog
- hx-post="/{tripId}/accommodation/import-url", hx-target="#accommodation-import-section", hx-swap="outerHTML"
- Success: full pre-filled editable form replaces the import section (name, address, URL, check-in/out, total price, inline editable rooms table)
- Failure: role="alert" fragment + blank form for manual entry
- Loading: aria-busy on import button, hx-disabled-elt on submit
- Rooms are editable inline in the preview form (user can correct type, bed count, price before saving)
- Scraper targets: og:title, schema.org/LodgingBusiness, schema.org/PostalAddress, schema.org/priceSpecification, room floor plans

### Kassenzettel-Scan / Receipt OCR (S10-B) — MEDIUM priority
- "Kassenzettel scannen" button (camera icon) next to "Beleg hinzufuegen" in expense Belege section
- Camera input: `<input type="file" accept="image/*" capture="environment">` — mobile camera, desktop file picker
- hx-post="/{tripId}/receipts/scan", hx-target="#scan-result-container", hx-swap="innerHTML", hx-encoding="multipart/form-data"
- Success: pre-filled receipt form (description, amount, date, category pre-selected by keyword inference, paidBy defaulting to current user)
- Failure: role="alert" + retry + manual fallback
- Loading: indeterminate `<progress>` with aria-live="polite"
- OCR technology choice delegated to architect (Tesseract / cloud OCR)

### Settlement per Category (S10-C — US-EXP-032) — carry-over
- New `<article>` on SETTLED expense page, below party settlement section (Iter 9)
- Table on desktop: Kategorie | Betrag | Anteil am Gesamt | `<progress>` bar
- Mobile: `<article>` card per category with progress bar
- Categories with 0 receipts shown at 0,00 EUR (intentional audit trail)
- Only rendered in SETTLED state

### Export Settlement as PDF (S10-D — US-EXP-033) — carry-over
- `<a href="/{tripId}/expense/export/pdf" role="button" class="secondary outline" download="...">` in SETTLED expense article header
- No HTMX — browser-native download (Content-Disposition: attachment)
- PDF structure: summary → transfers by party → party balances → category breakdown → all receipts
- Recommended implementation: Flying Saucer (XHTML-to-PDF via dedicated print template, not PicoCSS)
- Filename: `abrechnung-{tripName}-{year}.pdf`

## Iteration 11 Mobile UX Audit (2026-03-19)
- Full audit at `/docs/design/ux-audit/iteration-11-mobile-ux-audit.md`
- 10 P0 issues identified across all SCS — features completely unusable on 375px viewport
- Primary mobile use cases: Shopping list (at store), receipt scan, accepting invitations
- Three CSS fixes cover the vast majority of P0 issues (no template changes required for these):
  1. `.grid { grid-template-columns: 1fr }` at ≤640px — fixes ALL multi-column form grids
  2. Dialog full-screen override at ≤640px — fixes ALL 8+ dialogs across all SCS
  3. `.table-cards` CSS pattern + `th:data-label` on `<td>` — responsive table→card layout
- Stay-period form in `<td>` (trip/detail.html) requires template change — P0
- Shopping list action buttons have 14px tap targets (need 44px) — P0; fix: remove inline style overrides
- Meal plan table (4-col, each cell has 2-3 form elements) is most complex rewrite — defer to Iteration 12
- Recommended Iteration 11 scope: S11-A through S11-J (10 stories, Phase 1+2+partial 3)
- Phase 3 (meal plan accordion, accommodation room cards) deferred to Iteration 12

## Design Documents Produced
- `/docs/design/journeys/trip-planning-organizer.md` — organiser journey map
- `/docs/design/journeys/invitation-flow.md` — participant invitation flow
- `/docs/design/journeys/expense-discovery-flow.md` — expense discoverability analysis + phase journey map
- `/docs/design/journeys/expense-iteration6-flows.md` — Iteration 6: four-eyes review, settlement summary, categories, accommodation journey maps
- `/docs/design/journeys/shopping-list-iteration8-flows.md` — Iteration 8: 6 journey maps (organizer build, participant shop, concurrent shopping, post-trip review, invitation email, external invitation)
- `/docs/design/components/feedback-system.md` — inline feedback pattern + toast evaluation
- `/docs/design/components/trip-detail-page.md` — full page redesign spec including dialog-based invite
- `/docs/design/components/expense-integration.md` — full spec for making expenses discoverable from trips UI
- `/docs/design/components/expense-iteration6.md` — Iteration 6: receipt status badges, review flow, add-dialog with category/accommodation, settlement summary, HTMX map, i18n keys
- `/docs/design/components/shopping-list-iteration8.md` — Iteration 8: ShoppingListPage, ShoppingItemRow (3 states), AddManualItemForm (desktop+mobile), polling container, InvitationEmail HTML template, HTMX map, i18n keys
- `/docs/design/wireframes/expense-detail-iteration6.md` — full page wireframes for OPEN (review queue), SETTLED, mobile card layout
- `/docs/design/wireframes/shopping-list-iteration8.md` — Iteration 8: desktop + mobile wireframes, item state variations, email wireframe
- `/docs/design/wireframes/iteration-10-ux-analysis.md` — Iteration 10 (REPLANNED): Accommodation URL Import + Kassenzettel-Scan (Receipt OCR) + Category Breakdown + PDF Export; full Import Pipeline pattern, desktop+mobile wireframes, HTMX specs, i18n keys, journey maps, open questions for architect
- `/docs/design/ux-audit/iteration-11-mobile-ux-audit.md` — Full mobile UX audit: page-by-page severity table, ASCII wireframes for 3 critical pages, CSS fix proposals, breakpoint strategy, 3 mobile user journeys, phased implementation plan (10 stories S11-A through S11-J)

## Expense SCS Integration (v0.5.0 design decision)
- Expense SCS at /expense/{tripId} is fully built but unreachable — no navigation links anywhere
- nav.expense key exists in all three SCS message files but is never rendered in <nav>
- IAM dashboard is NOT the right place for expense links — wrong SCS context
- Primary fix: expense card on trip detail (COMPLETED only) + expense column on trip list
- Balance section uses hardcoded inline style="color:green/red" — needs CSS class replacement
- Settle button has no confirmation despite being irreversible — needs confirm pattern
- New i18n keys needed in trips SCS: trip.expense.title, trip.expense.hint, trip.expense.action
- New i18n keys needed in expense SCS: expense.settle.confirm
