package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReceiptTest {

    @Test
    void createsValidReceipt() {
        final ReceiptId receiptId = new ReceiptId(UUID.randomUUID());
        final UUID paidBy = UUID.randomUUID();
        final LocalDate date = LocalDate.of(2026, 7, 5);

        final Receipt receipt = new Receipt(
            receiptId, "Supermarket", new Amount(new BigDecimal("47.30")), paidBy, date
        );

        assertThat(receipt.receiptId()).isEqualTo(receiptId);
        assertThat(receipt.description()).isEqualTo("Supermarket");
        assertThat(receipt.amount().value()).isEqualByComparingTo("47.30");
        assertThat(receipt.paidBy()).isEqualTo(paidBy);
        assertThat(receipt.date()).isEqualTo(date);
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "  ", new Amount(BigDecimal.TEN), UUID.randomUUID(), LocalDate.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "Food", null, UUID.randomUUID(), LocalDate.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPaidBy() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "Food", new Amount(BigDecimal.TEN), null, LocalDate.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullDate() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "Food", new Amount(BigDecimal.TEN), UUID.randomUUID(), null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
