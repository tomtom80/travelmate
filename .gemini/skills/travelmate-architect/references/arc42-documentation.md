# arc42 Documentation Reference

Maintain all 12 sections in `docs/arc42/`:

1.  **Introduction and Goals**: Requirements, quality goals, stakeholders.
2.  **Constraints**: Technical, organizational, political.
3.  **Context and Scope**: Business and technical context (C4 Context diagram).
4.  **Solution Strategy**: Fundamental decisions and solution approaches.
5.  **Building Block View**: Static decomposition of the system (C4 Container/Component).
6.  **Runtime View**: Behavior of the system (Sequence diagrams).
7.  **Deployment View**: Infrastructure and deployment (Node diagrams).
8.  **Crosscutting Concepts**: Persistence, security, logging, etc.
9.  **Architecture Decisions**: ADRs linked from `docs/adr/`.
10. **Quality Requirements**: Quality tree and scenarios.
11. **Risks and Technical Debt**: Identified risks and their impact.
12. **Glossary**: Ubiquitous language and technical terms.

## PlantUML Integration
- Diagrams should be stored in `docs/design/*.puml`.
- Use C4-PlantUML library where possible.
