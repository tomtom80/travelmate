package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;

public class AccommodationImportSteps {

    private static final String TRIP_NAME = "BDD-Import " + RUN_ID;

    private static String tripDetailPath;
    private static boolean tripCreated = false;

    @Angenommen("es existiert eine Reise fuer den Unterkunfts-Import-Test")
    public void esExistiertEineReiseFuerDenImportTest() {
        if (!tripCreated) {
            navigateAndWait("/trips/");
            if (page.content().contains("kc-login") || page.url().contains("realms/travelmate")) {
                page.fill("#username", "bdd-" + RUN_ID + "@e2e.test");
                page.fill("#password", "Test1234!");
                page.click("#kc-login");
                page.waitForURL(url -> !url.contains("realms/travelmate"));
                waitForTripsReady();
            }

            navigateAndWait("/trips/new");
            page.locator("#name").fill(TRIP_NAME);
            page.locator("#startDate, input[name=startDate]").first().fill("2026-12-01");
            page.locator("#endDate, input[name=endDate]").first().fill("2026-12-07");
            page.locator("main button[type=submit]").click();
            page.waitForLoadState();

            page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
            page.waitForLoadState();
            tripDetailPath = page.url().replace(BASE_URL, "");

            tripCreated = true;
        }
    }

    @Wenn("ich die Unterkunftsseite fuer den Import-Test oeffne")
    public void ichDieUnterkunftsseiteOeffne() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/accommodation");
    }

    @Dann("sehe ich einen Bereich zum URL-Import")
    public void seheIchEinenBereichZumUrlImport() {
        final String content = page.content();
        assertThat(content).containsAnyOf("URL importieren", "Import from URL", "importieren", "Importieren");
    }

    @Angenommen("ich befinde mich auf der Unterkunftsseite ohne Unterkunft")
    public void ichBefindeMichAufDerUnterkunftsseiteOhneUnterkunft() {
        esExistiertEineReiseFuerDenImportTest();
        ichDieUnterkunftsseiteOeffne();
    }

    @Wenn("ich eine URL eingebe und auf Importieren klicke")
    public void ichEineUrlEingebeUndAufImportierenKlicke() {
        page.locator("input[name=url][placeholder*='huetten']").fill("https://www.huetten.com/de/huette/chalet-am-kogl-rt53575.html");
        clickAndWaitForHtmx("button:has-text('Importieren')");
    }

    @Dann("sehe ich ein vorausgefuelltes Formular mit den erkannten Daten")
    public void seheIchEinVorausgefuelltesFormular() {
        final String content = page.content();
        // The form should be pre-filled with at least the name
        assertThat(page.locator("input[name=name]").inputValue()).isNotEmpty();
    }

    @Und("ich kann die Daten korrigieren und speichern")
    public void ichKannDieDatenKorrigierenUndSpeichern() {
        // The form should be editable and have a save button
        assertThat(page.locator("button[type=submit]").count()).isPositive();
    }

    @Wenn("ich eine ungueltige URL eingebe und auf Importieren klicke")
    public void ichEineUngueligeUrlEingebeUndAufImportierenKlicke() {
        page.locator("input[name=url]").fill("https://invalid.example.com/not-found");
        clickAndWaitForHtmx("button:has-text('Importieren')");
    }

    @Dann("sehe ich eine Fehlermeldung zum Import")
    public void seheIchEineFehlermeldung() {
        final String content = page.content();
        assertThat(content).containsAnyOf("Fehler", "error", "nicht", "Error");
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
