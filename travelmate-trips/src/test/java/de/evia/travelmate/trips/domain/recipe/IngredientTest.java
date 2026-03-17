package de.evia.travelmate.trips.domain.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class IngredientTest {

    @Test
    void acceptsValidIngredient() {
        final Ingredient ingredient = new Ingredient("Mehl", new BigDecimal("500"), "g");

        assertThat(ingredient.name()).isEqualTo("Mehl");
        assertThat(ingredient.quantity()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(ingredient.unit()).isEqualTo("g");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Ingredient("", new BigDecimal("1"), "g"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroQuantity() {
        assertThatThrownBy(() -> new Ingredient("Mehl", BigDecimal.ZERO, "g"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than zero");
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> new Ingredient("Mehl", new BigDecimal("-1"), "g"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankUnit() {
        assertThatThrownBy(() -> new Ingredient("Mehl", new BigDecimal("500"), ""))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
