---
name: travelmate-testing
description: "QA strategy, test case design from User Stories, BDD (Gherkin), and Playwright E2E tests. Use when discussing test coverage, BDD scenarios, or E2E tests."
---

# Travelmate Testing Skill

Quality Assurance expertise for the Travelmate project.

## Core Mandates
- Every Use Case must be covered (not just happy path).
- Scenarios required: validation errors, permissions, negative tests, boundary values.
- Cross-tenant access must be tested (security).

## Specialized Workflows

### 📋 1. Test Case Design (from User Stories)
- TC Structure: Title, Priority, Type (Happy/Negative/Boundary), Preconditions, Steps, Expected Result.
- Covered Acceptance Criteria (traceability).
- **Reference**: See [test-strategy-e2e.md](references/test-strategy-e2e.md).

### 📖 2. Behaviour-Driven Development (BDD)
- Gherkin-style scenarios (Feature, Background, Scenario, Given/When/Then).
- Happy path, validation error, permission denied.
- **Reference**: See [test-strategy-e2e.md](references/test-strategy-e2e.md).

### 🎭 3. Playwright E2E Test Implementation
- Base Class: `E2ETestBase` (Java API).
- Helper methods: `signUpAndLogin()`, `navigateAndWait()`, `clickAndWaitForHtmx()`, `submitHtmxForm()`.
- Mailpit integration for email verification.
- **Reference**: See [test-strategy-e2e.md](references/test-strategy-e2e.md).

### 🏰 4. Test Pyramid Alignment
1. Domain Unit Tests (JUnit 5 + AssertJ).
2. Application Service Tests (Mockito).
3. Persistence Adapter Tests (H2).
4. Controller Tests (MockMvc).
5. E2E Tests (Playwright).
