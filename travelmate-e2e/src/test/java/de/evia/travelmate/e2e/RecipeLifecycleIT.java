package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecipeLifecycleIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Recipe " + RUN_ID;
    private static final String EMAIL = "recipe-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";
    private static final String RECIPE_NAME = "Spaghetti Bolognese " + RUN_ID;
    private static final String UPDATED_RECIPE_NAME = "Pasta Bolognese " + RUN_ID;

    @Test
    @Order(1)
    void setUpTravelPartyAndNavigateToRecipes() {
        signUpAndLogin(TENANT_NAME, "Chef", "Koch", EMAIL, PASSWORD);
        waitForTripsReady();

        navigateAndWait("/trips/recipes");
        final String content = page.content();
        assertThat(content).doesNotContain("Whitelabel Error Page");
        assertThat(content).doesNotContain("Internal Server Error");
        assertThat(content).doesNotContain("Forbidden");
    }

    @Test
    @Order(2)
    void recipesListShowsEmptyState() {
        navigateAndWait("/trips/recipes");

        assertThat(page.content()).contains("Noch keine Rezepte vorhanden");
    }

    @Test
    @Order(3)
    void recipesPageResolvesI18nMessages() {
        navigateAndWait("/trips/recipes");
        final String content = page.content();

        assertThat(content).doesNotContain("??");
        assertThat(content).contains("Rezepte");
    }

    @Test
    @Order(4)
    void navigationBarContainsRecipesLink() {
        navigateAndWait("/trips/");
        final var recipesLink = page.locator("nav a[href='/trips/recipes']");

        assertThat(recipesLink.count()).isPositive();
        assertThat(recipesLink.textContent()).contains("Rezepte");
    }

    @Test
    @Order(10)
    void createRecipeFormDisplaysAllFields() {
        navigateAndWait("/trips/recipes/new");
        page.waitForLoadState();

        assertThat(page.locator("input[name=name]").count()).isPositive();
        assertThat(page.locator("input[name=servings]").count()).isPositive();
        assertThat(page.locator("input[name=ingredientName]").count()).isPositive();
        assertThat(page.locator("input[name=ingredientQuantity]").count()).isPositive();
        assertThat(page.locator("input[name=ingredientUnit]").count()).isPositive();
        assertThat(page.locator("#add-ingredient-btn").count()).isPositive();
    }

    @Test
    @Order(11)
    void addIngredientRowDynamically() {
        navigateAndWait("/trips/recipes/new");

        final int initialRows = page.locator(".ingredient-row").count();
        page.click("#add-ingredient-btn");
        final int afterRows = page.locator(".ingredient-row").count();

        assertThat(afterRows).isEqualTo(initialRows + 1);
    }

    @Test
    @Order(20)
    void createRecipeWithIngredients() {
        navigateAndWait("/trips/recipes/new");

        page.fill("input[name=name]", RECIPE_NAME);
        page.fill("input[name=servings]", "4");

        // Fill the default first ingredient row
        page.locator("input[name=ingredientName]").first().fill("Spaghetti");
        page.locator("input[name=ingredientQuantity]").first().fill("500");
        page.locator("input[name=ingredientUnit]").first().fill("g");

        // Add a second ingredient
        page.click("#add-ingredient-btn");
        page.locator("input[name=ingredientName]").last().fill("Hackfleisch");
        page.locator("input[name=ingredientQuantity]").last().fill("400");
        page.locator("input[name=ingredientUnit]").last().fill("g");

        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.url()).contains("/trips/recipes");
        assertThat(page.content()).contains(RECIPE_NAME);
    }

    @Test
    @Order(21)
    void recipeAppearsInListWithCorrectDetails() {
        navigateAndWait("/trips/recipes");
        final String content = page.content();

        assertThat(content).contains(RECIPE_NAME);

        final var recipeRow = page.locator("tr", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(RECIPE_NAME));
        assertThat(recipeRow.count()).isPositive();
        assertThat(recipeRow.textContent()).contains("4");
        assertThat(recipeRow.textContent()).contains("2");
    }

    @Test
    @Order(22)
    void recipeListShowsEditAndDeleteButtons() {
        navigateAndWait("/trips/recipes");

        final var recipeRow = page.locator("tr", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(RECIPE_NAME));
        assertThat(recipeRow.locator("a[href*='/edit']").count()).isPositive();
        assertThat(recipeRow.locator("form[action*='/delete'] button[type=submit]").count()).isPositive();
    }

    @Test
    @Order(30)
    void editRecipeFormShowsPrefilledValues() {
        navigateAndWait("/trips/recipes");

        final var editLink = page.locator("tr", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(RECIPE_NAME))
            .locator("a[href*='/edit']");
        editLink.click();
        page.waitForLoadState();

        assertThat(page.locator("input[name=name]").inputValue()).isEqualTo(RECIPE_NAME);
        assertThat(page.locator("input[name=servings]").inputValue()).isEqualTo("4");

        final int ingredientRows = page.locator(".ingredient-row").count();
        assertThat(ingredientRows).isEqualTo(2);
    }

    @Test
    @Order(31)
    void editRecipeUpdatesDetails() {
        // Still on edit form from previous test
        page.fill("input[name=name]", UPDATED_RECIPE_NAME);
        page.fill("input[name=servings]", "6");
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.url()).contains("/trips/recipes");
        assertThat(page.content()).contains(UPDATED_RECIPE_NAME);
        assertThat(page.content()).doesNotContain(RECIPE_NAME);

        final var recipeRow = page.locator("tr", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(UPDATED_RECIPE_NAME));
        assertThat(recipeRow.textContent()).contains("6");
    }

    @Test
    @Order(40)
    void deleteRecipeRemovesFromList() {
        navigateAndWait("/trips/recipes");

        // Accept the browser confirm dialog
        page.onDialog(dialog -> dialog.accept());

        final var deleteButton = page.locator("tr", new com.microsoft.playwright.Page.LocatorOptions()
            .setHasText(UPDATED_RECIPE_NAME))
            .locator("form[action*='/delete'] button[type=submit]");
        deleteButton.click();
        page.waitForLoadState();

        assertThat(page.content()).doesNotContain(UPDATED_RECIPE_NAME);
    }

    @Test
    @Order(41)
    void recipeListIsEmptyAfterDeletion() {
        navigateAndWait("/trips/recipes");

        assertThat(page.content()).contains("Noch keine Rezepte vorhanden");
    }

    @Test
    @Order(50)
    void navigateToRecipesFromTripsPage() {
        navigateAndWait("/trips/");

        page.locator("nav a[href='/trips/recipes']").click();
        page.waitForLoadState();

        assertThat(page.url()).contains("/trips/recipes");
        assertThat(page.content()).contains("Rezepte");
    }

    @Test
    @Order(51)
    void navigateBackFromRecipesToTrips() {
        navigateAndWait("/trips/recipes");

        page.locator("main a[href='/trips/']").click();
        page.waitForURL(url -> url.endsWith("/trips/"));

        assertThat(page.url()).endsWith("/trips/");
    }
}
