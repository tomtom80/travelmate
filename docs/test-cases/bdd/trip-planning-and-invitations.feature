Feature: Trip Planning and Invitations
  As an organizer of a Reisepartei
  I want to plan trips, manage participants, and invite people by email
  So that everyone in my travel group can join and prepare for the trip

  Background:
    Given I am logged in as organizer "Anna Müller" of "Familie Müller"
    And the Trips SCS has processed the TravelParty projection

  # ---------- Create Trip ----------

  @happy-path
  Scenario: Organizer creates a new trip and it appears in the list
    Given I navigate to "/trips/new"
    When I fill in "Name" with "Alpenurlaub 2026"
    And I fill in "Von" with "2026-07-01"
    And I fill in "Bis" with "2026-07-14"
    And I submit the create-trip form
    Then I am redirected to the trip detail page
    And the trip detail shows "Alpenurlaub 2026"
    And the status is "PLANNING"
    And "Anna Müller" appears in the participant list

  @validation
  Scenario: Create trip with end date before start date shows validation error
    Given I navigate to "/trips/new"
    When I fill in "Von" with "2026-07-14"
    And I fill in "Bis" with "2026-07-01"
    And I submit the create-trip form
    Then I see a visible error message about invalid date range

  @validation
  Scenario: Create trip without a name shows a validation error
    Given I navigate to "/trips/new"
    When I leave the name field empty
    And I submit the create-trip form
    Then I see a validation error

  # ---------- Trip Lifecycle ----------

  @happy-path
  Scenario: Organizer moves a trip through the full lifecycle
    Given a trip "Alpenurlaub 2026" exists with status "PLANNING"
    When I click "Bestätigen"
    Then the status changes to "CONFIRMED"
    When I click "Starten"
    Then the status changes to "IN_PROGRESS"
    When I click "Abschließen"
    Then the status changes to "COMPLETED"
    And no lifecycle action buttons are shown

  @happy-path
  Scenario: Organizer cancels a trip in PLANNING status
    Given a trip "Strandurlaub" exists with status "PLANNING"
    When I click "Absagen"
    Then the status changes to "CANCELLED"
    And the confirm and start buttons are no longer shown

  # ---------- Stay Period ----------

  @happy-path
  Scenario: Participant sets their own stay period and it is saved
    Given I am a participant of trip "Alpenurlaub 2026"
    And I am on the trip detail page
    When I set my arrival date to "2026-07-02" and departure date to "2026-07-13"
    And I click "Speichern" for my stay period
    Then the participant list shows arrival "2026-07-02" and departure "2026-07-13" for my entry
    And the save button is visible and its label fits on one line without wrapping
    # The last assertion catches Bug #5 (button grows / label wraps)

  @validation
  Scenario: Stay period with departure before arrival shows a validation error
    Given I am on the trip detail page as a participant
    When I set arrival to "2026-07-13" and departure to "2026-07-02"
    And I click "Speichern"
    Then I see a visible error message about invalid stay period dates

  # ---------- Invite Member of Travel Party ----------

  @happy-path
  Scenario: Organizer invites a travel party member to the trip
    Given trip "Alpenurlaub 2026" has status "PLANNING"
    And "Lisa Müller" is a member of the travel party but not yet invited
    When I am on the trip detail page
    Then the invite-member dropdown contains "Lisa Müller"
    When I select "Lisa Müller" from the dropdown
    And I click "Einladung senden"
    Then the invitation list updates to show "Lisa Müller" with status "PENDING"

  @happy-path
  Scenario: Invited member receives an invitation email
    Given I just invited "Lisa Müller" (lisa@example.com) to trip "Alpenurlaub 2026"
    Then an invitation email arrives in Mailpit for "lisa@example.com"
    And the email mentions "Alpenurlaub 2026"

  @happy-path
  Scenario: Invited member accepts the invitation
    Given "Lisa Müller" has a pending invitation to "Alpenurlaub 2026"
    And I am logged in as "Lisa Müller"
    When I navigate to "/trips/"
    Then I see the pending invitation for "Alpenurlaub 2026"
    When I click "Annehmen"
    Then "Lisa Müller" appears in the participant list for "Alpenurlaub 2026"
    And the invitation status changes from "PENDING" to "ACCEPTED"

  @happy-path
  Scenario: Invited member declines the invitation
    Given "Lisa Müller" has a pending invitation to "Alpenurlaub 2026"
    And I am logged in as "Lisa Müller"
    When I click "Ablehnen"
    Then the invitation status changes to "DECLINED"
    And "Lisa Müller" is NOT in the participant list

  @validation
  Scenario: Organizer cannot invite the same member twice
    Given "Lisa Müller" is already invited to "Alpenurlaub 2026"
    When I try to invite "Lisa Müller" again
    Then I see a visible error message indicating the member is already invited

  # ---------- External Invitation (Per E-Mail einladen) ----------

  @happy-path
  Scenario: External invite form is immediately visible without extra clicks
    Given I am on the trip detail page as organizer
    Then the "Per E-Mail einladen" section is visible without clicking any toggle or details element
    And the email input, first name, last name, and date of birth fields are all visible
    # This catches Bug #4 (form hidden behind collapsible element)

  @happy-path
  Scenario: Organizer invites an external person by email and sees success feedback
    Given I am on the trip detail page as organizer
    When I fill in the "Per E-Mail einladen" form with email "extern@example.com", name "Tom", surname "Extern", birth "1988-04-20"
    And I click "Per E-Mail einladen"
    Then a success message is visible on the page (not hidden, not display:none)
    And the invitation list updates to show "Tom Extern" with status "AWAITING_REGISTRATION"
    And an invitation email arrives in Mailpit for "extern@example.com"
    # Catches Bug #4 (no feedback after submit) and Bug #6 (mail not delivered)

  @happy-path
  Scenario: External invitee receives email, registers, and is automatically added to the trip
    Given "extern@example.com" has been invited to trip "Alpenurlaub 2026" as an external user
    When the invitation email arrives in Mailpit for "extern@example.com"
    And I click the registration link in that email
    Then I see the registration form with the email pre-filled or identified
    When I complete registration with a password
    Then I am logged in as "Tom Extern"
    And I am a participant of "Alpenurlaub 2026" (status automatically ACCEPTED)

  @negative
  Scenario: External invitation email send failure shows a visible error to the organizer
    Given the mail server is unavailable
    When I submit the "Per E-Mail einladen" form
    Then I see a visible error message indicating the invitation could not be sent
    And I am still on the trip detail page
    And the invitation is NOT listed as "AWAITING_REGISTRATION"
    # Catches Bug #7 (MailSendException crashes without user feedback)

  @validation
  Scenario: External invitation with duplicate email shows an error
    Given "extern@example.com" was already invited to "Alpenurlaub 2026"
    When I try to invite "extern@example.com" again via "Per E-Mail einladen"
    Then I see a visible error message indicating this email is already invited

  # ---------- Cross-Tenant Security ----------

  @security
  Scenario: Participant cannot access a trip belonging to another travel party
    Given trip "FremdeReise" belongs to a different Reisepartei
    When I try to navigate directly to its trip detail URL
    Then I receive a 403 Forbidden response or am redirected away
    And I do NOT see any details of "FremdeReise"
