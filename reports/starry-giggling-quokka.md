# Plan: Iteration 19 Sprint Planning — Cross-Functional Team Approach

## Context

User-Mandat: „as a team plan the next iteration". Konkret bedeutet das:
- **Iteration 19** ist das Ziel (target version `v0.20.0`, theme „Visibility & Integrity")
- Aktueller Plan ist mit ~10 Stories **deutlich überladen** für ein Solo-Side-Project mit 1–3h/Woche Marketing+Implementation-Budget
- Team-Konstellation, die der User aktiviert hat: **Architect + Requirements Engineer**, **UX Designer**, **QA Engineer**, **DevOps + Security** — alle 4 Disziplinen sollen substantiv beitragen
- Detail-Tiefe: **Sprint-Plan mit Scope-Cut auf 3 Stories**, der Rest wird sauber in spätere Iterationen verschoben

**Realismus-Check**: Bei 1–3h/Woche × 3-Monats-Sprint = 12–36h verfügbare Coding-Zeit. Das passt zu max. 3 Stories à mittlerer Größe oder 1 große + 2 kleine.

**Theme-Refresh-Vorschlag**: Bisher „Visibility & Integrity" (Recipe CRUD + Observability + Outbox-Slice). Mit Sprint-Cut wird das zu „**Demo-Hardening + Phase-0-GTM**" — passt zur tatsächlich gewählten 3-Story-Selektion.

---

## Empfohlener Sprint-Cut

| Story | Größe | Begründung |
|---|---|---|
| **S19-INVITE-EXISTING** | M (5–15h) | Demo-Blocker am 2026-04-30 entdeckt — invitee-without-keycloak-account landet auf Login-Sackgasse. Story ist bereits voll spezifiziert mit 7 ACs und Code-Referenzen. Hohe User-Impact, klar abgrenzbar. |
| **S19-LANDING-WAITLIST** | M (10–20h) | Business-strategischer Trigger für Phase-0-Validation (siehe Business-Strategy-Doc §8). Ohne Wait-List kein Sign-up-Funnel, keine Customer-Interview-Kandidaten skalierbar werben. Aktuell nur als Stub im Backlog. |
| **S19-UI-POLISH-DEMO-FEEDBACK** | S (2–4h) | Logo-BG-Fix bereits in Hotfix gepusht. Verbleibend: Accommodation-Poll-i18n-Audit + Edit/Cancel-Button-Sizing. Hoher visueller Impact bei minimalem Aufwand. |

**Total: ~17–39h** → passt in 3-Monats-Sprint mit 1–3h/Woche-Capacity.

### Out-of-Scope (verschoben)

| Story | Verschiebt nach | Begründung |
|---|---|---|
| Recipe edit | Iter-20 | Solides Feature-Add, aber kein direkter Cash-Flow oder Demo-Critical |
| Recipe delete | Iter-20 | Wie oben |
| Recipe import (SSRF-aware) | Iter-21 | Größer als gedacht — SSRF-Adapter-Pattern ist eigenes Threat-Modell-Thema |
| Observability baseline (Micrometer/Prometheus) | Iter-20 | Wichtig für Skalierung, aber bei <10 User noch nicht kritisch |
| Centralized roadmap decision für logs/metrics/traces | Iter-20 | Begleit-Task zu Observability |
| Transactional outbox first slice | Iter-22 | Großer Architektur-Eingriff — gehört in Production-Hardening-Phase |
| Event versioning + naming conventions executable through tests | Iter-22 | Ähnlich — ist Engineering-Excellence, kein User-Wert |
| Documentation drift corrections | Iter-19 (continuous) | Lässt sich lose neben den 3 Hauptstories laufen, kein Sprint-Slot |
| **S19-INVITE-EXISTING** + **S19-UI-POLISH-DEMO-FEEDBACK** + **S19-LANDING-WAITLIST** | **Iter-19 (in scope)** | Die 3 ausgewählten |

---

## Team-Workshop-Choreographie

**Sequential statt parallel** — jeder Agent baut auf dem Output der vorigen auf. Vorteile:
- Kein Merge-Konflikt auf gemeinsamen Artefakten
- UX kann die ACs vom RE als Anker verwenden
- QA kann auf konkreten Wireframes Test-Scenarios schreiben
- Security kann mit vollem Kontext reviewen

### Session 1 — Architect + Requirements Engineer (co-design)

Dauer: ~30–45 Min Agent-Invocation
Agenten: `architect-agent` (DDD/Aggregate-Side) + `requirements-engineer-agent` (User-Story-Side)

Output pro Story:
- DDD-Bewertung: Welche neuen Aggregate / Events nötig?
- Hexagonal-Konformitäts-Check
- INVEST-konforme User Story (re-write wo nötig)
- Acceptance Criteria im Gherkin-Stil (Given-When-Then)
- Edge-Cases + Error-Scenarios
- Technical-Notes mit Code-Anchor (file:line)

Konkret pro Story:

**S19-INVITE-EXISTING**:
- Architect: KeycloakAdminClient-Erweiterung als neues Adapter-Interface, ExistingAccountInviteRouter als pure-domain-Service
- RE: bestehende ACs (siehe iter-19-plan.md:78-119) sind schon gut — ggf. Edge-Cases ergänzen (Was wenn Keycloak-API timeoutet? Was wenn Mail #1 schon versendet ist und Mail #2 dann fehlschlägt?)

**S19-LANDING-WAITLIST**:
- Architect: Wait-List ist read-model in IAM (oder neues SCS für Marketing?), Mailerlite als externer Adapter
- RE: ACs schreiben (Stub bisher!) — Subscriber-Email + Consent + Double-Opt-In + Plausible-Event-Tracking + DSGVO-Loeschpfad

**S19-UI-POLISH-DEMO-FEEDBACK**:
- Architect: rein UI/Theme-Änderungen, kein Domain-Impact
- RE: ACs sind bereits ausführlich (iter-19-plan.md S19-UI-POLISH-Sektion) — nur Audit der i18n-Keys und Button-Class-Choice

### Session 2 — UX Designer (Wireframes + Journey-Maps)

Dauer: ~20–30 Min
Agent: `ux-designer-agent`

Output pro user-facing Story:

**S19-INVITE-EXISTING**:
- Journey-Map: Invitee-without-Account → Mail #2 erhalten → Password-Setup-Link klicken → Keycloak-PW-Form → Travelmate-Login → Trip-Acceptance
- Mail-Template-Mockup für „Password setup" (Re-Login-Notice nicht UX-relevant, da Plain-Text-orientiert)

**S19-LANDING-WAITLIST**:
- Wireframe: Hero + Headline + Pain-Statement + Wait-List-Form + Privacy-Hinweis + Beta-Status
- Mobile-First-Layout (PWA-Pattern wie der Rest von Travelmate)
- HTMX-Form-Submit-Pattern (POST → Erfolgs-Toast → Inline-Replacement)

**S19-UI-POLISH-DEMO-FEEDBACK**:
- Edit/Cancel-Button-Side-by-Side-Vergleich vor/nach Fix (Pico-Class-Vorschlag)
- i18n-Key-Mapping-Tabelle für Accommodation-Poll-Create-Page

### Session 3 — QA Engineer (BDD-Scenarios + E2E-Test-Plan)

Dauer: ~30 Min
Agent: `qs-engineer-agent`

Output pro Story:
- Gherkin-Feature-File pro Story
- Liste aller Scenarios mit Given-When-Then-Steps
- Identifikation: Unit-Test vs Integration-Test vs E2E-Test
- Mocking-Strategie pro Test (Keycloak-Stub? Stripe-Mock? Mailerlite-Stub?)

Konkret:

**S19-INVITE-EXISTING**:
- Scenario 1: New account → existing skip-behavior preserved
- Scenario 2: Existing account, no Keycloak password → Password-Setup-Branch
- Scenario 3: Existing account, has Keycloak password → Re-Login-Notice-Branch
- Scenario 4: Keycloak-API-Timeout → Retry behavior
- E2E-Scenario: Full happy-path mit echtem Keycloak

**S19-LANDING-WAITLIST**:
- Scenario 1: Sign-up with valid email → email in Mailerlite, confirmation page shown
- Scenario 2: Sign-up with invalid email → form error, no Mailerlite call
- Scenario 3: Sign-up with already-registered email → graceful idempotent response
- Scenario 4: Mailerlite-API-Timeout → email in local fallback table
- Scenario 5: DSGVO Löschanfrage → email aus Mailerlite + lokaler Tabelle entfernt

**S19-UI-POLISH-DEMO-FEEDBACK**:
- Visual-Regression-Test (Screenshot-Compare) — wenn vorhanden, sonst manuell verifiziert
- Locale-Switch-Test: EN-Locale-Aufruf zeigt nur englische Labels

### Session 4 — DevOps + Security (Threat-Model + Deployment-Impact)

Dauer: ~20–30 Min
Agent: `devops-engineer-agent` + `security-expert-agent`

Output:

**Threat-Model pro Story**:

**S19-INVITE-EXISTING**:
- Spoofing: Kann ein Angreifer Password-Setup-Mails an fremde User triggern? → ACL: nur durch ExternalUserInvitedToTrip-Event
- Tampering: Kann der Redirect-URI in der Password-Setup-Mail manipuliert werden? → Whitelist über Keycloak Valid-Redirect-URIs
- Repudiation: Audit-Log für „Password-Setup-Link sent"?
- Information Disclosure: Mail #2 enthält keine sensiblen Daten außer Email/Trip-Name

**S19-LANDING-WAITLIST**:
- Spoofing: Bot-Sign-up-Schutz? → reCAPTCHA / Cloudflare-Turnstile / Honeypot-Field?
- Tampering: SQL-Injection im Email-Feld → Spring-JPA-Schutz reicht
- Information Disclosure: Mailerlite ist US-Hosted → Standard Contractual Clauses + Privacy-Erklärung-Update
- DoS: Massen-Sign-up-Attacks → Rate-Limit per IP

**S19-UI-POLISH-DEMO-FEEDBACK**:
- Niedriges Threat-Profile, aber: i18n-Keys validieren, dass keine HTML-Injection durchrutscht (wenn User-input in Templates)

**Deployment-Impact**:
- Keine neuen Container-Builds für UI-Polish
- LANDING-WAITLIST ggf. neue Mailerlite-Env-Vars in `.env.demo`
- INVITE-EXISTING ggf. neue Keycloak-Admin-Client-Permissions (kann existing admin-cli token reusen?)

---

## Plan-Outcome — Wie der Iter-19-Plan nach dem Workshop aussieht

### Format der Iter-19-Update

Datei: `docs/backlog/iteration-19-plan.md` wird komplett restrukturiert:

```markdown
# Iteration 19 — Demo-Hardening + Phase-0-GTM

**Target Version**: v0.20.0
**Status**: PLANNED
**Sprint-Duration**: 3 Monate (1-3h/Woche Solo-Pace)
**In-Scope**: 3 Stories (~17-39h Total)

> Note: Originally scoped as "Visibility & Integrity" with ~10 stories.
> After 2026-05-XX team planning, scope was cut to 3 demo-blocking and
> business-strategy-driven stories. Recipe CRUD, observability, outbox,
> event versioning moved to iter-20+. See "Out-of-Scope" section below.

## In-Scope Stories

### S19-INVITE-EXISTING (M, 5-15h)
[full story detail — already exists, augmented by team session output]

### S19-LANDING-WAITLIST (M, 10-20h)
[full story detail — newly written by RE based on stub]

### S19-UI-POLISH-DEMO-FEEDBACK (S, 2-4h)
[existing story detail — minor augmentations]

## Out-of-Scope (moved to later iterations)
[5-row table with target iteration + reason]

## Sprint Acceptance
- 3 in-scope stories meet their AC end-to-end
- Demo on travelmate-demo.de shows the polish improvements
- Wait-list captures real signups feeding GTM Phase 0
- Existing-account invitations no longer dead-end
```

### Per-Story Detail-Format

Jede der 3 Stories endet mit Sub-Sektionen, die der Team-Session-Output enthalten:

```markdown
## Story Detail: S19-XYZ

### User Story (RE)
[INVEST-konforme User-Story]

### Acceptance Criteria (RE)
[Gherkin Given-When-Then]

### DDD/Architecture Notes (Architect)
[Aggregate-Impact, neue Events, Hexagonal-Konformität]

### UX Wireframes (UX Designer)
[Wireframe-Beschreibung oder Mermaid/PlantUML-Diagramm]

### BDD Scenarios (QA)
[Gherkin-Feature-File-Block, mit Test-Pyramid-Note]

### Threat Model (Security)
[STRIDE-Kategorien mit Mitigation]

### Deployment Notes (DevOps)
[Env-Var-Änderungen, Container-Restart-Bedarf, Migration-Notes]

### Out of Scope (RE)
[explizite Abgrenzungen]
```

---

## Implementation Steps

### Step 1: Team-Sessions sequenziell durchlaufen

Pro Session:
1. Agent invoken mit konkretem Story-Kontext
2. Output sammeln (in temporären File oder Konversation)
3. Output validieren — passen ACs zur Story? Sind Edge-Cases abgedeckt?
4. Anpassungen einarbeiten

Reihenfolge:
- **Session 1** (Architect + RE): ~45 Min Agent-Time → 3 angereicherte Story-Definitions
- **Session 2** (UX): ~30 Min → 3 Wireframes/Journey-Maps
- **Session 3** (QA): ~30 Min → 3 Gherkin-Feature-Files
- **Session 4** (DevOps + Security): ~30 Min → 3 Threat-Models + Deployment-Notes

Zwischen den Sessions: 5 Min Konsolidierung — bringt jede neue Sicht in die Story-Sektion ein.

### Step 2: iter-19-plan.md restrukturieren

Komplette Re-Write der Datei nach dem oben skizzierten Format. Die 3 In-Scope-Stories bekommen Volldetails, die Out-of-Scope-Stories nur noch Tabellen-Eintrag mit Verschiebe-Ziel.

### Step 3: Out-of-Scope-Stories in target-iter-Plans migrieren

- iter-20-plan.md erhält:
  - Recipe edit
  - Recipe delete
  - Observability baseline + log/metric/trace decision
- iter-21-plan.md erhält:
  - Recipe import (SSRF-aware)
- iter-22-plan.md erhält:
  - Transactional outbox first slice
  - Event versioning executable through tests

Nur als Stubs mit Story-ID + 1-Zeilen-Beschreibung, keine Volldetails — das wird in den jeweiligen Sprint-Plannings nachgeholt.

### Step 4: Commit-Strategie

Drei Commits, weil getrennte Concerns:

1. `docs(backlog): refocus iteration 19 on demo hardening + GTM phase 0`
   — iter-19-plan.md komplett-Update mit den 3 In-Scope-Stories und Out-of-Scope-Tabelle
2. `docs(backlog): migrate out-of-scope iteration 19 stories to iter-20/21/22`
   — iter-20/21/22 plan.md jeweils Stubs ergänzen
3. (optional, falls Team-Output zu groß) — separates Doc unter `docs/backlog/iteration-19-team-notes.md` mit den vollständigen Workshop-Outputs

### Step 5: Push

Standard `git push origin main`. Workflow triggert (nur Doc-Updates, keine VM-Auswirkungen).

---

## Kritische Dateien (Übersicht)

| Datei | Zustand | Rolle |
|---|---|---|
| `docs/backlog/iteration-19-plan.md` | EDIT | Komplett restrukturiert: 3 In-Scope-Stories mit Team-Output, Out-of-Scope-Tabelle |
| `docs/backlog/iteration-20-plan.md` | EDIT | Recipe edit/delete + Observability als Stubs |
| `docs/backlog/iteration-21-plan.md` | EDIT | Recipe import (SSRF-aware) als Stub |
| `docs/backlog/iteration-22-plan.md` | EDIT | Outbox + Event versioning als Stubs |

---

## Verifikation

Plan ist erfolgreich, wenn:

1. **Sprint-Realismus**: Die 3 In-Scope-Stories summieren sich auf ≤39h, was in 3 Monaten × 3h/Woche × 4 Wochen = 36h realistisch ist (mit leichtem Stretch bei niedrigerer wöchentlicher Capacity)
2. **Team-Coverage**: Jede der 3 Stories hat mindestens je einen Beitrag von Architect, RE, UX, QA, DevOps, Security
3. **Klare Out-of-Scope-Begründung**: Für jede verschobene Story ist Ziel-Iteration und Begründung dokumentiert
4. **Backlog-Integrität**: Out-of-Scope-Stories tauchen in iter-20/21/22 als Stubs auf, nicht „verloren" im Backlog
5. **Lesbarkeit**: User kann den überarbeiteten iter-19-plan.md in 15 Min vollständig lesen und versteht den Sprint-Scope
6. **Demo-Verbindung**: Jede der 3 Stories adressiert direkt entweder einen Demo-Test-Befund (UI-Polish, INVITE-EXISTING) oder einen Business-Strategy-Trigger (LANDING-WAITLIST)

---

## Was bewusst NICHT Teil des Sprint-Plans ist

- **Code-Implementierung der 3 Stories**: Planning ≠ Doing. Die Implementierung läuft anschließend in der Sprint-Periode mit normalen Story-Pull-und-Push-Pattern.
- **Vollausarbeitung der Out-of-Scope-Stories**: Die Stubs in iter-20/21/22 bleiben Stubs, bis ihr eigenes Sprint-Planning sie aufnimmt.
- **Theme-Reframing über Iter-19 hinaus**: Iter-20/21/22-Themen bleiben unverändert, nur die Stories darin verschieben sich.
- **Time-Tracking pro Story**: Die 5-15h / 10-20h / 2-4h-Schätzungen sind grob, kein Estimation-Theater. Bei Solopreneur-Pace ist Effort-Tracking weniger wertvoll als „done"-Tracking.
