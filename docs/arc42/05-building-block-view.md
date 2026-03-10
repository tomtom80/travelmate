# 5. Bausteinsicht

## Ebene 1: Gesamtsystem

Das System besteht aus folgenden Bausteinen:

```
┌────────────────────────────────────────────────────────┐
│                    Spring Cloud Gateway                 │
└───────────┬──────────────┬──────────────┬──────────────┘
            │              │              │
     ┌──────▼──────┐ ┌────▼─────┐ ┌──────▼─────┐
     │ travelmate  │ │travelmate│ │ travelmate │
     │    -iam     │ │  -trips  │ │  -expense  │
     └──────┬──────┘ └────┬─────┘ └──────┬─────┘
            │              │              │
     ┌──────▼──────┐ ┌────▼─────┐ ┌──────▼─────┐
     │ PostgreSQL  │ │PostgreSQL│ │ PostgreSQL │
     │   (IAM)     │ │ (Trips)  │ │ (Expense)  │
     └─────────────┘ └──────────┘ └────────────┘
            │              │              │
            └──────────────┼──────────────┘
                     ┌─────▼─────┐
                     │  RabbitMQ │
                     │  (AMQP)  │
                     └───────────┘
            ┌──────────────┐
            │   Keycloak   │
            │    (OIDC)    │
            └──────────────┘
```

| Baustein | Verantwortung |
|----------|--------------|
| **Spring Cloud Gateway** | Zentrales Routing, Authentifizierungsprüfung |
| **travelmate-iam** | Reisepartei-Verwaltung (Tenants), Mitglieder (Accounts), Mitreisende (Dependents), Sign-up via Keycloak Admin API |
| **travelmate-trips** | Trip-Planung, Einladungen, Teilnehmer, Aufenthaltsdauer, Trip-Status-Lifecycle |
| **travelmate-expense** | Belege, Gewichtungen, Saldo-Berechnung |
| **RabbitMQ (AMQP)** | Asynchroner Nachrichtenaustausch zwischen den SCS (Topic Exchange `travelmate.events`) |
| **Keycloak** | Zentraler Identity Provider (OIDC) |
| **PostgreSQL (je SCS)** | Datenhaltung, jeweils isoliert pro Service |

## Ebene 2: Hexagonale Struktur eines SCS

Jedes SCS folgt der gleichen hexagonalen Paketstruktur:

```
de.evia.travelmate.<service>/
│
├── domain/                         # Kern: Reine Fachlogik
│   ├── <aggregate>/
│   │   ├── Entity (Record)         # Fachliche Entität
│   │   ├── ValueObject (Record)    # Wertobjekt mit Selbstvalidierung
│   │   └── Repository (Interface)  # Port: Schnittstelle zur Persistenz
│   └── DomainEvent (Interface)     # Basis für alle Domain Events
│
├── application/                    # Anwendungsfälle
│   ├── ApplicationService          # Orchestrierung der Fachlogik
│   ├── Command (Record)            # Eingehende Befehle
│   └── Representation (Record)     # Ausgehende Datenstrukturen
│
└── adapters/                       # Infrastruktur-Implementierungen
    ├── messaging/                  # RabbitMQ Producer / Consumer
    ├── persistence/                # Repository-Implementierungen
    ├── security/                   # Spring Security Konfiguration
    └── web/                        # REST Controller / Thymeleaf Controller
```

### Schichtenregeln

- **Domain** hat keine Abhängigkeit zu Spring oder anderen Frameworks
- **Application** kennt nur die Domain-Schicht
- **Adapters** implementieren die Ports der Domain-Schicht und nutzen Spring-Infrastruktur

### Aggregate pro Bounded Context

**IAM:**
- Tenant (Mandant mit Name und Beschreibung)
- Account (Benutzerkonto, verknuepft mit Keycloak-User via KeycloakUserId)
- Dependent (Mitreisender ohne eigenen Login, einem Guardian-Account zugeordnet)
- Role, Group, Policy (bestehend aus Iteration 1)

**Trips:**
- TravelParty (Projektion der IAM-Daten, konsumiert IAM-Events)
- Trip (Aggregate Root: TripId, Name, DateRange, Status, Organizer, Participants mit StayPeriod)
- Invitation (Aggregate Root: Einladung zu einem Trip, Typen: MEMBER/EXTERNAL, Status: AWAITING_REGISTRATION/PENDING/ACCEPTED/DECLINED)
- Zukuenftig: MealPlan, ShoppingList, Accommodation

**Trips Adapter-Erweiterungen (Iteration 4):**
- `adapters/mail/InvitationEmailListener` — Versendet Einladungs-E-Mails via Spring Mail + Thymeleaf nach `InvitationCreated`-Event
- `adapters/messaging/DomainEventPublisher` — Leitet Domain Events (TripCreated, ParticipantJoinedTrip, ExternalUserInvitedToTrip) an RabbitMQ weiter

**IAM Adapter-Erweiterungen (Iteration 4):**
- `adapters/messaging/ExternalInvitationConsumer` — Konsumiert `ExternalUserInvitedToTrip` und erstellt Keycloak-User + Account

**Expense:**
- Expense (pro Trip)
- Receipt (Beleg)
- Weighting, Settlement
- DownPayment

## Referenzen

![Fachliche Strukturierung](../../design/evia.team.orc.thomas-klingler%20-%20Fachliche%20Strukturierung.jpg)

![Level 1](../../design/evia.team.orc.thomas-klingler%20-%20Level%201.jpg)
