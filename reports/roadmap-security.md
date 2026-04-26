# Roadmap-Level Security Plan — Travelmate v1.0

**Datum**: 2026-04-26
**State after**: v0.17.0 (Iteration 18 in Vorbereitung)
**Target Version**: v1.0.0 (Production-ready)
**Methodik**: OWASP Top 10 (2021) + STRIDE + GDPR-Mapping
**Scope**: Roadmap-Planung, *kein* vollstaendiger Pentest und *kein* Code-Review-Bericht. Sicherheits-Iterationen werden hier abgeleitet.

---

## 0. Executive Summary

Travelmate hat eine **solide Architektur-Basis** fuer Security: Keycloak OIDC, Gateway als Single Entry Point, JWT-Resource-Server pro SCS, Spring Data JPA (kein nativer SQL), Thymeleaf-Autoescaping, TenantId-Scoping als Architekturprinzip, SSRF-Vorkehrungen (ADR-0016). Fuer eine Produktiv-Erstrelease (v1.0) fehlen jedoch zentrale operative und Hardening-Bausteine.

### Top 5 Risiken auf dem Weg zu v1.0
1. **CSRF-Schutz pauschal deaktiviert** in IAM, Trips, Expense und Gateway (`csrf.disable()`). Cookie-basierte OIDC-Session am Gateway -> klassischer CSRF-Vektor fuer state-changing HTMX-POSTs.
2. **Keine Production-Secrets-Strategie**: `application.yml` enthaelt funktionierende Defaults (`KEYCLOAK_CLIENT_SECRET:travelmate-gateway-secret`, `RABBITMQ_PASSWORD:guest`, `KEYCLOAK_ADMIN_PASSWORD:admin`). Vergessenes ENV-Override -> Standard-Credentials in Produktion.
3. **Keine Audit-Logs**: weder Login/Logout noch sicherheitskritische State-Changes (`grantOrganizerRole`, `deleteTrip`, `removeMember`) werden separat geloggt. Repudiation-Risiko und keine Forensik-Grundlage.
4. **Tenant-Isolation rein anwendungsseitig** ueber `WHERE tenant_id = ?` in JPA. Eine vergessene Klausel = Cross-Tenant-Leak ohne Datenbank-Defense-in-Depth (kein RLS, kein Schema-per-Tenant).
5. **Keine SCA/SAST-Pipeline**: kein OWASP Dependency-Check, kein Trivy/Snyk, kein CodeQL. Kein Dependabot. v1.0 ohne CVE-Watchpipeline ist nicht vertretbar.

### Mehrere bestehende Annahmen sind aktuell nicht zutreffend
- Security-Rules-Doku sagt "CSRF protection active (Spring Security default) — do not disable" — Code disabled CSRF in allen vier Modulen.
- ADR-0016 sagt "HTTPS-Only" fuer Import-URLs — `UrlSanitizer.validate()` erlaubt HTTP.
- Arc42 §8 erwaehnt einen `TenantIdentificationFilter` ueber HTTP-Header `x-travelmate-tenant-id` — der ist im Code nirgends referenziert; tatsaechlich erfolgt Tenant-Resolution ueber den JWT-`email`-Claim. Doku/Code sind divergent.

Diese drei Diskrepanzen muessen vor v1.0 *entweder* durch Implementierung *oder* durch Doku-Korrektur aufgeloest werden.

---

## 1. OWASP Top 10 (2021) Coverage Matrix

Bewertung: **GREEN** = produktionsreif | **YELLOW** = Luecken bekannt, Mitigation geplant | **RED** = blockierend fuer v1.0.

| ID | Kategorie | Status | Vorhanden | Luecke fuer v1.0 |
|----|-----------|--------|-----------|------------------|
| A01 | Broken Access Control | YELLOW | TenantId-Scoping in allen Aggregaten + JPA-Queries; JWT-`email`-Claim als Tenant-Resolver server-seitig; Keycloak-Rolle `organizer`/`participant`; ADR-0024 plant Multi-Organizer als Trip-lokale Aggregat-Property (verhindert Cross-Tenant-Promotions). | (a) Keine ArchUnit-Regel "alle Repository-Methoden filtern nach `tenantId`". (b) Keine DB-RLS oder Schema-per-Tenant als Defense-in-Depth. (c) IDOR-Pruefung systematisch fehlt: UUIDs in URLs sind nicht ratbar, aber jede `findById(...)`-Stelle braucht zusaetzlichen TenantId-Guard. (d) Method-Security (`@PreAuthorize`) wird nirgends genutzt — Authorization erfolgt rein imperativ in Application Services. |
| A02 | Cryptographic Failures | YELLOW | Keycloak handhabt Passwoerter (Argon2/bcrypt je Realm-Config). JWT-Signaturpruefung via JWK-Set-URI. Keine sensiblen Daten im Code. | (a) Keine TLS-Strategie produktionsseitig dokumentiert (Gateway? Ingress? Service-Mesh?). (b) HTTP zwischen Gateway und SCS ist intern, aber unverschluesselt — fuer Mehr-Host-Deployment ungeeignet. (c) Keycloak-Default-Passwort `admin/admin` und Default-Realm-Passwortregeln nicht produktionshart. (d) Verbindungsstrings + DB-Passwort als ENV — Secrets-Provider (Vault/SSM/Sealed-Secrets) nicht definiert. |
| A03 | Injection | GREEN-leaning | Spring Data JPA only — keine `@Query(nativeQuery=true)`-Treffer. Keine `Runtime.exec`/`ProcessBuilder` in Production-Code (nur in E2E-Helpers). Thymeleaf default-escaped, `th:utext` nirgends genutzt. | (a) RabbitMQ-Consumer deserialisieren via Jackson — Polymorphic-Type-Handling ist nicht explizit hard locked (Default-Typing aus). Sollte als ArchUnit/Boot-Test verifiziert werden. (b) Keine systematische Validation auf Consumer-Boundaries (Idempotency-Tests vorhanden, aber kein "alle Felder ueber `Assertion` validiert"-Contract-Test). |
| A04 | Insecure Design | YELLOW | DDD/Hexagonal trennt Business-Invariants vom Adapter-Layer; Aggregat-Guards (z.B. `Trip.deleteTrip` Status-Guard, `AccommodationPoll AWAITING_BOOKING`-Guard) sind explizit. ADR-0023 dokumentiert Cleanup-Eventflow. ADR-0024 plant Self-Demotion + Last-Organizer-Schutz. | (a) Kein formales Threat-Model-Dokument (`docs/threats/` existiert nicht). (b) Kein Rate-Limiting — Sign-up + Login + Password-Reset sind ungeschuetzt gegen Bruteforce/Enumeration. (c) Invitation-Token-Lifecycle: Single-use + Time-Limit muss verifiziert werden (relevant bei `ExternalUserInvitedToTrip`). (d) Keine Misuse-Cases dokumentiert (gegenstueck zu User-Stories). |
| A05 | Security Misconfiguration | RED | `@Profile("!test")` / `@Profile("test")` Trennung sauber (Test-Config kann nie produktiv aktiv sein). | (a) **CSRF in allen vier SecurityConfigs deaktiviert** — Cookie-basierte OIDC-Session am Gateway + HTMX-POST = aktiver CSRF-Vektor. (b) **Keine Security-Header** (HSTS, X-Frame-Options, X-Content-Type-Options, Content-Security-Policy, Referrer-Policy). (c) Actuator `gateway` und `prometheus` exposed in Gateway ohne Auth-Restriction — Routen-Inspektion oeffentlich erreichbar wenn nicht durch Ingress geschuetzt. (d) Default-Credentials in `application.yml` (Keycloak admin, RabbitMQ guest, Keycloak Client-Secret) sind technisch ENV-Override — aber im Falle einer ENV-Vergesslichkeit funktioniert das System mit Defaults. |
| A06 | Vulnerable Components | RED | Spring Boot BOM 4.0.3, Spring Cloud 2025.1.1 (Oakwood) als Aktualitaets-Anker. | (a) **Keine SCA-Pipeline** — kein OWASP Dependency-Check, kein Snyk, kein Trivy, kein CodeQL. (b) **Kein Dependabot/Renovate** in `.github/`. (c) Container-Images (gebaut via `docker compose --build`) werden nicht auf CVEs gescannt. (d) Jsoup, Tesseract, Flying-Saucer (PDF), Ollama-Client als zusaetzliche Angriffsflaeche — keine Versionspolicy. |
| A07 | Identification & Auth Failures | YELLOW | Keycloak OIDC mit Authorization-Code + PKCE; JWT-Validierung pro SCS via `oauth2ResourceServer.jwt()`. RP-Initiated Logout konfiguriert (Gateway-`SecurityConfig.keycloakLogoutSuccessHandler`). Email-Verifikation existiert (Keycloak Standard). | (a) Keycloak-Realm nicht hardened (Brute-Force-Detection, Passwort-Policy, MFA — alles Default). (b) Session-Timeout, Refresh-Token-Rotation und Idle-Timeout nicht in Realm-Config dokumentiert. (c) JWT-`email`-Claim als Tenant-Resolver — Email kann sich aendern (Keycloak Email-Update); kein Account-`subject`-Mapping vorhanden, was Re-Identification fragil macht. (d) Keine Detection fuer "JWT mit Email-Claim, der nicht mehr existiert" — Tenant-Resolution wirft `EntityNotFoundException`, das im GlobalExceptionHandler in HTTP 404 muendet (nicht 401/403). |
| A08 | Software & Data Integrity | YELLOW | Flyway-Migrationen mit Checksumme. Domain-Events ueber RabbitMQ in vertrauenswuerdigem Inner-Network. CSRF-Default-Token theoretisch verfuegbar, aber explizit disabled. | (a) Keine Container-Image-Signierung (Cosign), kein SBOM (CycloneDX). (b) Keine Event-Signing — Producer/Consumer vertrauen blind. Solange RabbitMQ inside-mesh laeuft, akzeptabel; bei spaeterer Multi-Tenant-Cloud-Migration kritisch. (c) ADR-0025 (geplant) adressiert Schema-Versionierung — Schema-Drift wird im Build sichtbar. (d) Mailpit ist Dev-only — produktiver SMTP-Provider + DKIM/SPF/DMARC Setup nicht definiert (verhindert Email-Spoofing fuer Invitation-E-Mails). |
| A09 | Security Logging & Monitoring | RED | Spring Boot Standard-Logging; Actuator-Health/Prometheus-Endpoints exponiert; Mailpit fuer Email-Inspektion in Dev. | (a) **Keine Audit-Logs** — kein Login/Logout-Event geloggt; kein `grantOrganizerRole`/`deleteTrip`/`removeMember`-Audit; keine Kennzeichnung "wer hat was wann gemacht". (b) Keine Aggregation/Forwarding (kein Loki, ELK, CloudWatch). (c) Keine Alerts auf Sicherheitsereignissen (failed-logins-spike, DLQ-Wachstum, 401/403-Bursts). (d) `GlobalExceptionHandler.handleRuntimeException` loggt mit Default-Level — keine Differenzierung "security-relevant" vs. "domain-error". |
| A10 | Server-Side Request Forgery | YELLOW | ADR-0016 dokumentiert SSRF-Schutz; `UrlSanitizer` blockt private/loopback/link-local IPs; `HtmlFetcher` (Trips Accommodation Import) hat Size-Limit (5MB), Timeout (10s), Redirect-Limit (3). | (a) **`UrlSanitizer.validate()` erlaubt HTTP** entgegen ADR-0016 ("HTTPS-Only"). Soll `https`-only sein. (b) `UrlSanitizer.checkHostNotPrivate` swallowt `UnknownHostException` und akzeptiert "nicht aufloesbar = nicht privat" — DNS-Rebinding-Vektor: gibt Angreifer kontrollierbare DNS-Antwort. (c) Kein DNS-Re-Resolve nach Redirect (ADR-0016 verlangt es). (d) Recipe-URL-Import (S19-B01) ist neu fuer Iteration 19 — wird die gleiche `HtmlFetcher`-Komponente verwenden. (e) Ollama-LLM-Client ruft `localhost:11434` an — das ist intentional, aber `OllamaClient` darf nicht durch konfigurierbare URL angreifbar werden (Server-Operator-Vertrauen). |

### Schnellindikator
- 2 RED (A05 Misconfig, A06 Vuln-Components, A09 Logging) -> diese drei sind nicht-verhandelbar fuer v1.0.
- 5 YELLOW -> mit gezielten Stories adressierbar.
- 1 GREEN-leaning (A03) -> kontinuierliche Wachsamkeit, aber kein akuter Block.

---

## 2. STRIDE Threat-Model — High-Level v1.0 Threat Surface

Pro Bounded Context die *strukturellen* Bedrohungen, nicht jeder einzelne Threat-Token. Detail-Threats werden in `docs/threats/<bc>-threat-model.md` ausformuliert (Story unten).

### 2.1 Cross-Cutting (Gateway / OIDC)

| STRIDE | Threat | Komponente | Aktuelle Mitigation | Gap |
|--------|--------|-----------|---------------------|-----|
| S | Forged JWT | Gateway, alle SCS | JWK-Set-URI gegen Keycloak | OK, vorausgesetzt JWK-Endpoint wird ueber HTTPS in Prod genutzt |
| S | Session-Cookie-Hijack (Gateway) | Gateway | Spring Session Cookie | Cookie-Flags (Secure, HttpOnly, SameSite=Lax/Strict) muessen verifiziert werden |
| T | CSRF auf state-changing HTMX-Endpoints | Alle SCS + Gateway | KEINE — `csrf.disable()` ueberall | RED — primaerer Mitigation-Auftrag |
| R | Login/Logout nicht geloggt | Keycloak | Keycloak-Eventlog (default off) | Audit-Eventlog aktivieren + Forwarding |
| I | Verbose Errors leaken Stacktraces | GlobalExceptionHandler | Sanitiesierte Messages, RuntimeException-Handler vorhanden | Verifikation: in Production-Profil keine `server.error.include-stacktrace=ALWAYS` |
| D | Bruteforce auf Login/Sign-up/Password-Reset | Gateway -> Keycloak / IAM `/signup` | KEINE | Rate-Limiting + Keycloak-Brute-Force-Detection |
| E | Privilege Escalation via Keycloak-Rollen | Keycloak Realm | Realm-Admin getrennt | Keycloak Admin-Console-Zugang im Prod nicht oeffentlich exposed |

### 2.2 IAM SCS

| STRIDE | Threat | Beispiel | Status |
|--------|--------|----------|--------|
| S | Anmeldung mit existierender Email -> Account-Takeover | `/signup` ohne Email-Verifikation-Lock | Keycloak verifiziert Email; vor erster Verifikation aber bereits Tenant + Account angelegt -> Half-state-Takeover-Risiko |
| T | DoB/Email-Manipulation in Sign-up Form | `SignUpService` | `Assertion`-Validation in Value Objects; Email-Format-Pflicht |
| R | Tenant-Loeschung nicht auditiert | `/admin/tenants/{id}` (E2E-Cleanup-Endpoint!) | RED — Endpoint existiert mit Authentifizierung, aber ohne Audit-Log und ohne Rollen-Pruefung im Code (nur `authenticated()`) |
| I | Cross-Tenant Member-Liste | TravelParty-Projection in Trips | App-seitiger TenantId-Filter pro Repository-Methode |
| D | Mass Sign-up flood | `/signup` public | KEINE Rate-Limit — CAPTCHA o.ae. fehlt |
| E | Externer Invitation-Flow erlaubt Account-Erstellung in fremdem Tenant | `ExternalInvitationConsumer` | Aggregat erzwingt Tenant aus Invitation-Event — verifikation per Test |

### 2.3 Trips SCS

| STRIDE | Threat | Beispiel | Status |
|--------|--------|----------|--------|
| S | Spoofing eines Trip-Owners via JWT-Email-Wechsel | Trip ist an `tenantId` gekoppelt; Email-Aenderung in Keycloak ohne Account-Update -> Tenant-Resolver wirft 404 (kein Access) | YELLOW — kein subject-basiertes Mapping |
| T | Trip-State-Manipulation (PLANNING -> COMPLETED skip) | `Trip.deleteTrip` `PLANNING`-only Guard | OK — Aggregat-Guards |
| T | Invitation-Replay (alter Token mehrfach genutzt) | `Invitation`-Aggregat | Single-use-Logik im Aggregat — verifikation per BDD |
| R | Multi-Organizer-Promotion nicht geloggt | ADR-0024-Plan | Audit-Log-Story Pflicht (siehe Backlog) |
| I | Cross-Tenant Trip-Liste | TravelParty-Projection | App-seitiger TenantId-Filter |
| D | Mass-Event-Publish ueber RabbitMQ-Backpressure | Producer veroeffentlicht in Schleife | RabbitMQ-DLQ + Consumer-Idempotenz; aber kein Rate-Limit auf Producer-Seite |
| E | Cross-Tenant Organizer-Promotion | ADR-0024-Mitigation: Participant-Constraint impliziert Tenant | OK — solange Aggregat-Tests den Guard absichern |

### 2.4 Expense SCS

| STRIDE | Threat | Beispiel | Status |
|--------|--------|----------|--------|
| S | Expense-Modification durch Nicht-Trip-Teilnehmer | `Expense`-Aggregat | TenantId-scoped + TripProjection-Constraint |
| T | Receipt-Datei-Tampering (Multipart-Upload) | OCR-Adapter (Tesseract) | ADR-0016 dokumentiert Magic-Bytes-Pruefung — Verifikation per Test |
| R | Expense-Settlement-Aenderung nicht auditiert | `Expense.settle()` | RED — keine Audit-Logs |
| I | PDF-Settlement-Export an falschen Empfaenger | `SettlementPdfService` | Authorization Check in Controller (TenantId match) — verifikation per Pentest |
| D | Massen-Receipt-Upload (10MB pro Datei x N Requests) | OCR-Endpoint | Multipart-Size-Limit OK; kein Rate-Limit per User |
| E | Settlement-Trigger ueber gefaktes `TripCompleted`-Event | Consumer in Expense | RabbitMQ-internes Vertrauen — bei Multi-Tenant-Cloud kritisch |

---

## 3. Security-Hardening-Backlog

Stories sind in **Effort-Buckets** (S = <1 Tag, M = 1-3 Tage, L = >3 Tage) und nach **v1.0-Blockierung** (P0 = Blocker, P1 = Strongly recommended, P2 = Nice-to-have) eingeordnet.

### P0 — Blocker fuer v1.0

| ID | Story | Effort | Begruendung |
|----|-------|--------|-------------|
| SEC-01 | **CSRF re-aktivieren** in IAM, Trips, Expense, Gateway. HTMX-Token-Header `X-CSRF-TOKEN` per Default-Tag in Layout integrieren; Webflux-CSRF im Gateway via `CookieServerCsrfTokenRepository.withHttpOnlyFalse()`. | M | Direktes Loch — jeder authentisierter Browser kann state-changing POSTs cross-origin triggern. |
| SEC-02 | **Production-Secrets-Strategie**: keine Defaults in `application.yml`; ENV-Pflicht via `${VAR}` ohne Fallback fuer DB_PASSWORD, RABBITMQ_PASSWORD, KEYCLOAK_CLIENT_SECRET, KEYCLOAK_ADMIN_PASSWORD. Doku in `docs/arc42/07-deployment-view.md`: Secrets via SOPS/Sealed-Secrets/Vault — Auswahl noch zu treffen. Boot-Fail wenn Pflicht-ENV fehlt. | M | Fail-secure statt Fail-with-default. |
| SEC-03 | **Security-Header** in allen vier `SecurityConfig`s: HSTS (`max-age=31536000; includeSubDomains; preload`), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, CSP (Default-src self + inline-Hash fuer HTMX/PicoCSS-CDN-URLs). | S | OWASP-Baseline. |
| SEC-04 | **TLS-Termination festlegen**: Empfehlung Reverse-Proxy/Ingress (nginx/Traefik/Caddy) vor Gateway; Gateway-Listen-Port intern; SCS nur ueber Gateway erreichbar (Network-Policy). Doku in `07-deployment-view.md`. | M | Ohne TLS keine Cookie-Secure-Flag, keine OIDC-Konformitaet. |
| SEC-05 | **SCA-Pipeline aktivieren**: OWASP Dependency-Check Maven-Plugin (`dependency-check-maven`) + Trivy fuer Container + Dependabot/Renovate fuer Dependency-Bumps. Failing-Build ab CVSS >= 7.0. | M | Continuous CVE-Watch ist Pflicht. |
| SEC-06 | **Audit-Log fuer Sicherheitsereignisse**: Application-Service-Layer publisht `SecurityAuditEvent` (Login, Logout, RoleGranted, TenantDeleted, TripDeleted, AccountRemoved). Persistierung in eigenem `audit_log`-Schema pro SCS; Queryable via Admin-UI (P2). Strukturlog-Forwarding ist v1.1. | L | Forensik-Grundlage; GDPR-Verarbeitungsverzeichnis. |
| SEC-07 | **Rate-Limiting am Gateway**: Spring Cloud Gateway `RequestRateLimiter`-Filter mit Redis (or in-memory bucket fuer MVP). Per-IP-Limit auf `/oauth2/authorization/keycloak`, `/iam/signup`, `/iam/register`. Per-User-Limit auf state-changing-Routes. | M | Bruteforce + Mass-Sign-up-Schutz. |
| SEC-08 | **Keycloak Production Hardening**: Brute-Force-Detection, Passwort-Policy (min length 12, complexity), Session-Idle 30min/Max 12h, Refresh-Token-Rotation, Email-Verifikation Pflicht VOR erster Anmeldung, Admin-Console nur ueber separates Realm/IP-Allowlist. Konfig deklarativ via `keycloak/realm-export.json`, nicht imperativ. | M | Realm-Defaults sind fuer Demo, nicht Produktion. |
| SEC-09 | **`@Profile("test")`-Permitall + AdminController-Cleanup**: AdminController `DELETE /admin/tenants/{id}` (E2E-Helper) muss `@Profile("e2e")` oder `@Profile("!production")`-restricted sein UND mit Keycloak-Rolle `realm-admin` geschuetzt; aktuell nur `authenticated()`. | S | Ein authentifizierter Nutzer kann beliebige Tenants loeschen, sofern URL bekannt. |

### P1 — Strongly Recommended (vor v1.0 abdecken)

| ID | Story | Effort | Begruendung |
|----|-------|--------|-------------|
| SEC-10 | **SSRF-Hardening** im `UrlSanitizer`: HTTPS-Only enforcen (entgegen aktueller Akzeptanz von `http://`), `UnknownHostException` als Fehler werfen statt swallowen, Re-Validation nach jedem Redirect, IPv6-Private-Ranges (fc00::/7) explizit blocken. | S | Diskrepanz zwischen ADR-0016 und Code. |
| SEC-11 | **ArchUnit-Regel "TenantId-scoped repositories"**: jede Repository-Methode in `adapters/persistence` muss entweder `tenantId`-Parameter haben oder `@Transactional`-private (interne Helper). Pilot in Trips, dann IAM/Expense. | M | Defense-in-Depth-Compiler-Stuetze fuer A01. |
| SEC-12 | **Misuse-Cases dokumentieren** pro Story-Template: `feature.story.md` erweitert um "Abuse Cases" Sektion. Backfill fuer Top-10 sicherheitsrelevante bestehende Stories (Sign-up, Trip-Delete, Multi-Organizer, External-Invitation, Receipt-Upload, URL-Import). | M | Insecure Design (A04). |
| SEC-13 | **Threat-Model-Dokumente** pro BC: `docs/threats/iam-threat-model.md`, `docs/threats/trips-threat-model.md`, `docs/threats/expense-threat-model.md` nach OWASP Four-Question Framework + STRIDE-per-element. | L | Lebende Doku. Update nach jedem Major-Feature Pflicht. |
| SEC-14 | **Pen-Test Light** vor v1.0-Release: Authorisierter Pentest gegen Staging-Environment durch security-expert-agent + extern. Report nach `reports/pentest-vYYYY-MM-DD.md`. | M | Externes Eyes-on. |
| SEC-15 | **JWT-Subject-Mapping**: zusaetzlich zum Email-Claim wird `sub` (Keycloak User-ID) als Account-Mapping persistiert. Tenant-Resolution faellt zurueck auf `sub`, falls Email nicht aufloesbar. | M | Email-Aenderbarkeit ist Risiko. |
| SEC-16 | **Actuator-Hardening**: `gateway`, `prometheus` Endpoints nur ueber separaten Management-Port + IP-Allowlist; `info` neutralisiert (keine Build-Versions-Leaks fuer extern). | S | A05 Misconfig. |
| SEC-17 | **Cookie-Flags verifizieren** (Spring Session Cookie am Gateway): `Secure`, `HttpOnly`, `SameSite=Lax`. Test in `gateway`-Modul. | S | Trivial, aber dokumentationspflichtig. |
| SEC-18 | **Email-Spoofing-Schutz fuer Invitation-Mails**: produktiver SMTP mit DKIM/SPF/DMARC-Records auf Domain. Fallback-Strategie wenn Mail-Bounce (DLQ-aehnlich). | M | Reputation + Phishing-Resistenz. |
| SEC-19 | **Magic-Bytes-Verifikation Receipt-Upload** verifizieren (ADR-0016 sagt "JPEG/PNG/HEIC"); Test in `expense`-Modul fuer manipulierte Content-Type-Header (HTML-in-Image). | S | A03 Injection-Vector ueber Upload. |

### P2 — Nice-to-have (v1.x)

| ID | Story | Effort | Begruendung |
|----|-------|--------|-------------|
| SEC-20 | DB Row-Level-Security (Postgres RLS) mit `tenant_id`-Policy. Erfordert Connection-Pool-Awareness via `SET app.tenant_id = ...`. | L | Defense-in-Depth fuer A01. Erst sinnvoll wenn Single-DB-Multi-Tenant. Aktuell sind 4 separate DBs schon eine BC-Trennung. |
| SEC-21 | Audit-Log-Admin-UI in IAM (read-only Filter nach User/Event-Type/TimeRange). | M | Operational tooling. |
| SEC-22 | SBOM (CycloneDX) + Container-Image-Signing (Cosign). | M | Supply-Chain-Hardening. |
| SEC-23 | MFA via Keycloak (TOTP) opt-in fuer Organizer-Rolle. | M | Zukunftssicher. |
| SEC-24 | RabbitMQ-TLS + Event-Signing (JWS-signed event payload). | L | Erst bei Cloud-Multi-Tenant relevant. |
| SEC-25 | DAST (OWASP ZAP) gegen Staging im Nightly. | M | Continuous Security-Testing. |

---

## 4. Compliance — GDPR

Travelmate verarbeitet **personenbezogene Daten** (Namen, Email, Geburtsdatum, ggf. Trip-Standorte) und **finanzielle Daten** (Receipts/Expenses). GDPR ist nicht optional.

### 4.1 Vorhandene GDPR-Bausteine

- **Datenminimierung in der Domain**: Dependents (Mitreisende) werden ohne Login angelegt — kein Email/Passwort-Speicher fuer sie.
- **Trip-Delete-Cascade** (ADR-0023): Loeschen einer Reise propagiert ueber `TripDeleted`-Event; Trip-bezogene Receipts/Projections werden mitgeloescht.
- **Tenant-Delete in IAM**: AdminController kann Tenant loeschen (E2E-Helper).
- **Mailpit Dev-only**: keine echten Mails in Test/Dev-DB.

### 4.2 GDPR-Luecken zu v1.0

| ID | Anforderung | Status | Story |
|----|-------------|--------|-------|
| GDPR-01 | **Right to Erasure** (Art. 17 GDPR) fuer Account selbst | TEILWEISE — Tenant-Delete loescht Tenant + Member, aber Cross-SCS-Propagation (TripProjection in Expense, Receipts mit Tenant-Bezug, Audit-Logs) ist nicht spezifiziert | SEC-GDPR-01: `AccountDeletionRequested`-Event mit Cascade-Propagation in alle SCS; Soft-Delete fuer Audit-Trail-Erhalt vs. Hard-Delete-Pflicht abstimmen (i.d.R. ueberwiegen GDPR-Interessen). |
| GDPR-02 | **Right to Access** (Art. 15) — User darf alle eigenen Daten exportieren | NICHT VORHANDEN | SEC-GDPR-02: `/iam/account/export`-Endpoint, der JSON/PDF mit allen TenantId-scoped Daten aller drei SCS aggregiert. |
| GDPR-03 | **Right to Rectification** (Art. 16) | TEILWEISE — Account-Edit existiert in IAM | OK fuer v1.0; Nachvollziehbarkeit via Audit-Log (SEC-06) |
| GDPR-04 | **Verarbeitungsverzeichnis** (Art. 30) | NICHT VORHANDEN | SEC-GDPR-03: `docs/compliance/processing-register.md` mit Datenarten, Zwecken, Empfaengern, Retention-Fristen pro BC. |
| GDPR-05 | **Privacy by Design / DPIA** | NICHT VORHANDEN | SEC-GDPR-04: Data-Protection-Impact-Assessment fuer Multi-Tenant-Travel-Daten + Geo-Daten + Finance-Daten. |
| GDPR-06 | **Data Retention Policy** | NICHT VORHANDEN | Trip-Daten nach Jahr X archivieren? Audit-Logs nach Y Monaten purgen? Receipts unter Steuerrecht 10 Jahre vs. GDPR-Loeschung — Spannungsfeld. SEC-GDPR-05. |
| GDPR-07 | **Datenuebertragbarkeit** (Art. 20) | NICHT VORHANDEN | Bei Account-Export-Story (GDPR-02) gleich strukturiertes JSON-Format spezifizieren. |
| GDPR-08 | **Auftragsverarbeitungsvertrag (AVV)** mit Hosting-/Mail-Provider | OFFEN | Prozessual, abhaengig von Hoster-Wahl. |
| GDPR-09 | **Cookie-Consent** | NICHT NOETIG falls nur Session-Cookies (Functional-Necessary). Verifizieren falls Analytics dazu kommt. | Status quo: Session-Only -> kein Consent-Banner. |
| GDPR-10 | **Datenschutzerklaerung + Impressum** | NICHT VORHANDEN | Public Static Pages am Gateway. |

### 4.3 Spezifischer Hinweis zu Trip-Delete-Cascade (ADR-0023)

ADR-0023 deckt **Trip-Loeschen**, nicht **Account-Loeschen**. Fuer GDPR Right-to-Erasure muss zusaetzlich:
- alle Trips, in denen der Account *einziger Organizer* ist, vorher uebergeben oder als Cancelled archiviert
- alle Trips, in denen der Account *Participant* ist, durch `ParticipantRemovedFromTrip` getrennt (Event existiert seit Iteration 17)
- alle Receipts, die der Account *Eigentuemer* ist, anonymisiert (nicht geloescht — Buchhaltungspflicht!) oder bei akzeptiertem Datenverlust haengt am Tenant-Lebenszyklus
- Keycloak-User per Admin-API geloescht
- Audit-Log: hier kollidieren Forensik (SEC-06) und Erasure — uebliche Loesung: Pseudonymisierung statt Loeschung

Das ist eine **eigene Story** (SEC-GDPR-01), nicht ein Folgekapitel von ADR-0023.

---

## 5. Empfohlene Security-Iteration-Sequenz (Iter 18 - 25)

Security-Stories werden **nicht in einer einzigen Security-Iteration** gebuendelt, sondern als P0-Block vor v1.0-Release ueber 3-4 Iterationen verteilt — gekoppelt mit funktionalen Iterationsthemen. Stage-gating: jede neue funktionale Iteration ab Iter 22 (Pre-v1.0) verlangt 0 offene P0-Stories.

| Iteration | Funktionaler Fokus (aus iteration-18-architecture.md weitergedacht) | Security-Stories | Begruendung |
|-----------|---|------------------|-------------|
| **Iter 18** | E2E-Stabilitaet + ADR-0024 Multi-Organizer | SEC-09 (AdminController-Hardening), SEC-10 (UrlSanitizer-Fix), SEC-13 (Trip-Threat-Model) | Multi-Organizer ist Authorisierungsthema -> Threat-Model jetzt; SEC-09 + SEC-10 sind kleine Gaps in bereits getouchten Modulen. |
| **Iter 19** | Recipe-URL-Import (S19-B01), ADR-0026 ExceptionHandler-Dedup | SEC-11 (ArchUnit Tenant-Scope), SEC-12 (Misuse-Cases), SEC-19 (Magic-Bytes-Test) | Recipe-Import bringt zweiten Use-Case fuer `HtmlFetcher` -> SSRF-Tests verstaerken. ArchUnit-Regel passt mit anderen Build-Hardening-Themen. |
| **Iter 20** | Audit + Logging (Sicherheitsfokus) | **SEC-06 (Audit-Log)**, SEC-16 (Actuator-Hardening), SEC-17 (Cookie-Flags) | Audit-Log ist groesste Einzelstory (L); braucht eigene Iter-Aufmerksamkeit. |
| **Iter 21** | Production-Hardening I | **SEC-01 (CSRF re-enable)**, SEC-03 (Security-Header), SEC-04 (TLS-Termination-Doku), SEC-05 (SCA-Pipeline) | CSRF + Header + TLS-Doku + SCA: das "Goalpost"-Set fuer Misconfig + Vuln-Components. |
| **Iter 22** | Production-Hardening II | SEC-02 (Secrets-Strategie), SEC-07 (Rate-Limiting), SEC-08 (Keycloak-Hardening) | Operational + Auth-Hardening kombiniert. Iter-22-Ende = Code-Freeze auf Security-Block-Themen. |
| **Iter 23** | GDPR + Compliance | SEC-GDPR-01 (Right-to-Erasure), SEC-GDPR-02 (Right-to-Access), SEC-GDPR-03 (Verarbeitungsverzeichnis), SEC-GDPR-05 (Retention-Policy) | GDPR ist groesster Compliance-Block; passt nach den technischen Hardenings. |
| **Iter 24** | Pre-Release Verification | SEC-14 (Pen-Test), SEC-15 (JWT-Sub-Mapping), SEC-18 (Email-Spoofing-Schutz) | Pentest am Ende, *nachdem* Hardening greift. |
| **Iter 25** | v1.0 Release-Iteration | Restbereinigung, Doku-Review, security.txt | Finale. |

**Begruendung der Reihenfolge:**
1. Iter 18-19 sind "Mitnahmen" — kleine Stories in Iterationen, deren funktionaler Fokus security-bezogene Module beruehrt. Niedriger Reibungsverlust.
2. Iter 20 (Audit-Log) ist absichtlich frueh: alle nachfolgenden Hardenings profitieren davon, dass man Wirkung *messen* kann.
3. Iter 21-22 sind die "schweren" technischen Hardenings (CSRF, TLS, Secrets, Rate-Limiting). Sie aendern Infrastruktur und Build — zwei Iterationen Pufferzeit fuer Fehler.
4. Iter 23 GDPR liegt nach Hardening, weil GDPR-Stories auf Audit-Log und Account-Lifecycle aufbauen.
5. Iter 24 Pentest ist der externe Reality-Check.
6. v1.0 Release in Iter 25.

**Critical Path-Variante** (falls Time-to-Market knapp):
Iter 21 + 22 + 24 reichen technisch fuer ein "v1.0 minus GDPR-Excellence". GDPR ist dann v1.0 mit dokumentiertem Restrisiko (DPIA als ToDo, RTE manuell ueber Admin-Anfrage). **Nicht empfohlen** — GDPR-Verstoesse sind teurer als 1-2 Iterationen Verzoegerung.

---

## 6. Verifikationsstrategie / Acceptance pro Iter

| Story-Cluster | Acceptance-Test | Tool |
|---------------|-----------------|------|
| SEC-01 (CSRF) | BDD-Test: HTMX-POST ohne `X-CSRF-TOKEN` -> 403; mit Token -> 200 | Cucumber + Playwright |
| SEC-03 (Headers) | Integration-Test: alle Antwort-Header gegen Erwartungsliste | Spring MockMvc |
| SEC-05 (SCA) | Maven-Build failed bei CVSS >= 7.0 | OWASP Dependency-Check |
| SEC-06 (Audit) | Application-Service-Test: jede sicherheits-relevante Methode publisht `SecurityAuditEvent` | Mockito + ApplicationEventPublisher-Spy |
| SEC-07 (Rate-Limit) | E2E: 11 schnelle Requests an `/iam/signup` -> 11. = 429 | Playwright + curl |
| SEC-09 (AdminController) | Integration-Test: ohne `realm-admin`-Rolle 403 statt 200 | Spring Security Test |
| SEC-10 (UrlSanitizer) | Unit-Test: `http://`, `file://`, IPv6-private, unbekannter Host -> Exception | JUnit |
| SEC-11 (ArchUnit) | Build-Failed bei nicht-tenant-scoped Repository-Methode | ArchUnit |
| SEC-14 (Pentest) | Externer Bericht; alle Critical/High geschlossen | manuell |
| SEC-GDPR-01 | E2E: Account-Loeschung erzeugt `AccountDeletionRequested`; alle SCS reagieren; Pruefung dass nach 24h keine PII mehr findbar | Cucumber |

---

## 7. Out-of-Scope fuer diese Roadmap

- **WAF / DDoS-Schutz auf L4/L7**: Hosting-Provider-Verantwortung (CloudFront, Cloudflare).
- **Bug-Bounty-Programm**: erst nach v1.0 sinnvoll.
- **SOC2 / ISO 27001**: nicht Ziel von v1.0; getrennte Compliance-Roadmap.
- **Hardware Security Module (HSM)** fuer Keycloak-Signing-Keys: Cloud-managed Keycloak (z.B. Keycloak-as-a-Service) erbringt das implizit; nur relevant bei Self-hosted-on-Bare-Metal.
- **Mobile-App-Specific-Security** (Cert-Pinning, Jailbreak-Detection): Travelmate ist PWA, kein native — entfaellt.

---

## 8. Zusammenfassung in einer Tabelle

| Bereich | Heutiger Stand | v1.0-Ziel | Gap-Stories |
|---------|----------------|-----------|-------------|
| Authentifizierung | OIDC + JWT, Keycloak default-config | Keycloak hardened, JWT-`sub`-mapping, RP-Logout produktionsverifiziert | SEC-08, SEC-15, SEC-17 |
| Autorisierung | TenantId-Scoping in Code | + ArchUnit-Garantie + Method-Security (`@PreAuthorize` selektiv) | SEC-11, SEC-09 |
| CSRF | disabled | enabled, HTMX-Token-Integration | SEC-01 |
| TLS | nicht konfiguriert | Reverse-Proxy-Termination, HSTS | SEC-04, SEC-03 |
| Secrets | Defaults in YAML | Pflicht-ENV ohne Fallback, Vault/SSM dokumentiert | SEC-02 |
| Logging | Standard-Spring-Log | Audit-Log + Aggregation | SEC-06 |
| Rate-Limit | keines | Gateway-Filter | SEC-07 |
| SCA | keine | Maven Dep-Check + Trivy + Renovate | SEC-05 |
| SSRF | partiell (Accommodation Import) | Strict (HTTPS-only, Re-resolve, IPv6) + Recipe-Import | SEC-10 |
| Audit/Forensik | keine | sicherheits-events persistiert | SEC-06 |
| GDPR | nur RtR-Cascade fuer Trip | RtR-Account, RtA, Verarbeitungsverzeichnis | SEC-GDPR-01..05 |
| Threat-Modeling | keine Doku | Drei BC-Threat-Model-Dokumente, Misuse-Cases pro Story | SEC-12, SEC-13 |
| Pentest | keine | externer Pentest in Iter 24 | SEC-14 |

---

## Anhang A — Diskrepanzen Doku <-> Code (zu beheben)

| Doku-Aussage | Code-Realitaet | Fix |
|--------------|----------------|-----|
| `security-rules.md`: "CSRF protection active (Spring Security default) — do not disable" | `csrf.disable()` in 4 Modulen | SEC-01 (Code anpassen) |
| ADR-0016: "HTTPS-Only — kein HTTP" | `UrlSanitizer.validate()` akzeptiert `http://` | SEC-10 (Code anpassen) |
| Arc42 §8: "TenantIdentificationFilter ueber `x-travelmate-tenant-id` Header" | Filter existiert nicht; Tenant-Resolution aus JWT-`email`-Claim | Doku korrigieren (`08-crosscutting-concepts.md`) — oder Filter implementieren falls dual-resolution gewollt |
| Arc42 §8: "Test-Profil: Security ist im `test`-Profil deaktiviert" | korrekt — aber Doku verschweigt CSRF-disable in Production-Profil | Doku transparenter machen + SEC-01 |

## Anhang B — Architecture-Decisions-Liste fuer Security

Neue ADRs, die im Verlauf der Security-Iterationen entstehen sollten (Stubs):

- **ADR-0027**: TLS-Termination und Trust-Boundary-Definition (vor SEC-04)
- **ADR-0028**: Production-Secrets-Management (vor SEC-02)
- **ADR-0029**: Audit-Log-Strategie und Retention (vor SEC-06)
- **ADR-0030**: GDPR Right-to-Erasure Cross-SCS-Propagation (vor SEC-GDPR-01) — analog zu ADR-0023 fuer Trip-Delete
- **ADR-0031**: Rate-Limiting Strategy (vor SEC-07)
