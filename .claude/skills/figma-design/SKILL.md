---
name: Figma Design Specs
description: "Generate component design specifications compatible with Figma implementation, respecting PicoCSS + HTMX constraints"
user-invocable: false
---

# Figma Design Specifications Skill

Generate design specifications for Figma implementation within Travelmate's PicoCSS + HTMX + Thymeleaf stack.

## Design System: PicoCSS 2

Base: https://picocss.com/ — Semantic HTML auto-styled, minimal custom CSS.

### Core Components
- **Typography**: `<h1>`-`<h6>`, `<p>`, `<small>`, `<mark>`, `<del>`, `<ins>`
- **Buttons**: `<button>`, `<a role="button">` — `.primary`, `.secondary`, `.contrast`, `.outline`
- **Forms**: `<input>`, `<select>`, `<textarea>` — auto-styled, validation states via `aria-invalid`
- **Cards**: `<article>` — header, body, footer sections
- **Navigation**: `<nav>` + `<ul>` — responsive navbar
- **Grid**: `.grid` class for equal-column layouts
- **Modal**: `<dialog>` — native HTML dialog
- **Tables**: `<table>` — responsive, striped
- **Accordion**: `<details>` + `<summary>`
- **Progress**: `<progress>` — determinate/indeterminate
- **Loading**: `aria-busy="true"` on any element

### Theme
- Light/dark mode: `data-theme="light|dark"` on `<html>`
- Color scheme: PicoCSS default blue (not teal, per ADR)
- Custom CSS in `src/main/resources/static/css/`

## Component Specification Format

```markdown
## Component: [Name]
**Page**: [Where it appears]
**PicoCSS Base**: [Semantic HTML element]

### Visual Design
- Layout: [Flexbox/Grid/Block]
- Spacing: [PicoCSS default spacing]
- Responsive: [Breakpoint behavior]

### States
| State | Visual | Trigger |
|-------|--------|---------|
| Default | [description] | Initial render |
| Hover | [description] | Mouse over |
| Focus | [description] | Keyboard focus |
| Loading | aria-busy="true" | HTMX request |
| Error | aria-invalid="true" | Validation failure |
| Empty | [empty state design] | No data |

### HTMX Interactions
- `hx-get="/path"` → [what it loads]
- `hx-target="#container"` → [what gets replaced]
- `hx-swap="innerHTML"` → [swap strategy]
- `hx-indicator="#spinner"` → [loading indicator]

### Accessibility
- ARIA labels: [required labels]
- Keyboard: [tab order, shortcuts]
- Screen reader: [announcements]

### Figma Specifications
- Frame: [Width × Height]
- Auto Layout: [direction, spacing, padding]
- Typography: [font, size, weight, color]
- Colors: [fill, stroke, background]
```

## Output Location
- Component specs → `docs/design/components/`
- Page layouts → `docs/design/wireframes/`
