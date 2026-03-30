Feature: Trip Recipes — Shared recipe pool within a trip
  As a trip participant
  I want to contribute recipes to a trip
  So that the organizer can use them in the meal plan

  Background:
    Given the Travelmate application is running

  @happy-path
  Scenario: Trip detail page shows a recipes feature card
    Given I am logged in as organizer of a new Reisepartei
    And I have created a trip "Sommerurlaub"
    When I navigate to the trip detail page
    Then I see a "Rezepte" feature card

  @happy-path
  Scenario: Create a recipe directly in the trip
    Given I am on the trip recipes page
    When I click "Neues Rezept"
    And I enter recipe name "Grillgemuese" with servings "4"
    And I add ingredient "Zucchini" quantity "2" unit "Stueck"
    And I submit the recipe form
    Then "Grillgemuese" appears in the trip recipe list

  @happy-path
  Scenario: Share a personal recipe into the trip
    Given I have a personal recipe "Kartoffelsalat"
    And I am on the trip recipes page
    When I expand the share section
    And I click share for "Kartoffelsalat"
    Then "Kartoffelsalat" appears in the trip recipe list
