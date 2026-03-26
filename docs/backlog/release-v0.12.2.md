# Release v0.12.2

**Release Date**: 2026-03-26
**Release Type**: Patch Release
**Based On**: `v0.12.1`

## Scope

`v0.12.2` konsolidiert die Arbeiten nach `v0.12.1` und bringt zwei funktionale Ergaenzungen in die freigegebene Linie:

- Reisepartei-Namen koennen in IAM umbenannt werden
- die Umbenennung wird als Domain Event ueber die beteiligten SCS propagiert
- Rezepte sind trip-spezifisch und werden im Kontext einer konkreten Reise angelegt
- Trip-Detailseiten zeigen den Einstieg in die trip-bezogene Rezeptverwaltung
- E2E- und BDD-Tests wurden auf das neue Rezept-Scoping angepasst
- Design-System- und Dokumentationsartefakte wurden ergaenzt

## Verification

- Quellstand auf Release-Version `0.12.2` gesetzt
- Root-README und Dokumentationsindex fuer den Repository-Einstieg ergaenzt
- bestehende Apache-2.0-Lizenz im Repository beibehalten und dokumentiert

## Included Work Since v0.12.1

- `feat: editable travel party name with cross-SCS event propagation`
- `feat: trip-scoped recipes - domain + persistence + service layer`
- `feat: trip recipe UI - controller, templates, trip detail card`
- nachgezogene E2E-/BDD-Anpassungen fuer trip-spezifische Rezeptpfade

## Notes

Diese Freigabe ist als Patch-Release dokumentiert, obwohl sie kleinere funktionale Erweiterungen enthaelt, weil sie auf der bestehenden `0.12.x`-Linie aufsetzt und keine neue Iteration einleitet.
