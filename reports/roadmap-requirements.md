# Travelmate — Roadmap Requirements Report
# Path to v1.0.0 (Production-Readiness)

**Stand**: 2026-04-26 | **Basis**: v0.17.0 (commit cafd37f) | **Tests**: 1064 grün, 287/289 E2E grün

---

## 1. Backlog Gap Analysis

### E-IAM-01: Sign-Up & Onboarding

| Story | Status | v1.0? |
|-------|--------|-------|
| US-IAM-001 Self-Service Sign-Up | ✅ It.3 | — |
| US-IAM-002 Login After Sign-Up | ✅ It.3 | — |
| US-IAM-003 Email Verification | ✅ It.3 | — |
| US-IAM-004 Invite Member to Travel Party | ✅ It.4 | — |

**Fazit**: Epic vollständig.

---

### E-IAM-02: Travel Party Management

| Story | Status | v1.0? |
|-------|--------|-------|
| US-IAM-010 View Dashboard | ✅ It.3 | — |
| US-IAM-011 Delete Travel Party | ✅ It.3 | — |
| US-IAM-012 Edit Travel Party Name | OPEN (Could, S) | Post-v1.0 |

**Begründung US-IAM-012**: Nice-to-have. Die Reisepartei-Umbenennung hat keine Downstream-Auswirkungen, die eine Invariante verletzen könnten — der Name ist nur Anzeigetext. Der User Journey von v1.0 kommt ohne diese Funktion aus.

---

### E-IAM-03: Member Management

| Story | Status | v1.0? |
|-------|--------|-------|
| US-IAM-020 List Members | ✅ It.3 | — |
| US-IAM-021 Remove Member | ✅ It.3 | — |
| US-IAM-022 Edit Member Profile | OPEN (Could, S) | Post-v1.0 |

**Begründung US-IAM-022**: Profilbearbeitung (Name) ist kein Blocking-Feature für den Kern-User-Journey. Erster Release ohne Self-Service-Profilediting ist üblich.

---

### E-IAM-04: Companion Management

| Story | Status | v1.0? |
|-------|--------|-------|
| US-IAM-030 Add Companion | ✅ It.3 | — |
| US-IAM-031 Remove Companion | ✅ It.3 | — |
| US-IAM-032 Edit Companion Details | OPEN (Could, S) | Post-v1.0 |

**Begründung US-IAM-032**: Same reasoning as US-IAM-022. Companion-Daten (Name, Geburtsdatum) sind in der aktuellen Domain nicht immutabel — ein Edit-Flow ist wünschenswert, aber kein Blocker.

---

### E-IAM-05: Multi-Organizer Support

| Story | Status | v1.0? |
|-------|--------|-------|
| US-IAM-040 Assign Organizer Role | OPEN → **It.18** (Should, M) | **Required** |
| US-IAM-041 Revoke Organizer Role | OPEN → **It.18** (Should, S) | **Required** |

**Begründung**: US-EXP-041 (Four-Eyes Receipt Review) setzt semantisch voraus, dass mindestens zwei Organizer existieren können. Ohne US-IAM-040/041 bleibt der Vier-Augen-Prozess für Single-Organizer-Parties ein Dead End. Das Feature ist im Trip-Aggregat bereits modelliert (`organizerIds`), die Keycloak-Adapter sind implementiert. Die Kosten sind niedrig, der Nutzen für Produktionsreife hoch.

---

### E-IAM-06: Notification Service

| Story | Status | v1.0? |
|-------|--------|-------|
| US-IAM-050 Email for Trip Invitation | OPEN → **It.18** (Must, M) | **Required** |
| US-IAM-051 SMS Notification | OPEN (Could, M) | Post-v1.0 |
| US-IAM-052 Notification Preferences | OPEN (Could, S) | Post-v1.0 |

**Begründung US-IAM-050**: Eine Einladung ohne E-Mail-Benachrichtigung erfordert, dass eingeladene Parteien aktiv in der App nachschauen. Das ist im Produktivbetrieb kein akzeptables UX-Muster. Die Infrastruktur (Mailpit, Spring Mail) ist vorhanden.

---

### E-IAM-07: Authentication & Security Enhancements

| Story | Status | v1.0? |
|-------|--------|-------|
| US-IAM-060 Password Reset via Keycloak | OPEN (Must, S) | **Required** |
| US-IAM-061 SMS as Second Factor | OPEN (Won't, L) | Post-v1.0 |

**Begründung US-IAM-060**: Keycloak "Forgot password"-Flow ist eine Konfigurationsoption (keine Codeänderung), erfordert aber bewusste Aktivierung im Realm + Keycloak-Theme-Anpassung für CI/Brand-Konsistenz. Ohne Password-Reset ist v1.0 für echte Nutzer nicht produktionsreif.

---

### E-TRIPS-01: Trip Lifecycle Management

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-001 Create Trip | ✅ It.3 | — |
| US-TRIPS-002 View Trip List | ✅ It.3 | — |
| US-TRIPS-003 View Trip Details | ✅ It.3 | — |
| US-TRIPS-004 Trip Status Transitions | ✅ It.3–5 | — |
| US-TRIPS-005 Edit Trip Details | ✅ It.17 | — |
| US-TRIPS-006 Delete Trip | ✅ It.17 | — |

**Fazit**: Epic vollständig.

---

### E-TRIPS-02: Invitation & Participation

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-010 Invite Travel Party | ✅ It.3 | — |
| US-TRIPS-011 Accept Invitation | ✅ It.3 | — |
| US-TRIPS-012 Decline Invitation | ✅ It.3 | — |
| US-TRIPS-013 Invite External Travel Party | OPEN (Should, L) | **Required** |
| US-TRIPS-014 Remove Participant | ✅ It.13 | — |

**Begründung US-TRIPS-013**: Der v1.0-User-Journey beginnt mit dem Szenario "Organizer lädt befreundete Familien ein". In der aktuellen Implementierung können nur bereits registrierte Travel Parties eingeladen werden. Externe Einladung per E-Mail ist ein Kern-Onboarding-Funnel für neue Nutzer — ohne dieses Feature kann die App nicht organisch wachsen. Abhängigkeit: US-IAM-050 (E-Mail-Infrastruktur).

---

### E-TRIPS-03: Stay Periods & Scheduling

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-020 Set Stay Period | ✅ It.3–5 | — |
| US-TRIPS-021 View Participant Schedule | OPEN (Should, M) | Post-v1.0 |
| US-TRIPS-022 Per-Day Participant Count | OPEN (Should, S) | Post-v1.0 |

**Begründung**: Die Kalender-/Grid-Ansicht (US-TRIPS-021) ist komfortabel, aber kein Blocker. Trip-Details zeigen bereits Participants mit StayPeriods. Per-Day-Count ist ein Komfort-Feature für Meal Planning, das heute manuell geschätzt werden kann.

---

### E-TRIPS-04: Meal Planning

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-030 Create Meal Plan | ✅ It.7 | — |
| US-TRIPS-031 Mark SKIP | ✅ It.7 | — |
| US-TRIPS-032 Mark EATING_OUT | ✅ It.7 | — |
| US-TRIPS-033 Assign Recipe to MealSlot | ✅ It.7 | — |
| US-TRIPS-034 View Meal Plan Overview | ✅ It.7 | — |
| US-TRIPS-035 Assign Kitchen Duty | ✅ It.17 | — |

**Fazit**: Epic vollständig.

---

### E-TRIPS-05: Recipe Management

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-040 Create Recipe Manually | ✅ It.7 | — |
| US-TRIPS-041 Import Recipe from URL | OPEN → **It.19** (Should, L) | Post-v1.0 |
| US-TRIPS-042 Edit Recipe | OPEN (Should, S) | **Required** |
| US-TRIPS-043 Delete Recipe | OPEN (Should, S) | **Required** |
| US-TRIPS-044 List Recipes | ✅ It.7 | — |

**Begründung US-TRIPS-042/043**: Edit und Delete für Recipes sind elementare CRUD-Operationen. Eine App, die Daten eintragen aber nicht korrigieren oder löschen lässt, ist nicht produktionsreif. Beide sind S-Größe.

**Begründung US-TRIPS-041**: Der URL-Import ist eine Komfort-Funktion (Should). Das manuelle Erfassen ist funktional ausreichend für v1.0. Technisch aufwändig (Jsoup, SSRF-Guard) — Post-v1.0 nach dem Accommodation-Import-Pattern (It.19).

---

### E-TRIPS-06: Shopping List

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-050 Auto-Generate Shopping List | ✅ It.8 | — |
| US-TRIPS-051 Add Manual Item | ✅ It.8 | — |
| US-TRIPS-052 Assign Item to Myself | ✅ It.8 | — |
| US-TRIPS-053 Mark as Purchased | ✅ It.8 | — |
| US-TRIPS-054 Real-Time Updates | ✅ It.8 (HTMX Polling) | — |
| US-TRIPS-055 Bring App Integration | OPEN (Could, L) | Post-v1.0 |

**Fazit**: Kern-Epic vollständig. Bring-Integration ist Post-v1.0 (keine stabilen API-Docs).

---

### E-TRIPS-07: Location & Accommodation

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-060 Add Accommodation Details | ✅ It.9 | — |
| US-TRIPS-061 Import Location Info from URL | ✅ It.10 (HtmlAccommodationImportAdapter) | — |
| US-TRIPS-062 Accommodation Poll (alt.) | Superseded by E-TRIPS-08 | — |

**Fazit**: Epic vollständig (US-TRIPS-062 wurde durch S14-D/E ersetzt).

---

### E-TRIPS-08: Collaborative Trip Decision Making

| Story | Status | v1.0? |
|-------|--------|-------|
| US-TRIPS-080 Create Date Poll | ✅ It.14 | — |
| US-TRIPS-081 Vote in Date Poll | ✅ It.14 | — |
| US-TRIPS-082 Confirm Date Poll | ✅ It.14 | — |
| US-TRIPS-083 Accommodation Candidates | ✅ It.14–15 | — |
| US-TRIPS-084 Vote for Accommodation | ✅ It.14–15 | — |
| US-TRIPS-085 Trip Lifecycle Alignment | ✅ It.14 (ADR-0021) | — |

**Fazit**: Epic vollständig (v0.14.0–v0.15.8).

---

### E-EXP-01: Receipt Management

| Story | Status | v1.0? |
|-------|--------|-------|
| US-EXP-001 Create Ledger from Trip Event | ✅ It.5 | — |
| US-EXP-002 Submit Receipt by Photo (OCR) | ✅ It.10 (partial — scan + manual OCR) | — |
| US-EXP-003 Manual Receipt Entry | ✅ It.5 | — |
| US-EXP-004 View All Receipts | ✅ It.5 | — |
| US-EXP-005 Edit Own Receipt | OPEN (Should, S) | **Required** |

**Begründung US-EXP-005**: Ein Beleg, der nicht editierbar ist, erzwingt Löschen und Neuanlage bei Tippfehlern. Das ist für v1.0 nicht akzeptabel.

---

### E-EXP-02: Expense Tracking & Categories

| Story | Status | v1.0? |
|-------|--------|-------|
| US-EXP-010 Categorize Receipt | ✅ It.6 | — |
| US-EXP-011 Track Per-Day Costs | OPEN (Should, M) | Post-v1.0 |
| US-EXP-012 Accommodation Cost Entry | ✅ It.6–9 | — |
| US-EXP-013 Advance Payment | ✅ It.9 | — |

**Begründung US-EXP-011**: Per-Day-Cost-View ist eine Analytics-Funktion. Die Settlement-Berechnung läuft ohne sie korrekt. Post-v1.0.

---

### E-EXP-03: Weighting & Splitting

| Story | Status | v1.0? |
|-------|--------|-------|
| US-EXP-020 Define Participant Weighting | ✅ It.5–6 | — |
| US-EXP-021 Equal Splitting (Default) | ✅ It.5–6 | — |
| US-EXP-022 Custom Splitting per Receipt | OPEN (Could, M) | Post-v1.0 |

**Begründung US-EXP-022**: Weighted-equal ist für den typischen Familien-Urlaubsfall ausreichend. Custom Split ist ein Power-User-Feature.

---

### E-EXP-04: Settlement & Calculation

| Story | Status | v1.0? |
|-------|--------|-------|
| US-EXP-030 Calculate Settlement | ✅ It.6 | — |
| US-EXP-031 View Settlement Summary | ✅ It.6 | — |
| US-EXP-032 Settlement per Category | ✅ It.10 | — |
| US-EXP-033 Export Settlement as PDF | ✅ It.10 | — |

**Fazit**: Epic vollständig.

---

### E-EXP-05: Four-Eyes Review Process

| Story | Status | v1.0? |
|-------|--------|-------|
| US-EXP-040 Submit Receipt for Review | ✅ It.6 | — |
| US-EXP-041 Review and Approve Receipt | ✅ It.6 | — |
| US-EXP-042 Re-Submit Rejected Receipt | ✅ It.9 | — |

**Fazit**: Epic vollständig.

---

### E-INFRA-01: CI/CD Pipeline

| Story | Status | v1.0? |
|-------|--------|-------|
| US-INFRA-001 GitHub Actions CI | OPEN (Should, M) | **Required** |
| US-INFRA-002 Docker Image Build in CI | OPEN (Could, M) | Post-v1.0 |

**Begründung US-INFRA-001**: Ein v1.0-Release ohne automatisierte CI-Pipeline ist ein Regressionsrisiko. Die bestehende lokale Test-Infrastruktur (1064 Tests, E2E) muss in CI abgesichert sein.

---

### E-INFRA-02: Observability & Monitoring

| Story | Status | v1.0? |
|-------|--------|-------|
| US-INFRA-010 Micrometer + Prometheus | OPEN (Should, M) | **Required** |
| US-INFRA-011 Grafana Dashboards | OPEN (Could, M) | Post-v1.0 |
| US-INFRA-012 Centralized Logging | OPEN (Could, M) | Post-v1.0 |

**Begründung US-INFRA-010**: Prometheus-Endpoint via Actuator ist eine einzige Dependency + Config — minimaler Aufwand, maximale Betriebsreife. Grafana-Dashboards und zentrales Logging sind Post-v1.0.

---

### E-INFRA-03: Architecture Fitness

| Story | Status | v1.0? |
|-------|--------|-------|
| US-INFRA-020 ArchUnit Tests | ✅ It.5 | — |
| US-INFRA-021 JaCoCo Coverage Thresholds | ✅ It.5 | — |
| US-INFRA-022 Dead Letter Queue | ✅ It.5 | — |

**Fazit**: Epic vollständig.

---

### E-INFRA-04: Security Hardening

| Story | Status | v1.0? |
|-------|--------|-------|
| US-INFRA-030 Tenant Isolation Security Tests | OPEN (Must, M) | **Required** |
| US-INFRA-031 OWASP Dependency Check | OPEN (Should, S) | **Required** |
| US-INFRA-032 Security Integration Tests | OPEN (Should, M) | **Required** |

**Begründung**: Security-Tests sind non-negotiable für v1.0. Tenant-Isolation ist die kritischste Sicherheitseigenschaft der gesamten Plattform (jede Abfrage ist TenantId-scoped — das muss automatisiert verifiziert sein).

---

### E-INFRA-05: PWA & Offline Support

| Story | Status | v1.0? |
|-------|--------|-------|
| US-INFRA-040 Service Worker Offline | OPEN (Could, XL) | Post-v1.0 |
| US-INFRA-041 PWA Manifest | ✅ It.9 | — |
| US-INFRA-042 Lighthouse CI | OPEN (Should, M) | **Required** |

**Begründung US-INFRA-042**: Lighthouse-Score >= 90 (Mobile Performance + Accessibility) ist für eine mobile-first App eine Qualitäts-Baseline, nicht ein Extra. Service Worker/Offline ist ein XL-Feature, das nach v1.0 folgt.

---

### E-INFRA-06: i18n, Documentation & Quality

| Story | Status | v1.0? |
|-------|--------|-------|
| US-INFRA-050 i18n (DE + EN) | ✅ It.3+ | — |
| US-INFRA-051 E2E Tests | ✅ It.4+ | — |
| US-INFRA-052 Arc42 Documentation | ✅ (laufend) | — |
| US-INFRA-053 Responsive CSS Theme | ✅ It.11 | — |
| US-INFRA-054 Event Contract Versioning | OPEN → It.18 als ADR-0025 (Should, M) | **Required** |
| US-INFRA-055 Transactional Outbox | OPEN (Could, XL) | Post-v1.0 |

**Begründung US-INFRA-054**: ADR-0025 ist bereits in Iteration 18 beschlossen. Die ArchUnit-Konformanzprüfung für Event-Naming und Pflichtfelder ist die Implementierung dieser ADR — niedrig im Aufwand, hoch im Wert für Maintenance.

---

## 2. MVP-Definition für v1.0 — Organizer User Journey

Der Kern-User-Journey eines Reise-Organisators definiert, was v1.0 leisten muss:

```
[1] Registrierung & Einladung
    Sign-Up → Travel Party anlegen → Mitglieder/Companions verwalten
    → Freunde per E-Mail einladen (extern)                     [US-IAM-004, US-TRIPS-013]
    → Passwort vergessen / Reset                               [US-IAM-060]
    → Multi-Organizer (zweiten Org. ernennen)                  [US-IAM-040/041]

[2] Reise planen
    Trip anlegen → DatePoll erstellen → Abstimmung abwarten
    → Reisezeitraum bestätigen                                 [US-TRIPS-080–082]
    → AccommodationPoll: Kandidaten vorschlagen + abstimmen
    → Unterkunft buchen / bestätigen                           [US-TRIPS-083–084]
    → StayPeriods der Teilnehmer setzen                        [US-TRIPS-020]

[3] Mahlzeiten & Einkauf
    MealPlan generieren → Recipes manuell erfassen / zuweisen  [US-TRIPS-030–034, 040, 044]
    → Recipe editieren/löschen                                 [US-TRIPS-042/043]
    → ShoppingList automatisch generieren + kollaborativ bearbeiten [US-TRIPS-050–054]
    → Küchendienst zuweisen                                    [US-TRIPS-035]

[4] Kosten & Abrechnung
    Belege erfassen (Foto + manuell) → editieren               [US-EXP-002–005]
    → Kategorisieren + Vier-Augen-Review                       [US-EXP-010, 040–042]
    → Gewichtungen + Party-Settlement                          [US-EXP-020/021, 030/031]
    → Unterkunftskosten + Vorauszahlungen                      [US-EXP-012/013]
    → PDF-Export                                               [US-EXP-033]

[5] Reise abschließen
    Trip auf COMPLETED setzen → Abrechnung finalisieren
    → Settlement per Kategorie (Übersicht)                     [US-EXP-032]
```

Ein User-Journey gilt als **vollständig**, wenn kein Schritt einen "Dead End" produziert (d.h. eine Aktion, die in v1.0 nicht rückgängig gemacht, korrigiert oder abgeschlossen werden kann).

---

## 3. Iteration Roadmap — v0.18 bis v1.0

### Iteration 18 (v0.18.0) — IAM Consolidation + Notification Foundation
**Theme**: Multi-Organizer + E-Mail-Benachrichtigung + Infrastrukturhygiene

| Story | Epic | Prio | Größe | BC |
|-------|------|------|-------|----|
| S18-D01 GlobalExceptionHandler vereinheitlichen | Cross | Should | S | IAM+Trips+Expense |
| S18-A01 Organizer-Rolle vergeben (US-IAM-040) | E-IAM-05 | Should | M | IAM |
| S18-A02 Organizer-Rolle entziehen (US-IAM-041) | E-IAM-05 | Should | S | IAM |
| S18-C01 Einladungs-E-Mail (US-IAM-050) | E-IAM-06 | Must | M | Trips |

**Deferred**: S18-B01 Rezept-Import → Iteration 19
**Dependency für Folgeiterationen**: S18-A01 entsperrt US-EXP-041-Vollständigkeit (Review ohne 2. Organizer bisher semantisch eingeschränkt); S18-C01 ist Prerequisit für US-TRIPS-013.

---

### Iteration 19 (v0.19.0) — Recipe + External Invitation + CI Foundation
**Theme**: Recipe vollständigen CRUD-Kreis schließen + Externe Einladung + CI-Pipeline

| Story | Epic | Prio | Größe | BC |
|-------|------|------|-------|----|
| S19-A01 Recipe editieren (US-TRIPS-042) | E-TRIPS-05 | Should | S | Trips |
| S19-A02 Recipe löschen (US-TRIPS-043) | E-TRIPS-05 | Should | S | Trips |
| S19-B01 Rezept aus URL importieren (US-TRIPS-041) | E-TRIPS-05 | Should | L | Trips |
| S19-C01 GitHub Actions CI-Pipeline (US-INFRA-001) | E-INFRA-01 | Should | M | Infra |

**Dependency**: S19-B01 benötigt S18-C01 (E-Mail-Infrastruktur fertig), S19-C01 kann parallel.
**Begründung**: Recipe-Edit/Delete sind S-Größe und schließen eine offensichtliche CRUD-Lücke. Rezept-Import (L) gibt Iteration 19 ihr Kernfeature. CI-Pipeline ist Must-Have vor v1.0 und hat keine Story-Dependency.

---

### Iteration 20 (v0.20.0) — External Trip Invitation + Password Reset
**Theme**: Externer Onboarding-Funnel + Auth-Hardening

| Story | Epic | Prio | Größe | BC |
|-------|------|------|-------|----|
| S20-A01 Externe Trip-Einladung (US-TRIPS-013) | E-TRIPS-02 | Should | L | Trips+IAM |
| S20-B01 Password Reset via Keycloak (US-IAM-060) | E-IAM-07 | Must | S | IAM+Keycloak |
| S20-C01 E2E-Stabilisierung (2 bekannte Flakes) | E-INFRA-06 | Must | S | e2e |

**Dependency**: S20-A01 benötigt S18-C01 (E-Mail-Infrastruktur) und US-IAM-050 (Invitation-E-Mail-Pattern). Password Reset ist Keycloak-Konfiguration + Theme-Anpassung (S). E2E-Flakes werden zur Pflicht vor v1.0.
**Begründung**: Externe Einladung ist der primäre Akquisitionskanal für neue Nutzer — kein virales Wachstum ohne dieses Feature.

---

### Iteration 21 (v0.21.0) — Receipt Edit + Security Hardening
**Theme**: Expense UX-Kompletierung + Security-Tests

| Story | Epic | Prio | Größe | BC |
|-------|------|------|-------|----|
| S21-A01 Beleg bearbeiten (US-EXP-005) | E-EXP-01 | Should | S | Expense |
| S21-B01 Tenant Isolation Security Tests (US-INFRA-030) | E-INFRA-04 | Must | M | All SCS |
| S21-B02 Security Integration Tests JWT Roles (US-INFRA-032) | E-INFRA-04 | Should | M | All SCS |
| S21-B03 OWASP Dependency Check (US-INFRA-031) | E-INFRA-04 | Should | S | CI |

**Begründung**: Security-Tests sind für v1.0 non-negotiable. Tenant Isolation Tests decken die kritischste Sicherheitseigenschaft ab. Receipt-Edit schließt die letzte offensichtliche CRUD-Lücke im Expense-Flow.

---

### Iteration 22 (v0.22.0) — Event Versioning + Observability + Lighthouse
**Theme**: Infra-Qualität vor v1.0-Release

| Story | Epic | Prio | Größe | BC |
|-------|------|------|-------|----|
| S22-A01 Event Contract Versioning ArchUnit (US-INFRA-054, ADR-0025) | E-INFRA-06 | Should | M | Common+All |
| S22-B01 Micrometer + Prometheus (US-INFRA-010) | E-INFRA-02 | Should | M | All SCS |
| S22-C01 Lighthouse CI (US-INFRA-042) | E-INFRA-05 | Should | M | e2e+CI |
| S22-D01 GlobalExceptionHandler Extraktion (ADR-0026) | Cross | Should | M | travelmate-web-commons (neu) |

**Begründung**: Diese Iteration schließt die Infrastruktur-Lücken, die für einen Production-Grade-Release zwingend sind. ADR-0026-Implementierung wird hier gezogen, nachdem It.21-E2E-Tests als Sicherheitsnetz für das Refactoring stehen.

---

### Iteration 23 (v0.23.0) — Arc42 + ADR-Abschluss + v1.0-Release-Candidate
**Theme**: Dokumentation, offene ADRs schließen, RC-Testing

| Story | Epic | Prio | Größe | BC |
|-------|------|------|-------|----|
| S23-A01 Arc42-Update (09-design-decisions, 10-quality-requirements, 11-risks) | E-INFRA-06 | Should | M | Docs |
| S23-B01 ADR-0024/0025/0026 fertigstellen und einchecken | E-INFRA-06 | Should | S | Docs |
| S23-C01 Vollständiger E2E-Lauf + Acceptance-Sign-Off | E-INFRA-06 | Must | M | e2e |
| S23-D01 v1.0.0-Release (Tag + Changelog) | — | Must | S | — |

**Begründung**: Kein Release ohne aktualisierte Arc42-Dokumentation (ADR-0015 im Projekt). Der RC-Lauf (287/289 → Ziel: 289/289 grün) ist das Acceptance-Gate.

---

### Iterationen 24–25 (Post-v1.0) — Enhancement Wave
**Theme**: Power-User-Features + Offline + Analytics

| Iteration | Theme | Key Stories |
|-----------|-------|-------------|
| It.24 | UX & Analytics | US-TRIPS-021 (Participant Schedule Grid), US-EXP-011 (Per-Day Costs), US-IAM-012 (Travel Party Rename) |
| It.25 | PWA & Offline | US-INFRA-040 (Service Worker), US-INFRA-012 (Centralized Logging), US-TRIPS-055 (Bring-Integration) |

---

## 4. Acceptance Criteria für v1.0.0

### 4.1 Story Completion

| Kategorie | Kriterium | Zielwert |
|-----------|-----------|----------|
| Must-Stories | Alle Must-Priorisierung Stories Done | 100% (0 offene Must-Stories) |
| Should-Stories für v1.0 | Mindestens die in Section 3 markierten Should-Stories Done | Siehe Liste unten |
| User Journey Vollständigkeit | Jeder Schritt des Kern-User-Journey in Section 2 ist ausführbar ohne Dead End | 100% |

**Must-Done-Stories für v1.0** (aktuell noch OPEN):

- US-IAM-040 Organizer-Rolle vergeben (It.18)
- US-IAM-041 Organizer-Rolle entziehen (It.18)
- US-IAM-050 E-Mail bei Einladung (It.18)
- US-IAM-060 Password Reset (It.20)
- US-TRIPS-013 Externe Trip-Einladung (It.20)
- US-TRIPS-042 Recipe editieren (It.19)
- US-TRIPS-043 Recipe löschen (It.19)
- US-EXP-005 Beleg bearbeiten (It.21)
- US-INFRA-001 GitHub Actions CI (It.19)
- US-INFRA-010 Micrometer Prometheus (It.22)
- US-INFRA-030 Tenant Isolation Tests (It.21)
- US-INFRA-031 OWASP Dependency Check (It.21)
- US-INFRA-032 Security Integration Tests (It.21)
- US-INFRA-042 Lighthouse CI (It.22)
- US-INFRA-054 Event Contract Versioning (It.22)

**Anzahl offener Required-Stories**: 15

---

### 4.2 Test- und Qualitätsmetriken

| Metrik | Zielwert | Messung |
|--------|----------|---------|
| Unit + Integration Tests | >= 1200 Tests grün | `./mvnw clean verify` |
| E2E Tests | 289/289 grün (0 Flakes) | `./mvnw -Pe2e verify` x 3 aufeinanderfolgend |
| JaCoCo Line Coverage | >= 80% pro SCS-Modul | JaCoCo Maven Plugin |
| ArchUnit | 0 Verletzungen in allen Modulen | Maven Build |
| Lighthouse Mobile | Performance >= 90, Accessibility >= 90 | Lighthouse CI |
| Playwright Retry Rate | 0 Retries pro Lauf | GitHub Actions Artefakte |
| OWASP CVEs | 0 Critical/High | dependency-check-maven |
| Event Schema ArchUnit | 0 Verletzungen (Naming + Pflichtfelder) | Common-Modul-Test |

---

### 4.3 Security-Baseline

| Kriterium | Prüfmethode |
|-----------|-------------|
| Kein Cross-Tenant-Zugriff möglich | Automatisierter Test: Tenant-A-User greift auf Tenant-B-Ressource → HTTP 404 |
| Alle Endpoints hinter JWT-Auth | SecurityConfig-Review + Integration Test mit `permitAll` aus Test-Profile |
| Kein Organizer-Endpoint für Participant erreichbar | @WithMockJwtAuth participant-role → HTTP 403 |
| Keine hardcodierten Secrets im Repo | Git-Grep-Scan im CI |

---

### 4.4 UX-Vollständigkeit

| Kriterium | Zielwert |
|-----------|----------|
| Alle CRUD-Entitäten haben Create + Read + Edit + Delete | Recipe, Receipt, Companion, Member (Profile-Edit Post-v1.0 akzeptiert als Ausnahme) |
| Alle Aktionen haben HTMX-Feedback (Toast/Spinner) | 100% gemäß ADR-0013 |
| Alle Fehlerseiten zeigen nutzerfreundliche Meldungen | GlobalExceptionHandler deckt alle 3 SCS identisch ab (ADR-0026) |
| Mobile Viewport (375px) vollständig bedienbar | Lighthouse + manuelle Überprüfung Key-Flows |

---

### 4.5 Dokumentations-Vollständigkeit

| Dokument | Kriterium |
|----------|-----------|
| ADRs | ADR-0001 bis ADR-0026 alle im Status "Akzeptiert" oder "Abgelehnt" (kein "Vorgeschlagen" im Release) |
| Arc42 | Sections 01–12 aktuell, Section 09 enthält alle It.14–23-ADRs |
| Backlog | Alle Done-Stories als ✅ markiert, Iteration-Referenz eingetragen |

---

## 5. Abhängigkeitsgraph (kritischer Pfad)

```
It.18: S18-C01 (E-Mail-Infra)
    └── It.20: S20-A01 (Externe Einladung)   ← Akquisitionskanal

It.18: S18-A01 (Organizer-Rolle vergeben)
    └── US-EXP-041 voll nutzbar (Vier-Augen mit 2 Org.)

It.19: S19-C01 (CI-Pipeline)
    └── It.21: S21-B01/B02/B03 (Security in CI)
    └── It.22: S22-C01 (Lighthouse CI)

It.21: S21-B01 (Tenant Isolation Tests)     ← Security Gate für v1.0
It.21: S21-A01 (Receipt Edit)               ← letzter CRUD-Gap Expense

It.22: S22-D01 (GlobalExceptionHandler Extraktion)
    → benötigt It.21-E2E als Sicherheitsnetz

It.23: v1.0.0-Release
    → benötigt alle Required-Stories aus It.18–22 als Done
    → benötigt 289/289 E2E grün
```

**Kritischer Pfad**: It.18 (E-Mail-Infra) → It.20 (Externe Einladung) → It.21 (Security Tests) → It.22 (Infra-Qualität) → It.23 (v1.0.0).

---

## 6. Zusammenfassung: v1.0 in Zahlen

| Dimension | Wert |
|-----------|------|
| Geplante Iterationen bis v1.0 | It.18 – It.23 (6 Iterationen) |
| Noch offene Required-Stories | 15 |
| Bereits Done-Stories | 64 (von 79 Gesamt-Required) |
| Done-Quote heute | ~81% |
| Post-v1.0 geparkte Stories | 12 (Could/Won't + XL-Features) |
| Kritischster Blocker | US-INFRA-030 (Tenant Isolation Test) |
| Schnellster Quick-Win | US-IAM-060 (Password Reset — Keycloak-Konfiguration, S) |
