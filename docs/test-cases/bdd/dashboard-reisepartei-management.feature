Feature: Dashboard — Reisepartei Management
  As an organizer of a Reisepartei
  I want to manage my Reisepartei including inviting members, adding companions, and deleting the party
  So that my travel group is correctly set up before we plan trips

  Background:
    Given I am logged in as an organizer of "Familie Müller"

  # ---------- Invite Member ----------

  @happy-path
  Scenario: Organizer invites a new member and sees them in the member list
    Given I am on the dashboard
    When I fill in the invite-member form with first name "Lisa", last name "Müller", email "lisa@example.com", date of birth "1992-03-10"
    And I submit the invite-member form
    Then the member list updates to show "Lisa Müller"
    And the member list shows "lisa@example.com"
    And a registration email arrives in Mailpit for "lisa@example.com"
    # The email assertion here catches Bug #6 (mail not reaching Mailpit means mail is broken)

  @happy-path
  Scenario: Registration email contains a working registration link
    Given a member invitation was sent to "lisa@example.com"
    When the registration email arrives in Mailpit for "lisa@example.com"
    Then the email contains a link containing "/iam/register?token="
    When I click that registration link
    Then I see the registration form for "lisa@example.com"
    And the form is pre-filled or labelled with the invited member's name

  @validation
  Scenario: Inviting the same email twice shows an error
    Given "lisa@example.com" is already a member of "Familie Müller"
    When I try to invite "lisa@example.com" again
    And I submit the invite-member form
    Then I see a visible error message in the member section
    And the member list is not duplicated

  @validation
  Scenario: Inviting a member without a date of birth shows a validation error
    Given I am on the dashboard
    When I fill in the invite-member form without a date of birth
    And I submit the invite-member form
    Then I see a validation error indicating date of birth is required

  # ---------- Add Companion ----------

  @happy-path
  Scenario: Organizer adds a companion (Mitreisende/r) and sees them in the companion list
    Given I am on the dashboard
    When I fill in the add-companion form with first name "Max", last name "Müller", date of birth "2020-06-15"
    And I submit the add-companion form
    Then the companion list updates to show "Max Müller"
    And the companion list shows "2020-06-15"

  @happy-path
  Scenario: Organizer deletes a companion and they disappear from the list
    Given "Max Müller" is in the companion list
    When I click the delete button next to "Max Müller"
    And I confirm the deletion dialog
    Then "Max Müller" is no longer in the companion list
    And no error message is shown

  # ---------- Delete Member ----------

  @happy-path
  Scenario: Organizer removes an invited member and they disappear from the list
    Given "Lisa Müller" is in the member list
    When I click the delete button next to "Lisa Müller"
    And I confirm the deletion dialog
    Then "Lisa Müller" is no longer in the member list

  @validation
  Scenario: Organizer cannot delete themselves (last member rule)
    Given I am the only member of "Familie Müller"
    When I click the delete button on my own entry
    And I confirm the deletion dialog
    Then I see a visible error message indicating I cannot delete the last member

  # ---------- Delete Reisepartei ----------

  @happy-path
  Scenario: Organizer deletes the Reisepartei and is logged out
    Given I am on the dashboard
    When I click "Reisepartei löschen" in the danger zone
    And I confirm the deletion dialog
    Then I am redirected to the logout flow
    And the Reisepartei no longer exists
    # This scenario directly catches Bug #3

  @negative
  Scenario: Tenant deletion failure shows a visible error — not a silent blank screen
    Given I am on the dashboard
    And the backend will fail when deleting the tenant (e.g. Keycloak is unavailable)
    When I click "Reisepartei löschen"
    And I confirm the deletion dialog
    Then I see a visible error message in the "Gefahrenzone" section
    And I am still on the dashboard
    And the Reisepartei was NOT deleted
    # This scenario directly catches Bug #3 (silent failure)

  # ---------- General Feedback ----------

  @negative
  Scenario: Every HTMX action that fails shows a visible error toast or inline message
    Given I trigger any dashboard action that causes a server-side error
    Then a visible error message appears in the page
    And the page does not just silently stay the same
    # This covers Bug #8 (general silent failures)
