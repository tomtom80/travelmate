# Product Backlog — Travelmate

Stand: 2026-03-10

---

## Subdomain-Klassifikation (Strategic DDD)

| Subdomain | Typ | Bounded Context | Beziehung |
|-----------|-----|-----------------|-----------|
| Trips | **Core** | travelmate-trips | Upstream zu Expense (Partnership) |
| IAM | Supporting | travelmate-iam | Upstream zu Trips (U/D Conformist) |
| Expense | Generic | travelmate-expense | Downstream von Trips (Partnership) |

## Context Map

```
IAM ──(U)──→ D. Conformist ──→ Trips ──→ Partnership ──→ Expense
```

- **IAM -> Trips**: Trips ist Downstream Conformist. Trips konsumiert IAM-Events (AccountRegistered, DependentAddedToTenant, TenantCreated) und baut lokale TravelParty-Projektionen.
- **Trips -> Expense**: Partnership. Expense konsumiert Trips-Events (TripCreated, ParticipantJoinedTrip, TripCompleted) fuer Ledger/Abrechnung.

---

## Ubiquitous Language (ADR-0011)

| Fachsprache (DE) | English | Code | Bounded Context |
|------------------|---------|------|-----------------|
| Reisepartei | Travel Party | Tenant | IAM |
| Mitglied | Member | Account | IAM |
| Mitreisende(r) | Companion | Dependent | IAM |
| Reise | Trip | Trip | Trips |
| Reisegruppe | Travel Group | — | Trips (alle Teilnehmer einer Reise) |
| Einladung | Invitation | Invitation | Trips |
| Organisator | Organizer | — | Trips |
| Teilnehmer | Participant | Participant | Trips |
| Aufenthaltsdauer | Stay Period | StayPeriod | Trips |
| Essensplan | Meal Plan | MealPlan | Trips |
| Rezept | Recipe | Recipe | Trips |
| Einkaufsliste | Shopping List | ShoppingList | Trips |
| Unterkunft | Accommodation | Location | Trips |
| Kassenzettel / Beleg | Receipt | Receipt | Expense |
| Abrechnung | Settlement | Settlement | Expense |
| Gewichtung | Weighting | Weighting | Expense |
| Ausgabenbuch | Ledger | Ledger | Expense |

---

## Epic Overview

| Epic ID | Bounded Context | Title | Status |
|---------|----------------|-------|--------|
| E-IAM-01 | IAM | Sign-Up & Onboarding | ✅ Done |
| E-IAM-02 | IAM | Travel Party Management | ✅ Done |
| E-IAM-03 | IAM | Member Management | ✅ Done |
| E-IAM-04 | IAM | Companion Management | ✅ Done |
| E-IAM-05 | IAM | Multi-Organizer Support | TODO |
| E-IAM-06 | IAM | Notification Service (Email, SMS) | TODO |
| E-IAM-07 | IAM | Authentication & Security Enhancements | TODO |
| E-TRIPS-01 | Trips | Trip Lifecycle Management | ✅ Done |
| E-TRIPS-02 | Trips | Invitation & Participation | ✅ Done |
| E-TRIPS-03 | Trips | Stay Periods & Scheduling | ✅ Done |
| E-TRIPS-04 | Trips | Meal Planning (Essensplan) | TODO |
| E-TRIPS-05 | Trips | Recipe Management (Rezepte) | TODO |
| E-TRIPS-06 | Trips | Shopping List (Einkaufsliste) | TODO |
| E-TRIPS-07 | Trips | Location & Accommodation | TODO |
| E-TRIPS-08 | Trips | Collaborative Trip Decision Making | TODO |
| E-EXP-01 | Expense | Receipt Management (Kassenzettel) | TODO |
| E-EXP-02 | Expense | Expense Tracking & Categories | TODO |
| E-EXP-03 | Expense | Weighting & Splitting | TODO |
| E-EXP-04 | Expense | Settlement & Calculation (Abrechnung) | TODO |
| E-EXP-05 | Expense | Four-Eyes Review Process | TODO |
| E-INFRA-01 | Cross-cutting | CI/CD Pipeline | TODO |
| E-INFRA-02 | Cross-cutting | Observability & Monitoring | TODO |
| E-INFRA-03 | Cross-cutting | Architecture Fitness (ArchUnit, JaCoCo) | TODO |
| E-INFRA-04 | Cross-cutting | Security Hardening | TODO |
| E-INFRA-05 | Cross-cutting | PWA & Offline Support | TODO |
| E-INFRA-06 | Cross-cutting | i18n, Documentation & Quality | Partial |

---

## Epics & User Stories

---

### E-IAM-01: Sign-Up & Onboarding

> Self-service registration flow: creating a Travel Party, provisioning a Keycloak user, and automatic login via OIDC.

---

#### US-IAM-001: Self-Service Sign-Up ✅
**Epic**: E-IAM-01
**Priority**: Must
**Size**: L
**As a** new user, **I want** to create a Travel Party and register as the first Member, **so that** I can start planning trips immediately.

##### Acceptance Criteria
- **Given** I am on the public sign-up page (`/iam/signup`), **When** I fill in Travel Party Name, First Name, Last Name, Email, Password, and submit, **Then** a Tenant, Keycloak user, and Account are created atomically, and I receive a verification email.
- **Given** the Email is already registered, **When** I submit, **Then** I see an error "A member with this email address already exists."
- **Given** the Travel Party Name already exists, **When** I submit, **Then** I see an error "A travel party with this name already exists."
- **Given** the passwords do not match, **When** I submit, **Then** I see an error "Passwords do not match."

##### Technical Notes
- Bounded Context: IAM
- Aggregate(s): Tenant, Account
- Domain Events: TenantCreated, AccountRegistered
- UI: `signup.html` (public, no login required)
- Port: IdentityProviderService -> KeycloakIdentityProviderAdapter (Keycloak Admin API)

---

#### US-IAM-002: Login After Sign-Up ✅
**Epic**: E-IAM-01
**Priority**: Must
**Size**: M
**As a** registered Member, **I want** to log in with my credentials via the Gateway, **so that** I can access my Travel Party.

##### Acceptance Criteria
- **Given** I have verified my email, **When** I navigate to the login page and enter my credentials, **Then** I am authenticated via OIDC and redirected to the IAM Dashboard.
- **Given** my JWT is valid, **When** any SCS receives a request, **Then** the Tenant context is resolved from the JWT email claim.

##### Technical Notes
- Bounded Context: IAM + Gateway
- Gateway: OIDC login -> TokenRelay to SCS
- Tenant resolution: AccountService.findByEmail(jwt.email) -> tenantId

---

#### US-IAM-003: Email Verification ✅
**Epic**: E-IAM-01
**Priority**: Must
**Size**: S
**As a** newly registered Member, **I want** to verify my email address, **so that** my account is activated and I can log in.

##### Acceptance Criteria
- **Given** I have signed up, **When** I check my inbox, **Then** I find a verification email from Keycloak with a confirmation link.
- **Given** I click the verification link, **When** Keycloak processes it, **Then** my account is marked as email-verified and I can log in.

##### Technical Notes
- Keycloak VERIFY_EMAIL required action, emailVerified=false on signup
- Mailpit in dev: SMTP :1025, Web UI :8025

---

#### US-IAM-004: Invite Member to Travel Party ✅
**Epic**: E-IAM-01
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** to invite a new Member to my Travel Party by email, **so that** they can register and join my party.

##### Acceptance Criteria
- **Given** I am on the Dashboard, **When** I enter an email address and click "Invite Member", **Then** an Account is provisioned in Keycloak and added to my Travel Party.
- **Given** the email is already registered in my Travel Party, **When** I try to invite, **Then** I see an error message.

##### Technical Notes
- Bounded Context: IAM
- Command: InviteMemberCommand
- Aggregate(s): Account (register in existing Tenant)
- Domain Events: MemberAddedToTenant
- Port: IdentityProviderService for Keycloak user creation

---

### E-IAM-02: Travel Party Management

> Managing the Travel Party (Tenant): viewing details, editing, and deleting.

---

#### US-IAM-010: View Travel Party Dashboard ✅
**Epic**: E-IAM-02
**Priority**: Must
**Size**: M
**As a** Member, **I want** to see my Travel Party Dashboard with all Members and Companions, **so that** I have an overview of my party.

##### Acceptance Criteria
- **Given** I am logged in, **When** I navigate to `/iam/dashboard`, **Then** I see the Travel Party name, all Members, and all Companions.
- **Given** I am logged in, **When** I navigate to `/iam/`, **Then** I am redirected to the Dashboard.

##### Technical Notes
- Bounded Context: IAM
- Controller: DashboardController
- UI: `dashboard.html` with Members and Companions sections

---

#### US-IAM-011: Delete Travel Party ✅
**Epic**: E-IAM-02
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to delete my Travel Party, **so that** all data is permanently removed.

##### Acceptance Criteria
- **Given** I am on the Dashboard Danger Zone, **When** I confirm deletion, **Then** the Tenant, all Accounts, all Dependents, and all Keycloak users are deleted.
- **Given** the Tenant is deleted, **When** the event is published, **Then** downstream systems (Trips, Expense) clean up their projections.

##### Technical Notes
- Domain Events: TenantDeleted
- AdminController: DELETE `/admin/tenants/{id}` for E2E cleanup

---

#### US-IAM-012: Edit Travel Party Name
**Epic**: E-IAM-02
**Priority**: Could
**Size**: S
**As an** Organizer, **I want** to rename my Travel Party, **so that** the name reflects changes (e.g., after marriage).

##### Acceptance Criteria
- **Given** I am on the Dashboard, **When** I click "Edit" and change the name, **Then** the Tenant name is updated.
- **Given** the new name is empty or already taken, **When** I submit, **Then** I see a validation error.

##### Technical Notes
- Bounded Context: IAM
- Aggregate: Tenant
- UI: Inline edit via HTMX

---

### E-IAM-03: Member Management

> Managing Members (Accounts) within a Travel Party.

---

#### US-IAM-020: List Members ✅
**Epic**: E-IAM-03
**Priority**: Must
**Size**: S
**As a** Member, **I want** to see all Members of my Travel Party, **so that** I know who is part of our group.

##### Acceptance Criteria
- **Given** I am on the Dashboard, **When** the page loads, **Then** I see a list of all Members with their name and email.

##### Technical Notes
- Bounded Context: IAM
- Controller: DashboardController (member list section)

---

#### US-IAM-021: Remove Member ✅
**Epic**: E-IAM-03
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to remove a Member from the Travel Party, **so that** they no longer have access.

##### Acceptance Criteria
- **Given** a Travel Party has multiple Members, **When** I click "Delete" on a Member and confirm, **Then** the Account and Keycloak user are deleted, and MemberRemovedFromTenant is published.
- **Given** only one Member remains, **When** I try to remove them, **Then** I see an error "The last member cannot be removed. Delete the travel party instead."

##### Technical Notes
- Domain Events: MemberRemovedFromTenant
- Keycloak user deletion via IdentityProviderService

---

#### US-IAM-022: Edit Member Profile
**Epic**: E-IAM-03
**Priority**: Could
**Size**: S
**As a** Member, **I want** to edit my own profile (name), **so that** my data is up to date.

##### Acceptance Criteria
- **Given** I am viewing my profile, **When** I change my first or last name and save, **Then** my Account is updated.

##### Technical Notes
- Bounded Context: IAM
- Aggregate: Account

---

### E-IAM-04: Companion Management

> Managing Companions (Dependents) — people without their own login.

---

#### US-IAM-030: Add Companion ✅
**Epic**: E-IAM-04
**Priority**: Must
**Size**: M
**As a** Member, **I want** to add a Companion (child, partner) to my Travel Party, **so that** they are counted as part of the party for trips.

##### Acceptance Criteria
- **Given** I am on the Dashboard, **When** I fill in First Name, Last Name, Date of Birth, and submit, **Then** a Dependent is created under my Account.
- **Given** the Dependent is created, **When** the event is published, **Then** Trips SCS receives DependentAddedToTenant and updates TravelParty.

##### Technical Notes
- Domain Events: DependentAddedToTenant
- Aggregate: Dependent (guardian = current Account)

---

#### US-IAM-031: Remove Companion ✅
**Epic**: E-IAM-04
**Priority**: Must
**Size**: S
**As a** Member, **I want** to remove a Companion from my Travel Party, **so that** they are no longer listed.

##### Acceptance Criteria
- **Given** I have a Companion, **When** I click "Delete" and confirm, **Then** the Dependent is removed and DependentRemovedFromTenant is published.

##### Technical Notes
- Domain Events: DependentRemovedFromTenant

---

#### US-IAM-032: Edit Companion Details
**Epic**: E-IAM-04
**Priority**: Could
**Size**: S
**As a** Member, **I want** to edit a Companion's name or date of birth, **so that** their data is correct.

##### Acceptance Criteria
- **Given** I have a Companion, **When** I edit their details and save, **Then** the Dependent is updated.

##### Technical Notes
- Bounded Context: IAM
- Aggregate: Dependent

---

### E-IAM-05: Multi-Organizer Support

> Allow multiple Members within a Travel Party or Trip to have organizer privileges.

---

#### US-IAM-040: Assign Organizer Role to Member
**Epic**: E-IAM-05
**Priority**: Should
**Size**: M
**As an** Organizer, **I want** to grant the Organizer role to another Member, **so that** they can also manage trips and approve receipts.

##### Acceptance Criteria
- **Given** I am an Organizer of my Travel Party, **When** I select a Member and click "Grant Organizer", **Then** the Member receives the `organizer` role in Keycloak.
- **Given** the role is assigned, **When** the event is published, **Then** downstream systems recognize the new Organizer.

##### Technical Notes
- Bounded Context: IAM
- Domain Events: RoleAssignedToUser
- Keycloak: Assign `organizer` realm role via Admin API

---

#### US-IAM-041: Revoke Organizer Role from Member
**Epic**: E-IAM-05
**Priority**: Should
**Size**: S
**As an** Organizer, **I want** to revoke the Organizer role from another Member, **so that** they return to participant-only access.

##### Acceptance Criteria
- **Given** there are at least two Organizers, **When** I revoke one's Organizer role, **Then** the role is removed in Keycloak and RoleUnassignedFromUser is published.
- **Given** only one Organizer remains, **When** I try to revoke, **Then** I see an error "At least one organizer is required."

##### Technical Notes
- Domain Events: RoleUnassignedFromUser
- Invariant: At least one Organizer per Travel Party

---

### E-IAM-06: Notification Service (Email, SMS)

> Email and SMS notifications for invitations, receipts, and other events.

---

#### US-IAM-050: Email Notification for Trip Invitation
**Epic**: E-IAM-06
**Priority**: Must
**Size**: M
**As a** Travel Party being invited to a Trip, **I want** to receive an email notification, **so that** I am aware of the invitation and can respond.

##### Acceptance Criteria
- **Given** an Organizer invites my Travel Party to a Trip, **When** the Invitation is created, **Then** all Members of the invited Travel Party receive an email with a link to accept or decline.
- **Given** I click the link in the email, **When** I am logged in, **Then** I am taken directly to the Invitation view.

##### Technical Notes
- Cross-cutting: Trips publishes event, IAM/Notification service sends email
- Requires: Email sending infrastructure (Spring Mail + Mailpit in dev)
- New event: InvitationCreated (Trips -> Notification)

---

#### US-IAM-051: SMS Notification for Trip Invitation
**Epic**: E-IAM-06
**Priority**: Could
**Size**: M
**As a** Member, **I want** to optionally receive SMS notifications, **so that** I am informed even when I don't check email.

##### Acceptance Criteria
- **Given** I have configured my phone number in my profile, **When** an Invitation is created, **Then** I also receive an SMS notification.
- **Given** I have not configured a phone number, **When** an Invitation is created, **Then** only email is sent.

##### Technical Notes
- Requires: SMS gateway integration (e.g., Twilio)
- Profile extension: phone number field on Account

---

#### US-IAM-052: Notification Preferences
**Epic**: E-IAM-06
**Priority**: Could
**Size**: S
**As a** Member, **I want** to configure my notification preferences (email, SMS, or both), **so that** I control how I am contacted.

##### Acceptance Criteria
- **Given** I am on my profile settings, **When** I toggle notification channels, **Then** my preferences are saved and respected for future notifications.

##### Technical Notes
- Bounded Context: IAM
- New VO: NotificationPreferences on Account

---

### E-IAM-07: Authentication & Security Enhancements

> Enhanced authentication flows including password reset and 2FA.

---

#### US-IAM-060: Password Reset via Keycloak
**Epic**: E-IAM-07
**Priority**: Must
**Size**: S
**As a** Member who forgot their password, **I want** to reset it via email, **so that** I can regain access to my account.

##### Acceptance Criteria
- **Given** I am on the login page, **When** I click "Forgot password" and enter my email, **Then** Keycloak sends a password reset email.
- **Given** I click the reset link, **When** I enter a new password, **Then** my password is updated and I can log in.

##### Technical Notes
- Keycloak built-in flow: "Forgot password" = ON in realm settings
- No custom code needed; Keycloak theme customization for branding

---

#### US-IAM-061: SMS as Second Factor Authentication
**Epic**: E-IAM-07
**Priority**: Won't (this iteration)
**Size**: L
**As a** Member, **I want** to enable SMS as a second factor for login, **so that** my account is more secure.

##### Acceptance Criteria
- **Given** I have registered my phone number, **When** I log in with email + password, **Then** Keycloak sends an SMS code that I must enter to complete login.

##### Technical Notes
- Keycloak OTP via SMS requires SPI extension or external plugin
- Dependency on SMS gateway (US-IAM-051)
- Deferred to future iteration

---

### E-TRIPS-01: Trip Lifecycle Management

> Creating, viewing, and managing Trip status transitions.

---

#### US-TRIPS-001: Create Trip ✅
**Epic**: E-TRIPS-01
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** to create a new Trip with a name, description, and date range, **so that** I can start planning a holiday.

##### Acceptance Criteria
- **Given** I am on the Trips page, **When** I fill in Name, Description, Start Date, End Date and submit, **Then** a Trip is created in PLANNING status with me as the Organizer.
- **Given** the Trip is created, **When** the TripCreated event is published, **Then** the Expense SCS can create a Ledger for this Trip.
- **Given** the Start Date is after the End Date, **When** I submit, **Then** I see a validation error.

##### Technical Notes
- Bounded Context: Trips
- Aggregate: Trip (factory method `Trip.plan(...)`)
- Domain Events: TripCreated
- VOs: TripId, TripName, TripDescription, DateRange

---

#### US-TRIPS-002: View Trip List ✅
**Epic**: E-TRIPS-01
**Priority**: Must
**Size**: S
**As a** Member, **I want** to see all Trips for my Travel Party, **so that** I have an overview of planned, ongoing, and past trips.

##### Acceptance Criteria
- **Given** I am on the Trips page, **When** the page loads, **Then** I see all Trips with name, date range, and status.

##### Technical Notes
- Controller: TripController
- UI: `trip/list.html`

---

#### US-TRIPS-003: View Trip Details ✅
**Epic**: E-TRIPS-01
**Priority**: Must
**Size**: M
**As a** Member, **I want** to see the details of a Trip, **so that** I know the schedule, participants, and status.

##### Acceptance Criteria
- **Given** I click on a Trip, **When** the detail page loads, **Then** I see name, description, dates, status, all Participants with their stay periods, and pending Invitations.

##### Technical Notes
- Controller: TripController (detail view)
- UI: `trip/detail.html`

---

#### US-TRIPS-004: Trip Status Transitions ✅
**Epic**: E-TRIPS-01
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to advance the Trip through its lifecycle (PLANNING -> CONFIRMED -> IN_PROGRESS -> COMPLETED), **so that** all participants know the current state.

##### Acceptance Criteria
- **Given** a Trip is in PLANNING, **When** I click "Confirm", **Then** the status changes to CONFIRMED.
- **Given** a Trip is in CONFIRMED, **When** I click "Start", **Then** the status changes to IN_PROGRESS.
- **Given** a Trip is IN_PROGRESS, **When** I click "Complete", **Then** the status changes to COMPLETED and TripCompleted is published.
- **Given** a Trip is in PLANNING, **When** I click "Cancel", **Then** the status changes to CANCELLED.

##### Technical Notes
- Domain Events: TripCompleted
- Status enum: PLANNING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED

---

#### US-TRIPS-005: Edit Trip Details
**Epic**: E-TRIPS-01
**Priority**: Should
**Size**: S
**As an** Organizer, **I want** to edit a Trip's name, description, or date range, **so that** I can adjust plans.

##### Acceptance Criteria
- **Given** a Trip is in PLANNING or CONFIRMED, **When** I edit and save, **Then** the Trip details are updated.
- **Given** a Trip is IN_PROGRESS or COMPLETED, **When** I try to edit, **Then** editing is not allowed.
- **Given** I change dates, **When** existing StayPeriods fall outside the new range, **Then** I see a validation warning.

##### Technical Notes
- Bounded Context: Trips
- Aggregate: Trip

---

#### US-TRIPS-006: Delete Trip
**Epic**: E-TRIPS-01
**Priority**: Should
**Size**: S
**As an** Organizer, **I want** to delete a Trip that is still in PLANNING, **so that** I can remove mistakenly created trips.

##### Acceptance Criteria
- **Given** a Trip is in PLANNING, **When** I click "Delete" and confirm, **Then** the Trip and all associated Invitations are deleted.
- **Given** a Trip is not in PLANNING, **When** I try to delete, **Then** deletion is not allowed.

##### Technical Notes
- New event: TripDeleted (for Expense cleanup)

---

### E-TRIPS-02: Invitation & Participation

> Inviting Travel Parties to Trips and managing participation.

---

#### US-TRIPS-010: Invite Travel Party to Trip ✅
**Epic**: E-TRIPS-02
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to invite Members of my Travel Party to a Trip, **so that** they can participate.

##### Acceptance Criteria
- **Given** I am on the Trip detail page, **When** I select a Member and click "Invite", **Then** an Invitation in PENDING status is created.
- **Given** the Member is already invited, **When** I try to invite again, **Then** no duplicate Invitation is created.
- **Given** all Members are already invited, **When** I view the invite form, **Then** I see "All members already invited."

##### Technical Notes
- Aggregate: Invitation
- Domain Events: (none yet; US-IAM-050 introduces InvitationCreated for notifications)

---

#### US-TRIPS-011: Accept Invitation ✅
**Epic**: E-TRIPS-02
**Priority**: Must
**Size**: M
**As a** Member, **I want** to accept a Trip Invitation, **so that** I am added as a Participant.

##### Acceptance Criteria
- **Given** I have a pending Invitation, **When** I click "Accept", **Then** the Invitation status becomes ACCEPTED and I am added as a Participant to the Trip.
- **Given** I accept, **When** the event is processed, **Then** ParticipantJoinedTrip is published.

##### Technical Notes
- Domain Events: ParticipantJoinedTrip
- Trip.addParticipant() called when Invitation is accepted

---

#### US-TRIPS-012: Decline Invitation ✅
**Epic**: E-TRIPS-02
**Priority**: Must
**Size**: S
**As a** Member, **I want** to decline a Trip Invitation, **so that** the Organizer knows I won't participate.

##### Acceptance Criteria
- **Given** I have a pending Invitation, **When** I click "Decline", **Then** the Invitation status becomes DECLINED.

##### Technical Notes
- Aggregate: Invitation

---

#### US-TRIPS-013: Invite External Travel Party to Trip
**Epic**: E-TRIPS-02
**Priority**: Should
**Size**: L
**As an** Organizer, **I want** to invite an external person (not yet in any Travel Party) to my Trip via email, **so that** friends and other families can join.

##### Acceptance Criteria
- **Given** I enter an email address of someone not yet registered, **When** I click "Invite", **Then** an external Invitation is created and a notification email is sent with a sign-up/accept link.
- **Given** the invitee clicks the link, **When** they sign up or log in, **Then** they are guided to accept the Trip Invitation.

##### Technical Notes
- Bounded Context: Trips (Invitation with external email)
- Cross-cutting: Requires notification service (E-IAM-06)
- New Invitation type or status for external invitees

---

#### US-TRIPS-014: Remove Participant from Trip
**Epic**: E-TRIPS-02
**Priority**: Should
**Size**: S
**As an** Organizer, **I want** to remove a Participant from a Trip, **so that** the participant list is accurate.

##### Acceptance Criteria
- **Given** a Trip has Participants, **When** I remove a Participant, **Then** they are removed from the Trip.
- **Given** the Participant has existing StayPeriods, **When** removed, **Then** the StayPeriod is also deleted.

##### Technical Notes
- New event: ParticipantLeftTrip (for Expense adjustment)

---

### E-TRIPS-03: Stay Periods & Scheduling

> Managing individual arrival and departure dates within a Trip.

---

#### US-TRIPS-020: Set Stay Period ✅
**Epic**: E-TRIPS-03
**Priority**: Must
**Size**: M
**As a** Participant, **I want** to set my arrival and departure dates, **so that** the Organizer knows when I will be present.

##### Acceptance Criteria
- **Given** I am a Participant of a Trip, **When** I set my Arrival Date and Departure Date, **Then** my StayPeriod is saved.
- **Given** my dates fall outside the Trip's date range, **When** I submit, **Then** I see a validation error.

##### Technical Notes
- VO: StayPeriod (arrivalDate, departureDate)
- Invariant: StayPeriod must be within Trip's DateRange

---

#### US-TRIPS-021: View Participant Schedule
**Epic**: E-TRIPS-03
**Priority**: Should
**Size**: M
**As an** Organizer, **I want** to see a calendar/grid view of all Participants' stay periods, **so that** I know who is present on each day.

##### Acceptance Criteria
- **Given** a Trip has Participants with StayPeriods, **When** I view the schedule, **Then** I see a day-by-day grid showing who is present.
- **Given** a day has no Participants, **When** I view the schedule, **Then** the day is shown as empty.

##### Technical Notes
- UI: Grid/table with days as columns, Participants as rows
- Derived data: no new aggregate needed, computed from StayPeriods

---

#### US-TRIPS-022: Per-Day Participant Count
**Epic**: E-TRIPS-03
**Priority**: Should
**Size**: S
**As an** Organizer, **I want** to see the participant count per day, **so that** I can plan meals and logistics accordingly.

##### Acceptance Criteria
- **Given** a Trip has Participants with StayPeriods, **When** I view the Trip details, **Then** I see the number of people present on each day (including Companions).

##### Technical Notes
- Computed from StayPeriods + Dependents attached to each Participant
- Important for Meal Planning scaling (E-TRIPS-04)

---

### E-TRIPS-04: Meal Planning (Essensplan)

> Planning meals for the entire Trip duration: a grid of days x meals, with the ability to skip meals or dine out.

---

#### US-TRIPS-030: Create Meal Plan for Trip
**Epic**: E-TRIPS-04
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** to create a Meal Plan for a Trip, **so that** we can organize meals for each day.

##### Acceptance Criteria
- **Given** a Trip has a date range, **When** I create a Meal Plan, **Then** a grid is generated with one row per day and columns for Breakfast (Fruehstueck), Lunch (Mittagessen), and Dinner (Abendessen).
- **Given** the Meal Plan is created, **When** I view it, **Then** each MealSlot shows its current state (PLANNED, SKIP, EATING_OUT).

##### Technical Notes
- Bounded Context: Trips
- New Aggregate: MealPlan (root) with MealSlot entities
- MealSlot: day (LocalDate), mealType (BREAKFAST, LUNCH, DINNER), status (PLANNED, SKIP, EATING_OUT), recipeId (optional), restaurantName (optional)
- MealPlan is generated from Trip's DateRange

---

#### US-TRIPS-031: Mark MealSlot as SKIP
**Epic**: E-TRIPS-04
**Priority**: Must
**Size**: S
**As an** Organizer, **I want** to mark a MealSlot as "skip", **so that** no cooking is planned for that meal.

##### Acceptance Criteria
- **Given** a MealSlot is PLANNED, **When** I click "Skip", **Then** the MealSlot status becomes SKIP and it is excluded from shopping list generation.

##### Technical Notes
- MealSlot status transition: PLANNED -> SKIP (reversible)

---

#### US-TRIPS-032: Mark MealSlot as EATING_OUT
**Epic**: E-TRIPS-04
**Priority**: Must
**Size**: S
**As an** Organizer, **I want** to mark a MealSlot as "eating out", **so that** we know we are going to a restaurant.

##### Acceptance Criteria
- **Given** a MealSlot is PLANNED, **When** I click "Eating Out" and optionally enter a restaurant name, **Then** the MealSlot status becomes EATING_OUT.

##### Technical Notes
- MealSlot status transition: PLANNED -> EATING_OUT (reversible)
- Optional: restaurantName field

---

#### US-TRIPS-033: Assign Recipe to MealSlot
**Epic**: E-TRIPS-04
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to assign a Recipe to a MealSlot, **so that** we know what to cook and the ingredients flow into the Shopping List.

##### Acceptance Criteria
- **Given** a MealSlot is PLANNED and Recipes exist, **When** I select a Recipe and assign it, **Then** the MealSlot references the Recipe and the MealSlot status remains PLANNED.
- **Given** a Recipe is assigned, **When** the Shopping List is viewed, **Then** the Recipe's ingredients appear automatically (scaled by participant count).

##### Technical Notes
- MealSlot.recipeId links to Recipe aggregate
- Ingredient aggregation done in ShoppingList generation (E-TRIPS-06)

---

#### US-TRIPS-034: View Meal Plan Overview
**Epic**: E-TRIPS-04
**Priority**: Must
**Size**: M
**As a** Participant, **I want** to view the Meal Plan for a Trip, **so that** I know what meals are planned each day.

##### Acceptance Criteria
- **Given** a Trip has a Meal Plan, **When** I navigate to the Meal Plan tab, **Then** I see the day x meal grid with assigned Recipes, skips, and eating-out entries.

##### Technical Notes
- UI: Responsive grid/table. On mobile: day-by-day card view
- HTMX: Partial updates when MealSlots change

---

#### US-TRIPS-035: Assign Kitchen Duty to Travel Parties
**Epic**: E-TRIPS-04
**Priority**: Should
**Size**: M
**As an** Organizer, **I want** to assign one or more travel parties as kitchen duty for an executed meal, **so that** cooking and dishwashing responsibility is transparent.

##### Acceptance Criteria
- **Given** a MealSlot was actually carried out, **When** I assign one or more travel parties as kitchen duty, **Then** the assignment is saved for that meal.
- **Given** a MealSlot is marked as SKIP or EATING_OUT, **When** I try to assign kitchen duty, **Then** the system rejects the action.
- **Given** kitchen duty was assigned, **When** participants view the meal plan, **Then** they can see which travel parties were responsible for cooking / dishwashing.
- **Given** I need to correct the assignment, **When** I update the responsible travel parties, **Then** the previous assignment is replaced.

##### Technical Notes
- Bounded Context: Trips
- Extend `MealSlot` or add supporting entity for `KitchenDutyAssignment`
- Organizer-only command on executed meals
- Useful later for fairness analytics and retrospective planning

---

### E-TRIPS-05: Recipe Management (Rezepte)

> Managing recipes for meal planning: manual entry and import from URLs.

---

#### US-TRIPS-040: Create Recipe Manually
**Epic**: E-TRIPS-05
**Priority**: Must
**Size**: M
**As a** Member, **I want** to create a Recipe with name, description, servings, and ingredients, **so that** it can be used in the Meal Plan.

##### Acceptance Criteria
- **Given** I am on the Recipe page, **When** I enter Name, Description, Servings (number), and at least one Ingredient (name, quantity, unit), **Then** the Recipe is saved.
- **Given** a Recipe exists, **When** I view it, **Then** I see all details and the ingredient list.

##### Technical Notes
- Bounded Context: Trips
- New Aggregate: Recipe (RecipeId, name, description, servings, ingredients[])
- Ingredient VO: name, quantity (BigDecimal), unit (string, e.g., "g", "ml", "Stueck")
- Recipes are scoped by TenantId

---

#### US-TRIPS-041: Import Recipe from URL
**Epic**: E-TRIPS-05
**Priority**: Should
**Size**: L
**As a** Member, **I want** to import a Recipe from a URL (e.g., chefkoch.de), **so that** I don't have to type ingredients manually.

##### Acceptance Criteria
- **Given** I paste a URL that contains schema.org/Recipe structured data, **When** I click "Import", **Then** the Recipe name, servings, and ingredients are extracted and pre-filled.
- **Given** the URL does not contain structured data, **When** I click "Import", **Then** I see a message "Could not extract recipe from this URL" and can enter data manually.

##### Technical Notes
- Web scraping: Fetch URL, parse JSON-LD for `schema.org/Recipe`
- Adapter: RecipeImportAdapter (in adapters/web or adapters/integration)
- Fallback to manual entry if parsing fails

---

#### US-TRIPS-042: Edit Recipe
**Epic**: E-TRIPS-05
**Priority**: Should
**Size**: S
**As a** Member, **I want** to edit an existing Recipe, **so that** I can correct or update ingredients.

##### Acceptance Criteria
- **Given** a Recipe exists, **When** I edit ingredients or servings and save, **Then** the Recipe is updated and Shopping List reflects changes.

##### Technical Notes
- Ingredient list is fully replaceable on edit

---

#### US-TRIPS-043: Delete Recipe
**Epic**: E-TRIPS-05
**Priority**: Should
**Size**: S
**As a** Member, **I want** to delete a Recipe, **so that** unused recipes don't clutter the list.

##### Acceptance Criteria
- **Given** a Recipe is not assigned to any MealSlot, **When** I delete it, **Then** it is removed.
- **Given** a Recipe is assigned to a MealSlot, **When** I try to delete, **Then** I see a warning and must confirm (the MealSlot will revert to PLANNED without a recipe).

##### Technical Notes
- Check for MealSlot references before deletion

---

#### US-TRIPS-044: List Recipes per Trip / Travel Party
**Epic**: E-TRIPS-05
**Priority**: Must
**Size**: S
**As a** Member, **I want** to see all available Recipes, **so that** I can choose one for a MealSlot.

##### Acceptance Criteria
- **Given** I am managing a Trip's Meal Plan, **When** I open the Recipe selector, **Then** I see all Recipes belonging to my Travel Party.

##### Technical Notes
- Recipes are TenantId-scoped; reusable across trips

---

### E-TRIPS-06: Shopping List (Einkaufsliste)

> A shared shopping list per Trip with automatic entries from recipes and manual additions.

---

#### US-TRIPS-050: Auto-Generate Shopping List from Meal Plan
**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** the Shopping List to be automatically generated from the Meal Plan's recipes, **so that** I don't have to compute ingredients manually.

##### Acceptance Criteria
- **Given** a Meal Plan has Recipes assigned to MealSlots, **When** I view the Shopping List, **Then** ingredients from all Recipes are aggregated (same ingredients summed), scaled by participant count relative to recipe servings.
- **Given** a MealSlot is SKIP or EATING_OUT, **When** the Shopping List is generated, **Then** that slot's ingredients are excluded.
- **Given** two MealSlots reference Recipes that both need "500g flour", **When** the Shopping List is generated, **Then** the entry shows "1000g flour" (aggregated).

##### Technical Notes
- Bounded Context: Trips
- New Aggregate: ShoppingList (ShoppingListId, tripId, items[])
- ShoppingItem: name, quantity, unit, source (RECIPE / MANUAL), status (OPEN / ASSIGNED / PURCHASED), assignedTo (memberId, optional)
- Scaling: `recipeServings -> tripParticipantCount` ratio
- Regeneration: triggered when MealPlan changes (recipe assigned/removed, slot status change)

---

#### US-TRIPS-051: Add Manual Shopping Item
**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: S
**As a** Participant, **I want** to manually add items to the Shopping List (snacks, drinks, household items), **so that** everything we need is in one place.

##### Acceptance Criteria
- **Given** I am on the Shopping List, **When** I add an item with name, quantity, and unit, **Then** it is added with source=MANUAL and status=OPEN.

##### Technical Notes
- Manual items are preserved across Meal Plan regeneration

---

#### US-TRIPS-052: Assign Shopping Item to Myself
**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: S
**As a** Participant, **I want** to assign a Shopping Item to myself, **so that** others know I will take care of buying it.

##### Acceptance Criteria
- **Given** a ShoppingItem is OPEN, **When** I click "I'll get it", **Then** the item is ASSIGNED to me and shows my name.
- **Given** a ShoppingItem is ASSIGNED to me, **When** I click "Unassign", **Then** it returns to OPEN.

##### Technical Notes
- ShoppingItem.assignTo(memberId)
- UI: HTMX partial update for real-time feel

---

#### US-TRIPS-053: Mark Shopping Item as Purchased
**Epic**: E-TRIPS-06
**Priority**: Must
**Size**: S
**As a** Participant, **I want** to mark a Shopping Item as purchased, **so that** others know it's done.

##### Acceptance Criteria
- **Given** a ShoppingItem is ASSIGNED to me, **When** I click "Done", **Then** the status becomes PURCHASED.
- **Given** a ShoppingItem is PURCHASED, **When** others view the list, **Then** they see it as completed.

##### Technical Notes
- Status transition: OPEN -> ASSIGNED -> PURCHASED

---

#### US-TRIPS-054: Real-Time Shopping List Updates
**Epic**: E-TRIPS-06
**Priority**: Should
**Size**: M
**As a** Participant, **I want** to see Shopping List changes made by others in near-real-time, **so that** we don't buy the same things.

##### Acceptance Criteria
- **Given** another Participant marks an item as purchased, **When** I view the list, **Then** I see the update within a few seconds.

##### Technical Notes
- Implementation: HTMX polling (hx-trigger="every 5s") or SSE (Server-Sent Events)
- SSE is preferred for lower latency

---

#### US-TRIPS-055: Bring App Integration
**Epic**: E-TRIPS-06
**Priority**: Could
**Size**: L
**As a** Participant, **I want** to sync my assigned Shopping Items to the Bring! app, **so that** I can use Bring while shopping.

##### Acceptance Criteria
- **Given** I have linked my Bring! account, **When** items are assigned to me, **Then** they appear in my Bring! shopping list.
- **Given** I mark an item as purchased in Bring!, **When** the sync runs, **Then** the item is marked as PURCHASED in Travelmate.

##### Technical Notes
- Integration adapter: BringApiAdapter (Bring REST API)
- Requires Bring API credentials per user (profile setting)
- Sync direction: Travelmate -> Bring (push), Bring -> Travelmate (poll or webhook)

---

### E-TRIPS-07: Location & Accommodation

> Managing accommodation details, location information, and polls.

---

#### US-TRIPS-060: Add Accommodation Details to Trip
**Epic**: E-TRIPS-07
**Priority**: Should
**Size**: M
**As an** Organizer, **I want** to add accommodation details (name, address, URL, room count, price per night) to a Trip, **so that** all Participants have the information.

##### Acceptance Criteria
- **Given** I am editing a Trip, **When** I add accommodation details, **Then** the information is saved and visible on the Trip detail page.
- **Given** a Trip has accommodation info, **When** a Participant views the Trip, **Then** they see all accommodation details.

##### Technical Notes
- Bounded Context: Trips
- New VO or Entity: Location (name, address, url, roomCount, pricePerNight, notes)
- Embedded in Trip aggregate or separate entity

---

#### US-TRIPS-061: Import Location Info from URL
**Epic**: E-TRIPS-07
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to paste a booking URL and have accommodation details extracted automatically, **so that** I don't have to enter them manually.

##### Acceptance Criteria
- **Given** I paste a URL, **When** I click "Import", **Then** the system extracts name, address, and image from the page's metadata (Open Graph, schema.org).
- **Given** the URL has no extractable data, **When** I click "Import", **Then** I see a message and can enter details manually.

##### Technical Notes
- Web scraping: Fetch URL, parse Open Graph (`og:title`, `og:description`, `og:image`) and schema.org/LodgingBusiness
- Adapter: LocationImportAdapter

---

#### US-TRIPS-062: Accommodation Poll (Abstimmung)
**Epic**: E-TRIPS-07
**Priority**: Could
**Size**: L
**As an** Organizer, **I want** to create a poll with multiple accommodation options, **so that** Participants can vote on their preferred location.

##### Acceptance Criteria
- **Given** I create a poll with 2+ accommodation options, **When** Participants vote, **Then** each Participant can vote for their preferred option(s).
- **Given** all votes are in, **When** I view the poll results, **Then** I see the vote counts and the winner.

##### Technical Notes
- New Aggregate: LocationPoll (pollId, tripId, options[], votes[])
- Each Participant gets one vote (or ranked choice)

---

### E-TRIPS-08: Collaborative Trip Decision Making

> Early-trip collaboration: collect date options, vote on the best travel period, gather accommodation candidates, vote transparently, and let the organizer finalize the decision.

---

#### US-TRIPS-080: Create Date Poll for Trip Initialization
**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** to create a date poll with multiple candidate trip periods, **so that** all invited travel parties can coordinate around school holidays and closure periods.

##### Acceptance Criteria
- **Given** I am organizing a trip in early planning, **When** I define two or more candidate date ranges, **Then** a date poll is created for the trip.
- **Given** a date poll exists, **When** participants open the trip planning area, **Then** they can see all candidate date ranges in a clear poll view.
- **Given** I need to adjust the options before a final decision, **When** I add or remove a candidate date range, **Then** the poll is updated and existing votes on unchanged options remain visible.

##### Technical Notes
- Bounded Context: Trips
- New Aggregate candidate: `DatePoll`
- Poll should start in parallel with invitations and participant onboarding

---

#### US-TRIPS-081: Vote in Date Poll as Adult Party Member
**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As an** adult member with an account, **I want** to mark which candidate date ranges work for my travel party, **so that** the group can find the best travel period.

##### Acceptance Criteria
- **Given** a date poll is open, **When** I view the poll, **Then** I can mark one or more candidate date ranges as suitable in a simple, doodle-like selection UI.
- **Given** I already voted, **When** I change my availability marks, **Then** my previous vote is replaced by the new selection.
- **Given** I am a dependent without an account, **When** I open the trip, **Then** I cannot cast a vote.
- **Given** votes exist, **When** any participant views the poll results, **Then** the current vote totals per date option are visible at any time.

##### Technical Notes
- Voting right is per adult/member account, not per dependent
- Read model should expose both individual availability marks and aggregated result counts
- UI should optimize for fast multi-select rather than ranked voting

---

#### US-TRIPS-082: Confirm Winning Travel Period from Date Poll
**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to select the final travel period from the date poll result, **so that** the trip can move from open scheduling into concrete booking.

##### Acceptance Criteria
- **Given** a date poll has votes, **When** I review the results, **Then** I can see which option currently has the most support.
- **Given** I am the organizer, **When** I confirm one of the poll options as final, **Then** the trip date range is updated to that selected period.
- **Given** a final period was selected, **When** participants open the trip, **Then** they see the chosen trip period and the historical poll result.

##### Technical Notes
- Connects poll outcome back into the `Trip` aggregate
- Tie situations stay organizer-decidable; the organizer is not forced to auto-pick the numerical winner
- Requires validation against existing stay-period editing rules

---

#### US-TRIPS-083: Collect Accommodation Candidates Collaboratively
**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As a** participant with an account, **I want** to submit accommodation suggestions for a trip, **so that** the group can build a shared shortlist before booking.

##### Acceptance Criteria
- **Given** a trip is in planning, **When** I submit accommodation details or a booking link, **Then** the candidate is added to the accommodation shortlist.
- **Given** accommodation candidates exist, **When** participants open the trip planning area, **Then** they can see the full shortlist and the current status of each candidate.
- **Given** an accommodation was not selected, **When** the organizer books another option, **Then** the losing candidates remain archived for fallback use.
- **Given** an accommodation candidate is no longer relevant, **When** the organizer archives it before booking, **Then** it disappears from the active vote but stays in history.

##### Technical Notes
- Reuses and extends `E-TRIPS-07` accommodation data
- Candidate lifecycle should distinguish active shortlist entries from archived fallback entries
- This story prepares, but does not yet finalize, the booking decision

---

#### US-TRIPS-084: Vote and Re-Vote for Accommodation Candidate
**Epic**: E-TRIPS-08
**Priority**: Must
**Size**: L
**As a** participant with an account, **I want** to give exactly one active vote to my preferred accommodation and move it later if needed, **so that** the group can converge on a fair decision.

##### Acceptance Criteria
- **Given** active accommodation candidates exist, **When** I cast my vote, **Then** exactly one active vote is assigned to my chosen accommodation.
- **Given** I already voted, **When** I choose another accommodation, **Then** my vote is removed from the previous candidate and moved to the new one.
- **Given** the vote result is tied, **When** I reconsider my choice, **Then** I can change my vote at any time while the poll remains open.
- **Given** participants vote, **When** anyone views the shortlist, **Then** the current result visualization is visible at any time.
- **Given** I am the organizer, **When** I decide to book one accommodation, **Then** I can mark that candidate as selected even if the vote result is tied.

##### Technical Notes
- Voting right is one active vote per account-holding participant
- Organizer retains final booking decision after the poll
- Optional add-on later: show availability for the selected date range directly on the candidate
- Could supersede the older generic `US-TRIPS-062` once implemented in detail

---

### E-EXP-01: Receipt Management (Kassenzettel)

> Submitting, reviewing, and managing receipts for trip expenses.

---

#### US-EXP-001: Create Ledger from Trip Event
**Epic**: E-EXP-01
**Priority**: Must
**Size**: M
**As the** system, **I want** to automatically create a Ledger when a Trip is created, **so that** expenses can be tracked from the start.

##### Acceptance Criteria
- **Given** a TripCreated event is received, **When** the Expense SCS processes it, **Then** a Ledger is created with the tripId, tenantId, and trip name.
- **Given** a ParticipantJoinedTrip event is received, **When** processed, **Then** the Participant is added to the Ledger.

##### Technical Notes
- Bounded Context: Expense
- New Aggregate: Ledger (ledgerId, tripId, tenantId, participants[], receipts[])
- Consumed Events: TripCreated, ParticipantJoinedTrip, TripCompleted

---

#### US-EXP-002: Submit Receipt by Photo (Camera)
**Epic**: E-EXP-01
**Priority**: Must
**Size**: XL
**As a** Participant, **I want** to photograph a receipt with my phone camera and submit it, **so that** the expense is recorded quickly.

##### Acceptance Criteria
- **Given** I am on the expense page for a Trip, **When** I click "Add Receipt" and take a photo (or upload an image), **Then** the photo is stored and associated with the Ledger.
- **Given** the photo is uploaded, **When** the system processes it, **Then** OCR extracts the total amount and pre-fills the amount field.
- **Given** the OCR result is incorrect, **When** I review the result, **Then** I can manually correct the amount before submitting.

##### Technical Notes
- Bounded Context: Expense
- New Aggregate: Receipt (receiptId, ledgerId, submitterId, imageUrl, amount, description, status, ocrResult)
- File storage: Local filesystem or S3-compatible storage
- OCR: Tesseract (local) or cloud OCR API (Google Vision, AWS Textract)
- Mobile: HTML5 `<input type="file" capture="environment" accept="image/*">`

---

#### US-EXP-003: Manual Receipt Entry
**Epic**: E-EXP-01
**Priority**: Must
**Size**: S
**As a** Participant, **I want** to manually enter a receipt amount and description without a photo, **so that** I can record cash expenses or situations where I don't have a photo.

##### Acceptance Criteria
- **Given** I am on the expense page, **When** I enter amount, description, and date, **Then** a Receipt is created without a photo.

##### Technical Notes
- Receipt with imageUrl=null is valid
- Same aggregate, simpler flow

---

#### US-EXP-004: View All Receipts for a Trip
**Epic**: E-EXP-01
**Priority**: Must
**Size**: M
**As a** Participant, **I want** to see all submitted receipts for a Trip, **so that** I have an overview of expenses.

##### Acceptance Criteria
- **Given** a Trip has receipts, **When** I view the expense page, **Then** I see a list of all receipts with submitter, amount, description, date, and review status.

##### Technical Notes
- UI: Receipt list with HTMX filtering

---

#### US-EXP-005: Edit Own Receipt
**Epic**: E-EXP-01
**Priority**: Should
**Size**: S
**As a** Participant, **I want** to edit a receipt I submitted (amount, description), **so that** I can correct mistakes.

##### Acceptance Criteria
- **Given** my Receipt is in SUBMITTED status, **When** I edit and save, **Then** the Receipt is updated and its review status resets to PENDING.

##### Technical Notes
- Only submitter can edit; editing resets review status

---

### E-EXP-02: Expense Tracking & Categories

> Categorizing expenses and tracking per-day costs.

---

#### US-EXP-010: Categorize Receipt
**Epic**: E-EXP-02
**Priority**: Should
**Size**: S
**As a** Participant, **I want** to assign a category to a receipt (Accommodation, Groceries, Restaurant, Activity, Transport, Other), **so that** expenses can be analyzed by type.

##### Acceptance Criteria
- **Given** I submit a receipt, **When** I select a category, **Then** the Receipt is tagged with that category.
- **Given** I view the expense summary, **When** I look at category breakdown, **Then** I see totals per category.

##### Technical Notes
- New VO: ExpenseCategory (enum)
- Receipt.category field

---

#### US-EXP-011: Track Per-Day Costs
**Epic**: E-EXP-02
**Priority**: Should
**Size**: M
**As an** Organizer, **I want** to see the total cost per day of the Trip, **so that** I can monitor the budget.

##### Acceptance Criteria
- **Given** receipts have dates, **When** I view the per-day summary, **Then** I see the total expense for each day of the Trip.
- **Given** a day has no receipts, **When** I view the summary, **Then** the day shows EUR 0.00.

##### Technical Notes
- Derived view: Receipts grouped by date
- UI: Table or bar chart

---

#### US-EXP-012: Accommodation Cost Entry
**Epic**: E-EXP-02
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to record the accommodation cost as a single expense with a defined split period, **so that** it is shared among all Participants proportionally.

##### Acceptance Criteria
- **Given** I enter accommodation total cost, **When** I submit, **Then** a Receipt with category=ACCOMMODATION is created.
- **Given** Participants have different StayPeriods, **When** the settlement is calculated, **Then** accommodation cost is split proportionally to the number of nights each Participant stayed.

##### Technical Notes
- Accommodation cost is split by nights (from StayPeriod), not equally
- Cross-reference with Trips SCS for StayPeriod data

---

#### US-EXP-013: Advance Payment (Anzahlung) — Equal Round Amount per TravelParty
**Epic**: E-EXP-02
**Priority**: Should
**Size**: M
**As an** Organizer, **I want** to set a single equal advance payment amount for all TravelParties — auto-suggested from the accommodation cost — **so that** each family pays the same round amount before the trip and that amount is credited in the final settlement.

##### Acceptance Criteria
- **Given** the accommodation price is set and parties are known, **When** the Organizer opens the advance payment form, **Then** the system suggests: `ceil(accommodationPrice / partyCount / 50) × 50`.
- **Given** the suggested amount is shown, **When** the Organizer adjusts it (e.g., adds buffer), **Then** the custom amount is accepted.
- **Given** the Organizer confirms, **Then** the same amount is recorded as advance for EACH travel party.
- **Given** advances are recorded, **When** the settlement is calculated, **Then** each party's advance is credited as a flat amount.

##### Technical Notes
- New entity `AdvancePayment` within Expense aggregate (advancePaymentId, description, amount, paidByPartyId, paidByPartyName, paidAt, paid, createdAt)
- New Domain Service: `AdvancePaymentSuggestion` — formula: `ceil(accommodationCost / partyCount / 50) × 50`
- Same amount for ALL parties — no per-party weighting for advances
- "Bezahlt" toggle per party to track actual payment receipt
- Depends on S9-A (accommodation price) and S9-C (party-level settlement view)

---

### E-EXP-03: Weighting & Splitting

> Defining how expenses are split among Participants.

---

#### US-EXP-020: Define Participant Weighting
**Epic**: E-EXP-03
**Priority**: Must
**Size**: M
**As an** Organizer, **I want** to assign a weighting factor to each Participant (Adult=1.0, Child<3=0.0, Child 3-12=0.5, Part-time=0.5), **so that** expenses are split fairly.

##### Acceptance Criteria
- **Given** a Trip has Participants, **When** I view the weighting page, **Then** I see each Participant with a default weighting (adults=1.0, children based on age).
- **Given** I adjust a weighting (e.g., part-time participant to 0.5), **When** I save, **Then** the weighting is persisted.
- **Given** a child is under 3 years old, **When** the default weighting is computed, **Then** it is 0.0.

##### Technical Notes
- Bounded Context: Expense
- New VO: Weighting (BigDecimal, 0.0-1.0)
- Age computation from Dependent.dateOfBirth relative to Trip.startDate
- Cross-SCS data: ParticipantJoinedTrip includes memberId; age data from TravelParty projection or new event field

---

#### US-EXP-021: Equal Splitting (Default)
**Epic**: E-EXP-03
**Priority**: Must
**Size**: S
**As an** Organizer, **I want** the default expense splitting to be equal (weighted), **so that** costs are shared proportionally by default.

##### Acceptance Criteria
- **Given** no custom split is defined for a Receipt, **When** the settlement is calculated, **Then** the expense is split according to each Participant's weighting factor.

##### Technical Notes
- Default split mode: WEIGHTED_EQUAL

---

#### US-EXP-022: Custom Splitting per Receipt
**Epic**: E-EXP-03
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to define a custom split for a specific receipt (e.g., only some Participants share the cost), **so that** not every expense is shared by everyone.

##### Acceptance Criteria
- **Given** I submit a receipt, **When** I select "Custom Split" and choose which Participants share the cost, **Then** only those Participants are included in the split.
- **Given** a custom split is defined, **When** the settlement is calculated, **Then** only the selected Participants bear the cost (with their respective weights).

##### Technical Notes
- Receipt.splitParticipants: list of participantIds (if empty = all)
- Split modes: WEIGHTED_EQUAL (default), CUSTOM

---

### E-EXP-04: Settlement & Calculation (Abrechnung)

> Computing the final settlement: who owes whom how much.

---

#### US-EXP-030: Calculate Settlement
**Epic**: E-EXP-04
**Priority**: Must
**Size**: L
**As an** Organizer, **I want** to calculate the settlement after a Trip is completed, **so that** everyone knows who owes whom.

##### Acceptance Criteria
- **Given** a Trip is COMPLETED and all Receipts are approved, **When** I click "Calculate Settlement", **Then** the system computes each Participant's share (total expenses x their weight / total weight) and shows the balance per Participant.
- **Given** the settlement is calculated, **When** I view it, **Then** I see: total expenses, per-participant share, per-participant paid amount, and the resulting balance (positive = is owed, negative = owes).

##### Technical Notes
- Bounded Context: Expense
- New Aggregate: Settlement (settlementId, ledgerId, items[])
- SettlementItem: participantId, totalShare, totalPaid, balance
- Algorithm: Standard expense-splitting with weighted shares

---

#### US-EXP-031: View Settlement Summary
**Epic**: E-EXP-04
**Priority**: Must
**Size**: M
**As a** Participant, **I want** to see the settlement summary, **so that** I know how much I owe or am owed.

##### Acceptance Criteria
- **Given** a settlement is calculated, **When** I view it, **Then** I see my balance and a list of transfers (e.g., "Alice pays Bob EUR 45.00").
- **Given** the settlement is optimized, **When** transfers are calculated, **Then** the minimum number of transfers is shown (debt simplification).

##### Technical Notes
- Debt simplification algorithm: minimize the number of transfers
- UI: Summary table + transfer list

---

#### US-EXP-032: Settlement per Category
**Epic**: E-EXP-04
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to see the settlement broken down by expense category, **so that** I understand where the money went.

##### Acceptance Criteria
- **Given** receipts are categorized, **When** I view the settlement, **Then** I see totals per category (Accommodation, Groceries, etc.) and their percentage of total.

##### Technical Notes
- Derived view: Group receipts by category, sum amounts

---

#### US-EXP-033: Export Settlement as PDF
**Epic**: E-EXP-04
**Priority**: Could
**Size**: M
**As an** Organizer, **I want** to export the settlement as a PDF, **so that** I can share it with Participants who don't use the app.

##### Acceptance Criteria
- **Given** a settlement is calculated, **When** I click "Export PDF", **Then** a PDF with the summary, transfers, and category breakdown is generated.

##### Technical Notes
- PDF generation: iText, Apache FOP, or Thymeleaf-to-PDF
- Attachment or download

---

### E-EXP-05: Four-Eyes Review Process

> Two-organizer review process for receipts before they are included in the settlement.

---

#### US-EXP-040: Submit Receipt for Review
**Epic**: E-EXP-05
**Priority**: Must
**Size**: M
**As a** Participant, **I want** my submitted receipt to go through a review process, **so that** incorrect amounts are caught before settlement.

##### Acceptance Criteria
- **Given** I submit a receipt, **When** it is saved, **Then** its status is SUBMITTED (awaiting review).
- **Given** a receipt is SUBMITTED, **When** the submitter views it, **Then** they see "Awaiting review" status.

##### Technical Notes
- Receipt status flow: SUBMITTED -> APPROVED / REJECTED
- The submitter cannot approve their own receipt (four-eyes principle)

---

#### US-EXP-041: Review and Approve Receipt (Four-Eyes)
**Epic**: E-EXP-05
**Priority**: Must
**Size**: M
**As an** Organizer (who is not the submitter), **I want** to review and approve or reject a receipt, **so that** only verified expenses enter the settlement.

##### Acceptance Criteria
- **Given** a Receipt is SUBMITTED, **When** an Organizer (different from the submitter) views the review queue, **Then** they see the receipt photo, OCR result, amount, and description.
- **Given** the Organizer approves, **When** they click "Approve", **Then** the Receipt status becomes APPROVED and it is included in settlement calculations.
- **Given** the Organizer rejects, **When** they click "Reject" with a reason, **Then** the Receipt status becomes REJECTED and the submitter is notified.
- **Given** the submitter is also an Organizer, **When** they try to approve their own receipt, **Then** the system prevents it.

##### Technical Notes
- Bounded Context: Expense
- Receipt status enum: SUBMITTED, APPROVED, REJECTED
- Invariant: reviewer != submitter
- Requires: Multiple Organizers per trip (E-IAM-05)
- reviewedBy, reviewedAt, rejectionReason fields on Receipt

---

#### US-EXP-042: Re-Submit Rejected Receipt
**Epic**: E-EXP-05
**Priority**: Should
**Size**: S
**As a** Participant, **I want** to correct and re-submit a rejected receipt, **so that** it can be reviewed again.

##### Acceptance Criteria
- **Given** my Receipt was REJECTED, **When** I edit and re-submit, **Then** its status returns to SUBMITTED for a new review cycle.

##### Technical Notes
- Receipt status transition: REJECTED -> SUBMITTED (after edit)

---

### E-INFRA-01: CI/CD Pipeline

> Automated build, test, and deployment pipeline.

---

#### US-INFRA-001: GitHub Actions CI Pipeline
**Epic**: E-INFRA-01
**Priority**: Should
**Size**: M
**As a** developer, **I want** an automated CI pipeline that builds and tests all modules on every push, **so that** regressions are caught early.

##### Acceptance Criteria
- **Given** a push to any branch, **When** GitHub Actions runs, **Then** `./mvnw clean verify` succeeds.
- **Given** any test fails, **When** the pipeline runs, **Then** the build is marked as failed.

##### Technical Notes
- GitHub Actions workflow
- QS-M04 reference: Full build < 5 minutes

---

#### US-INFRA-002: Docker Image Build in CI
**Epic**: E-INFRA-01
**Priority**: Could
**Size**: M
**As a** developer, **I want** Docker images for each SCS built automatically in CI, **so that** deployments are consistent.

##### Acceptance Criteria
- **Given** a build succeeds on main, **When** CI completes, **Then** Docker images for gateway, iam, trips, and expense are built and tagged.

##### Technical Notes
- Spring Boot Buildpacks or Dockerfile per module

---

### E-INFRA-02: Observability & Monitoring

> Centralized logging, metrics, and alerting. References: QS-R03, QS-P01, QS-P03.

---

#### US-INFRA-010: Micrometer + Prometheus Metrics
**Epic**: E-INFRA-02
**Priority**: Should
**Size**: M
**As a** developer, **I want** Micrometer metrics exposed via Prometheus endpoints, **so that** I can monitor request latency, event throughput, and DB pool usage.

##### Acceptance Criteria
- **Given** any SCS is running, **When** I query `/actuator/prometheus`, **Then** I get HTTP request metrics, JVM metrics, and HikariCP pool metrics.
- **Given** events are processed, **When** I check metrics, **Then** I see event processing latency timers.

##### Technical Notes
- Dependencies: micrometer-registry-prometheus, spring-boot-starter-actuator
- QS-P01, QS-P03, QS-R02 references

---

#### US-INFRA-011: Grafana Dashboards
**Epic**: E-INFRA-02
**Priority**: Could
**Size**: M
**As a** developer, **I want** Grafana dashboards for key metrics, **so that** I can visualize system health.

##### Acceptance Criteria
- **Given** Prometheus is collecting metrics, **When** I open Grafana, **Then** I see dashboards for request latency (p95), event processing, DB pool usage, and DLQ depth.

##### Technical Notes
- Docker Compose: Add Prometheus + Grafana services
- Pre-configured dashboard JSON files

---

#### US-INFRA-012: Centralized Logging
**Epic**: E-INFRA-02
**Priority**: Could
**Size**: M
**As a** developer, **I want** centralized structured logging across all SCS, **so that** I can trace requests across services.

##### Acceptance Criteria
- **Given** a request flows through Gateway -> SCS, **When** I search logs, **Then** I can correlate logs using a traceId.

##### Technical Notes
- Spring Boot Micrometer Tracing (formerly Sleuth)
- Structured JSON logging with Logback

---

### E-INFRA-03: Architecture Fitness (ArchUnit, JaCoCo)

> Automated enforcement of architectural rules and test coverage. References: QS-M01, QS-M03, QS-M05.

---

#### US-INFRA-020: ArchUnit Tests for Hexagonal Architecture
**Epic**: E-INFRA-03
**Priority**: Must
**Size**: M
**As a** developer, **I want** ArchUnit tests that enforce hexagonal architecture rules, **so that** domain purity is maintained automatically.

##### Acceptance Criteria
- **Given** the domain package exists, **When** ArchUnit runs, **Then** it verifies that domain classes do not import `org.springframework`, `jakarta.persistence`, or adapter packages.
- **Given** a developer adds a Spring annotation to a domain class, **When** tests run, **Then** the build fails.

##### Technical Notes
- ArchUnit dependency in each SCS test scope
- Rules: domain must not depend on adapters, application, or framework packages
- QS-M01, QS-M05 references

---

#### US-INFRA-021: JaCoCo Coverage Thresholds
**Epic**: E-INFRA-03
**Priority**: Should
**Size**: S
**As a** developer, **I want** JaCoCo to enforce per-module test coverage >= 80%, **so that** coverage gaps are visible.

##### Acceptance Criteria
- **Given** a module has < 80% line coverage, **When** `mvn verify` runs, **Then** the build fails with a coverage report.
- **Given** coverage is above threshold, **When** `mvn verify` runs, **Then** the build succeeds and a coverage report is generated.

##### Technical Notes
- JaCoCo Maven plugin with `check` goal
- QS-M03 reference

---

#### US-INFRA-022: Dead Letter Queue Configuration
**Epic**: E-INFRA-03
**Priority**: Must
**Size**: M
**As a** developer, **I want** RabbitMQ Dead Letter Queues configured for all consumer queues, **so that** unprocessable messages are not silently lost.

##### Acceptance Criteria
- **Given** a consumer fails to process a message 3 times, **When** the message is rejected, **Then** it is routed to the DLQ.
- **Given** messages are in the DLQ, **When** I check RabbitMQ Management UI, **Then** I see the failed messages for inspection.

##### Technical Notes
- RabbitMQ: `x-dead-letter-exchange`, `x-dead-letter-routing-key` arguments
- QS-R03 reference

---

### E-INFRA-04: Security Hardening

> Strengthening security: tenant isolation tests, OWASP scanning, security integration tests. References: QS-S01, QS-S02, QS-S03.

---

#### US-INFRA-030: Tenant Isolation Security Tests
**Epic**: E-INFRA-04
**Priority**: Must
**Size**: M
**As a** developer, **I want** security integration tests that verify cross-tenant access returns 404, **so that** tenant isolation is guaranteed.

##### Acceptance Criteria
- **Given** User A belongs to Tenant 1, **When** User A tries to access a resource of Tenant 2, **Then** the response is 404 (not 403).

##### Technical Notes
- Use `@WithMockJwtAuth` or `SecurityMockMvcRequestPostProcessors.jwt()`
- QS-S01 reference

---

#### US-INFRA-031: OWASP Dependency Check in CI
**Epic**: E-INFRA-04
**Priority**: Should
**Size**: S
**As a** developer, **I want** automated OWASP dependency scanning in CI, **so that** vulnerable dependencies are detected early.

##### Acceptance Criteria
- **Given** CI runs, **When** `dependency-check-maven` executes, **Then** any Critical or High CVE fails the build.

##### Technical Notes
- `org.owasp:dependency-check-maven` plugin
- QS-S01 metrics reference

---

#### US-INFRA-032: Security Integration Tests with Mock JWT
**Epic**: E-INFRA-04
**Priority**: Should
**Size**: M
**As a** developer, **I want** integration tests that verify role-based access control, **so that** privilege escalation is prevented.

##### Acceptance Criteria
- **Given** a user has only `participant` role, **When** they try to create a Trip, **Then** the response is 403.
- **Given** a user has `organizer` role, **When** they create a Trip, **Then** it succeeds.

##### Technical Notes
- QS-S03 reference
- `@WithMockJwtAuth` for JWT claims simulation

---

### E-INFRA-05: PWA & Offline Support

> Progressive Web App capabilities for mobile-first and offline access. References: QS-U01, QS-U03.

---

#### US-INFRA-040: Service Worker for Offline Caching
**Epic**: E-INFRA-05
**Priority**: Could
**Size**: XL
**As a** Participant in a cabin with poor internet, **I want** to access previously loaded pages (Shopping List, Trip details) offline, **so that** I can still use the app.

##### Acceptance Criteria
- **Given** I have previously loaded the Shopping List, **When** I lose internet connectivity, **Then** the cached version is displayed.
- **Given** I am offline, **When** I try to make changes, **Then** changes are queued and synced when connectivity returns.

##### Technical Notes
- Service Worker with cache-first strategy for read-heavy pages
- QS-U03 reference
- Significant effort for SSR + HTMX architecture

---

#### US-INFRA-041: PWA Manifest & Install Prompt
**Epic**: E-INFRA-05
**Priority**: Should
**Size**: S
**As a** user, **I want** to install Travelmate as a PWA on my phone's home screen, **so that** it feels like a native app.

##### Acceptance Criteria
- **Given** I visit the app in a mobile browser, **When** the PWA criteria are met, **Then** I see an "Add to Home Screen" prompt.
- **Given** I install the PWA, **When** I open it from the home screen, **Then** it opens in standalone mode with the Travelmate icon and splash screen.

##### Technical Notes
- `manifest.json` with icons, theme color, display: standalone
- QS-U01 reference

---

#### US-INFRA-042: Lighthouse CI Integration
**Epic**: E-INFRA-05
**Priority**: Should
**Size**: M
**As a** developer, **I want** Lighthouse CI to run on key pages, **so that** mobile quality and accessibility are continuously monitored.

##### Acceptance Criteria
- **Given** CI runs, **When** Lighthouse audits key pages, **Then** Mobile score >= 90 and Accessibility score >= 90.

##### Technical Notes
- Lighthouse CI in GitHub Actions
- Key pages: Landing, Dashboard, Trip detail, Shopping List
- QS-U01, QS-U04 references

---

### E-INFRA-06: i18n, Documentation & Quality

> Internationalization, documentation, and general quality.

---

#### US-INFRA-050: i18n (German + English) ✅
**Epic**: E-INFRA-06
**Priority**: Must
**Size**: M
**As a** user, **I want** to switch between German and English, **so that** I can use the app in my preferred language.

##### Acceptance Criteria
- **Given** the app is in German, **When** I switch to English, **Then** all labels and messages are displayed in English.

##### Technical Notes
- Spring MessageSource + LocaleChangeInterceptor
- messages_de.properties + messages_en.properties per SCS

---

#### US-INFRA-051: E2E Tests ✅
**Epic**: E-INFRA-06
**Priority**: Must
**Size**: L
**As a** developer, **I want** Playwright E2E tests covering all critical flows, **so that** regressions are caught before release.

##### Acceptance Criteria
- **Given** the full stack is running, **When** E2E tests execute, **Then** Sign-up, Login, Trip CRUD, Invitation accept/decline, and StayPeriod flows are tested.

##### Technical Notes
- Module: travelmate-e2e, profile: `-Pe2e`
- Playwright Java API

---

#### US-INFRA-052: Arc42 Documentation ✅
**Epic**: E-INFRA-06
**Priority**: Should
**Size**: M
**As a** developer, **I want** up-to-date Arc42 documentation, **so that** architecture decisions and runtime views are documented.

##### Acceptance Criteria
- **Given** a new feature is implemented, **When** the iteration is complete, **Then** relevant Arc42 sections are updated.

##### Technical Notes
- 12 sections in `docs/arc42/`

---

#### US-INFRA-053: Responsive CSS Theme ✅
**Epic**: E-INFRA-06
**Priority**: Should
**Size**: M
**As a** user, **I want** a responsive design that works on mobile and desktop, **so that** the app is usable on any device.

##### Acceptance Criteria
- **Given** I view the app on a 360px viewport, **When** the page loads, **Then** all content is readable and touch targets are >= 44px.

##### Technical Notes
- PicoCSS 2 + custom responsive overrides
- QS-U01 reference

---

#### US-INFRA-054: Event Contract Versioning Strategy
**Epic**: E-INFRA-06
**Priority**: Should
**Size**: M
**As a** developer, **I want** a versioning strategy for event contracts, **so that** breaking changes don't require simultaneous deployment of all SCS.

##### Acceptance Criteria
- **Given** a new field is added to an event, **When** consumers deserialize, **Then** they ignore unknown fields (forward compatibility).
- **Given** an event version is incremented, **When** the old version is received, **Then** it is handled gracefully.

##### Technical Notes
- QS-F03 reference
- Strategy: Add-only fields (backward compatible), version header for breaking changes

---

#### US-INFRA-055: Transactional Outbox Pattern
**Epic**: E-INFRA-06
**Priority**: Could
**Size**: XL
**As a** developer, **I want** event publishing to use the Transactional Outbox pattern, **so that** events are not lost when RabbitMQ is temporarily unavailable.

##### Acceptance Criteria
- **Given** a domain event is registered, **When** the aggregate is saved, **Then** the event is persisted in an outbox table within the same transaction.
- **Given** an event is in the outbox, **When** a polling publisher reads it, **Then** it is published to RabbitMQ and marked as sent.

##### Technical Notes
- Risk mitigation for "Event Loss on Publish"
- Each SCS gets an `outbox` table + scheduled publisher
- Significant effort; deferred to Iteration 6+

---

## Tech Debt Backlog

Items from `docs/arc42/11-risks-and-technical-debt.md`:

| ID | Debt Item | Story Reference | Priority |
|----|-----------|----------------|----------|
| TD-01 | ~~In-Memory Repositories~~ | Resolved in Iteration 2 | ~~Done~~ |
| TD-02 | Missing Expense SCS Implementation | E-EXP-01 through E-EXP-05 | Must |
| TD-03 | Security Disabled in Test Profile | US-INFRA-030, US-INFRA-032 | Should |
| TD-04 | No ArchUnit Tests | US-INFRA-020 | Must |
| TD-05 | No JaCoCo Coverage Enforcement | US-INFRA-021 | Should |
| TD-06 | No Dead Letter Queue Configuration | US-INFRA-022 | Must |
| TD-07 | No OWASP Dependency Scanning | US-INFRA-031 | Should |
| TD-08 | Event Contract Versioning | US-INFRA-054 | Should |
| TD-09 | No Transactional Outbox | US-INFRA-055 | Could |
| TD-10 | Monitoring and Observability | US-INFRA-010, US-INFRA-011, US-INFRA-012 | Should |
| TD-11 | Incomplete Kubernetes Manifests | — (future) | Could |
| TD-12 | ~~Missing E2E Tests~~ | US-INFRA-051 | ~~Done~~ |

---

## Iteration Planning (Suggested)

### Iteration 4 (v0.4.0 — Current, Invitation Flow + Email + Birthday)

| Story | Epic | Priority | Size | Description |
|-------|------|----------|------|-------------|
| US-IAM-070 | E-IAM-01 | Must | M | Birthday Required for All Users |
| US-TRIPS-070 | E-TRIPS-02 | Must | M | Trip Invitation Email Notification |
| US-TRIPS-071 | E-TRIPS-02 | Must | S | Pending Invitations on Trip List Page |
| US-TRIPS-072 | E-TRIPS-02 | Must | L | External Trip Invitation (Invite by Email) |
| US-TRIPS-073 | E-TRIPS-02 | Must | M | Auto-Join Trip on Registration via Invitation |
| US-IAM-071 | E-IAM-06 | Should | S | Improve Keycloak Invitation Email |
| US-INFRA-060 | E-INFRA-06 | Should | M | Close Existing Test Coverage Gaps |

### Iteration 5 (v0.5.0 — Architecture Fitness + Expense Foundation)

| Story | Epic | Priority | Size |
|-------|------|----------|------|
| US-INFRA-020 | E-INFRA-03 | Must | M |
| US-INFRA-021 | E-INFRA-03 | Should | S |
| US-INFRA-022 | E-INFRA-03 | Must | M |
| US-INFRA-030 | E-INFRA-04 | Must | M |
| US-EXP-001 | E-EXP-01 | Must | M |
| US-EXP-002 | E-EXP-01 | Must | XL |
| US-EXP-003 | E-EXP-01 | Must | S |
| US-EXP-004 | E-EXP-01 | Must | M |
| US-EXP-020 | E-EXP-03 | Must | M |
| US-EXP-021 | E-EXP-03 | Must | S |
| US-IAM-040 | E-IAM-05 | Should | M |
| US-IAM-041 | E-IAM-05 | Should | S |

### Iteration 6 (v0.6.0 — Four-Eyes Review + Settlement)

| Story | Epic | Priority | Size |
|-------|------|----------|------|
| US-EXP-040 | E-EXP-05 | Must | M |
| US-EXP-041 | E-EXP-05 | Must | M |
| US-EXP-030 | E-EXP-04 | Must | L |
| US-EXP-031 | E-EXP-04 | Must | M |
| US-EXP-010 | E-EXP-02 | Should | S |
| US-EXP-012 | E-EXP-02 | Must | M |
| US-INFRA-010 | E-INFRA-02 | Should | M |

### Iteration 7 (v0.7.0 — Meal Planning + Recipes)

| Story | Epic | Priority | Size |
|-------|------|----------|------|
| US-TRIPS-030 | E-TRIPS-04 | Must | L |
| US-TRIPS-031 | E-TRIPS-04 | Must | S |
| US-TRIPS-032 | E-TRIPS-04 | Must | S |
| US-TRIPS-033 | E-TRIPS-04 | Must | M |
| US-TRIPS-034 | E-TRIPS-04 | Must | M |
| US-TRIPS-040 | E-TRIPS-05 | Must | M |
| US-TRIPS-041 | E-TRIPS-05 | Should | L |
| US-TRIPS-044 | E-TRIPS-05 | Must | S |

### Iteration 8 (v0.8.0 — Shopping List + Notifications)

| Story | Epic | Priority | Size |
|-------|------|----------|------|
| US-TRIPS-050 | E-TRIPS-06 | Must | L |
| US-TRIPS-051 | E-TRIPS-06 | Must | S |
| US-TRIPS-052 | E-TRIPS-06 | Must | S |
| US-TRIPS-053 | E-TRIPS-06 | Must | S |
| US-TRIPS-054 | E-TRIPS-06 | Should | M |
| US-IAM-050 | E-IAM-06 | Must | M |

### Iteration 9 (v0.9.0 — Accommodation, Party Settlement, Advance Payments, Expense Polish + PWA)

| Story | ID | Epic | Priority | Size | Bounded Context |
|-------|----|------|----------|------|-----------------|
| S9-A: Create Accommodation with Room Inventory | US-TRIPS-060 | E-TRIPS-07 | Must | L | Trips |
| S9-B: Assign Travel Parties to Rooms | US-TRIPS-065 | E-TRIPS-07 | Must | M | Trips |
| S9-C: Party-Level Settlement View | US-EXP-050 | E-EXP-04 | Must | M | Expense |
| S9-D: Advance Payment (Equal Round Amount) | US-EXP-013 | E-EXP-02 | Should | M | Expense |
| S9-E: Re-Submit Rejected Receipt | US-EXP-042 | E-EXP-05 | Should | S | Expense |
| S9-F: PWA Manifest & Install Prompt | US-INFRA-041 | E-INFRA-05 | Should | S | Infrastructure |

### Iteration 10 (v0.10.0 — Accommodation URL Import, Receipt Scan, Settlement Polish)

| Story | ID | Epic | Priority | Size | Bounded Context |
|-------|----|------|----------|------|-----------------|
| S10-A: Accommodation URL Import | US-TRIPS-061 | E-TRIPS-07 | Must | L | Trips |
| S10-B: Kassenzettel-Scan (Receipt Photo OCR) | US-EXP-060 | E-EXP-06 | Should | M | Expense |
| S10-C: Settlement per Category | US-EXP-032 | E-EXP-04 | Could | M | Expense |
| S10-D: Export Settlement as PDF | US-EXP-033 | E-EXP-04 | Could | M | Expense |
| S10-E: Lighthouse CI | US-INFRA-042 | E-INFRA-05 | Should | S | Infrastructure |

### Iteration 11+ (Future — Polls, Bring, Recipe Import, Custom Split, Polish)

| Story | Epic | Priority | Size |
|-------|------|----------|------|
| US-TRIPS-041 | E-TRIPS-05 | Could | L |
| US-TRIPS-062 | E-TRIPS-07 | Could | L |
| US-TRIPS-055 | E-TRIPS-06 | Could | L |
| US-EXP-022 | E-EXP-03 | Could | M |
| US-INFRA-040 | E-INFRA-05 | Could | XL |
| US-INFRA-055 | E-INFRA-06 | Could | XL |
| US-IAM-051 | E-IAM-06 | Could | M |
| US-IAM-052 | E-IAM-06 | Could | S |
| US-IAM-061 | E-IAM-07 | Won't | L |

---

### Important Next Step After Iteration 12

The next high-value Trips slice after Iteration 12 should focus on collaborative trip initialization and decision-making, not on more accounting polish.

| Story | Epic | Priority | Size |
|-------|------|----------|------|
| US-TRIPS-080 | E-TRIPS-08 | Must | L |
| US-TRIPS-081 | E-TRIPS-08 | Must | L |
| US-TRIPS-082 | E-TRIPS-08 | Must | M |
| US-TRIPS-083 | E-TRIPS-08 | Must | L |
| US-TRIPS-084 | E-TRIPS-08 | Must | L |
| US-TRIPS-035 | E-TRIPS-04 | Should | M |

Rationale:
- Once party self-management and live party accounts are in place, the next domain bottleneck is the planning start of a trip.
- Date discovery and accommodation selection both happen before or alongside booking and invitations.
- Kitchen duty extends the existing meal-plan domain with a concrete fairness and responsibility feature.

---

## Story Count Summary

| Bounded Context | Total | Done | Remaining |
|----------------|-------|------|-----------|
| IAM | 18 | 10 | 8 |
| Trips | 30 | 8 | 22 |
| Expense | 16 | 0 | 16 |
| Infrastructure | 15 | 4 | 11 |
| **Total** | **79** | **22** | **57** |

---

## Quality Scenario Cross-Reference

| Quality Scenario | Related Stories |
|-----------------|----------------|
| QS-F01 Domain Integrity | All aggregate stories (domain validation via Assertion) |
| QS-F02 Functional Correctness | US-TRIPS-020, US-EXP-020, US-EXP-030 |
| QS-F03 Event Contract Stability | US-INFRA-054 |
| QS-U01 Mobile-First | US-INFRA-053, US-INFRA-042 |
| QS-U02 Ease of Use | US-IAM-001, US-TRIPS-011 |
| QS-U03 Offline Capability | US-INFRA-040 |
| QS-U04 Accessibility | US-INFRA-042 |
| QS-R01 Event Idempotency | US-INFRA-022 (DLQ), all event consumers |
| QS-R02 Fault Isolation | SCS architecture (inherent) |
| QS-R03 Dead Letter Handling | US-INFRA-022 |
| QS-P01 Page Load Time | US-INFRA-010 |
| QS-P03 Event Processing Latency | US-INFRA-010 |
| QS-S01 Tenant Isolation | US-INFRA-030 |
| QS-S02 JWT Validation | US-IAM-002 (inherent) |
| QS-S03 Role Enforcement | US-INFRA-032 |
| QS-S04 Input Sanitization | Thymeleaf default escaping (inherent) |
| QS-S05 CSRF Protection | Spring Security default (inherent) |
| QS-M01 Framework-Free Domain | US-INFRA-020 |
| QS-M03 Test Coverage | US-INFRA-021 |
| QS-M05 Architectural Conformance | US-INFRA-020 |
| QS-PO1 Container Deployment | US-INFRA-002 |
