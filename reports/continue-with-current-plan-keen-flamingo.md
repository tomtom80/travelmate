# Roadmap to v1.0.0 — Iterations 18-25

**Datum**: 2026-04-26
**Ausgangsversion**: v0.17.0 (commit `cafd37f`, Tag `v0.17.0`)
**Zielversion**: v1.0.0 (production-ready)
**Synthese aus**:
- `reports/roadmap-requirements.md` (Requirements-Engineer)
- `reports/roadmap-architecture.md` (Architect)
- `reports/roadmap-security.md` (Security-Expert)
- `reports/roadmap-qs.md` (QA-Engineer)
- `reports/iteration-18-stories.md` + `reports/iteration-18-architecture.md`

---

## Context

Travelmate ist **fachlich nahe an v1.0** (~81% der produktionsrelevanten Stories ✅, 1064 Tests grün, 23 ADRs, 12 arc42-Sektionen) — aber die **Plattform-/Betriebs-/Hardening-Schicht** ist deutlich unreifer als die Domäne. Der Weg zu v1.0 ist kein Feature-Pfad, sondern ein **Plattform-Reife-Pfad**.

**Konvergenz aller 4 Agents**: v1.0 landet bei **Iter 25** (Architect & Security explizit; Requirements optimistischer mit Iter 23, was aber Plattform-Themen unterschätzt). Iter 18-25 sollten zu **~60% nicht-funktional / 40% funktional** sein.

---

## Kritische Befunde — sofort zu klären (vor / während Iter 18)

Drei Funde aus dem Security-Roadmap sind heute schon Risiken und sollten **vor** Plattform-Reife-Iterationen mindestens dokumentarisch eingeordnet werden:

| Befund | Wo | Empfehlung |
|--------|-----|-----------|
| **CSRF in allen 4 SecurityConfigs disabled** trotz `security-rules.md` "do not disable" | `gateway/`, `iam/`, `trips/`, `expense/SecurityConfig.java` | **Doku-Code-Diskrepanz** — entweder Iter 21 implementieren ODER die security-rules.md ehrlich anpassen + ADR-0027 als Begründung. Nicht stillschweigend lassen. |
| **`AdminController.DELETE /admin/tenants/{id}`** ist nur `.authenticated()`, jeder eingeloggte Nutzer kann **jeden Tenant löschen** | `travelmate-iam/.../adapters/web/AdminController.java` | Heute "by design" für E2E-Cleanup (Memory). **Jetzt** mit `@Profile("!production")` oder Role-Guard `ROLE_ADMIN` schützen. SEC-09 — keine Iter-18-Story, sondern Quick-Fix. |
| **`application.yml`-Defaults** für `KEYCLOAK_CLIENT_SECRET`, `RABBITMQ_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD` | alle 4 Module | Iter 20 (Secrets-Story SEC-02), aber **vor produktivem Deploy** unbedingt. Heute ENV-Override → vergessenes Override = Default-Creds in Prod. |

ADR-0016 ↔ Code-Diskrepanzen (Security-Report Abschnitt 0): UrlSanitizer akzeptiert HTTP, swallowt UnknownHostException, Arc42 §8 referenziert nicht-existenten `TenantIdentificationFilter`. Bündeln in einem Doku-Korrektur-Commit zu Beginn von Iter 19.

---

## Iteration-Roadmap 18-25

| Iter | Funktionaler Fokus | Nicht-funktionaler Fokus | Neue ADRs | v1.0-Bezug |
|------|--------------------|--------------------------|-----------|------------|
| **18** | Multi-Organizer (ADR-0024 Trip-lokal), Einladungs-E-Mail | E2E-Stabilität (HTMX/Playwright-Sync), arc42 §08 Event-Tabelle aktualisieren | 0024 ✓; 0025/0026 als Drafts | Foundation |
| **19** | Recipe-URL-Import, Recipe Edit/Delete | **Observability-Stack** (Prometheus+Grafana+Loki+Tempo), **Transactional Outbox**, ADR-0026 Implementierung (`web-commons`-Modul) | 0027 (Observability), 0028 (Outbox); 0025/0026 ✓ | Sichtbarkeit + Datenintegrität |
| **20** | Externe Einladung (US-TRIPS-013), Password-Reset (US-IAM-060) | **Audit-Logging** (SEC-06), Doku-Korrekturen (CSRF, UrlSanitizer, TenantIdentificationFilter) | 0029 (Audit-Strategie) | Forensik-Basis |
| **21** | Receipt Edit/Delete | **Production-Hardening I**: CSRF re-enable, Security-Headers, Deployment-Topologie (Compose+Traefik), CI/CD-Multi-Stage, Backup/Restore-Drill | 0030 (Deployment), 0031 (Secrets via SOPS+Age), 0032 (TLS) | Operative Reife I |
| **22** | (Bug-Fixes / kleine Stories nach Bedarf) | **Production-Hardening II**: Rate-Limit, Keycloak-Hardening, Auto-Update via Renovate/Dependabot, OWASP-Dep-Scan | 0033 (CI/CD-Stages), 0034 (Rate-Limit) | Operative Reife II |
| **23** | (GDPR-Stories: Right-to-Erasure für Account, Right-to-Access, Privacy Notice) | **GDPR-Compliance**: Account-Lifecycle-Cascade analog zu ADR-0023, Lighthouse-CI, a11y axe-core | 0035 (Account-RTE-Cascade), 0036 (Data-Retention) | Compliance |
| **24** | (Verbleibende Backlog-Reste) | **Pentest** + Tenant-Isolation-Tests (US-INFRA-030, SEC-11), JWT-`sub`-Mapping (Email mutable), Pact-Contract-Tests für 19 Events | 0037 (Tenant-Isolation-Test-Slice), 0038 (Spring Session+Redis falls >1 Replica) | Pre-Release-Verification |
| **25** | **v1.0.0 Release** | Lasttest-Baseline (Gatling/k6), JVM-Tuning, Final Regression, security.txt, Doku-Review | 0039 (Lasttest-Baseline) | Release |

**Kritischer Pfad**: 18 → 19 (Observability) → 20 (Audit) → 21 (Hardening I) → 24 (Tenant-Isolation) → 25.

**Parallelisierbare Threads** (zwischen Iterationen verteilbar): Externe Einladung, Recipe-CRUD, Receipt-Edit — alle ohne harte Dep-Beziehung untereinander.

---

## Iteration 18 — Detailplan (nächste Iteration)

**Theme**: E2E-Pipeline-Stabilität + Multi-Organizer + Einladungs-E-Mail. Foundation für alle nachfolgenden Iterationen.

### Stories

| Story | Priorität | Größe | Beschreibung |
|-------|-----------|-------|--------------|
| **S18-Q01** E2E-Pipeline-Stabilisierung | Must | M (Time-Box 2 Tage) | HTMX-Settle-Helper in `PlaywrightHooks`; `htmx:afterSettle`-Wait einbauen; Akzeptanz: 20× E2E grün ohne Retries |
| **S18-A01** Co-Organizer ernennen (per ADR-0024) | Should | M | `Trip.grantOrganizerRights` Authorization-Guard ergänzen (nur Organizer, nur Participant); `OrganizerRoleGranted`-Event |
| **S18-A02** Co-Organizer entziehen | Should | S | `Trip.revokeOrganizerRights` (neu) mit Last-Organizer-Schutz `organizerIds.size() ≥ 1`; `OrganizerRoleRevoked`-Event |
| **S18-B01** Einladungs-E-Mail (US-IAM-050) | **Must** | M | Direktversand in Trips via neuem `EmailNotificationPort`; `@TransactionalEventListener(AFTER_COMMIT)`; Mail-Fehler darf Tx nicht rollbacken |
| **S18-D01** ADR-Drafts | Should | XS | ADR-0024 akzeptieren; ADR-0025 (Event-Versionierung) + ADR-0026 (ExceptionHandler-Dedup) als `Vorgeschlagen` einchecken |
| **S18-Z01** Doku-Befund-Triage | Should | XS | SEC-09 Quick-Fix (`AdminController` Profile-Guard); offene Diskrepanzen ADR-0016 ↔ Code im Issue-Tracker dokumentieren (nicht beheben — Iter 19/20) |

### Reihenfolge

1. S18-Z01 (Quick-Fix; reduziert Risiko sofort)
2. S18-Q01 (vor allen anderen — schafft vertrauenswürdige Pipeline für TDD)
3. S18-D01 (ADR-0024 vor S18-A)
4. S18-A01 → S18-A02 (gemeinsame UI/Tests)
5. S18-B01 (parallel zu A02 möglich)

### Kritische Dateien

- `travelmate-trips/.../domain/trip/Trip.java` — `grantOrganizerRights`/`revokeOrganizerRights` mit Authorization
- `travelmate-trips/.../application/TripService.java` — Service-Methoden, Event-Publishing
- `travelmate-common/.../events/trips/OrganizerRoleGranted.java` + `OrganizerRoleRevoked.java` (neu)
- `travelmate-common/.../messaging/RoutingKeys.java`
- `travelmate-trips/.../domain/invitation/EmailNotificationPort.java` (neu)
- `travelmate-trips/.../adapters/messaging/SmtpInvitationEmailAdapter.java` (neu)
- `travelmate-trips/src/main/resources/templates/mail/invitation.html` (neu)
- `travelmate-e2e/.../bdd/PlaywrightHooks.java` — `waitForHtmxSettle()` Helper
- `travelmate-iam/.../adapters/web/AdminController.java` — `@Profile("!production")` Guard
- `docs/adr/0024-organizer-rolle-trip-lokal.md` (akzeptiert), `0025-event-versionierung-und-naming-konvention.md` (vorgeschlagen), `0026-zentralisierter-globalexceptionhandler.md` (vorgeschlagen)

### Reuse-Punkte

- `Trip.grantOrganizerRights(UUID)` (`travelmate-trips/.../domain/trip/Trip.java`) — Methode existiert, nur Guards ergänzen
- `GrantTripOrganizerCommand` und `POST /{tripId}/organizers/{accountId}` Endpoint existieren — Aggregat-Tests + Authorization-Guards verifizieren
- `DomainEventPublisher` Pattern aus `TripDeleted` für `OrganizerRoleGranted/Revoked`
- `PlaywrightHooks.page.onDialog()` (Iter 17 zentralisiert) als Vorbild für zentrale Helper
- Mailpit-Container im `docker-compose.yml` (5025, 8025) bereits laufend
- Spring Mail Starter — Dependency hinzufügen

---

## v1.0.0 Acceptance Criteria

Synthese aus allen 4 Roadmap-Reports — alle kumulativ erforderlich:

### Funktional
- [ ] 100% der "Required for v1.0"-Stories ✅ Done (~15 noch offen per Requirements-Report)
- [ ] User-Journey vollständig: Sign-up → Trip-Planung (DatePoll/AccommodationPoll) → Mahlzeit/Einkauf → Abrechnung → Abschluss
- [ ] Externe Einladung funktional (US-TRIPS-013)
- [ ] Password-Reset (US-IAM-060)
- [ ] Multi-Organizer (US-IAM-040/041 via ADR-0024)
- [ ] Einladungs-E-Mail (US-IAM-050)

### Tests & Qualität (QS-Report)
- [ ] **Coverage** ≥ 75% Line / ≥ 70% Branch (JaCoCo)
- [ ] **0 transiente E2E-Fehler** in 20 aufeinanderfolgenden `./mvnw -Pe2e verify`-Läufen
- [ ] **0 SAST Critical/High** (Spotbugs/SonarQube)
- [ ] **0 CVSS ≥ 7.0 CVEs** (OWASP Dependency-Check)
- [ ] **Pact Contract-Tests** für alle 19 Events (Producer + Consumer)
- [ ] **Lighthouse Score ≥ 80** Performance + a11y (mobile)
- [ ] **Lasttest-Baseline**: 50 concurrent users, 5 min sustained, p95 dashboard < 1s

### Security (Security-Report — alle P0 abgeschlossen)
- [ ] CSRF re-enabled (SEC-01)
- [ ] Production-Secrets via Vault/SOPS, keine Defaults (SEC-02)
- [ ] Security-Headers (HSTS, CSP, X-Frame, X-Content-Type, Referrer-Policy) — SEC-03
- [ ] TLS-Termination dokumentiert (Traefik/Let's Encrypt) — SEC-04
- [ ] OWASP-Dep-Scan + Trivy + Dependabot in CI — SEC-05
- [ ] Audit-Log für Login/Logout, `grantOrganizerRole`, `deleteTrip`, `removeMember` — SEC-06
- [ ] Rate-Limiting am Gateway — SEC-07
- [ ] Keycloak-Hardening (Brute-Force, Password-Policy, Sessions) — SEC-08
- [ ] AdminController Role-restricted — SEC-09
- [ ] Externer Pentest durchgeführt + kritische Findings gefixt — SEC-14
- [ ] GDPR: Right-to-Erasure für Account, Right-to-Access, Privacy Notice — SEC-GDPR-01..05

### Plattform & Betrieb (Architect-Report)
- [ ] Observability-Stack (Prometheus + Grafana + Loki + Tempo) deployed
- [ ] Transactional Outbox in allen 3 SCS — verhindert Event-Verlust
- [ ] Backup/Restore-Drill <30min dokumentiert + getestet
- [ ] CI/CD: 3 Umgebungen (PR-Preview, Staging, Prod) mit Approval-Gate
- [ ] Rollback per Image-Tag <5min dokumentiert + getestet
- [ ] Runbooks für db-down, rabbit-down, keycloak-down, dlq-full, deployment-failure

### ADRs
- [ ] ADR-0024 bis ADR-0039 alle akzeptiert (siehe Iteration-Tabelle)

---

## Verifikation pro Iteration

Pro Iteration unverhandelbar (per Memory `feedback_e2e_bdd_per_story`):
1. `./mvnw clean verify` grün vor Story-Abschluss
2. BDD-Feature pro Story (ggf. `@manuell` für UI-fragile Szenarien)
3. **Voll-E2E-Lauf** `./mvnw -Pe2e verify` nach jeder Story (per Memory `feedback_always_run_e2e`)
4. Release-Dance am Iterationsende (revision bump → release commit → tag → snapshot bump)

---

## Risiken & Sequenzierung

| ID | Risiko | Mitigation | Iter |
|----|--------|-----------|------|
| AR-07 | Secret-Leck via Repo-Defaults | ADR-0031, P0-SEC-02 | 21 |
| AR-02 | Cross-Tenant-Datenleck | ADR-0037 + Tenant-Test-Slice | 24 |
| AR-05 | Datenverlust ohne Backup | ADR-0032 + Backup-Drill | 21 |
| AR-01 | Event-Verlust bei RabbitMQ-Down | ADR-0028 Outbox | 19 |
| AR-03 | Keine Diagnose bei Prod-Incident | ADR-0027 Observability | 19 |
| Roadmap-Slip | Plattform-Themen werden für Features deferred → "feature-rich, infrastructure-thin" | 60/40-Mix nicht-funktional/funktional einhalten; Iter 21-22 ist plattformgetrieben | laufend |

**Architect-Empfehlung**: ohne ADR-0030 (Secrets), ADR-0032 (Backup), ADR-0037 (Tenant-Test) **darf v1.0 nicht freigegeben werden**.

---

## Out-of-Scope für v1.0

- WAF / DDoS L4-L7 (Provider-Level)
- Bug-Bounty-Programm (post-v1.0)
- SOC2 / ISO 27001 (separate Compliance-Roadmap)
- HSM (nur relevant für self-hosted Keycloak)
- Mobile-Native-App (PWA-only)
- Schema-Registry (ADR-0025 behandelt Versionierung leichtgewichtig)
- Postgres Row-Level-Security (Architect-Empfehlung: nicht für v1.0; App-Layer + ArchUnit + Tests reichen)
- K3s / Kubernetes (v1.5+ Migrationspfad in ADR-0030 dokumentiert; v1.0 = Single-VM-Compose)

---

## Offene Fragen / Hot-Spots

Per Architect-Report — vor Iter 19/20 zu klären:
1. **`travelmate-common` splitten** in plain-JAR + `travelmate-web-commons` (Spring-Web-Abhängigkeit für ADR-0026)?
2. **SMTP-Provider** für Production (nicht Mailpit) — Sendgrid/Postmark/AWS SES?
3. **On-Call-Modell** — wer reagiert auf Alerts, in welcher SLA-Zeit?
4. **Compose vs. K3s für v1.0** — Architect empfiehlt Compose+Traefik mit dokumentiertem K3s-Migrationspfad
5. **Email mutable** in Keycloak — wechseln auf JWT-`sub`-Claim als Tenant-Resolver (SEC-15)?
6. **arc42 §08 Doku-Drift** — `TenantIdentificationFilter` referenziert, existiert nicht

---

## Nächste Aktion

**Mit Iteration 18 starten** (oben detailliert):
1. S18-Z01 Quick-Fix (`AdminController` Profile-Guard, ~15 min)
2. S18-Q01 E2E-Stabilisierung (~2 Tage Time-Box)
3. S18-D01 ADR-Drafts (~2 h)
4. S18-A01/A02 Multi-Organizer (~1 Tag)
5. S18-B01 Einladungs-E-Mail (~1 Tag)
6. Voll-E2E + Release v0.18.0
