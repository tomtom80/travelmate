# Testing & E2E Reference

## Test Case Structure
- **Priority**: Critical / High / Medium / Low.
- **Type**: Happy Path / Negative / Boundary / Permission / Edge Case.
- **Preconditions**: Required setup.
- **Steps**: Actions to perform.
- **Expected Result**: Assertion and expected outcome.

## BDD Scenario Format
```gherkin
Feature: [Feature Name]
  Scenario: [Scenario Name]
    Given [precondition]
    When [action]
    Then [expected result]
```

## Playwright E2E Patterns (`E2ETestBase`)
- **Setup**: `signUpAndLogin(email, first, last, password)`.
- **Navigation**: `navigateAndWait(url)`.
- **HTMX Interactions**:
  - `clickAndWaitForHtmx(selector)`: Click and wait for HTMX swap.
  - `submitHtmxForm(selector)`: Submit form and wait for response.
- **Cleanup**: Handled via `DELETE /admin/tenants/{id}` in `@AfterEach`.
- **Selectors**: Prefer `data-testid` attributes.

## E2E Rules
- Independent tests (own user/tenant).
- Wait for HTMX responses (no arbitrary sleeps).
- Full Docker Compose infrastructure (`./mvnw -Pe2e verify`).
