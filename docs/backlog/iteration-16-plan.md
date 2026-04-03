# Iteration 16 — Delivery Plan: UI Polish und PDF CI-Styling

**Date**: 2026-04-03
**Target Version**: v0.16.0
**Input**: Visuelles Review der Hero-Layout-Überarbeitung (Commit 4430c17), Screenshots mit Mobile-Overflow- und PDF-Problemen

**Status**: IN PROGRESS

---

## Planning Goal

Iteration 16 ist eine reine Polish-Iteration. Sie behebt UI-Probleme, die bei der
Hero-Layout-Überarbeitung in Iteration 15 (v0.15.8) aufgefallen sind, und bringt die
Abrechnungs-PDF auf das Travelmate Corporate-Identity-Niveau.

Kein neuer Fachcode — nur Templates, CSS und PDF-Template.

---

## Stories

### S16-A: PDF-Layout restructurieren + CI-Styling
**Priorität**: MUST

Die generierte Abrechnungs-PDF (settlement-pdf.html) ist abgeschnitten: die Party-Accounts-Tabelle
hat 9 Spalten + inline Kontozeilen und passt nicht auf A4.

**Lösung:**
- `@page { size: A4; margin: 1.5cm; }` statt `body { margin: 2cm }`
- CI-Farben: `#3366CC` (Primary), `#E07B00` (Expense-Accent)
- Party-Accounts von einer breiten Tabelle zu Per-Party-Karten mit Key-Value-Grid
- `page-break-inside: avoid` pro Karte
- Kategorie- und Vorauszahlungs-Tabellen bleiben (3–4 Spalten)

**Dateien:** `travelmate-expense/.../templates/expense/settlement-pdf.html`

### S16-B: Expense Hero SVG ersetzen
**Priorität**: MUST

Das aktuelle SVG (Kreise mit Plus-Symbol) ist nicht als Geld/Abrechnung erkennbar.

**Lösung:** Quittung (Zickzack-Rand) + Euro-Münze im gleichen stroke-only Style wie andere Hero-SVGs.

**Dateien:** `travelmate-expense/.../templates/expense/detail.html`

### S16-C: Settled Expense Mobile Overflow
**Priorität**: MUST

Bei abgerechneten Reisen läuft das Layout mobil über die Boundaries:
1. Settled Advance Payments `<article>` fehlt `class="section-card"`
2. Party-Account-Summary-Grid: `minmax(10rem, 1fr)` erzwingt 2 Spalten auf 375px
3. Table-Cards `<td>` mit `<span>` + `<small>` stacken nicht korrekt

**Lösung:**
- `section-card` Klasse + `section-heading` Wrapper
- Mobile: `grid-template-columns: 1fr` für Summary
- Mobile: `flex-wrap: wrap` + `small { flex-basis: 100% }` für Table-Cards

**Dateien:** `travelmate-expense/.../templates/expense/detail.html`, `travelmate-expense/.../static/css/style.css`

### S16-D: "Eigene Rezepte Teilen" Section-Card
**Priorität**: SHOULD

Der Bereich "Eigene Rezepte teilen" auf der Trip-Rezeptseite fehlt das Card-Styling.

**Lösung:** `<details>` in `<section class="section-card">` wrappen.

**Dateien:** `travelmate-trips/.../templates/recipe/trip-list.html`

---

## Dokumentation

- Arc42 05-building-block-view.md: DatePoll + AccommodationPoll Aggregates
- Arc42 09-architecture-decisions.md: ADR-0019, ADR-0021, ADR-0022
- Arc42 11-risks-and-technical-debt.md: Resolved items markieren
- MEMORY.md: v0.16.0-SNAPSHOT, 992 Tests, Iterationen 14+15 als DONE

---

## Verifikation

- Unit/Integration Tests: `./mvnw clean verify` (alle Module)
- E2E + BDD: `./mvnw -Pe2e verify` (gegen laufende Docker-Infrastruktur)
- PDF: Manuell generierte PDF öffnen → A4, CI-Farben, keine Abschneidung
- Mobile: Chrome DevTools 375px → abgerechnete Reise → kein horizontaler Overflow
