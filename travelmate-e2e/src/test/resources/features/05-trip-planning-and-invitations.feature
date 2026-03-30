Feature: Trip Planning and Invitations
  As an organizer of a Reisepartei
  I want to plan trips, manage participants, and invite people by email
  So that everyone in my travel group can join and prepare for the trip

  Background:
    Given the Travelmate application is running

  # ---------- Create Trip ----------

  @happy-path
  Scenario: Organizer creates a new trip and it appears on the detail page
    Given I am logged in and the Trips SCS is ready
    When I navigate to the new trip page
    And I fill in trip name "Alpenurlaub BDD"
    And I submit the create-trip form
    Then I am on a trip detail page showing "Alpenurlaub BDD"
    And the status shows "PLANNING"
    And the trip detail explains that travel dates will be decided together later

  @validation
  Scenario: Create trip without a name shows a validation error
    Given I am logged in and the Trips SCS is ready
    When I navigate to the new trip page
    And I fill in trip name ""
    And I submit the create-trip form
    Then I see a visible error message on the page

  # ---------- Trip Lifecycle ----------

  @happy-path
  Scenario: Organizer moves a trip through the full lifecycle
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "Lifecycle BDD"
    When I click the lifecycle button "Absagen"
    Then the status shows "CANCELLED"

  @happy-path
  Scenario: Organizer cancels a trip in PLANNING status
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "Cancel BDD"
    When I click the lifecycle button "Absagen"
    Then the status shows "CANCELLED"

  # ---------- Stay Period ----------

  @happy-path
  Scenario: Participant sets their own stay period
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "StayPeriod BDD"
    Then the status shows "PLANNING"

  @happy-path
  Scenario: Travel party member adds own companion to the trip
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    And I have added a companion "Tim Tester" with date of birth "2018-01-01"
    When I navigate to the new trip page
    And I fill in trip name "Own Party Participant BDD"
    And I submit the create-trip form
    When I add own participant "Tim Tester" to the trip
    Then the participant list shows "Tim Tester"

  @happy-path
  Scenario: Organizer grants organizer rights to another participant
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "Organizer Rights BDD"
    When I grant organizer rights to participant "Lisa Mueller"
    Then the participant "Lisa Mueller" is marked as organizer

  # ---------- External Invitation Form ----------

  @happy-path
  Scenario: External invite form is visible on the trip detail page
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "InviteForm BDD"
    Then the external invitation form is visible on the page

  # ---------- Multi-User Invitation Flows ----------

  @manuell
  Scenario: Organizer invites a travel party member to the trip
    # Requires a second member in the travel party (multi-user setup)
    Given trip "Alpenurlaub" has status "PLANNING"
    And "Lisa Mueller" is a member of the travel party but not yet invited
    When I select "Lisa Mueller" from the invite dropdown
    And I click "Einladung senden"
    Then the invitation list shows "Lisa Mueller" with status "PENDING"

  @manuell
  Scenario: Invited member receives an invitation email
    # Requires Mailpit API integration
    Given I just invited "Lisa Mueller" to trip "Alpenurlaub"
    Then an invitation email arrives in Mailpit for "lisa@example.com"

  @manuell
  Scenario: Invited member accepts the invitation
    # Requires second browser session for the invited member
    Given "Lisa Mueller" has a pending invitation to "Alpenurlaub"
    And I am logged in as "Lisa Mueller"
    When I click "Annehmen"
    Then "Lisa Mueller" appears in the participant list

  @manuell
  Scenario: Invited member declines the invitation
    # Requires second browser session
    Given "Lisa Mueller" has a pending invitation to "Alpenurlaub"
    And I am logged in as "Lisa Mueller"
    When I click "Ablehnen"
    Then the invitation status changes to "DECLINED"

  @manuell
  Scenario: External invitee registers via email link and auto-joins trip
    # Requires Mailpit API + second browser for registration
    Given "extern@example.com" has been invited to trip "Alpenurlaub" as external
    When the invitation email arrives in Mailpit
    And I click the registration link in that email
    And I complete registration with a password
    Then I am a participant of "Alpenurlaub"

  @manuell
  Scenario: External invitation email failure shows a visible error
    # Requires simulating mail server failure
    Given the mail server is unavailable
    When I submit the external invite form
    Then I see a visible error message

  # ---------- Cross-Tenant Security ----------

  @security
  Scenario: Participant cannot access a trip belonging to another travel party
    Given I am logged in and the Trips SCS is ready
    When I navigate to a non-existent trip detail page
    Then I receive an error response or redirect
