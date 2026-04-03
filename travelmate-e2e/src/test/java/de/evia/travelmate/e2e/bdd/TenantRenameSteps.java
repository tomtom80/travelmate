package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TenantRenameSteps {

    @When("I click the rename button on the dashboard")
    public void iClickTheRenameButtonOnTheDashboard() {
        page.locator("#tenant-header button.btn-icon").click();
        page.locator("dialog[open]").waitFor();
    }

    @When("I change the party name to {string}")
    public void iChangeThePartyNameTo(final String newName) {
        page.locator("dialog[open] input[name=name]").fill(newName);
    }

    @When("I submit the rename form")
    public void iSubmitTheRenameForm() {
        page.locator("dialog[open] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForTimeout(500);
    }

    @Then("the dashboard shows the party name {string}")
    public void theDashboardShowsThePartyName(final String name) {
        // Refresh to ensure HTMX swap completed
        navigateAndWait("/iam/dashboard");
        assertThat(page.content()).contains(name);
    }

    @Then("the current trip participant list shows the party name {string}")
    public void theCurrentTripParticipantListShowsThePartyName(final String name) {
        final String tripDetailUrl = TripPlanningSteps.getCurrentTripDetailUrl();
        assertThat(tripDetailUrl).as("Expected current trip detail URL to be available").isNotBlank();

        for (int i = 0; i < 20; i++) {
            navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));
            final String content = page.content();
            if (content.contains("Teilnehmer") && content.contains(name)) {
                return;
            }
            page.waitForTimeout(500);
        }

        navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));
        assertThat(page.content()).contains("Teilnehmer");
        assertThat(page.content()).contains(name);
    }
}
