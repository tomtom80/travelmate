package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.de.Und;

/**
 * Step definitions for 14-date-poll.feature.
 */
public class DatePollSteps {

    private static final String TRIP_NAME = "BDD-Terminpoll " + RUN_ID;

    private static String tripDetailPath;
    private static boolean tripCreated = false;
    private static boolean pollCreated = false;

    @Angenommen("es existiert eine Reise fuer die Terminabstimmung")
    public void esExistiertEineReiseFuerDieTerminabstimmung() {
        if (!tripCreated) {
            navigateAndWait("/trips/");
            if (page.content().contains("kc-login") || page.url().contains("realms/travelmate")) {
                page.fill("#username", "bdd-" + RUN_ID + "@e2e.test");
                page.fill("#password", "Test1234!");
                page.click("#kc-login");
                page.waitForURL(url -> !url.contains("realms/travelmate"));
                waitForTripsReady();
            }

            createTripWithoutDates(TRIP_NAME);
            openTripFromList(TRIP_NAME);
            tripDetailPath = page.url().replace(BASE_URL, "");

            tripCreated = true;
        }
    }

    @Wenn("ich die Terminabstimmungsseite der Reise oeffne")
    public void ichDieTerminabstimmungsseiteOeffne() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/datepoll");
    }

    @Dann("sehe ich den Hinweis dass noch keine Terminabstimmung vorhanden ist")
    public void seheIchDenHinweisDassNochKeineTerminabstimmungVorhandenIst() {
        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("keine Terminabstimmung"),
            c -> assertThat(c).containsIgnoringCase("No date poll"),
            c -> assertThat(c).containsIgnoringCase("Terminabstimmung erstellen")
        );
    }

    @Und("ich eine Terminabstimmung mit zwei Zeitraeumen erstelle")
    public void ichEineTerminabstimmungMitZweiZeitraeunenErstelle() {
        // Navigate to create form
        page.locator("a[href*='datepoll/create']").click();
        page.waitForLoadState();

        // Fill in the two date ranges (the form has 2 pre-rendered pairs)
        final var startInputs = page.locator("input[name=startDate]");
        final var endInputs = page.locator("input[name=endDate]");

        startInputs.nth(0).fill("2026-09-01");
        endInputs.nth(0).fill("2026-09-14");
        startInputs.nth(1).fill("2026-10-01");
        endInputs.nth(1).fill("2026-10-14");

        page.locator("button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        pollCreated = true;
    }

    @Dann("sehe ich die Terminabstimmung mit Status {string}")
    public void seheIchDieTerminabstimmungMitStatus(final String status) {
        assertThat(page.content()).contains(status);
    }

    @Und("die Abstimmung hat {int} Zeitraumoptionen")
    public void dieAbstimmungHatZeitraumoptionen(final int count) {
        // Each option has a checkbox for voting
        final int checkboxes = page.locator("input[name=selectedOptionIds]").count();
        assertThat(checkboxes).isEqualTo(count);
    }

    @Angenommen("eine Terminabstimmung wurde fuer die Reise erstellt")
    public void eineTerminabstimmungWurdeFuerDieReiseErstellt() {
        if (!pollCreated) {
            ichDieTerminabstimmungsseiteOeffne();
            ichEineTerminabstimmungMitZweiZeitraeunenErstelle();
        } else {
            ichDieTerminabstimmungsseiteOeffne();
        }
    }

    @Wenn("ich fuer den ersten Zeitraum abstimme")
    public void ichFuerDenErstenZeitraumAbstimme() {
        page.locator("input[name=selectedOptionIds]").first().check();
        page.locator("button[type=submit]:has-text('Abstimmen'), button[type=submit]:has-text('Submit Vote')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Dann("hat der erste Zeitraum mindestens {int} Stimme")
    public void hatDerErsteZeitraumMindestensStimmen(final int minVotes) {
        // After voting, the page should still show the poll with at least one checked checkbox
        assertThat(page.locator("input[name=selectedOptionIds]:checked").count())
            .isGreaterThanOrEqualTo(minVotes);
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        return PlaywrightHooks.extractTripId(url);
    }
}
