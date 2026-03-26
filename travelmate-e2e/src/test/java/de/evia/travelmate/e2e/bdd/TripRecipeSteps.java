package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import com.microsoft.playwright.options.LoadState;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TripRecipeSteps {

    @When("I navigate to the trip detail page")
    public void iNavigateToTheTripDetailPage() {
        final String url = TripPlanningSteps.getCurrentTripDetailUrl();
        if (url != null) {
            navigateAndWait(url.replace(BASE_URL, ""));
        }
    }

    @Then("I see a {string} feature card")
    public void iSeeAFeatureCard(final String cardTitle) {
        assertThat(page.locator(".feature-card h3:has-text('" + cardTitle + "')").count()).isPositive();
    }

    @Given("I am on the trip recipes page")
    public void iAmOnTheTripRecipesPage() {
        final String url = TripPlanningSteps.getCurrentTripDetailUrl();
        if (url != null) {
            final String tripPath = url.replace(BASE_URL, "");
            navigateAndWait(tripPath.replaceAll("/$", "") + "/recipes");
        }
    }

    @When("I click {string}")
    public void iClick(final String text) {
        page.locator("a:has-text('" + text + "'), button:has-text('" + text + "')").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @When("I enter recipe name {string} with servings {string}")
    public void iEnterRecipeNameWithServings(final String name, final String servings) {
        page.fill("input[name=name]", name);
        page.fill("input[name=servings]", servings);
    }

    @When("I add ingredient {string} quantity {string} unit {string}")
    public void iAddIngredient(final String name, final String quantity, final String unit) {
        page.locator(".ingredient-card input[name=ingredientName]").last().fill(name);
        page.locator(".ingredient-card input[name=ingredientQuantity]").last().fill(quantity);
        page.locator(".ingredient-card input[name=ingredientUnit]").last().fill(unit);
    }

    @When("I submit the recipe form")
    public void iSubmitTheRecipeForm() {
        page.locator("main form button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @Then("{string} appears in the trip recipe list")
    public void appearsInTheTripRecipeList(final String recipeName) {
        assertThat(page.content()).contains(recipeName);
    }

    @Given("I have a personal recipe {string}")
    public void iHaveAPersonalRecipe(final String name) {
        navigateAndWait("/trips/recipes/new");
        page.fill("input[name=name]", name);
        page.fill("input[name=servings]", "4");
        page.locator(".ingredient-card input[name=ingredientName]").first().fill("Kartoffeln");
        page.locator(".ingredient-card input[name=ingredientQuantity]").first().fill("1");
        page.locator(".ingredient-card input[name=ingredientUnit]").first().fill("kg");
        page.locator("main form button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    @When("I expand the share section")
    public void iExpandTheShareSection() {
        page.locator("main details summary").click();
        page.waitForTimeout(300);
    }

    @When("I click share for {string}")
    public void iClickShareFor(final String recipeName) {
        page.locator("details tr:has-text('" + recipeName + "') button[type=submit]").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
}
