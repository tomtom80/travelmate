package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import java.util.UUID;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for 05-trip-planning-and-invitations.feature.
 */
public class TripPlanningSteps {

    private static String currentTripDetailUrl;

    @When("I navigate to the new trip page")
    public void iNavigateToTheNewTripPage() {
        navigateAndWait("/trips/new");
    }

    @When("I fill in trip name {string}, start date {string}, end date {string}")
    public void iFillInTripDetails(final String name, final String startDate, final String endDate) {
        page.fill("input[name=name]", name);
        page.fill("input[name=startDate]", startDate);
        page.fill("input[name=endDate]", endDate);
    }

    @When("I submit the create-trip form")
    public void iSubmitTheCreateTripForm() {
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();
    }

    @Then("I am on a trip detail page showing {string}")
    public void iAmOnATripDetailPageShowing(final String tripName) {
        currentTripDetailUrl = page.url();
        assertThat(page.content()).contains(tripName);
    }

    @Then("the status shows {string}")
    public void theStatusShows(final String status) {
        final String content = page.content();
        // Status may be displayed in German (i18n)
        final boolean found = content.contains(status) || statusMatchesI18n(content, status);
        assertThat(found).as("Expected status '%s' on the page", status).isTrue();
    }

    @Given("I have created a trip {string} from {string} to {string}")
    public void iHaveCreatedATrip(final String name, final String startDate, final String endDate) {
        navigateAndWait("/trips/new");
        page.fill("input[name=name]", name + " " + RUN_ID);
        page.fill("input[name=startDate]", startDate);
        page.fill("input[name=endDate]", endDate);
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        // Navigate to the trip detail page
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(name + " " + RUN_ID)).click();
        page.waitForLoadState();
        currentTripDetailUrl = page.url();
    }

    @When("I click the lifecycle button {string}")
    public void iClickTheLifecycleButton(final String buttonText) {
        final String action = mapLifecycleAction(buttonText);
        page.locator("form[action$='/" + action + "'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @When("I set my arrival date to {string} and departure to {string}")
    public void iSetMyArrivalAndDeparture(final String arrival, final String departure) {
        if (page.locator("form[action*='/stay-period']").count() > 0) {
            page.locator("form[action*='/stay-period'] input[name=arrivalDate]").fill(arrival);
            page.locator("form[action*='/stay-period'] input[name=departureDate]").fill(departure);
        }
    }

    @When("I save the stay period")
    public void iSaveTheStayPeriod() {
        page.locator("form[action*='/stay-period'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Then("the participant entry shows arrival {string} and departure {string}")
    public void theParticipantEntryShowsDates(final String arrival, final String departure) {
        final String content = page.content();
        assertThat(content).contains(arrival);
        assertThat(content).contains(departure);
    }

    @Then("the external invitation form is visible on the page")
    public void theExternalInvitationFormIsVisibleOnThePage() {
        final int formCount = page.locator("form[action*='/invitations/external'], form[hx-post*='/invitations/external']").count();
        assertThat(formCount).as("External invitation form should be visible").isPositive();
    }

    @When("I navigate to a non-existent trip detail page")
    public void iNavigateToANonExistentTripDetailPage() {
        navigateAndWait("/trips/" + UUID.randomUUID());
    }

    @Then("I receive an error response or redirect")
    public void iReceiveAnErrorResponseOrRedirect() {
        final String content = page.content();
        final String url = page.url();
        final boolean isErrorOrRedirect = content.contains("404") || content.contains("Not Found")
            || content.contains("Forbidden") || content.contains("403")
            || content.contains("Fehler") || content.contains("Error")
            || content.contains("Whitelabel") || content.contains("Zugriff")
            || content.contains("Nicht gefunden") || content.contains("nicht gefunden")
            || url.endsWith("/trips/") || url.contains("realms/travelmate");
        assertThat(isErrorOrRedirect).as("Expected an error or redirect for non-existent trip").isTrue();
    }

    private boolean statusMatchesI18n(final String content, final String status) {
        return switch (status) {
            case "PLANNING" -> content.contains("In Planung") || content.contains("Planung");
            case "CONFIRMED" -> content.contains("Bestaetigt") || content.contains("Bestätigt");
            case "IN_PROGRESS" -> content.contains("Laeuft") || content.contains("Läuft");
            case "COMPLETED" -> content.contains("Abgeschlossen");
            case "CANCELLED" -> content.contains("Abgesagt");
            default -> false;
        };
    }

    private String mapLifecycleAction(final String buttonText) {
        return switch (buttonText) {
            case "Bestaetigen" -> "confirm";
            case "Starten" -> "start";
            case "Abschliessen" -> "complete";
            case "Absagen" -> "cancel";
            default -> buttonText.toLowerCase();
        };
    }

    static String getCurrentTripDetailUrl() {
        return currentTripDetailUrl;
    }
}
