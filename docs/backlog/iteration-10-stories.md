# Iteration 10 — Refined User Stories: Accommodation URL Import, Receipt OCR Scan, Settlement Polish + Lighthouse CI

**Date**: 2026-03-19 (REWRITTEN after user priority correction)
**Target Version**: v0.10.0
**Bounded Contexts**: Trips (Accommodation URL Import), Expense (Receipt OCR Scan, Settlement per Category, PDF Export), Infrastructure (Lighthouse CI)

---

## Overview

This document replaces the earlier Iteration 10 scope. The previous plan treated Recipe Import
from URL as the headline story. After a priority review with the user, the correct priorities are:

1. **HIGH** — Accommodation URL Import (US-TRIPS-061): paste a vacation rental URL, system scrapes
   it, editable preview form, saves as Accommodation with rooms. This is the highest-priority
   remaining feature because accommodation drives cost distribution, advance payments, and room
   assignment — the core group trip planning challenge.

2. **MEDIUM** — Kassenzettel-Scan / Receipt Photo OCR (US-EXP-002): participant photographs a
   grocery receipt with their phone, OCR extracts total amount and date, editable preview form,
   saves as Receipt in Expense. MVP scope: extract total amount + date reliably. Item-level line
   extraction is a stretch goal.

3. **LOW** — Recipe Import from URL (US-TRIPS-041): DEFERRED to Iteration 11+. The meal planning
   area is not the core value driver. The adapter pattern developed for Accommodation URL Import
   (scraping + SSRF + editable preview) applies here as well when the time comes.

### Shared Import Pipeline Pattern

All import features follow the same UX pipeline:

```
Input (URL / Camera) → Analyse (HTTP + OCR) → Vorschau (editable form, pre-filled) → EDIT → Bestätigung → Speichern
```

The EDIT step is mandatory. The import result is a suggestion, not a commitment. Users must be
able to correct any field before saving. The form after import reuses the existing create form
for the respective entity — no separate confirmation UI is needed.

---

## Dependency Graph

```
S10-A: Accommodation URL Import (US-TRIPS-061)
  — requires S9-A Accommodation entity to exist (done in Iteration 9)
  — new secondary port: AccommodationImportPort in Trips domain/accommodation/
  — new adapter: HtmlAccommodationImportAdapter in adapters/integration/
  — reuses existing SetAccommodationCommand + TripService.setAccommodation()
  — publishes AccommodationPriceSet event (via existing path) when import is saved
  — SSRF protection required (same pattern for future Recipe Import)
  — no Flyway migration (schema exists from Iter 9)
  — foundation for future US-TRIPS-062 (Accommodation Poll with comparison)

S10-B: Kassenzettel-Scan / Receipt Photo OCR (US-EXP-002)
  — new secondary port: ReceiptOcrPort in Expense domain/expense/
  — new adapter: OcrReceiptAdapter in adapters/integration/
  — reuses existing SubmitReceiptCommand + Expense.submitReceipt()
  — MVP: extract totalAmount + date; item lines are a stretch goal
  — requires a decision on OCR technology (see Open Design Questions)
  — Flyway migration: image_path column on receipt table (if not already present)
  — no new domain events

S10-C: Settlement per Category (US-EXP-032)
  — pure read-side: derives category totals from existing Receipt data
  — no new aggregates, no Flyway migration
  — independent; can be developed alongside S10-A or S10-B
  — renders as a new section on the existing settlement detail page

S10-D: Export Settlement as PDF (US-EXP-033)
  — depends on S10-C category breakdown being available (reuses in PDF)
  — introduces new library dependency (iText or similar)
  — Thymeleaf-to-PDF is an alternative if iText licensing is a concern
  — no schema changes

S10-E: Lighthouse CI (US-INFRA-042)
  — GitHub Actions workflow addition; no SCS changes
  — depends on PWA manifest (done in Iteration 9)
  — can be implemented at any point in the iteration

--- NOT this iteration ---

US-TRIPS-041 (Recipe Import from URL)
  — LOW priority; deferred to Iteration 11+
  — the HtmlAccommodationImportAdapter pattern (S10-A) will be reused when the time comes

US-TRIPS-062 (Accommodation Poll)
  — Could/L; new LocationPoll aggregate; out of scope

US-EXP-022 (Custom Receipt Splitting)
  — Could/M; deferred — OCR scan (S10-B) takes the M slot in Expense

US-INFRA-040 (Service Worker / Offline)
  — Could/XL; deferred

US-TRIPS-055 (Bring App Integration)
  — Could; no stable public API; deferred
```

---

## Recommended Iteration 10 Scope

| ID | Story | Priority | Size | Bounded Context |
|----|-------|----------|------|-----------------|
| S10-A | Accommodation URL Import | Must | L | Trips |
| S10-B | Kassenzettel-Scan / Receipt Photo OCR | Should | M | Expense |
| S10-C | Settlement per Category | Could | M | Expense |
| S10-D | Export Settlement as PDF | Could | M | Expense |
| S10-E | Lighthouse CI | Should | S | Infrastructure |

**Scope rationale:**
- S10-A is the mandatory L story: accommodation is THE core planning hub, and entering it manually
  is friction that URL import directly eliminates. The Iteration 9 Accommodation entity already
  exists; this story adds the import path to it.
- S10-B closes the receipt entry UX gap. Photographing a supermarket receipt is how group trips
  actually work in the field — the current manual entry forces users to read out and type every
  number. Even a rough OCR with an editable form is a significant UX improvement.
- S10-C is pure read-side work with no schema changes — low risk, visible improvement.
- S10-D builds naturally on S10-C (the category breakdown goes into the PDF). Introduces a new
  dependency but the scope is bounded.
- S10-E closes the automated quality monitoring gap now that the PWA manifest is in place.

---

## Recommended Implementation Order

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S10-E | Lighthouse CI — purely additive GitHub Actions change, no SCS impact |
| 2 | S10-A | Accommodation URL Import — highest user value; reuses Iter 9 schema |
| 3 | S10-C | Settlement per Category — read-side only, safe to merge at any time |
| 4 | S10-D | PDF Export — builds on S10-C category breakdown |
| 5 | S10-B | Receipt OCR Scan — requires OCR technology decision first (see Open Design Questions) |

---

## New Domain Model (Iteration 10)

### Trips SCS — AccommodationImportPort (Secondary Port)

Accommodation URL Import introduces a new secondary port in the domain layer and an adapter in
`adapters/integration/`. The existing `Accommodation` entity, `SetAccommodationCommand`, and
`TripService.setAccommodation()` are not changed — the import path ends at the editable preview
form and the save goes through the existing command.

```
AccommodationImportPort (interface in domain/accommodation/)
  Optional<AccommodationImportResult> importFromUrl(String url)

AccommodationImportResult (Record in domain/accommodation/)
  String name
  String address           (nullable)
  String bookingUrl        (the original URL, preserved as booking reference)
  LocalDate checkIn        (nullable)
  LocalDate checkOut       (nullable)
  BigDecimal totalPrice    (nullable)
  String notes             (nullable — e.g. scraped description snippet)
  List<ImportedRoom> rooms (may be empty if room structure not found on page)

ImportedRoom (Record in domain/accommodation/)
  String name              (e.g. "Schlafzimmer 1", "Doppelzimmer")
  RoomType roomType        (best guess from scraped text; nullable → defaults to DOUBLE)
  int bedCount             (default 2 if not parseable)
  BigDecimal pricePerNight (nullable)

HtmlAccommodationImportAdapter (implements AccommodationImportPort, in adapters/integration/)
  — fetches URL with 5-second timeout
  — parses HTML via Jsoup (new dependency on travelmate-trips pom.xml)
  — extracts: og:title / property name, og:description / address, price, dates, room hints
  — SSRF protection: resolves host to IP, rejects RFC 1918 / loopback ranges before fetching
  — returns Optional.empty() on fetch error, parse error, or no relevant data found
```

The adapter is wired via Spring `@Bean` in a `@Configuration` class. The domain only knows the
port. Tests mock the port to avoid real HTTP calls. The adapter uses heuristics — different
rental platforms have different HTML structures. The editable preview form is the safety net for
all misrecognition.

**Primary extraction targets (in priority order):**
1. `og:title` → `name`
2. `schema.org/LodgingBusiness` JSON-LD: `name`, `address`, `priceRange`, `checkinTime`,
   `checkoutTime`, `amenityFeature`
3. `og:description` → `notes` (first 500 chars)
4. Booking.com / Ferienhausmiete.de HTML patterns as fallback (opportunistic, not guaranteed)
5. If nothing found: return `Optional.empty()`

### Expense SCS — ReceiptOcrPort (Secondary Port)

Receipt OCR introduces a new secondary port in the domain layer and an adapter in
`adapters/integration/`. The existing `SubmitReceiptCommand` and `Expense.submitReceipt()` path
is unchanged — the OCR result pre-fills the form, and the user saves through the existing flow.

```
ReceiptOcrPort (interface in domain/expense/)
  OcrResult analyseReceipt(byte[] imageBytes, String mimeType)

OcrResult (Record in domain/expense/)
  boolean success
  BigDecimal totalAmount   (nullable — main extraction target)
  LocalDate receiptDate    (nullable)
  String rawText           (full OCR text output, for debugging / manual correction)
  List<OcrLineItem> items  (may be empty — stretch goal)
  String errorMessage      (non-null when success=false)

OcrLineItem (Record in domain/expense/)
  String description
  BigDecimal amount

TesseractOcrAdapter (implements ReceiptOcrPort, in adapters/integration/)
  OR
CloudVisionOcrAdapter (implements ReceiptOcrPort, in adapters/integration/)
  — technology to be decided (see Open Design Questions)
  — MVP extraction: total amount (last large number on receipt) + date (dd.MM.yyyy pattern)
  — image pre-processing: convert to greyscale + contrast boost before OCR if using Tesseract
  — returns success=false with errorMessage when image is unreadable or OCR confidence too low
```

### Receipt Entity — Image Storage

If image storage is not already on `receipt` (check whether US-EXP-002 was previously partially
implemented), add:

```
Receipt (existing entity — may need extension)
  + String imagePath    (nullable — relative path or object key; null for manual entries)
  + String mimeType     (nullable — "image/jpeg", "image/png")
```

Image storage MVP: local filesystem in a configurable directory (e.g., `./uploads/receipts/`).
S3-compatible storage is a future concern. The image is stored before OCR is called. If OCR
fails, the image is still stored and the user can enter data manually.

---

## Story S10-A: US-TRIPS-061 — Accommodation URL Import

**Epic**: E-TRIPS-07
**Priority**: Must (was Could in product backlog — elevated by user)
**Size**: L
**As an** Organizer, **I want** to paste a vacation rental URL and have the system extract the
accommodation details automatically, **so that** I don't have to manually type the property name,
address, price, and room details from a booking confirmation page.

### Background

The Iteration 9 Accommodation entity exists with a full room inventory model (name, address, URL,
check-in/out, rooms with types + bed counts, total price). The manual entry form is already
implemented. This story adds an import path: the Organizer pastes a URL, the system scrapes
what it can, and the normal accommodation form is pre-filled for review and correction.

The import pipeline is: **URL Input → Fetch + Parse → Editable Pre-filled Form → Save**

The pre-filled form is the *same* accommodation form as used for manual entry — no separate
confirmation step. The user edits what the scraper got wrong and saves normally.

### Acceptance Criteria

#### Happy Path — URL with Structured Data

- **Given** I am viewing a Trip detail page as Organizer and the Trip has no accommodation yet,
  **When** I click "Unterkunft aus URL importieren",
  **Then** a dialog or section opens with a URL input field and an "Importieren" button.

- **Given** I paste the URL of a vacation rental page that contains `schema.org/LodgingBusiness`
  JSON-LD or rich Open Graph metadata (name, address, price),
  **When** I click "Importieren",
  **Then** the accommodation form is displayed pre-filled with:
  - Name extracted from the page title / og:title / JSON-LD name
  - Address from JSON-LD address or og:description
  - The original URL pre-filled as "Buchungslink"
  - Price if found (total or per-night × nights)
  - Rooms if the page contains room-level information (room type hints, bed counts)
  All fields are editable. I can correct any misrecognized value before saving.

- **Given** the form is pre-filled and I click "Speichern",
  **Then** the accommodation is saved exactly as if I had entered it manually (same
  `SetAccommodationCommand` path, same domain event `AccommodationPriceSet` published if
  total price is set).

#### Partial Import — Some Fields Missing

- **Given** the URL page has a name but no price or room details,
  **When** the form is pre-filled,
  **Then** name is filled, all other fields are empty with their normal placeholders.
  The empty fields are editable — I fill them in before saving.

- **Given** the scraped page has no recognizable room structure,
  **When** the form is pre-filled,
  **Then** the room list starts with one empty room row (same default as manual "Zimmer hinzufügen").
  I can add, edit, and remove rooms before saving.

- **Given** the scraped total price is a per-night price and check-in/out are not found,
  **When** the form is pre-filled,
  **Then** the price field is pre-filled with the scraped number and a hint note is shown:
  "Dieser Preis könnte ein Preis pro Nacht sein — bitte überprüfe den Gesamtpreis."

#### Import Failure — No Data Extractable

- **Given** I paste a URL for a page that exists but contains no recognizable accommodation
  metadata (no og:title, no JSON-LD, no price patterns),
  **When** I click "Importieren",
  **Then** the normal accommodation form is shown empty with the URL pre-filled as "Buchungslink"
  and a notice: "Auf dieser Seite konnten keine Unterkunftsdaten erkannt werden. Bitte fülle
  die Felder manuell aus."

#### Import Failure — HTTP Error

- **Given** I paste a URL that returns HTTP 404, 500, or any non-200 response,
  **When** I click "Importieren",
  **Then** I see the error: "Die URL konnte nicht geladen werden (Status {statusCode}).
  Bitte prüfe den Link oder gib die Unterkunft manuell ein."

- **Given** the URL request times out (longer than 5 seconds),
  **When** I click "Importieren",
  **Then** I see the error: "Die Verbindung hat zu lange gedauert. Bitte versuche es erneut
  oder gib die Unterkunft manuell ein."

#### SSRF Protection

- **Given** I paste a URL pointing to a private IP range (10.x.x.x, 192.168.x.x,
  172.16.x.x–172.31.x.x) or localhost,
  **When** I click "Importieren",
  **Then** the request is blocked before any network call is made. I see:
  "Diese URL ist nicht erreichbar."

- **Given** I paste a URL with a non-HTTP/HTTPS scheme (e.g., `file://`, `ftp://`),
  **When** I click "Importieren",
  **Then** I see the validation error: "Bitte gib eine gültige HTTP- oder HTTPS-URL ein."

#### Editable Preview is Mandatory

- **Given** the import succeeds and the form is pre-filled,
  **When** I correct the total price, add or remove a room, or change the room type,
  **Then** the saved accommodation reflects my edits, not the raw import result.
  The import result is a suggestion only — saving goes through the normal form submission path.

- **Given** the import succeeds and I save without changing anything,
  **Then** the accommodation is saved with the imported values.

#### Authorization

- **Given** I have the `participant` role (not `organizer`),
  **When** I attempt to access the import form or POST to the import endpoint,
  **Then** I receive HTTP 403 Forbidden.

- **Given** I am an Organizer of a different Tenant,
  **When** I attempt to import accommodation for a Trip I don't own,
  **Then** the Trip is not found (TenantId scoping enforced at repository). HTTP 404.

#### Trip Status Constraints

- **Given** the Trip is COMPLETED or CANCELLED,
  **When** the Organizer views the Trip detail page,
  **Then** the "Unterkunft aus URL importieren" button is not rendered. Existing accommodation
  is shown in read-only mode.

- **Given** the Trip is PLANNING, CONFIRMED, or IN_PROGRESS,
  **When** the Organizer uses the import,
  **Then** the operation is allowed.

#### Multi-Tenancy

- The imported accommodation is scoped to the organizer's TenantId, resolved from the JWT
  email claim via TravelPartyRepository. No cross-tenant access.

### Technical Notes

- Bounded Context: Trips
- New secondary port: `AccommodationImportPort` in `domain/accommodation/`
  ```java
  public interface AccommodationImportPort {
      Optional<AccommodationImportResult> importFromUrl(String url);
  }
  ```
- New Records in `domain/accommodation/`:
  - `AccommodationImportResult(String name, String address, String bookingUrl, LocalDate checkIn, LocalDate checkOut, BigDecimal totalPrice, String notes, List<ImportedRoom> rooms)`
  - `ImportedRoom(String name, RoomType roomType, int bedCount, BigDecimal pricePerNight)`
- New adapter: `HtmlAccommodationImportAdapter` in `adapters/integration/`
  - HTTP client: `java.net.http.HttpClient` with 5-second connection + read timeout
  - HTML parsing: **Jsoup 1.18.x** (new dependency on `travelmate-trips/pom.xml`)
  - JSON-LD: parse `<script type="application/ld+json">` blocks via `ObjectMapper`; find
    `@type: "LodgingBusiness"` or `@type: "Hotel"` or `@type: "VacationRental"`
  - Open Graph fallback: `og:title` → name, `og:description` → notes
  - Price extraction: regex on page text for EUR/€ patterns; prefer JSON-LD `priceRange`
  - Room extraction: look for `numberOfRooms`, `amenityFeature` with bed counts; fallback to
    counting `<article>` or `<section>` elements with bed-related keywords (Bett, Zimmer, Schlafzimmer)
  - SSRF check: resolve URL's hostname to IP via `InetAddress.getByName()`, reject if:
    - 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 (RFC 1918)
    - 127.0.0.0/8 or `::1` (loopback)
    - 169.254.0.0/16 (link-local)
  - Returns `Optional.empty()` on any error (timeout, parse failure, no data found)
- New Controller endpoints in `TripController` (or new `AccommodationImportController`):
  - `GET /trips/{tripId}/accommodation/import` → renders `accommodation/import-form.html`
    (URL input only, with SSRF note visible to user)
  - `POST /trips/{tripId}/accommodation/import` → calls `AccommodationImportPort.importFromUrl(url)`,
    renders `accommodation/form.html` (existing create form) pre-populated with result.
    On failure: renders `accommodation/import-form.html` with error message.
  - The import step does NOT save. The save goes through `POST /trips/{tripId}/accommodation`
    (existing `SetAccommodationCommand` path).
- No new domain event (AccommodationPriceSet is already published by existing path on save)
- No Flyway migration (Accommodation schema from V11/V12 in Trips already exists)
- Routing key: no new routing key needed
- Test strategy:
  - Unit tests for `HtmlAccommodationImportAdapter` with mocked HTTP responses (WireMock)
  - Unit tests for SSRF IP range validation — positive and negative cases
  - Unit tests for price and room extraction heuristics with fixture HTML snippets
  - Controller test: mock `AccommodationImportPort` → verify pre-fill + error paths
  - E2E test: WireMock stub serves a fixture HTML page with JSON-LD LodgingBusiness; verify
    the accommodation form is pre-filled and saves correctly
- BDD scenario: "Als Organisator möchte ich eine Unterkunft per URL importieren"

---

## Story S10-B: US-EXP-002 — Kassenzettel-Scan / Receipt Photo OCR

**Epic**: E-EXP-01
**Priority**: Should
**Size**: M
**As a** Participant, **I want** to photograph a receipt with my phone and have the total amount
and date extracted automatically, **so that** I can submit an expense quickly without typing out
every number from a long supermarket receipt.

### Background

In practice, trip participants stop at a supermarket, bakery, or lift ticket office and pay for
the group. The current flow requires opening the app, manually entering the amount, date, and
description. For a long supermarket receipt this is slow and error-prone. Photographing the
receipt and having OCR extract the total and date reduces the entry burden to a quick review
and one tap.

MVP scope: extract total amount and date reliably. The user still enters description and category
manually. Line-item extraction (individual items on the receipt) is a stretch goal — it adds
complexity and the primary value is capturing the total.

The pipeline is: **Camera/Gallery Upload → Server-side OCR → Editable Pre-filled Form → Submit**

### Acceptance Criteria

#### Happy Path — Photo Upload and OCR

- **Given** I am on the Expense receipt page for a Trip,
  **When** I click "Kassenzettel fotografieren" (or "Foto hochladen"),
  **Then** the browser opens the camera (on mobile) or file picker (on desktop) filtered to
  image types (`accept="image/*" capture="environment"`).

- **Given** I take a photo or select an image from my gallery,
  **When** the image is uploaded (POST to the server),
  **Then** the server stores the image, runs OCR, and returns the pre-filled receipt form:
  - Amount field: extracted total amount (last large currency value on the receipt), or empty
  - Date field: extracted date in dd.MM.yyyy format, or today's date as default
  - Description: empty (user fills in)
  - Category: unchanged default (GROCERIES pre-selected or user's last used)
  The original photo is shown as a thumbnail on the form for reference.

- **Given** the form is pre-filled and I verify or correct the amount and date,
  **When** I click "Beleg einreichen",
  **Then** the Receipt is created with the entered (corrected) amount, date, description,
  category, and a reference to the stored image. This goes through the existing
  `SubmitReceiptCommand` path.

#### Partial Recognition — Amount or Date Missing

- **Given** the OCR runs but cannot confidently identify the total amount
  (e.g., the receipt is blurry, or no large currency value is found),
  **When** the form is shown,
  **Then** the amount field is empty. A hint is shown: "Betrag konnte nicht erkannt werden —
  bitte manuell eingeben." The user types the amount.

- **Given** the OCR finds the amount but not the date,
  **When** the form is shown,
  **Then** the date field defaults to today's date. A hint is shown: "Datum konnte nicht
  erkannt werden — bitte prüfen."

- **Given** the OCR fails entirely (image too dark, receipt crumpled, file unreadable),
  **When** OCR processing completes,
  **Then** the receipt form is shown empty (except the stored image thumbnail) with the message:
  "Der Kassenzettel konnte nicht gelesen werden. Bitte fülle die Felder manuell aus."

#### Image Format Constraints

- **Given** I upload a file that is not an image (e.g., a PDF, a text file),
  **When** the upload is submitted,
  **Then** I see the error: "Bitte lade ein Bild hoch (JPEG, PNG oder HEIC)."

- **Given** I upload an image larger than 10 MB,
  **When** the upload is submitted,
  **Then** I see the error: "Die Datei ist zu groß. Bitte wähle ein Bild unter 10 MB."

#### Manual Entry Path — Not Affected

- **Given** I choose to enter a receipt without a photo (existing "Beleg manuell hinzufügen"),
  **When** I submit,
  **Then** the existing flow is unchanged. No photo, no OCR. `imagePath` on the Receipt is null.

#### View — Receipt with Photo

- **Given** a Receipt has a stored image,
  **When** I view the receipt in the list,
  **Then** a small photo thumbnail is shown next to the receipt row.

- **Given** I click the thumbnail,
  **Then** the image opens in a lightbox or new tab for review.

#### Authorization

- **Given** I have the `participant` role,
  **When** I upload a receipt photo and submit the form,
  **Then** the operation is allowed. Both participants and organizers can scan receipts.

- **Given** I try to access OCR results for a receipt that belongs to a different Trip or Tenant,
  **When** the request is made,
  **Then** HTTP 403 Forbidden (TenantId scoping enforced).

#### Multi-Tenancy

- The uploaded image is stored in a tenant-scoped path (e.g., `uploads/{tenantId}/receipts/`).
  No cross-tenant image access.

### Technical Notes

- Bounded Context: Expense
- New secondary port: `ReceiptOcrPort` in `domain/expense/`
  ```java
  public interface ReceiptOcrPort {
      OcrResult analyseReceipt(byte[] imageBytes, String mimeType);
  }
  ```
- New Records in `domain/expense/`:
  - `OcrResult(boolean success, BigDecimal totalAmount, LocalDate receiptDate, String rawText, List<OcrLineItem> items, String errorMessage)`
  - `OcrLineItem(String description, BigDecimal amount)`
- OCR technology: **OPEN DESIGN QUESTION — see Open Design Questions section below**
- Image storage: local filesystem for MVP
  - Config property: `travelmate.upload.receipts-dir` (default `./uploads/receipts/`)
  - Filename: `{tenantId}/{receiptId}.{extension}` (UUID prevents collisions)
  - Receipt entity extension: `imagePath VARCHAR(500) NULL`, `mimeType VARCHAR(50) NULL`
  - Flyway migration: check whether `imagePath` already exists on `receipt` table (US-EXP-002
    was partially described in product backlog as XL). If not, add in new Expense Flyway V11.
- New Controller endpoints in `ReceiptController` (or new `ReceiptScanController`):
  - `GET /expense/{tripId}/receipts/scan` → renders `expense/receipt-scan.html`
    (camera/file upload form only, no OCR yet)
  - `POST /expense/{tripId}/receipts/scan/upload` → multipart upload:
    1. Validates file type and size
    2. Stores image to filesystem
    3. Calls `ReceiptOcrPort.analyseReceipt(bytes, mimeType)`
    4. Renders `expense/receipt-form.html` (existing form) pre-filled with OCR result
       and `imagePath` as hidden field
  - The save goes through existing `POST /expense/{tripId}/receipts` (`SubmitReceiptCommand`)
- New HTMX interaction: the upload triggers a spinner while OCR runs server-side.
  Use `hx-indicator` on the upload button. OCR latency is typically 1–5 seconds.
- Total amount extraction heuristics (for Tesseract path):
  - Scan `rawText` for the last occurrence of a currency pattern: `\d+[.,]\d{2}` or `EUR \d+`
  - Candidate filtering: ignore per-item prices (too many), prefer the largest number in the
    last 3 lines of the receipt (where "GESAMT", "TOTAL", "SUMME" typically appears)
  - Date extraction: regex for German date formats: `\d{2}\.\d{2}\.\d{4}`, `\d{2}/\d{2}/\d{4}`
- Test strategy:
  - Unit tests for `TesseractOcrAdapter` (or `CloudVisionOcrAdapter`) with fixture image files
  - Unit tests for amount and date extraction logic
  - Unit tests for file type and size validation
  - Controller test: mock `ReceiptOcrPort` → verify pre-fill + error paths
  - E2E test: upload a pre-prepared JPEG of a sample receipt; assert amount + date fields
    are populated (or show the empty-with-hint state if OCR not configured in CI)
  - E2E test: verify manual entry path is unaffected
- Flyway migration: V11 in Expense (if `image_path` / `mime_type` not already on `receipt`)
- BDD scenario: "Als Teilnehmer möchte ich einen Kassenzettel fotografieren und einreichen"

---

## Story S10-C: US-EXP-032 — Settlement per Category

**Epic**: E-EXP-04
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to see the settlement broken down by expense category (Unterkunft,
Lebensmittel, Restaurant, Aktivitäten, Transport, Sonstiges), **so that** everyone understands
where the shared money was spent.

### Background

The party-level settlement view (Iteration 9) answers "who owes whom". The category breakdown
answers "what did we spend it on". Both views are derived from the same `Receipt` data — no new
aggregate or schema is needed. The category breakdown is a secondary section on the existing
settlement page.

### Acceptance Criteria

#### Category Breakdown Section

- **Given** a Trip is COMPLETED and has an approved expense ledger,
  **When** the Organizer views the Settlement page,
  **Then** below the party balance table, a "Kostenübersicht nach Kategorie" section is shown.

- **Given** the category section is rendered,
  **Then** it shows one row per category that has at least one approved receipt:
  ```
  Kategorie        Betrag    Anteil
  Unterkunft       380 €      38%
  Lebensmittel     290 €      29%
  Restaurant       180 €      18%
  Aktivitäten       90 €       9%
  Transport         60 €       6%
  ─────────────────────────────────
  Gesamt         1.000 €     100%
  ```
  Only categories with amounts > 0 are shown. Categories with 0 EUR are omitted.

- **Given** the accommodation total from `TripProjection.accommodationTotal` is set (populated
  by the `AccommodationPriceSet` event consumed in Iteration 9),
  **When** the category section is rendered,
  **Then** it is included in the "Unterkunft" row alongside any ACCOMMODATION-category receipts.
  The Unterkunft total = `accommodationTotal + sum(receipts with category=ACCOMMODATION)`.

- **Given** a receipt has no category set (null / legacy data),
  **When** the category section is rendered,
  **Then** it is counted under "Sonstiges".

- **Given** there are no approved receipts,
  **When** the settlement page loads,
  **Then** the category section is not shown (empty state).

#### Participant View

- **Given** a Participant (not Organizer) views the settlement page,
  **When** the category section is shown,
  **Then** the same breakdown is visible. All participants can see the category summary.

- **Given** I click on a category row,
  **Then** a detail panel expands (HTMX `hx-get`) showing the individual receipts in that
  category: description, date, submitter name, amount.

#### Multi-Tenancy

- Category totals are derived from the `Expense` aggregate scoped to the organizer's `tenantId`.
  No cross-tenant data exposure.

### Technical Notes

- Bounded Context: Expense
- No new aggregate, entity, or event
- No Flyway migration
- New read model: `CategoryBreakdown` record
  ```java
  record CategoryBreakdown(
      ExpenseCategory category,
      BigDecimal total,
      BigDecimal percentage,
      List<ReceiptSummary> receipts
  )
  ```
  Computed in `SettlementService` (or a new `CategoryBreakdownService`) from
  `Expense.receipts()` filtered to `status == APPROVED`.
  Accommodation total added to the `ACCOMMODATION` bucket from `TripProjection.accommodationTotal`.
- New method: `List<CategoryBreakdown> computeCategoryBreakdown(Expense expense, TripProjection projection)` — pure computation, no side effects
- UI: `settlement/detail.html` — new collapsible section below the party balance table.
  Each row is expandable via HTMX `hx-get` to load receipt detail for that category.
  New Thymeleaf fragment: `settlement/category-breakdown.html`
- `ExpenseCategory` enum already exists (from Iteration 6): GROCERIES, ACCOMMODATION,
  RESTAURANT, ACTIVITIES, TRANSPORT, OTHER. Use as-is.
- BDD scenario: "Als Organisator möchte ich die Gesamtkosten nach Kategorie aufgeschlüsselt sehen"

---

## Story S10-D: US-EXP-033 — Export Settlement as PDF

**Epic**: E-EXP-04
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to export the settlement as a PDF document, **so that** I can
share it with Travel Parties who don't use the app, send it by email, or keep it as a record.

### Background

After a group trip, the Organizer often needs to communicate the final settlement to all parties.
Not all participants reliably check the app after the trip ends. A shareable PDF with the
party-level settlement, transfer statements, and category breakdown is a practical artefact that
closes the trip administratively.

### Acceptance Criteria

#### PDF Export Trigger

- **Given** a Trip is COMPLETED and a settlement has been calculated,
  **When** the Organizer views the Settlement page,
  **Then** an "Abrechnung als PDF exportieren" button is visible.

- **Given** I click the button,
  **When** the PDF is generated,
  **Then** the browser downloads a PDF file named: `Abrechnung_{tripName}_{date}.pdf`

#### PDF Content

- **Given** the PDF is generated,
  **Then** it contains the following sections in order:
  1. **Header**: Trip name, trip dates, generation date, "Erstellt mit Travelmate"
  2. **Party-Level Settlement Summary**: one row per TravelParty with share, credits, balance
  3. **Transfer Statements**: "Familie Schmidt überweist 130 € an Familie Müller" per line
  4. **Cost Breakdown per Category** (from S10-C): table with category, amount, percentage
  5. **Footer**: "Diese Abrechnung ist nicht rechtsverbindlich."

- **Given** no `AccommodationPriceSet` event has been received for the Trip,
  **When** the PDF is generated,
  **Then** the category breakdown shows only receipt-based costs. No accommodation row.

- **Given** advance payments were recorded (S9-D),
  **When** the PDF is generated,
  **Then** each party row shows: share, receipts credited, advance credited, balance.

#### Format

- **Given** the PDF is opened on any device,
  **Then** it is formatted for A4 (portrait), readable without zooming on a desktop screen.
  Tables use standard borders. Currency values use German number formatting (. thousands, , decimal).

- **Given** a party name or trip name is longer than 50 characters,
  **When** the PDF is rendered,
  **Then** the text wraps or is truncated with "…" — no content overflow outside the page bounds.

#### Authorization

- **Given** I have the `participant` role (not `organizer`),
  **When** I try to access the PDF export endpoint,
  **Then** I receive HTTP 403 Forbidden.

- **Given** the Trip is not in COMPLETED status,
  **When** the PDF export is triggered,
  **Then** the button is not shown. If the endpoint is called directly, HTTP 400 Bad Request.

#### Multi-Tenancy

- The PDF is generated from data scoped to the organizer's TenantId. No cross-tenant data.

### Technical Notes

- Bounded Context: Expense
- PDF generation library: **iText 8** (community/AGPL) or **Flying Saucer** (LGPL, renders
  Thymeleaf HTML to PDF) — team decision (see Open Design Questions)
- Preferred approach for maintainability: **Thymeleaf HTML template → Flying Saucer / OpenPDF**
  (reuses the existing Thymeleaf skill; no separate PDF layout API to learn)
  Alternative: iText 8 programmatic approach (more control, stricter layout)
- New Thymeleaf template: `settlement/pdf.html` — designed for print layout (no HTMX, no
  interactive elements, CSS `@media print` rules)
- New Controller endpoint: `GET /expense/{tripId}/settlement/pdf`
  - Requires `organizer` role
  - Calls `SettlementService.computeSettlement(...)` and `computeCategoryBreakdown(...)`
  - Generates PDF bytes
  - Returns `ResponseEntity<byte[]>` with `Content-Type: application/pdf` and
    `Content-Disposition: attachment; filename="Abrechnung_...pdf"`
- No new domain event, no Flyway migration
- BDD scenario: "Als Organisator möchte ich die Abrechnung als PDF herunterladen"

---

## Story S10-E: US-INFRA-042 — Lighthouse CI Integration

**Epic**: E-INFRA-05
**Priority**: Should
**Size**: S
**As a** developer, **I want** Lighthouse CI to automatically audit key Travelmate pages on every
pull request, **so that** mobile performance, PWA installability, and accessibility regressions
are caught before they reach main.

### Background

The PWA manifest is deployed (Iteration 9). Lighthouse can now audit PWA installability. The
existing E2E test suite runs against the full stack in CI (via Docker Compose). Lighthouse CI
plugs into the same infrastructure, running after the E2E suite completes.

### Acceptance Criteria

#### CI Integration

- **Given** a pull request is opened or updated,
  **When** the GitHub Actions pipeline runs,
  **Then** after the E2E tests pass, a Lighthouse CI step audits at least the Trip list page
  (`/trips`) as an authenticated user.

- **Given** Lighthouse runs on the Trip list page,
  **When** the audit completes,
  **Then** the following score thresholds must pass:
  - Performance: >= 80
  - Accessibility: >= 90
  - Best Practices: >= 90
  - PWA (Installable): pass (installable-manifest)

- **Given** any threshold is breached,
  **When** the CI step completes,
  **Then** the pipeline is marked as failed. The Lighthouse HTML report is uploaded as a CI artifact.

- **Given** all thresholds pass,
  **When** the CI step completes,
  **Then** the Lighthouse HTML report is available as a build artifact (retained for 7 days).

#### Audit Configuration

- **Then** the following Lighthouse categories are audited: `performance`, `accessibility`,
  `best-practices`, `pwa`. The `seo` category is excluded (internal app, not public-facing).

- **Given** pages require authentication,
  **When** Lighthouse runs,
  **Then** a pre-script performs a headless login via test user credentials
  (`testuser` / `testpassword`) and passes the session to Lighthouse.

#### Scope Limitation

- Lighthouse is run with the "Mobile" preset (throttled 4G, mobile viewport 375px).
- Not run on POST/mutation endpoints.
- No custom install prompt UI required by this story.

### Technical Notes

- Bounded Context: Infrastructure / CI
- Tool: `@lhci/cli` (Lighthouse CI) via `npm`
- GitHub Actions: new job `lighthouse` in CI workflow
  - `needs: [e2e]` (runs after E2E succeeds)
  - `actions/upload-artifact` for HTML reports
- Config file: `.lighthouserc.json` at repository root
- Authentication: same Docker test context as E2E tests; Playwright login step extracts
  session cookie before Lighthouse audit
- No SCS code changes required
- BDD scenario: none (CI infrastructure story)

---

## Cross-SCS Event Flow (Iteration 10)

No new cross-SCS events are introduced in Iteration 10.

The `AccommodationPriceSet` event (published by Trips, consumed by Expense) was introduced in
Iteration 9 and is triggered whenever `TripService.setAccommodation()` saves a non-null totalPrice.
S10-A reuses this path: when the imported accommodation is saved through the normal form, the
same event is published. No change needed.

---

## Open Design Questions

### Q1: OCR Technology Choice (S10-B — MUST RESOLVE BEFORE IMPLEMENTATION)

Two viable paths for receipt OCR in the JVM / Docker environment:

#### Option A: Tesseract (Self-Hosted)

- Java wrapper: `tess4j 5.x` (wraps Tesseract C library via JNI) or `bytedeco/tesseract`
- Tesseract binary + German language pack (`deu.traineddata`) must be present in the Docker image
- Docker image size increase: ~200–400 MB for Tesseract + language packs
- Accuracy on printed receipts: good for laser/inkjet, poor for thermal/faded receipts
- No API key, no external dependency, no per-call cost
- Latency: 2–5 seconds per image on a typical server CPU
- **Setup effort**: moderate — Dockerfile changes + JNI dependency + image pre-processing
  (greyscale + contrast) to improve recognition accuracy on supermarket thermal receipts
- **Recommendation for MVP**: viable if the team is comfortable with Docker changes

#### Option B: Google Cloud Vision API

- Library: `google-cloud-vision` Java client
- Accuracy: significantly better than Tesseract, especially for thermal receipts
- Cost: free tier — 1000 units/month; then $1.50 per 1000 units. For a hobby project: free.
- Requires a Google Cloud API key stored as an environment variable (`GOOGLE_VISION_API_KEY`)
- Latency: 1–2 seconds per image (network-dependent)
- No Docker image changes required
- **Setup effort**: low — add Maven dependency + config property + API key in docker-compose
- **Recommendation**: preferred for accuracy and simplicity of setup

#### Option C: AWS Textract

- Similar to Cloud Vision; stronger for structured documents (invoices, tables)
- Overkill for MVP receipt scan; more complex IAM setup
- Not recommended for this iteration

**Decision needed from team:** Is external API dependency acceptable (Cloud Vision), or is
self-hosted Tesseract required for offline / privacy / cost reasons?

**Pragmatic default**: start with Cloud Vision (Option B) for accuracy in the MVP iteration.
Design `ReceiptOcrPort` so the adapter is swappable if a switch to Tesseract is required later.
If no API key is configured, the adapter returns `OcrResult(success=false, errorMessage="OCR
nicht konfiguriert")` and the form shows empty with a note — the manual entry path always works.

### Q2: PDF Generation Library (S10-D)

Two options:

#### Option A: Flying Saucer + OpenPDF (LGPL)

- Renders an XHTML Thymeleaf template to PDF via CSS
- Familiar: reuses existing Thymeleaf template skills
- Limitations: CSS support is CSS2.1 only (no Flexbox/Grid); PicoCSS will need a print
  stylesheet override
- Dependencies: `org.xhtmlrenderer:flying-saucer-pdf-openpdf` + `com.github.librepdf:openpdf`

#### Option B: iText 8 (AGPL)

- Programmatic PDF generation; full control over layout
- AGPL license: requires open-sourcing any derivative work — acceptable for an open-source project
- More verbose API but no CSS limitations
- Dependencies: `com.itextpdf:itext-core`

**Recommendation**: Flying Saucer + OpenPDF for the MVP. The settlement PDF is a simple table
document — CSS2.1 is sufficient. If the layout proves too constrained, migrate to iText later.

### Q3: Accommodation Scraping Accuracy Expectations

Vacation rental platforms (Booking.com, Airbnb, Ferienhausmiete.de, HomeAway) actively work
to prevent scraping, change their HTML structure, and may return different markup to server-side
requests vs. browser requests. The import is **best-effort**:

- The user ALWAYS gets an editable form — scraping failure is a degraded mode, not an error
- The "Buchungslink" field is always pre-filled with the original URL (even if nothing else is extracted)
- Platform-specific adapters (Booking.com, Ferienhausmiete.de) are NOT planned — the adapter
  uses only generic metadata (Open Graph, JSON-LD LodgingBusiness)
- Accuracy expectation: name and URL reliably; price, rooms, and dates opportunistically

---

## Out-of-Scope for Iteration 10

| Item | Reason |
|------|--------|
| US-TRIPS-041: Recipe Import from URL | LOW priority per user — deferred to Iteration 11+ |
| US-TRIPS-062: Accommodation Poll | Could/L; new LocationPoll aggregate; out of scope |
| US-EXP-022: Custom Splitting per Receipt | Could/M; Receipt OCR (S10-B) takes the M slot |
| US-INFRA-040: Service Worker / Offline Caching | Could/XL; deferred |
| US-TRIPS-055: Bring App Integration | Could; no stable public API; deferred |
| US-IAM-040/041: Multi-Organizer Role Management | Should; deferred to dedicated IAM iteration |
| Rejection history / audit trail for receipts | Out of scope from S9-E; future story |
| US-INFRA-055: Transactional Outbox Pattern | Could/XL; significant infrastructure change; deferred |
| S10-E (formerly): TravelParty Name Propagation | Was in old Iter 10 plan; re-evaluate — still needed for party name in settlement (separate small story if capacity allows) |
| OCR line-item extraction | Stretch goal for S10-B; not in MVP acceptance criteria |
| Accommodation URL import for Airbnb / Booking.com structured scraping | Out of scope; generic Open Graph + JSON-LD only |

---

## Flyway Migration Summary

| SCS | Version | Content |
|-----|---------|---------|
| Trips | none new | Accommodation schema exists from V11/V12 (Iter 9); no changes for import path |
| Expense | V11 (if needed) | Add `image_path VARCHAR(500) NULL`, `mime_type VARCHAR(50) NULL` to `receipt` table (check if already present from earlier stubs) |
| Common | — | No new event contracts in Iteration 10 |

---

## Ubiquitous Language Compliance

| UI (DE) | UI (EN) | Code | Context |
|---------|---------|------|---------|
| Unterkunft aus URL importieren | Import Accommodation from URL | AccommodationImportPort | Trips |
| Importieren | Import | POST /trips/{id}/accommodation/import | Trips |
| Vorschau bearbeiten | Edit Preview | editable pre-filled form | Trips + Expense |
| Buchungslink | Booking URL | AccommodationImportResult.bookingUrl | Trips |
| Kassenzettel fotografieren | Scan Receipt | ReceiptOcrPort | Expense |
| Betrag erkannt | Amount detected | OcrResult.totalAmount | Expense |
| Betrag konnte nicht erkannt werden | Amount could not be detected | OcrResult (fallback hint) | Expense |
| Abrechnung als PDF exportieren | Export Settlement as PDF | GET /expense/{id}/settlement/pdf | Expense |
| Kostenübersicht nach Kategorie | Settlement per Category | CategoryBreakdown | Expense |
| Qualitätsprüfung | Quality Gate | Lighthouse CI | Infrastructure |
| Barrierefreiheit | Accessibility | Lighthouse `accessibility` category | Infrastructure |
