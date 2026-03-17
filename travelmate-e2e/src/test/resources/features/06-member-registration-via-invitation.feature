@manuell
Feature: Member Registration via Invitation
  As a person who has been invited to join a Reisepartei
  I want to click the link in my invitation email, set a password, and log in
  So that I can join the travel party without interfering with the organizer's session

  # ALL scenarios require multi-browser sessions + Mailpit API integration.
  # Tagged @manuell at feature level — none are automatable in single-browser E2E.

  Background:
    Given "Anna Mueller" is the organizer of "Familie Mueller"
    And Anna has sent a member invitation to "lisa@example.com"

  Scenario: Invitee receives the registration email and can open the form
    When the member invitation email arrives in Mailpit for "lisa@example.com"
    Then the email contains a link to "/iam/register?token="
    When I click that link in a separate browser
    Then I see a registration form for completing my account

  Scenario: Invitee completes registration and can log in independently
    Given I am on the registration form for "lisa@example.com"
    When I enter a password and confirm it
    And I submit the registration form
    Then I can log in with the new credentials
    And the dashboard shows "Familie Mueller"

  Scenario: Completing member registration does not affect the organizer's session
    Given Anna is currently logged in in browser A
    When Lisa opens browser B and completes registration via the invitation link
    Then Anna's session in browser A is unaffected

  Scenario: Registration with mismatched passwords shows an error
    Given I am on the registration form for "lisa@example.com"
    When I enter password "LisaSecure789!" and confirmation "Different999!"
    And I submit the form
    Then I see a visible error about password mismatch

  Scenario: Using an expired or invalid invitation token shows a clear error
    Given I have an invitation link with an invalid or expired token
    When I navigate to that link
    Then I see a clear error page or message

  Scenario: Using the same invitation token twice is rejected
    Given I have already registered using an invitation token
    When I try to use the same invitation link again
    Then I see a clear error indicating the token has already been used

  Scenario: Invitation token cannot be used to register into a different tenant
    Given there is a valid invitation token for tenant "Familie Mueller"
    When an attacker manipulates the registration request to use a different tenantId
    Then the registration is rejected
