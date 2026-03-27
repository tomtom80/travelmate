Feature: Member Registration via Travel Party Invitation
  As a person who has been invited to join an existing Reisepartei
  I want to click the link in my travel-party invitation email, set my password during onboarding, and log in
  So that I can join the travel party without interfering with the organizer's session and independently from trip invitations

  Background:
    Given "Anna Müller" is the organizer of "Familie Müller"
    And Anna has sent a member invitation to "lisa@example.com"

  # ---------- Registration flow ----------

  @happy-path
  Scenario: Invitee receives the registration email and can open the registration form
    When the member invitation email arrives in Mailpit for "lisa@example.com"
    Then the email contains a link to "/iam/register?token="
    When I (as Lisa, unauthenticated) click that link
    Then I see a registration form for completing my account
    And the email field shows "lisa@example.com" (pre-filled or read-only)

  @happy-path
  Scenario: Invitee completes registration and can log in independently
    Given I am on the registration form for "lisa@example.com"
    When I enter a password "LisaSecure789!"
    And I confirm the password
    And I submit the registration form
    Then I see a confirmation page or am redirected to login
    When I log in with "lisa@example.com" and "LisaSecure789!"
    Then I am on the IAM dashboard
    And the dashboard shows "Familie Müller"
    And the dashboard shows "Lisa"

  @happy-path
  Scenario: Travel party invitation onboarding is distinct from trip invitations
    Given I was invited as a member of "Familie Müller" but not yet to any trip
    When I complete registration from the member invitation email
    Then I can log in to Travelmate
    And I am a member of "Familie Müller"
    And I do not automatically join any trip
    And any later trip invitation must be shown and handled separately inside Trips

  @happy-path
  Scenario: Completing member registration does not affect the organizer's session
    Given Anna is currently logged in and on the dashboard
    When Lisa opens a private browser and completes registration via the invitation link
    Then Anna's session is unaffected
    And Anna can continue using the dashboard without being logged out or redirected
    # This covers the requirement that external registration must not interfere

  @validation
  Scenario: Registration form with passwords that do not match shows an error
    Given I am on the registration form for "lisa@example.com"
    When I enter password "LisaSecure789!" and confirmation "Different999!"
    And I submit the form
    Then I see a visible error about password mismatch
    And I am still on the registration form

  @validation
  Scenario: Using an expired or invalid invitation token shows a clear error
    Given I have an invitation link with an invalid or expired token
    When I navigate to that link
    Then I see a clear error page or message
    And I do NOT see the registration form
    And I am given guidance on what to do next (e.g. contact the organizer)

  @negative
  Scenario: Using the same invitation token twice is rejected
    Given I have already successfully registered using an invitation token
    When I try to use the same invitation link again
    Then I see a clear error indicating the token has already been used
    And I am NOT allowed to create a duplicate account

  @security
  Scenario: Invitation token cannot be used to register into a different tenant
    Given there is a valid invitation token for tenant "Familie Müller"
    When an attacker manipulates the registration request to use a different tenantId
    Then the registration is rejected
    And no account is created for the wrong tenant
