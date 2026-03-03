# ADR-0004: Server-Side Rendering mit Thymeleaf + HTMX statt SPA

## Status

Accepted

## Context

Jedes SCS benoetigt eine eigene UI (siehe ADR-0001). Eine SPA (React, Vue) wuerde ein separates Frontend-Projekt pro SCS erfordern, die Build-Komplexitaet erhoehen und die Kopplung zwischen Frontend und Backend ueber REST-APIs erzwingen. Das Team hat primaer Java/Spring-Expertise.

## Decision

Wir verwenden Server-Side Rendering mit Thymeleaf und HTMX, eingebettet in jedes SCS. Thymeleaf rendert die HTML-Seiten serverseitig, HTMX ermoeglicht partielle Seitenaktualisierungen ohne Full-Page-Reloads. Die UI ist damit Teil des SCS und wird im gleichen Spring Boot Prozess ausgeliefert. Fuer ein einheitliches Look-and-Feel nutzen alle SCS ein Shared CSS/Layout-Fragment.

## Consequences

### Positive

- Kein separater Frontend-Build-Prozess, alles im Maven-Build
- Java-Entwickler koennen die UI ohne JavaScript-Framework-Kenntnisse erstellen
- HTMX liefert SPA-aehnliche UX (partielle Updates, keine Full-Page-Reloads)
- Einfacheres Deployment: UI und Backend in einem Artefakt
- SSR ist SEO-freundlich und performant bei erster Seitenladung

### Negative

- Weniger interaktive UI-Moeglichkeiten als mit einer SPA
- HTMX-Patterns erfordern Umdenken gegenueber klassischem MVC
- Komplexe Client-seitige Logik (z.B. Drag-and-Drop) erfordert zusaetzliches JavaScript
- Kleineres Oekosystem und Community im Vergleich zu React/Vue
