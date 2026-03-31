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
 * Step definitions for 07-expense-navigation-and-lifecycle.feature.
 */
public class ExpenseLifecycleSteps {

    private static String completedTripDetailUrl;
    private static String expensePageUrl;
    private static String lastCreatedTripName;
    private static boolean receiptAdded = false;

    @Given("I have a completed trip {string}")
    public void iHaveACompletedTrip(final String tripName) {
        final String fullName = tripName + " " + RUN_ID;
        // Only create a new trip if it differs from the previously created one
        if (!fullName.equals(lastCreatedTripName)) {
            createAndCompleteTrip(tripName);
            expensePageUrl = null; // Reset expense URL for new trip
        }
        // Navigate to trip detail
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(fullName)).click();
        page.waitForLoadState();
        completedTripDetailUrl = page.url();
    }

    @When("I am on the trip detail page")
    public void iAmOnTheTripDetailPage() {
        if (completedTripDetailUrl != null) {
            navigateAndWait(completedTripDetailUrl.replace(BASE_URL, ""));
        }
    }

    @Then("the page contains a link to the expense page")
    public void thePageContainsALinkToTheExpensePage() {
        assertThat(page.locator("a[href*='/expense/']").count()).isPositive();
    }

    @Then("no expense link is visible on the trip detail page")
    public void noExpenseLinkIsVisibleOnTheTripDetailPage() {
        assertThat(page.locator("a[href*='/expense/']").count()).isZero();
    }

    @When("I click the expense link on the trip detail page")
    public void iClickTheExpenseLinkOnTheTripDetailPage() {
        // Wait for expense availability because trip events are consumed asynchronously.
        for (int i = 0; i < 30; i++) {
            if (completedTripDetailUrl != null) {
                navigateAndWait(completedTripDetailUrl.replace(BASE_URL, ""));
            }
            final var expenseLink = page.locator("a[href*='/expense/']");
            if (expenseLink.count() > 0) {
                expenseLink.first().click();
                page.waitForLoadState(LoadState.NETWORKIDLE);
                final String content = page.content();
                if (content.contains("Abrechnung") && !content.contains("404")) {
                    expensePageUrl = page.url();
                    return;
                }
            }
            page.waitForTimeout(2000);
        }
        expensePageUrl = page.url();
    }

    @Then("I am on the expense detail page")
    public void iAmOnTheExpenseDetailPage() {
        assertThat(page.url()).contains("/expense/");
        assertThat(page.content()).contains("Abrechnung");
    }

    @Then("the page shows status {string}")
    public void thePageShowsStatus(final String status) {
        assertThat(page.content()).contains(status);
    }

    @Then("the page shows the section {string}")
    public void thePageShowsTheSection(final String section) {
        assertPageEventuallyContains(section);
    }

    @Given("I am on the expense detail page for a completed trip")
    public void iAmOnTheExpenseDetailPageForACompletedTrip() {
        if (lastCreatedTripName == null) {
            iHaveACompletedTrip("ExpenseDetail-BDD");
        }
        if (expensePageUrl == null) {
            iClickTheExpenseLinkOnTheTripDetailPage();
        } else {
            navigateAndWait(expensePageUrl.replace(BASE_URL, ""));
        }
    }

    @When("I add a receipt with description {string}, amount {string}, date {string}")
    public void iAddAReceipt(final String description, final String amount, final String date) {
        page.locator("button", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText("Beleg hinzufuegen")).first().click();
        page.waitForSelector("dialog[open]");

        page.locator("dialog input[name=description]").fill(description);
        page.locator("dialog input[name=amount]").fill(amount);
        page.locator("dialog input[name=date]").fill(date);
        page.locator("dialog select[name=paidBy]").selectOption(
            new com.microsoft.playwright.options.SelectOption().setIndex(0));

        page.waitForResponse(
            response -> response.url().contains("/expense/"),
            () -> page.locator("#add-receipt-dialog button[type=submit]").click()
        );
        page.waitForLoadState(LoadState.NETWORKIDLE);
        receiptAdded = true;
    }

    @Then("the receipt {string} appears in the receipts list")
    public void theReceiptAppearsInTheReceiptsList(final String description) {
        assertThat(page.content()).contains(description);
    }

    @Then("the page shows the account line {string}")
    public void thePageShowsTheAccountLine(final String label) {
        assertPageEventuallyContains(label);
    }

    @Then("the page shows participant {string} in the party account")
    public void thePageShowsParticipantInThePartyAccount(final String participantName) {
        assertPageEventuallyContains(participantName);
    }

    @And("I set stay period for participant {string} to arrival {string} and departure {string}")
    public void iSetStayPeriodForParticipant(final String participantName,
                                             final String arrival,
                                             final String departure) {
        final String tripDetailUrl = TripPlanningSteps.getCurrentTripDetailUrl();
        final String tripId = extractTripIdFromCurrentTrip();
        ensureDateDecisionReady(tripId);
        for (int i = 0; i < 30; i++) {
            final var participantRow = page.locator("tr:has(.action-col)", new com.microsoft.playwright.Page.LocatorOptions()
                .setHasText(participantName)).first();
            if (participantRow.count() > 0 && participantRow.locator("button.btn-icon[data-dialog-id]").count() > 0) {
                participantRow.locator("button.btn-icon[data-dialog-id]").click();
                page.locator("dialog[open] input[name=arrivalDate]").fill(arrival);
                page.locator("dialog[open] input[name=departureDate]").fill(departure);
                page.locator("dialog[open] button[type=submit]").click();
                page.waitForLoadState(LoadState.NETWORKIDLE);
                return;
            }
            page.waitForTimeout(500);
            if (tripDetailUrl != null) {
                navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));
            }
        }
        final var participantRow = page.locator("tr:has(.action-col)", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(participantName)).first();
        participantRow.locator("button.btn-icon[data-dialog-id]").click();
        page.locator("dialog[open] input[name=arrivalDate]").fill(arrival);
        page.locator("dialog[open] input[name=departureDate]").fill(departure);
        page.locator("dialog[open] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @And("I have added accommodation price {string}")
    public void iHaveAddedAccommodationPrice(final String totalPrice) {
        final String tripId = extractTripIdFromCurrentTrip();
        ensureAccommodationManagementReady(tripId);
        navigateAndWait("/trips/" + tripId + "/accommodation");
        if (page.locator("button:has-text('Unterkunft hinzufuegen')").count() > 0) {
            page.locator("button:has-text('Unterkunft hinzufuegen')").click();
            page.waitForSelector("dialog[open]");
            page.locator("dialog input[name=name]").fill("BDD Expense Unterkunft");
            page.locator("dialog input[name=address]").fill("Testweg 5");
            page.locator("dialog input[name=totalPrice]").fill(totalPrice);
            page.locator("dialog input[name=roomName]").fill("Zimmer 1");
            page.locator("dialog input[name=roomBedCount]").fill("2");
            page.locator("dialog button[type=submit]").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        } else if (page.locator("button[onclick*='edit-accommodation-dialog']").count() > 0) {
            page.locator("button[onclick*='edit-accommodation-dialog']").first().click();
            page.waitForSelector("#edit-accommodation-dialog[open]");
            page.locator("#edit-accommodation-dialog input[name=totalPrice]").fill(totalPrice);
            page.locator("#edit-accommodation-dialog button[type=submit]").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        }
        for (int i = 0; i < 20; i++) {
            final String content = page.content();
            if (content.contains(totalPrice)
                || content.contains("300,00")) {
                return;
            }
            page.waitForTimeout(500);
            navigateAndWait("/trips/" + tripId + "/accommodation");
        }
        assertThat(page.content()).containsAnyOf(totalPrice, "300,00");
    }

    private void ensureAccommodationManagementReady(final String tripId) {
        ensureDateDecisionReady(tripId);

        navigateAndWait("/trips/" + tripId + "/accommodation");
        if (page.locator("button:has-text('Unterkunft hinzufuegen')").count() > 0
            || page.content().contains("BDD Expense Unterkunft")) {
            return;
        }

        if (page.url().contains("/planning") || page.content().contains("Unterkunft abstimmen")) {
            if (page.locator("a[href*='/accommodationpoll/create']").count() > 0) {
                createAndConfirmAccommodationPoll(tripId, "BDD Expense Unterkunft", "BDD Expense Alternative");
            } else {
                navigateAndWait("/trips/" + tripId + "/accommodationpoll");
                if (page.locator("a[href*='/accommodationpoll/create']").count() > 0) {
                    createAndConfirmAccommodationPoll(tripId, "BDD Expense Unterkunft", "BDD Expense Alternative");
                }
            }
        }
    }

    private void ensureDateDecisionReady(final String tripId) {
        navigateAndWait("/trips/" + tripId);
        final String detailContent = page.content();

        if (page.locator("a[href*='/datepoll/create']").count() > 0) {
            createAndConfirmDatePoll(tripId, "2026-07-01", "2026-07-05", "2026-07-02", "2026-07-06");
            navigateAndWait("/trips/" + tripId);
            return;
        }

        if (detailContent.contains("Termin abstimmen") || detailContent.contains("Planung abstimmen")) {
            navigateAndWait("/trips/" + tripId + "/datepoll");
            if (page.locator("a[href*='/datepoll/create']").count() > 0) {
                createAndConfirmDatePoll(tripId, "2026-07-01", "2026-07-05", "2026-07-02", "2026-07-06");
                navigateAndWait("/trips/" + tripId);
            }
        }
    }

    @When("I open the expense page for the current trip")
    public void iOpenTheExpensePageForTheCurrentTrip() {
        final String tripId = extractTripIdFromCurrentTrip();
        navigateAndWait("/expense/" + tripId);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        expensePageUrl = page.url();
    }

    @Then("the page shows amount {string}")
    public void thePageShowsAmount(final String amount) {
        assertPageEventuallyContains(amount);
    }

    @When("I confirm advance payments amount {string}")
    public void iConfirmAdvancePaymentsAmount(final String amount) {
        final String tripId = extractTripIdFromCurrentTrip();
        page.evaluate("""
            ([tripId, amount]) => fetch(`/expense/${tripId}/advance-payments`, {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: `amount=${encodeURIComponent(amount)}`
            })
            """, new Object[] {tripId, amount});
        for (int i = 0; i < 20; i++) {
            navigateAndWait("/expense/" + tripId);
            if (page.content().contains("Kontoverlauf") || page.content().contains("Offene Anzahlung")) {
                expensePageUrl = page.url();
                return;
            }
            page.waitForTimeout(500);
        }
        expensePageUrl = page.url();
    }

    private void assertPageEventuallyContains(final String expectedText) {
        for (int i = 0; i < 20; i++) {
            if (page.content().contains(expectedText)) {
                return;
            }
            page.waitForTimeout(500);
            if (expensePageUrl != null) {
                navigateAndWait(expensePageUrl.replace(BASE_URL, ""));
            } else if (page.url().contains("/expense/")) {
                page.reload(new com.microsoft.playwright.Page.ReloadOptions()
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            }
        }
        assertThat(page.content()).contains(expectedText);
    }

    @When("I toggle the first advance payment as paid")
    public void iToggleTheFirstAdvancePaymentAsPaid() {
        final String action = page.locator("form[action*='/advance-payments/'][action$='/toggle-paid']")
            .first()
            .getAttribute("action");
        page.evaluate("""
            action => fetch(action, {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}
            })
            """, action);
        page.reload(new com.microsoft.playwright.Page.ReloadOptions()
            .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
    }

    @Given("I am on the expense detail page with at least one receipt")
    public void iAmOnTheExpenseDetailPageWithAtLeastOneReceipt() {
        iHaveACompletedTrip("Expense-Settle-BDD");
        expensePageUrl = null;
        iClickTheExpenseLinkOnTheTripDetailPage();
        receiptAdded = false;
        iAddAReceipt("BDD-Beleg", "25.00", "2026-07-05");
    }

    @When("I click the settle expense button")
    public void iClickTheSettleExpenseButton() {
        for (int i = 0; i < 20; i++) {
            final var settleButton = page.locator("form[action$='/settle'] button[type=submit]").first();
            if (settleButton.count() > 0) {
                settleButton.click();
                page.waitForLoadState(LoadState.NETWORKIDLE);
                return;
            }
            page.waitForTimeout(500);
            if (expensePageUrl != null) {
                navigateAndWait(expensePageUrl.replace(BASE_URL, ""));
            } else if (page.url().contains("/expense/")) {
                page.reload(new com.microsoft.playwright.Page.ReloadOptions()
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
            }
        }
        page.locator("form[action$='/settle'] button[type=submit]").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Then("the expense status changes to {string}")
    public void theExpenseStatusChangesTo(final String status) {
        assertThat(page.content()).contains(status);
    }

    @When("I click the back link")
    public void iClickTheBackLink() {
        page.locator("main a[href*='/trips/']").first().click();
        page.waitForLoadState();
    }

    @Then("I am on the trips list page")
    public void iAmOnTheTripsListPage() {
        assertThat(page.url()).contains("/trips/");
    }

    @When("I navigate directly to an expense page")
    public void iNavigateDirectlyToAnExpensePage() {
        navigateAndWait("/expense/" + UUID.randomUUID());
    }

    private void createAndCompleteTrip(final String tripName) {
        final String fullName = tripName + " " + RUN_ID;
        lastCreatedTripName = fullName;

        createTripWithoutDates(fullName);
        final String tripId = openTripFromList(fullName);
        completedTripDetailUrl = page.url();
        createAndConfirmDatePoll(tripId, "2026-07-01", "2026-07-14", "2026-07-02", "2026-07-15");
        createAndConfirmAccommodationPoll(tripId, "BDD Expense Unterkunft", "BDD Expense Alternative");
        iHaveAddedAccommodationPrice("300.00");
        navigateAndWait("/trips/" + tripId);

        // Move through lifecycle
        page.locator("form[action$='/confirm'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.locator("form[action$='/start'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.locator("form[action$='/complete'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Wait a moment for the TripCompleted event to propagate via RabbitMQ to Expense SCS
        page.waitForTimeout(3000);
    }

    private String extractTripIdFromCurrentTrip() {
        final String currentUrl = page.url();
        final String url;
        if (currentUrl.contains("/trips/") || currentUrl.contains("/expense/")) {
            url = currentUrl;
        } else if (TripPlanningSteps.getCurrentTripDetailUrl() != null) {
            url = TripPlanningSteps.getCurrentTripDetailUrl();
        } else {
            url = completedTripDetailUrl;
        }
        return extractTripId(url);
    }
}
