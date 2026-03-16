package de.evia.travelmate.expense.application.representation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.domain.expense.Amount;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;

class ExpenseRepresentationTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void categoryBreakdownGroupsReceiptsByCategory() {
        final Expense expense = createExpense();
        expense.addReceipt("Supermarkt", new Amount(new BigDecimal("30.00")),
            ALICE, ALICE, LocalDate.now(), ExpenseCategory.GROCERIES);
        expense.addReceipt("Restaurant", new Amount(new BigDecimal("50.00")),
            BOB, BOB, LocalDate.now(), ExpenseCategory.RESTAURANT);
        expense.addReceipt("Snacks", new Amount(new BigDecimal("10.00")),
            ALICE, ALICE, LocalDate.now(), ExpenseCategory.GROCERIES);

        final ExpenseRepresentation repr = ExpenseRepresentation.from(expense);

        assertThat(repr.categoryBreakdown()).hasSize(2);
        // Sorted by total descending: RESTAURANT 50, GROCERIES 40
        assertThat(repr.categoryBreakdown().get(0).category()).isEqualTo(ExpenseCategory.RESTAURANT);
        assertThat(repr.categoryBreakdown().get(0).total()).isEqualByComparingTo("50.00");
        assertThat(repr.categoryBreakdown().get(0).receiptCount()).isEqualTo(1);
        assertThat(repr.categoryBreakdown().get(1).category()).isEqualTo(ExpenseCategory.GROCERIES);
        assertThat(repr.categoryBreakdown().get(1).total()).isEqualByComparingTo("40.00");
        assertThat(repr.categoryBreakdown().get(1).receiptCount()).isEqualTo(2);
    }

    @Test
    void categoryBreakdownEmptyWhenNoReceipts() {
        final Expense expense = createExpense();

        final ExpenseRepresentation repr = ExpenseRepresentation.from(expense);

        assertThat(repr.categoryBreakdown()).isEmpty();
    }

    @Test
    void participantSummaryShowsPaidShareAndBalance() {
        final Expense expense = createExpense();
        expense.addReceipt("Hotel", new Amount(new BigDecimal("200.00")),
            ALICE, ALICE, LocalDate.now(), ExpenseCategory.ACCOMMODATION);

        final ExpenseRepresentation repr = ExpenseRepresentation.from(expense);

        assertThat(repr.participantSummaries()).hasSize(2);

        final ParticipantSummaryRepresentation aliceSummary = repr.participantSummaries().stream()
            .filter(ps -> ps.participantId().equals(ALICE))
            .findFirst().orElseThrow();
        assertThat(aliceSummary.totalPaid()).isEqualByComparingTo("200.00");
        assertThat(aliceSummary.fairShare()).isEqualByComparingTo("100.00");
        assertThat(aliceSummary.balance()).isEqualByComparingTo("100.00");

        final ParticipantSummaryRepresentation bobSummary = repr.participantSummaries().stream()
            .filter(ps -> ps.participantId().equals(BOB))
            .findFirst().orElseThrow();
        assertThat(bobSummary.totalPaid()).isEqualByComparingTo("0.00");
        assertThat(bobSummary.fairShare()).isEqualByComparingTo("100.00");
        assertThat(bobSummary.balance()).isEqualByComparingTo("-100.00");
    }

    @Test
    void dailyCostsGroupReceiptsByDateWithZeroFill() {
        final Expense expense = createExpense();
        expense.addReceipt("Supermarkt", new Amount(new BigDecimal("30.00")),
            ALICE, ALICE, LocalDate.of(2026, 7, 1), ExpenseCategory.GROCERIES);
        expense.addReceipt("Restaurant", new Amount(new BigDecimal("50.00")),
            BOB, BOB, LocalDate.of(2026, 7, 1), ExpenseCategory.RESTAURANT);
        expense.addReceipt("Museum", new Amount(new BigDecimal("20.00")),
            ALICE, ALICE, LocalDate.of(2026, 7, 3), ExpenseCategory.ACTIVITY);

        final ExpenseRepresentation repr = ExpenseRepresentation.from(
            expense, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 4));

        assertThat(repr.dailyCosts()).hasSize(4);
        assertThat(repr.dailyCosts().get(0).date()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(repr.dailyCosts().get(0).total()).isEqualByComparingTo("80.00");
        assertThat(repr.dailyCosts().get(0).receiptCount()).isEqualTo(2);
        assertThat(repr.dailyCosts().get(1).date()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(repr.dailyCosts().get(1).total()).isEqualByComparingTo("0.00");
        assertThat(repr.dailyCosts().get(1).receiptCount()).isEqualTo(0);
        assertThat(repr.dailyCosts().get(2).date()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(repr.dailyCosts().get(2).total()).isEqualByComparingTo("20.00");
        assertThat(repr.dailyCosts().get(2).receiptCount()).isEqualTo(1);
        assertThat(repr.dailyCosts().get(3).date()).isEqualTo(LocalDate.of(2026, 7, 4));
        assertThat(repr.dailyCosts().get(3).total()).isEqualByComparingTo("0.00");
    }

    @Test
    void dailyCostsEmptyWhenNoTripDates() {
        final Expense expense = createExpense();
        expense.addReceipt("Supermarkt", new Amount(new BigDecimal("30.00")),
            ALICE, ALICE, LocalDate.of(2026, 7, 1), ExpenseCategory.GROCERIES);

        final ExpenseRepresentation repr = ExpenseRepresentation.from(expense);

        assertThat(repr.dailyCosts()).isEmpty();
    }

    @Test
    void dailyCostsIncludesReceiptsOutsideTripRange() {
        final Expense expense = createExpense();
        expense.addReceipt("Pre-trip", new Amount(new BigDecimal("15.00")),
            ALICE, ALICE, LocalDate.of(2026, 6, 30), ExpenseCategory.OTHER);
        expense.addReceipt("In-trip", new Amount(new BigDecimal("40.00")),
            BOB, BOB, LocalDate.of(2026, 7, 1), ExpenseCategory.GROCERIES);

        final ExpenseRepresentation repr = ExpenseRepresentation.from(
            expense, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2));

        assertThat(repr.dailyCosts()).hasSize(3);
        // Trip range: 2026-07-01 to 2026-07-02 (zero-filled), plus 2026-06-30 added by merge
        assertThat(repr.dailyCosts().get(0).total()).isEqualByComparingTo("40.00");
        assertThat(repr.dailyCosts().get(0).date()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(repr.dailyCosts().get(1).total()).isEqualByComparingTo("0.00");
        assertThat(repr.dailyCosts().get(1).date()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(repr.dailyCosts().get(2).total()).isEqualByComparingTo("15.00");
        assertThat(repr.dailyCosts().get(2).date()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @Test
    void transfersArePopulated() {
        final Expense expense = createExpense();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("80.00")),
            ALICE, ALICE, LocalDate.now(), ExpenseCategory.RESTAURANT);

        final ExpenseRepresentation repr = ExpenseRepresentation.from(expense);

        assertThat(repr.transfers()).hasSize(1);
        assertThat(repr.transfers().getFirst().from()).isEqualTo(BOB);
        assertThat(repr.transfers().getFirst().to()).isEqualTo(ALICE);
        assertThat(repr.transfers().getFirst().amount()).isEqualByComparingTo("40.00");
    }

    private Expense createExpense() {
        return Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, BigDecimal.ONE)
        ));
    }
}
