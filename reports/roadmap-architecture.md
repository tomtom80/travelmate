# Travelmate — Architektur-Roadmap zu v1.0.0

**Datum**: 2026-04-26
**Ausgangsversion**: v0.17.0 (post-iter-17), Working Branch v0.17.0-SNAPSHOT
**Zielversion**: v1.0.0 (produktionsreif)
**Scope**: Nicht-funktionale Architektur-Roadmap. Fachliche Roadmap ist separat.
**Methodik**: ISO-25010 Quality Storming + ATAM-light + Risk Matrix

---

## 1. Executive Summary

Travelmate ist fachlich nahe an einer ersten produktiven Version. Die fachliche Tiefe (23 ADRs, 9 Trip-Aggregate, vollstaendiger Trip-/Expense-/Recipe-Lifecycle, PWA-Manifest, Cucumber-BDD + 992 Tests) uebertrifft den Reifegrad der **Plattform-, Betriebs- und Observability-Schicht** deutlich.

**Kernthese**: Der Weg zu v1.0.0 ist *kein Feature-Pfad*, sondern ein **Plattform-Reife-Pfad**. Die naechsten 6-8 Iterationen (Iter 18-25) sollten zu mindestens 60% nicht-funktionale Themen und zu maximal 40% Feature-Themen fuehren, sonst steht ein "feature-complete v0.20" einer **nicht-betreibbaren** Plattform gegenueber.

Heutige Lage in einem Satz pro ISO-Achse:

- **Security**: Keycloak-Realm steht, JWT-Validation steht, **Tenant-Isolation jenseits der ThreadLocal-/JWT-Klammer ist nicht testabgesichert**, keine Secrets-Verwaltung, kein TLS-Konzept fuer Produktion, keine Rate-Limit-Strategie.
- **Reliability**: Spring Actuator ungenutzt, kein Monitoring, kein Alerting, **at-most-once Event-Publishing nach AFTER_COMMIT**, kein Backup-/Restore-Konzept fuer 4 PostgreSQL-Instanzen.
- **Performance**: Keine Lasttest-Baseline, keine JVM-Tuning-Vorgaben, **HikariCP-Defaults**, kein Caching, keine Query-Performance-Messung.
- **Maintainability**: ArchUnit + JaCoCo + 992 Tests stark, **Observability-Stack fehlt komplett**, **CI deployt nicht**, demo-deploy.yml ist Single-VM-Compose.
- **Scalability**: Servlet-SCS sind grundsaetzlich stateless (positiv), **Statelessness nicht verifiziert** (ThreadLocal `TenantContext`!), kein horizontales Scaling-Story-Test.
- **Compatibility**: 19 Events ohne Versionierung (ADR-0025 in Vorbereitung), kein API-Vertragstest.

---

## 2. Production-Readiness Gap Analysis (ISO-25010)

### 2.1 Security

| Aspekt | Heute | Gap | v1.0-Acceptance-Bar |
|--------|-------|-----|---------------------|
| **Secrets-Management** | Hartkodierte Defaults in `application.yml`, ENV-Variablen ohne Vault | Kein Secrets-Lifecycle, keine Rotation, kein Audit | Externer Secrets-Provider (Vault/sealed-secrets/SOPS). Alle DB- und Keycloak-Client-Secrets aus Vault, keine `application.yml`-Defaults in Prod. Rotation fuer DB-Passwoerter dokumentiert. |
| **TLS-Terminierung** | Plain HTTP zwischen Compose-Services, kein TLS in Repo | Production hat *keine* TLS-Strategie | TLS-Termination am Ingress (Traefik/Nginx). Let's Encrypt-Automatik. mTLS *innerhalb* Cluster optional, aber mindestens TLS zwischen Gateway und SCS dokumentiert. HSTS-Header gesetzt. |
| **Keycloak-Hardening** | Realm + Client `travelmate-gateway`, Theme aktiv. Admin/admin Default. | Default-Admin-Account, Brute-Force-Detection nicht konfiguriert, Session-Timeouts nicht gesetzt, Password-Policy unklar | Admin-Account rotiert. Brute-Force-Lockout aktiv. Session/Token-TTL dokumentiert (Access 15min, Refresh 8h). Password-Policy: Mindestens 12 Zeichen, kein Wiederverwenden. SMTP-Konfig mit echten Credentials. |
| **Rate-Limiting** | Keine | Kein Schutz gegen Login-Flooding, Sign-up-Spam, Crawler | Gateway-Filter fuer Anonyme: 10 req/s Sign-up, 5 req/s Login. Authentifiziert: 50 req/s pro JWT-`sub`. Werkzeug: Spring Cloud Gateway `RequestRateLimiter` + Redis. |
| **Tenant-Isolation-Test** | Aggregate haben TenantId, ArchUnit prueft Domain-Reinheit | Keine *systematischen* Cross-Tenant-Probetests. QS-S01 ist Should, kein Test existiert. | Eigener Test-Slice `tenant-isolation/` pro SCS: Fuer jeden Controller-Endpunkt ein Negativtest, der mit JWT von Tenant B auf Resource von Tenant A zugreift -> erwartet 404. |
| **OWASP-Dep-Scan** | Nicht in CI | `dependency-check-maven` nicht eingebunden | `org.owasp:dependency-check-maven` als Build-Stage. Critical/High failt PR. SBOM-Export (CycloneDX) als Build-Artefakt. |
| **CSP / Security-Headers** | Spring Security Defaults | Keine Content-Security-Policy fuer HTMX-CDN, kein Referrer-Policy | CSP whitelistet HTMX-CDN-Hash. `Referrer-Policy: strict-origin-when-cross-origin`, `X-Frame-Options: DENY`, `Permissions-Policy` minimal. |
| **Audit-Log** | Nur applikatives Logback | Keine separate Sicherheits-Audit-Spur | `SecurityAuditLogger` (eigener Logger-Name) fuer Authn-Failure, Cross-Tenant-Versuche, Role-Grant-Events. Ueber Loki/CloudWatch separat indexierbar. |

### 2.2 Reliability

| Aspekt | Heute | Gap | v1.0-Acceptance-Bar |
|--------|-------|-----|---------------------|
| **Health Checks** | Spring Actuator vorhanden, aber `/actuator/health` nicht systematisch konfiguriert | Liveness vs. Readiness nicht getrennt, externe Probes fehlen | Pro SCS: `/actuator/health/liveness` + `/actuator/health/readiness`. Readiness prueft DB + RabbitMQ. Compose/Kubernetes-Probes konfiguriert. |
| **Monitoring** | Keine | Kein Prometheus-Scrape-Endpoint, keine Dashboards | Micrometer + `prometheus`-Endpoint pro SCS. Prometheus-Scrape laeuft. Grafana-Dashboards: Per-SCS-Latenz, RabbitMQ-Queue-Tiefe, JVM-Heap, HikariCP. |
| **Alerting** | Keine | Kein Alarmweg | Grafana-Alerts auf: DLQ > 0, p95 > 1s, SCS down > 30s, HikariCP > 80%. Webhook in Mailpit/Slack-Channel. |
| **Logging-Aggregation** | Stdout per Container, keine Sammlung | Cross-SCS-Korrelation unmoeglich | Loki/CloudWatch-Equivalent. Correlation-ID per Request (`X-Correlation-Id`) in MDC. JSON-Logs in Prod. |
| **Tracing** | Keine | Cross-SCS-Event-Flow nicht nachvollziehbar | OpenTelemetry, exportiert zu Tempo/Jaeger. Tracecontext im RabbitMQ-Header propagiert. |
| **Graceful Shutdown** | Spring Boot Default (timeout 30s), aber `lifecycle.timeout-per-shutdown-phase` nicht konfiguriert | Inflight-Requests koennten 502en | `server.shutdown=graceful` und `spring.lifecycle.timeout-per-shutdown-phase=30s` explizit. Kubernetes preStop + terminationGracePeriodSeconds. |
| **Event-Loss-Resilienz** | `@TransactionalEventListener(AFTER_COMMIT)` -> potenzieller Verlust bei RabbitMQ-Ausfall | DB-Commit + RabbitMQ-Send sind nicht atomar | Transactional Outbox Pattern (siehe ADR-Stub unten). DB-Tabelle `domain_events_outbox`, Poller publiziert + markiert. Toleriert RabbitMQ-Ausfall bis Plattenplatz. |
| **Backup/Restore** | Keine | 4 PostgreSQL-Instanzen ohne Backup-Plan | Tabelle `pg_dump` per Cron ueber alle 4 DBs, S3-kompatibles Object-Storage, 14-Tage-Retention. **Restore in <30min dokumentiert + getestet** (nicht nur dokumentiert!). RabbitMQ-Definitions-Backup. |
| **DLQ-Handling** | DLQ konfiguriert (Iter 5) | Keine UI/Tooling fuer Replay | `/admin/dlq/replay/{queue}/{messageId}` Admin-Endpoint im Gateway, role-gated. Manuelles Replay dokumentiert. |
| **Idempotenz-Verifikation** | Implementiert, kein systematischer Test | Re-Delivery-Tests fehlen pro Consumer | Pro Consumer: BDD-Szenario "Event wird zweimal zugestellt -> genau ein Resultat". |

### 2.3 Performance

| Aspekt | Heute | Gap | v1.0-Acceptance-Bar |
|--------|-------|-----|---------------------|
| **Lasttest-Baseline** | Keine | Keine Aussage zu Belastbarkeit | k6/Gatling-Skript fuer Top-10-Flows. Baseline auf Referenzhardware: 50 concurrent users, 5 min sustained. p95-Werte dokumentiert. |
| **JVM-Tuning** | Default | Heap-Groessen nicht profilbasiert | Pro SCS: `-Xmx`/`-Xms` aufeinander gesetzt, GC-Wahl dokumentiert (G1 default), JFR-Sampling im Monitoring-Profil. |
| **DB-Connection-Pool** | HikariCP-Defaults (10 max) | Nicht profilbasiert auf Containerquota | Pool-Size = `2 * vCPUs + 1` als Startwert (Hikari-Empfehlung), Lasttest validiert, Metric `hikaricp.connections.pending` ueberwacht. |
| **Caching-Strategie** | Keine | Wiederholte Reads (TravelParty, TripProjection) treffen DB | Caffeine als L1-Cache fuer gelesene Read-Models (z.B. TravelParty-Lookup per JWT-Email). Invalidation auf Event-Empfang. Caching *nicht* in Aggregaten. |
| **Query-Performance** | Keine N+1-Detection | Hibernate-Statistik aus | `hibernate-statistics`-Endpoint im `monitoring`-Profil. SQL-Slowlog ab 100ms. ArchUnit + Test: kein `@OneToMany(fetch = EAGER)`. |
| **Static-Asset-Caching** | Templates nicht gecached, Frontend-Assets noch ungeklaert | Browser-Caching nicht konfiguriert | `Cache-Control: public, max-age=86400` fuer Static, `immutable` mit Hash-Filename fuer JS/CSS. Thymeleaf `cache=true` in Prod. |

### 2.4 Maintainability

| Aspekt | Heute | Gap | v1.0-Acceptance-Bar |
|--------|-------|-----|---------------------|
| **CI/CD-Pipeline** | `ci.yml` (verify), `demo-deploy.yml` (Compose-Push zu Hetzner) | **Kein Multi-Environment-Promote** (dev->staging->prod), kein Approval-Gate, kein Rollback | Drei Umgebungen: ephemeral PR-Preview, persistente Staging, Prod. Promote-Pipeline mit manuellem Approval auf Prod. Rollback per Image-Tag (re-deploy Vorgaengerversion <5min). |
| **Container-Hardening** | Eigene Dockerfile, JRE 21 | Distroless? Non-root? Health-Check? | Distroless Java Base. Non-root user. `HEALTHCHECK` per Image. CVE-Scan pro Image (Trivy in Pipeline). |
| **Observability-Stack** | Keine | Logs, Metriken, Traces fehlen | Stack-Entscheidung in ADR (Vorschlag: Prometheus+Grafana+Loki+Tempo, alle Open Source, alle in eigenem Compose-Bundle "ops"). Ein dediziertes ADR. |
| **Doku-Pflege** | arc42 + 23 ADRs gepflegt | Runbooks fehlen ("Was tun bei DLQ?", "Wie rollt man back?") | `docs/runbooks/` mit Troubleshooting-Playbooks: incident-{topic}.md je Failure-Pattern. Mindestens: db-down, rabbit-down, keycloak-down, dlq-full, deployment-failure. |
| **Dependency-Updates** | Manuell | Stuendlich anfallender Tech-Debt | Renovate/Dependabot mit Auto-Merge fuer Patch-Versionen, PR fuer Minor/Major. CVE-Patches im SLA "binnen 72h gemerged". |

### 2.5 Scalability

| Aspekt | Heute | Gap | v1.0-Acceptance-Bar |
|--------|-------|-----|---------------------|
| **Statelessness** | SCS sind Spring-Boot-Servlets, KEINE Session am Server | **TenantContext nutzt ThreadLocal** -> per Request korrekt, aber **"Stateless" in Reviews verifiziert?** | ArchUnit + Review-Checklist: Keine `static`-Mutable, keine `@SessionScope`. Stateless-Test: zwei Replicas hinter Round-Robin, gleiche Requests, keine Affinity-Erfordernis. |
| **Session-Affinity** | Gateway nutzt Spring Authorization Server-Session (oauth2Login) | **Reactive Gateway-Session in Memory** -> Multi-Instance Gateway nicht moeglich ohne Session-Replikation | Spring Session + Redis, oder JWT-only-Mode am Gateway dokumentiert. Bevor zweite Gateway-Replica laeuft, muss Sessionspeicher extern sein. |
| **Horizontale Skalierung** | Nicht getestet | 1 Replica je SCS angenommen | Lasttest mit 2 Replicas pro SCS hinter L7-LB. Verifikation: Event-Konsum gleichmaessig, keine doppelten Verarbeitungen, RabbitMQ Queue prefetch konfiguriert. |
| **DB-Scaling** | Single-Instance Postgres je SCS | Kein Read-Replica-Konzept | Fuer v1.0 OK. Dokumentiertes Upgrade-Path-Statement: ab welchem Volumen Read-Replica + Connection-Routing eingefuehrt wird. |

### 2.6 Compatibility

| Aspekt | Heute | Gap | v1.0-Acceptance-Bar |
|--------|-------|-----|---------------------|
| **Event-Versionierung** | Keine, ADR-0025 in Vorbereitung | Brechende Schema-Aenderungen waeren riskant | ADR-0025 akzeptiert + ArchUnit-Test fuer Naming + `tenantId+occurredOn` Pflicht. Erste Migration `AccommodationPriceSet -> AccommodationPriceUpdated` in Iter 19. |
| **API-Versionierung** | UI-Routen ohne `/v1/`-Praefix | Aktuell nur Server-Rendering, keine externe API | Keine REST-API extern, daher keine v1.0-Aufgabe. **Aber**: `/admin/`-Routen (Gateway) und `/iam/admin/`-Routen klassifizieren als "Internal API, no compat guarantee". |
| **Browser-Kompatibilitaet** | HTMX 2.x, PicoCSS 2 | Keine Browser-Matrix dokumentiert | Browserlist-Datei: "last 2 versions, not IE 11". Lighthouse-CI gegen diese Matrix. Polyfill-Strategie dokumentiert (heute: keine). |
| **PWA-Update-Strategie** | Manifest existiert, kein SW-Update-Konzept | User koennte alte App-Version offline ausfuehren | Service-Worker mit Versionsnummer, `clients.claim()` + "Reload required"-Toast bei neuer Version. |

---

## 3. ADR-Roadmap Iteration 18-25

### Bereits geplant (aus Iter-18-Plan)

| ADR | Iter | Status | Titel |
|-----|------|--------|-------|
| ADR-0024 | 18 | Akzeptiert | Organizer-Rolle als Trip-lokale Aggregat-Eigenschaft |
| ADR-0025 | 18 | Vorgeschlagen | Event-Versionierung und Naming-Konvention |
| ADR-0026 | 19 | Vorgeschlagen | Zentralisierter GlobalExceptionHandler (Modul `travelmate-web-commons`) |

### Neue ADRs auf dem Weg zu v1.0

| ADR | Iter | Status-Ziel | Titel | Inhalt in einem Satz |
|-----|------|-------------|-------|----------------------|
| **ADR-0027** | 19 | Vorgeschlagen -> Akzeptiert | **Observability-Stack: Prometheus + Grafana + Loki + Tempo** | Open-Source-OTel-Stack, lokal als `compose.observability.yml` Bundle, in Prod als eigener Stack neben SCS. Gegen Datadog/NewRelic abgewogen (Vendor-Lock vs. Self-Host-Aufwand). |
| **ADR-0028** | 19 | Vorgeschlagen -> Akzeptiert | **Transactional Outbox Pattern** | DB-Tabelle `domain_events_outbox` pro SCS, Poller mit `@Scheduled(fixedDelay)` veroeffentlicht und markiert. Loest Event-Loss-Risiko aus arc42 §11. |
| **ADR-0029** | 20 | Vorgeschlagen -> Akzeptiert | **Production-Deployment-Topologie** | Hetzner CAX21 -> Hetzner Cloud K3s? oder Single-VM Compose mit `traefik`+`watchtower`? Entscheidung dokumentiert. Verkettet mit ADR-0030 + ADR-0031. |
| **ADR-0030** | 20 | Vorgeschlagen -> Akzeptiert | **Secrets-Management-Strategie** | SOPS+Age fuer Repo-Secrets vs. Bitwarden-CLI vs. HashiCorp Vault. Abhaengig von ADR-0029. Default-Vorschlag: SOPS+Age (kein zusaetzlicher Dienst, GitOps-kompatibel). |
| **ADR-0031** | 20 | Vorgeschlagen -> Akzeptiert | **TLS-Terminierung und Reverse-Proxy** | Traefik vor Gateway. Let's Encrypt automatisiert. HSTS, CSP, Security-Headers. mTLS Cluster-intern: out-of-scope v1.0. |
| **ADR-0032** | 21 | Vorgeschlagen -> Akzeptiert | **Backup- und Restore-Strategie** | `pg_dump` per Cron, 4 DBs, 14d Retention auf Hetzner Object-Storage. Restore-Drill quartalsweise. RabbitMQ-Definitions-Backup mit Build. |
| **ADR-0033** | 21 | Vorgeschlagen -> Akzeptiert | **CI/CD-Pipeline-Architektur** | GitHub Actions Multi-Stage: PR-Preview, Staging, Prod. Image-Tagging-Schema. Rollback-Mechanismus. Approval-Gate Prod. Trivy-Scan. |
| **ADR-0034** | 22 | Vorgeschlagen -> Akzeptiert | **Rate-Limiting im Gateway** | Spring Cloud Gateway `RequestRateLimiter` + Redis. Per-IP-Limit fuer anonyme Pfade, per-JWT-`sub` fuer authentifizierte. |
| **ADR-0035** | 22 | Vorgeschlagen -> Akzeptiert | **Caching-Strategie** | Caffeine als L1 fuer Read-Models. Invalidation auf Event-Konsum. Kein Aggregat-Caching. |
| **ADR-0036** | 23 | Vorgeschlagen -> Akzeptiert | **Logging- und Correlation-ID-Konzept** | JSON-Logs in Prod, `X-Correlation-Id`-Filter im Gateway, MDC-Propagierung in RabbitMQ-Header. SecurityAuditLogger separater Stream. |
| **ADR-0037** | 24 | Vorgeschlagen -> Akzeptiert | **Tenant-Isolation-Tests als Architektur-Fitness** | Pro Controller automatischer Cross-Tenant-Negativtest via Test-Slice. Aufgehoben in ArchUnit-PlantUML-Diagramm-Validation. |
| **ADR-0038** | 24 | Vorgeschlagen -> Akzeptiert | **Statelessness-Verifikation und Session-Strategie** | Spring Session + Redis fuer Gateway-OIDC-Session. Erlaubt 2+ Gateway-Replicas. SCS bleiben sessionsfrei. |
| **ADR-0039** | 25 | Vorgeschlagen | **Performance-Lasttest-Baseline** | k6-Skript-Korpus, Referenzhardware, p95-Ziele pro Endpoint. CI-Run als nightly. |

**Gesamt**: 13 neue ADRs (ADR-0027 bis ADR-0039) zwischen Iter 19 und Iter 25, plus die drei bereits in Iter 18 gestarteten. Mit ADR-0023 sind das 17 ADRs Plattform-/Reife-Themen vs. 23 fachlich-orientierte ADRs heute. Realistisch und proportional.

---

## 4. Tech-Debt-Inventar (zu tilgen vor v1.0)

### Code-Niveau

| ID | Debt | Iter | Aufwand | Begruendung |
|----|------|------|---------|-------------|
| TD-01 | **GlobalExceptionHandler dreifach kopiert** (IAM, Trips, Expense) | 19 | M | ADR-0026 + Modul `travelmate-web-commons`. Voraussetzung fuer einheitliches Error-Verhalten. |
| TD-02 | **Event-Naming-Inkonsistenz**: `AccommodationPriceSet` (vs. `*Updated`); `*JoinedTrip`/`*RemovedFromTrip` mixed style | 19 | S | Soft-Migration via ADR-0025. Alter Routing-Key bleibt parallel. |
| TD-03 | **Event-Pflichtfelder nicht erzwungen**: `tenantId` und `occurredOn` per ArchUnit-Regel | 19 | S | Im common-Modul, ArchUnit-Test ueber alle `events.**`-Records. |
| TD-04 | **TenantContext ist ThreadLocal** ohne ArchUnit-Schutz vor Leak (z.B. asynchrone Threads ohne Cleanup) | 20 | M | Test: TenantContext nach jedem Request gecleared (ServletFilter), pro `@Async`-Annotation Inheritance dokumentiert. |
| TD-05 | **Security in Test-Profil deaktiviert** | 22 | M | Mindestens dedizierte Security-Slice-Tests mit `@WithMockJwtAuth` fuer alle Controller. arc42 §11 fuehrt das schon als "Security Test Gap". |
| TD-06 | **Keine Cross-Tenant-Isolation-Tests** | 24 | M | ADR-0037. Pro Controller-Endpoint einen Negativtest mit fremdem JWT. |
| TD-07 | **In-Memory-Repositories in Tests** OK, aber **keine Property-based Tests** fuer Aggregat-Invarianten | optional | L | jqwik fuer Trip + Expense + Polls. Optional, nicht v1.0-Sperre. |

### Infrastruktur-Niveau

| ID | Debt | Iter | Aufwand | Begruendung |
|----|------|------|---------|-------------|
| TD-08 | **Keine Outbox** -> Event-Loss-Risiko | 19 | L | ADR-0028. Eine `domain_events_outbox`-Tabelle pro SCS, Flyway-Migration, Polling-Bean. |
| TD-09 | **Keine Observability** | 19 | XL | ADR-0027. Stack-Bring-up + Dashboards + erste Alerts. |
| TD-10 | **Keine Backups** | 21 | M | ADR-0032. `pg_dump` per Cron, dokumentierter Restore-Drill. |
| TD-11 | **Demo-Deploy = Prod-Deploy?** | 20-21 | L | Aktuell pusht `demo-deploy.yml` direkt nach `main`. Trennung dev/staging/prod ueber Tag-basierte Pipeline. |
| TD-12 | **Hartkodierte Secrets in `application.yml`** als Defaults | 20 | M | ADR-0030. Vault/SOPS-Strategie, alle Defaults aus Prod-Profilen entfernen. |
| TD-13 | **Keine TLS in Repo** | 20 | M | ADR-0031. Traefik + Let's Encrypt Setup. |
| TD-14 | **Keine OWASP-Dep-Scan in CI** | 22 | S | `org.owasp:dependency-check-maven` als CI-Job. |
| TD-15 | **Keine Lighthouse-CI** | 23 | M | Lighthouse-CI gegen Demo-Umgebung, Mobile + Accessibility-Score-Gates. |
| TD-16 | **Keine Lasttest-Baseline** | 25 | M | k6 / Gatling. Nightly. |

### Doku-Niveau

| ID | Debt | Iter | Aufwand |
|----|------|------|---------|
| TD-17 | **Runbooks fehlen** | laufend ab 19 | S je Runbook |
| TD-18 | **arc42 §07 Deployment-View** beschreibt Zielbild, nicht Ist-Zustand | 20-21 | S |
| TD-19 | **arc42 §08 Cross-Cutting** kennt `RoleAssigned`/`RoleUnassigned` nicht | 18 | S |
| TD-20 | **Glossar §12** noch ohne `Outbox`, `Correlation-Id`, `Replica` | laufend | S |

---

## 5. Architektur-Risiko-Matrix v1.0

Skala: Wahrscheinlichkeit × Impact, jeweils Niedrig/Mittel/Hoch/Kritisch.

| ID | Risiko | Wahrscheinlichkeit | Impact | Mitigation | Owner-Iter |
|----|--------|--------------------|--------|------------|------------|
| AR-01 | **Event-Verlust unter Last** (RabbitMQ-Down nach DB-Commit) | Mittel | Hoch | ADR-0028 Outbox | 19 |
| AR-02 | **Cross-Tenant-Datenleck** durch ungeprueften Endpoint | Niedrig | Kritisch | ADR-0037 Tenant-Test-Slice + Audit-Logger | 24 |
| AR-03 | **Keine Diagnose bei Prod-Incident** | Hoch | Hoch | ADR-0027 Observability-Stack | 19 |
| AR-04 | **Kein Rollback-Mechanismus** | Hoch | Hoch | ADR-0033 CI/CD + Image-Tag-Rollback | 21 |
| AR-05 | **Datenverlust ohne Backup** | Mittel | Kritisch | ADR-0032 Backup + Restore-Drill | 21 |
| AR-06 | **Brechende Event-Aenderung in Prod = stille DLQ-Ueberfuelung** | Mittel | Hoch | ADR-0025 + ArchUnit-Naming-Test | 18-19 |
| AR-07 | **Secret-Leck via Repo** (z.B. `application.yml`-Defaults) | Hoch | Kritisch | ADR-0030 Secrets-Management | 20 |
| AR-08 | **Brute-Force-/Spam-Sign-up** | Hoch | Mittel | ADR-0034 Rate-Limit | 22 |
| AR-09 | **Out-of-Memory unter Last** wegen Default-JVM | Mittel | Hoch | Lasttest + JVM-Tuning | 25 |
| AR-10 | **Gateway-Session-Daten verloren bei Restart** -> User muss neu einloggen | Hoch | Niedrig | ADR-0038 Spring Session + Redis (sobald >1 Replica) | 24 |
| AR-11 | **CVE in Transitive-Dependency uneerkannt** | Hoch | Mittel | TD-14 OWASP Dep-Scan | 22 |
| AR-12 | **HTMX-CDN-Ausfall** macht App unbrauchbar | Niedrig | Hoch | HTMX self-hosting via Spring-Static-Resource. Iter 22-23. | 23 |
| AR-13 | **PWA-User auf altem SW** sieht falsche UI nach Update | Mittel | Mittel | SW-Versionsschema + Reload-Toast | 23 |
| AR-14 | **Multi-Organizer-Promotion-Bug** (z.B. Self-Demotion last organizer) | Mittel | Hoch | Aggregat-Invariante `organizerIds.size() >= 1` (siehe Iter-18-Plan R-018-05) | 18 |
| AR-15 | **`travelmate-common` Spring-Web-Verseuchung** durch ADR-0026 | Mittel | Mittel | Eigenes Modul `travelmate-web-commons` | 19 |

### Risiko-Matrix-Heatmap

```
              Niedrig-W      Mittel-W       Hoch-W
            ┌─────────────┬─────────────┬─────────────┐
  Kritisch  │ AR-02       │ AR-05       │ AR-07       │
            ├─────────────┼─────────────┼─────────────┤
  Hoch      │ AR-12       │ AR-01 AR-06 │ AR-03 AR-04 │
            │             │ AR-09 AR-14 │             │
            ├─────────────┼─────────────┼─────────────┤
  Mittel    │             │ AR-13 AR-15 │ AR-08 AR-11 │
            ├─────────────┼─────────────┼─────────────┤
  Niedrig   │             │             │ AR-10       │
            └─────────────┴─────────────┴─────────────┘
```

Zwei kritische Hoch-Wahrscheinlichkeit-Risiken (AR-07 Secret-Leck) und drei kritische Mittel-Wahrscheinlichkeit-Risiken (AR-02, AR-05) dominieren die v1.0-Bar. **Ohne ADR-0030 (Secrets), ADR-0032 (Backup), ADR-0037 (Tenant-Test) sollte v1.0 nicht freigegeben werden.**

---

## 6. Empfehlung Iteration-Sequencing 18-25

### Logik der Reihenfolge

1. **Erst Sichtbarkeit, dann alles andere**. Ohne Observability-Stack ist jede Optimierung blind. -> Iter 19.
2. **Dann Datenintegritaet**. Outbox + Event-Versionierung verhindern stille Datenverluste, bevor wir Last drauf werfen. -> Iter 19-20.
3. **Dann Plattform-Topology**. Deployment, TLS, Secrets in einem zusammenhaengenden Block. -> Iter 20-21.
4. **Dann Schutz-Layer**. Rate-Limit, Caching, Tenant-Isolation. -> Iter 22-24.
5. **Erst zum Schluss Performance/Polishing**. Lasttest-Baseline, JVM-Tuning, Browser-Polishing. -> Iter 25.

### Empfohlene Sequenz

```
Iter 18 (laufend):  E2E-Stabilisierung + Multi-Organizer (ADR-0024)
                    + ADR-0025/0026 als Drafts
                    + arc42 §08 Event-Tabelle aktualisieren (TD-19)

Iter 19:  OBSERVABILITY + EVENT-INTEGRITAET
          - ADR-0026 Implementierung (web-commons)
          - ADR-0027 Observability-Stack (Prometheus+Grafana+Loki+Tempo)
            * Micrometer-Wiring pro SCS
            * Erste Dashboards: Per-SCS-Latenz, RabbitMQ, JVM
          - ADR-0028 Transactional Outbox
            * Flyway-Migration `domain_events_outbox` pro SCS
            * Outbox-Poller-Bean
          - ADR-0025 Implementation: ArchUnit-Naming-Test, Pflichtfeld-Test
          - TD-02 AccommodationPriceSet -> AccommodationPriceUpdated (V2 parallel)
          - Erste 2 Runbooks: rabbit-down, dlq-replay

Iter 20:  DEPLOYMENT-TOPOLOGIE + SECRETS + TLS
          - ADR-0029 Production-Deployment (Compose vs. K3s entscheiden)
          - ADR-0030 Secrets (SOPS+Age)
          - ADR-0031 TLS (Traefik + Let's Encrypt)
          - Defaults aus application.yml in Prod-Profilen entfernen (TD-12)
          - Container-Hardening: Distroless, non-root, HEALTHCHECK
          - Trivy in CI

Iter 21:  CI/CD + BACKUPS
          - ADR-0033 Multi-Stage CI/CD (PR-Preview / Staging / Prod)
            * Approval-Gate Prod
            * Image-Tag-basierter Rollback dokumentiert + getestet
          - ADR-0032 Backup-/Restore-Drill
            * pg_dump per Cron, alle 4 DBs
            * Quartalsweiser Restore-Drill in Staging
          - arc42 §07 Deployment-View an Ist-Zustand anpassen (TD-18)

Iter 22:  SCHUTZ-LAYER
          - ADR-0034 Rate-Limiting (Redis + Spring Cloud Gateway)
          - ADR-0035 Caching-Strategie (Caffeine)
          - TD-05 Security-Slice-Tests mit @WithMockJwtAuth (alle Controller)
          - TD-14 OWASP Dependency-Check in CI

Iter 23:  LOGGING + MOBILE-READINESS
          - ADR-0036 Logging + Correlation-ID
            * JSON-Logs in Prod
            * SecurityAuditLogger separater Stream
          - HTMX self-hosting (Risiko AR-12)
          - PWA-Update-Strategie (AR-13): SW-Version + Reload-Toast
          - TD-15 Lighthouse-CI gegen Staging

Iter 24:  TENANT-ISOLATION + STATELESSNESS
          - ADR-0037 Tenant-Isolation-Test-Slice
          - ADR-0038 Spring Session + Redis fuer Gateway
          - 2-Replica-Test Gateway + je SCS in Staging
          - TD-04 TenantContext-ThreadLocal-Cleanup-Verifikation

Iter 25:  PERFORMANCE-BASELINE + V1.0-FREEZE
          - ADR-0039 Performance-Baseline (k6-Skripte, p95-Ziele)
          - Nightly-Lasttest in Staging
          - JVM-Tuning auf Basis der Lasttest-Ergebnisse
          - TD-16 Lasttest-Baseline-Doku in arc42 §10
          - **v1.0.0-RC1 Cut**
```

### Iter 18 ist explizit *kein* nicht-funktionales Powerhouse

E2E-Stabilisierung und Multi-Organizer sind *Voraussetzung* fuer alles weitere. Ohne stabile Pipeline sind ADR-0027/0028-Refactorings (Outbox, Observability) gefaehrlich, weil sie sich quer durch alle drei SCS ziehen. Daher: **Iter 18 ist eine Investition in Iter 19**.

### Verzweigung "Compose vs. K3s" (Iter 20)

Aus `2026-03-26-kubernetes-hosting-marktrecherche.md` und `2026-03-26-demo-betriebskonzept.md` zeichnet sich heute eine **Single-VM-Compose-Empfehlung** fuer die Demo ab. Fuer v1.0 muss in Iter 20 entschieden werden:

- **Option A (Compose auf groesserer VM)**: Niedrige Komplexitaet, kein K8s-Wissen noetig, Skalierung nur vertikal. Realistisch fuer "Familien-/Freundes-App".
- **Option B (K3s auf 2-3 VMs)**: Hoehere Komplexitaet, Lerneffekt, echtes horizontales Scaling. Ueberproportional fuer aktuelles Volumen.

**Empfehlung in ADR-0029**: **Option A** fuer v1.0 (Compose mit Traefik), **mit dokumentiertem Migrationspfad nach K3s** fuer v1.5+. Diese Entscheidung passt zu ADR-0001 (SCS) ebenso wie zur dokumentierten Hetzner-Empfehlung. K8s-Komplexitaet jetzt einzufuehren waere ein Bus-Factor-Risiko (Memory: "Small Team Size, High Probability"). Nicht jede SCS-Architektur muss in K8s laufen.

### Was *nicht* in v1.0 muss

- Kuechendienst-Fairness-Analytik (im Iter-18-Plan als "in-aggregate-query bleibt" entschieden).
- Read-Replicas der Postgres-Instanzen.
- Cross-SCS-Tracing mit OpenTelemetry-Collector — `Iter 19` macht Tracing nur, soweit es ohne dedizierten Collector geht (Tempo OTLP-direct).
- Schema-Registry fuer Events — bleibt auch laut ADR-0025-Stub explizit out-of-scope.
- mTLS Cluster-intern.
- Push-Notifications.

### v1.0-Acceptance-Definition (vorgeschlagen)

**Travelmate ist v1.0.0-faehig, wenn folgende Bedingungen kumulativ erfuellt sind:**

1. ADR-0027 bis ADR-0034, ADR-0036, ADR-0037 akzeptiert und implementiert.
2. ADR-0028 (Outbox) im Lasttest validiert: 0 Events in DLQ bei kuenstlichem RabbitMQ-Restart.
3. Restore-Drill durchgefuehrt und dokumentiert: 4 DBs unter 30 min wiederhergestellt.
4. Lighthouse-Mobile-Score >= 90 auf Top-5-Pages.
5. Security-Slice-Tests fuer 100% der Controller in IAM, Trips, Expense gruen.
6. Cross-Tenant-Negativtest fuer 100% der Controller-Endpunkte gruen.
7. Lasttest 50 concurrent users x 5 min: p95 < 500ms auf Top-10-Flows. 0 OOMs.
8. Runbooks fuer mind. 5 Failure-Patterns vorhanden und reviewt.
9. SBOM (CycloneDX) als Build-Artefakt; 0 Critical CVEs.
10. Backups laufen seit >= 14 Tagen produktiv ohne Fehler.

---

## 7. Offene Fragen / Hot Spots

| ID | Frage | Empfehlung |
|----|-------|------------|
| HS-01 | Soll `travelmate-common` ueberhaupt gespalten werden (Plain JAR-Invariante)? | Ja — `travelmate-web-commons` als zweites Modul. ADR-0026 dokumentiert es bereits. |
| HS-02 | Welche Mailpit-/SMTP-Strategie in Prod? Nutzt das System Brevo/Sendgrid/SMTP-Provider? | Eigenes ADR (potenziell ADR-0040, nach v1.0). Aktuell Mailpit nur dev. Fuer v1.0: dokumentierter SMTP-Provider, nicht zwingend eigene Implementation. |
| HS-03 | Wer betreibt Travelmate v1.0 nach Go-Live? On-Call-Modell? | Out-of-scope der Architektur-Roadmap. Aber: Runbooks (TD-17) sind die *technische* Grundlage fuer eine On-Call-Diskussion. |
| HS-04 | Multi-Region oder Single-Region? | Single-Region (DE) reicht. Multi-Region nicht in v1.0. Klarstellen in ADR-0029. |
| HS-05 | Soll Trips-/Expense-Datenmenge irgendwann archiviert werden (alte Trips > 5 Jahre)? | DSGVO-Frage. Aktuell offen. Nicht v1.0-Sperre, aber **DSGVO-Loeschkonzept** als ADR-0041 oder Operations-Doku einplanen. |
| HS-06 | Wird CSV/PDF-Export ein API-Vertrag oder bleibt UI-only? | Bleibt UI-only laut Iter-10-Output. Damit kein API-Versions-Problem. |

---

## 8. Verifikationsstrategie fuer v1.0-Reife

Pro Iter-Block werden folgende Architektur-Fitness-Funktionen ergaenzt:

- **Iter 19**: ArchUnit-Tests `events_have_tenantid_and_occurred_on`, `event_naming_convention_*Created/*Updated/*Deleted`, `outbox_table_present_per_scs`.
- **Iter 20**: ArchUnit-Test `no_default_credentials_in_application_yml` (Source-Scanner). Trivy-Gate in CI.
- **Iter 21**: Backup-Restore-Drill als Operational-Acceptance-Test (manuell, Quartal).
- **Iter 22**: Security-Slice-Test-Coverage-Bericht. OWASP-Dep-Scan-Bericht.
- **Iter 23**: Lighthouse-CI-Bericht je PR.
- **Iter 24**: Cross-Tenant-Negativtest pro Controller (automatisiert in CI).
- **Iter 25**: Lasttest-Bericht als Build-Artefakt. SLO-Verletzungen failen Nightly.

Plus durchgehend: ArchUnit-PlantUML-Diagramm-Validation (`adhereToPlantUmlDiagram`) — wird in Iter 21 bei Aktualisierung der Deployment-View eingefuehrt.

---

## 9. Zusammenfassung in einem Bild

```
v0.17.0 ──┬─ Iter 18 ──┬─ Iter 19 ──┬─ Iter 20 ──┬─ Iter 21 ──┬─ Iter 22 ──┬─ Iter 23 ──┬─ Iter 24 ──┬─ Iter 25 ──┬─ v1.0.0-RC1
          │            │            │            │            │            │            │            │            │
          │ Multi-Org  │ Outbox     │ Deployment │ CI/CD      │ Rate-Limit │ Logging    │ Tenant-    │ Lasttest   │ Freeze
          │ E2E-Fix    │ Observ.    │ Secrets    │ Backups    │ Caching    │ HTMX self  │ Isolation  │ JVM-Tune   │ +
          │ ADR-0024   │ ADR-0026   │ TLS        │ ADR-0032   │ ADR-0034   │ PWA-SW     │ Stateless  │ ADR-0039   │ Acceptance
          │ ADR-0025-D │ ADR-0027   │ ADR-0029   │ ADR-0033   │ ADR-0035   │ ADR-0036   │ ADR-0037   │            │ Drill
          │            │ ADR-0028   │ ADR-0030   │            │            │            │ ADR-0038   │            │
          │            │            │ ADR-0031   │            │            │            │            │            │
          │            │            │            │            │            │            │            │            │
          ──Feature──   ───Plattform─-Reife────────────────────────────────────────────────────────────────────────►
```

**Roadmap-These**: Travelmate ist ein "feature-rich, infrastructure-thin"-Projekt. Der Pfad zu v1.0 ist die *Inversion* dieses Verhaeltnisses — nicht durch Featurereduktion, sondern durch konsequentes Plattform-Investment ueber sieben Iterationen.

---

## Referenzen

- arc42 §07 (Deployment View), §08 (Cross-Cutting), §10 (Quality Requirements), §11 (Risks)
- ADR-0001, ADR-0006, ADR-0008, ADR-0011, ADR-0013, ADR-0023
- `reports/iteration-18-architecture.md` (Iter-18-Vorlauf)
- `docs/operations/2026-03-26-demo-betriebskonzept.md`
- `docs/operations/2026-03-26-kubernetes-hosting-marktrecherche.md`
- ISO 25010:2011, "Systems and software Quality Requirements and Evaluation"
- INNOQ Quality Storming, Quality Value Chain Evolution
- embarc Architekturbewertung (ATAM, qualitative scenarios)
