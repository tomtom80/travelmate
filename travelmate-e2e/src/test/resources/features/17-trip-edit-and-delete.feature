Feature: Edit and Delete Trips
  As an organizer
  I want to edit a trip's name and description, or delete a trip in PLANNING status
  So that I can correct mistakes or remove trips that should never have existed

  Background:
    Given the Travelmate application is running

  @happy-path
  Scenario: Organizer edits the trip name and description
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "EditMe BDD"
    When I open the trip edit form
    And I change the trip name to "Edited BDD"
    And I change the trip description to "Aktualisierte Beschreibung BDD"
    And I submit the edit-trip form
    Then I am on a trip detail page showing "Edited BDD"
    And the trip detail explains that travel dates will be decided together later

  @happy-path
  Scenario: Organizer deletes a trip in PLANNING status
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "DeleteMe BDD"
    When I click the trip delete button
    Then I am back on the trip list
    And the trip "DeleteMe BDD" is no longer in the trip list
