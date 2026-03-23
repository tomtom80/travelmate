# UX Design Reference

## User Journey Map Structure
| Phase | Action | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|--------|------------|---------|-------------|---------------|
| Phase | ...    | ...        | ...     | ...         | ...           |

## Component Specification Format
- **PicoCSS Base**: Semantic HTML/PicoCSS component.
- **Variants**: Default, Hover, Focus, Error, Loading.
- **Layout**: Desktop, Tablet, Mobile behavior.
- **Interactions (HTMX)**: `hx-trigger`, `hx-target`, `hx-swap`, `hx-indicator`.
- **Accessibility**: ARIA labels, Keyboard navigation.

## Design System (PicoCSS + Custom)
- **Forms**: Native `<form>`, `<input>`, `<select>`, `<textarea>`.
- **Buttons**: `<button>`, `role="button"` (primary/secondary/contrast).
- **Cards**: `<article>`.
- **Navigation**: `<nav>` with `<ul>`.
- **Dialogs**: `<dialog>`.

## HTMX Interaction Patterns
- `hx-get` / `hx-post`: Partial page updates.
- `hx-target` + `hx-swap`: Targeted DOM replacement.
- `hx-indicator`: Loading state.
- `hx-trigger="click" / "submit" / "change"`.
- `hx-confirm`: Confirmation for destructive actions.
