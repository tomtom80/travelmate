---
name: ADR Writer
description: "Write Architecture Decision Records in MADR format (German) following the project's existing ADR conventions"
user-invocable: false
---

# ADR Writer Skill

Write Architecture Decision Records in MADR format, German language, stored in `docs/adr/`.

## Existing ADRs (ADR-0001 through ADR-0011)
Check existing ADRs before creating new ones to avoid contradictions.

## MADR Template

```markdown
# ADR-[NNNN]: [Titel der Entscheidung]

## Status
[Vorgeschlagen | Akzeptiert | Abgelöst | Veraltet]

## Kontext
[Beschreibung des Problems und der Rahmenbedingungen, die zu dieser Entscheidung führen]

## Entscheidung
[Die getroffene Entscheidung und ihre Begründung]

## Konsequenzen

### Positiv
- [Vorteil 1]
- [Vorteil 2]

### Negativ
- [Nachteil / Trade-off 1]
- [Nachteil / Trade-off 2]

## Alternativen

### [Alternative 1]
- Vorteile: ...
- Nachteile: ...

### [Alternative 2]
- Vorteile: ...
- Nachteile: ...

## Referenzen
- [Links zu relevanten Quellen]
```

## Naming Convention
- File: `docs/adr/ADR-NNNN-kurze-beschreibung.md`
- Next number: Check existing files and increment
- Language: German (Kontext, Entscheidung, Konsequenzen, Alternativen)
- Link new ADRs from `docs/arc42/09-architecture-decisions.md`
