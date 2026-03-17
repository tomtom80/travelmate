package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import java.util.HashMap;
import java.util.Map;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for 03-registration-and-login.feature.
 */
public class RegistrationLoginSteps {

    private static final Map<String, String> formValues = new HashMap<>();
    private static String registeredEmail;
    private static String registeredPassword;
    private static int scenarioCounter = 0;

    @Given("I am on the signup page {string}")
    public void iAmOnTheSignupPage(final String path) {
        navigateAndWait(path);
    }

    @When("I fill in {string} with a unique value {string}")
    public void iFillInWithAUniqueValue(final String field, final String prefix) {
        final String value = prefix + " " + RUN_ID;
        page.fill("#" + field, value);
        formValues.put(field, value);
    }

    @When("I fill in {string} with {string}")
    public void iFillInWith(final String field, final String value) {
        page.fill("#" + field, value);
        formValues.put(field, value);
    }

    @When("I fill in {string} with a unique email {string}")
    public void iFillInWithAUniqueEmail(final String field, final String prefix) {
        final String email = prefix + "-" + RUN_ID + "@e2e.test";
        page.fill("#" + field, email);
        formValues.put(field, email);
        registeredEmail = email;
    }

    @When("I submit the registration form")
    public void iSubmitTheRegistrationForm() {
        registeredPassword = formValues.getOrDefault("password", "Secure123!");
        page.locator("button[type=submit]").click();
        page.waitForLoadState();
    }

    @Then("I see a success page containing {string}")
    public void iSeeASuccessPageContaining(final String text) {
        assertThat(page.content()).contains(text);
    }

    @Then("I see a link to log in")
    public void iSeeALinkToLogIn() {
        assertThat(page.locator("a[href*='authorization/keycloak'], a[href*='login']").count()).isPositive();
    }

    @Given("I have registered as a new Reisepartei {string}")
    public void iHaveRegisteredAsANewReisepartei(final String tenantName) {
        scenarioCounter++;
        final String suffix = RUN_ID + "-" + scenarioCounter;
        final String uniqueTenant = tenantName + " " + suffix;
        registeredEmail = "reg-" + suffix + "@e2e.test";
        registeredPassword = "Secure123!";

        navigateAndWait("/iam/signup");
        page.fill("#tenantName", uniqueTenant);
        page.fill("#firstName", "Anna");
        page.fill("#lastName", "Mueller");
        page.fill("#dateOfBirth", "1990-01-15");
        page.fill("#email", registeredEmail);
        page.fill("#password", registeredPassword);
        page.fill("#passwordConfirm", registeredPassword);
        page.locator("button[type=submit]").click();
        page.waitForLoadState();
    }

    @When("I log in with the registered credentials")
    public void iLogInWithTheRegisteredCredentials() {
        // Clear cookies to prevent Keycloak SSO auto-login from prior test scenarios
        context.clearCookies();
        // Navigate fresh to the success page's login link via Gateway OAuth2 flow
        navigateAndWait("/oauth2/authorization/keycloak");
        page.waitForURL(url -> url.contains("realms/travelmate"));
        page.fill("#username", registeredEmail);
        page.fill("#password", registeredPassword);
        page.click("#kc-login");
        page.waitForURL(url -> !url.contains("realms/travelmate"));
    }

    @Then("I am redirected to {string}")
    public void iAmRedirectedTo(final String path) {
        assertThat(page.url()).contains(path);
    }

    @Then("the page shows {string}")
    public void thePageShows(final String text) {
        assertThat(page.content()).contains(text);
    }

    @Given("I am logged in as a registered member")
    public void iAmLoggedInAsARegisteredMember() {
        iHaveRegisteredAsANewReisepartei("Familie Logout");
        iLogInWithTheRegisteredCredentials();
    }

    @When("I click the logout button")
    public void iClickTheLogoutButton() {
        page.locator("form[action='/logout'] button[type=submit]").click();
        page.waitForLoadState();
    }

    @Then("I am on the landing page")
    public void iAmOnTheLandingPage() {
        final String url = page.url();
        assertThat(url).satisfiesAnyOf(
            u -> assertThat(u).endsWith("/"),
            u -> assertThat(u).contains("realms/travelmate")
        );
    }

    @Then("the URL does not contain {string}")
    public void theUrlDoesNotContain(final String fragment) {
        assertThat(page.url()).doesNotContain(fragment);
    }

    @Then("I am still on the signup page")
    public void iAmStillOnTheSignupPage() {
        assertThat(page.url()).contains("/signup");
    }

    @When("I try to register again with the same email")
    public void iTryToRegisterAgainWithTheSameEmail() {
        navigateAndWait("/iam/signup");
        page.fill("#tenantName", "Duplicate " + RUN_ID);
        page.fill("#firstName", "Dup");
        page.fill("#lastName", "Test");
        page.fill("#dateOfBirth", "1990-01-15");
        page.fill("#email", registeredEmail);
        page.fill("#password", "Secure123!");
        page.fill("#passwordConfirm", "Secure123!");
    }
}
