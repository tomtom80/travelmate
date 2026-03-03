# 4. Lösungsstrategie

## Architekturansatz: Self-Contained Systems (SCS)

Die Anwendung ist in drei unabhängige Self-Contained Systems aufgeteilt, die jeweils einen Bounded Context abbilden:

| SCS | Bounded Context | Verantwortung |
|-----|----------------|---------------|
| **travelmate-iam** | Identity & Access Management | Mandanten, Benutzer, Rollen, Gruppen |
| **travelmate-trips** | Trip-Planung | Trips, Teilnehmer, Essensplanung, Einkaufslisten, Unterkünfte |
| **travelmate-expense** | Abrechnung | Belege, Gewichtungen, Saldo-Berechnung, Anzahlungen |

Jedes SCS besitzt eine eigene UI (Thymeleaf), eigene Datenbank (PostgreSQL) und eigene Fachlogik.

## Technologieentscheidungen

### Authentifizierung: Keycloak OIDC

- Zentraler Identity Provider mit Keycloak
- OIDC-basierte Authentifizierung über Spring Security
- Multi-Tenancy wird über Keycloak Realms oder Tenant-spezifische Konfiguration abgebildet

### UI: Thymeleaf + HTMX

- Server-Side Rendering statt Single Page Application
- HTMX für partielle Seitenaktualisierungen ohne vollständige Roundtrips
- Geringere Komplexität im Frontend, kein separater Build-Prozess
- PWA-Fähigkeiten für Offline-Nutzung und App-ähnliches Verhalten

### Asynchrone Kommunikation: Apache Kafka (KRaft)

- Event-basierte Kommunikation zwischen den SCS
- Kafka ohne Zookeeper (KRaft-Modus) für vereinfachten Betrieb
- Domain Events als Integrationsverträge (z.B. `RoleAssignedToUser`, `RoleUnassignedFromUser`)
- Gemeinsame Event-Definitionen in `travelmate-common`

### Domain-Driven Design + Hexagonale Architektur

- **Strategisch:** Bounded Contexts mit Context Mapping definieren Systemgrenzen
- **Taktisch:** Aggregates, Value Objects, Domain Events als Java Records
- **Hexagonal:** Ports (Interfaces im Domain-Layer) und Adapters (Implementierungen im Adapter-Layer)
- Domain-Schicht bleibt frei von Framework-Abhängigkeiten

### API Gateway: Spring Cloud Gateway

- Zentraler Eintrittspunkt für alle HTTP-Anfragen
- Routing zu den einzelnen SCS
- Cross-Cutting Concerns (z.B. CORS, Rate Limiting)

## Referenzen

![Context Mapping](../../design/evia.team.orc.thomas-klingler%20-%20Context-Mapping.jpg)

![High Level Service Design](../../design/evia.team.orc.thomas-klingler%20-%20High%20Level%20Service%20Design.jpg)
