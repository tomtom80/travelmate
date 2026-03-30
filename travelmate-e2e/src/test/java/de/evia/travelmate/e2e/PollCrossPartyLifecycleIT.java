package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PollCrossPartyLifecycleIT extends E2ETestBase {

    private static final String PARTY1_NAME = "E2E-Poll-Party1 " + RUN_ID;
    private static final String PARTY1_EMAIL = "poll-party1-" + RUN_ID + "@e2e.test";
    private static final String PARTY1_PASSWORD = "Test1234!";

    private static final String PARTY2_NAME = "E2E-Poll-Party2 " + RUN_ID;
    private static final String PARTY2_EMAIL = "poll-party2-" + RUN_ID + "@e2e.test";
    private static final String PARTY2_PASSWORD = "Party21234!";

    private static final String TRIP_NAME = "Cross Party Poll Trip " + RUN_ID;

    private static BrowserContext party2Context;
    private static Page party2Page;
    private static String tripId;
    private static String tripInvitationLink;

    @AfterAll
    static void closeAdditionalContexts() {
        if (party2Context != null) {
            party2Context.close();
        }
    }

    @Test
    @Order(1)
    void setUpOrganizerAndTrip() {
        signUpAndLogin(PARTY1_NAME, "Paula", "Organizer", PARTY1_EMAIL, PARTY1_PASSWORD);
        waitForTripsReady();

        createTripWithoutDates(TRIP_NAME, "Cross-party poll visibility test");
        openTripFromList(TRIP_NAME);

        assertThat(page.content()).contains(TRIP_NAME);
        tripId = extractTripId(page);
    }

    @Test
    @Order(2)
    void setUpSecondTravelParty() {
        party2Context = newContext();
        party2Page = newPage(party2Context);
        signUpAndLogin(party2Page, PARTY2_NAME, "Rita", "Receiver", PARTY2_EMAIL, PARTY2_PASSWORD);
        waitForTripsReady(party2Page);

        navigateAndWait(party2Page, "/iam/dashboard");
        assertThat(party2Page.content()).contains(PARTY2_NAME);
    }

    @Test
    @Order(3)
    void inviteSecondTravelPartyToTrip() {
        navigateAndWait("/trips/" + tripId);

        page.locator("button.outline[onclick*='invite-external-dialog'][onclick*='showModal']").click();
        page.locator("#invite-external-dialog input[name=firstName]").fill("Rita");
        page.locator("#invite-external-dialog input[name=lastName]").fill("Receiver");
        page.locator("#invite-external-dialog input[name=email]").fill(PARTY2_EMAIL);
        page.locator("#invite-external-dialog input[name=dateOfBirth]").fill("1991-04-12");
        submitHtmxForm("#invite-external-dialog form[hx-post*='/invitations/external']");

        assertThat(page.locator("#invitations").innerHTML()).contains(PARTY2_NAME);
        tripInvitationLink = waitForMailpitLink(PARTY2_EMAIL, "/trips/invitations/");
        assertThat(tripInvitationLink).isNotBlank();
    }

    @Test
    @Order(4)
    void secondTravelPartyAcceptsInvitationAndSeesTrip() {
        navigateAndWait(party2Page, tripInvitationLink);
        assertThat(party2Page.content()).contains("Offene Einladungen");
        assertThat(party2Page.content()).contains(TRIP_NAME);

        party2Page.locator("section article", new Page.LocatorOptions().setHasText(TRIP_NAME))
            .locator("button", new com.microsoft.playwright.Locator.LocatorOptions().setHasText("Annehmen"))
            .click();
        party2Page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(party2Page.content()).contains(TRIP_NAME);
        party2Page.locator("a", new Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        party2Page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(party2Page.url()).contains("/trips/" + tripId);
    }

    @Test
    @Order(5)
    void organizerCreatesDateAndAccommodationPolls() {
        navigateAndWait("/trips/" + tripId + "/datepoll/create");

        page.locator("input[name=startDate]").nth(0).fill("2026-10-01");
        page.locator("input[name=endDate]").nth(0).fill("2026-10-10");
        page.locator("input[name=startDate]").nth(1).fill("2026-10-03");
        page.locator("input[name=endDate]").nth(1).fill("2026-10-12");
        page.locator("button[type=submit]").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(page.content()).contains("Offen");

        navigateAndWait("/trips/" + tripId + "/accommodationpoll/create");

        page.locator("input[name=candidateName]").nth(0).fill("Hotel Alpenblick");
        page.locator("input[name=candidateUrl]").nth(0).fill("https://alpenblick.example");
        page.locator("input[name=candidateDescription]").nth(0).fill("Direkt am See");
        page.locator("input[name=candidateName]").nth(1).fill("Berghaus Morgenrot");
        page.locator("input[name=candidateDescription]").nth(1).fill("Ruhige Lage");
        page.locator("button[type=submit]").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(page.content()).contains("Hotel Alpenblick");
    }

    @Test
    @Order(6)
    void secondTravelPartyCanSeeAndVoteInBothPolls() {
        navigateAndWait(party2Page, "/trips/" + tripId + "/planning");
        assertThat(party2Page.content()).contains("Terminabstimmung");
        assertThat(party2Page.content()).contains("Unterkunftsabstimmung");
        assertThat(party2Page.content()).contains("Offen");

        navigateAndWait(party2Page, "/trips/" + tripId + "/datepoll");
        assertThat(party2Page.content()).contains("2026-10-01");
        party2Page.locator("input[name=selectedOptionIds]").first().check();
        party2Page.locator("button[type=submit]:has-text('Abstimmen')").click();
        party2Page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(party2Page.locator("input[name=selectedOptionIds]:checked").count()).isGreaterThanOrEqualTo(1);

        navigateAndWait(party2Page, "/trips/" + tripId + "/accommodationpoll");
        assertThat(party2Page.content()).contains("Hotel Alpenblick");
        party2Page.locator("input[name=selectedCandidateId]").first().check();
        party2Page.locator("button[type=submit]:has-text('Abstimmen')").click();
        party2Page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat((boolean) party2Page.evaluate(
            "document.querySelector('input[name=selectedCandidateId]:checked') !== null")).isTrue();
    }

    @Test
    @Order(7)
    void organizerConfirmsBothPollsAndSecondPartySeesFinalState() {
        navigateAndWait("/trips/" + tripId + "/datepoll");
        page.locator("select[name=confirmedOptionId]").selectOption(
            page.locator("select[name=confirmedOptionId] option:not([value=''])").first().getAttribute("value"));
        page.locator("button[type=submit]:has-text('Bestaetigen')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(page.locator("mark").allTextContents()).anySatisfy(text ->
            assertThat(text).containsIgnoringCase("bestaetigt"));

        navigateAndWait("/trips/" + tripId + "/accommodationpoll");
        page.locator("select[name=confirmedCandidateId]").selectOption(
            page.locator("select[name=confirmedCandidateId] option:not([value=''])").first().getAttribute("value"));
        page.locator("button[type=submit]:has-text('Bestaetigen')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertThat(page.locator("mark").allTextContents()).anySatisfy(text ->
            assertThat(text).containsIgnoringCase("bestaetigt"));

        navigateAndWait(party2Page, "/trips/" + tripId + "/planning");
        assertThat(party2Page.content()).contains("Bestaetigt");
        assertThat(party2Page.content()).contains("Hotel Alpenblick");

        navigateAndWait(party2Page, "/trips/" + tripId + "/datepoll");
        assertThat(party2Page.content()).contains("Bestaetigt");
        navigateAndWait(party2Page, "/trips/" + tripId + "/accommodationpoll");
        assertThat(party2Page.content()).contains("Hotel Alpenblick");
        assertThat(party2Page.content()).contains("Bestaetigt");

        createAccommodationAfterPollDecision(
            tripId,
            "Hotel Alpenblick",
            "Alpweg 7",
            "2026-10-01",
            "2026-10-10",
            "1800",
            "Familienzimmer",
            "4"
        );
        navigateAndWait(party2Page, "/trips/" + tripId + "/accommodation");
        assertThat(party2Page.content()).contains("Hotel Alpenblick");
    }
}
