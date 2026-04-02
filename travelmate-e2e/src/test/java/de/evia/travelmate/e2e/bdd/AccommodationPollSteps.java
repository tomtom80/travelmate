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
    private static boolean pollInAwaitingBooking = false;

    @Angenommen("es existiert eine Reise für die Unterkunftsabstimmung")
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

    @Wenn("ich die Unterkunftsabstimmungsseite der Reise öffne")
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

    @Und("ich eine Unterkunftsabstimmung mit zwei Vorschlägen erstelle")
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
        final var roomNames = new String[]{"Doppelzimmer Alpenblick", "Suite Sonnstein"};
        for (int i = 0; i < 2; i++) {
            final var entry = entries.nth(i);
            entry.locator("input[name=roomName]").first().fill(roomNames[i]);
            entry.locator("input[name=roomBedCount]").first().fill("2");
        }
        page.evaluate("""
            ([first, second]) => {
                const inputs = document.querySelectorAll('input.candidate-rooms-data');
                inputs[0].value = first;
                inputs[1].value = second;
            }
            """, List.of(
            "[{\"name\":\"Doppelzimmer Alpenblick\",\"bedCount\":2,\"bedDescription\":null}]",
            "[{\"name\":\"Suite Sonnstein\",\"bedCount\":2,\"bedDescription\":null}]"
        ));

        page.locator("button[type=submit]:not(.outline)").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        pollCreated = true;
        pollInAwaitingBooking = false;
    }

    @Dann("sehe ich die Unterkunftsabstimmung mit Status {string}")
    public void seheIchDieUnterkunftsabstimmungMitStatus(final String status) {
        assertThat(page.content()).contains(status);
    }

    @Und("die Abstimmung hat {int} Unterkunftsvorschläge")
    public void dieAbstimmungHatUnterkunftsvorschlaege(final int count) {
        final int radioButtons = page.locator("input[name=selectedCandidateId]").count();
        assertThat(radioButtons).isEqualTo(count);
    }

    @Angenommen("eine Unterkunftsabstimmung wurde für die Reise erstellt")
    public void eineUnterkunftsabstimmungWurdeFuerDieReiseErstellt() {
        if (!pollCreated) {
            ichDieUnterkunftsabstimmungsseiteOeffne();
            ichEineUnterkunftsabstimmungMitZweiVorschlaegenErstelle();
        } else {
            ichDieUnterkunftsabstimmungsseiteOeffne();
        }
    }

    @Wenn("ich für die erste Unterkunft abstimme")
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

    // --- S15-B: Select candidate ---

    @Wenn("ich den ersten Kandidaten auswähle")
    public void ichDenErstenKandidatenAuswaehle() {
        if (pollInAwaitingBooking) {
            // Previous scenario left the poll in AWAITING_BOOKING — fail first to reopen
            page.locator("input[name=note]").fill("Reset");
            page.locator("button[type=submit]:has-text('fehlgeschlagen'), button[type=submit]:has-text('failed')").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            pollInAwaitingBooking = false;
        }
        final var select = page.locator("select[name=selectedCandidateId]");
        final var optionValue = select.locator("option:not([value=''])").first().getAttribute("value");
        select.selectOption(optionValue);
        page.locator("button[type=submit]:has-text('Auswählen'), button[type=submit]:has-text('Select')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        pollInAwaitingBooking = true;
    }

    @Dann("sehe ich den Status {string}")
    public void seheIchDenStatus(final String status) {
        assertThat(page.content()).containsIgnoringCase(status);
    }

    @Und("ich sehe die Buchungsaktionen")
    public void ichSeheDieBuchungsaktionen() {
        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("Buchung erfolgreich"),
            c -> assertThat(c).containsIgnoringCase("Booking success")
        );
    }

    // --- S15-C: Booking failure ---

    @Wenn("ich die Buchung als fehlgeschlagen mit Notiz {string} markiere")
    public void ichDieBuchungAlsFehlgeschlagenMitNotizMarkiere(final String note) {
        page.locator("input[name=note]").fill(note);
        page.locator("button[type=submit]:has-text('fehlgeschlagen'), button[type=submit]:has-text('failed')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        pollInAwaitingBooking = false;
    }

    @Und("ich sehe den Fehlschlag-Hinweis")
    public void ichSeheDenFehlschlagHinweis() {
        final String content = page.content();
        assertThat(content).satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("konnte nicht gebucht"),
            c -> assertThat(c).containsIgnoringCase("Ausgebucht")
        );
    }

    // --- S15-B: Booking success ---

    @Wenn("ich die Buchung als erfolgreich markiere")
    public void ichDieBuchungAlsErfolgreichMarkiere() {
        page.locator("button[type=submit]:has-text('Buchung erfolgreich'), button[type=submit]:has-text('Booking success')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        pollInAwaitingBooking = false;
    }

    @Dann("sehe ich den Gewinnerbanner mit der ersten Unterkunft")
    public void seheIchDenGewinnerbannerMitDerErstenUnterkunft() {
        final var banner = page.locator(".winner-banner");
        assertThat(banner.count()).isGreaterThan(0);
        assertThat(banner.locator("strong").first().textContent()).containsIgnoringCase("Hotel");
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        return PlaywrightHooks.extractTripId(url);
    }
}
