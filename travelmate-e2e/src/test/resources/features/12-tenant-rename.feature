Feature: Dashboard — Rename Travel Party
  As an organizer of a Reisepartei
  I want to rename my Reisepartei
  So that invited parties see the correct name

  Background:
    Given the Travelmate application is running

  @happy-path
  Scenario: Organizer renames the travel party via dialog
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    When I click the rename button on the dashboard
    And I change the party name to "Familie Abenteuer"
    And I submit the rename form
    Then the dashboard shows the party name "Familie Abenteuer"
