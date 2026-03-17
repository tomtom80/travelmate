package de.evia.travelmate.trips.domain.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RecipeNameTest {

    @Test
    void acceptsValidName() {
        final RecipeName name = new RecipeName("Spaghetti Bolognese");
        assertThat(name.value()).isEqualTo("Spaghetti Bolognese");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new RecipeName("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new RecipeName(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNameExceedingMaxLength() {
        final String longName = "A".repeat(201);
        assertThatThrownBy(() -> new RecipeName(longName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("200");
    }
}
