Feature: Registration and Login Journey
  As a person who wants to plan trips with their travel party
  I want to register a new Reisepartei and immediately be able to log in
  So that I can start working without unnecessary interruptions

  Background:
    Given the Travelmate application is running
    And Mailpit is reachable and empty

  # ---------- Registration ----------

  @happy-path
  Scenario: New Reisepartei registers and reaches the dashboard in one go
    Given I am on the signup page "/iam/signup"
    When I fill in "Reisepartei Name" with "Familie Müller"
    And I fill in "Vorname" with "Anna"
    And I fill in "Nachname" with "Müller"
    And I fill in "E-Mail" with "anna@example.com"
    And I fill in "Passwort" with "Secure123!"
    And I fill in "Passwort bestätigen" with "Secure123!"
    And I submit the registration form
    Then I see a success page containing "Registrierung erfolgreich"
    And I see a link to log in

  @happy-path
  Scenario: Email verification link leads directly to the Keycloak login page
    Given I have successfully registered with email "anna@example.com"
    When a verification email arrives in Mailpit for "anna@example.com"
    And I click the verification link in that email
    Then I land on the Keycloak login page
    And I do NOT see any intermediate "Verify Email" page
    And I do NOT see any "You may proceed" page
    # This scenario directly catches Bug #1

  @happy-path
  Scenario: After email verification, user logs in and reaches the dashboard
    Given I have clicked the email verification link for "anna@example.com"
    And I am now on the Keycloak login page
    When I enter email "anna@example.com" and password "Secure123!"
    And I click "Anmelden"
    Then I am redirected to "/iam/dashboard"
    And the dashboard shows "Familie Müller"
    And the dashboard shows "Anna"

  @validation
  Scenario: Registration with passwords that do not match shows an error
    Given I am on the signup page "/iam/signup"
    When I fill in all required fields correctly
    And I fill in "Passwort" with "Secure123!"
    And I fill in "Passwort bestätigen" with "Different456!"
    And I submit the registration form
    Then I see a visible error message about password mismatch
    And I am still on the signup page

  @validation
  Scenario: Registration with a missing required field shows a validation error
    Given I am on the signup page "/iam/signup"
    When I leave "E-Mail" empty
    And I submit the registration form
    Then I see a validation error
    And I am still on the signup page

  @validation
  Scenario: Registration with an already-used email shows an error
    Given a Reisepartei already exists with email "existing@example.com"
    When I try to register again with email "existing@example.com"
    And I submit the registration form
    Then I see a visible error message indicating the email is already taken
    And I am still on the signup page

  # ---------- Password Reset ----------

  @happy-path
  Scenario: User requests password reset and lands on the change-password form
    Given I am a registered member with email "anna@example.com"
    And I am on the Keycloak login page
    When I click "Passwort vergessen"
    And I enter my email "anna@example.com"
    And I submit the forgot-password form
    Then a password reset email arrives in Mailpit for "anna@example.com"
    When I click the password reset link in that email
    Then I see a form to enter a new password
    And I do NOT land on the IAM dashboard directly
    # This scenario directly catches Bug #2

  @happy-path
  Scenario: User completes password reset and can log in with the new password
    Given I am on the Keycloak change-password form via a valid reset link
    When I enter "NewSecure456!" as the new password
    And I confirm "NewSecure456!"
    And I submit the form
    Then I see a confirmation that the password was changed
    When I log in with "anna@example.com" and "NewSecure456!"
    Then I am on the IAM dashboard

  # ---------- Login / Logout ----------

  @happy-path
  Scenario: Authenticated user logs out and is sent to the landing page
    Given I am logged in as "anna@example.com"
    When I click "Abmelden"
    Then I am on the landing page
    And I see "Anmelden" and "Registrieren"
    And I do NOT see "dashboard" in the URL

  @security
  Scenario: Unauthenticated access to dashboard redirects to Keycloak login
    Given I am not logged in
    When I navigate to "/iam/dashboard"
    Then I am redirected to the Keycloak login page

  @security
  Scenario: After logout, navigating to dashboard requires re-authentication
    Given I was logged in as "anna@example.com" and then logged out
    When I navigate to "/iam/dashboard"
    Then I am redirected to the Keycloak login page
    And I do NOT see dashboard content
