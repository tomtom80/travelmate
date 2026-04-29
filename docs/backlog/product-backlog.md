# Product Backlog — Travelmate

Stand: 2026-04-27

## Delivery Snapshot

- letzte stabile Freigabe: `v0.18.0`
- aktueller Entwicklungsstand: `0.19.0-SNAPSHOT`
- der Produktkern ist fachlich breit geliefert; der Weg zum Go-Live ist nun primaer ein Plattform-, Security- und Betriebsreife-Pfad

## Executive Summary

Travelmate ist fachlich nahe an einem Major Release. Die Kernjourney von Reisepartei ueber Reiseplanung bis Abrechnung ist in den bounded contexts IAM, Trips und Expense weitgehend umgesetzt. Die grössten offenen Luecken fuer ein `v1.0.0` liegen nicht mehr im Domänenkern, sondern in:

- Delivery und CI/CD
- Security Hardening
- Observability und Auditierbarkeit
- Backup/Restore und betrieblicher Wiederanlauf
- Tenant-Isolation-Absicherung und Event-Vertragsstabilitaet
- Go-Live-relevanter Doku- und Compliance-Reife

Deshalb folgt die Planung fuer `v0.19.0` bis `v1.0.0` bewusst einem Zielbild von grob `60%` nicht-funktionalen Themen und `40%` funktionalen Themen.

## Fachlicher Scope heute

### IAM / Reisepartei

Bereits geliefert:

- Self-Service Sign-up und Login via Gateway und Keycloak
- Reisepartei-Dashboard mit Mitgliedern und Mitreisenden
- Mitgliedseinladung per E-Mail mit nachgelagerter Registrierung
- Entfernen von Mitgliedern und Mitreisenden

Weiter offen:

- Passwort-Reset ueber Keycloak als produktionsreife Standardfunktion
- Profilpflege fuer Mitglieder und Mitreisende als Komfortfunktion

### Trips / Reiseplanung

Bereits geliefert:

- Reise anlegen, anzeigen, bearbeiten, loeschen und durch den Lifecycle fuehren
- Teilnehmer und eigene Reisepartei verwalten
- externe Reiseeinladung per E-Mail
- trip-lokale Mehr-Organisator-Unterstuetzung
- Terminabstimmungen und Unterkunftsabstimmungen
- Unterkunftsverwaltung inklusive Import aus URL
- Rezepte, Essensplaene, Einkaufslisten und Kuechendienst

Weiter offen:

- Rezept-Import aus URL
- Password-Reset- und Extern-Einladungs-nahe Onboarding-Komplettierung
- betriebliche und sicherheitstechnische Absicherung der bestehenden Flows

### Expense / Abrechnung

Bereits geliefert:

- Expense-Read-Model aus Trip-Events
- Belege manuell erfassen, pruefen, neu einreichen und entfernen
- OCR-unterstuetzter Scan-Flow
- Gewichtungen, Unterkunftskosten und Vorauszahlungen
- Abrechnung, Kategorien und PDF-Export

Weiter offen:

- gezielte UX- und Security-Nacharbeiten im Expense-Flow
- Auditierbarkeit, Tenant-Isolation-Tests und Go-Live-Hardening

## Epic-Status

| Epic | Status | Kommentar |
|------|--------|-----------|
| E-IAM-01 Sign-Up & Onboarding | DONE | Kernflow geliefert |
| E-IAM-02 Travel Party Management | MOSTLY DONE | Rename-/Profil-Komfortfunktionen offen |
| E-IAM-03 Member Management | MOSTLY DONE | Self-Service-Edit offen |
| E-IAM-04 Companion Management | MOSTLY DONE | Edit-Flow offen |
| E-IAM-05 Multi-Organizer Support | DONE | trip-lokal gemaess ADR-0024 umgesetzt |
| E-IAM-06 Notification Service | PARTIAL | E-Mail-Flows vorhanden, Preferences/SMS offen |
| E-IAM-07 Authentication & Security Enhancements | OPEN | Password reset und Produktions-Hardening offen |
| E-TRIPS-01 Trip Lifecycle Management | DONE | inkl. edit/delete |
| E-TRIPS-02 Invitation & Participation | DONE | inkl. externer E-Mail-Einladung |
| E-TRIPS-03 Stay Periods & Scheduling | DONE | Kernfluss geliefert |
| E-TRIPS-04 Meal Planning | DONE | inkl. kitchen duty |
| E-TRIPS-05 Recipe Management | MOSTLY DONE | URL-Import offen |
| E-TRIPS-06 Shopping List | DONE | Kernflow geliefert |
| E-TRIPS-07 Location & Accommodation | DONE | inkl. Import |
| E-TRIPS-08 Collaborative Trip Decision Making | DONE | DatePoll + AccommodationPoll geliefert |
| E-EXP-01 Receipt Management | MOSTLY DONE | Produktions-Polish offen |
| E-EXP-02 Expense Tracking & Categories | DONE | Kernflow geliefert |
| E-EXP-03 Weighting & Splitting | DONE | Kernflow geliefert |
| E-EXP-04 Settlement & Calculation | DONE | inkl. PDF |
| E-EXP-05 Four-Eyes Review Process | PARTIAL | funktional vorhanden, Produktionstiefe offen |
| E-INFRA-01 CI/CD Pipeline | PARTIAL | CI und Demo-Deploy vorhanden, Produktionsreife offen |
| E-INFRA-02 Observability & Monitoring | OPEN | noch kein produktionsreifer Stack |
| E-INFRA-03 Architecture Fitness | PARTIAL | ArchUnit/JaCoCo vorhanden, Event- und Security-Fitness offen |
| E-INFRA-04 Security Hardening | OPEN | P0 fuer Go-Live |
| E-INFRA-05 PWA & Offline Support | PARTIAL | Manifest vorhanden, v1-Haertung offen |
| E-INFRA-06 Documentation & Quality | ACTIVE | laufende Konsolidierung |

## Major Release Planning

### Zielbild `v1.0.0`

Travelmate gilt fuer den ersten Go-Live nicht dann als fertig, wenn nur alle sichtbaren Features vorhanden sind, sondern wenn die Plattform die folgenden vier Ebenen gleichzeitig erreicht:

1. **fachliche Vollstaendigkeit**
2. **betriebliche Reife**
3. **Security- und Compliance-Grundschutz**
4. **nachweisbare Test- und Release-Faehigkeit**

### Release-Gate fuer den Go-Live

Vor einem Tag `v1.0.0` muessen mindestens diese Bedingungen erfuellt sein:

- die v1-relevanten Required Stories aus IAM, Trips und Expense sind geliefert
- keine offenen P0-Sicherheitsluecken aus der Roadmap
- CI, Contract-Tests, E2E und Lasttest-Baseline sind grün
- Secrets-, Backup-, Restore- und Rollback-Prozesse sind dokumentiert und praktisch verifiziert
- Observability, Audit Logging und Tenant-Isolation-Absicherung sind aktiv

## Kritische Befunde aus der Major-Release-Planung

Diese Punkte beeinflussen die Priorisierung unmittelbar:

1. CSRF ist im Code deaktiviert, obwohl fruehere Sicherheitsdokumentation das Gegenteil suggeriert.
2. Der fruehere IAM-Admin-Cleanup-Pfad musste produktionsseitig abgesichert werden und bleibt ein Sensitivpunkt.
3. Produktiv gefaehrliche Default-Secrets duerfen spaetestens vor Go-Live nicht mehr wirksam sein.
4. GHCR- und Demo-Delivery sind bereits automatisiert, aber noch nicht produktionsreif genug fuer ein echtes Promotion-Modell.
5. Event-Verlust nach `AFTER_COMMIT` ist mit dem aktuellen Publishing-Modell moeglich und muss vor dem Go-Live adressiert werden.
6. Tenant-Isolation ist architektonisch angelegt, aber noch nicht tief genug testautomatisiert.

## Roadmap `v0.19.0` bis `v1.0.0`

| Iteration | Zielversion | Fokus | Ergebnis |
|-----------|-------------|-------|---------|
| Iteration 19 | `v0.19.0` | Observability, Outbox, Recipe-Import, Recipe-CRUD, Delivery- und Doku-Korrekturen | Sichtbarkeit und Event-Integritaet herstellen |
| Iteration 20 | `v0.20.0` | externe Einladung, Passwort-Reset, Audit-Logging, Security-Doku-Angleichung | Onboarding- und Forensik-Luecken schliessen |
| Iteration 21 | `v0.21.0` | Production Hardening I: CSRF, Security-Header, Secrets/TLS, Backup/Restore, CI/CD-Stufen | produktionsnahe Betriebsfaehigkeit schaffen |
| Iteration 22 | `v0.22.0` | Production Hardening II: Rate-Limits, Keycloak-Hardening, Dependency-Scanning, Auto-Updates | Angriffsoberflaeche und Betriebsrisiken weiter senken |
| Iteration 23 | `v0.23.0` | GDPR, Accessibility, Lighthouse, Datenexport/Loeschung | Compliance- und UX-Reife schaffen |
| Iteration 24 | `v0.24.0` | Tenant-Isolation-Test-Slice, Event-Contracts, JWT-`sub`-Mapping, Pentest | Pre-Release-Absicherung |
| Iteration 25 | `v1.0.0` | Lasttest, Feinschliff, Rollback-Drill, finale Release-Checkliste | Major Release |

Die ausfuehrliche Iterationsplanung liegt in den Dateien:

- [`roadmap-v1.0.0.md`](./roadmap-v1.0.0.md)
- [`iteration-19-plan.md`](./iteration-19-plan.md)
- [`iteration-20-plan.md`](./iteration-20-plan.md)
- [`iteration-21-plan.md`](./iteration-21-plan.md)
- [`iteration-22-plan.md`](./iteration-22-plan.md)
- [`iteration-23-plan.md`](./iteration-23-plan.md)
- [`iteration-24-plan.md`](./iteration-24-plan.md)
- [`iteration-25-plan.md`](./iteration-25-plan.md)

## Priorisierte naechste Themen

1. Observability-Stack und Transactional Outbox
2. Audit Logging, Password Reset und externer Onboarding-Funnel
3. Secrets-, TLS-, Backup- und Rollback-Reife
4. Tenant-Isolation- und Event-Contract-Fitness
5. finale Go-Live-Absicherung mit Pentest, Lasttest und Compliance-Artefakten

## Verweise

- Release-Stand: [`release-v0.18.0.md`](./release-v0.18.0.md)
- Letzte abgeschlossene Iteration: [`iteration-18-plan.md`](./iteration-18-plan.md)
- Major-Release-Roadmap: [`roadmap-v1.0.0.md`](./roadmap-v1.0.0.md)
- Aktueller Delivery-Status: [`../operations/2026-04-27-release-and-demo-delivery-status.md`](../operations/2026-04-27-release-and-demo-delivery-status.md)
