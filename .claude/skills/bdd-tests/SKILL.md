---
name: BDD Tests
description: "Generate Behaviour-Driven Development test scenarios in Gherkin format from User Stories and acceptance criteria"
user-invocable: false
---

# BDD Tests Skill

Generate Gherkin-style BDD scenarios from User Stories for the Travelmate project.

## Gherkin Template

```gherkin
Feature: [Feature from User Story]
  As a [role from Ubiquitous Language]
  I want [action]
  So that [benefit]

  Background:
    Given a registered travel party "[Reisepartei Name]"
    And an authenticated member "[Mitglied Name]" with role "[organizer/participant]"

  @happy-path
  Scenario: [Happy path description]
    Given [precondition using domain language]
    When [user action]
    Then [expected outcome]
    And [additional assertion]

  @validation
  Scenario: [Validation error case]
    Given [precondition]
    When [action with invalid input]
    Then [error message expectation]

  @security
  Scenario: [Authorization check]
    Given [user with insufficient role/wrong tenant]
    When [action requiring specific permission]
    Then [access denied expectation]

  @boundary
  Scenario Outline: [Boundary value testing]
    Given [precondition]
    When [action with "<parameter>"]
    Then [expectation]

    Examples:
      | parameter | expected |
      | ...       | ...      |
```

## Travelmate Domain Language in Scenarios

Use the Ubiquitous Language (ADR-0011):
- "Reisepartei" / "Travel Party" (not "Tenant")
- "Mitglied" / "Member" (not "Account")
- "Mitreisende(r)" / "Companion" (not "Dependent")
- "Reise" / "Trip"
- "Einladung" / "Invitation"

## Required Scenario Categories
1. **Happy Path** — Normal successful flow
2. **Validation Errors** — Invalid input, missing required fields
3. **Authorization** — Wrong role, wrong tenant
4. **Boundary Values** — Edge cases, limits
5. **Negative Tests** — What should NOT happen
6. **Concurrent Access** — Race conditions (where applicable)

## Output Location
- BDD scenarios → `docs/test-cases/bdd/[feature-name].feature`
