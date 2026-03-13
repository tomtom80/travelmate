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

class ExpenseTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CHILD = UUID.randomUUID();

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
    void addReceiptToOpenExpense() {
        final Expense expense = createExpenseForTwoAdults();

        expense.addReceipt("Groceries", new Amount(new BigDecimal("60.00")), ALICE, LocalDate.now());

        assertThat(expense.receipts()).hasSize(1);
        assertThat(expense.receipts().getFirst().description()).isEqualTo("Groceries");
        assertThat(expense.receipts().getFirst().paidBy()).isEqualTo(ALICE);
    }

    @Test
    void addReceiptRejectsUnknownPayer() {
        final Expense expense = createExpenseForTwoAdults();
        final UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> expense.addReceipt("Food", new Amount(BigDecimal.TEN), stranger, LocalDate.now()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a participant");
    }

    @Test
    void removeReceiptFromOpenExpense() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Groceries", new Amount(BigDecimal.TEN), ALICE, LocalDate.now());
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

    @Test
    void settleRegistersExpenseSettledEvent() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("100.00")), ALICE, LocalDate.now());
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
    void cannotAddReceiptToSettledExpense() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(BigDecimal.TEN), ALICE, LocalDate.now());
        expense.settle();

        assertThatThrownBy(() -> expense.addReceipt("More food", new Amount(BigDecimal.TEN), BOB, LocalDate.now()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SETTLED");
    }

    @Test
    void cannotSettleTwice() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(BigDecimal.TEN), ALICE, LocalDate.now());
        expense.settle();

        assertThatThrownBy(expense::settle)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void calculateBalancesEqualSplit() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("100.00")), ALICE, LocalDate.now());

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        // Alice paid 100, owes 50 → balance +50
        assertThat(balances.get(ALICE)).isEqualByComparingTo("50.00");
        // Bob paid 0, owes 50 → balance -50
        assertThat(balances.get(BOB)).isEqualByComparingTo("-50.00");
    }

    @Test
    void calculateBalancesMultipleReceipts() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("100.00")), ALICE, LocalDate.now());
        expense.addReceipt("Taxi", new Amount(new BigDecimal("40.00")), BOB, LocalDate.now());

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        // Total 140, each owes 70
        // Alice paid 100, owes 70 → +30
        assertThat(balances.get(ALICE)).isEqualByComparingTo("30.00");
        // Bob paid 40, owes 70 → -30
        assertThat(balances.get(BOB)).isEqualByComparingTo("-30.00");
    }

    @Test
    void calculateBalancesWithChildWeighting() {
        final Expense expense = Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, BigDecimal.ONE),
            new ParticipantWeighting(CHILD, BigDecimal.ZERO)
        ));
        expense.addReceipt("Groceries", new Amount(new BigDecimal("100.00")), ALICE, LocalDate.now());

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        // Total weight = 2.0. Alice and Bob each owe 50. Child owes 0.
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
        expense.addReceipt("Hotel", new Amount(new BigDecimal("150.00")), ALICE, LocalDate.now());

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();

        // Total weight = 1.5. Alice's share = 100, Bob's share = 50.
        assertThat(balances.get(ALICE)).isEqualByComparingTo("50.00");
        assertThat(balances.get(BOB)).isEqualByComparingTo("-50.00");
    }

    @Test
    void updateWeightingChangesBalance() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(new BigDecimal("90.00")), ALICE, LocalDate.now());

        expense.updateWeighting(BOB, new BigDecimal("2.0"));

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();
        // Total weight = 3.0. Alice share = 30, Bob share = 60.
        assertThat(balances.get(ALICE)).isEqualByComparingTo("60.00");
        assertThat(balances.get(BOB)).isEqualByComparingTo("-60.00");
    }

    @Test
    void cannotUpdateWeightingOnSettledExpense() {
        final Expense expense = createExpenseForTwoAdults();
        expense.addReceipt("Food", new Amount(BigDecimal.TEN), ALICE, LocalDate.now());
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

    private Expense createExpenseForTwoAdults() {
        return Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, BigDecimal.ONE)
        ));
    }
}
