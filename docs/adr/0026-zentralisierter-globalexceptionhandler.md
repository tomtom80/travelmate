# ADR-0026: Zentralisierter GlobalExceptionHandler

## Status
Vorgeschlagen — Implementierung deferred auf Iteration 19

## Kontext

Drei nahezu identische `GlobalExceptionHandler`-Klassen in IAM, Trips, Expense. Jeder neue Domain-Exception-Typ verdreifacht sich. Trips besitzt zusaetzlich:
- i18n-`MessageSource`-Resolver
- `ResponseStatusException`-Handler

IAM und Expense haben diese Erweiterungen nicht — Drift-Risiko bei jeder zukuenftigen Aenderung.

## Entscheidung

Extraktion einer abstrakten Basisklasse `AbstractGlobalExceptionHandler`, gehostet in einem **neuen Modul `travelmate-web-commons`** (nicht in `travelmate-common`, da letzteres als Plain-JAR ohne Spring-Web-Abhaengigkeit bestehen bleibt — siehe ADR-0008).

SCS-spezifische Subklassen koennen weitere `@ExceptionHandler` ergaenzen (z.B. `MaxUploadSizeExceededException` in Expense). i18n-Aufloesung via `MessageSource` wird Default-Verhalten; alle SCS pflegen `messages.properties`.

Implementierung deferred: ohne stabile E2E-Pipeline (Iter 18 S18-Q01) waere die Refactoring-Aenderung ein Risiko-Refactor, der HTMX-Toast-Verhalten subtil brechen koennte. Iter 19 startet die Implementierung **nach** stabiler Pipeline.

## Konsequenzen

### Positiv
- Single Source of Truth fuer HTMX-Toast-Mechanismus
- Neue Exception-Typen werden einmal implementiert
- Kontrakt-Tests im neuen Modul moeglich
- IAM und Expense bekommen automatisch i18n-Aufloesung und `ResponseStatusException`-Handler

### Negativ
- Neues Modul `travelmate-web-commons` erhoeht POM-Komplexitaet
- Migration erfordert sorgfaeltige BDD-Abdeckung pro SCS, um HTMX-Verhalten nicht zu brechen
- Spring-Web-Abhaengigkeit zentralisiert (war vorher pro SCS)

## Alternativen (verworfen)

### A: ExceptionHandler in `travelmate-common`
Bricht "common ist Plain JAR"-Invariante (ADR-0008). Common ist DDD-Building-Blocks-Modul, nicht Web-Layer.

### B: Code-Generierung
Overengineering. Generator + Template + Build-Integration fuer 3 Klassen ist nicht verhaeltnismaessig.

### C: Status quo akzeptieren
Akzeptierter DRY-Verstoss, waechst mit jedem Exception-Typ. Aktuell ueberschaubar, aber tendenziell verschlimmernd.

## Referenzen
- ADR-0008 (DDD + Hexagonal — Common ist Plain JAR)
- ADR-0013 (HTMX Feedback und Error Handling)
- Iteration-18-Architektur-Report Sektion 2 + 6
