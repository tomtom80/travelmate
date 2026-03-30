Feature: Expense Settlement Navigation and Lifecycle
  As a member of a travel party
  I want to navigate to the expense settlement from the trip detail page
  So that I can track and settle shared costs before, during, and after a trip

  Background:
    Given the Travelmate application is running

  # ---------- Navigation: Trip Detail -> Expense Link ----------

  @happy-path
  Scenario: Completed trip shows an expense link on the detail page
    Given I am logged in and the Trips SCS is ready
    And I have a completed trip "Expense-Nav BDD"
    When I am on the trip detail page
    Then the page contains a link to the expense page

  @navigation
  Scenario: Planning trip already shows an expense link
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "No-Expense BDD"
    Then the page contains a link to the expense page

  # ---------- Expense Detail: Full Journey ----------

  @happy-path @integration
  Scenario: Organizer opens the current trip expense
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "Expense-Full BDD"
    When I am on the trip detail page
    And I click the expense link on the trip detail page
    Then I am on the expense detail page
    And the page shows status "Offen"
    And the page shows the section "Belege"
    And the page shows the section "Reisekonto nach Reisepartei"

  @happy-path @integration
  Scenario: Organizer adds a receipt on the expense page
    Given I am logged in and the Trips SCS is ready
    And I am on the expense detail page for a completed trip
    When I add a receipt with description "Supermarkt", amount "38.90", date "2026-07-05"
    Then the receipt "Supermarkt" appears in the receipts list
    And the page shows the account line "Belegguthaben"

  @happy-path @integration
  Scenario: Accommodation price and stay period change the party account
    Given I am logged in as organizer of a new Reisepartei
    And I am on the dashboard
    And I have added a companion "Tim Tester" with date of birth "2018-01-01"
    And I have created a trip "Expense-Accommodation BDD"
    And I add own participant "Tim Tester" to the trip
    And I set stay period for participant "Tim Tester" to arrival "2026-07-01" and departure "2026-07-03"
    And I have added accommodation price "300"
    When I open the expense page for the current trip
    Then the page shows the section "Reisekonto nach Reisepartei"
    And the page shows the section "Kontoverlauf"
    And the page shows participant "Tim Tester" in the party account
    Then the page shows the account line "Unterkunftsanteil"
    And the page shows amount "300,00"

  @happy-path @integration
  Scenario: Advance payments are visible in the party account
    Given I am logged in and the Trips SCS is ready
    And I have created a trip "Expense-Advance BDD"
    And I have added accommodation price "300"
    When I open the expense page for the current trip
    And I confirm advance payments amount "300"
    Then the page shows the section "Kontoverlauf"
    Then the page shows the account line "Offene Anzahlung"
    And the page shows amount "300,00"
    When I toggle the first advance payment as paid
    Then the page shows the account line "Vorauszahlung"

  @happy-path @integration
  Scenario: Organizer settles the expense after adding receipts
    Given I am logged in and the Trips SCS is ready
    And I am on the expense detail page with at least one receipt
    When I click the settle expense button
    Then the expense status changes to "Abgerechnet"

  @navigation
  Scenario: Back link on expense detail navigates to trip list
    Given I am logged in and the Trips SCS is ready
    And I am on the expense detail page for a completed trip
    When I click the back link
    Then I am on the trips list page

  # ---------- Security ----------

  @security
  Scenario: Unauthenticated access to expense page redirects to login
    Given I am not logged in
    When I navigate directly to an expense page
    Then I am redirected to the Keycloak login page

  @manuell
  Scenario: Member from a different travel party cannot access another party's expense
    # Requires two separate users with different tenants
    Given "Hans Fremder" is a member of a different travel party
    When Hans navigates directly to the expense page of another party's trip
    Then he receives a 403 or 404 error

  # ---------- Edge Cases ----------

  @manuell
  Scenario: Expense with zero receipts cannot be settled
    # Already verified in unit tests; E2E would need precise state setup
    Given I am on an expense page with no receipts
    Then the settle button is NOT visible

  @manuell @gap
  Scenario: Global navigation bar does not contain an Abrechnung link
    # Known product gap — expense is reachable only via trip detail
    Given I am logged in
    When I view any page with the standard navigation layout
    Then the nav bar does NOT contain a direct link to expense

  @manuell @gap
  Scenario: Trip list does not show expense summary column for completed trips
    # Known product gap — no expense column in trip list
    Given I have one completed trip with a settled expense
    When I navigate to the trips list
    Then the trip row shows no expense amount column

  @manuell @gap
  Scenario: IAM dashboard does not provide access to expense settlements
    # Known product gap — no cross-SCS navigation from IAM to Expense
    Given I am on the IAM dashboard
    Then there is no link to any expense page

  @manuell
  Scenario: Async lag — expense not yet created when user navigates
    # Requires precise timing control of event processing
    Given a trip was just completed but the Expense SCS has not processed the event
    When I navigate to the expense page
    Then I see a meaningful error, not a stack trace
