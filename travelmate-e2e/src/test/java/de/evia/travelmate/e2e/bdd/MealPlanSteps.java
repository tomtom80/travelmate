package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;

public class MealPlanSteps {

    private static final String TRIP_NAME = "BDD-Reise " + RUN_ID;
    private static final String RECIPE_NAME = "BDD-Rezept " + RUN_ID;

    private static String tripDetailPath;
    private static boolean tripCreated = false;
    private static boolean mealPlanGenerated = false;
    private static boolean recipeCreated = false;

    @Angenommen("es existiert eine Reise mit Startdatum und Enddatum")
    public void esExistiertEineReiseMitStartdatumUndEnddatum() {
        if (!tripCreated) {
            navigateAndWait("/trips/new");
            page.fill("input[name=name]", TRIP_NAME);
            page.fill("input[name=startDate]", "2026-10-01");
            page.fill("input[name=endDate]", "2026-10-03");
            page.locator("main button[type=submit]").click();
            page.waitForLoadState();
            tripCreated = true;
        }
    }

    @Wenn("ich die Reisedetailseite oeffne")
    public void ichDieReisedetailseiteOeffne() {
        navigateAndWait("/trips/");
        page.locator("a",
            new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();
        tripDetailPath = page.url().replace(BASE_URL, "");
    }

    @Dann("sehe ich einen Button {string}")
    public void seheIchEinenButton(final String buttonText) {
        if ("Essensplan erstellen".equals(buttonText)) {
            assertThat(page.locator("form[action$='/mealplan/generate'] button[type=submit]").count())
                .isPositive();
        }
    }

    @Wenn("ich auf der Reisedetailseite {string} klicke")
    public void ichAufDerReisedetailseiteKlicke(final String buttonText) {
        if ("Essensplan erstellen".equals(buttonText)) {
            ichDieReisedetailseiteOeffne();
            page.locator("form[action$='/mealplan/generate'] button[type=submit]").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            mealPlanGenerated = true;
        }
    }

    @Dann("werde ich zur Essensplan-Uebersicht weitergeleitet")
    public void werdeIchZurEssensplanUebersichtWeitergeleitet() {
        assertThat(page.url()).contains("/mealplan");
    }

    @Und("ich sehe ein Tagesraster mit den Spalten Fruehstueck, Mittagessen und Abendessen")
    public void ichSeheEinTagesraster() {
        final String content = page.content();
        assertThat(content).contains("Fruehstueck");
        assertThat(content).contains("Mittagessen");
        assertThat(content).contains("Abendessen");
    }

    @Und("fuer jeden Reisetag existiert eine Zeile")
    public void fuerJedenReisetagExistiertEineZeile() {
        final String content = page.content();
        assertThat(content).contains("2026-10-01");
        assertThat(content).contains("2026-10-02");
        assertThat(content).contains("2026-10-03");
    }

    @Angenommen("ein Essensplan wurde fuer die Reise erstellt")
    public void einEssensplanWurdeFuerDieReiseErstellt() {
        if (!mealPlanGenerated) {
            ichAufDerReisedetailseiteKlicke("Essensplan erstellen");
        }
    }

    @Dann("sehe ich den Link {string} statt {string}")
    public void seheIchDenLinkStatt(final String neuerLink, final String alterButton) {
        assertThat(page.content()).contains(neuerLink);
        assertThat(page.locator("form[action$='/mealplan/generate']").count()).isZero();
    }

    @Angenommen("ich befinde mich auf der Essensplan-Uebersicht")
    public void ichBefindeMichAufDerEssensplanUebersicht() {
        if (!mealPlanGenerated) {
            ichAufDerReisedetailseiteKlicke("Essensplan erstellen");
        }
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/mealplan");
    }

    @Wenn("ich den Status eines Mahlzeit-Slots auf {string} aendere")
    public void ichDenStatusEinesMahlzeitSlotsAendere(final String status) {
        final String optionValue = mapStatusToOption(status);

        final String currentPath = page.url().replace(BASE_URL, "");
        final var statusSelect = page.locator("select[name=status]").first();
        statusSelect.evaluate("(el, val) => { " +
            "el.value = val; " +
            "const form = el.form; " +
            "const data = new URLSearchParams(new FormData(form)); " +
            "return fetch(form.action, { " +
            "  method: 'POST', " +
            "  body: data, " +
            "  headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, " +
            "  redirect: 'follow', " +
            "  credentials: 'include' " +
            "}).then(r => r.ok); " +
            "}", optionValue);
        navigateAndWait(currentPath);
    }

    @Dann("wird der Slot als {string} angezeigt")
    public void wirdDerSlotAlsAngezeigt(final String status) {
        final String expectedValue = mapStatusToOption(status);
        final var firstStatus = page.locator("select[name=status]").first();
        assertThat(firstStatus.inputValue()).isEqualTo(expectedValue);
    }

    @Angenommen("ein Mahlzeit-Slot hat den Status {string}")
    public void einMahlzeitSlotHatDenStatus(final String status) {
        ichBefindeMichAufDerEssensplanUebersicht();
    }

    @Wenn("ich den Status auf {string} zuruecksetze")
    public void ichDenStatusAufZuruecksetze(final String status) {
        ichDenStatusEinesMahlzeitSlotsAendere(status);
    }

    @Und("ich sehe eine Rezeptauswahl fuer diesen Slot")
    public void ichSeheEineRezeptauswahlFuerDiesenSlot() {
        assertThat(page.locator("select[name=recipeId]").count()).isPositive();
    }

    @Und("es existiert ein Rezept in meiner Rezeptsammlung")
    public void esExistiertEinRezeptInMeinerRezeptsammlung() {
        if (!recipeCreated) {
            navigateAndWait("/trips/recipes/new");
            page.fill("input[name=name]", RECIPE_NAME);
            page.fill("input[name=servings]", "4");
            page.locator("input[name=ingredientName]").first().fill("Nudeln");
            page.locator("input[name=ingredientQuantity]").first().fill("500");
            page.locator("input[name=ingredientUnit]").first().fill("g");
            page.locator("main button[type=submit]").click();
            page.waitForLoadState();
            recipeCreated = true;
        }
        // Navigate back to meal plan overview after recipe creation
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/mealplan");
    }

    @Wenn("ich fuer einen geplanten Slot ein Rezept auswaehle")
    public void ichFuerEinenGeplantenSlotEinRezeptAuswaehle() {
        final var recipePicker = page.locator("select[name=recipeId]").first();
        final var options = recipePicker.locator("option");
        final int optionCount = options.count();

        String foundValue = null;
        for (int i = 0; i < optionCount; i++) {
            final String text = options.nth(i).textContent();
            if (text.contains(RECIPE_NAME)) {
                foundValue = options.nth(i).getAttribute("value");
                break;
            }
        }
        assertThat(foundValue).as("Recipe option should be available").isNotNull();

        final String currentPath = page.url().replace(BASE_URL, "");
        final String recipeValue = foundValue;
        recipePicker.evaluate("(el, val) => { " +
            "el.value = val; " +
            "const form = el.form; " +
            "const data = new URLSearchParams(new FormData(form)); " +
            "return fetch(form.action, { " +
            "  method: 'POST', " +
            "  body: data, " +
            "  headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, " +
            "  redirect: 'follow', " +
            "  credentials: 'include' " +
            "}).then(r => r.ok); " +
            "}", recipeValue);
        navigateAndWait(currentPath);
    }

    @Dann("wird der Rezeptname im Slot angezeigt")
    public void wirdDerRezeptnameImSlotAngezeigt() {
        assertThat(page.content()).contains(RECIPE_NAME);
    }

    @Dann("sehe ich eine Tabelle mit den Spalten Datum, Fruehstueck, Mittagessen, Abendessen")
    public void seheIchEineTabelleMitDenSpalten() {
        final String content = page.content();
        assertThat(content).contains("Fruehstueck");
        assertThat(content).contains("Mittagessen");
        assertThat(content).contains("Abendessen");
    }

    @Und("jeder Slot hat ein Status-Dropdown")
    public void jederSlotHatEinStatusDropdown() {
        final int statusDropdowns = page.locator("select[name=status]").count();
        assertThat(statusDropdowns).isEqualTo(9);
    }

    @Und("geplante Slots haben eine Rezeptauswahl")
    public void geplanteSlotsHabenEineRezeptauswahl() {
        assertThat(page.locator("select[name=recipeId]").count()).isPositive();
    }

    @Dann("werde ich zur Reisedetailseite weitergeleitet")
    public void werdeIchZurReisedetailseiteWeitergeleitet() {
        assertThat(page.url()).contains("/trips/");
        assertThat(page.content()).contains(TRIP_NAME);
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }

    private String mapStatusToOption(final String displayStatus) {
        return switch (displayStatus) {
            case "Auslassen" -> "SKIP";
            case "Auswaerts essen" -> "EATING_OUT";
            case "Geplant" -> "PLANNED";
            default -> displayStatus;
        };
    }
}
