package de.evia.travelmate.expense.domain.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.expense.ExpenseCreated;
import de.evia.travelmate.common.events.expense.ExpenseSettled;

import static de.evia.travelmate.expense.domain.expense.ExpenseCategory.GROCERIES;
import static de.evia.travelmate.expense.domain.expense.ExpenseCategory.RESTAURANT;
import static de.evia.travelmate.expense.domain.expense.ExpenseCategory.TRANSPORT;

class ExpenseTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CHILD = UUID.randomUUID();

    // --- Existing: Creation ---

    @Test
    void createRegistersExpenseCreatedEvent() {
        final Expense expense = createExpenseForTwoAdults();

        assertThat(expense.status()).isEqualTo(ExpenseStatus.OPEN);
        assertThat(expense.expenseId()).isNotNull();
        assertThat(expense.tenantId()).isEqualTo(TENANT_ID);
        assertThat(expense.tripId()).isEqualTo(TRIP_ID);
        assertThat(expense.domainEvents()).hasSize(1);
        assertThat(expense.domainEvents().getFirst()).isInstanceOf(ExpenseCreated.class);
    }

    @Test
    void createRequiresAtLeastOneWeighting() {
        assertThatThrownBy(() -> Expense.create(TENANT_ID, TRIP_ID, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("weighting");
    }

    @Test
    void createDefaultsToNoReviewRequired() {
        final Expense expense = createExpenseForTwoAdults();

        assertThat(expense.reviewRequired()).isFalse();
    }

    @Test
    void createWithReviewRequired() {
        final Expense expense = createReviewExpense();

        assertThat(expense.reviewRequired()).isTrue();
    }

    // --- Solo-Org Mode: addReceipt auto-approves ---

    @Test
    void addReceiptToOpenExpense() {
        final Expense expense = createExpenseForTwoAdults();

        expense.addReceipt("Groceries", new Amount(new BigDecimal("60.00")), ALICE, ALICE, LocalDate.now(), GROCERIES);

        assertThat(expense.receipts()).hasSize(1);
        assertThat(expense.receipts().getFirst().description()).isEqualTo("Groceries");
        assertThat(expense.receipts().getFirst().paidBy()).isEqualTo(ALICE);
        assertThat(expense.receipts().getFirst().submittedBy()).isEqualTo(ALICE);
    }

    @Test
    void addReceiptAutoApprovesWhenNoReviewRequired() {
        final Expense expense = createExpenseForTwoAdults();

        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);

        assertThat(expense.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void addReceiptRejectsUnknownPayer() {
        final Expense expense = createExpenseForTwoAdults();
        final UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> expense.addReceipt("Food", new Amount(BigDecimal.TEN), stranger, ALICE, LocalDate.now(), GROCERIES))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a participant");
    }

    // --- Multi-Org Mode: addReceipt sets SUBMITTED ---

    @Test
    void addReceiptSetsSubmittedWhenReviewRequired() {
        final Expense expense = createReviewExpense();

        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);

        assertThat(expense.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.SUBMITTED);
    }

    @Test
    void addReceiptAllowsSubmittedByDifferentFromPaidBy() {
        final Expense expense = createReviewExpense();

        expense.addReceipt("Dinner", new Amount(BigDecimal.TEN), ALICE, BOB, LocalDate.now(), RESTAURANT);

        assertThat(expense.receipts().getFirst().paidBy()).isEqualTo(ALICE);
        assertThat(expense.receipts().getFirst().submittedBy()).isEqualTo(BOB);
    }

    // --- Review: Approve ---

    @Test
    void approveReceiptChangesStatus() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        final ReceiptId receiptId = expense.receipts().getFirst().receiptId();

        expense.approveReceipt(receiptId, BOB);

        assertThat(expense.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    void approveReceiptEnforcesFourEyes() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        final ReceiptId receiptId = expense.receipts().getFirst().receiptId();

        assertThatThrownBy(() -> expense.approveReceipt(receiptId, ALICE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("four-eyes");
    }

    // --- Review: Reject ---

    @Test
    void rejectReceiptChangesStatus() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        final ReceiptId receiptId = expense.receipts().getFirst().receiptId();

        expense.rejectReceipt(receiptId, BOB, "Wrong amount");

        assertThat(expense.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(expense.receipts().getFirst().rejectionReason()).isEqualTo("Wrong amount");
    }

    // --- Review: Resubmit ---

    @Test
    void resubmitReceiptResetsToSubmitted() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        final ReceiptId receiptId = expense.receipts().getFirst().receiptId();
        expense.rejectReceipt(receiptId, BOB, "Wrong");

        expense.resubmitReceipt(receiptId, "Corrected", new Amount(new BigDecimal("15.00")),
            LocalDate.now(), GROCERIES);

        assertThat(expense.receipts().getFirst().reviewStatus()).isEqualTo(ReviewStatus.SUBMITTED);
        assertThat(expense.receipts().getFirst().description()).isEqualTo("Corrected");
    }

    // --- Remove Receipt ---

    @Test
    void removeReceiptFromOpenExpense() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        final ReceiptId receiptId = expense.receipts().getFirst().receiptId();

        expense.removeReceipt(receiptId);

        assertThat(expense.receipts()).isEmpty();
    }

    @Test
    void removeNonexistentReceiptThrows() {
        final Expense expense = createExpenseForTwoAdults();

        assertThatThrownBy(() -> expense.removeReceipt(new ReceiptId(UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    // --- READY_FOR_SETTLEMENT auto-transition ---

    @Test
    void autoTransitionsToReadyForSettlementWhenAllApproved() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.addReceipt("Dinner", new Amount(BigDecimal.TEN), BOB, BOB, LocalDate.now(), RESTAURANT);
        final ReceiptId r1 = expense.receipts().get(0).receiptId();
        final ReceiptId r2 = expense.receipts().get(1).receiptId();

        expense.approveReceipt(r1, BOB);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.OPEN);

        expense.approveReceipt(r2, ALICE);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.READY_FOR_SETTLEMENT);
    }

    @Test
    void readyForSettlementRevertsToOpenOnNewReceipt() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.approveReceipt(expense.receipts().getFirst().receiptId(), BOB);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.READY_FOR_SETTLEMENT);

        expense.addReceipt("New item", new Amount(BigDecimal.TEN), BOB, BOB, LocalDate.now(), RESTAURANT);

        assertThat(expense.status()).isEqualTo(ExpenseStatus.OPEN);
    }

    @Test
    void rejectingReceiptKeepsStatusOpen() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.addReceipt("Dinner", new Amount(BigDecimal.TEN), BOB, BOB, LocalDate.now(), RESTAURANT);
        final ReceiptId r1 = expense.receipts().get(0).receiptId();
        final ReceiptId r2 = expense.receipts().get(1).receiptId();

        expense.approveReceipt(r1, BOB);
        expense.rejectReceipt(r2, ALICE, "Wrong amount");

        assertThat(expense.status()).isEqualTo(ExpenseStatus.OPEN);
    }

    @Test
    void readyForSettlementRevertsToOpenOnResubmit() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.addReceipt("Dinner", new Amount(BigDecimal.TEN), BOB, BOB, LocalDate.now(), RESTAURANT);
        final ReceiptId r1 = expense.receipts().get(0).receiptId();
        final ReceiptId r2 = expense.receipts().get(1).receiptId();

        expense.approveReceipt(r1, BOB);
        expense.rejectReceipt(r2, ALICE, "Wrong");
        assertThat(expense.status()).isEqualTo(ExpenseStatus.OPEN);

        expense.resubmitReceipt(r2, "Fixed dinner", new Amount(BigDecimal.TEN), LocalDate.now(), RESTAURANT);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.OPEN);

        expense.approveReceipt(r2, ALICE);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.READY_FOR_SETTLEMENT);
    }

    @Test
    void soloOrgAutoTransitionsToReadyForSettlementOnAdd() {
        final Expense expense = createExpenseForTwoAdults();

        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);

        assertThat(expense.status()).isEqualTo(ExpenseStatus.READY_FOR_SETTLEMENT);
    }

    @Test
    void soloOrgCanSettleDirectly() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.READY_FOR_SETTLEMENT);

        expense.settle();

        assertThat(expense.status()).isEqualTo(ExpenseStatus.SETTLED);
    }

    // --- Settle ---

    @Test
    void settleRegistersExpenseSettledEvent() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("100.00")), ALICE, ALICE, LocalDate.now(), RESTAURANT);
        expense.clearDomainEvents();

        expense.settle();

        assertThat(expense.status()).isEqualTo(ExpenseStatus.SETTLED);
        assertThat(expense.domainEvents()).hasSize(1);
        assertThat(expense.domainEvents().getFirst()).isInstanceOf(ExpenseSettled.class);
    }

    @Test
    void settleFromReadyForSettlement() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.approveReceipt(expense.receipts().getFirst().receiptId(), BOB);
        assertThat(expense.status()).isEqualTo(ExpenseStatus.READY_FOR_SETTLEMENT);
        expense.clearDomainEvents();

        expense.settle();

        assertThat(expense.status()).isEqualTo(ExpenseStatus.SETTLED);
        assertThat(expense.domainEvents()).hasSize(1);
        assertThat(expense.domainEvents().getFirst()).isInstanceOf(ExpenseSettled.class);
    }

    @Test
    void settleWithoutReceiptsThrows() {
        final Expense expense = createExpenseForTwoAdults();

        assertThatThrownBy(expense::settle)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no receipts");
    }

    @Test
    void settleWithUnapprovedReceiptsThrows() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);

        assertThatThrownBy(expense::settle)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("approved");
    }

    @Test
    void cannotAddReceiptToSettledExpense() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.settle();

        assertThatThrownBy(() -> expense.addReceipt("More food", new Amount(BigDecimal.TEN), BOB, BOB, LocalDate.now(), GROCERIES))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SETTLED");
    }

    @Test
    void cannotSettleTwice() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.settle();

        assertThatThrownBy(expense::settle)
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotApproveOnSettledExpense() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        final ReceiptId rid = expense.receipts().getFirst().receiptId();
        expense.settle();

        assertThatThrownBy(() -> expense.approveReceipt(rid, BOB))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SETTLED");
    }

    // --- Balance calculations (existing, updated signatures) ---

    @Test
    void calculateBalancesEqualSplit() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("100.00")), ALICE, ALICE, LocalDate.now(), RESTAURANT);

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        assertThat(balances.get(ALICE)).isEqualByComparingTo("50.00");
        assertThat(balances.get(BOB)).isEqualByComparingTo("-50.00");
    }

    @Test
    void calculateBalancesMultipleReceipts() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("100.00")), ALICE, ALICE, LocalDate.now(), RESTAURANT);
        expense.addReceipt("Taxi", new Amount(new BigDecimal("40.00")), BOB, BOB, LocalDate.now(), TRANSPORT);

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        assertThat(balances.get(ALICE)).isEqualByComparingTo("30.00");
        assertThat(balances.get(BOB)).isEqualByComparingTo("-30.00");
    }

    @Test
    void calculateBalancesWithChildWeighting() {
        final Expense expense = Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, BigDecimal.ONE),
            new ParticipantWeighting(CHILD, BigDecimal.ZERO)
        ));
        expense.addReceipt("Groceries", new Amount(new BigDecimal("100.00")), ALICE, ALICE, LocalDate.now(), GROCERIES);

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        assertThat(balances.get(ALICE)).isEqualByComparingTo("50.00");
        assertThat(balances.get(BOB)).isEqualByComparingTo("-50.00");
        assertThat(balances.get(CHILD)).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateBalancesWithCustomWeightings() {
        final Expense expense = Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, new BigDecimal("0.5"))
        ));
        expense.addReceipt("Hotel", new Amount(new BigDecimal("150.00")), ALICE, ALICE, LocalDate.now(), ExpenseCategory.ACCOMMODATION);

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        assertThat(balances.get(ALICE)).isEqualByComparingTo("50.00");
        assertThat(balances.get(BOB)).isEqualByComparingTo("-50.00");
    }

    @Test
    void updateWeightingChangesBalance() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(new BigDecimal("90.00")), ALICE, ALICE, LocalDate.now(), GROCERIES);

        expense.updateWeighting(BOB, new BigDecimal("2.0"));

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();
        assertThat(balances.get(ALICE)).isEqualByComparingTo("60.00");
        assertThat(balances.get(BOB)).isEqualByComparingTo("-60.00");
    }

    @Test
    void cannotUpdateWeightingOnSettledExpense() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(BigDecimal.TEN), ALICE, ALICE, LocalDate.now(), GROCERIES);
        expense.settle();

        assertThatThrownBy(() -> expense.updateWeighting(BOB, BigDecimal.ZERO))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noReceiptsBalancesAreAllZero() {
        final Expense expense = createExpenseForTwoAdults();

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        assertThat(balances.get(ALICE)).isEqualByComparingTo("0");
        assertThat(balances.get(BOB)).isEqualByComparingTo("0");
    }

    // --- Settlement Plan ---

    @Test
    void calculateSettlementPlanProducesTransfers() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("100.00")), ALICE, ALICE, LocalDate.now(), RESTAURANT);

        final SettlementPlan plan = expense.calculateSettlementPlan();

        assertThat(plan.transfers()).hasSize(1);
        assertThat(plan.transfers().getFirst().from()).isEqualTo(BOB);
        assertThat(plan.transfers().getFirst().to()).isEqualTo(ALICE);
        assertThat(plan.transfers().getFirst().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void calculateSettlementPlanEmptyWhenNoReceipts() {
        final Expense expense = createExpenseForTwoAdults();

        final SettlementPlan plan = expense.calculateSettlementPlan();

        assertThat(plan.transfers()).isEmpty();
    }

    // --- Helpers ---

    private Expense createExpenseForTwoAdults() {
        return Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, BigDecimal.ONE)
        ));
    }

    private Expense createReviewExpense() {
        return Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, BigDecimal.ONE)
        ), true);
    }
}
