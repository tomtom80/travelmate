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
class ParticipantDeletionLifecycleIT extends E2ETestBase {

    private static final String PARTY_NAME = "E2E-Deletion " + RUN_ID;
    private static final String ORGANIZER_EMAIL = "deletion-organizer-" + RUN_ID + "@e2e.test";
    private static final String ORGANIZER_PASSWORD = "Test1234!";

    private static final String MEMBER_FIRST_NAME = "Sven";
    private static final String MEMBER_LAST_NAME = "Mitglied";
    private static final String MEMBER_NAME = MEMBER_FIRST_NAME + " " + MEMBER_LAST_NAME;
    private static final String MEMBER_EMAIL = "deletion-member-" + RUN_ID + "@e2e.test";
    private static final String MEMBER_PASSWORD = "Member1234!";

    private static final String COMPANION_FIRST_NAME = "Lina";
    private static final String COMPANION_LAST_NAME = "Family";
    private static final String COMPANION_NAME = COMPANION_FIRST_NAME + " " + COMPANION_LAST_NAME;

    private static final String TRIP_NAME = "Loeschreise " + RUN_ID;
    private static final String MANUAL_ITEM_NAME = "Kaffee";

    private static BrowserContext memberContext;
    private static Page memberPage;
    private static String tripId;

    @AfterAll
    static void closeAdditionalContexts() {
        if (memberContext != null) {
            memberContext.close();
        }
    }

    @Test
    @Order(1)
    void setUpPartyWithRegisteredMemberAndCompanion() {
        signUpAndLogin(PARTY_NAME, "Nina", "Deletion", ORGANIZER_EMAIL, ORGANIZER_PASSWORD);
        waitForTripsReady();

        navigateAndWait("/iam/dashboard");
        page.fill("form[hx-post$='/dashboard/members'] input[name=firstName]", MEMBER_FIRST_NAME);
        page.fill("form[hx-post$='/dashboard/members'] input[name=lastName]", MEMBER_LAST_NAME);
        page.fill("form[hx-post$='/dashboard/members'] input[name=email]", MEMBER_EMAIL);
        page.fill("form[hx-post$='/dashboard/members'] input[name=dateOfBirth]", "1991-04-12");
        submitHtmxForm("form[hx-post$='/dashboard/members']");

        final String registrationLink = waitForMailpitLink(MEMBER_EMAIL, "/iam/register?token=");
        assertThat(registrationLink).as("member registration mail link").isNotBlank();

        memberContext = newContext();
        memberPage = newPage(memberContext);
        navigateAndWait(memberPage, registrationLink);
        memberPage.fill("#password", MEMBER_PASSWORD);
        memberPage.fill("#passwordConfirm", MEMBER_PASSWORD);
        memberPage.click("button[type=submit]");
        memberPage.waitForLoadState();

        loginViaKeycloak(memberPage, MEMBER_EMAIL, MEMBER_PASSWORD);
        navigateAndWait(memberPage, "/iam/dashboard");
        assertThat(memberPage.content()).contains(PARTY_NAME);
        assertThat(memberPage.content()).contains(MEMBER_FIRST_NAME);

        navigateAndWait("/iam/dashboard");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=firstName]", COMPANION_FIRST_NAME);
        page.fill("form[hx-post$='/dashboard/companions'] input[name=lastName]", COMPANION_LAST_NAME);
        page.fill("form[hx-post$='/dashboard/companions'] input[name=dateOfBirth]", "2018-06-15");
        submitHtmxForm("form[hx-post$='/dashboard/companions']");

        assertThat(page.locator("#members").innerHTML()).contains(MEMBER_FIRST_NAME);
        assertThat(page.locator("#companions").innerHTML()).contains(COMPANION_FIRST_NAME);
    }

    @Test
    @Order(2)
    void createTripAndPrepareShoppingList() {
        navigateAndWait("/trips/new");
        page.fill("input[name=name]", TRIP_NAME);
        page.fill("#description", "Participant deletion lifecycle");
        page.fill("input[name=startDate]", "2026-11-01");
        page.fill("input[name=endDate]", "2026-11-05");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        navigateAndWait("/trips/");
        page.locator("a", new Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        tripId = extractTripId(page.url());

        ensureParticipantOnTrip(MEMBER_NAME);
        ensureParticipantOnTrip(COMPANION_NAME);

        navigateAndWait("/trips/" + tripId + "/shoppinglist");
        if (page.locator("form[action$='/shoppinglist/generate']").count() > 0) {
            page.locator("form[action$='/shoppinglist/generate'] button[type=submit]").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        }

        page.locator("form:has(input[name=unit]) input[name=name]").fill(MANUAL_ITEM_NAME);
        page.locator("form:has(input[name=unit]) input[name=quantity]").fill("2");
        page.locator("form:has(input[name=unit]) input[name=unit]").fill("Pakete");
        clickAndWaitForHtmx("form:has(input[name=unit]) button[type=submit]");

        navigateAndWait(memberPage, "/trips/" + tripId + "/shoppinglist");
        clickAndWaitForHtmx(memberPage, "tr:has-text('" + MANUAL_ITEM_NAME + "') button:has-text('Ich uebernehme')");
        assertThat(memberPage.content()).contains("Uebernommen");

        navigateAndWait("/trips/" + tripId + "/shoppinglist");
        assertThat(page.locator("tr", new Page.LocatorOptions().setHasText(MANUAL_ITEM_NAME)).innerText())
            .contains(MEMBER_FIRST_NAME);
    }

    @Test
    @Order(3)
    void iamDeletionIsBlockedWhileParticipantsAreStillOnActiveTrip() {
        navigateAndWait("/iam/dashboard");

        final String blockedMessage =
            "Mitglieder oder Mitreisende koennen nicht geloescht werden, solange sie in einer Reise verwendet werden.";

        assertCompanionDeletionBlocked(COMPANION_FIRST_NAME, blockedMessage);
        assertThat(page.locator("#companions").innerHTML()).contains(COMPANION_FIRST_NAME);

        assertMemberDeletionBlocked(MEMBER_FIRST_NAME, blockedMessage);
        assertThat(page.locator("#members").innerHTML()).contains(MEMBER_FIRST_NAME);
    }

    @Test
    @Order(4)
    void removingTripMemberClearsShoppingAssignmentAndAllowsMemberDeletion() {
        navigateAndWait("/trips/" + tripId);
        removeParticipantFromTrip(MEMBER_NAME);
        assertParticipantRowMissing(MEMBER_NAME);

        navigateAndWait("/trips/" + tripId + "/shoppinglist");
        final String itemRow = page.locator("tr", new Page.LocatorOptions().setHasText(MANUAL_ITEM_NAME)).innerText();
        assertThat(itemRow).doesNotContain(MEMBER_FIRST_NAME);
        assertThat(itemRow).contains("Ich uebernehme");

        deleteMemberWhenAllowed(MEMBER_FIRST_NAME);
        assertThat(page.locator("#members").innerHTML()).doesNotContain(MEMBER_FIRST_NAME);
    }

    @Test
    @Order(5)
    void removingTripCompanionAllowsCompanionDeletion() {
        navigateAndWait("/trips/" + tripId);
        removeParticipantFromTrip(COMPANION_NAME);
        assertParticipantRowMissing(COMPANION_NAME);

        deleteCompanionWhenAllowed(COMPANION_FIRST_NAME);
        assertThat(page.locator("#companions").innerHTML()).doesNotContain(COMPANION_FIRST_NAME);
    }

    private void removeParticipantFromTrip(final String participantName) {
        final var participantRow = page.locator("tr:has(.action-col)",
            new Page.LocatorOptions().setHasText(participantName)).first();
        participantRow.waitFor();
        participantRow.locator("details.kebab-menu").evaluate("el => el.open = true");
        participantRow.locator("form[action*='/remove'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void ensureParticipantOnTrip(final String participantName) {
        for (int i = 0; i < 40; i++) {
            final var participantRow = page.locator("tr:has(.action-col)",
                new Page.LocatorOptions().setHasText(participantName)).first();
            if (participantRow.count() > 0) {
                return;
            }
            final var option = page.locator("form[action$='/participants'] select[name=participantId] option")
                .filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText(participantName));
            if (option.count() > 0) {
                page.selectOption("form[action$='/participants'] select[name=participantId]",
                    new com.microsoft.playwright.options.SelectOption().setLabel(participantName));
                page.locator("form[action$='/participants'] button[type=submit]").click();
                page.waitForLoadState(LoadState.NETWORKIDLE);
                continue;
            }
            page.waitForTimeout(500);
            navigateAndWait("/trips/" + tripId);
        }
        throw new AssertionError("Participant not available on trip: " + participantName);
    }

    private void assertParticipantRowMissing(final String participantName) {
        final var participantRow = page.locator("tr:has(.action-col)",
            new Page.LocatorOptions().setHasText(participantName)).first();
        assertThat(participantRow.count()).isZero();
    }

    private void assertCompanionDeletionBlocked(final String firstName, final String message) {
        for (int i = 0; i < 30; i++) {
            navigateAndWait("/iam/dashboard");
            page.onceDialog(dialog -> dialog.accept());
            clickAndWaitForHtmx("#companions tr:has-text('" + firstName + "') button.btn-icon--danger");
            if (toastContains(page, message)) {
                return;
            }
            if (!page.locator("#companions").innerHTML().contains(firstName)) {
                throw new AssertionError("Companion was deleted before trip participation block was applied: " + firstName);
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Companion deletion was not blocked for active trip participation: " + firstName);
    }

    private void assertMemberDeletionBlocked(final String firstName, final String message) {
        for (int i = 0; i < 30; i++) {
            navigateAndWait("/iam/dashboard");
            page.onceDialog(dialog -> dialog.accept());
            page.waitForResponse(
                response -> response.url().contains("/iam/dashboard/members/"),
                () -> page.locator("#members tr:has-text('" + firstName + "') button.btn-icon--danger").click()
            );
            page.waitForLoadState(LoadState.NETWORKIDLE);
            if (toastContains(page, message)) {
                return;
            }
            if (!page.locator("#members").innerHTML().contains(firstName)) {
                throw new AssertionError("Member was deleted before trip participation block was applied: " + firstName);
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Member deletion was not blocked for active trip participation: " + firstName);
    }

    private void deleteMemberWhenAllowed(final String firstName) {
        for (int i = 0; i < 30; i++) {
            navigateAndWait("/iam/dashboard");
            if (!page.locator("#members").innerHTML().contains(firstName)) {
                return;
            }
            page.onceDialog(dialog -> dialog.accept());
            page.waitForResponse(
                response -> response.url().contains("/iam/dashboard/members/"),
                () -> page.locator("#members tr:has-text('" + firstName + "') button.btn-icon--danger").click()
            );
            page.waitForLoadState(LoadState.NETWORKIDLE);
            if (!page.locator("#members").innerHTML().contains(firstName)) {
                return;
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Member was not deletable after trip removal: " + firstName);
    }

    private void deleteCompanionWhenAllowed(final String firstName) {
        for (int i = 0; i < 30; i++) {
            navigateAndWait("/iam/dashboard");
            if (!page.locator("#companions").innerHTML().contains(firstName)) {
                return;
            }
            page.onceDialog(dialog -> dialog.accept());
            clickAndWaitForHtmx("#companions tr:has-text('" + firstName + "') button.btn-icon--danger");
            if (!page.locator("#companions").innerHTML().contains(firstName)) {
                return;
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Companion was not deletable after trip removal: " + firstName);
    }

    private boolean toastContains(final Page targetPage, final String message) {
        for (int i = 0; i < 20; i++) {
            final String toastText = targetPage.locator("#toast-container").innerText();
            if (toastText.contains(message)) {
                return true;
            }
            targetPage.waitForTimeout(250);
        }
        return false;
    }

    private String extractTripId(final String url) {
        final String path = url.replaceFirst(".*/trips/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
