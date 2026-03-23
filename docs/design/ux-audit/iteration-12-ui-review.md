# Iteration 12 UI Review and Follow-Up Guide

## Status

Iteration 12 introduced important new capabilities in Trips and Expense. The initial review found the following UI inconsistencies:

- table action buttons break on desktop because German labels are long
- action rows mix primary, secondary, destructive and edit actions without one stable pattern
- trip feature actions (`Unterkunft`, `Essensplan`, `Einkaufsliste`, `Abrechnung`) use different button lengths and visual weights
- the current weighting UI no longer matches the domain rule that age-based weighting must still exist

This document defined the next UI and domain alignment step. The main review outcomes were implemented in the release slice on 2026-03-23.

## Implemented Review Outcome

- participant table actions now use one visible primary action plus an overflow menu instead of long inline button rows
- organizer assignment is hidden for dependents and other participants without account
- trip feature entries use a shared card pattern with aligned CTA sizing
- weighting is visible again, with age-band recommendations and organizer override
- the remaining follow-up is visual polish, not missing interaction structure

## Decision 1: Do not use inline action button rows in dense tables

### Problem

The participant table currently places these actions side by side in one row:

- `Speichern`
- `Zum Organizer machen`
- `Entfernen`

This creates unstable widths, squeezed controls and poor scanability on both desktop and mobile.

### Guideline

For dense tables and card-lists, use this pattern:

- keep at most one inline primary action visible
- move all secondary and destructive actions into a contextual overflow menu
- use the same overflow pattern on desktop and mobile

### Recommended Pattern

Each row gets:

- inline primary action: `Speichern`
- inline quiet status badge: `Organizer`
- overflow action trigger: `...`

Overflow menu entries:

- `Zum Organizer machen`
- `Entfernen`

### UX Notes

- desktop: overflow opens as anchored popover menu at the row end
- mobile: the same trigger opens a bottom-sheet style action list or full-width menu
- destructive actions must be visually separated and styled consistently
- menu labels may stay long in German because they no longer compress the table layout

## Decision 2: Standardize trip feature actions as one card system

### Problem

The trip feature links are currently implemented as separate articles with unrelated button sizes and text lengths.

### Guideline

All feature entry points on the trip detail page must use a shared card component with:

- same title hierarchy
- same supporting text length budget
- same action area height
- same button width behavior

### Recommended Card Structure

Each feature card contains:

- title
- one short helper text line
- one primary action button of equal width
- optional secondary metadata row

### Mandatory UI Tokens

- button labels should start with a verb and stay short
- use one primary CTA per card
- primary CTAs in this area should be full-width on mobile
- primary CTAs in this area should share a common min-width on desktop
- helper text must be one sentence max

### Recommended Labels

- `Unterkunft oeffnen`
- `Essensplan oeffnen`
- `Einkaufsliste oeffnen`
- `Abrechnung oeffnen`

Avoid mixing `anzeigen`, `hinzufuegen`, `erstellen` and `zur ...` on the same level unless the feature state really differs. If state differs, keep the card layout stable and only swap the CTA label.

## Decision 3: Add a small UI Design Guide for interaction consistency

The team needs a lightweight design guide, not just ad hoc template fixes.

The guide should define:

- action hierarchy: primary, secondary, destructive, overflow-only
- when inline buttons are allowed
- when an overflow menu is required
- card CTA sizing rules
- spacing and label length rules
- desktop/mobile behavior for table actions
- badge usage for role and status labels

Minimum rule:

`If a row needs more than one persistent action, use an overflow menu.`

## Decision 4: Restore weighting as an explicit domain concept

### Problem

Weighting was made read-only, but the domain still needs explicit age-based weighting:

- children under 3: `0.0`
- children from 3 to 16: `0.5`
- adults: `1.0`

In addition, the organizer must be able to override the weighting per participant.

### Domain Rule

Settlement must be based on:

- participant-specific stay period
- participant-specific weighting

The accommodation share is therefore based on:

`effective nights x weighting`

per participant, then rolled up into the party account.

### Required Model Change

Weighting must become:

- explicitly visible in the UI
- initialized from age bands
- editable by organizer only
- stored per participant

### Recommended Behavior

- derive an initial weight automatically from date of birth
- allow organizer override in the expense context
- show whether the weight is `automatisch` or `manuell gesetzt`
- use the overridden weight for accommodation and other fair-share calculations

## Decision 5: Organizer role must stay account-bound

Dependents and children without account must never be organizer-eligible.

This means:

- backend must reject organizer assignment for non-account participants
- UI must not render the organizer action for them
- organizer badges remain visible only for account-bound participants

## Recommended Implementation Order

1. Enforce organizer eligibility by account in Trips domain and UI.
2. Replace participant row multi-button layout with overflow actions.
3. Introduce a shared trip feature card component for `Unterkunft`, `Essensplan`, `Einkaufsliste`, `Abrechnung`.
4. Reintroduce weighting editing with organizer-only permissions.
5. Apply age-band defaults and show auto/manual source in Expense.

## Acceptance Criteria for the Next Slice

- A dependent or child without account cannot be promoted to organizer.
- Participant table actions remain visually stable on desktop at common German label lengths.
- The same participant actions work on mobile without wrapping rows into unusable layouts.
- Trip feature cards have equal action sizing and aligned structure.
- Weighting is visible and organizer-editable again.
- Default weighting follows age bands `0.0 / 0.5 / 1.0`.
- Accommodation share uses `stay period x weighting`.
