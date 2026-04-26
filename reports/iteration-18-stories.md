# Iteration 18 — Story Candidates

**Datum**: 2026-04-26
**Target Version**: v0.18.0
**Input**: Product Backlog, Iteration-17-Follow-ups (glimmering-riding-canyon.md:240-246), Codebase-Analyse (v0.17.0, 1064 Tests)

**Status**: PLANNING

---

## Kontext: Wo stehen wir nach Iteration 17?

Die letzten drei Iterationen (15-17) haben den Core-Domain-Kern von Trips massiv erweitert:
Buchungsworkflow, Amenity-Modell, Trip-Lifecycle (Bearbeiten/Löschen/Kaskade), Küchendienst.

Iteration 18 hat drei Kandidaten-Cluster aus dem Backlog:

1. **E-IAM-05: Multi-Organizer** — explizit als Follow-up aus It-17 genannt; `IdentityProviderService.assignRole()` und `RoleAssignedToUser`/`RoleUnassignedFromUser`-Events existieren bereits in `travelmate-common`
2. **E-TRIPS-05: Rezept-Import** — seit Iteration 9 mehrfach zurückgestellt; Should-Priorität; Jsoup-Pattern aus S10-A (Accommodation URL Import) ist etabliert
3. **E-IAM-06: E-Mail-Benachrichtigungen** — US-IAM-050 (Trip-Einladung per E-Mail) ist Must; Spring Mail + Mailpit-Infrastruktur im Docker Compose vorhanden
4. **Cross-cutting: GlobalExceptionHandler-Deduplizierung** — ebenfalls It-17-Follow-up; die drei Handler in IAM, Trips und Expense sind funktional auseinanderdriftet

---

## Epic-Cluster A: E-IAM-05 — Multi-Organizer Support

### S18-A01: Organizer-Rolle vergeben (US-IAM-040)
**Priorität**: Should | **Größe**: M

Als Organisator möchte ich einem anderen Mitglied meiner Reisepartei die Organizer-Rolle erteilen, damit er ebenfalls Reisen verwalten und Belege prüfen kann.

#### Acceptance Criteria
- **Given** ich bin Organisator und öffne die Mitgliederliste meiner Reisepartei,
  **When** ich bei einem Mitglied "Organizer-Rolle vergeben" klicke und bestätige,
  **Then** erhält das Mitglied die `organizer`-Realm-Rolle in Keycloak und das Ereignis `RoleAssignedToUser` wird publiziert.
- **Given** das Mitglied hat bereits die Organizer-Rolle,
  **When** ich die Aktion erneut aufrufe,
  **Then** sehe ich eine Fehlermeldung "Mitglied ist bereits Organisator."
- **Given** ich bin kein Organisator,
  **When** ich die Mitgliederliste aufrufe,
  **Then** ist die Schaltfläche "Organizer-Rolle vergeben" nicht sichtbar.

#### Technical Notes
- Bounded Context: IAM
- Aggregate: Account (kein neues Feld nötig — Keycloak ist Source of Truth für Rollen)
- Domain Events: `RoleAssignedToUser` (bereits in `travelmate-common`)
- Routing Key: `RoutingKeys.ROLE_ASSIGNED_TO_USER` — prüfen, ob schon vorhanden, sonst ergänzen
- `IdentityProviderService.assignRole(KeycloakUserId, "organizer")` — Port existiert, Keycloak-Adapter implementiert
- Neuer Command: `AssignOrganizerRoleCommand(UUID accountId, UUID actorAccountId)`
- UI: Schaltfläche in `dashboard/members.html` (HTMX POST, nur für Organizer sichtbar); Bestätigungs-Dialog
- Multi-Tenancy: Rolle darf nur innerhalb desselben TenantId vergeben werden — Guard in ApplicationService
- Flyway: keine Migration nötig

---

### S18-A02: Organizer-Rolle entziehen (US-IAM-041)
**Priorität**: Should | **Größe**: S

Als Organisator möchte ich einem anderen Mitglied die Organizer-Rolle entziehen, damit es wieder nur als Teilnehmer agiert.

#### Acceptance Criteria
- **Given** es gibt mindestens zwei Organisatoren,
  **When** ich einem Mitglied die Organizer-Rolle entziehe,
  **Then** wird die Rolle in Keycloak entfernt und `RoleUnassignedFromUser` publiziert.
- **Given** das Mitglied ist der einzige verbliebene Organisator der Reisepartei,
  **When** ich die Aktion versuche,
  **Then** erhalte ich den Fehler "Mindestens ein Organisator ist erforderlich." — die Aktion wird blockiert.
- **Given** ich möchte meine eigene Organizer-Rolle entziehen,
  **When** ich die Aktion versuche,
  **Then** ist die Schaltfläche für mich selbst deaktiviert (Self-Revocation nicht erlaubt).

#### Technical Notes
- Bounded Context: IAM
- Aggregate: Account + AccountRepository (Methode `countOrganizersInTenant(TenantId)` nötig)
- Domain Events: `RoleUnassignedFromUser` (bereits in `travelmate-common`)
- Port-Erweiterung: `IdentityProviderService.revokeRole(KeycloakUserId, String)` — prüfen ob vorhanden, ggf. ergänzen
- Invariante im ApplicationService: `if (organizerCount <= 1) throw BusinessRuleViolationException`
- UI: Schaltfläche in `dashboard/members.html` neben "Rolle vergeben"; für Selbst-Revocation disabled-Attribute
- Flyway: keine Migration nötig
- Dependency: S18-A01 muss abgeschlossen sein (gleiche UI-Sektion)

---

## Epic-Cluster B: E-TRIPS-05 — Rezept-Import aus URL

### S18-B01: Rezept aus URL importieren (US-TRIPS-041)
**Priorität**: Should | **Größe**: L

Als Mitglied möchte ich ein Rezept per URL (z.B. von chefkoch.de) importieren, damit ich Zutaten nicht manuell eintippen muss.

#### Acceptance Criteria
- **Given** ich bin auf der Rezept-Seite und gebe eine URL ein und klicke "Importieren",
  **When** die URL erreichbar ist und strukturierte Daten (JSON-LD `Recipe`, `og:title`) enthält,
  **Then** wird ein Vorschau-Formular mit vorausgefüllten Feldern (Name, Portionen, Zutaten) angezeigt.
- **Given** das Vorschau-Formular wird angezeigt,
  **When** ich die vorgeschlagenen Werte prüfe, ggf. korrigiere und auf "Speichern" klicke,
  **Then** wird das Rezept unter meinem TenantId gespeichert.
- **Given** die URL ist nicht erreichbar oder enthält keine auswertbaren Daten,
  **When** ich auf "Importieren" klicke,
  **Then** erhalte ich eine Hinweismeldung "Keine Rezeptdaten gefunden" und das leere manuelle Formular wird geöffnet.
- **Given** die URL zeigt auf eine RFC-1918-Adresse oder Loopback-Adresse (SSRF-Schutz),
  **When** ich auf "Importieren" klicke,
  **Then** wird die Anfrage mit Fehler "Ungültige URL" abgelehnt.

#### Technical Notes
- Bounded Context: Trips
- Aggregate: Recipe (kein neues Feld; bestehende Recipe.create() Factory-Methode wird nach Import genutzt)
- Domain Events: keine neuen Events; Rezept-Erstellung publiziert kein Downstream-Event
- Neuer Port: `RecipeImportPort` in `domain/recipe/` — `RecipeImportResult parse(URI url)`
- Neuer Adapter: `HtmlRecipeImportAdapter` in `adapters/integration/` — Jsoup + JSON-LD Schema.org/Recipe-Parser
  - Extraktion: `name`, `recipeYield` (Portionen), `recipeIngredient[]` (Name + Menge als Text-Parsing)
  - Timeout: 5s; keine Redirects auf private Adressen
  - SSRF-Guard: RFC-1918-Blocklist identisch zu `HtmlAccommodationImportAdapter`
- UI-Flow (Import-Pipeline-Pattern aus S10-A):
  - `GET /recipes/import` → Import-Formular (URL-Eingabe)
  - `POST /recipes/import` → ruft Adapter, rendert Vorschau-Formular (= normales create-Formular, vorausgefüllt)
  - `POST /recipes` → speichert (bestehender Endpunkt, keine Änderung)
- Template: `recipe/import.html` (Eingabe) + Vorschau füllt `recipe/form.html` (Partial)
- Kein neues Flyway-Script nötig (Recipe-Schema unverändert)
- Dependency: keine (Recipe-Aggregate vollständig implementiert)

---

## Epic-Cluster C: E-IAM-06 — E-Mail-Benachrichtigung bei Einladung

### S18-C01: E-Mail-Benachrichtigung bei Trip-Einladung (US-IAM-050)
**Priorität**: Must | **Größe**: M

Als eingeladene Reisepartei möchte ich bei einer neuen Trip-Einladung eine E-Mail erhalten, damit ich nicht aktiv in der App nachschauen muss.

#### Acceptance Criteria
- **Given** ein Organisator lädt meine Reisepartei zu einer Reise ein,
  **When** die Einladung erstellt wird,
  **Then** erhalten alle Account-Mitglieder der eingeladenen Reisepartei eine E-Mail mit Reisename, Einladendem und einem Direktlink zur Einladungsansicht.
- **Given** die E-Mail-Versandinfrastruktur ist nicht erreichbar (Mailpit down),
  **When** die Einladung erstellt wird,
  **Then** wird die Einladung trotzdem gespeichert (Mail-Fehler darf Hauptfluss nicht blockieren) und der Fehler wird geloggt.
- **Given** meine Reisepartei hat kein Account-Mitglied (nur Dependents),
  **When** eine Einladung erstellt wird,
  **Then** wird keine E-Mail verschickt (kein Empfänger).

#### Technical Notes
- Bounded Context: Trips publiziert `InvitationCreated`-Event (RabbitMQ); IAM konsumiert und versendet
- Alternativansatz (einfacher, bevorzugt für MVP): Trips SCS sendet E-Mail direkt via `EmailNotificationPort` (kein Cross-SCS-Event nötig) — entscheiden vor Implementierung
  - Direktversand in Trips: `InvitationEmailAdapter` in `adapters/messaging/`, Spring Mail Starter, `EmailNotificationPort` in `domain/invitation/`
  - Konfiguration: `spring.mail.host`, `spring.mail.port` (Mailpit: `localhost:1025` in dev)
- Template: Thymeleaf-HTML-E-Mail-Template `mail/invitation.html` (Reisename, Organisator, Direktlink)
- i18n: Betreff und Body in `messages.properties`
- Einladungslink: `{gateway.base-url}/trips/{tripId}/invitations/{invitationId}` — via Spring Config Property
- Fehlerbehandlung: E-Mail-Versand in `@TransactionalEventListener(AFTER_COMMIT)` oder separatem `@Async` Block, sodass Fehler die Transaktion nicht rollbackt
- Flyway: keine Migration
- Multi-Tenancy: E-Mail-Empfänger aus `TravelPartyRepository.findAccountsByTenantId(invitedTenantId)`

---

## Cross-cutting Concern: GlobalExceptionHandler-Deduplizierung

### S18-D01: GlobalExceptionHandler vereinheitlichen
**Priorität**: Should | **Größe**: S

Als Entwickler möchte ich, dass alle drei SCS einen identisch strukturierten `GlobalExceptionHandler` haben, damit Bugfixes und Erweiterungen nicht dreimal eingepflegt werden müssen.

#### Acceptance Criteria
- **Given** ein IAM-Controller wirft eine `BusinessRuleViolationException`,
  **When** der Handler greift,
  **Then** wird die i18n-aufgelöste Fehlermeldung zurückgegeben (bisher fehlt IAM die `MessageSource`-basierte Auflösung).
- **Given** ein IAM-Controller antwortet mit `ResponseStatusException`,
  **When** der Handler greift,
  **Then** wird der Reason korrekt weitergegeben (bisher fehlt IAM der `ResponseStatusException`-Handler).
- **Given** eine `RuntimeException` tritt auf,
  **When** der Handler greift,
  **Then** ist Verhalten in allen drei SCS identisch (HTTP 500, Generic-Error-Message, HTMX-Toast).
- **Given** alle drei Handler sind angeglichen,
  **When** `./mvnw clean verify` läuft,
  **Then** sind alle 1064+ Tests grün.

#### Technical Notes
- Keine neuen Abstraktionsebenen, kein gemeinsames Modul — pure Angleichung der drei Handler-Klassen
- IAM fehlen gegenüber Trips und Expense: `MessageSource`-Injection, `ResponseStatusException`-Handler
- Expense hat zusätzlich `MaxUploadSizeExceededException` — bleibt expense-spezifisch, kein Backport
- Trips-Handler nutzt bereits `resolveMessage()` mit MessageSource; IAM-Handler wirft rohen Exception-Key zurück
- Identische Methoden: `triggerErrorToast()`, `isHtmxRequest()`, `escapeJson()` — Ziel: exakt gleicher Code in allen drei Klassen (kein Shared-Util, da SCS-Isolierung gewahrt bleiben soll)
- Betroffene Dateien:
  - `travelmate-iam/.../adapters/web/GlobalExceptionHandler.java`
  - `travelmate-trips/.../adapters/web/GlobalExceptionHandler.java` (Referenz, keine Änderung nötig)
  - `travelmate-expense/.../adapters/web/GlobalExceptionHandler.java` (nur `resolveMessage` für BRV nachrüsten, da dort der rohe Key ohne i18n-Auflösung zurückgegeben wird)
- `GlobalExceptionHandlerTest` in IAM um `ResponseStatusException`-Fall und MessageSource-Auflösung erweitern

---

## Implementierungsreihenfolge (falls alle 5 Stories im Scope)

| Reihenfolge | Story | Begründung |
|-------------|-------|------------|
| 1 | S18-D01 (Handler) | XS/S, kein Risiko, sofortiger Mehrwert für alle weiteren Stories |
| 2 | S18-A01 (Rolle vergeben) | Unabhängig; etabliert das IAM-Pattern für A02 |
| 3 | S18-A02 (Rolle entziehen) | Baut auf A01 auf (gleiche UI-Sektion) |
| 4 | S18-C01 (Einladungs-E-Mail) | Unabhängig von A-Stories; Spring-Mail-Setup ggf. parallel |
| 5 | S18-B01 (Rezept-Import) | Größte Story; Jsoup-Pattern aus S10-A übertragen |

**Parallelisierung:** S18-C01 und S18-B01 können parallel laufen, sobald S18-D01 abgeschlossen ist.

---

## Empfohlener Iterationsscope

**Empfehlung: S18-D01 + S18-A01 + S18-A02 + S18-C01** (XS + M + S + M = ca. 2,5 Sprint-Punkte)

### Begründung

**S18-D01 (GlobalExceptionHandler)** ist eine obligatorische Hygiene-Story, die jeden anderen Test-Run sicherer macht. Sie ist in 2-3 Stunden erledigt und reduziert Rauschen in allen nachfolgenden PR-Reviews. Kein Risiko.

**S18-A01 + S18-A02 (Multi-Organizer)** bilden ein kohärentes Mini-Epic: beide Stories teilen die gleiche UI-Seite, den gleichen Keycloak-Adapter-Aufruf und die bereits existierenden Events in `travelmate-common`. `assignRole()` ist im Port und Adapter implementiert — der IAM-Implementierungsaufwand ist überschaubar (M + S). Das Feature ist von Iteration 16 memory als "explizit zurückgestellt" markiert und passt gut als IAM-Iteration.

**S18-C01 (Einladungs-E-Mail)** ist Must-Priorität und infrastrukturell bereits vorbereitet (Mailpit im Docker Compose, Spring Mail verfügbar). Die Einladungs-E-Mail ist ein oft genanntes User-Feedback-Item und schließt eine offensichtliche Lücke im Onboarding-Flow.

**S18-B01 (Rezept-Import) wird auf Iteration 19 verschoben**, weil:
- Die L-Größe allein schon das Budget einer Iteration füllt, wenn A01+A02+C01 drin sind.
- Das Jsoup-Pattern ist aus S10-A bekannt und nicht risikoreich; es läuft nicht weg.
- Ein eigenständiger "Recipe-Import"-Sprint ist thematisch sauberer als ein gemischter IAM+Trips-Sprint.
- Die Iteration bleibt so fokussiert auf IAM-Erweiterungen + Notification-Fundament.

### Scope-Zusammenfassung

| Story | Epic | Priorität | Größe | BC |
|-------|------|-----------|-------|----|
| S18-D01 | Cross-cutting | Should | S | IAM + Trips + Expense |
| S18-A01 | E-IAM-05 | Should | M | IAM |
| S18-A02 | E-IAM-05 | Should | S | IAM |
| S18-C01 | E-IAM-06 | Must | M | Trips (+ IAM optional) |

**Zurückgestellt:** S18-B01 (Rezept-Import) → Iteration 19

---

## Follow-up (Out of Scope Iteration 18)

- **S19-B01** — Rezept-Import aus URL (US-TRIPS-041): L, Should; Iteration 19
- **Küchendienst-Fairness-Analytik**: Wer hat wie oft Küchendienst gehabt? Read-Model über alle MealSlots; Could; Iteration 19+
- **US-EXP-005** (Beleg bearbeiten) + **US-EXP-010** (Beleg kategorisieren): Should, S; könnten in Iteration 19 als Expense-Pflege-Sprint kombiniert werden
- **US-IAM-051** (SMS-Benachrichtigung): Could; Abhängigkeit von Twilio/SMS-Gateway; zurückgestellt
