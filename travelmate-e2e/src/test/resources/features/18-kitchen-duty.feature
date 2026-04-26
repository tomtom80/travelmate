Feature: Kitchen Duty Assignment
  As an organizer
  I want to assign kitchen duty for each planned meal
  So that everyone knows who is cooking on which day

  Background:
    Given the Travelmate application is running

  @manuell
  Scenario: Organizer assigns kitchen duty to a planned meal slot
    # Requires generated meal plan (trip with confirmed date range)
    # Covered by domain/service/controller unit tests; full E2E flow needs DatePoll setup
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "Kitchen Duty BDD"
    When I open the meal plan for the current trip
    And I assign myself as kitchen duty for the first meal slot
    Then the first meal slot shows my name as kitchen duty
