package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpenseLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Expense " + RUN_ID;
    private static final String EMAIL = "expense-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Abrechnungsreise " + RUN_ID;

    private static String tripDetailUrl;

    @Test
    @Order(1)
    void setUpTravelPartyAndCreateTrip() {
        signUpAndLogin(TENANT_NAME, "Eva", "Expense", EMAIL, PASSWORD);
        waitForTripsReady();

        navigateAndWait("/trips/new");
        page.fill("input[name=name]", TRIP_NAME);
        page.fill("input[name=startDate]", "2026-08-01");
        page.fill("input[name=endDate]", "2026-08-14");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.content()).contains(TRIP_NAME);
    }

    @Test
    @Order(2)
    void completeTripToTriggerExpenseCreation() {
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();

        tripDetailUrl = page.url();

        page.locator("form[action$='/confirm'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        page.locator("form[action$='/start'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        page.locator("form[action$='/complete'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        assertThat(page.content()).contains("Abgeschlossen");
    }

    @Test
    @Order(5)
    void tripDetailShowsExpenseLinkForCompletedTrip() {
        navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));
        page.waitForLoadState();

        final String content = page.content();
        assertThat(content).contains("Abgeschlossen");
        assertThat(page.locator("a[href*='/expense/']").count()).isPositive();
    }

    @Test
    @Order(6)
    void tripListShowsExpenseLinkForCompletedTrip() {
        navigateAndWait("/trips/");

        final var row = page.locator("tr", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(TRIP_NAME));
        assertThat(row.locator("a[href*='/expense/']").count()).isPositive();
    }

    @Test
    @Order(10)
    void expensePageLoadsViaNavigationLink() {
        navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));

        for (int i = 0; i < 15; i++) {
            final var expenseLink = page.locator("a[href*='/expense/']");
            if (expenseLink.count() > 0) {
                expenseLink.first().click();
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
                final String content = page.content();
                if (content.contains("Abrechnung") && !content.contains("404") && !content.contains("Not Found")) {
                    break;
                }
            }
            page.waitForTimeout(1000);
            navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));
        }

        final String content = page.content();
        assertThat(content).contains("Abrechnung");
        assertThat(content).contains("Offen");
        assertThat(content).contains("Belege");
        assertThat(content).contains("Gewichtung");
    }

    @Test
    @Order(20)
    void addReceiptViaDialog() {
        final String tripId = extractTripId();
        navigateAndWait("/expense/" + tripId);

        page.locator("button", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText("Beleg hinzufuegen")).first().click();
        page.waitForSelector("dialog[open]");

        page.locator("dialog input[name=description]").fill("Supermarkt");
        page.locator("dialog input[name=amount]").fill("42.50");
        page.locator("dialog input[name=date]").fill("2026-08-05");
        page.locator("dialog select[name=paidBy]").selectOption(
            new com.microsoft.playwright.options.SelectOption().setIndex(0));

        page.waitForResponse(
            response -> response.url().contains("/expense/"),
            () -> page.locator("dialog button[type=submit]").click()
        );
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        final String content = page.content();
        assertThat(content).contains("Supermarkt");
        assertThat(content).contains("42,50");
    }

    @Test
    @Order(30)
    void balancesDisplayAfterReceipt() {
        final String tripId = extractTripId();
        navigateAndWait("/expense/" + tripId);

        final String content = page.content();
        assertThat(content).contains("Saldo");
        assertThat(content).contains("42,50");
    }

    @Test
    @Order(40)
    void settleExpenseChangesStatus() {
        final String tripId = extractTripId();
        navigateAndWait("/expense/" + tripId);

        page.locator("form[action$='/settle'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        final String content = page.content();
        assertThat(content).contains("Abgerechnet");
        assertThat(page.locator("form[action$='/settle']").count()).isZero();
    }

    @Test
    @Order(50)
    void settledExpenseDoesNotShowAddReceiptButton() {
        final String tripId = extractTripId();
        navigateAndWait("/expense/" + tripId);

        assertThat(page.locator("dialog#add-receipt-dialog").count()).isZero();
    }

    @Test
    @Order(60)
    void expenseBackLinkNavigatesToTripDetail() {
        final String tripId = extractTripId();
        navigateAndWait("/expense/" + tripId);

        page.locator("main a[href*='/trips/" + tripId + "']").click();
        page.waitForLoadState();

        assertThat(page.url()).contains("/trips/" + tripId);
        assertThat(page.content()).contains(TRIP_NAME);
    }

    private String extractTripId() {
        final String url = tripDetailUrl != null ? tripDetailUrl : page.url();
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
