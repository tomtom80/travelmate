package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
        page.locator(".candidate-entry").nth(0).locator("input[name=roomName]").first().fill("Familienzimmer");
        page.locator(".candidate-entry").nth(0).locator("input[name=roomBedCount]").first().fill("4");

        nameInputs.nth(1).fill("Berghuette Sonnstein");
        descInputs.nth(1).fill("Gemuetliche Huette");
        page.locator(".candidate-entry").nth(1).locator("input[name=roomName]").first().fill("Doppelzimmer");
        page.locator(".candidate-entry").nth(1).locator("input[name=roomBedCount]").first().fill("2");
        page.evaluate("""
            ([first, second]) => {
                const inputs = document.querySelectorAll('input.candidate-rooms-data');
                inputs[0].value = first;
                inputs[1].value = second;
            }
            """, List.of(
            "[{\"name\":\"Familienzimmer\",\"bedCount\":4,\"bedDescription\":null}]",
            "[{\"name\":\"Doppelzimmer\",\"bedCount\":2,\"bedDescription\":null}]"
        ));

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
        assertThat(page.content()).contains("Hotel Alpenblick");
        final boolean anyChecked = (boolean) page.evaluate(
            "document.querySelector('input[name=selectedCandidateId]:checked') !== null");
        assertThat(anyChecked).isTrue();
    }

    // --- S15-B: Booking Workflow ---

    @Test
    @Order(30)
    void selectCandidateSetsAwaitingBookingStatus() {
        page.locator("select[name=selectedCandidateId]").selectOption(
            page.locator("select[name=selectedCandidateId] option:not([value=''])").first().getAttribute("value")
        );
        page.locator("button[type=submit]:has-text('Auswaehlen'), button[type=submit]:has-text('Select')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("Buchung ausstehend"),
            c -> assertThat(c).containsIgnoringCase("Awaiting booking")
        );
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("Buchung erfolgreich"),
            c -> assertThat(c).containsIgnoringCase("Booking success")
        );
    }

    // --- S15-C: Booking Failure ---

    @Test
    @Order(31)
    void bookingFailureReturnsToOpen() {
        page.locator("input[name=note]").fill("Ausgebucht");
        page.locator("button[type=submit]:has-text('fehlgeschlagen'), button[type=submit]:has-text('failed')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).contains("Offen"),
            c -> assertThat(c).contains("Open")
        );
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("konnte nicht gebucht"),
            c -> assertThat(c).containsIgnoringCase("Ausgebucht")
        );
    }

    @Test
    @Order(32)
    void selectAnotherCandidateAfterFailure() {
        page.locator("select[name=selectedCandidateId]").selectOption(
            page.locator("select[name=selectedCandidateId] option:not([value=''])").first().getAttribute("value")
        );
        page.locator("button[type=submit]:has-text('Auswaehlen'), button[type=submit]:has-text('Select')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.content()).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("Buchung ausstehend"),
            c -> assertThat(c).containsIgnoringCase("Awaiting booking")
        );
    }

    @Test
    @Order(33)
    void bookingSuccessSetsPollToBooked() {
        page.locator("button[type=submit]:has-text('Buchung erfolgreich'), button[type=submit]:has-text('Booking success')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("Gebucht"),
            c -> assertThat(c).containsIgnoringCase("Booked")
        );
    }

    @Test
    @Order(40)
    void planningPageShowsAccommodationPollConfirmed() {
        navigateAndWait("/trips/" + tripId + "/planning");

        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).contains("Hotel Alpenblick"),
            c -> assertThat(c).contains("Berghuette Sonnstein")
        );
    }
}
