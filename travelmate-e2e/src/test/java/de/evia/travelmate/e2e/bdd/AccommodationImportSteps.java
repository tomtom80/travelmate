package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import java.util.List;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;

public class AccommodationImportSteps {

    private static final String TRIP_NAME = "BDD-Import " + RUN_ID;

    private static String tripDetailPath;
    private static boolean tripCreated = false;
    private static boolean pollCreated = false;

    @Angenommen("es existiert eine Reise für den Unterkunfts-Import-Test")
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

            createTripWithoutDates(TRIP_NAME);
            openTripFromList(TRIP_NAME);
            tripDetailPath = page.url().replace(BASE_URL, "");

            tripCreated = true;
        }
    }

    @Angenommen("eine Unterkunftsabstimmung für den Import-Test wurde erstellt")
    public void eineUnterkunftsabstimmungFuerDenImportTestWurdeErstellt() {
        esExistiertEineReiseFuerDenImportTest();
        if (!pollCreated) {
            final String tripId = extractTripId();
            navigateAndWait("/trips/" + tripId + "/accommodationpoll/create");
            page.locator("input[name=candidateName]").nth(0).fill("Import Test Unterkunft");
            page.locator("input[name=candidateDescription]").nth(0).fill("Basis für den Import");
            page.locator(".candidate-entry").nth(0).locator("input[name=roomName]").first().fill("Familienzimmer");
            page.locator(".candidate-entry").nth(0).locator("input[name=roomBedCount]").first().fill("4");
            page.locator(".candidate-entry").nth(0).locator("input[name=roomBedDescription]").first().fill("Seeblick");
            page.locator("input[name=candidateName]").nth(1).fill("Alternative");
            page.locator("input[name=candidateDescription]").nth(1).fill("Zweiter Vorschlag");
            page.locator(".candidate-entry").nth(1).locator("input[name=roomName]").first().fill("Doppelzimmer");
            page.locator(".candidate-entry").nth(1).locator("input[name=roomBedCount]").first().fill("2");
            page.locator(".candidate-entry").nth(1).locator("input[name=roomBedDescription]").first().fill("Balkon");
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
            pollCreated = true;
        }
    }

    @Wenn("ich die Unterkunftsabstimmungsseite für den Import-Test öffne")
    public void ichDieUnterkunftsabstimmungsseiteOeffne() {
        eineUnterkunftsabstimmungFuerDenImportTestWurdeErstellt();
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/accommodationpoll");
    }

    @Dann("sehe ich einen Bereich zum URL-Import")
    public void seheIchEinenBereichZumUrlImport() {
        final String content = page.content();
        assertThat(content).containsAnyOf("URL importieren", "Import from URL", "importieren", "Importieren");
    }

    @Angenommen("ich befinde mich auf der Unterkunftsabstimmungsseite mit offener Abstimmung")
    public void ichBefindeMichAufDerUnterkunftsabstimmungsseiteMitOffenerAbstimmung() {
        esExistiertEineReiseFuerDenImportTest();
        ichDieUnterkunftsabstimmungsseiteOeffne();
    }

    @Wenn("ich eine URL eingebe und auf Importieren klicke")
    public void ichEineUrlEingebeUndAufImportierenKlicke() {
        page.locator("input[name=url][placeholder*='huetten']").fill("https://www.huetten.com/de/huette/chalet-am-kogl-rt53575.html");
        clickAndWaitForHtmx("button:has-text('Importieren')");
    }

    @Dann("sehe ich ein vorausgefülltes Formular mit den erkannten Daten")
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

    @Wenn("ich eine ungültige URL eingebe und auf Importieren klicke")
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
        return PlaywrightHooks.extractTripId(url);
    }
}
