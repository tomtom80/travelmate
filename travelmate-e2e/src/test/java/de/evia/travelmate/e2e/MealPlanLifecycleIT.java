package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MealPlanLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-MealPlan " + RUN_ID;
    private static final String EMAIL = "mealplan-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String TRIP_NAME = "Essensreise " + RUN_ID;
    private static final String RECIPE_NAME = "E2E-Rezept " + RUN_ID;

    private static String tripDetailUrl;

    @Test
    @Order(1)
    void setUpTravelPartyAndCreateTrip() {
        signUpAndLogin(TENANT_NAME, "Kai", "Planer", EMAIL, PASSWORD);
        waitForTripsReady();

        navigateAndWait("/trips/new");
        page.fill("input[name=name]", TRIP_NAME);
        page.fill("input[name=startDate]", "2026-09-01");
        page.fill("input[name=endDate]", "2026-09-03");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.content()).contains(TRIP_NAME);
    }

    @Test
    @Order(2)
    void createRecipeForLaterAssignment() {
        navigateAndWait("/trips/recipes/new");

        page.fill("input[name=name]", RECIPE_NAME);
        page.fill("input[name=servings]", "4");
        page.locator("input[name=ingredientName]").first().fill("Nudeln");
        page.locator("input[name=ingredientQuantity]").first().fill("500");
        page.locator("input[name=ingredientUnit]").first().fill("g");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.content()).contains(RECIPE_NAME);
    }

    @Test
    @Order(10)
    void tripDetailShowsGenerateMealPlanButton() {
        navigateAndWait("/trips/");
        page.locator("a", new com.microsoft.playwright.Page.LocatorOptions().setHasText(TRIP_NAME)).click();
        page.waitForLoadState();

        tripDetailUrl = page.url();

        assertThat(page.content()).contains("Essensplan");
        assertThat(page.locator("form[action$='/mealplan/generate'] button[type=submit]").count()).isPositive();
    }

    @Test
    @Order(11)
    void generateMealPlanRedirectsToOverview() {
        // On trip detail page from previous test
        page.locator("form[action$='/mealplan/generate'] button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        assertThat(page.url()).contains("/mealplan");
        assertThat(page.content()).contains("Essensplan");
    }

    @Test
    @Order(12)
    void mealPlanOverviewResolvesI18nMessages() {
        final String content = page.content();

        assertThat(content).doesNotContain("??");
        assertThat(content).contains("Fruehstueck");
        assertThat(content).contains("Mittagessen");
        assertThat(content).contains("Abendessen");
    }

    @Test
    @Order(13)
    void mealPlanOverviewShowsGridForAllDays() {
        final String content = page.content();

        // Trip is 2026-09-01 to 2026-09-03 → 3 days
        assertThat(content).contains("2026-09-01");
        assertThat(content).contains("2026-09-02");
        assertThat(content).contains("2026-09-03");
    }

    @Test
    @Order(14)
    void mealPlanOverviewShowsStatusDropdowns() {
        final int statusDropdowns = statusSelects().count();

        // 3 days × 3 meals = 9 status dropdowns
        assertThat(statusDropdowns).isEqualTo(9);
    }

    @Test
    @Order(15)
    void allSlotsAreInitiallyPlanned() {
        final var statusSelects = statusSelects();
        final int count = statusSelects.count();

        for (int i = 0; i < count; i++) {
            assertThat(statusSelects.nth(i).inputValue()).isEqualTo("PLANNED");
        }
    }

    @Test
    @Order(16)
    void plannedSlotsShowRecipePicker() {
        final int recipePickers = recipePickers().count();

        // All 9 slots are PLANNED → all should have recipe picker
        assertThat(recipePickers).isEqualTo(9);
    }

    @Test
    @Order(20)
    void markSlotAsSkip() {
        final var firstStatus = statusSelects().first();
        selectAndSubmit(firstStatus, "SKIP");

        final var firstStatusAfter = statusSelects().first();
        assertThat(firstStatusAfter.inputValue()).isEqualTo("SKIP");
    }

    @Test
    @Order(21)
    void skipSlotShowsSkipLabel() {
        assertThat(page.content()).contains("Auslassen");
    }

    @Test
    @Order(22)
    void skipSlotDoesNotShowRecipePicker() {
        // SKIP slot should not have a recipe picker
        // Count recipe pickers — should be 8 (one slot is SKIP)
        final int recipePickers = recipePickers().count();
        assertThat(recipePickers).isEqualTo(8);
    }

    @Test
    @Order(30)
    void markSlotAsEatingOut() {
        final var secondStatus = statusSelects().nth(1);
        selectAndSubmit(secondStatus, "EATING_OUT");

        final var secondStatusAfter = statusSelects().nth(1);
        assertThat(secondStatusAfter.inputValue()).isEqualTo("EATING_OUT");
    }

    @Test
    @Order(31)
    void eatingOutSlotShowsEatingOutLabel() {
        assertThat(page.content()).contains("Auswaerts essen");
    }

    @Test
    @Order(40)
    void revertSlotBackToPlanned() {
        final var firstStatus = statusSelects().first();
        selectAndSubmit(firstStatus, "PLANNED");

        final var firstStatusAfter = statusSelects().first();
        assertThat(firstStatusAfter.inputValue()).isEqualTo("PLANNED");
    }

    @Test
    @Order(50)
    void assignRecipeToSlot() {
        // Find a recipe picker for a PLANNED slot and select the recipe
        final var recipePicker = recipePickers().first();
        final var options = recipePicker.locator("option");
        final int optionCount = options.count();

        // Find the option that contains our recipe name
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
    @Order(60)
    void tripDetailShowsViewMealPlanLinkAfterGeneration() {
        navigateAndWait(tripDetailUrl.replace(BASE_URL, ""));

        assertThat(page.locator("a[href$='/mealplan']").count()).isPositive();
        assertThat(page.content()).contains("Essensplan anzeigen");
        // Generate button should NOT be visible anymore
        assertThat(page.locator("form[action$='/mealplan/generate']").count()).isZero();
    }

    @Test
    @Order(70)
    void navigateBackFromMealPlanToTripDetail() {
        final String tripId = extractTripId();
        navigateAndWait("/trips/" + tripId + "/mealplan");

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

    private com.microsoft.playwright.Locator statusSelects() {
        return page.locator(".mealplan-desktop select[name=status]");
    }

    private com.microsoft.playwright.Locator recipePickers() {
        return page.locator(".mealplan-desktop select[name=recipeId]");
    }

    private String extractTripId() {
        final String url = tripDetailUrl != null ? tripDetailUrl : page.url();
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }
}
