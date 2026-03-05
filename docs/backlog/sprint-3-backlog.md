# Sprint 3 Backlog - Sign-up + Trips Core

Sprint-Ziel: Vollstaendiger Sign-up-Flow (Reisegruppe + Admin), Login, Mitgliederverwaltung. Dann grundlegende Reiseplanung mit Trip-Erstellung, Einladungen und Teilnehmerverwaltung.

---

## Teil A: IAM Sign-up & Login (Voraussetzung)

### Architektur-Entscheidungen

**Keycloak Admin API Integration:**
- IAM-SCS nutzt Keycloak Admin REST API um Benutzer programmatisch anzulegen
- Dependency: `org.keycloak:keycloak-admin-client`
- Sign-up ist ein oeffentlicher Endpunkt (kein Login erforderlich)
- Nach User-Erstellung in Keycloak: Redirect zum OIDC Login Flow

**Tenant-Kontext aus JWT:**
- Custom Keycloak Mapper: tenantId als JWT-Claim (oder User-Attribute)
- Alternative: Account-Lookup via KeycloakUserId aus JWT `sub` Claim
- Jeder Request hat implizit einen Tenant-Kontext

**Sign-up Aggregate (IAM Domain - erweitert):**
- `SignUpService` (Application Service): Orchestriert Tenant + Account + Keycloak User
- `KeycloakUserService` (Port in Domain, Adapter in adapters/keycloak/): User-Provisioning
- Neues Event: `TenantCreated` (fuer Trips TravelParty-Initialisierung)

### Sprint Items - Teil A

#### S3-A01: Keycloak Admin Client Integration [IAM Adapter]
- `keycloak-admin-client` Dependency zum IAM POM
- `KeycloakUserService` Port (Interface in domain/)
- `KeycloakUserAdapter` (adapters/keycloak/): User anlegen, Passwort setzen, Rollen zuweisen
- Konfiguration: Keycloak Admin Credentials in application.yml
- TDD: Unit Tests mit Mock, Integration Test gegen Keycloak (Testcontainers oder @Profile)
- **Akzeptanz**: Keycloak-User kann programmatisch angelegt werden

#### S3-A02: Self-Service Sign-up Flow [IAM]
- Oeffentliche Sign-up-Seite: GET /iam/signup (kein Login)
- SecurityConfig: /signup und /signup/** permitAll
- Formular: Reisegruppen-Name, Vorname, Nachname, E-Mail, Passwort, Passwort-Bestaetigung
- `SignUpCommand`: tenantName, firstName, lastName, email, password
- `SignUpService`:
  1. Tenant.create(name)
  2. Keycloak-User anlegen (KeycloakUserService.createUser)
  3. Account.register(tenantId, keycloakUserId, ...)
  4. Organizer-Rolle zuweisen
  5. Events publizieren: TenantCreated, AccountRegistered
- Validierung: E-Mail eindeutig, Reisegruppen-Name eindeutig, Passwort-Regeln
- Nach Erfolg: Redirect zum Gateway Login (OIDC Flow startet automatisch)
- **Akzeptanz**: Neuer Benutzer kann Reisegruppe anlegen und ist danach eingeloggt

#### S3-A03: Login & Tenant-Kontext [IAM + Gateway]
- Nach Sign-up: Automatischer OIDC-Login via Gateway
- Tenant-Kontext ermitteln: AccountService.findByKeycloakUserId(sub) -> tenantId
- Dashboard nach Login: Reisegruppen-Uebersicht mit Mitgliedern
- Navigation: Mitglieder verwalten, Kinder hinzufuegen
- **Akzeptanz**: Nach Login sieht Benutzer seine Reisegruppe und Mitglieder

#### S3-A04: Weitere Mitglieder zur Reisegruppe einladen [IAM]
- Organisator kann neue Mitglieder per E-Mail-Adresse einladen
- `MemberInvitation` Aggregate: invitationId, tenantId, email, invitedBy, token, status
- Eingeladener erhaelt Link: /iam/signup/invite/{token}
- Registrierungsformular (vorausgefuellte E-Mail): Vorname, Nachname, Passwort
- Keycloak-User + Account werden beim bestehenden Tenant erstellt
- Event: MemberAddedToTenant
- **Akzeptanz**: Eingeladener kann sich registrieren und ist Teil der Reisegruppe

#### S3-A05: Mitglieder- und Kinder-Verwaltung [IAM]
- Dashboard: Alle Mitglieder + Kinder der Reisegruppe anzeigen
- Kinder hinzufuegen/bearbeiten (nur durch Eltern-Account)
- Bestehende Dependent-Logik nutzen, UI auf Reisegruppen-Sprache umstellen
- i18n: Alle Labels DE/EN
- **Akzeptanz**: Mitglieder sehen die Reisegruppe, Eltern verwalten ihre Kinder

---

## Teil B: Trips Core

### Tactical DDD - Trips Bounded Context

#### 1. TravelParty (Read Model / Projektion)
Lokale Projektion der IAM-Daten. Konsumiert IAM-Events ueber RabbitMQ.
- **TravelParty** (Aggregate Root): tenantId, members[], dependents[]
- **Member** (Entity): memberId, username, firstName, lastName, email
- **TravelPartyDependent** (Entity): dependentId, guardianMemberId, firstName, lastName
- Consumed Events: AccountRegistered, DependentAddedToTenant, TenantCreated

#### 2. Trip (Core Aggregate)
Zentrale Entitaet fuer die Reiseplanung.
- **Trip** (Aggregate Root): tripId, tenantId, name, description, startDate, endDate, status, organizerId, participants[]
- **TripId** (VO): UUID
- **TripName** (VO): String, max 255, not blank
- **TripDescription** (VO): String, optional, max 2000
- **DateRange** (VO): startDate, endDate (start <= end)
- **TripStatus** (Enum): PLANNING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
- **Participant** (Entity innerhalb Trip): participantId (= memberId), stayPeriod (optional)
- **StayPeriod** (VO): arrivalDate, departureDate (innerhalb Trip DateRange)
- Domain Events: TripCreated, ParticipantJoinedTrip, TripCompleted
- Invarianten:
  - Organizer muss Member der TravelParty sein
  - Participants muessen Members oder Dependents der TravelParty sein
  - StayPeriod muss innerhalb der Trip DateRange liegen
  - Status-Uebergaenge: PLANNING->CONFIRMED->IN_PROGRESS->COMPLETED, PLANNING->CANCELLED

#### 3. Invitation
Einladung eines Teilnehmers zu einer Reise.
- **Invitation** (Aggregate Root): invitationId, tripId, tenantId, inviteeId, status
- **InvitationId** (VO): UUID
- **InvitationStatus** (Enum): PENDING, ACCEPTED, DECLINED
- Invarianten:
  - Invitee muss Member der TravelParty sein
  - Keine doppelten Einladungen pro Trip+Invitee
  - Accept -> ParticipantJoinedTrip Event auf Trip

### Sprint Items - Teil B

#### S3-B01: i18n-Infrastruktur aufsetzen [Querschnittlich]
- Spring MessageSource konfigurieren (messages_de.properties, messages_en.properties)
- Thymeleaf #{...} Syntax, LocaleChangeInterceptor
- Gilt fuer IAM-SCS UND Trips-SCS
- **Akzeptanz**: Sprachwechsel DE/EN funktioniert

#### S3-B02: TravelParty - IAM-Event-Consumer [TravelParty]
- RabbitMQ Consumer fuer AccountRegistered, DependentAddedToTenant, TenantCreated
- Flyway-Migration: travel_party_member, travel_party_dependent Tabellen
- Domain + Persistence Adapter
- **Akzeptanz**: IAM-Events werden konsumiert und als lokale Projektion gespeichert

#### S3-B03: Trip erstellen [Trip Core]
- Domain: Trip Aggregate Root mit Factory Method `Trip.plan(...)`
- VOs: TripId, TripName, TripDescription, DateRange, TripStatus
- Application: TripService + CreateTripCommand
- Persistence + Flyway
- Web: TripController + Thymeleaf Templates (i18n!)
- Event: TripCreated publizieren
- **Akzeptanz**: Organisator kann Reise erstellen

#### S3-B04: Teilnehmer einladen + annehmen [Invitation + Trip]
- Invitation Aggregate + InvitationService
- Accept -> Trip.addParticipant() + ParticipantJoinedTrip Event
- Web: Einladen, Annehmen/Ablehnen per HTMX
- **Akzeptanz**: Organisator laedt ein, Teilnehmer nimmt an

#### S3-B05: Aufenthaltsdauer + Trip-Status-Lifecycle [Trip]
- StayPeriod VO, Status-Uebergaenge (confirm, start, complete, cancel)
- TripCompleted Event
- Web: StayPeriod-Formular + Status-Buttons
- **Akzeptanz**: Teilnehmer legt Aufenthaltsdauer fest, Organisator steuert Status

#### S3-B06: E2E-Tests [QA]
- Alle Use Cases (Sign-up, Login, Trip-Erstellung, Einladungen, Status) als Playwright E2E
- Happy Path + Fehlerfaelle
- **Akzeptanz**: Vollstaendige E2E-Abdeckung

#### S3-B07: Dokumentation [Docs]
- Arc42 aktualisieren (Sign-up-Flow, Trips Bausteine, Keycloak-Integration)
- README.md erstellen
- ADR: Keycloak Admin API Integration
