package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.LoadState;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatePollLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-DatePoll " + RUN_ID;
    private static final String EMAIL = "datepoll-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Terminreise " + RUN_ID;

    private static String tripId;

    @Test
    @Order(1)
    void setUpTravelPartyAndCreateTrip() {
        signUpAndLogin(TENANT_NAME, "Max", "Planer", EMAIL, PASSWORD);
        waitForTripsReady();

        createTripWithoutDates(TRIP_NAME, null);

        assertThat(page.content()).contains(TRIP_NAME);

        tripId = openTripFromList(TRIP_NAME);
    }

    @Test
    @Order(10)
    void datePollPageShowsEmptyState() {
        navigateAndWait("/trips/" + tripId + "/datepoll");

        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("keine Terminabstimmung"),
            c -> assertThat(c).containsIgnoringCase("Terminabstimmung erstellen")
        );
    }

    @Test
    @Order(11)
    void createDatePollWithTwoOptions() {
        page.locator("a[href*='datepoll/create']").click();
        page.waitForLoadState();

        final var startInputs = page.locator("input[name=startDate]");
        final var endInputs = page.locator("input[name=endDate]");

        startInputs.nth(0).fill("2026-09-01");
        endInputs.nth(0).fill("2026-09-14");
        startInputs.nth(1).fill("2026-10-01");
        endInputs.nth(1).fill("2026-10-14");

        page.locator("button[type=submit]:not(.outline)").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Test
    @Order(12)
    void datePollShowsOpenStatusWithTwoOptions() {
        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).contains("Offen"),
            c -> assertThat(c).contains("Open")
        );
        assertThat(page.locator("input[name=selectedOptionIds]").count()).isEqualTo(2);
    }

    @Test
    @Order(13)
    void i18nResolvedCorrectlyOnDatePollPage() {
        assertThat(page.content()).doesNotContain("??");
    }

    @Test
    @Order(20)
    void voteForFirstOption() {
        page.locator("input[name=selectedOptionIds]").first().check();
        page.locator("button[type=submit]:has-text('Abstimmen'), button[type=submit]:has-text('Submit Vote')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // After voting, the page should still show the poll
        assertThat(page.content()).satisfiesAnyOf(
            c -> assertThat(c).contains("Offen"),
            c -> assertThat(c).contains("Open")
        );
    }

    @Test
    @Order(21)
    void voteIsPreselectedOnReload() {
        navigateAndWait("/trips/" + tripId + "/datepoll");
        // The first checkbox should be checked
        assertThat(page.locator("input[name=selectedOptionIds]:checked").count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(30)
    void confirmDatePoll() {
        final var select = page.locator("select[name=confirmedOptionId]");
        select.selectOption(
            page.locator("select[name=confirmedOptionId] option:not([value=''])").first().getAttribute("value")
        );
        page.locator("button[type=submit]:has-text('Bestätigen'), button[type=submit]:has-text('Confirm')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.locator("mark").allTextContents()).anySatisfy(text ->
            assertThat(text).matches("(?i).*confirmed.*|.*bestätigt.*"));
    }

    @Test
    @Order(40)
    void planningPageShowsDatePollStatus() {
        navigateAndWait("/trips/" + tripId + "/planning");

        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).contains("Bestätigt"),
            c -> assertThat(c).contains("Confirmed")
        );
    }
}
