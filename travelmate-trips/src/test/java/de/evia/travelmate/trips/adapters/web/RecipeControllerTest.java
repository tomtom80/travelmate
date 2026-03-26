package de.evia.travelmate.trips.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.RecipeService;
import de.evia.travelmate.trips.application.command.CreateRecipeCommand;
import de.evia.travelmate.trips.application.command.DeleteRecipeCommand;
import de.evia.travelmate.trips.application.command.UpdateRecipeCommand;
import de.evia.travelmate.trips.application.representation.IngredientRepresentation;
import de.evia.travelmate.trips.application.representation.RecipeRepresentation;
import de.evia.travelmate.trips.domain.recipe.RecipeId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecipeControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID MEMBER_UUID = UUID.randomUUID();
    private static final UUID RECIPE_UUID = UUID.randomUUID();
    private static final String MEMBER_EMAIL = "chef@test.de";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private TravelPartyRepository travelPartyRepository;

    @BeforeEach
    void setUpParty() {
        final TravelParty party = TravelParty.create(new TenantId(TENANT_UUID), "Test");
        party.addMember(MEMBER_UUID, MEMBER_EMAIL, "Max", "Koch");
        when(travelPartyRepository.findByMemberEmail(MEMBER_EMAIL)).thenReturn(Optional.of(party));
    }

    @Test
    void listShowsRecipes() throws Exception {
        when(recipeService.findAllPersonalByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(List.of());

        mockMvc.perform(get("/recipes")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "recipe/list"))
            .andExpect(model().attributeExists("recipes"));
    }

    @Test
    void newFormShowsEmptyForm() throws Exception {
        mockMvc.perform(get("/recipes/new")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "recipe/form"));
    }

    @Test
    void createRecipeRedirectsToList() throws Exception {
        when(recipeService.createPersonalRecipe(any(CreateRecipeCommand.class)))
            .thenReturn(new RecipeRepresentation(RECIPE_UUID, TENANT_UUID, null, null, "Pasta", 4, List.of()));

        mockMvc.perform(post("/recipes")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Pasta")
                .param("servings", "4")
                .param("ingredientName", "Nudeln")
                .param("ingredientQuantity", "500")
                .param("ingredientUnit", "g"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/recipes"));

        verify(recipeService).createPersonalRecipe(any(CreateRecipeCommand.class));
    }

    @Test
    void editFormShowsExistingRecipe() throws Exception {
        final RecipeRepresentation recipe = new RecipeRepresentation(
            RECIPE_UUID, TENANT_UUID, null, null, "Pasta", 4,
            List.of(new IngredientRepresentation("Nudeln", new BigDecimal("500"), "g"))
        );
        when(recipeService.findById(new RecipeId(RECIPE_UUID))).thenReturn(recipe);

        mockMvc.perform(get("/recipes/" + RECIPE_UUID + "/edit")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "recipe/form"))
            .andExpect(model().attributeExists("recipe"));
    }

    @Test
    void updateRecipeRedirectsToList() throws Exception {
        final RecipeRepresentation recipe = new RecipeRepresentation(
            RECIPE_UUID, TENANT_UUID, null, null, "Pasta", 4, List.of());
        when(recipeService.findById(new RecipeId(RECIPE_UUID))).thenReturn(recipe);
        when(recipeService.updateRecipe(any(UpdateRecipeCommand.class))).thenReturn(recipe);

        mockMvc.perform(post("/recipes/" + RECIPE_UUID + "/edit")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL)))
                .param("name", "Updated Pasta")
                .param("servings", "6")
                .param("ingredientName", "Penne")
                .param("ingredientQuantity", "400")
                .param("ingredientUnit", "g"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/recipes"));

        verify(recipeService).updateRecipe(any(UpdateRecipeCommand.class));
    }

    @Test
    void deleteRecipeRedirectsToList() throws Exception {
        final RecipeRepresentation recipe = new RecipeRepresentation(
            RECIPE_UUID, TENANT_UUID, null, null, "Delete Me", 1, List.of());
        when(recipeService.findById(new RecipeId(RECIPE_UUID))).thenReturn(recipe);

        mockMvc.perform(post("/recipes/" + RECIPE_UUID + "/delete")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/recipes"));

        verify(recipeService).deleteRecipe(any(DeleteRecipeCommand.class));
    }

    @Test
    void editFormRejectsCrossTenantAccess() throws Exception {
        final UUID otherTenantUuid = UUID.randomUUID();
        final RecipeRepresentation recipe = new RecipeRepresentation(
            RECIPE_UUID, otherTenantUuid, null, null, "Pasta", 4, List.of());
        when(recipeService.findById(new RecipeId(RECIPE_UUID))).thenReturn(recipe);

        mockMvc.perform(get("/recipes/" + RECIPE_UUID + "/edit")
                .with(jwt().jwt(j -> j.claim("email", MEMBER_EMAIL))))
            .andExpect(status().isForbidden());
    }
}
