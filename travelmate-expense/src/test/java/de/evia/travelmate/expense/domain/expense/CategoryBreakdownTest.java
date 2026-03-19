package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CategoryBreakdownTest {

    @Test
    void emptyReceiptsProduceEmptyBreakdown() {
        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(List.of());

        assertThat(breakdown.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(breakdown.categories()).isEmpty();
    }

    @Test
    void singleCategoryReceiptsProduceSingleShare() {
        final List<Receipt> receipts = List.of(
            approvedReceipt("50.00", ExpenseCategory.GROCERIES),
            approvedReceipt("30.00", ExpenseCategory.GROCERIES)
        );

        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(receipts);

        assertThat(breakdown.totalAmount()).isEqualByComparingTo("80.00");
        assertThat(breakdown.categories()).hasSize(1);
        assertThat(breakdown.categories().getFirst().category()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(breakdown.categories().getFirst().amount()).isEqualByComparingTo("80.00");
        assertThat(breakdown.categories().getFirst().percentage()).isEqualByComparingTo("100.0");
        assertThat(breakdown.categories().getFirst().receiptCount()).isEqualTo(2);
    }

    @Test
    void multipleCategoriesSortedByAmountDescending() {
        final List<Receipt> receipts = List.of(
            approvedReceipt("100.00", ExpenseCategory.GROCERIES),
            approvedReceipt("200.00", ExpenseCategory.RESTAURANT),
            approvedReceipt("50.00", ExpenseCategory.TRANSPORT)
        );

        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(receipts);

        assertThat(breakdown.totalAmount()).isEqualByComparingTo("350.00");
        assertThat(breakdown.categories()).hasSize(3);
        assertThat(breakdown.categories().get(0).category()).isEqualTo(ExpenseCategory.RESTAURANT);
        assertThat(breakdown.categories().get(0).amount()).isEqualByComparingTo("200.00");
        assertThat(breakdown.categories().get(1).category()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(breakdown.categories().get(2).category()).isEqualTo(ExpenseCategory.TRANSPORT);
    }

    @Test
    void onlyApprovedReceiptsAreIncluded() {
        final Receipt approved = approvedReceipt("100.00", ExpenseCategory.GROCERIES);
        final Receipt submitted = new Receipt(
            new ReceiptId(UUID.randomUUID()), "Submitted", new Amount(new BigDecimal("50.00")),
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(),
            ExpenseCategory.GROCERIES, ReviewStatus.SUBMITTED, null, null
        );
        final Receipt rejected = new Receipt(
            new ReceiptId(UUID.randomUUID()), "Rejected", new Amount(new BigDecimal("30.00")),
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(),
            ExpenseCategory.RESTAURANT, ReviewStatus.REJECTED, UUID.randomUUID(), "Wrong"
        );

        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(
            List.of(approved, submitted, rejected));

        assertThat(breakdown.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(breakdown.categories()).hasSize(1);
        assertThat(breakdown.categories().getFirst().category()).isEqualTo(ExpenseCategory.GROCERIES);
    }

    @Test
    void percentagesAddUpCorrectly() {
        final List<Receipt> receipts = List.of(
            approvedReceipt("60.00", ExpenseCategory.GROCERIES),
            approvedReceipt("40.00", ExpenseCategory.RESTAURANT)
        );

        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(receipts);

        assertThat(breakdown.categories().get(0).percentage()).isEqualByComparingTo("60.0");
        assertThat(breakdown.categories().get(1).percentage()).isEqualByComparingTo("40.0");
    }

    @Test
    void accommodationTotalFromProjectionIsIncluded() {
        final List<Receipt> receipts = List.of(
            approvedReceipt("100.00", ExpenseCategory.GROCERIES),
            approvedReceipt("50.00", ExpenseCategory.ACCOMMODATION)
        );

        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(
            receipts, new BigDecimal("300.00"));

        assertThat(breakdown.totalAmount()).isEqualByComparingTo("450.00");

        final CategoryShare accShare = breakdown.categories().stream()
            .filter(c -> c.category() == ExpenseCategory.ACCOMMODATION)
            .findFirst().orElseThrow();
        assertThat(accShare.amount()).isEqualByComparingTo("350.00");
    }

    @Test
    void accommodationTotalAloneWithNoAccommodationReceipts() {
        final List<Receipt> receipts = List.of(
            approvedReceipt("100.00", ExpenseCategory.GROCERIES)
        );

        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(
            receipts, new BigDecimal("500.00"));

        assertThat(breakdown.totalAmount()).isEqualByComparingTo("600.00");
        assertThat(breakdown.categories()).hasSize(2);

        final CategoryShare accShare = breakdown.categories().stream()
            .filter(c -> c.category() == ExpenseCategory.ACCOMMODATION)
            .findFirst().orElseThrow();
        assertThat(accShare.amount()).isEqualByComparingTo("500.00");
        assertThat(accShare.receiptCount()).isEqualTo(0);
    }

    @Test
    void nullAccommodationTotalIsIgnored() {
        final List<Receipt> receipts = List.of(
            approvedReceipt("100.00", ExpenseCategory.GROCERIES)
        );

        final CategoryBreakdown breakdown = CategoryBreakdown.fromReceipts(receipts, null);

        assertThat(breakdown.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(breakdown.categories()).hasSize(1);
    }

    private Receipt approvedReceipt(final String amount, final ExpenseCategory category) {
        return new Receipt(
            new ReceiptId(UUID.randomUUID()),
            "Test receipt",
            new Amount(new BigDecimal(amount)),
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDate.now(),
            category,
            ReviewStatus.APPROVED,
            null,
            null
        );
    }
}
