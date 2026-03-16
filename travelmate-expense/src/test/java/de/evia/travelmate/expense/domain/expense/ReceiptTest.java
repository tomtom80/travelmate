package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReceiptTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void createsValidReceipt() {
        final ReceiptId receiptId = new ReceiptId(UUID.randomUUID());
        final LocalDate date = LocalDate.of(2026, 7, 5);

        final Receipt receipt = new Receipt(
            receiptId, "Supermarket", new Amount(new BigDecimal("47.30")),
            ALICE, ALICE, date, ExpenseCategory.GROCERIES,
            ReviewStatus.SUBMITTED, null, null
        );

        assertThat(receipt.receiptId()).isEqualTo(receiptId);
        assertThat(receipt.description()).isEqualTo("Supermarket");
        assertThat(receipt.amount().value()).isEqualByComparingTo("47.30");
        assertThat(receipt.paidBy()).isEqualTo(ALICE);
        assertThat(receipt.submittedBy()).isEqualTo(ALICE);
        assertThat(receipt.date()).isEqualTo(date);
        assertThat(receipt.category()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(receipt.reviewStatus()).isEqualTo(ReviewStatus.SUBMITTED);
        assertThat(receipt.reviewerId()).isNull();
        assertThat(receipt.rejectionReason()).isNull();
    }

    @Test
    void createsReceiptWithDefaultCategory() {
        final Receipt receipt = new Receipt(
            new ReceiptId(UUID.randomUUID()), "Something", new Amount(BigDecimal.TEN),
            ALICE, ALICE, LocalDate.now(), null,
            ReviewStatus.APPROVED, null, null
        );

        assertThat(receipt.category()).isEqualTo(ExpenseCategory.OTHER);
    }

    @Test
    void submittedByCanDifferFromPaidBy() {
        final Receipt receipt = new Receipt(
            new ReceiptId(UUID.randomUUID()), "Dinner", new Amount(BigDecimal.TEN),
            ALICE, BOB, LocalDate.now(), ExpenseCategory.RESTAURANT,
            ReviewStatus.SUBMITTED, null, null
        );

        assertThat(receipt.paidBy()).isEqualTo(ALICE);
        assertThat(receipt.submittedBy()).isEqualTo(BOB);
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "  ", new Amount(BigDecimal.TEN),
            ALICE, ALICE, LocalDate.now(), ExpenseCategory.OTHER,
            ReviewStatus.SUBMITTED, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "Food", null,
            ALICE, ALICE, LocalDate.now(), ExpenseCategory.OTHER,
            ReviewStatus.SUBMITTED, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPaidBy() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "Food", new Amount(BigDecimal.TEN),
            null, ALICE, LocalDate.now(), ExpenseCategory.OTHER,
            ReviewStatus.SUBMITTED, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullSubmittedBy() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "Food", new Amount(BigDecimal.TEN),
            ALICE, null, LocalDate.now(), ExpenseCategory.OTHER,
            ReviewStatus.SUBMITTED, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullDate() {
        assertThatThrownBy(() -> new Receipt(
            new ReceiptId(UUID.randomUUID()), "Food", new Amount(BigDecimal.TEN),
            ALICE, ALICE, null, ExpenseCategory.OTHER,
            ReviewStatus.SUBMITTED, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    // --- Review: Approve ---

    @Test
    void approveChangesStatusToApproved() {
        final Receipt receipt = createSubmittedReceipt(ALICE);

        receipt.approve(BOB);

        assertThat(receipt.reviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(receipt.reviewerId()).isEqualTo(BOB);
        assertThat(receipt.rejectionReason()).isNull();
    }

    @Test
    void approveRejectsIfReviewerIsSubmitter() {
        final Receipt receipt = createSubmittedReceipt(ALICE);

        assertThatThrownBy(() -> receipt.approve(ALICE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("four-eyes");
    }

    @Test
    void approveRejectsIfNotSubmitted() {
        final Receipt receipt = createSubmittedReceipt(ALICE);
        receipt.approve(BOB);

        assertThatThrownBy(() -> receipt.approve(BOB))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SUBMITTED");
    }

    // --- Review: Reject ---

    @Test
    void rejectChangesStatusToRejected() {
        final Receipt receipt = createSubmittedReceipt(ALICE);

        receipt.reject(BOB, "Amount seems wrong");

        assertThat(receipt.reviewStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(receipt.reviewerId()).isEqualTo(BOB);
        assertThat(receipt.rejectionReason()).isEqualTo("Amount seems wrong");
    }

    @Test
    void rejectRejectsIfReviewerIsSubmitter() {
        final Receipt receipt = createSubmittedReceipt(ALICE);

        assertThatThrownBy(() -> receipt.reject(ALICE, "Bad receipt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("four-eyes");
    }

    @Test
    void rejectRejectsBlankReason() {
        final Receipt receipt = createSubmittedReceipt(ALICE);

        assertThatThrownBy(() -> receipt.reject(BOB, "  "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectRejectsIfNotSubmitted() {
        final Receipt receipt = createSubmittedReceipt(ALICE);
        receipt.approve(BOB);

        assertThatThrownBy(() -> receipt.reject(BOB, "Too late"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SUBMITTED");
    }

    // --- Review: Resubmit ---

    @Test
    void resubmitResetsToSubmitted() {
        final Receipt receipt = createSubmittedReceipt(ALICE);
        receipt.reject(BOB, "Wrong amount");

        receipt.resubmit("Corrected", new Amount(new BigDecimal("25.00")),
            LocalDate.of(2026, 7, 10), ExpenseCategory.GROCERIES);

        assertThat(receipt.reviewStatus()).isEqualTo(ReviewStatus.SUBMITTED);
        assertThat(receipt.description()).isEqualTo("Corrected");
        assertThat(receipt.amount().value()).isEqualByComparingTo("25.00");
        assertThat(receipt.date()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(receipt.category()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(receipt.reviewerId()).isNull();
        assertThat(receipt.rejectionReason()).isNull();
    }

    @Test
    void resubmitRejectsIfNotRejected() {
        final Receipt receipt = createSubmittedReceipt(ALICE);

        assertThatThrownBy(() -> receipt.resubmit("Update", new Amount(BigDecimal.TEN),
            LocalDate.now(), ExpenseCategory.OTHER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("REJECTED");
    }

    @Test
    void approveAllowsReviewerEqualToPaidBy() {
        final Receipt receipt = new Receipt(
            new ReceiptId(UUID.randomUUID()), "Dinner", new Amount(BigDecimal.TEN),
            ALICE, BOB, LocalDate.now(), ExpenseCategory.RESTAURANT,
            ReviewStatus.SUBMITTED, null, null
        );

        receipt.approve(ALICE);

        assertThat(receipt.reviewStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    // --- Helpers ---

    private Receipt createSubmittedReceipt(final UUID submittedBy) {
        return new Receipt(
            new ReceiptId(UUID.randomUUID()), "Groceries", new Amount(BigDecimal.TEN),
            submittedBy, submittedBy, LocalDate.now(), ExpenseCategory.GROCERIES,
            ReviewStatus.SUBMITTED, null, null
        );
    }
}
