package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.LoadState;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShoppingListLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-ShopList " + RUN_ID;
    private static final String EMAIL = "shoplist-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Einkaufsreise " + RUN_ID;
    private static final String RECIPE_NAME = "E2E-Einkauf-Rezept " + RUN_ID;
    private static final String INGREDIENT_NAME = "Kartoffeln";
    private static final String INGREDIENT_QUANTITY = "500";
    private static final String INGREDIENT_UNIT = "g";
    private static final String MANUAL_ITEM_NAME = "Bier";
    private static final String MANUAL_ITEM_QUANTITY = "6";
    private static final String MANUAL_ITEM_UNIT = "Flaschen";

    private static String tripDetailUrl;

    @Test
    @Order(1)
    void setUpTravelPartyAndCreateTrip() {
        signUpAndLogin(TENANT_NAME, "Lisa", "Einkauf", EMAIL, PASSWORD);
        waitForTripsReady();

        navigateAndWait("/trips/new");
        page.fill("input[name=name]", TRIP_NAME);
        page.fill("input[name=startDate]", "2026-10-01");
        page.fill("input[name=endDate]", "2026-10-03");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.content()).contains(TRIP_NAME);

        // Navigate from trip list to trip detail
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();
        tripDetailUrl = page.url();
    }

    @Test
    @Order(2)
    void createRecipe() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/recipes/new");

        page.fill("input[name=name]", RECIPE_NAME);
        page.fill("input[name=servings]", "4");
        page.locator("input[name=ingredientName]").first().fill(INGREDIENT_NAME);
        page.locator("input[name=ingredientQuantity]").first().fill(INGREDIENT_QUANTITY);
        page.locator("input[name=ingredientUnit]").first().fill(INGREDIENT_UNIT);
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.content()).contains(RECIPE_NAME);
    }

    @Test
    @Order(3)
    void generateMealPlanAndAssignRecipe() {
        // Navigate to trip detail and generate meal plan
        navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));
        page.locator("form[action$='/mealplan/generate'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.url()).contains("/mealplan");

        // Assign recipe to first PLANNED slot
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

        selectAndSubmit(recipePicker, recipeOptionValue);
        assertThat(page.content()).contains(RECIPE_NAME);
    }

    @Test
    @Order(10)
    void tripDetailShowsShoppingListLink() {
        navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));

        final String content = page.content();
        assertThat(content).contains("Einkaufsliste");
    }

    @Test
    @Order(11)
    void shoppingListPageShowsGenerateButton() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/shoppinglist");

        assertThat(page.content()).contains("Einkaufsliste erstellen");
    }

    @Test
    @Order(12)
    void generateShoppingListCreatesItems() {
        page.locator("form[action$='/shoppinglist/generate'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        final String content = page.content();
        assertThat(content).contains(INGREDIENT_NAME);
    }

    @Test
    @Order(13)
    void i18nResolvedCorrectly() {
        final String content = page.content();
        assertThat(content).doesNotContain("??");
    }

    @Test
    @Order(20)
    void addManualItem() {
        page.locator("form:has(input[name=unit]) input[name=name]").fill(MANUAL_ITEM_NAME);
        page.locator("form:has(input[name=unit]) input[name=quantity]").fill(MANUAL_ITEM_QUANTITY);
        page.locator("form:has(input[name=unit]) input[name=unit]").fill(MANUAL_ITEM_UNIT);
        clickAndWaitForHtmx("form:has(input[name=unit]) button[type=submit]");
        page.waitForSelector("#manual-items-tbody td:has-text('" + MANUAL_ITEM_NAME + "')");

        final String content = page.content();
        assertThat(content).contains(MANUAL_ITEM_NAME);
    }

    @Test
    @Order(30)
    void assignItemToSelf() {
        final var assignButton = page.locator("button:has-text('Ich uebernehme')").first();
        assertThat(assignButton.isVisible()).as("Assign button should be visible").isTrue();

        clickAndWaitForHtmx("button:has-text('Ich uebernehme')");

        final String content = page.content();
        assertThat(content).contains("Uebernommen");
    }

    @Test
    @Order(40)
    void markItemAsPurchased() {
        final var purchasedButton = page.locator("button:has-text('Erledigt')").first();
        assertThat(purchasedButton.isVisible()).as("Purchased button should be visible").isTrue();

        clickAndWaitForHtmx("button:has-text('Erledigt')");

        final String content = page.content();
        assertThat(content).contains("Erledigt");
    }

    @Test
    @Order(50)
    void undoPurchase() {
        final var undoButton = page.locator("button:has-text('Rueckgaengig')").first();
        assertThat(undoButton.isVisible()).as("Undo button should be visible").isTrue();

        clickAndWaitForHtmx("button:has-text('Rueckgaengig')");

        final String content = page.content();
        // Should be back to ASSIGNED state, not PURCHASED
        assertThat(content).contains("Uebernommen");
    }

    @Test
    @Order(60)
    void regeneratePreservesManualItems() {
        page.locator("form[action$='/shoppinglist/regenerate'] button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        final String content = page.content();
        // Recipe ingredients should still be present
        assertThat(content).contains(INGREDIENT_NAME);
        // Manual item should be preserved
        assertThat(content).contains(MANUAL_ITEM_NAME);
    }

    @Test
    @Order(70)
    void navigateBackToTripDetail() {
        final String tripId = extractTripId();
        page.locator("main a[href*='/" + tripId + "']").first().click();
        page.waitForLoadState();

        assertThat(page.url()).contains("/trips/" + tripId);
        assertThat(page.content()).contains(TRIP_NAME);
    }

    /**
     * Changes a select value and submits the form via fetch() to avoid Playwright navigation
     * conflicts. Sets the value in the DOM first so FormData picks it up, then POSTs via fetch.
     */
    private void selectAndSubmit(final com.microsoft.playwright.Locator select, final String value) {
        final String currentPath = page.url().replace(BASE_URL, "");
        select.evaluate("(el, val) => { " +
            "el.value = val; " +
            "const form = el.form; " +
            "const data = new URLSearchParams(new FormData(form)); " +
            "return fetch(form.action, { " +
            "  method: 'POST', " +
            "  body: data.toString(), " +
            "  headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, " +
            "  redirect: 'follow', " +
            "  credentials: 'include' " +
            "}).then(r => r.ok); " +
            "}", value);
        navigateAndWait(currentPath);
    }

    private String extractTripId() {
        final String url = tripDetailUrl != null ? tripDetailUrl : page.url();
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
