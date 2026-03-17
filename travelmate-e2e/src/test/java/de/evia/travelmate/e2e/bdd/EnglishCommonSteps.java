package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Shared English step definitions used across multiple feature files (03–07).
 * Manages login state and provides common assertions.
 */
public class EnglishCommonSteps {

    static final String PASSWORD = "Test1234!";

    private static String currentEmail;
    private static String currentTenantName;
    private static boolean loggedIn = false;

    @Given("the Travelmate application is running")
    public void theTravelmateApplicationIsRunning() {
        // Application is assumed to be running — no-op
    }

    @Given("I am logged in and the Trips SCS is ready")
    public void iAmLoggedInAndTripsScsIsReady() {
        if (!loggedIn) {
            // Clear cookies to avoid stale Keycloak sessions from prior scenarios
            context.clearCookies();
            currentTenantName = "BDD-EN " + RUN_ID;
            currentEmail = "bdd-en-" + RUN_ID + "@e2e.test";
            signUpAndLogin(currentTenantName, "Tester", "BDD", currentEmail, PASSWORD);
            waitForTripsReady();
            loggedIn = true;
        }
    }

    @Given("I am not logged in")
    public void iAmNotLoggedIn() {
        context.clearCookies();
        loggedIn = false;
    }

    @Then("I see a visible error message on the page")
    public void iSeeAVisibleErrorMessageOnThePage() {
        final String content = page.content();
        final boolean hasError = content.contains("error") || content.contains("Error")
            || content.contains("Fehler") || content.contains("alert")
            || page.locator("[role=alert]").count() > 0
            || page.locator(".error, .alert, .notification").count() > 0;
        assertThat(hasError).as("Expected a visible error message on the page").isTrue();
    }

    @Then("I am redirected to the Keycloak login page")
    public void iAmRedirectedToTheKeycloakLoginPage() {
        assertThat(page.url()).contains("realms/travelmate");
    }

    @When("I navigate to {string}")
    public void iNavigateTo(final String path) {
        navigateAndWait(path);
    }

    @Then("the page contains {string}")
    public void thePageContains(final String text) {
        assertThat(page.content()).contains(text);
    }

    static String getCurrentEmail() {
        return currentEmail;
    }

    static String getCurrentTenantName() {
        return currentTenantName;
    }

    static void resetLoginState() {
        loggedIn = false;
        currentEmail = null;
        currentTenantName = null;
    }
}
