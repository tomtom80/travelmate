package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AdvancePaymentTest {

    @Test
    void createsAdvancePayment() {
        final AdvancePaymentId id = new AdvancePaymentId(UUID.randomUUID());
        final UUID partyId = UUID.randomUUID();

        final AdvancePayment payment = new AdvancePayment(
            id, partyId, "Familie Mueller", new BigDecimal("500.00"), false);

        assertThat(payment.advancePaymentId()).isEqualTo(id);
        assertThat(payment.partyTenantId()).isEqualTo(partyId);
        assertThat(payment.partyName()).isEqualTo("Familie Mueller");
        assertThat(payment.amount()).isEqualByComparingTo("500.00");
        assertThat(payment.paid()).isFalse();
    }

    @Test
    void togglePaidFlipsStatus() {
        final AdvancePayment payment = new AdvancePayment(
            new AdvancePaymentId(UUID.randomUUID()), UUID.randomUUID(),
            "Familie Mueller", new BigDecimal("500.00"), false);

        payment.togglePaid();
        assertThat(payment.paid()).isTrue();

        payment.togglePaid();
        assertThat(payment.paid()).isFalse();
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> new AdvancePayment(
            new AdvancePaymentId(UUID.randomUUID()), UUID.randomUUID(),
            "Familie Mueller", BigDecimal.ZERO, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than 0");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> new AdvancePayment(
            new AdvancePaymentId(UUID.randomUUID()), UUID.randomUUID(),
            "Familie Mueller", new BigDecimal("-100"), false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("greater than 0");
    }

    @Test
    void rejectsNullPartyTenantId() {
        assertThatThrownBy(() -> new AdvancePayment(
            new AdvancePaymentId(UUID.randomUUID()), null,
            "Familie Mueller", new BigDecimal("500"), false))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
