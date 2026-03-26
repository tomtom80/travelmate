package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.de.Und;

/**
 * Step definitions for 09-accommodation.feature.
 */
public class AccommodationSteps {

    private static final String TRIP_NAME = "BDD-Unterkunft " + RUN_ID;

    private static String tripDetailPath;
    private static boolean tripCreated = false;
    private static boolean accommodationCreated = false;

    @Angenommen("es existiert eine Reise fuer die Unterkunftsverwaltung")
    public void esExistiertEineReiseFuerDieUnterkunftsverwaltung() {
        if (!tripCreated) {
            // Verify session is still active
            navigateAndWait("/trips/");
            if (page.content().contains("kc-login") || page.url().contains("realms/travelmate")) {
                page.fill("#username", "bdd-" + RUN_ID + "@e2e.test");
                page.fill("#password", "Test1234!");
                page.click("#kc-login");
                page.waitForURL(url -> !url.contains("realms/travelmate"));
                waitForTripsReady();
            }

            // Create trip
            navigateAndWait("/trips/new");
            page.locator("#name, input[name=name]").first().fill(TRIP_NAME);
            page.locator("#startDate, input[name=startDate]").first().fill("2026-12-01");
            page.locator("#endDate, input[name=endDate]").first().fill("2026-12-05");
            page.locator("main button[type=submit]").click();
            page.waitForLoadState();

            // Navigate to trip detail from list
            page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
            page.waitForLoadState();
            tripDetailPath = page.url().replace(BASE_URL, "");

            tripCreated = true;
        }
    }

    @Wenn("ich die Unterkunftsseite der Reise oeffne")
    public void ichDieUnterkunftsseiteOeffne() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/accommodation");
    }

    @Dann("sehe ich den Hinweis dass noch keine Unterkunft hinterlegt ist")
    public void seheIchDenHinweisDassNochKeineUnterkunftHinterlegtIst() {
        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).contains("Noch keine Unterkunft"),
            c -> assertThat(c).contains("Unterkunft hinzufuegen")
        );
    }

    @Und("ich eine Unterkunft mit Name {string} und Adresse {string} und Gesamtpreis {string} erfasse")
    public void ichEineUnterkunftErfasse(final String name, final String address, final String price) {
        // Open dialog
        page.locator("button:has-text('Unterkunft hinzufuegen')").click();
        page.waitForSelector("dialog[open]");

        // Fill form
        page.locator("dialog input[name=name]").fill(name);
        page.locator("dialog input[name=address]").fill(address);
        page.locator("dialog input[name=totalPrice]").fill(price);

        // Fill first room (required in add dialog)
        page.locator("dialog input[name=roomName]").fill("Standardzimmer");
        page.locator("dialog input[name=roomBedCount]").fill("2");

        // Submit
        page.locator("dialog button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        accommodationCreated = true;
    }

    @Dann("sehe ich die Unterkunftsdetails mit Name {string}")
    public void seheIchDieUnterkunftsdetailsMitName(final String name) {
        assertThat(page.content()).contains(name);
    }

    @Angenommen("eine Unterkunft wurde fuer die Reise erfasst")
    public void eineUnterkunftWurdeFuerDieReiseErfasst() {
        if (!accommodationCreated) {
            ichDieUnterkunftsseiteOeffne();
            ichEineUnterkunftErfasse("BDD-Huette", "Testweg 1", "1200");
        } else {
            final String tripId = extractTripId();
            navigateAndWait("/trips/" + tripId + "/accommodation");
        }
    }

    @Wenn("ich ein Zimmer mit Name {string} und Betten {string} hinzufuege")
    public void ichEinZimmerHinzufuege(final String name, final String beds) {
        // Open "Zimmer hinzufuegen" dialog
        page.locator("button[onclick*='add-room-dialog'][onclick*='showModal']").click();

        // Fill room form in dialog
        page.locator("dialog[open] input[name=name]").fill(name);
        page.locator("dialog[open] input[name=bedCount]").fill(beds);

        // Submit
        page.locator("dialog[open] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Dann("wird das Zimmer {string} in der Zimmerliste angezeigt")
    public void wirdDasZimmerInDerZimmerlisteAngezeigt(final String roomName) {
        assertThat(page.content()).contains(roomName);
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
