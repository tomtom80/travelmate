package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.LoadState;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccommodationLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Accom " + RUN_ID;
    private static final String EMAIL = "accom-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Huettenreise " + RUN_ID;
    private static final String ACCOMMODATION_NAME = "Berghuette Alpenblick";
    private static final String ACCOMMODATION_ADDRESS = "Alpweg 7";
    private static final String ACCOMMODATION_PRICE = "1800";
    private static final String ROOM_1_NAME = "Familienzimmer";
    private static final String ROOM_2_NAME = "Doppelzimmer";

    private static String tripDetailUrl;

    @Test
    @Order(1)
    void setUpTravelPartyAndCreateTrip() {
        signUpAndLogin(TENANT_NAME, "Heidi", "Alpen", EMAIL, PASSWORD);
        waitForTripsReady();

        createTripWithoutDates(TRIP_NAME, null);

        assertThat(page.content()).contains(TRIP_NAME);

        final String tripId = openTripFromList(TRIP_NAME);
        tripDetailUrl = page.url();
        createAndConfirmDatePoll(tripId, "2026-11-01", "2026-11-05", "2026-11-02", "2026-11-06");
    }

    @Test
    @Order(10)
    void tripDetailShowsAccommodationSection() {
        navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));

        final String content = page.content();
        assertThat(content).contains("Unterkunft");
    }

    @Test
    @Order(11)
    void navigateToAccommodationPageShowsEmptyState() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/accommodation");

        assertThat(page.url()).contains("/planning");
    }

    @Test
    @Order(12)
    void confirmAccommodationPollBeforeManagingAccommodation() {
        final String tripId = extractTripId();
        createAndConfirmAccommodationPoll(
            tripId,
            ACCOMMODATION_NAME,
            null,
            "Tolle Aussicht",
            "Berghaus Sonnstein",
            "Ruhige Lage"
        );

        navigateAndWait("/trips/" + tripId + "/accommodation");
        assertThat(page.content()).contains("Unterkunft");
    }

    @Test
    @Order(13)
    void addAccommodationViaDialog() {
        // Open the add accommodation dialog
        page.locator("button:has-text('Unterkunft hinzufuegen')").click();
        page.waitForSelector("dialog[open]");

        // Fill in the accommodation form
        page.locator("dialog input[name=name]").fill(ACCOMMODATION_NAME);
        page.locator("dialog input[name=address]").fill(ACCOMMODATION_ADDRESS);
        page.locator("dialog input[name=checkIn]").fill("2026-11-01");
        page.locator("dialog input[name=checkOut]").fill("2026-11-05");
        page.locator("dialog input[name=totalPrice]").fill(ACCOMMODATION_PRICE);

        // Fill in the first room (required in add dialog)
        page.locator("dialog input[name=roomName]").fill(ROOM_1_NAME);
        page.locator("dialog input[name=roomBedCount]").fill("4");

        // Submit the form
        page.locator("dialog button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Test
    @Order(14)
    void accommodationDetailsShown() {
        final String content = page.content();
        assertThat(content).contains(ACCOMMODATION_NAME);
        assertThat(content).contains(ACCOMMODATION_ADDRESS);
        assertThat(content).contains(ACCOMMODATION_PRICE);
        assertThat(content).contains(ROOM_1_NAME);
    }

    @Test
    @Order(15)
    void i18nResolvedCorrectlyOnAccommodationPage() {
        final String content = page.content();
        assertThat(content).doesNotContain("??");
    }

    @Test
    @Order(20)
    void addSecondRoomViaForm() {
        // Open the "Zimmer hinzufuegen" dialog
        page.locator("button[onclick*='add-room-dialog'][onclick*='showModal']").click();

        // Fill in room form in dialog
        page.locator("dialog[open] input[name=name]").fill(ROOM_2_NAME);
        page.locator("dialog[open] input[name=bedCount]").fill("2");

        // Submit
        page.locator("dialog[open] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Test
    @Order(22)
    void twoRoomsShownInTable() {
        final String content = page.content();
        assertThat(content).contains(ROOM_1_NAME);
        assertThat(content).contains(ROOM_2_NAME);

        // Verify the room table has both rows
        final int roomRows = page.locator("table tbody tr").count();
        assertThat(roomRows).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(30)
    void navigateBackToTripDetail() {
        final String tripId = extractTripId();
        page.locator("a[href*='/" + tripId + "']:not([href*='/accommodation'])").first().click();
        page.waitForLoadState();

        assertThat(page.url()).contains("/trips/" + tripId);
        assertThat(page.url()).doesNotContain("/accommodation");
        assertThat(page.content()).contains(TRIP_NAME);
    }

    private String extractTripId() {
        final String url = tripDetailUrl != null ? tripDetailUrl : page.url();
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
