---
name: Arc42 Documentation
description: "Create and maintain arc42 architecture documentation with C4 model diagrams in PlantUML"
user-invocable: false
---

# Arc42 Documentation Skill

Maintain the 12 arc42 sections in `docs/arc42/` following https://arc42.org/

## Sections

| # | Section | File | Content |
|---|---------|------|---------|
| 1 | Introduction & Goals | `01-introduction-and-goals.md` | Requirements, quality goals, stakeholders |
| 2 | Constraints | `02-constraints.md` | Technical, organizational, conventions |
| 3 | Context & Scope | `03-context-and-scope.md` | System context (C4 Level 0), external interfaces |
| 4 | Solution Strategy | `04-solution-strategy.md` | Key architectural decisions, technology choices |
| 5 | Building Block View | `05-building-block-view.md` | C4 Levels 1-3: containers, components, code |
| 6 | Runtime View | `06-runtime-view.md` | Key scenarios as sequence/activity diagrams |
| 7 | Deployment View | `07-deployment-view.md` | Infrastructure, Docker Compose, environments |
| 8 | Crosscutting Concepts | `08-crosscutting-concepts.md` | Security, persistence, events, i18n, error handling |
| 9 | Architecture Decisions | `09-architecture-decisions.md` | Links to ADRs in `docs/adr/` |
| 10 | Quality Requirements | `10-quality-requirements.md` | Quality tree, quality scenarios |
| 11 | Risks & Technical Debt | `11-risks-and-technical-debt.md` | Known risks, mitigations, debt items |
| 12 | Glossary | `12-glossary.md` | Ubiquitous Language (ADR-0011) |

## C4 Model Diagrams (PlantUML)

### Level 1 — System Context
```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

Person(user, "Reisender", "Organizer or Participant")
System(travelmate, "Travelmate", "Multi-tenant travel management platform")
System_Ext(keycloak, "Keycloak", "OIDC Identity Provider")
System_Ext(mailpit, "Mail Server", "SMTP email delivery")

Rel(user, travelmate, "Uses", "HTTPS")
Rel(travelmate, keycloak, "Authenticates via", "OIDC/JWT")
Rel(travelmate, mailpit, "Sends emails via", "SMTP")
@enduml
```

### Level 2 — Container
```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

Person(user, "Reisender")
System_Boundary(travelmate, "Travelmate") {
    Container(gateway, "Gateway", "Spring Cloud Gateway", "OIDC login, token relay, routing")
    Container(iam, "IAM", "Spring Boot", "Tenant, Account, Dependent management")
    Container(trips, "Trips", "Spring Boot", "Trip, Invitation, TravelParty management")
    Container(expense, "Expense", "Spring Boot", "Ledger, Receipt, Weighting")
    ContainerDb(db_iam, "PostgreSQL IAM", "Port 5432")
    ContainerDb(db_trips, "PostgreSQL Trips", "Port 5433")
    ContainerDb(db_expense, "PostgreSQL Expense", "Port 5434")
    ContainerQueue(rabbitmq, "RabbitMQ", "Domain Events")
}
@enduml
```

## Documentation Rules
- All diagrams in PlantUML format, stored alongside markdown or in `docs/design/`
- Use C4-PlantUML library for consistency
- Keep arc42 sections up to date when architecture changes
- Cross-reference ADRs from Section 9
- Glossary must match ADR-0011 ubiquitous language
