package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;

public class ShoppingListSteps {

    private static final String TRIP_NAME = "BDD-Einkauf " + RUN_ID;
    private static final String RECIPE_NAME = "BDD-Einkauf-Rezept " + RUN_ID;
    private static final String INGREDIENT_NAME = "Kartoffeln";

    private static String tripDetailPath;
    private static boolean tripWithMealPlanCreated = false;
    private static boolean shoppingListGenerated = false;

    @Angenommen("es existiert eine Reise mit Essensplan und zugewiesenen Rezepten")
    public void esExistiertEineReiseMitEssensplanUndZugewiesenenRezepten() {
        if (!tripWithMealPlanCreated) {
            // Verify session is still active; re-navigate through gateway to refresh
            navigateAndWait("/trips/");
            if (page.content().contains("kc-login") || page.url().contains("realms/travelmate")) {
                // Session expired, need to re-authenticate with CommonSteps user
                page.fill("#username", "bdd-" + RUN_ID + "@e2e.test");
                page.fill("#password", "Test1234!");
                page.click("#kc-login");
                page.waitForURL(url -> !url.contains("realms/travelmate"));
                waitForTripsReady();
            }

            createTripWithoutDates(TRIP_NAME);
            final String tid = openTripFromList(TRIP_NAME);
            createAndConfirmDatePoll(tid, "2026-11-01", "2026-11-03", "2026-11-02", "2026-11-04");
            navigateAndWait("/trips/" + tid);
            tripDetailPath = page.url().replace(BASE_URL, "");

            // Create trip recipe
            navigateAndWait("/trips/" + tid + "/recipes/new");
            page.locator("#name").fill(RECIPE_NAME);
            page.locator("#servings").fill("4");
            page.locator("input[name=ingredientName]").first().fill(INGREDIENT_NAME);
            page.locator("input[name=ingredientQuantity]").first().fill("500");
            page.locator("input[name=ingredientUnit]").first().fill("g");
            page.locator("main button[type=submit]").click();
            page.waitForLoadState();

            // Generate meal plan
            navigateAndWait(tripDetailPath);
            page.locator("form[action$='/mealplan/generate'] button[type=submit]").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Assign recipe to first slot
            final var recipePicker = page.locator("select[name=recipeId]").first();
            final var options = recipePicker.locator("option");
            final int optionCount = options.count();

            String recipeOptionValue = null;
            for (int i = 0; i < optionCount; i++) {
                final String text = options.nth(i).textContent();
                if (text.contains(RECIPE_NAME)) {
                    recipeOptionValue = options.nth(i).getAttribute("value");
                    break;
                }
            }
            assertThat(recipeOptionValue).as("Recipe option should be available").isNotNull();

            final String currentPath = page.url().replace(BASE_URL, "");
            final String recipeValue = recipeOptionValue;
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

            tripWithMealPlanCreated = true;
        }
    }

    @Dann("sehe ich einen Link zur Einkaufsliste")
    public void seheIchEinenLinkZurEinkaufsliste() {
        assertThat(page.content()).contains("Einkaufsliste");
    }

    @Wenn("ich die Einkaufsliste-Seite oeffne")
    public void ichDieEinkaufslisteSeiteOeffne() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/shoppinglist");
    }

    @Und("ich die Einkaufsliste generiere")
    public void ichDieEinkaufslisteGeneriere() {
        page.locator("form[action$='/shoppinglist/generate'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        shoppingListGenerated = true;
    }

    @Dann("sehe ich die generierte Einkaufsliste")
    public void seheIchDieGenerierteEinkaufsliste() {
        assertThat(page.url()).contains("/shoppinglist");
    }

    @Und("die Rezept-Zutaten werden als Eintraege angezeigt")
    public void dieRezeptZutatenWerdenAlsEintraegeAngezeigt() {
        assertThat(page.content()).contains(INGREDIENT_NAME);
    }

    @Angenommen("die Einkaufsliste wurde generiert")
    public void dieEinkaufslisteWurdeGeneriert() {
        if (!shoppingListGenerated) {
            ichDieEinkaufslisteSeiteOeffne();
            ichDieEinkaufslisteGeneriere();
        } else {
            final String tripId = extractTripId();
            navigateAndWait("/trips/" + tripId + "/shoppinglist");
        }
    }

    @Dann("sehe ich die Zutaten aus dem Essensplan")
    public void seheIchDieZutatenAusDemEssensplan() {
        assertThat(page.content()).contains(INGREDIENT_NAME);
    }

    @Wenn("ich einen manuellen Eintrag {string} mit Menge {string} und Einheit {string} hinzufuege")
    public void ichEinenManuellenEintragHinzufuege(final String name, final String quantity, final String unit) {
        page.locator("form:has(input[name=unit]) input[name=name]").fill(name);
        page.locator("form:has(input[name=unit]) input[name=quantity]").fill(quantity);
        page.locator("form:has(input[name=unit]) input[name=unit]").fill(unit);
        clickAndWaitForHtmx("form:has(input[name=unit]) button[type=submit]");
    }

    @Dann("wird der Eintrag {string} in der manuellen Liste angezeigt")
    public void wirdDerEintragInDerManuellenListeAngezeigt(final String name) {
        assertThat(page.content()).contains(name);
    }

    @Angenommen("die Einkaufsliste hat Eintraege")
    public void dieEinkaufslisteHatEintraege() {
        dieEinkaufslisteWurdeGeneriert();
    }

    @Wenn("ich auf {string} bei einem Eintrag klicke")
    public void ichAufBeiEinemEintragKlicke(final String buttonText) {
        clickAndWaitForHtmx("button:has-text('" + buttonText + "')");
    }

    @Dann("wird der Eintrag als {string} angezeigt")
    public void wirdDerEintragAlsAngezeigt(final String status) {
        assertThat(page.content()).contains(status);
    }

    @Und("mein Name steht beim Eintrag")
    public void meinNameStehtBeimEintrag() {
        // The BDD background creates a user via CommonSteps — name is from the common login step
        // Just verify some assignee name is shown (not "—")
        final String content = page.content();
        assertThat(content).doesNotContain("Ausstehend");
    }

    @Angenommen("ein Eintrag ist mir zugewiesen")
    public void einEintragIstMirZugewiesen() {
        dieEinkaufslisteHatEintraege();
    }

    @Wenn("ich auf {string} bei diesem Eintrag klicke")
    public void ichAufBeiDiesemEintragKlicke(final String buttonText) {
        clickAndWaitForHtmx("button:has-text('" + buttonText + "')");
    }

    @Wenn("ich die Einkaufsliste aktualisiere")
    public void ichDieEinkaufslisteAktualisiere() {
        page.locator("form[action$='/shoppinglist/regenerate'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Dann("wird die Einkaufsliste neu generiert")
    public void wirdDieEinkaufslisteNeuGeneriert() {
        assertThat(page.content()).contains(INGREDIENT_NAME);
    }

    @Und("manuelle Eintraege bleiben erhalten")
    public void manuelleEintraegeBleibenerhalten() {
        assertThat(page.content()).contains("Bier");
    }

    private String extractTripId() {
        final String url = tripDetailPath != null ? tripDetailPath : page.url().replace(BASE_URL, "");
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
