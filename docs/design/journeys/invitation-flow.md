## User Journey: Invitation Flow (Participant)
**Persona**: Participant (invited member of an existing Travel Party, or external person receiving first invitation)
**Goal**: Accept an invitation to a trip and join as a participant
**Trigger**: Receives invitation email with a link

### Journey Phases — Existing Member

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Receive email | Reads trip invitation email. Clicks link. | Redirected to Keycloak login (if not authenticated) or directly to trip list. | Email client | Curious | User does not know what page to expect after clicking the link. | Email should state clearly: "Click to accept — you'll need to log in first." |
| 2. Login | Enters credentials at Keycloak. | Gateway redirects to trip list (`/trips/`). | Keycloak login | Neutral | No connection between login and the invitation context — user lands on trip list, not on the invitation. | After login, redirect to the specific trip detail where they have a pending invitation. Use `redirect_uri` or store pending invitation ID in session. |
| 3. Find invitation | Sees "Offene Einladungen" section at top of trip list. | Cards with trip name, inviter, dates. Accept/Decline buttons. | `trip/list.html` pending section | Relieved (if found) | Pending invitations are on the trip list page, mixed with the trip list itself. On first login the trip list is empty — only the invitation cards appear, which is good. | Good pattern. Keep it. Make the card more prominent (coloured border or badge). |
| 4. Accept invitation | Clicks "Annehmen". | Full-page POST, redirect back to trip list. Now trip appears in the list. | Invitation card footer | Neutral | No success feedback. Did the accept work? User only infers it from the trip appearing in the list. | Show a brief inline confirmation or use HTMX to replace the card with a "Willkommen an Bord!" message before the list refreshes. |

### Journey Phases — External Person (New User)

| Phase | User Action | System Response | Touchpoint | Emotion | Pain Points | Opportunities |
|-------|------------|-----------------|------------|---------|-------------|---------------|
| 1. Receive email | Reads trip invitation email. Clicks registration link. | Redirected to Keycloak's "set password" action page. | Email client | Uncertain | User has never heard of Travelmate. Email must explain what the app is and what they are joining. | Customise Keycloak email template with trip context (trip name, organiser name). |
| 2. Set password | Sets password in Keycloak. | Keycloak completes required actions. Redirects to application. | Keycloak set-password page | Confused | Keycloak's default UI is generic. User does not know they are now "registered" with Travelmate. | Customised Keycloak theme showing trip context in the header. |
| 3. Land on application | Arrives at trip list or dashboard. | Trip should already appear as ACCEPTED (auto-join via `linkToMember` choreography). | Trip list | Uncertain | User does not know if auto-join worked. There is no "Welcome, you have joined [trip name]" message. | Flash message on landing: "Willkommen! Du bist der Reise [Name] beigetreten." |
| 4. Explore | Navigates to trip detail. Sees own row in participants table. | Participant row with stay-period form. | `trip/detail.html` | Satisfied if everything is clear | If auto-join race condition delays the join, user sees no trip. | Loading state or retry hint if trip does not appear within a few seconds of registration. |

### Key Metrics
| Metric | Target | Measurement |
|--------|--------|-------------|
| Invitation-to-join completion rate | >80% | Event tracking |
| Time from email click to trip participation | <5 min | Analytics |
| External user confusion (contacts organiser for help) | <10% | Support tickets |

### Improvement Opportunities
1. **Priority: High** — After accepting an invitation, there is zero feedback. HTMX partial update should replace the card with a confirmation before removing it from the list.
2. **Priority: High** — External users land on the trip list without any contextual welcome message. They do not know the auto-join worked.
3. **Priority: Medium** — Login after email link does not redirect to the relevant trip. User must find the invitation in the pending section manually.
4. **Priority: Medium** — Keycloak set-password page has no Travelmate branding or trip context. The user experience gap between the invitation email and Keycloak is jarring.
5. **Priority: Low** — Invitation cards on the trip list could use a distinct visual treatment (left border accent colour, badge) to make them stand out from regular trip rows.
