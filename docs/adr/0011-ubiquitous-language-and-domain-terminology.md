# ADR-0011: Ubiquitous Language und Domain-Terminologie

## Status

Accepted

## Context

Bei der Entwicklung des Sign-up-Flows und der Einladungsfunktion wurde klar, dass die bisherige Benennung "Reisegruppe" fuer den Tenant-Begriff irreführend ist. Ein Tenant repraesentiert nicht die Reisegruppe eines Trips, sondern eine zusammengehoerige Einheit (Einzelperson, Paar, Familie), die sich registriert und gemeinsam reist.

Die eigentliche "Reisegruppe" entsteht erst durch die Teilnehmer einer konkreten Reise (Trip). Einladungen gehoeren daher zum Trip-Kontext (Core Domain), nicht zum IAM-Kontext (Supporting Subdomain).

## Decision

Wir fuehren eine verbindliche Ubiquitous Language ein, die in UI, Dokumentation und fachlicher Kommunikation konsistent verwendet wird. Technische Klassennamen (Tenant, Account, Dependent) bleiben im Code bestehen, da sie zur Supporting Subdomain IAM gehoeren.

### Begriffsmatrix

| Fachbegriff (DE)    | Fachbegriff (EN) | Technisch (Code) | Kontext | Beschreibung |
|---------------------|------------------|-------------------|---------|-------------|
| **Reisepartei**     | Travel Party     | Tenant            | IAM     | Registrierungseinheit: Einzelperson, Paar oder Familie |
| **Mitglied**        | Member           | Account           | IAM     | Person mit eigenem Login, plant aktiv mit |
| **Mitreisende(r)**  | Companion        | Dependent         | IAM     | Person ohne Login, reist mit (Kind, Partner der nicht plant) |
| **Reise**           | Trip             | Trip              | Trips   | Ein konkreter Urlaub/Event mit Zeitraum |
| **Reisegruppe**     | Travel Group     | —                 | Trips   | Alle Teilnehmer einer Reise (entsteht durch Einladungen) |
| **Einladung**       | Invitation       | Invitation        | Trips   | Einladung einer Reisepartei zu einer Reise |
| **Organisator**     | Organizer        | —                 | Trips   | Mitglied das eine Reise erstellt und verwaltet |
| **Teilnehmer**      | Participant      | Participant       | Trips   | Reisepartei die an einer Reise teilnimmt |

### Abgrenzung der Kontexte

**IAM (Supporting Subdomain):**
- Verwaltet Reiseparteien (Tenants), Mitglieder (Accounts) und Mitreisende (Dependents)
- Registrierung, Authentifizierung, Profilverwaltung
- Keine Einladungslogik — diese gehoert zur Core Domain

**Trips (Core Domain):**
- Verwaltet Reisen, Einladungen, Teilnehmer
- Die "Reisegruppe" ist die Menge aller Teilnehmer einer Reise
- Einladungen richten sich an externe Personen/Reiseparteien
- Eingeladene muessen sich ggf. erst registrieren (IAM), bevor sie einer Reise beitreten

### Keycloak-Strategie

- **Registrierung**: Custom-Flow (SignUpService → Keycloak Admin API), weil Tenant + Account atomar erstellt werden
- **Passwort vergessen/zuruecksetzen**: Keycloaks Built-in-Flows (Realm Setting: "Forgot password" = ON)
- **E-Mail-Verifikation**: Optional ueber Keycloak konfigurierbar

## Consequences

### Positive

- Klare, einheitliche Fachsprache in UI und Dokumentation
- Korrekte Zuordnung: Einladungen gehoeren zur Core Domain (Trips), nicht zur Supporting Subdomain (IAM)
- Das Konzept "Reisepartei" ist im Tourismus etabliert und deckt Einzelpersonen, Paare und Familien ab
- "Mitreisende(r)" ist inklusiver als "Kind" — deckt auch Partner ab, die nicht aktiv planen

### Negative

- Bestandscode verwendet weiterhin technische Namen (Tenant, Account, Dependent) — Mapping noetig
- Bestehende Dokumentation und Backlogs muessen aktualisiert werden
