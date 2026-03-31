# Release v0.14.1

**Release Date**: 2026-03-31
**Release Type**: Patch Release
**Based On**: `v0.14.0`

## Scope

`v0.14.1` konsolidiert die neue demokratische Reiseplanung nach `v0.14.0` und schliesst die wichtigsten fachlichen und UX-Luecken im Poll- und Planungsfluss.

- gestartete Termin- und Unterkunftsabstimmungen werden fuer mehrere Reiseparteien konsistent angezeigt
- neue Reisen verlangen keinen festen Reisezeitraum mehr vor der Terminabstimmung
- Unterkunftskandidaten muessen Zimmer und Ausstattungsmerkmale mitbringen, damit die Wahl belastbar ist
- der Sieger einer Unterkunftsabstimmung wird direkt als Unterkunft uebernommen und kann danach weiter gepflegt werden
- Poll- und Planungsoberflaechen folgen wieder dem Standard-Layout, zeigen Gewinner klarer und visualisieren Stimmenstaende
- Einladungen und Reisepartei-Referenzen rendern aktuelle Parteinamen statt veralteter Projektionen
- Compose- und Testharness-Haertungen stabilisieren die lokale E2E- und BDD-Verifikation

## Verification

- Trips-Modultests mit `mvn -pl travelmate-trips test` erfolgreich ausgefuehrt
- kompletter Browser- und BDD-Verify-Lauf mit `./mvnw -Pe2e -pl travelmate-e2e verify` erfolgreich ausgefuehrt
- Failsafe-Reports direkt geprueft: `280` Tests, `0` Failures, `0` Errors, `33` Skips

## Included Work Since v0.14.0

- `fix: make collaborative date and accommodation polls visible across travel parties`
- `feat: require candidate room details and auto-adopt confirmed accommodation poll winners`
- `feat: align planning and poll dashboards with default layout and richer vote visualization`
- `test: extend Playwright and Cucumber coverage for cross-party voting and expense follow-up`
- `chore: harden docker compose and local verification setup for reliable E2E execution`

## Notes

Diese Freigabe ist ein fachlicher Patch-Release: Der Kernnutzen liegt nicht in neuen Modulen, sondern in der Korrektur des Reise-Lifecycle, in stabileren kollaborativen Entscheidungen und in belastbarer End-to-End-Verifikation vor jedem weiteren Release.
