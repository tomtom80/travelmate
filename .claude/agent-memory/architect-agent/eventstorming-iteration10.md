---
name: EventStorming Iteration 10 (REVISED)
description: Iteration 10 REVISED scope — Accommodation URL Import (headline, L), Kassenzettel-Scan/Receipt OCR (M), Category Breakdown (M), PDF Export (M), Lighthouse CI (S). Recipe Import DEFERRED to Iter 11+. Import-Pipeline pattern shared across URL import and OCR.
type: project
---

## EventStorming: Iteration 10 Scope -- REVISED (2026-03-19)

**CRITICAL CORRECTION**: Previous plan had Recipe Import as S10-A headline. User corrected priorities:
1. Accommodation URL Import = HIGH (was S10-B, now S10-A, upgraded to L)
2. Kassenzettel-Scan = MEDIUM (new feature, S10-B)
3. Recipe Import from URL = LOW, DEFERRED to Iteration 11+

**Recommended Stories (v0.10.0)**:
1. S10-A: Accommodation URL Import (L) -- HEADLINE. Scrape vacation rental URLs -> editable form -> create Accommodation
2. S10-B: Kassenzettel-Scan / Receipt Photo OCR (M) -- NEW. Photo -> OCR -> editable Receipt form
3. S10-C: Settlement per Category (M) -- pure read model
4. S10-D: Export Settlement as PDF (M) -- Thymeleaf + Flying Saucer
5. S10-E: Lighthouse CI (S) -- GitHub Actions

**Shared Import-Pipeline Pattern**:
Input -> Analyse -> Vorschau -> **EDIT** -> Bestaetigung -> Speichern
The EDIT step is a FULLY EDITABLE pre-filled form, not read-only preview.

**Key Design Decisions**:
- AccommodationImportPort in Trips domain, ReceiptScanPort in Expense domain
- WebScrapingAccommodationImportAdapter in adapters/integration/
- TesseractReceiptScanAdapter in adapters/ocr/ (self-hosted, DSGVO-konform)
- HtmlFetcher with SSRF protection (reusable for future URL imports)
- CategoryGuesser domain service for store-name -> ExpenseCategory mapping
- No image persistence in v1 (transient OCR only)
- Tesseract requires Docker image extension (tesseract-ocr + tessdata-deu)

**No new Cross-SCS events** -- all features BC-internal.
**No Flyway migrations** -- all changes are adapter/application layer.

**Hot Spots**:
- HS-10-1: SSRF via URL import (CRITICAL -- ADR-0016)
- HS-10-2: Website blocking of bot requests
- HS-10-3: OCR quality for receipt photos (~70% target for v1)
- HS-10-4: Tesseract native dependency in Docker
- HS-10-5: Image upload size/format
- HS-10-6/7: PDF rendering, OCR privacy

**ADR candidates**: ADR-0016 (Import-Pipeline + SSRF), ADR-0017 (OCR tech), ADR-0018 (PDF generation)

**Why:** Accommodation is THE central planning element. URL import has highest UX leverage. Receipt OCR reduces friction during trips. Recipe import is low priority -- meal planning is not core value.

**How to apply:** S10-A first (establishes import infra + SSRF). S10-B second (new Expense feature, independent). S10-C/D are independent Expense work. S10-E at the end.
