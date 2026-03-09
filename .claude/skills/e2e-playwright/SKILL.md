---
name: E2E Playwright Tests
description: "Write Playwright E2E tests in Java following the existing E2ETestBase patterns for the Travelmate project"
user-invocable: false
---

# E2E Playwright Tests Skill

Write Playwright E2E tests following the existing patterns in `travelmate-e2e/`.

## Base Class: E2ETestBase

Available helper methods:
- `signUpAndLogin(email, firstName, lastName, password)` — Full sign-up + email verification + login
- `navigateAndWait(url)` — Navigate and wait for network idle
- `clickAndWaitForHtmx(selector)` — Click element and wait for HTMX swap completion
- `submitHtmxForm(formSelector)` — Submit form and wait for HTMX response
- `waitForTripsReady()` — Wait until Trips SCS has processed IAM events
- `extractVerificationLink(email)` — Get email verification link from Mailpit

Properties:
- `baseUrl` — Gateway URL (http://localhost:8080)
- `iamAdminUrl` — IAM admin endpoint for cleanup
- `mailpitUrl` — Mailpit API URL

## Test Structure

```java
package de.evia.travelmate.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

class FeatureNameIT extends E2ETestBase {

    @Test
    void should_describe_expected_behavior() {
        // Given
        final var testEmail = "test-" + UUID.randomUUID() + "@example.com";
        final var password = "TestPassword123!";
        signUpAndLogin(testEmail, "Test", "User", password);

        // When
        navigateAndWait(baseUrl + "/trips/create");
        page.fill("[name='name']", "Summer Vacation");
        page.fill("[name='startDate']", "2026-07-01");
        page.fill("[name='endDate']", "2026-07-14");
        submitHtmxForm("form[action*='trips']");

        // Then
        assertThat(page.locator("h2").textContent())
            .contains("Summer Vacation");
    }

    @Test
    void should_reject_invalid_input() {
        // Given
        final var testEmail = "test-" + UUID.randomUUID() + "@example.com";
        signUpAndLogin(testEmail, "Test", "User", "TestPassword123!");

        // When — submit empty form
        navigateAndWait(baseUrl + "/trips/create");
        submitHtmxForm("form[action*='trips']");

        // Then — validation error displayed
        assertThat(page.locator(".error, [role='alert']").count())
            .isGreaterThan(0);
    }
}
```

## Rules
- Each test creates its own user (independent, repeatable)
- Use `data-testid` attributes for element selection where possible
- `final` on ALL local variables and parameters
- Wait for HTMX responses, never use `Thread.sleep()`
- Clean up test data via DELETE `/admin/tenants/{id}` in @AfterEach
- Run with: `./mvnw -Pe2e verify`
- Test class naming: `*IT.java` (integration test, Failsafe plugin)

## Location
`travelmate-e2e/src/test/java/de/evia/travelmate/e2e/`
