# 9. Architekturentscheidungen

Die Architekturentscheidungen werden als Architecture Decision Records (ADRs) dokumentiert und befinden sich im Verzeichnis:

**[ADR-Verzeichnis](../adr/)**

## Übersicht der wesentlichen Entscheidungen

| Entscheidung | Begründung |
|-------------|------------|
| **Self-Contained Systems (SCS)** | Unabhängige Entwicklung und Deployment der Bounded Contexts |
| **Thymeleaf + HTMX statt SPA** | Geringere Frontend-Komplexität, kein separater Build-Prozess, bessere Server-Side-Kontrolle |
| **Kafka (KRaft) für Events** | Asynchrone, entkoppelte Kommunikation; KRaft vereinfacht den Betrieb (kein Zookeeper) |
| **Keycloak als IdP** | Etablierter Open-Source Identity Provider mit OIDC-Support und Multi-Tenancy-Fähigkeit |
| **Java Records für Domain-Objekte** | Immutabilität, kompakte Syntax, Selbstvalidierung in Compact Constructors |
| **Hexagonale Architektur** | Testbare Domain-Logik ohne Framework-Abhängigkeiten |
| **PostgreSQL pro Service** | Datenisolierung gemäß SCS-Prinzip, keine geteilte Datenbank |
| **Maven Monorepo** | Gemeinsame Build-Konfiguration, einfaches Dependency-Management für `travelmate-common` |
| **PWA** | Mobile-First-Zugang ohne App-Store-Abhängigkeit, Offline-Fähigkeit |
| **Spring Cloud Gateway** | Zentrales Routing mit Spring-Ökosystem-Integration |

Detaillierte Begründungen, Alternativen und Konsequenzen sind in den einzelnen ADRs beschrieben.
