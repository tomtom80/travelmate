package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AdvancePaymentSuggestionTest {

    @Test
    void suggestRoundsUpToNearest50() {
        // 1400 / 3 = 466.67 -> ceil(466.67/50) = 10 -> 10 * 50 = 500
        final BigDecimal result = AdvancePaymentSuggestion.suggest(new BigDecimal("1400"), 3);

        assertThat(result).isEqualByComparingTo("500");
    }

    @Test
    void suggestExactDivision() {
        // 3000 / 3 = 1000 -> ceil(1000/50) = 20 -> 20 * 50 = 1000
        final BigDecimal result = AdvancePaymentSuggestion.suggest(new BigDecimal("3000"), 3);

        assertThat(result).isEqualByComparingTo("1000");
    }

    @Test
    void suggestRoundsUpWhenNotDivisibleBy50() {
        // 2500 / 4 = 625 -> ceil(625/50) = 13 -> 13 * 50 = 650
        final BigDecimal result = AdvancePaymentSuggestion.suggest(new BigDecimal("2500"), 4);

        assertThat(result).isEqualByComparingTo("650");
    }

    @Test
    void suggestSingleParty() {
        // 1800 / 1 = 1800 -> ceil(1800/50) = 36 -> 36 * 50 = 1800
        final BigDecimal result = AdvancePaymentSuggestion.suggest(new BigDecimal("1800"), 1);

        assertThat(result).isEqualByComparingTo("1800");
    }

    @Test
    void suggestSmallAmount() {
        // 100 / 3 = 33.34 -> ceil(33.34/50) = 1 -> 1 * 50 = 50
        final BigDecimal result = AdvancePaymentSuggestion.suggest(new BigDecimal("100"), 3);

        assertThat(result).isEqualByComparingTo("50");
    }

    @Test
    void suggestRejectsZeroCost() {
        assertThatThrownBy(() -> AdvancePaymentSuggestion.suggest(BigDecimal.ZERO, 3))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than 0");
    }

    @Test
    void suggestRejectsZeroPartyCount() {
        assertThatThrownBy(() -> AdvancePaymentSuggestion.suggest(new BigDecimal("1000"), 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than 0");
    }

    @Test
    void suggestRejectsNullCost() {
        assertThatThrownBy(() -> AdvancePaymentSuggestion.suggest(null, 3))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
