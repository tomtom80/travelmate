# Architect Agent Memory

## Architecture Decisions
- 18 ADRs exist (ADR-0001 through ADR-0018)
- ADR-0016: Import-Pipeline-Pattern + SSRF-Schutz (Accepted, 2026-03-19)
- ADR-0017: OCR-Technologiewahl Kassenzettel-Scan (Proposed, 2026-03-19)
- ADR-0018: Lokales LLM (Ollama + Qwen2.5-VL 7B) fuer Import-Pipelines (Proposed, 2026-03-19)
- Iteration 10 remaining ADR candidate: ADR-0019 PDF-Generierung (Thymeleaf + Flying Saucer)

## Known Architecture Risks
- Expense uses ParticipantWeighting (per-individual) + PartySettlement aggregation layer (Option A from Iteration 9 analysis). Full PartyWeighting refactoring (Option C) was NOT implemented.
- PWA Service Worker scope in multi-SCS Gateway architecture (decided: minimal, App Shell only)
- No integration adapters exist yet — Iteration 10 introduces first external URL fetching (SSRF risk) AND first image upload (OCR)
- Tesseract native dependency requires Docker image extension (tesseract-ocr + tessdata-deu)

## EventStorming Sessions
- [eventstorming-expense-iter6](eventstorming-expense-iter6.md) — Expense Iteration 6 session (2026-03-16)
- [eventstorming-trips-iter8](eventstorming-trips-iter8.md) — Trips Iteration 8: Shopping List + Email Notifications (2026-03-17)
- [eventstorming-iteration9](eventstorming-iteration9.md) — Iteration 9 REVISED: PartyWeighting refactoring, Accommodation Aggregate, Advance Payment, Resubmit UI, PWA (2026-03-18)
- [eventstorming-iteration10](eventstorming-iteration10.md) — Iteration 10 REVISED: Accommodation URL Import (headline), Kassenzettel-Scan/OCR, Category Breakdown, PDF Export, Lighthouse CI (2026-03-19). Recipe Import DEFERRED.
- [eventstorming-iteration14](eventstorming-iteration14.md) — Iteration 14: DatePoll + AccommodationPoll aggregates for collaborative trip planning (2026-03-29). Two separate aggregates, BC-internal events.

## Quality Attributes
- (to be populated after Quality Storming)
