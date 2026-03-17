package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;

public class RecipeSteps {

    @Angenommen("ich befinde mich auf der Rezeptseite")
    public void ichBefindeMichAufDerRezeptseite() {
        navigateAndWait("/trips/recipes");
    }

    @Angenommen("ich befinde mich auf einer beliebigen Seite")
    public void ichBefindeMichAufEinerBeliebigenSeite() {
        navigateAndWait("/trips/");
    }

    @Dann("sehe ich die Meldung {string}")
    public void seheIchDieMeldung(final String meldung) {
        assertThat(page.content()).contains(meldung);
    }

    @Wenn("ich auf {string} klicke")
    public void ichAufKlicke(final String buttonText) {
        switch (buttonText) {
            case "Neues Rezept" -> navigateAndWait("/trips/recipes/new");
            case "Zutat hinzufuegen" -> page.click("#add-ingredient-btn");
            case "Zurueck" -> {
                final String tripId = extractTripIdFromPage();
                page.locator("main a[href*='/" + tripId + "']").first().click();
                page.waitForLoadState();
            }
            default -> page.locator("a", new com.microsoft.playwright.Page.LocatorOptions()
                .setHasText(buttonText)).click();
        }
    }

    @Dann("sehe ich ein Formular mit den Feldern Name, Portionen und Zutaten")
    public void seheIchEinFormularMitDenFeldern() {
        assertThat(page.locator("input[name=name]").count()).isPositive();
        assertThat(page.locator("input[name=servings]").count()).isPositive();
        assertThat(page.locator("input[name=ingredientName]").count()).isPositive();
    }

    @Und("es gibt eine leere Zutatenzeile")
    public void esGibtEineLeereZutatenzeile() {
        assertThat(page.locator(".ingredient-row").count()).isPositive();
    }

    @Und("es gibt einen Button {string}")
    public void esGibtEinenButton(final String buttonText) {
        if ("Zutat hinzufuegen".equals(buttonText)) {
            assertThat(page.locator("#add-ingredient-btn").count()).isPositive();
        }
    }

    @Und("ich den Namen {string} eingebe")
    public void ichDenNamenEingebe(final String name) {
        page.fill("input[name=name]", name);
    }

    @Und("ich die Portionen auf {string} setze")
    public void ichDiePortionenAufSetze(final String portionen) {
        page.fill("input[name=servings]", portionen);
    }

    @Und("ich die Zutat {string} mit Menge {string} und Einheit {string} eingebe")
    public void ichDieZutatEingebe(final String zutat, final String menge, final String einheit) {
        page.locator("input[name=ingredientName]").first().fill(zutat);
        page.locator("input[name=ingredientQuantity]").first().fill(menge);
        page.locator("input[name=ingredientUnit]").first().fill(einheit);
    }

    @Und("ich eine weitere Zutat {string} mit Menge {string} und Einheit {string} hinzufuege")
    public void ichEineWeitereZutatHinzufuege(final String zutat, final String menge, final String einheit) {
        page.click("#add-ingredient-btn");
        page.locator("input[name=ingredientName]").last().fill(zutat);
        page.locator("input[name=ingredientQuantity]").last().fill(menge);
        page.locator("input[name=ingredientUnit]").last().fill(einheit);
    }

    @Dann("werde ich zur Rezeptliste weitergeleitet")
    public void werdeIchZurRezeptlisteWeitergeleitet() {
        assertThat(page.url()).contains("/trips/recipes");
    }

    @Und("ich sehe {string} in der Liste")
    public void ichSeheInDerListe(final String text) {
        assertThat(page.content()).contains(text);
    }

    @Dann("sehe ich in der Rezepttabelle den Namen {string}")
    public void seheIchInDerRezepttabelleDenNamen(final String name) {
        navigateAndWait("/trips/recipes");
        final var row = page.locator("tr",
            new com.microsoft.playwright.Page.LocatorOptions().setHasText(name));
        assertThat(row.count()).isPositive();
    }

    @Und("die Portionenzahl {string}")
    public void diePortionenzahl(final String portionen) {
        assertThat(page.content()).contains(portionen);
    }

    @Und("die Zutatenzahl {string}")
    public void dieZutatenzahl(final String anzahl) {
        assertThat(page.content()).contains(anzahl);
    }

    @Und("Buttons zum Bearbeiten und Loeschen")
    public void buttonsZumBearbeitenUndLoeschen() {
        assertThat(page.locator("a[href*='/edit']").count()).isPositive();
        assertThat(page.locator("form[action*='/delete'] button[type=submit]").count()).isPositive();
    }

    @Wenn("ich beim Rezept {string} auf {string} klicke")
    public void ichBeimRezeptAufKlicke(final String rezeptName, final String aktion) {
        navigateAndWait("/trips/recipes");
        final var row = page.locator("tr",
            new com.microsoft.playwright.Page.LocatorOptions().setHasText(rezeptName));

        if ("Bearbeiten".equals(aktion)) {
            row.locator("a[href*='/edit']").click();
            page.waitForLoadState();
        } else if ("Loeschen".equals(aktion)) {
            page.onDialog(dialog -> dialog.accept());
            row.locator("form[action*='/delete'] button[type=submit]").click();
            page.waitForLoadState();
        }
    }

    @Dann("sehe ich das Formular mit den vorausgefuellten Werten")
    public void seheIchDasFormularMitDenVorausgefuelltenWerten() {
        assertThat(page.locator("input[name=name]").inputValue()).isNotEmpty();
        assertThat(page.locator("input[name=servings]").inputValue()).isNotEmpty();
    }

    @Wenn("ich den Namen auf {string} aendere")
    public void ichDenNamenAufAendere(final String neuerName) {
        page.fill("input[name=name]", neuerName);
    }

    @Dann("sehe ich {string} in der Rezeptliste")
    public void seheIchInDerRezeptliste(final String text) {
        assertThat(page.content()).contains(text);
    }

    @Und("die Portionenzahl zeigt {string}")
    public void diePortionenzahlZeigt(final String portionen) {
        assertThat(page.content()).contains(portionen);
    }

    @Und("ich die Loeschbestaetigung akzeptiere")
    public void ichDieLoeschbestaetigungAkzeptiere() {
        // Dialog acceptance already registered in ichBeimRezeptAufKlicke
    }

    @Dann("wird das Rezept aus der Liste entfernt")
    public void wirdDasRezeptAusDerListeEntfernt() {
        assertThat(page.content()).doesNotContain("Pasta Bolognese");
    }

    @Wenn("ich in der Navigation auf {string} klicke")
    public void ichInDerNavigationAufKlicke(final String linkText) {
        if ("Rezepte".equals(linkText)) {
            page.locator("nav a[href='/trips/recipes']").click();
            page.waitForLoadState();
        }
    }

    @Dann("sehe ich eine zusaetzliche Zutatenzeile")
    public void seheIchEineZusaetzlicheZutatenzeile() {
        assertThat(page.locator(".ingredient-row").count()).isGreaterThanOrEqualTo(2);
    }

    private static String extractTripIdFromPage() {
        final String url = page.url();
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
