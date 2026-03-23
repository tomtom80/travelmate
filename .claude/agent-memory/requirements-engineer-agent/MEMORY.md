# Requirements Engineer Agent Memory

## Iteration History
- Iteration 3 (v0.3.0): Completed — S3-A01 through S3-A05 (IAM), S3-B01 through S3-B07 (Trips)
- Iteration 4 (v0.5.0): Completed — Registration flow, error handling, UX hardening
- Iteration 5 (v0.6.0): Completed — Expense SCS, ArchUnit, JaCoCo, RabbitMQ DLQ, HTMX
- Iteration 6 (v0.7.0): Completed — Receipt categories, four-eyes review, settlement, accommodation, metrics
- Iteration 7 (v0.8.0): Completed — Recipe CRUD, MealPlan (generate/status/assign), Cucumber BDD
- Iteration 8 (v0.9.0-SNAPSHOT): Stories refined — Shopping List + Email Notifications
  - Refined story document: docs/backlog/iteration-8-stories.md
  - S8-A: Auto-Generate Shopping List (ShoppingList aggregate, scaling, regeneration)
  - S8-B: Add Manual Shopping Item (both roles allowed, preserved across regen)
  - S8-C: Assign Item to Myself (optimistic locking for concurrent assignment)
  - S8-D: Mark as Purchased (shortcut OPEN→PURCHASED, undo to ASSIGNED)
  - S8-E: Real-Time Updates (HTMX polling every 8s, SSE deferred)
  - S8-F: Email for Trip Invitation (extend existing InvitationEmailListener + invitationLink)
- Iteration 9 (v0.9.0): Stories REWRITTEN after domain knowledge deepdive — Accommodation with Rooms, Party Settlement, Advance Payments
  - Refined story document: docs/backlog/iteration-9-stories.md
  - S9-A (L): Create Accommodation with Room Inventory (US-TRIPS-060) — Accommodation entity with List<Room>, Flyway V11+V12, AccommodationPriceSet event
  - S9-B (M): Assign Travel Parties to Rooms (US-TRIPS-063) — RoomAssignment entity in Accommodation, partyTenantId, person count
  - S9-C (M): Party-Level Settlement View (US-EXP-050) — PartySettlementPlan, TripProjection extended with partyTenantId, AccommodationPriceSetConsumer, Flyway V5
  - S9-D (M): Advance Payment Tracking per TravelParty (US-EXP-013) — AdvancePayment.paidByPartyId = TenantId (NOT participantId), Flyway V6, party credits in settlement
  - S9-E (S): Re-Submit Rejected Receipt (US-EXP-042) — REJECTED→SUBMITTED transition, clears rejectionReason+reviewedBy+reviewedAt
  - S9-F (S): PWA Manifest (US-INFRA-041) — manifest.json served from Gateway static resources, apple-touch-icon, no Service Worker
  - DEFERRED to Iteration 10: Recipe Import from URL (US-TRIPS-041) — accommodation work takes the L slot
- Iteration 10 (v0.10.0): Stories REWRITTEN after priority correction — Accommodation URL Import (HIGH), Receipt OCR Scan (MEDIUM), Recipe Import DEFERRED
  - Refined story document: docs/backlog/iteration-10-stories.md
  - S10-A (L): Accommodation URL Import (US-TRIPS-061) — AccommodationImportPort + HtmlAccommodationImportAdapter, Jsoup HTML parsing, SSRF protection, 5s timeout, reuses existing SetAccommodationCommand + Accommodation schema from V11/V12
  - S10-B (M): Kassenzettel-Scan / Receipt Photo OCR (US-EXP-002) — ReceiptOcrPort + OcrReceiptAdapter, OCR tech TBD (Cloud Vision preferred vs Tesseract), pre-filled editable form, image_path on receipt, Flyway V11 Expense if needed
  - S10-C (M): Settlement per Category (US-EXP-032) — CategoryBreakdown read model, pure read-side, no migration, includes accommodationTotal in ACCOMMODATION bucket
  - S10-D (M): Export Settlement as PDF (US-EXP-033) — Thymeleaf HTML → Flying Saucer/OpenPDF or iText 8, party settlement + transfer statements + category breakdown, Organizer only
  - S10-E (S): Lighthouse CI (US-INFRA-042) — GitHub Actions job after E2E, .lighthouserc.json, thresholds: performance>=80, accessibility>=90, PWA installable
  - DEFERRED: Recipe Import from URL (US-TRIPS-041) — LOW priority per user; pushed to Iteration 11+
- Iteration 11 (v0.11.0): Stories written 2026-03-19 — Mobile UX Refactoring (pure CSS + template changes, no domain changes)
  - Refined story document: docs/backlog/iteration-11-stories.md
  - S11-A (S): Responsive Navigation (hamburger, shared CSS utilities)
  - S11-F (M): Dialogs → Mobile Bottom Sheets (CSS-only)
  - S11-D (M): Shopping List Mobile-First (HTMX fragment refactor: tr → article)
  - S11-E (M): Receipt Scan Mobile-First (scan dialog + scan-result form)
  - S11-B (S): Trip List → Mobile Cards
  - S11-C (M): Trip Detail → Accordion (one new model attr: pendingInvitationCount)
  - S11-G (M): Expense Settlement Mobile (CSS responsive tables)
  - S11-H (S): Member/Companion/Participant Tables → Cards
  - S11-I (S): Lighthouse Mobile Score >=90 (raise Performance threshold)

## Story ID Conventions
- SN-A## = IAM stories
- SN-B## = Trips stories
- SN-C## = Expense stories
- SN-D## = Gateway/Infra stories

## Key Design Decisions (Iteration 11 — 2026-03-19)
- Theme: Mobile UX Refactoring — NO new domain logic, NO Flyway migrations, NO new aggregates
- Iteration 11 is purely CSS + Thymeleaf template refactoring across all three SCS
- S11-A (S): Responsive Navigation — hamburger menu via <details>/<summary>, CSS-only, all three layout/default.html files; defines shared CSS utility classes (.card-grid, .btn-touch, .filter-pills, dialog bottom-sheet)
- S11-F (M): Dialogs → Mobile Bottom Sheets — CSS @media (max-width: 767px) on native <dialog>; all existing dialogs become bottom sheets; no JS; .grid inside dialogs stacks to single column
- S11-D (M): Shopping List Mobile-First — CRITICAL: itemRow HTMX fragment changes from <tr> to <article>; hx-target="closest tr" → "closest article" in ALL action forms; filter pills; large tap targets; card layout for items
- S11-E (M): Receipt Scan Mobile-First — scan dialog interior CSS; scan-result form large inputs; camera file input full-width; no controller/domain changes
- S11-B (S): Trip List → Cards — trip/list.html table replaced with .card-grid; pending invitations unchanged
- S11-C (M): Trip Detail → Accordion — <details class="mobile-accordion"> wraps non-essential sections; only new Java: pendingInvitationCount model attribute in TripController.detail(); desktop: summary hidden via CSS, details always open
- S11-G (M): Expense Settlement Mobile — responsive tables using td::before + data-label pattern (CSS-only stacking); party settlement + transfers + advance payments + category breakdown
- S11-H (S): Member/Companion/Participant Tables → Cards — dashboard/members.html, dashboard/companions.html, trip/invitations.html, participants in trip/detail.html; HTMX hx-target="closest tr" → "closest article" for delete fragments
- S11-I (S): Lighthouse Mobile Score >=90 — raise Performance threshold from 80 to 90; add Shopping List URL to audit; mobile preset (375px, throttled 4G)
- Implementation order: S11-A → S11-F → S11-D → S11-E → S11-H → S11-B → S11-C → S11-G → S11-I
- DEFERRED from Iteration 11: MealPlan grid mobile, Accommodation overview mobile, receipts list mobile, Service Worker

## Key Design Decisions (Iteration 10 — REWRITTEN 2026-03-19)
- CRITICAL PRIORITY CORRECTION: Accommodation URL Import is now S10-A (L); Recipe Import deferred to Iter 11+; Receipt OCR is new S10-B (M)
- Shared Import Pipeline pattern (all import features): Input → Analyse → Vorschau (editable pre-filled form) → EDIT → Bestätigung → Speichern; EDIT step is mandatory
- Accommodation URL Import: AccommodationImportPort in domain/accommodation/; HtmlAccommodationImportAdapter in adapters/integration/; Jsoup for HTML; SSRF protection (RFC 1918 + loopback)
- Accommodation URL Import: extracts og:title, schema.org/LodgingBusiness JSON-LD, price patterns; best-effort scraping; empty fields left editable; booking URL always preserved
- Accommodation URL Import: POST /trips/{id}/accommodation/import → pre-fills existing accommodation form; save goes through existing SetAccommodationCommand (no new command)
- Accommodation URL Import: no Flyway migration (Accommodation schema from V11/V12 already exists from Iter 9)
- Receipt OCR: ReceiptOcrPort in domain/expense/; adapter swappable (Cloud Vision preferred over Tesseract for accuracy)
- Receipt OCR: MVP extracts totalAmount + date; line-item extraction is stretch goal
- Receipt OCR: image stored BEFORE OCR runs; OCR failure → empty form with manual entry fallback
- Receipt OCR: image stored tenant-scoped: uploads/{tenantId}/receipts/{receiptId}.{ext}
- Receipt OCR: may need Flyway V11 Expense for image_path + mime_type columns on receipt table (check if already stubbed)
- Receipt OCR: Cloud Vision adapter — no API key configured → OcrResult(success=false) → form shows empty with hint; manual path always works
- Settlement Category: unchanged from old plan — CategoryBreakdown, pure read-side, no migration, accommodationTotal in ACCOMMODATION bucket
- PDF Export: Thymeleaf HTML template → Flying Saucer + OpenPDF (preferred for maintainability); iText 8 as alternative; Organizer only; A4 portrait
- PDF Export: includes party settlement + transfer statements + category breakdown; no new event, no schema change
- Lighthouse CI: unchanged — .lighthouserc.json, performance>=80, accessibility>=90, PWA installable, GitHub Actions after e2e
- TravelPartyNameRegistered event (from old Iter 10 plan): still needed — re-evaluate inclusion if capacity allows; not in core S10 scope

## Key Design Decisions (Iteration 9 — rewritten 2026-03-18)
- Accommodation: entity (NOT a simple VO) within Trip aggregate; carries List<Room> (entities) + RoomAssignment list
- Accommodation schema: separate tables accommodation + accommodation_room + room_assignment (NOT columns on trip table)
- Accommodation Flyway: V11 (accommodation + room tables), V12 (room_assignment table) in Trips SCS
- Accommodation: out-of-range check-in/check-out is a warning, not a hard error
- Accommodation: two pricing paths — per-room (UI computes sum → totalPrice) OR manual totalPrice; domain stores totalPrice
- Accommodation: publishes AccommodationPriceSet(tripId, tenantId, totalPrice) event via RabbitMQ
- AccommodationPriceSet: consumed by Expense SCS → stored as TripProjection.accommodationTotal; added to settlement pool
- RoomAssignment: partyTenantId = TenantId of the TravelParty being assigned (NOT participant UUID)
- Settlement unit = TravelParty (Reisepartei), NOT individual — partyShare = (partyWeight / totalWeight) × pool
- PartySettlementPlan: new record in Expense SCS grouping SettlementPlan data by partyTenantId
- TripProjection extension: ProjectedParticipant gains partyTenantId + partyName fields (populated from ParticipantJoinedTrip.tenantId)
- TravelParty display name in Expense: derived from first participant's lastName ("Family {lastName}") — full propagation deferred to Iteration 10
- Advance Payment: paidByPartyId = TenantId (NOT participantId UUID) — the key model difference from old design
- Advance Payment: Flyway V6 in Expense SCS — advance_payment table with paid_by_party_id, paid_by_party_name
- Advance Payment: only Organizer can record/edit/delete
- Re-Submit Receipt: REJECTED→SUBMITTED transition; clears rejectionReason, reviewedBy, reviewedAt; Organizer can re-submit on behalf of submitter
- PWA Manifest: manifest.json served from Gateway static; link rel="manifest" in shared Thymeleaf layout; no Service Worker in this iteration
- Recipe Import (US-TRIPS-041): DEFERRED to Iteration 10; RecipeImportAdapter design preserved (adapters/integration/, RecipeImportPort, JSON-LD, SSRF, 5s timeout)

## Key Design Decisions (Iteration 8)
- ShoppingList is a new Aggregate in Trips, scoped by TenantId + TripId
- Aggregation rule: same name (case-insensitive) + same unit → sum quantities
- Different units for the same ingredient → kept as separate items (no conversion)
- Scaling: ingredientQty × (participantCount / recipeServings), HALF_UP 2dp
- Regeneration: RECIPE items recomputed, MANUAL items preserved
- ASSIGNED/PURCHASED RECIPE items survive regen only if name+unit still exists post-regen
- Concurrent assignment conflict: optimistic locking (@Version or conditional UPDATE)
- Direct purchase shortcut: OPEN → ASSIGNED → PURCHASED in one action
- Un-purchase reverts to ASSIGNED (not OPEN), preserving assignee
- Polling interval: 8 seconds (not SSE; servlet SCS constraint)
- InvitationEmailListener already exists; extend to cover MEMBER-type invitations
- New invitationLink field needed in InvitationCreated event (or built from config property)

## Future Feature Ideas (from user)
- US-TRIPS-041 Recipe Import from URL: DEFERRED to Iteration 12+ (LOW priority per user — HtmlAccommodationImportAdapter pattern from S10-A will be reused; Iteration 11 is fully mobile UX)
- US-EXP-022 Custom Receipt Splitting: deferred from Iter 10 (Receipt OCR takes M slot); Iteration 11+
- US-TRIPS-062 Accommodation Poll: deferred — new LocationPoll aggregate, Could priority
- TravelPartyNameRegistered event (was S10-E old plan): still needed for correct party names in settlement; small story, re-evaluate for Iter 11
- Bring-App: Einkaufsliste → Bring API sync (US-TRIPS-055, deferred — no stable API docs)
- SSE-based real-time updates (deferred from Iteration 8)
- Email retry / DLQ for failed invitation emails (deferred from Iteration 8)
- Service Worker / Offline (US-INFRA-040): deferred, XL
- US-IAM-040/041 Multi-Organizer Role Management: deferred to dedicated IAM iteration
- TravelParty name update propagation (if name changes via US-IAM-012): deferred — depends on US-IAM-012 which is not yet implemented
- Transactional Outbox Pattern (US-INFRA-055): deferred, Could/XL
