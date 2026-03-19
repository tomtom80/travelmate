package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CategoryGuesserTest {

    @ParameterizedTest
    @CsvSource({
        "EDEKA Markt Mueller, GROCERIES",
        "REWE City, GROCERIES",
        "Aldi Nord, GROCERIES",
        "Lidl, GROCERIES",
        "Penny Markt, GROCERIES",
        "Baeckerei Schmidt, GROCERIES",
        "Restaurant Alpenhof, RESTAURANT",
        "Pizzeria Da Vinci, RESTAURANT",
        "Cafe Central, RESTAURANT",
        "Shell Tankstelle, FUEL",
        "Aral Center, FUEL",
        "OMV Station, FUEL",
        "Apotheke am Markt, HEALTH",
        "DM Drogerie, HEALTH",
        "Bergbahn Zugspitze, ACTIVITY",
        "Therme Erding, ACTIVITY",
        "Museum der Moderne, ACTIVITY",
        "DB Fernverkehr, TRANSPORT",
        "Taxi Zentrale, TRANSPORT",
        "Parkhaus Innenstadt, TRANSPORT"
    })
    void guessesCorrectCategory(final String storeName, final ExpenseCategory expected) {
        assertThat(CategoryGuesser.guess(storeName)).isEqualTo(expected);
    }

    @Test
    void returnsOtherForUnknownStore() {
        assertThat(CategoryGuesser.guess("Random Shop")).isEqualTo(ExpenseCategory.OTHER);
    }

    @Test
    void returnsOtherForNull() {
        assertThat(CategoryGuesser.guess(null)).isEqualTo(ExpenseCategory.OTHER);
    }

    @Test
    void returnsOtherForBlank() {
        assertThat(CategoryGuesser.guess("  ")).isEqualTo(ExpenseCategory.OTHER);
    }

    @Test
    void isCaseInsensitive() {
        assertThat(CategoryGuesser.guess("EDEKA CENTER")).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(CategoryGuesser.guess("restaurant alpenblick")).isEqualTo(ExpenseCategory.RESTAURANT);
    }
}
