# UX Designer Agent Memory

## Design System
- PicoCSS 2 via CDN — semantic HTML auto-styled
- HTMX 2.0.8 via CDN — progressive enhancement
- Thymeleaf server-side rendering
- Color scheme: PicoCSS default blue (not teal)
- Responsive CSS theme implemented (5a9107b)
- Custom CSS in `static/css/style.css` (both iam and trips) — `.error`, section spacing, danger zone, footer

## Existing Pages
- Landing page, Sign-up, Login (Keycloak)
- Dashboard (`/iam/dashboard`) — members table, companions table, danger zone; all HTMX partial updates
- Trip list (`/trips/`) — pending invitation cards at top, trip table below
- Trip detail (`/trips/{id}`) — flat sections: metadata, participants table, invitations section with inline forms
- Trip create form (`/trips/new`) — simple name/description/dates form
- Invitation management — inline in trip detail and trip list

## Known UX Problems (documented from user feedback, v0.4.0)
See [project memory on UX issues](../../../.claude/projects/-Users-t-klingler-repos-privat-travelmate/memory/MEMORY.md) for history.

### Critical gaps (not addressed by architect's proposals)
1. Companions (Dependents) cannot set stay periods on trip detail — no row for them in participants table
2. The participant stay-period form is inline in a `<td>` — breaks on mobile, button label wraps (reported bug)

### Identified in code review
- External invite feedback uses hidden `<span>` toggled by raw JS + `style="color:green"` — not design system, not accessible
- All enum values (PLANNING, CONFIRMED, etc.) are displayed raw to users — need localised labels
- Stay-period date inputs have no associated `<label>` elements — accessibility violation
- Invitation section uses `<h3>` headings to introduce always-visible forms — not intuitive

## Feedback Pattern Decision (documented)
- Toast notifications are NOT the right primary feedback mechanism for this app
- Inline fragment feedback (server-rendered `role="alert"` in HTMX swap target) is the correct pattern
- Toast is acceptable only as additive/secondary channel for non-critical transient success
- See `/docs/design/components/feedback-system.md` for full rationale

## Design Documents Produced
- `/docs/design/journeys/trip-planning-organizer.md` — organiser journey map
- `/docs/design/journeys/invitation-flow.md` — participant invitation flow
- `/docs/design/components/feedback-system.md` — inline feedback pattern + toast evaluation
- `/docs/design/components/trip-detail-page.md` — full page redesign spec including dialog-based invite
