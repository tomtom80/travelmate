---
name: travelmate-architect
description: "Architectural guidance for DDD, collaborative modeling (EventStorming, Quality Storming), and documentation (ADRs, arc42). Use when discussing bounded contexts, aggregates, context maps, architectural decisions, or quality attributes."
---

# Travelmate Architect Skill

Expert guidance for Domain-Driven Design (DDD), collaborative modeling, and architecture documentation within the Travelmate project.

## Core Architectural Mandates
Always refer to the root `GEMINI.md` for the "Always Active" DDD and Security rules.

## Specialized Workflows

### 📖 1. Architecture Decision Records (ADR)
Use the MADR format (German) in `docs/adr/`.
- **References**: See [adr-writer.md](references/adr-writer.md) for templates and naming conventions.
- **Process**: Research existing ADRs, propose new decision, update `docs/arc42/09-architecture-decisions.md`.

### 📚 2. arc42 Documentation
Maintain all 12 sections in `docs/arc42/`.
- **References**: See [arc42-documentation.md](references/arc42-documentation.md) for section details.
- **C4 Model**: Use for structural diagrams (Context, Container, Component).
- **PlantUML**: Generate diagrams in `docs/design/`.

### 🧩 3. Domain-Driven Design (DDD)
Expertise in Strategic and Tactical DDD.
- **Strategic**: Bounded Contexts, Context Maps, Core/Supporting/Generic subdomains.
- **Tactical**: Aggregates (extend `AggregateRoot`), Value Objects (Records), Domain Events.
- **References**: See [ddd-modeling.md](references/ddd-modeling.md) for detailed patterns.

### 👥 4. Collaborative Modeling
Facilitate and document modeling sessions.
- **EventStorming**: Discover events, commands, aggregates.
- **Domain Storytelling**: Model workflows with actors and work objects.
- **Quality Storming**: Workshop quality requirements.
- **References**: See [collaborative-modeling.md](references/collaborative-modeling.md) for methodologies.

## Implementation Patterns
- **Hexagonal Architecture**: Keep domain framework-free.
- **Async Communication**: RabbitMQ topic exchange `travelmate.events`.
- **Tenant Isolation**: Resolve `TenantId` from JWT server-side.
