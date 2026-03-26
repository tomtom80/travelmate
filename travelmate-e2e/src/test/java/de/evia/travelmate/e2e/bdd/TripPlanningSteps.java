package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import java.util.UUID;

import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for 05-trip-planning-and-invitations.feature.
 */
public class TripPlanningSteps {

    private static String currentTripDetailUrl;
    private static String lastTripName;
    private static int invitedMemberCounter = 0;

    @When("I navigate to the new trip page")
    public void iNavigateToTheNewTripPage() {
        navigateAndWait("/trips/new");
    }

    @When("I fill in trip name {string}, start date {string}, end date {string}")
    public void iFillInTripDetails(final String name, final String startDate, final String endDate) {
        lastTripName = name;
        page.fill("input[name=name]", name);
        page.fill("input[name=startDate]", startDate);
        page.fill("input[name=endDate]", endDate);
    }

    @When("I submit the create-trip form")
    public void iSubmitTheCreateTripForm() {
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();
        ensureCurrentTripDetailPage();
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
        final var editButton = page.locator("button.btn-icon[data-dialog-id]").first();
        if (editButton.count() > 0) {
            editButton.click();
            page.locator("dialog[open] input[name=arrivalDate]").fill(arrival);
            page.locator("dialog[open] input[name=departureDate]").fill(departure);
        }
    }

    @When("I save the stay period")
    public void iSaveTheStayPeriod() {
        page.locator("dialog[open] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @When("I add own participant {string} to the trip")
    public void iAddOwnParticipantToTheTrip(final String name) {
        ensureCurrentTripDetailPage();
        final String[] parts = name.split(" ", 2);
        final String firstName = parts[0];
        final String lastName = parts.length > 1 ? parts[1] : "";
        final var participantRow = page.locator("tr", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(name)).first();
        if (participantRow.count() > 0 && participantRow.locator("button.btn-icon").count() > 0) {
            return;
        }
        if (!waitForOwnParticipantOption(name)) {
            ensureOwnCompanionAvailable(name, "2018-01-01");
            if (!waitForOwnParticipantOption(name)) {
                if (tripParticipantExists(extractTripId(currentTripDetailUrl), firstName, lastName)) {
                    navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
                    return;
                }
                throw new AssertionError("Participant option not available on trip detail page: " + name);
            }
        }
        page.selectOption("form[action$='/participants'] select[name=participantId]",
            new SelectOption().setLabel(name));
        page.locator("form[action$='/participants'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitForTripParticipant(extractTripId(currentTripDetailUrl), firstName, lastName);
        navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
    }

    @When("I grant organizer rights to participant {string}")
    public void iGrantOrganizerRightsToParticipant(final String name) {
        ensureOrganizerEligibleParticipantAvailableOnTrip(name, "1992-03-10");
        final var participantRow = waitForParticipantActionRow(name);
        participantRow.waitFor();
        participantRow.locator("details.kebab-menu").evaluate("el => el.open = true");
        final var organizerForm = participantRow.locator("details.kebab-menu form[action*='/organizers/']").first();
        organizerForm.waitFor();
        final String organizerAction = organizerForm.getAttribute("action");
        page.evaluate("""
            action => fetch(action, {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            """, organizerAction);
        page.waitForTimeout(500);
        ensureCurrentTripDetailPage();
        navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
    }

    @Then("the participant entry shows arrival {string} and departure {string}")
    public void theParticipantEntryShowsDates(final String arrival, final String departure) {
        final String content = page.content();
        assertThat(content).contains(arrival);
        assertThat(content).contains(departure);
    }

    @Then("the participant list shows {string}")
    public void theParticipantListShows(final String name) {
        final var participantRow = waitForParticipantActionRow(name);
        assertThat(participantRow.count()).as("Expected participant row for %s", name).isPositive();
    }

    @Then("the participant {string} is marked as organizer")
    public void theParticipantIsMarkedAsOrganizer(final String name) {
        final var participantRow = waitForParticipantActionRow(name);
        for (int i = 0; i < 20; i++) {
            if (participantRow.innerText().contains("Organizer")) {
                return;
            }
            page.waitForTimeout(500);
            ensureCurrentTripDetailPage();
            navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
        }
        assertThat(participantRow.innerText()).contains("Organizer");
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

    private boolean waitForOwnParticipantOption(final String name) {
        ensureCurrentTripDetailPage();
        if (currentTripDetailUrl == null || currentTripDetailUrl.isBlank()) {
            throw new AssertionError("Trip detail URL could not be resolved before checking participant options.");
        }
        for (int i = 0; i < 60; i++) {
            final var select = page.locator("form[action$='/participants'] select[name=participantId]");
            if (select.count() > 0) {
                final var option = select.locator("option").filter(new com.microsoft.playwright.Locator.FilterOptions()
                    .setHasText(name));
                if (option.count() > 0) {
                    return true;
                }
            }
            page.waitForTimeout(500);
            navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
        }
        return false;
    }

    private void ensureCurrentTripDetailPage() {
        if (page.url().matches(".*/trips/[0-9a-fA-F-]+$")) {
            currentTripDetailUrl = page.url();
            return;
        }
        if (lastTripName == null || lastTripName.isBlank()) {
            return;
        }
        for (int i = 0; i < 30; i++) {
            navigateAndWait("/trips/");
            final var tripLink = page.locator("a", new com.microsoft.playwright.Page.LocatorOptions()
                .setHasText(lastTripName)).first();
            if (tripLink.count() > 0) {
                tripLink.click();
                page.waitForLoadState();
                if (page.url().matches(".*/trips/[0-9a-fA-F-]+$")) {
                    currentTripDetailUrl = page.url();
                    return;
                }
            }
            page.waitForTimeout(500);
        }
    }

    private void ensureOrganizerEligibleParticipantAvailableOnTrip(final String name, final String dateOfBirth) {
        final var participantRow = page.locator("tr:has(.action-col)", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(name)).first();
        if (participantRow.count() > 0
            && participantRow.locator("details.action-menu summary").count() > 0) {
            return;
        }

        navigateAndWait("/iam/dashboard");
        final String[] parts = name.split(" ", 2);
        invitedMemberCounter++;
        page.fill("form[hx-post$='/dashboard/members'] input[name=firstName]", parts[0]);
        page.fill("form[hx-post$='/dashboard/members'] input[name=lastName]", parts.length > 1 ? parts[1] : "");
        page.fill("form[hx-post$='/dashboard/members'] input[name=email]",
            "trip-organizer-" + RUN_ID + "-" + invitedMemberCounter + "@e2e.test");
        page.fill("form[hx-post$='/dashboard/members'] input[name=dateOfBirth]", dateOfBirth);
        submitHtmxForm("form[hx-post$='/dashboard/members']");
        assertThat(page.locator("#members").innerHTML()).contains(parts[0]);
        if (parts.length > 1 && !parts[1].isBlank()) {
            assertThat(page.locator("#members").innerHTML()).contains(parts[1]);
        }

        navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
        iAddOwnParticipantToTheTrip(name);
        waitForParticipantActionRow(name);
    }

    private void ensureOwnCompanionAvailable(final String name, final String dateOfBirth) {
        if (DashboardManagementSteps.currentTenantName() == null) {
            return;
        }
        navigateAndWait("/iam/dashboard");
        final String[] parts = name.split(" ", 2);
        page.fill("form[hx-post$='/dashboard/companions'] input[name=firstName]", parts[0]);
        page.fill("form[hx-post$='/dashboard/companions'] input[name=lastName]", parts.length > 1 ? parts[1] : "");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=dateOfBirth]", dateOfBirth);
        submitHtmxForm("form[hx-post$='/dashboard/companions']");
        waitForTripsDependentProjection(DashboardManagementSteps.currentTenantName(), parts[0], parts.length > 1 ? parts[1] : "");
        navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
    }

    private String extractTripId(final String tripDetailUrl) {
        return tripDetailUrl.substring(tripDetailUrl.lastIndexOf('/') + 1);
    }

    private com.microsoft.playwright.Locator waitForParticipantActionRow(final String name) {
        for (int i = 0; i < 30; i++) {
            final var row = page.locator("tr:has(.action-col)", new com.microsoft.playwright.Page.LocatorOptions()
                .setHasText(name)).first();
            if (row.count() > 0) {
                return row;
            }
            page.waitForTimeout(500);
            ensureCurrentTripDetailPage();
            if (currentTripDetailUrl != null) {
                navigateAndWait(currentTripDetailUrl.replace(BASE_URL, ""));
            }
        }
        return page.locator("tr:has(.action-col)", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(name)).first();
    }
}
