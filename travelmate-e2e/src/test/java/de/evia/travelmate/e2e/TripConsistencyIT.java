package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TripConsistencyIT extends E2ETestBase {

    private static final String ORIGINAL_PARTY_NAME = "E2E-Consistency " + RUN_ID;
    private static final String RENAMED_PARTY_NAME = ORIGINAL_PARTY_NAME + " Renamed";
    private static final String EMAIL = "consistency-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Konsistenzreise " + RUN_ID;
    private static final String COMPANION_NAME = "Lina Family";

    private static String tripId;

    @Test
    @Order(1)
    void setUpTravelPartyAndCompanion() {
        signUpAndLogin(ORIGINAL_PARTY_NAME, "Nina", "Consistency", EMAIL, PASSWORD);
        waitForTripsReady();

        navigateAndWait("/iam/dashboard");
        assertThat(page.content()).contains(ORIGINAL_PARTY_NAME);

        page.fill("form[hx-post$='/dashboard/companions'] input[name=firstName]", "Lina");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=lastName]", "Family");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=dateOfBirth]", "2018-06-15");
        submitHtmxForm("form[hx-post$='/dashboard/companions']");

        assertThat(page.locator("#companions").innerHTML()).contains("Lina");
        assertThat(page.locator("#companions").innerHTML()).contains("Family");
    }

    @Test
    @Order(2)
    void createTripAndAddCompanionParticipant() {
        navigateAndWait("/trips/new");
        page.fill("input[name=name]", TRIP_NAME);
        page.fill("#description", "Rename and lifecycle consistency");
        page.fill("input[name=startDate]", "2026-10-01");
        page.fill("input[name=endDate]", "2026-10-07");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        navigateAndWait("/trips/");
        page.locator("a", new Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.url()).contains("/trips/");
        assertThat(page.content()).contains(TRIP_NAME);
        tripId = extractTripId(page.url());

        if (!page.content().contains(COMPANION_NAME)) {
            waitForSelectOption(page, "form[action$='/participants'] select[name=participantId]", COMPANION_NAME);
            page.selectOption("form[action$='/participants'] select[name=participantId]",
                new com.microsoft.playwright.options.SelectOption().setLabel(COMPANION_NAME));
            page.locator("form[action$='/participants'] button[type=submit]").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        }

        assertThat(page.content()).contains(COMPANION_NAME);
    }

    @Test
    @Order(3)
    void completeTripAndExpenseInitiallyShowsOriginalPartyName() {
        navigateAndWait("/trips/" + tripId);

        page.locator("form[action$='/confirm'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.locator("form[action$='/start'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.locator("form[action$='/complete'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.content()).contains("Abgeschlossen");
        assertThat(page.locator("tr", new Page.LocatorOptions().setHasText(COMPANION_NAME))
            .locator("form[action*='/remove']").count()).isZero();

        navigateAndWait("/expense/" + tripId);
        assertThat(page.content()).contains("Abrechnung");
        assertThat(page.content()).contains(ORIGINAL_PARTY_NAME);
        assertThat(page.content()).contains(COMPANION_NAME);
    }

    @Test
    @Order(4)
    void renamingTravelPartyPropagatesToExpenseWithoutClientRepairLogic() {
        navigateAndWait("/iam/dashboard");
        page.locator("#tenant-header button.btn-icon").click();
        page.locator("dialog[open] input[name=name]").fill(RENAMED_PARTY_NAME);
        submitHtmxForm("dialog[open] form[hx-post$='/dashboard/tenant/rename']");
        page.waitForTimeout(500);
        navigateAndWait("/iam/dashboard");

        assertThat(page.content()).contains(RENAMED_PARTY_NAME);

        assertExpenseEventuallyShowsRenamedParty();
    }

    private void assertExpenseEventuallyShowsRenamedParty() {
        for (int i = 0; i < 30; i++) {
            navigateAndWait("/expense/" + tripId);
            final String content = page.content();
            if (content.contains(RENAMED_PARTY_NAME)
                && !content.contains("<strong>" + ORIGINAL_PARTY_NAME + "</strong>")
                && !content.contains(">" + ORIGINAL_PARTY_NAME + "<")) {
                assertThat(content).contains(COMPANION_NAME);
                return;
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Expense page did not show renamed party name: " + RENAMED_PARTY_NAME);
    }

    private void waitForSelectOption(final Page targetPage, final String selectSelector, final String label) {
        for (int i = 0; i < 60; i++) {
            final var option = targetPage.locator(selectSelector + " option").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText(label)
            );
            if (option.count() > 0) {
                return;
            }
            targetPage.waitForTimeout(500);
            targetPage.reload(new Page.ReloadOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE));
        }
        throw new AssertionError("Did not find select option: " + label);
    }

    private String extractTripId(final String url) {
        final String path = url.replaceFirst(".*/trips/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
