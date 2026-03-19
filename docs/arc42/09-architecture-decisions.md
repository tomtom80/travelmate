# 9. Architekturentscheidungen

Die Architekturentscheidungen werden als Architecture Decision Records (ADRs) dokumentiert und befinden sich im Verzeichnis:

**[ADR-Verzeichnis](../adr/)**

## Übersicht der wesentlichen Entscheidungen

| Entscheidung | Begründung |
|-------------|------------|
| **Self-Contained Systems (SCS)** | Unabhängige Entwicklung und Deployment der Bounded Contexts |
| **Thymeleaf + HTMX statt SPA** | Geringere Frontend-Komplexität, kein separater Build-Prozess, bessere Server-Side-Kontrolle |
| **RabbitMQ 4.0 (AMQP) für Events** | Asynchrone, entkoppelte Kommunikation; Topic Exchange für flexible Event-Verteilung |
| **Keycloak als IdP** | Etablierter Open-Source Identity Provider mit OIDC-Support und Multi-Tenancy-Fähigkeit |
| **Java Records für Domain-Objekte** | Immutabilität, kompakte Syntax, Selbstvalidierung in Compact Constructors |
| **Hexagonale Architektur** | Testbare Domain-Logik ohne Framework-Abhängigkeiten |
| **PostgreSQL pro Service** | Datenisolierung gemäß SCS-Prinzip, keine geteilte Datenbank |
| **Maven Monorepo** | Gemeinsame Build-Konfiguration, einfaches Dependency-Management für `travelmate-common` |
| **PWA** | Mobile-First-Zugang ohne App-Store-Abhängigkeit, Offline-Fähigkeit |
| **Spring Cloud Gateway** | Zentrales Routing mit Spring-Ökosystem-Integration |
| **Trip-Einladungs-E-Mail im Trips SCS** (ADR-0012) | SCS-Eigenstaendigkeit durch lokalen Mail-Versand; kein separates Notification SCS noetig |
| **Externe Einladung via Event-Choreografie** (ADR-0012) | Lose Kopplung zwischen Trips und IAM; Auto-Join bei Registrierung vereinfacht Onboarding |
| **HTMX Feedback und Error-Handling Architektur** (ADR-0013) | Konsistentes Benutzer-Feedback (Toast-Benachrichtigungen), resiliente Event-Listener, i18n-faehige Fehlermeldungen |
| **Expense Domain Design** (ADR-0014) | Gewichtete proportionale Kostenaufteilung; TripProjection als lokales Read-Model; automatische Expense-Erstellung bei TripCompleted; Saldo-Berechnung im Aggregat |
| **Shopping List Aggregate Design** (ADR-0015) | Persistiertes ShoppingList-Aggregate im Trips-SCS; RECIPE- und MANUAL-Items mit Status-Lifecycle (OPEN/ASSIGNED/PURCHASED); IngredientAggregator fuer Skalierung und Zusammenfuehrung; explizite Regeneration (lazy); HTMX-Polling alle 5s fuer Echtzeit-Updates |

Detaillierte Begründungen, Alternativen und Konsequenzen sind in den einzelnen ADRs beschrieben.
