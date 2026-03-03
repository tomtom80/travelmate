# 2. Randbedingungen

## Technische Randbedingungen

| Randbedingung | Erläuterung |
|---------------|-------------|
| **Java 21** | LTS-Version als Basis für alle Services |
| **Spring Boot 3.2.x** | Framework für alle Self-Contained Systems |
| **Maven Monorepo** | Alle Services in einem Repository mit Maven Wrapper |
| **PostgreSQL** | Relationale Datenbank, pro Service eine eigene Instanz |
| **Apache Kafka (KRaft)** | Asynchrone Kommunikation zwischen Services, ohne Zookeeper |
| **Keycloak** | Identity Provider für OIDC-basierte Authentifizierung |
| **Thymeleaf + HTMX** | Server-Side Rendering mit partiellen Updates (kein SPA-Framework) |
| **Spring Cloud Gateway** | Zentraler Eintrittspunkt und Routing zu den SCS |
| **PWA** | Progressive Web App für mobile Nutzung und Offline-Fähigkeit |

## Organisatorische Randbedingungen

| Randbedingung | Erläuterung |
|---------------|-------------|
| **Kleines Team** | Entwicklung durch ein kleines Team, daher Fokus auf einfache, wartbare Lösungen |
| **Zeitrahmen** | Iterative Entwicklung, MVP-Ansatz |
| **CI/CD** | Azure Pipelines mit Docker-Build und Deployment auf Kubernetes |
| **Google Artifact Registry** | Container-Images werden in `europe-west3-docker.pkg.dev` gespeichert |

## Konventionen

| Konvention | Erläuterung |
|------------|-------------|
| **Domain-Driven Design (DDD)** | Strategisch (Bounded Contexts, Context Mapping) und taktisch (Aggregates, Value Objects, Domain Events) |
| **Hexagonale Architektur** | Ports & Adapters: Domain-Schicht frei von Framework-Abhängigkeiten |
| **Test-Driven Development (TDD)** | Tests zuerst, hohe Testabdeckung |
| **Code Style** | Keine Wildcard-Imports, immer Klammern bei Kontrollstrukturen, definierte Import-Reihenfolge, `final` für lokale Variablen und Parameter |
| **Java Records** | Für Entities, Value Objects, Commands und Domain Events |
| **Self-Contained Systems (SCS)** | Jeder Bounded Context ist ein eigenständiger Service mit eigener UI, Datenbank und Fachlogik |
