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
        // Wait for expense to be created (async via RabbitMQ — TripCompleted → Expense)
        // Increased to 30 retries × 2s = 60s max to handle cold-start container scenarios
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
        assertThat(page.content()).contains(section);
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
            () -> page.locator("dialog button[type=submit]").click()
        );
        page.waitForLoadState(LoadState.NETWORKIDLE);
        receiptAdded = true;
    }

    @Then("the receipt {string} appears in the receipts list")
    public void theReceiptAppearsInTheReceiptsList(final String description) {
        assertThat(page.content()).contains(description);
    }

    @Given("I am on the expense detail page with at least one receipt")
    public void iAmOnTheExpenseDetailPageWithAtLeastOneReceipt() {
        iAmOnTheExpenseDetailPageForACompletedTrip();
        if (!receiptAdded) {
            iAddAReceipt("BDD-Beleg", "25.00", "2026-07-05");
        }
    }

    @When("I click the settle expense button")
    public void iClickTheSettleExpenseButton() {
        page.locator("form[action$='/settle'] button[type=submit]").click();
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

        navigateAndWait("/trips/new");
        page.fill("input[name=name]", fullName);
        page.fill("input[name=startDate]", "2026-07-01");
        page.fill("input[name=endDate]", "2026-07-14");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        // Navigate to trip detail
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(fullName)).click();
        page.waitForLoadState();
        completedTripDetailUrl = page.url();

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
}
