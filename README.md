# Travelmate

Travelmate ist eine mandantenfaehige Reiseplanungsplattform als Maven-Monorepo. Die Anwendung kombiniert Identity & Access Management, Reiseplanung und Ausgabenverwaltung in einer Self-Contained-Systems-Architektur mit serverseitig gerenderter UI.

## Release

Aktueller Stand dieses Repository-Zustands: `v0.12.2`

Patch-Release `v0.12.2` umfasst:

- editierbare Namen fuer Reiseparteien mit Cross-SCS-Event-Propagation
- trip-spezifische Rezepte direkt im Trip-Kontext
- angepasste E2E-/BDD-Abdeckung fuer das neue Rezept-Scoping
- aktualisierte Projekt- und Release-Dokumentation

## Architekturueberblick

Module:

- `travelmate-common`: Shared Kernel fuer Domain-Primitiven und Event-Vertraege
- `travelmate-gateway`: Spring Cloud Gateway mit OIDC-Login und Token Relay
- `travelmate-iam`: Reiseparteien, Accounts, Dependents, Registrierung
- `travelmate-trips`: Reisen, Einladungen, Teilnehmer, Rezepte, Meal Plans, Shopping Lists
- `travelmate-expense`: Party Account, Belege, Gewichtungen, Vorauszahlungen
- `travelmate-e2e`: Playwright- und Cucumber-basierte End-to-End-Tests

Technik:

- Java 21
- Spring Boot 4
- Spring Cloud Gateway
- Thymeleaf + HTMX
- PostgreSQL + Flyway
- RabbitMQ
- Keycloak

## Schnellstart

Voraussetzungen:

- Java 21
- Docker und Docker Compose

Infrastruktur starten:

```bash
docker compose up -d
```

Projekt bauen:

```bash
./mvnw clean verify
```

Alle Services mit Compose bauen und starten:

```bash
docker compose up --build
```

## Tests

Ein Modul testen:

```bash
./mvnw -pl travelmate-trips clean test
```

E2E-Suite ausfuehren:

```bash
./mvnw -Pe2e -pl travelmate-e2e clean verify -DskipTests=false
```

## Dokumentation

Die technische und fachliche Dokumentation liegt unter [`docs/`](./docs/README.md):

- Architektur: `docs/arc42/`
- Entscheidungen: `docs/adr/`
- Backlog und Releases: `docs/backlog/`
- Design und UX: `docs/design/`
- Testfaelle: `docs/test-cases/`

## Lizenz

Dieses Projekt steht unter der Apache License 2.0. Details stehen in [`LICENSE`](./LICENSE).
