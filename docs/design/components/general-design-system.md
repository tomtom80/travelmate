# Travelmate Design System

Stand: 2026-04-28

## Product Overview

### Product Name

Travelmate

### Product Description

Travelmate is a shared trip planning and expense management application for small travel groups, especially families. It helps people organize a trip, invite participants, coordinate planning decisions, and keep shared costs transparent.

### Product Goal

- Reduce friction in shared trip planning.
- Make responsibilities and decisions visible.
- Keep travel expenses understandable for everyone involved.
- Support collaborative planning without forcing users into a complex project-management style workflow.

### Core Domain Areas

- Trip planning and participation
- Invitations and onboarding
- Shared expense tracking
- Household- or family-oriented collaboration
- Transparent status and responsibility management

## Typical Users

Travelmate is designed for a mixed audience with different levels of technical confidence.

- Trip organizer: creates the trip, invites others, coordinates decisions, and manages the main workflow.
- Participant: joins a trip, answers polls, contributes information, and follows planning updates.
- Expense contributor: adds receipts, tracks shared costs, and checks balances.
- Occasional user: joins only for a single trip and needs a very low-friction onboarding path.
- Mobile-first user: uses the app mostly on a phone while traveling or while standing in a store, on the road, or at a holiday apartment.

## Personas

### Organizer

- Usually the person who takes responsibility for the trip.
- Wants an overview, not a complicated control panel.
- Needs to invite others, see progress, and manage decisions quickly.
- Cares about clarity, reliability, and fewer follow-up questions from others.

### Family Participant

- Joins because they are part of the trip, not because they are interested in the system itself.
- Wants to understand what is planned, what is decided, and what still needs input.
- Needs simple language and a low learning curve.
- Often uses the app only occasionally, so recall must be easy.

### Shared Expense Contributor

- Adds receipts or checks who owes what.
- Wants fast input, clear summaries, and visible balances.
- Cares about trust, traceability, and avoiding double entries.

### First-Time Invitee

- May not know Travelmate at all before receiving an invitation.
- Needs immediate context: what the app is, why they received the invite, and what to do next.
- Requires a smooth path from invitation email to first login.

## Purpose

This document captures the general design principles for the Travelmate UI. It is the shared reference for page layout, visual tone, component behavior, and interaction patterns across the application.

## Design Principles

- Keep the UI calm, functional, and friendly.
- Prefer clear structure over decorative complexity.
- Optimize for mobile-first use without sacrificing desktop clarity.
- Use semantic HTML and server-rendered interaction patterns.
- Keep feedback immediate and state changes visible.

## Visual Direction

- Travelmate should feel like a practical family travel assistant, not a generic enterprise dashboard.
- Use a clean, light interface with strong contrast and restrained color accents.
- Keep spacing generous enough to support dense travel and expense data without feeling crowded.
- Avoid loud gradients, heavy shadows, and overly ornamental UI patterns.

## Brand Tone

- Friendly but not childish.
- Trustworthy and calm.
- Organized, helpful, and slightly personal.
- The product language should support planning, clarity, and shared decision-making.

## Technical UI Baseline

- Thymeleaf for server-rendered pages.
- HTMX for partial updates and progressive interactions.
- PicoCSS as the base styling layer.
- Inline SVG icons with `currentColor`.
- No SPA patterns unless a specific workflow clearly needs them.

## Layout Rules

- Use a mobile-first layout with a single primary action per view where possible.
- Prefer cards and stacked sections on narrow screens.
- Use tables only when the data is naturally tabular.
- Keep primary navigation visible and predictable.
- Make destructive and secondary actions harder to trigger accidentally.

## Typography

- Use readable, system-aligned text sizing with strong hierarchy.
- Headings should be short and descriptive.
- Body copy should be concise and task-oriented.
- Avoid decorative typography that competes with content density.

## Color and Emphasis

- Use one primary accent color for focus, links, and primary actions.
- Reserve danger styling for destructive or irreversible actions.
- Use muted surfaces for secondary information and summaries.
- Keep status colors consistent across modules.

## Component Behavior

- Buttons must have clear labels or accessible icon titles.
- Forms should show loading, validation, empty, and error states explicitly.
- Dialogs should be used for contained workflows, not large page replacements.
- Action menus should be reserved for secondary or rare actions.
- Icons should support the action, not replace the label when clarity matters.

## State Handling

- Every interactive view should account for loading, empty, success, and error states.
- Use visible confirmation for destructive actions.
- Keep optimistic UI behavior limited to low-risk interactions.
- Partial updates should preserve context and scroll position whenever possible.

## Accessibility

- Use semantic HTML first.
- Maintain visible focus states.
- Ensure keyboard access for all meaningful interactions.
- Preserve sufficient contrast for text, icons, and status indicators.
- Do not rely on color alone to communicate important states.

## Responsive Behavior

- Mobile views should prioritize the main task and reduce secondary noise.
- Desktop views may show more detail, but should not add unnecessary complexity.
- Keep action placement consistent across breakpoints.
- Avoid layouts that require horizontal scrolling for standard workflows.

## Content Guidelines

- Prefer short labels and concrete verbs.
- Write user-facing text in the ubiquitous language of the domain.
- Keep empty states helpful and actionable.
- Explain technical failures in user terms, not infrastructure terms.

## Design Implications

- The UI should support both power users and occasional invitees.
- Primary actions must stay obvious for organizers, while passive participants should not be overwhelmed.
- Invitation flows need stronger contextual explanation than internal views.
- Expense and trip summaries should expose the minimum needed information first, with details available on demand.
- Mobile flows must remain usable when the user is under time pressure or distracted.

## Cross References

- [Existing component design notes](./design-system.md)
- [Navigation and interaction patterns](./trip-detail-page.md)
- [Feedback system](./feedback-system.md)
- [Travelmate UX guidance](../README.md)
