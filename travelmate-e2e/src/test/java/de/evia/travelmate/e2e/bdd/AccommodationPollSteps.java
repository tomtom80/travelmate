package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import java.util.List;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.de.Und;

/**
 * Step definitions for 15-accommodation-poll.feature.
 */
public class AccommodationPollSteps {

    private static final String TRIP_NAME = "BDD-Unterkunftspoll " + RUN_ID;

    private static String tripDetailPath;
    private static boolean tripCreated = false;
    private static boolean pollCreated = false;

    @Angenommen("es existiert eine Reise fuer die Unterkunftsabstimmung")
    public void esExistiertEineReiseFuerDieUnterkunftsabstimmung() {
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

    @Wenn("ich die Unterkunftsabstimmungsseite der Reise oeffne")
    public void ichDieUnterkunftsabstimmungsseiteOeffne() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/accommodationpoll");
    }

    @Dann("sehe ich den Hinweis dass noch keine Unterkunftsabstimmung vorhanden ist")
    public void seheIchDenHinweisDassNochKeineUnterkunftsabstimmungVorhandenIst() {
        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("keine Unterkunftsabstimmung"),
            c -> assertThat(c).containsIgnoringCase("No accommodation poll"),
            c -> assertThat(c).containsIgnoringCase("Unterkunftsabstimmung erstellen")
        );
    }

    @Und("ich eine Unterkunftsabstimmung mit zwei Vorschlaegen erstelle")
    public void ichEineUnterkunftsabstimmungMitZweiVorschlaegenErstelle() {
        page.locator("a[href*='accommodationpoll/create']").click();
        page.waitForLoadState();

        final var nameInputs = page.locator("input[name=candidateName]");
        final var urlInputs = page.locator("input[name=candidateUrl]");
        final var descInputs = page.locator("input[name=candidateDescription]");

        nameInputs.nth(0).fill("Hotel Alpenblick");
        urlInputs.nth(0).fill("https://alpenblick.at");
        descInputs.nth(0).fill("Tolle Aussicht");

        nameInputs.nth(1).fill("Berghuette Sonnstein");
        descInputs.nth(1).fill("Gemuetliche Huette");
        final var entries = page.locator(".candidate-entry");
        final var features = new String[]{"Panorama-Balkon, WLAN", "Holzofen, Sauna"};
        final var roomNames = new String[]{"Doppelzimmer Alpenblick", "Suite Sonnstein"};
        for (int i = 0; i < 2; i++) {
            final var entry = entries.nth(i);
            entry.locator("input[name=roomName]").first().fill(roomNames[i]);
            entry.locator("input[name=roomBedCount]").first().fill("2");
            entry.locator("input[name=roomFeatures]").first().fill(features[i]);
        }
        page.evaluate("""
            ([first, second]) => {
                const inputs = document.querySelectorAll('input.candidate-rooms-data');
                inputs[0].value = first;
                inputs[1].value = second;
            }
            """, List.of(
            "[{\"name\":\"Doppelzimmer Alpenblick\",\"bedCount\":2,\"features\":\"Panorama-Balkon, WLAN\"}]",
            "[{\"name\":\"Suite Sonnstein\",\"bedCount\":2,\"features\":\"Holzofen, Sauna\"}]"
        ));

        page.locator("button[type=submit]:not(.outline)").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        pollCreated = true;
    }

    @Dann("sehe ich die Unterkunftsabstimmung mit Status {string}")
    public void seheIchDieUnterkunftsabstimmungMitStatus(final String status) {
        assertThat(page.content()).contains(status);
    }

    @Und("die Abstimmung hat {int} Unterkunftsvorschlaege")
    public void dieAbstimmungHatUnterkunftsvorschlaege(final int count) {
        final int radioButtons = page.locator("input[name=selectedCandidateId]").count();
        assertThat(radioButtons).isEqualTo(count);
    }

    @Angenommen("eine Unterkunftsabstimmung wurde fuer die Reise erstellt")
    public void eineUnterkunftsabstimmungWurdeFuerDieReiseErstellt() {
        if (!pollCreated) {
            ichDieUnterkunftsabstimmungsseiteOeffne();
            ichEineUnterkunftsabstimmungMitZweiVorschlaegenErstelle();
        } else {
            ichDieUnterkunftsabstimmungsseiteOeffne();
        }
    }

    @Wenn("ich fuer die erste Unterkunft abstimme")
    public void ichFuerDieErsteUnterkunftAbstimme() {
        page.locator("input[name=selectedCandidateId]").first().check();
        page.locator("button[type=submit]:has-text('Abstimmen'), button[type=submit]:has-text('Submit Vote')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Dann("hat die erste Unterkunft mindestens {int} Stimme")
    public void hatDieErsteUnterkunftMindestensStimmen(final int minVotes) {
        final String content = page.content();
        assertThat(content).contains("Hotel Alpenblick");
    }

    @Und("ich die erste Unterkunft bestaetige")
    public void ichDieErsteUnterkunftBestaetige() {
        final var select = page.locator("select[name=confirmedCandidateId]");
        final var optionValue = select.locator("option").nth(1).getAttribute("value");
        select.selectOption(optionValue);
        page.locator("button[type=submit]:has-text('Bestaetigen'), button[type=submit]:has-text('Confirm')")
            .click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Dann("sehe ich den Gewinnerbanner mit der ersten Unterkunft")
    public void seheIchDenGewinnerbannerMitDerErstenUnterkunft() {
        final var banner = page.locator(".winner-banner");
        assertThat(banner.count()).isGreaterThan(0);
        assertThat(banner.locator("strong").first().textContent()).containsIgnoringCase("Hotel");
        assertThat(banner.locator(".room-summary").count()).isGreaterThan(0);
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        return PlaywrightHooks.extractTripId(url);
    }
}
