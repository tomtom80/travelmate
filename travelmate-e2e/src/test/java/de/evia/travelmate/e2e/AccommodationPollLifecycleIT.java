package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.LoadState;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccommodationPollLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-AccPoll " + RUN_ID;
    private static final String EMAIL = "accpoll-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Unterkunftreise " + RUN_ID;

    private static String tripId;

    @Test
    @Order(1)
    void setUpTravelPartyAndCreateTrip() {
        signUpAndLogin(TENANT_NAME, "Lisa", "Reisend", EMAIL, PASSWORD);
        waitForTripsReady();

        createTripWithoutDates(TRIP_NAME, null);

        assertThat(page.content()).contains(TRIP_NAME);

        tripId = openTripFromList(TRIP_NAME);
    }

    @Test
    @Order(10)
    void accommodationPollPageShowsEmptyState() {
        navigateAndWait("/trips/" + tripId + "/accommodationpoll");

        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("keine Unterkunftsabstimmung"),
            c -> assertThat(c).containsIgnoringCase("Unterkunftsabstimmung erstellen")
        );
    }

    @Test
    @Order(11)
    void createAccommodationPollWithTwoCandidates() {
        page.locator("a[href*='accommodationpoll/create']").click();
        page.waitForLoadState();

        final var nameInputs = page.locator("input[name=candidateName]");
        final var urlInputs = page.locator("input[name=candidateUrl]");
        final var descInputs = page.locator("input[name=candidateDescription]");

        nameInputs.nth(0).fill("Hotel Alpenblick");
        urlInputs.nth(0).fill("https://alpenblick.at");
        descInputs.nth(0).fill("Tolle Aussicht");

        nameInputs.nth(1).fill("Berghuette Sonnstein");
        descInputs.nth(1).fill("Gemuetliche Huette");

        page.locator("button[type=submit]:not(.outline)").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Test
    @Order(12)
    void accommodationPollShowsOpenStatusWithTwoCandidates() {
        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).contains("Offen"),
            c -> assertThat(c).contains("Open")
        );
        assertThat(content).contains("Hotel Alpenblick");
        assertThat(content).contains("Berghuette Sonnstein");
        assertThat(page.locator("input[name=selectedCandidateId]").count()).isEqualTo(2);
    }

    @Test
    @Order(13)
    void i18nResolvedCorrectlyOnAccommodationPollPage() {
        assertThat(page.content()).doesNotContain("??");
    }

    @Test
    @Order(20)
    void voteForFirstCandidate() {
        page.locator("input[name=selectedCandidateId]").first().check();
        page.locator("button[type=submit]:has-text('Abstimmen'), button[type=submit]:has-text('Submit Vote')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.content()).satisfiesAnyOf(
            c -> assertThat(c).contains("Offen"),
            c -> assertThat(c).contains("Open")
        );
    }

    @Test
    @Order(21)
    void voteIsPreselectedOnReload() {
        navigateAndWait("/trips/" + tripId + "/accommodationpoll");
        // After voting, the page should show the poll with candidates
        assertThat(page.content()).contains("Hotel Alpenblick");
        // Verify the vote was persisted by checking the radio button state via JS
        final boolean anyChecked = (boolean) page.evaluate(
            "document.querySelector('input[name=selectedCandidateId]:checked') !== null");
        assertThat(anyChecked).isTrue();
    }

    @Test
    @Order(30)
    void confirmAccommodationPoll() {
        page.locator("select[name=confirmedCandidateId]").selectOption(
            page.locator("select[name=confirmedCandidateId] option:not([value=''])").first().getAttribute("value")
        );
        page.locator("button[type=submit]:has-text('Bestaetigen'), button[type=submit]:has-text('Confirm')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.locator("mark").allTextContents()).anySatisfy(text ->
            assertThat(text).matches("(?i).*confirmed.*|.*bestaetigt.*"));
    }

    @Test
    @Order(40)
    void planningPageShowsAccommodationPollConfirmed() {
        navigateAndWait("/trips/" + tripId + "/planning");

        final String content = page.content();
        assertThat(content).contains("Hotel Alpenblick");
    }
}
