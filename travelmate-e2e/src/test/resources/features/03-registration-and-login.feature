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

  @happy-path
  Scenario: User completes the full password reset flow and logs in with the new password
    Given I am a registered member with a fresh account
    And I am on the Keycloak login page
    When I request a password reset for my account
    Then a password reset email arrives in Mailpit
    When I follow the reset link and set "NewSecure456!" as the new password
    Then I can log in with "NewSecure456!"
    And the original password no longer grants access

  @security
  Scenario: Password reset with an unregistered email shows a generic response
    Given I am on the Keycloak login page
    When I request a password reset for unknown email "nobody-bdd@nowhere.invalid"
    Then the response is generic with no email enumeration

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
