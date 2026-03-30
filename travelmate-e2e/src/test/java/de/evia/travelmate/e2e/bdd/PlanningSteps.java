package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

/**
 * Step definitions for 16-planning.feature.
 */
public class PlanningSteps {

    private static final String TRIP_NAME = "BDD-Planung " + RUN_ID;

    private static String tripDetailPath;
    private static boolean tripCreated = false;

    @Angenommen("es existiert eine Reise fuer die Planungsuebersicht")
    public void esExistiertEineReiseFuerDiePlanungsuebersicht() {
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

    @Wenn("ich die Planungsseite der Reise oeffne")
    public void ichDiePlanungsseiteOeffne() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/planning");
    }

    @Dann("sehe ich den Status {string} fuer beide Abstimmungen")
    public void seheIchDenStatusFuerBeideAbstimmungen(final String status) {
        final String content = page.content();
        // The planning page shows "Nicht gestartet" for each poll when none exist
        final long count = content.split(status, -1).length - 1;
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Dann("sehe ich die neue Planungsueberschrift mit Ruecksprung zur Reise")
    public void seheIchDieNeuePlanungsueberschriftMitRuecksprungZurReise() {
        assertThat(page.locator("h1").textContent()).containsIgnoringCase("planung");
        final var backLink = page.locator("hgroup a[href*='/trips/']").first();
        assertThat(backLink.count()).isPositive();
        assertThat(backLink.textContent()).contains(TRIP_NAME);
    }

    @Dann("der Untertitel erwaehnt Reisezeitraum und Unterkunft")
    public void derUntertitelErwaehntReisezeitraumUndUnterkunft() {
        final String content = page.content();
        assertThat(content).contains("Reisezeitraum");
        assertThat(content).contains("Unterkunft");
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        return PlaywrightHooks.extractTripId(url);
    }
}
