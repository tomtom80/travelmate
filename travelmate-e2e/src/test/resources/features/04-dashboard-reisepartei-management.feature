Feature: Dashboard — Reisepartei Management
  As an organizer of a Reisepartei
  I want to manage my Reisepartei including inviting members, adding companions, and deleting the party
  So that my travel group is correctly set up before we plan trips

  Background:
    Given the Travelmate application is running

  # ---------- Add Companion ----------

  @happy-path
  Scenario: Organizer adds a companion and sees them in the companion list
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    When I fill in the add-companion form with first name "Max", last name "Kind", date of birth "2020-06-15"
    And I submit the add-companion form
    Then the companion list shows "Max Kind"

  @happy-path
  Scenario: Organizer deletes a companion and they disappear from the list
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    And I have added a companion "Lina Kind" with date of birth "2018-03-20"
    When I click the delete button for companion "Lina Kind"
    Then "Lina Kind" is no longer in the companion list

  # ---------- Invite Member ----------

  @happy-path
  Scenario: Organizer invites a new member and sees them in the member list
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    When I fill in the invite-member form with first name "Lisa", last name "Gast", email "lisa-invite@e2e.test", date of birth "1992-03-10"
    And I submit the invite-member form
    Then the member list shows "Lisa Gast"

  @manuell
  Scenario: Registration email contains a working registration link
    # Requires Mailpit API to retrieve email + second browser for registration
    Given a member invitation was sent to "lisa@example.com"
    When the registration email arrives in Mailpit for "lisa@example.com"
    Then the email contains a link containing "/iam/register?token="

  @validation
  Scenario: Inviting the same email twice shows an error
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    And I have invited a member with email "dupe@e2e.test"
    When I fill in the invite-member form with first name "Dupe", last name "Again", email "dupe@e2e.test", date of birth "1990-01-01"
    And I submit the invite-member form
    Then I see a visible error message on the page

  # ---------- Delete Reisepartei ----------

  @happy-path
  Scenario: Organizer deletes the Reisepartei and is logged out
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    When I click the delete Reisepartei button
    And I confirm the deletion dialog
    Then I am redirected away from the dashboard

  @manuell
  Scenario: Tenant deletion failure shows a visible error — not a silent blank screen
    # Requires simulating Keycloak unavailability
    Given I am on the dashboard
    And the backend will fail when deleting the tenant
    When I click "Reisepartei loeschen"
    And I confirm the deletion dialog
    Then I see a visible error message in the danger zone section

  @manuell
  Scenario: Every HTMX action that fails shows a visible error toast or inline message
    # Generic scenario — cannot be automated with a single deterministic test
    Given I trigger any dashboard action that causes a server-side error
    Then a visible error message appears in the page
