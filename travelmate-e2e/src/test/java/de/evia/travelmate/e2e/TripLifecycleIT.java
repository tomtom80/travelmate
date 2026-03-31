package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TripLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Trips " + RUN_ID;
    private static final String EMAIL = "trips-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Sommerurlaub " + RUN_ID;

    @Test
    @Order(1)
    void setUpTravelPartyAndNavigateToTrips() {
        signUpAndLogin(TENANT_NAME, "Kai", "Trips", EMAIL, PASSWORD);
        waitForTripsReady();

        navigateAndWait("/trips/");
        final String content = page.content();
        assertThat(content).doesNotContain("Whitelabel Error Page");
        assertThat(content).doesNotContain("Internal Server Error");
        assertThat(content).doesNotContain("Forbidden");
    }

    @Test
    @Order(2)
    void tripsListShowsForNewUser() {
        navigateAndWait("/trips/");

        assertThat(page.content()).contains("Reisen");
    }

    @Test
    @Order(3)
    void tripsPageResolvesI18nMessages() {
        navigateAndWait("/trips/");
        final String content = page.content();

        assertThat(content).doesNotContain("??");
        assertThat(content).contains("Travelmate");
    }

    @Test
    @Order(10)
    void createTripFormDisplaysAllFields() {
        navigateAndWait("/trips/new");
        page.waitForLoadState();

        assertThat(page.locator("input[name=name]").count()).isPositive();
        assertThat(page.locator("textarea[name=description], input[name=description]").count()).isPositive();
        assertThat(page.locator("input[name=startDate]").count()).isZero();
        assertThat(page.locator("input[name=endDate]").count()).isZero();
    }

    @Test
    @Order(11)
    void createTripSucceeds() {
        createTripWithoutDates(TRIP_NAME, "Ein E2E-Testurlaub");

        assertThat(page.url()).contains("/trips");
        assertThat(page.content()).contains(TRIP_NAME);
    }

    @Test
    @Order(12)
    void tripAppearsInList() {
        navigateAndWait("/trips/");
        final String content = page.content();

        assertThat(content).contains(TRIP_NAME);
        assertThat(content).contains("—");
        assertThat(content).contains("In Planung");
    }

    @Test
    @Order(20)
    void tripDetailShowsCorrectInfo() {
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();

        final String content = page.content();
        assertThat(content).contains(TRIP_NAME);
        assertThat(content).contains("Ein E2E-Testurlaub");
        assertThat(content).contains("gemeinsam");
        assertThat(content).contains("In Planung");
    }

    @Test
    @Order(21)
    void tripDetailShowsOrganizerAsParticipant() {
        assertThat(page.content()).contains("Teilnehmer");
        assertThat(page.content()).contains("Kai");
    }

    @Test
    @Order(22)
    void tripDetailShowsStatusActionButtons() {
        assertThat(page.locator("form[action$='/confirm'] button[type=submit]").count()).isZero();
        assertThat(page.locator("form[action$='/cancel'] button[type=submit]").count()).isPositive();
    }

    @Test
    @Order(24)
    void planningFlowMakesTripConfirmable() {
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();

        final String tripId = extractTripId(page);
        createAndConfirmDatePoll(tripId, "2026-07-01", "2026-07-14", "2026-07-03", "2026-07-16");
        createAndConfirmAccommodationPoll(
            tripId,
            "Hotel Alpenblick",
            "https://alpenblick.example",
            "Direkt am See",
            "Berghaus Morgenrot",
            "Ruhige Lage"
        );

        navigateAndWait("/trips/" + tripId);
        assertThat(page.locator("form[action$='/confirm'] button[type=submit]").count()).isPositive();
    }

    @Test
    @Order(25)
    void setStayPeriodForOrganizer() {
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();

        final var editButton = page.locator("button.btn-icon[data-dialog-id]").first();
        if (editButton.count() > 0) {
            editButton.click();
            page.locator("dialog[open] input[name=arrivalDate]").fill("2026-07-02");
            page.locator("dialog[open] input[name=departureDate]").fill("2026-07-13");
            page.locator("dialog[open] button[type=submit]").click();
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

            assertThat(page.content()).contains("2026-07-02");
            assertThat(page.content()).contains("2026-07-13");
        }
    }

    @Test
    @Order(30)
    void confirmTripChangesStatusToConfirmed() {
        page.locator("form[action$='/confirm'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        assertThat(page.content()).contains("Bestaetigt");
    }

    @Test
    @Order(31)
    void startTripChangesStatusToInProgress() {
        page.locator("form[action$='/start'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        assertThat(page.content()).contains("Laeuft");
    }

    @Test
    @Order(32)
    void completeTripChangesStatusToCompleted() {
        page.locator("form[action$='/complete'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        assertThat(page.content()).contains("Abgeschlossen");
    }

    @Test
    @Order(33)
    void completedTripHasNoStatusActionButtons() {
        assertThat(page.locator("form[action$='/confirm']").count()).isZero();
        assertThat(page.locator("form[action$='/start']").count()).isZero();
        assertThat(page.locator("form[action$='/complete']").count()).isZero();
        assertThat(page.locator("form[action$='/cancel']").count()).isZero();
    }

    @Test
    @Order(40)
    void createAndCancelTrip() {
        final String cancelTripName = "Storniert " + RUN_ID;

        createTripWithoutDates(cancelTripName, null);

        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(cancelTripName)).click();
        page.waitForLoadState();
        assertThat(page.content()).contains("In Planung");

        page.locator("form[action$='/cancel'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        assertThat(page.content()).contains("Abgesagt");
    }

    @Test
    @Order(60)
    void navigateBackFromTripDetail() {
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();

        page.locator("main a[href='/trips/']").click();
        page.waitForURL(url -> url.endsWith("/trips/"));

        assertThat(page.url()).endsWith("/trips/");
    }
}
