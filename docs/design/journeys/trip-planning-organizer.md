## User Journey: Trip Planning (Organizer)
**Persona**: Organizer
**Goal**: Plan a trip, manage who attends and when, invite members from the travel party and external people
**Trigger**: User arrives at the trip list and clicks "Neue Reise planen"

### Journey Phases

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Create Trip | Fills in name, dates, description. Submits form. | Full-page redirect to trip detail. | `trip/form.html` | Neutral | No feedback if creation failed (silent redirect). | Show inline validation; confirm creation with a success notice on the detail page. |
| 2. Review trip detail | Reads status (PLANNING), sees empty participants and invitations tables. | Page renders with current user already in participant table. | `trip/detail.html` | Confused | Page is a wall of sections with no visual hierarchy. Status "PLANNING" is raw enum text. Invite section headline `h3` looks the same as trip metadata text. | Use `<article>` cards per section with clear headers. Localise status labels. |
| 3. Set own stay period | Finds date inputs next to own row in participants table. Edits dates. Clicks "Speichern". | Full-page POST, page reloads. | Participants table row | Frustrated | Inline form in table cell is cramped on mobile. Button label wraps on small screens (the reported bug). No feedback after save — did it work? | HTMX partial update of that table row. Show brief inline "Gespeichert" confirmation. |
| 4. Set stay period for companions | Looks for companion rows in the participants table. | Companions are not in the participants table — they have no row at all. | `trip/detail.html` | Blocked | No way to set arrival/departure for dependents from this page. This is the gap the architect's proposals do not address at all. | Companions must be added as participants when joining a trip, with their own editable stay-period rows. |
| 5. Invite travel party member | Scrolls to "Einladungen" section. Sees `h3 "Mitglied einladen"` inline with no visual grouping. Selects member from dropdown. Clicks submit. | `#invitations` div replaced via HTMX. | Invitations section | Frustrated | No success feedback — invitation list refreshes silently. The form does not visually distinguish from the surrounding text. | After successful HTMX swap, show an inline confirmation message in `#invitations`. Wrap form in `<article>`. |
| 6. Invite external person | Finds "Per E-Mail einladen" below member invite form. Fills in email, first/last name, birthday. Submits. | Hidden `<span id="msg-invite-sent">` is shown via inline JS. `#invitations` updates. | External invite form | Uncertain | Feedback mechanism is a hidden `<span>` toggled by raw JS — fragile, not accessible to screen readers, invisible if JS fails. Green text via `style="color:green"` is not in the design system. | Replace with a server-rendered success message fragment returned by the HTMX response (OOB swap or fragment replace). |
| 7. Advance trip status | Clicks "Bestaetigen" / "Starten" / "Abschliessen". | Full-page POST, page reloads with new status. | Status actions grid | Neutral | Status actions are bare `<button>` inside `<form>` — no confirmation for destructive states (Cancel). Status display is raw enum value. | Add `hx-confirm` for Cancel. Localise status labels. Consider a status timeline component. |

### Key Metrics
| Metric | Target | Measurement |
|--------|--------|-------------|
| Task completion — set stay period for all family members | >90% | User testing |
| Time from trip creation to first invitation sent | <3 min | Analytics |
| Error visibility rate (users who see an error when one occurs) | >95% | Error log vs session analysis |

### Improvement Opportunities
1. **Priority: Critical** — Companions cannot set stay periods at all. The participant table only shows the current user's inline form. Dependents must appear as editable rows.
2. **Priority: High** — Zero success feedback after inviting a member (HTMX swap silently updates list). User cannot tell if the action worked.
3. **Priority: High** — External invite feedback uses fragile inline JS + non-design-system colour. Must be replaced with server-rendered fragment.
4. **Priority: High** — Stay-period save in a table cell breaks on mobile (button label wraps — reported bug). Must move to HTMX partial update with dedicated UI.
5. **Priority: Medium** — Page has no visual hierarchy between "trip metadata", "participants", and "invitations" — they are three `<section>` blocks with `h2` headings that look identical.
6. **Priority: Medium** — Raw enum values (PLANNING, CONFIRMED, etc.) displayed to users. Must be replaced with localised labels.
7. **Priority: Low** — Trip creation form gives no confirmation; user lands on detail page without knowing creation succeeded.
