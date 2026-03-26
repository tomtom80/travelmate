package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for 04-dashboard-reisepartei-management.feature.
 * Each scenario creates a fresh login to avoid state leakage between scenarios.
 */
public class DashboardManagementSteps {

    private static final String PASSWORD = "Test1234!";
    private static int scenarioCounter = 0;
    private static String currentTenantName;

    @Given("I am logged in as organizer of a new Reisepartei")
    public void iAmLoggedInAsOrganizerOfANewReisepartei() {
        // Always create a fresh tenant per scenario to avoid state leakage
        scenarioCounter++;
        context.clearCookies();
        final String suffix = RUN_ID + "-" + scenarioCounter;
        final String tenantName = "BDD-Dashboard " + suffix;
        final String email = "bdd-dash-" + suffix + "@e2e.test";
        currentTenantName = tenantName;
        signUpAndLogin(tenantName, "Orga", "Tester", email, PASSWORD);
    }

    @Given("I am on the dashboard")
    public void iAmOnTheDashboard() {
        navigateAndWait("/iam/dashboard");
    }

    @When("I fill in the add-companion form with first name {string}, last name {string}, date of birth {string}")
    public void iFillInTheAddCompanionForm(final String firstName, final String lastName, final String dob) {
        page.fill("form[hx-post$='/dashboard/companions'] input[name=firstName]", firstName);
        page.fill("form[hx-post$='/dashboard/companions'] input[name=lastName]", lastName);
        page.fill("form[hx-post$='/dashboard/companions'] input[name=dateOfBirth]", dob);
    }

    @When("I submit the add-companion form")
    public void iSubmitTheAddCompanionForm() {
        submitHtmxForm("form[hx-post$='/dashboard/companions']");
    }

    @Then("the companion list shows {string}")
    public void theCompanionListShows(final String text) {
        // The companion table has firstName and lastName in separate <td> cells.
        // Check each word individually in the #companions section.
        final String html = page.locator("#companions").innerHTML();
        for (final String part : text.split(" ")) {
            assertThat(html).contains(part);
        }
    }

    @Given("I have added a companion {string} with date of birth {string}")
    public void iHaveAddedACompanion(final String name, final String dob) {
        final String[] parts = name.split(" ", 2);
        iFillInTheAddCompanionForm(parts[0], parts.length > 1 ? parts[1] : "", dob);
        iSubmitTheAddCompanionForm();
        for (int i = 0; i < 20; i++) {
            final String html = page.locator("#companions").innerHTML();
            if (html.contains(parts[0]) && (parts.length == 1 || html.contains(parts[1]))) {
                return;
            }
            page.waitForTimeout(500);
            navigateAndWait("/iam/dashboard");
        }
        theCompanionListShows(name);
    }

    @When("I click the delete button for companion {string}")
    public void iClickTheDeleteButtonForCompanion(final String name) {
        // Override confirm() to auto-accept — avoids Playwright dialog handler issues in Cucumber
        page.evaluate("window.confirm = () => true");
        clickAndWaitForHtmx("#companions tr:has-text('" + name + "') button.btn-icon--danger");
    }

    @Then("{string} is no longer in the companion list")
    public void isNoLongerInTheCompanionList(final String name) {
        assertThat(page.locator("#companions").innerHTML()).doesNotContain(name);
    }

    @When("I fill in the invite-member form with first name {string}, last name {string}, email {string}, date of birth {string}")
    public void iFillInTheInviteMemberForm(final String firstName, final String lastName,
                                            final String email, final String dob) {
        // Make email unique per test run to avoid Keycloak conflicts from prior runs
        final String uniqueEmail = email.replace("@", "-" + RUN_ID + "@");
        page.fill("form[hx-post$='/dashboard/members'] input[name=firstName]", firstName);
        page.fill("form[hx-post$='/dashboard/members'] input[name=lastName]", lastName);
        page.fill("form[hx-post$='/dashboard/members'] input[name=email]", uniqueEmail);
        page.fill("form[hx-post$='/dashboard/members'] input[name=dateOfBirth]", dob);
    }

    @When("I submit the invite-member form")
    public void iSubmitTheInviteMemberForm() {
        submitHtmxForm("form[hx-post$='/dashboard/members']");
    }

    @Then("the member list shows {string}")
    public void theMemberListShows(final String text) {
        // The member table has firstName and lastName in separate <td> cells.
        final String html = page.locator("#members").innerHTML();
        for (final String part : text.split(" ")) {
            assertThat(html).contains(part);
        }
    }

    @Given("I have invited a member with email {string}")
    public void iHaveInvitedAMemberWithEmail(final String email) {
        final String uniqueEmail = email.replace("@", "-" + RUN_ID + "@");
        page.fill("form[hx-post$='/dashboard/members'] input[name=firstName]", "Invited");
        page.fill("form[hx-post$='/dashboard/members'] input[name=lastName]", "Member");
        page.fill("form[hx-post$='/dashboard/members'] input[name=email]", uniqueEmail);
        page.fill("form[hx-post$='/dashboard/members'] input[name=dateOfBirth]", "1990-01-01");
        submitHtmxForm("form[hx-post$='/dashboard/members']");
    }

    @When("I click the delete Reisepartei button")
    public void iClickTheDeleteReiseparteiButton() {
        // Override confirm() to auto-accept — avoids Playwright dialog handler issues in Cucumber
        page.evaluate("window.confirm = () => true");
        page.locator("button[hx-delete$='/dashboard/tenant']").click();
    }

    @When("I confirm the deletion dialog")
    public void iConfirmTheDeletionDialog() {
        // confirm() was overridden to return true — just wait for the response and redirect.
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(1000);
    }

    @Then("I am redirected away from the dashboard")
    public void iAmRedirectedAwayFromTheDashboard() {
        final String url = page.url();
        assertThat(url).satisfiesAnyOf(
            u -> assertThat(u).doesNotContain("/dashboard"),
            u -> assertThat(u).contains("realms/travelmate")
        );
    }

    static String currentTenantName() {
        return currentTenantName;
    }
}
