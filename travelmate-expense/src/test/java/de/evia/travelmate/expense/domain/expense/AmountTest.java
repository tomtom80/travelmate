package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AmountTest {

    @Test
    void createsValidAmount() {
        final Amount amount = new Amount(new BigDecimal("42.50"));

        assertThat(amount.value()).isEqualByComparingTo("42.50");
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> new Amount(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> new Amount(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> new Amount(new BigDecimal("-5.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }
}
