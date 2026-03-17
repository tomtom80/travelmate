Feature: Registration and Login Journey
  As a person who wants to plan trips with their travel party
  I want to register a new Reisepartei and immediately be able to log in
  So that I can start working without unnecessary interruptions

  Background:
    Given the Travelmate application is running

  # ---------- Registration ----------

  @happy-path
  Scenario: New Reisepartei registers and sees success page
    Given I am on the signup page "/iam/signup"
    When I fill in "tenantName" with a unique value "Familie E2E-Reg"
    And I fill in "firstName" with "Anna"
    And I fill in "lastName" with "Mueller"
    And I fill in "dateOfBirth" with "1990-01-15"
    And I fill in "email" with a unique email "reg"
    And I fill in "password" with "Secure123!"
    And I fill in "passwordConfirm" with "Secure123!"
    And I submit the registration form
    Then I see a success page containing "Registrierung erfolgreich"
    And I see a link to log in

  @happy-path
  Scenario: After registration, user logs in and reaches the dashboard
    Given I have registered as a new Reisepartei "Familie E2E-Login"
    When I log in with the registered credentials
    Then I am redirected to "/iam/dashboard"
    And the page shows "Familie E2E-Login"

  @manuell
  Scenario: Email verification link leads directly to the Keycloak login page
    # Email verification is currently SKIPPED (emailVerified=true on signup)
    Given I have successfully registered with email "anna@example.com"
    When a verification email arrives in Mailpit for "anna@example.com"
    And I click the verification link in that email
    Then I land on the Keycloak login page

  @validation
  Scenario: Registration with passwords that do not match shows an error
    Given I am on the signup page "/iam/signup"
    When I fill in "tenantName" with a unique value "Familie Mismatch"
    And I fill in "firstName" with "Test"
    And I fill in "lastName" with "User"
    And I fill in "dateOfBirth" with "1990-01-15"
    And I fill in "email" with a unique email "mismatch"
    And I fill in "password" with "Secure123!"
    And I fill in "passwordConfirm" with "Different456!"
    And I submit the registration form
    Then I see a visible error message on the page
    And I am still on the signup page

  @validation
  Scenario: Registration with an already-used email shows an error
    Given I have registered as a new Reisepartei "Familie Duplicate"
    When I try to register again with the same email
    And I submit the registration form
    Then I see a visible error message on the page
    And I am still on the signup page

  # ---------- Password Reset ----------

  @manuell
  Scenario: User requests password reset and lands on the change-password form
    # Requires Mailpit API + Keycloak forgot-password flow
    Given I am a registered member with email "anna@example.com"
    And I am on the Keycloak login page
    When I click "Passwort vergessen"
    And I enter my email "anna@example.com"
    And I submit the forgot-password form
    Then a password reset email arrives in Mailpit for "anna@example.com"

  @manuell
  Scenario: User completes password reset and can log in with the new password
    # Requires Mailpit API + Keycloak change-password form
    Given I am on the Keycloak change-password form via a valid reset link
    When I enter "NewSecure456!" as the new password
    And I confirm "NewSecure456!"
    And I submit the form
    Then I see a confirmation that the password was changed

  # ---------- Login / Logout ----------

  @happy-path
  Scenario: Authenticated user logs out and is sent to the landing page
    Given I am logged in as a registered member
    When I click the logout button
    Then I am on the landing page
    And the page contains "Anmelden"
    And the URL does not contain "dashboard"

  @security
  Scenario: Unauthenticated access to dashboard redirects to Keycloak login
    Given I am not logged in
    When I navigate to "/iam/dashboard"
    Then I am redirected to the Keycloak login page

  @security
  Scenario: After logout, navigating to dashboard requires re-authentication
    Given I am logged in as a registered member
    When I click the logout button
    And I navigate to "/iam/dashboard"
    Then I am redirected to the Keycloak login page
