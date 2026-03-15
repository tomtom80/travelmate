Feature: Expense Settlement Navigation and Lifecycle
  As a member of a travel party
  I want to navigate to the expense settlement from the trip detail page
  So that I can track and settle shared costs after a trip is completed

  Background:
    Given a registered travel party "Bergsteiger e.V."
    And an authenticated member "Alina Bergmann" with role "organizer"
    And a trip "Zugspitz-Tour" with start date 2026-07-01 and end date 2026-07-14

  # ============================================================
  # NAVIGATION: Trip Detail -> Expense Link
  # ============================================================

  @navigation @happy-path
  Scenario: Completed trip shows a link to expense settlement on the detail page
    Given the trip "Zugspitz-Tour" has status "COMPLETED"
    And the expense settlement for "Zugspitz-Tour" has been created
    When Alina navigates to the trip detail page of "Zugspitz-Tour"
    Then the page contains a link to "/expense/{tripId}"
    And the link text matches the i18n key "expense.title" (e.g. "Abrechnung" or "Expense")

  @navigation @negative
  Scenario: In-planning trip does NOT show an expense link on the detail page
    Given the trip "Zugspitz-Tour" has status "PLANNING"
    When Alina navigates to the trip detail page of "Zugspitz-Tour"
    Then no link pointing to "/expense/" is visible on the page

  @navigation @negative
  Scenario: Confirmed trip does NOT show an expense link on the detail page
    Given the trip "Zugspitz-Tour" has status "CONFIRMED"
    When Alina navigates to the trip detail page of "Zugspitz-Tour"
    Then no link pointing to "/expense/" is visible on the page

  @navigation @negative
  Scenario: Cancelled trip does NOT show an expense link on the detail page
    Given the trip "Zugspitz-Tour" has been cancelled
    When Alina navigates to the trip detail page of "Zugspitz-Tour"
    Then no link pointing to "/expense/" is visible on the page

  # ============================================================
  # NAVIGATION: Global Nav Bar -> Expense
  # ============================================================

  @navigation @gap
  Scenario: Global navigation bar does not contain an "Abrechnung" link
    Given Alina is logged in
    When she views any page with the standard navigation layout
    Then the nav bar contains "Reisepartei" and "Reisen"
    But the nav bar does NOT contain a direct link to "/expense/"
    # NOTE: This is a known product gap. Expense is reachable only via trip detail.
    # A future iteration should add an "Abrechnung" nav entry or a dashboard widget.

  # ============================================================
  # NAVIGATION: Trip List -> Expense Shortcut (future)
  # ============================================================

  @navigation @gap
  Scenario: Trip list does not show expense summary column for completed trips
    Given Alina has one completed trip with a settled expense
    When she navigates to the trips list "/trips/"
    Then the trip row for the completed trip shows no total expense amount
    And there is no direct "Abrechnung" link in the trip row
    # NOTE: This is a known product gap. The trip list table has columns:
    # Name, Von, Bis, Status — no expense column exists yet.

  # ============================================================
  # NAVIGATION: IAM Dashboard -> Expense (future)
  # ============================================================

  @navigation @gap
  Scenario: IAM dashboard does not provide access to expense settlements
    Given Alina is on her dashboard at "/iam/dashboard"
    When she looks for a way to reach expense settlements
    Then there is no link to "/expense/" on the dashboard
    # NOTE: The IAM dashboard only manages Travel Party (members, companions, danger zone).
    # No cross-SCS navigation to Expense exists from the IAM SCS.

  # ============================================================
  # EXPENSE DETAIL: Integrated user journey after trip completion
  # ============================================================

  @happy-path @integration
  Scenario: Organizer completes a trip and then views the auto-created expense settlement
    Given Alina has a trip "Zugspitz-Tour" in status "IN_PROGRESS"
    When she clicks "Abschliessen" on the trip detail page
    And the page reloads showing status "Abgeschlossen"
    And she clicks the "Abrechnung" link on the trip detail page
    Then she is navigated to "/expense/{tripId}"
    And the page shows the expense with status "Offen"
    And the page shows the section "Belege"
    And the page shows the section "Gewichtung"
    And the page shows the section "Saldo"
    And the page shows the trip name "Zugspitz-Tour" as heading

  @happy-path @integration
  Scenario: Organizer adds a receipt on the expense page after navigation from trip detail
    Given the trip "Zugspitz-Tour" is completed and Alina is on the expense detail page
    When she clicks "Beleg hinzufuegen"
    And she fills in description "Supermarkt" amount "38.90" date "2026-07-05"
    And she selects herself as "Bezahlt von"
    And she submits the dialog
    Then the receipt "Supermarkt" appears in the receipts list
    And the toast notification confirms success
    And the "Saldo" section is updated

  @happy-path @integration
  Scenario: Organizer settles the expense after adding receipts
    Given Alina is on the expense detail page for "Zugspitz-Tour"
    And there is at least one receipt recorded
    When she clicks "Abrechnung abschliessen"
    Then the expense status changes to "Abgerechnet"
    And the "Beleg hinzufuegen" button is no longer visible
    And the "Abrechnung abschliessen" button is no longer visible

  @navigation
  Scenario: Back link on expense detail navigates to trip list
    Given Alina is on the expense detail page for "Zugspitz-Tour"
    When she clicks the "Zurueck" link
    Then she is navigated to "/trips/"
    # Verified from expense/detail.html: <a href="/trips/" th:text="#{common.back}">

  # ============================================================
  # PERMISSIONS / AUTHORIZATION
  # ============================================================

  @security
  Scenario: Unauthenticated user is redirected to login when accessing expense page
    Given no user is logged in
    When the user navigates directly to "/expense/{anyTripId}"
    Then the user is redirected to the Keycloak login page

  @security
  Scenario: Member from a different travel party cannot access another party's expense
    Given "Hans Fremder" is a member of a different travel party
    And a trip "Zugspitz-Tour" belongs to "Bergsteiger e.V."
    And the expense for "Zugspitz-Tour" exists
    When Hans navigates directly to "/expense/{tripId}"
    Then he receives a 403 or 404 error
    # The TripProjection resolves tenantId from the tripId. If Hans has a valid JWT
    # but his tenantId doesn't match the projection's tenantId, access must be denied.
    # NOTE: Current implementation in ExpenseController does NOT explicitly validate
    # that the requesting user's tenantId matches the TripProjection tenantId.
    # This is a potential security gap — the controller resolves tenantId solely from
    # the TripProjection, not from the JWT. Cross-tenant read access may be possible.

  @security
  Scenario: Participant (non-organizer) can view expense settlement for their trip
    Given "Ben Teilnehmer" has role "participant" and was invited to "Zugspitz-Tour"
    And the trip is completed with an expense
    When Ben navigates to "/expense/{tripId}"
    Then he can see the expense detail page
    And the "Abrechnung abschliessen" button is visible (if expense is OPEN)
    # NOTE: The current UI does not distinguish organizer vs participant on expense page.
    # All authenticated users with access to the TripProjection see the same buttons.

  # ============================================================
  # EDGE CASES
  # ============================================================

  @edge-case
  Scenario: User navigates to expense of a trip that has no expense record yet (async lag)
    Given the trip "Zugspitz-Tour" was just completed (TripCompleted event published)
    But the Expense SCS has not yet processed the TripCompleted event (async lag)
    When Alina navigates directly to "/expense/{tripId}"
    Then she receives a meaningful error (404 or a "not yet available" message)
    And not a stack trace or Whitelabel Error Page
    # The ExpenseService.findByTripId with createIfMissing=true should handle this,
    # but the TripProjection might also be missing if the event hasn't been consumed.

  @edge-case
  Scenario: TripProjection is missing for a valid tripId (messaging lag or data loss)
    Given a tripId is known but no TripProjection exists in the Expense SCS database
    When any user navigates to "/expense/{tripId}"
    Then the response is HTTP 404
    And the error page is the styled 404.html (not Whitelabel)
    # Covered at controller level by: findProjection() throws ResponseStatusException(NOT_FOUND)
    # But the GlobalExceptionHandler must map this to the 404.html template.

  @edge-case
  Scenario: User with multiple completed trips sees correct expense per trip
    Given Alina has two completed trips: "Zugspitz-Tour" (tripId=A) and "Bodensee-Reise" (tripId=B)
    And each trip has its own expense record
    When she navigates from the "Zugspitz-Tour" detail page to its expense
    Then the expense page shows "Zugspitz-Tour" as the trip name
    When she navigates back and then to "Bodensee-Reise" expense
    Then the expense page shows "Bodensee-Reise" as the trip name
    And the receipts shown belong only to "Bodensee-Reise"

  @edge-case
  Scenario: Expense with zero receipts cannot be settled
    Given Alina is on the expense detail page for "Zugspitz-Tour"
    And no receipts have been added
    Then the "Abrechnung abschliessen" button is NOT visible
    # Verified from expense/detail.html:
    # th:if="${expense.status().name() == 'OPEN' && !expense.receipts().isEmpty()}"

  @boundary
  Scenario Outline: Receipt amount boundary values
    Given Alina is on an open expense page
    When she tries to add a receipt with amount "<amount>"
    Then the form "<result>"

    Examples:
      | amount | result                                    |
      | 0.00   | rejects — amount must be > 0 (min="0.01") |
      | 0.01   | accepts — minimum valid amount            |
      | -1.00  | rejects — negative amount not allowed     |
      | 99999.99 | accepts — large but valid amount        |

  # ============================================================
  # ACCEPTANCE CRITERIA: "Expense is reachable from the UI"
  # ============================================================
  # AC-01: A completed trip's detail page MUST show a link to /expense/{tripId}
  # AC-02: The link MUST NOT be shown for trips in PLANNING, CONFIRMED, IN_PROGRESS, or CANCELLED status
  # AC-03: Following the link MUST load the expense detail page without error
  # AC-04: The expense detail page MUST display the correct trip name
  # AC-05: The expense detail page back link MUST navigate to /trips/
  # AC-06: An unauthenticated request to /expense/{id} MUST redirect to login
  # AC-07: Cross-party access to /expense/{id} MUST return 403 or 404 (no data leakage)
  # AC-08: A missing TripProjection MUST return HTTP 404 with a styled error page, not a stack trace
