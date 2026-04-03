package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvitationLifecycleIT extends E2ETestBase {

    private static final String PARTY1_NAME = "E2E-Party1 " + RUN_ID;
    private static final String PARTY1_EMAIL = "party1-" + RUN_ID + "@e2e.test";
    private static final String PARTY1_PASSWORD = "Test1234!";

    private static final String INVITED_MEMBER_EMAIL = "party1-member-" + RUN_ID + "@e2e.test";
    private static final String INVITED_MEMBER_PASSWORD = "Member1234!";

    private static final String PARTY2_NAME = "E2E-Party2 " + RUN_ID;
    private static final String PARTY2_EMAIL = "party2-" + RUN_ID + "@e2e.test";
    private static final String PARTY2_PASSWORD = "Party21234!";
    private static final String PARTY2_LATE_DEPENDENT = "Mila Family";

    private static final String TRIP_NAME = "Invitation Trip " + RUN_ID;

    private static BrowserContext invitedMemberContext;
    private static Page invitedMemberPage;
    private static BrowserContext party2Context;
    private static Page party2Page;
    private static String tripInvitationLink;

    @AfterAll
    static void closeAdditionalContexts() {
        if (invitedMemberContext != null) {
            invitedMemberContext.close();
        }
        if (party2Context != null) {
            party2Context.close();
        }
    }

    @Test
    @Order(1)
    void setUpOrganizerParty() {
        signUpAndLogin(PARTY1_NAME, "Paula", "Organizer", PARTY1_EMAIL, PARTY1_PASSWORD);
        waitForTripsReady();

        navigateAndWait("/iam/dashboard");
        assertThat(page.content()).contains(PARTY1_NAME);
        assertThat(page.content()).contains("Paula");
    }

    @Test
    @Order(2)
    void invitedTravelPartyMemberCanRegisterFromEmailWithoutBreakingOrganizerSession() {
        navigateAndWait("/iam/dashboard");

        page.fill("form[hx-post$='/dashboard/members'] input[name=firstName]", "Lisa");
        page.fill("form[hx-post$='/dashboard/members'] input[name=lastName]", "Party");
        page.fill("form[hx-post$='/dashboard/members'] input[name=email]", INVITED_MEMBER_EMAIL);
        page.fill("form[hx-post$='/dashboard/members'] input[name=dateOfBirth]", "1992-03-10");
        submitHtmxForm("form[hx-post$='/dashboard/members']");

        assertThat(page.locator("#members").innerHTML()).contains("Lisa");
        assertThat(page.locator("#members").innerHTML()).contains(INVITED_MEMBER_EMAIL);

        final String registrationLink = waitForMailpitLink(INVITED_MEMBER_EMAIL, "/iam/register?token=");
        assertThat(registrationLink).as("member invitation mail link").isNotBlank();

        invitedMemberContext = newContext();
        invitedMemberPage = newPage(invitedMemberContext);
        navigateAndWait(invitedMemberPage, registrationLink);

        assertThat(invitedMemberPage.content()).contains("Registrierung abschließen");
        assertThat(invitedMemberPage.content()).contains(INVITED_MEMBER_EMAIL);

        invitedMemberPage.fill("#password", INVITED_MEMBER_PASSWORD);
        invitedMemberPage.fill("#passwordConfirm", INVITED_MEMBER_PASSWORD);
        invitedMemberPage.click("button[type=submit]");
        invitedMemberPage.waitForLoadState();

        assertThat(invitedMemberPage.content()).contains("Registrierung abgeschlossen");

        loginViaKeycloak(invitedMemberPage, INVITED_MEMBER_EMAIL, INVITED_MEMBER_PASSWORD);
        navigateAndWait(invitedMemberPage, "/iam/dashboard");

        assertThat(invitedMemberPage.content()).contains(PARTY1_NAME);
        assertThat(invitedMemberPage.content()).contains("Lisa");

        navigateAndWait("/iam/dashboard");
        assertThat(page.content()).contains(PARTY1_NAME);
        assertThat(page.content()).contains("Paula");

        navigateAndWait(invitedMemberPage, "/trips/");
        assertThat(invitedMemberPage.content()).doesNotContain("Offene Einladungen");
        assertThat(invitedMemberPage.content()).contains("Noch keine Reisen");
    }

    @Test
    @Order(10)
    void setUpRegisteredParty2() {
        party2Context = newContext();
        party2Page = newPage(party2Context);
        signUpAndLogin(party2Page, PARTY2_NAME, "Rita", "Receiver", PARTY2_EMAIL, PARTY2_PASSWORD);
        waitForTripsReady(party2Page);

        navigateAndWait(party2Page, "/iam/dashboard");
        assertThat(party2Page.content()).contains(PARTY2_NAME);
        assertThat(party2Page.content()).contains("Rita");
    }

    @Test
    @Order(11)
    void organizerInvitesRegisteredParty2ByEmailAndMailLinkTargetsTripInvitationLanding() {
        waitForTripsReady();
        createTripWithoutDates(TRIP_NAME, "Trip invitation lifecycle E2E");
        openTripFromList(TRIP_NAME);

        page.locator("button[onclick*='invite-external-dialog'][onclick*='showModal']").click();
        page.locator("#invite-external-dialog input[name=firstName]").fill("Rita");
        page.locator("#invite-external-dialog input[name=lastName]").fill("Receiver");
        page.locator("#invite-external-dialog input[name=email]").fill(PARTY2_EMAIL);
        page.locator("#invite-external-dialog input[name=dateOfBirth]").fill("1991-04-12");
        submitHtmxForm(page, "#invite-external-dialog form[hx-post*='/invitations/external']");

        assertThat(page.locator("#invitations").innerHTML()).contains("Rita Receiver");
        assertThat(page.locator("#invitations").innerHTML()).contains(PARTY2_NAME);

        tripInvitationLink = waitForMailpitLink(PARTY2_EMAIL, "/trips/invitations/");
        assertThat(tripInvitationLink).as("trip invitation mail link").isNotBlank();
    }

    @Test
    @Order(12)
    void registeredParty2CanOpenMailLinkAcceptInvitationAndSeeTrip() {
        assertThat(tripInvitationLink).isNotBlank();

        navigateAndWait(party2Page, tripInvitationLink);
        assertThat(party2Page.url()).contains("/trips/");
        assertThat(party2Page.content()).contains("Offene Einladungen");
        assertThat(party2Page.content()).contains(TRIP_NAME);

        party2Page.locator("section article", new Page.LocatorOptions().setHasText(TRIP_NAME))
            .locator("button", new com.microsoft.playwright.Locator.LocatorOptions().setHasText("Annehmen"))
            .click();
        party2Page.waitForLoadState();

        assertThat(party2Page.content()).contains(TRIP_NAME);
        party2Page.locator("a", new Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        party2Page.waitForLoadState();
        assertThat(party2Page.content()).contains("Rita Receiver");
    }

    @Test
    @Order(13)
    void invitedRegisteredPartyCanAddLateDependentAndThenAddThemToTrip() {
        navigateAndWait(party2Page, "/iam/dashboard");

        party2Page.fill("form[hx-post$='/dashboard/companions'] input[name=firstName]", "Mila");
        party2Page.fill("form[hx-post$='/dashboard/companions'] input[name=lastName]", "Family");
        party2Page.fill("form[hx-post$='/dashboard/companions'] input[name=dateOfBirth]", "2020-06-15");
        submitHtmxForm(party2Page, "form[hx-post$='/dashboard/companions']");

        assertThat(party2Page.locator("#companions").innerHTML()).contains("Mila");
        assertThat(party2Page.locator("#companions").innerHTML()).contains("Family");

        navigateAndWait(party2Page, "/trips/");
        party2Page.locator("a", new Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        party2Page.waitForLoadState();

        waitForSelectOption(party2Page, "form[action$='/participants'] select[name=participantId]", PARTY2_LATE_DEPENDENT);
        party2Page.selectOption("form[action$='/participants'] select[name=participantId]",
            new com.microsoft.playwright.options.SelectOption().setLabel(PARTY2_LATE_DEPENDENT));
        party2Page.locator("form[action$='/participants'] button[type=submit]").click();
        party2Page.waitForLoadState();

        assertThat(party2Page.content()).contains(PARTY2_LATE_DEPENDENT);
    }

    private void waitForSelectOption(final Page targetPage, final String selectSelector, final String label) {
        for (int i = 0; i < 40; i++) {
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
}
