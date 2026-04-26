# QA Roadmap — Travelmate v1.0

Stand: 2026-04-26 | Basis: v0.17.0-SNAPSHOT (post-release)

---

## 1. Test Pyramid Health Check

### Aktuelle Verteilung

| Ebene | Anzahl | Anteil | Werkzeuge |
|---|---|---|---|
| Unit / Integration | 1 064 | ~79 % | JUnit 5, AssertJ, Mockito, H2, MockMvc |
| E2E / BDD | 287 | ~21 % | Playwright (Java), Cucumber |
| Contract Tests | 0 | 0 % | — |
| Performance / Load | 0 | 0 % | — |

**Bewertung:** Die Pyramide ist grundsätzlich in Ordnung — der hohe Unit-Anteil ist erwünscht. Der E2E-Anteil von 21 % ist für ein server-rendered HTMX-System jedoch vertretbar, solange die BDD-Szenarien echte User-Journeys abdecken und nicht nur technische Pfade.

**Kritische Lücken:**

- **Contract Tests fehlen vollständig.** 19 RabbitMQ-Events verbinden IAM → Trips → Expense asynchron. Kein automatisierter Vertrag prüft, ob ein Publisher-Schema-Change den Consumer bricht. Das ist das größte strukturelle Risiko für v1.0.
- **Keine Performance-Baseline.** Weder Gatling noch k6 sind integriert. Für Produktion ohne SLO-Definition nicht akzeptabel.
- **ArchUnit vorhanden** in allen 3 SCS — das ist positiv und sollte ausgebaut werden (z. B. keine direkten SCS-zu-SCS-Aufrufe).
- **JaCoCo konfiguriert**, aber kein Coverage-Gate im CI definiert — Regressionen sind unsichtbar.

---

## 2. Coverage Gap Analysis

### BDD Feature-Dateien (18 Dateien, `features/01–18`)

| # | Feature | Bewertung | Lücken |
|---|---|---|---|
| 01 | Recipe Management | ausreichend | Import-Fehlerbehandlung (HTTP 4xx/5xx) fehlt |
| 02 | Meal Plan | ausreichend | Löschen eines Plans mit bestehenden Einträgen |
| 03 | Registration & Login | gut | Passwort-Reset-Flow komplett fehlt |
| 04 | Dashboard / Reisepartei | gut | Mitglied-Entfernung mit offener Reise |
| 05 | Trip Planning & Invitations | gut | Einladung nach Tripabschluss (COMPLETED-State) |
| 06 | Member Registration via Invitation | gut | Abgelaufener Einladungs-Link |
| 07 | Expense Navigation & Lifecycle | **2 Known Flakes** | Expense-Navigation aus Trip-Detail (UI-Lücke bekannt) |
| 08 | Shopping List | ausreichend | HTMX-Polling bei Server-Down |
| 09 | Accommodation | ausreichend | Zimmer-Überbuchtung (mehr Bewohner als Betten) |
| 10 | Accommodation Import | teilweise | LLM-Fehlerfall (Timeout / ungültige Antwort) |
| 11 | Receipt Scan | teilweise | Scan mit nicht lesbarem Bild |
| 12 | Tenant Rename | ausreichend | — |
| 13 | Trip Recipes | ausreichend | Rezept zu abgeschlossener Reise hinzufügen |
| 14 | Date Poll | gut | Gleichstand / kein Votum nach Deadline |
| 15 | Accommodation Poll | gut | Booking-Attempt → Failure → Fallback-Flow tief |
| 16 | Planning | ausreichend | — |
| 17 | Trip Edit & Delete | ausreichend | Löschen einer Reise mit verbundenen Expenses |
| 18 | Kitchen Duty | neu | vollständige Coverage noch offen |

### Fehlende User-Journey-Coverage (nicht durch BDD abgedeckt)

1. **Passwort-Reset-Flow** — kein einziges Szenario (geplant It.20)
2. **Externe Einladung + Fehlerpfade** — Link abgelaufen, E-Mail-Conflict, falscher Tenant (geplant It.20)
3. **Multi-Organizer-Szenarien** — Rollenübergabe, parallele Änderungen (geplant It.18)
4. **E-Mail-Benachrichtigungen end-to-end** — kein BDD, das Mailpit-Inbox prüft (geplant It.18)
5. **Settlement-PDF-Inhalt** — E2E öffnet PDF nicht, prüft nur HTTP 200
6. **Session-Timeout / Token-Refresh** — OIDC-Token-Ablauf während HTMX-Session
7. **Browser-Back-Button nach HTMX-Swap** — ungetestetes UX-Grenzverhalten

### Unit-Level-Lücken (nach Modul)

| Modul | Bekannte Lücke |
|---|---|
| `travelmate-expense` | `ExpenseController` — TenantId-Auflösung aus TripProjection statt JWT-Claim (Security-Lücke, kein Test) |
| `travelmate-trips` | `DatePoll` — kein Test für Gleichstand bei `confirmDateRange` |
| `travelmate-trips` | `ShoppingList` — Concurrent-Modification unter Polling-Last nicht getestet |
| `travelmate-iam` | `ExternalInvitationConsumer` — Dead-Letter-Queue-Requeue-Szenario kein Test |
| `travelmate-common` | `RoutingKeys` — keine Kompilierung-safe-Prüfung gegen tatsächliche Queue-Bindings |

---

## 3. Non-Functional Test Coverage Gaps

### 3.1 Performance / Load

**Status: 0 Tests — kritische Lücke für v1.0**

Empfohlenes Setup:

| Tool | Einsatz | Ziel-SLOs |
|---|---|---|
| **Gatling 3.12** | Baseline-Lasttests (Maven-Plugin) | p95 Dashboard < 1 s bei 50 VU |
| **k6** | Smoke-Tests im CD-Pipeline | p99 Trip-Erstellung < 2 s |

Priorisierte SLO-Definitionen für v1.0:

| Endpoint | Metrik | Ziel |
|---|---|---|
| `GET /iam/dashboard` | p95 | < 1 000 ms |
| `GET /trips/` | p95 | < 800 ms |
| `POST /trips/{id}/expense/receipts` (File-Upload) | p95 | < 3 000 ms |
| `GET /expense/{id}/settlement.pdf` | p95 | < 5 000 ms |
| RabbitMQ Event-Verarbeitung (`TripCompleted` → Expense-Erstellung) | End-to-End | < 500 ms |

### 3.2 Security Testing in CI

| Maßnahme | Tool | Iteration | Aufwand |
|---|---|---|---|
| SAST | SpotBugs + FindSecBugs-Plugin | It.21 | M |
| Dependency-Scanning | OWASP Dependency-Check Maven-Plugin | It.21 | S |
| DAST | OWASP ZAP (Docker, gegen laufende Compose-Infrastruktur) | It.21 | L |
| Secret-Scanning | git-secrets / Trivy im CI | It.21 | S |

**OWASP-Top-10 Checklist für v1.0:**

| # | Risiko | Aktueller Status |
|---|---|---|
| A01 | Broken Access Control | `ExpenseController` TenantId-Lücke — offen (US-INFRA-030) |
| A02 | Cryptographic Failures | Keycloak verwaltet Secrets — OK |
| A03 | Injection | Spring Data JPA only — OK; kein `nativeQuery` mit User-Input |
| A04 | Insecure Design | TenantId in URL-Pfaden vermieden — OK |
| A05 | Security Misconfiguration | CSRF aktiv, SecurityConfig `@Profile("!test")` — OK |
| A06 | Vulnerable Components | Dependency-Check fehlt — offen |
| A07 | Identification/Auth Failures | Passwort-Reset fehlt — offen (It.20) |
| A08 | Software/Data Integrity | Event-Schema-Versioning fehlt — offen (It.22) |
| A09 | Logging Failures | Kein PII-Logging-Audit — offen |
| A10 | SSRF | Jsoup-URL-Import ohne Allowlist — prüfen |

### 3.3 Accessibility (a11y)

**Status: kein Test vorhanden**

- axe-core via `@axe-core/playwright` in bestehende Playwright-Tests integrieren
- Ziel: 0 Critical/Serious Violations auf allen Hauptseiten (Dashboard, Trip-Detail, Expense-Settlement)
- Lighthouse CI im GitHub-Actions-Workflow für Performance-Score ≥ 80

### 3.4 Contract Tests (RabbitMQ Events)

**Status: kritische Lücke — 19 Events ohne Vertrag**

Empfohlenes Tooling: **Spring Cloud Contract** (bereits im Spring-Ökosystem) oder Pact JVM.

Event-Inventar für Contract-Abdeckung (Priorität 1):

| Event | Publisher | Consumer(s) |
|---|---|---|
| `TenantCreated` | IAM | Trips |
| `AccountRegistered` | IAM | Trips |
| `MemberAddedToTenant` | IAM | Trips |
| `DependentAddedToTenant` | IAM | Trips |
| `MemberRemovedFromTenant` | IAM | Trips |
| `DependentRemovedFromTenant` | IAM | Trips |
| `TenantDeleted` | IAM | Trips |
| `TripCreated` | Trips | Expense |
| `ParticipantJoinedTrip` | Trips | Expense |
| `TripCompleted` | Trips | Expense |

Vorgehensweise:
1. Consumer-Tests: Trips/Expense-Seite definiert erwartetes Payload-Schema als Pact-Vertrag
2. Publisher-Tests: IAM/Trips-Seite verifiziert Konformität beim Build
3. Integration in Maven-Build: `./mvnw verify` schlägt bei Schema-Verletzung fehl

### 3.5 Resilience Testing

| Szenario | Werkzeug | Priorität |
|---|---|---|
| RabbitMQ-Down während Publish | Spring Rabbit Test (MockConnectionFactory) | Hoch |
| Postgres-Failover | Testcontainers + Failsafe | Mittel |
| SCS-Down (Trips down, IAM reagiert) | WireMock / Network partitioning | Niedrig |
| DLQ-Requeue nach transientem Fehler | Spring Rabbit Test | Hoch |

---

## 4. E2E Hardening Roadmap

### Bekannte strukturelle Schwächen

| Problem | Risiko | Maßnahme |
|---|---|---|
| 2 flaky Tests in `07-expense-navigation-and-lifecycle.feature` | CI-Instabilität | It.18: HTMX-Settle-Helper + Retry-Metrik M-018-01 |
| `signUpAndLogin()` hat bedingte Workarounds (Zeilen 100–113) | Maskiert Bug #1 | It.18: Verification-Flow direkt testen |
| `getVerificationLinkFromMailpit()` gibt null zurück statt Exception | Stille Fehlschläge | It.18: Werfen statt null-return |
| Kein Multi-User-Concurrency-Test | Race-Conditions unsichtbar | It.21 |
| Kein Browser-Back-Button-Test nach HTMX-Swap | UX-Regression | It.20 |
| Mobile Viewport nicht getestet (PicoCSS responsive) | Mobile-Regressions | It.19 |
| Test-Data-Isolation: alle Tests laufen sequenziell | Skalierungsproblem | It.22 |

### HTMX-spezifische Stabilisierung

- Einheitlicher `waitForHtmxSettle()` Helper in `E2ETestBase` (ersetzt ad-hoc Sleeps)
- `hx-indicator` Sichtbarkeits-Assert vor und nach Swap
- Network-Request-Interceptor zum Verifizieren dass kein `HX-Trigger` verloren geht

---

## 5. QA Iterations-Roadmap (It.18–23)

### It.18 — E2E Stabilisierung + Notification-Tests

| Story-Ref | QA-Aufgabe | Deliverable |
|---|---|---|
| M-018-01 | E2E-Flake in Feature 07 beheben | 0 flaky tests, Retry-Metrik im CI |
| M-018-01 | `E2ETestBase` HTMX-Settle-Helper | `waitForHtmxSettle()` Methode |
| S18-x (Notification) | BDD-Szenarien für E-Mail-Benachrichtigungen | `19-email-notifications.feature` |
| S18-x (Notification) | E2E: Mailpit-Inbox-Assert nach HTMX-Action | Szenario in neuem Feature-File |
| S18-x (Multi-Organizer) | BDD + E2E für Rollenübergabe | `20-multi-organizer.feature` |

### It.19 — Accessibility + Lighthouse CI

| Aufgabe | Deliverable |
|---|---|
| axe-core in Playwright-Basis-Klasse integrieren | `assertNoA11yViolations(page)` Helper |
| Lighthouse CI konfigurieren (GitHub Actions) | Performance-Score ≥ 80 Gate |
| Mobile Viewport (375px) für alle BDD-Szenarien | `@Tag("mobile")` Cucumber-Profil |
| BDD-Szenarien für Recipe-CRUD und URL-Import-Fehlerfall | `21-recipe-import-errors.feature` |

### It.20 — Externe Einladung + Passwort-Reset BDD

| Aufgabe | Deliverable |
|---|---|
| BDD: Externer Einladungs-Fehlerpfad (Link abgelaufen, Email-Conflict) | `22-external-invitation-error-paths.feature` |
| BDD + E2E: Passwort-Reset-Flow vollständig | `23-password-reset.feature` |
| E2E: Browser-Back nach HTMX-Swap (Regression-Test) | Test in `NavigationIT` oder separatem Feature |

### It.21 — Security Hardening + Contract Tests

| Aufgabe | Deliverable |
|---|---|
| **US-INFRA-030**: Tenant-Isolation Property-Tests | Parameterisierte Tests in allen 3 SCS |
| Pact Consumer-Contracts für alle 10 Priority-1-Events | `pact/` Verzeichnis, Maven-Build-Gate |
| OWASP Dependency-Check im Maven-Build | Build-Fehler bei CVSS ≥ 7.0 |
| SpotBugs + FindSecBugs in CI | 0 high-severity Findings Gate |
| OWASP ZAP gegen Docker Compose | Automated scan in CI mit Baseline-Profil |
| `ExpenseController` TenantId-Lücke: Test + Fix | Neuer Controller-Test + Security-Fix |

### It.22 — Infrastruktur-Qualität + Performance-Baseline

| Aufgabe | Deliverable |
|---|---|
| Gatling-Maven-Plugin, Baseline-Szenarien (Dashboard, Trip-Liste) | `travelmate-load-tests/` Modul |
| SLO-Definitionen in `docs/slo.md` | Dokumentiertes Akzeptanzkriterium |
| JaCoCo-Gate im CI (≥ 75 % Line, ≥ 70 % Branch) | `jacoco-check` Maven-Goal aktiviert |
| Event-Schema-Versioning (ADR-0026) + Contract-Test-Erweiterung | Bestehende Pact-Verträge versioniert |
| ADR-0023 (Performance), ADR-0024 (a11y), ADR-0025 (Contract Tests) | Akzeptierte ADRs |

### It.23 — v1.0.0 Release

| Aufgabe | Deliverable |
|---|---|
| Vollständiger Regressions-Lauf (20× sequenziell) | 0 transiente E2E-Fehler nachgewiesen |
| Deploy Smoke Tests (Health-Check + Keycloak-Login + Dashboard) | `smoke/` Test-Klasse |
| Alle Quality Gates grün (siehe Abschnitt 6) | Release-Checkliste abgehakt |

---

## 6. v1.0 Quality Gate

Alle folgenden Kriterien müssen vor dem Tag `v1.0.0` erfüllt sein:

| Kategorie | Kriterium | Messung |
|---|---|---|
| **Coverage** | Line Coverage ≥ 75 % | JaCoCo `jacoco-check` Goal im Maven-Build |
| **Coverage** | Branch Coverage ≥ 70 % | JaCoCo `jacoco-check` Goal im Maven-Build |
| **E2E-Stabilität** | 0 transiente Fehler in 20 sequenziellen Runs | CI-Protokoll (Retry-Metrik M-018-01) |
| **SAST** | 0 Critical/High Findings | SpotBugs + FindSecBugs Report |
| **Dependency-Security** | 0 Known CVEs mit CVSS ≥ 7.0 | OWASP Dependency-Check Report |
| **DAST** | Keine OWASP-Top-10-Findings (High/Critical) | OWASP ZAP Baseline Scan |
| **Contract Tests** | Alle 10 Priority-1-Events durch Pact abgedeckt | Pact-Verifikation im Build |
| **Performance** | p95 Dashboard < 1 000 ms @ 50 VU | Gatling-Report |
| **Performance** | p95 Trip-Erstellung < 2 000 ms @ 50 VU | Gatling-Report |
| **Accessibility** | 0 axe-core Critical/Serious Violations | Playwright a11y-Report |
| **Accessibility** | Lighthouse Performance Score ≥ 80 | Lighthouse CI |
| **ADRs** | ADR-0023 bis ADR-0026 akzeptiert | `docs/adr/` |
| **OWASP** | Alle Top-10-Punkte adressiert (A01–A10) | Security-Review-Dokument |

---

## Anhang: Offene Risiken

| Risiko | Wahrscheinlichkeit | Impact | Mitigation |
|---|---|---|---|
| Event-Schema-Bruch IAM→Trips durch Refactoring | Mittel | Hoch | Pact-Contract-Tests (It.21) |
| `ExpenseController` Cross-Tenant-Lücke in Produktion | Niedrig | Kritisch | US-INFRA-030 (It.21, Prio 1) |
| Gatling-Baseline zeigt p95 > SLO vor Release | Mittel | Mittel | Frühzeitig in It.22 messen, nicht erst It.23 |
| Flaky E2E-Tests blockieren Release-Entscheidung | Niedrig (nach It.18) | Hoch | M-018-01 Retry-Metrik als CI-Gate |
| PicoCSS-Update bricht Responsive-Layout ohne Test | Mittel | Niedrig | Mobile-Viewport-Tests in It.19 |
