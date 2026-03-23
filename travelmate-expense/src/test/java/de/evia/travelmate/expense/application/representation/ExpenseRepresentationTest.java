package de.evia.travelmate.expense.application.representation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.expense.domain.expense.Amount;
import de.evia.travelmate.expense.domain.expense.AdvancePayment;
import de.evia.travelmate.expense.domain.expense.AdvancePaymentId;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.PartyAccountEntryType;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.trip.TripParticipant;

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

    @Test
    void partyAccountsShowOutstandingAndCreditIncludingAdvances() {
        final UUID partyA = UUID.randomUUID();
        final UUID partyB = UUID.randomUUID();
        final Expense expense = createExpense();
        expense.addReceipt("Dinner", new Amount(new BigDecimal("80.00")),
            ALICE, ALICE, LocalDate.now(), ExpenseCategory.RESTAURANT);
        final Expense expenseWithAdvances = new Expense(
            expense.expenseId(),
            expense.tenantId(),
            expense.tripId(),
            expense.status(),
            expense.receipts(),
            expense.weightings(),
            List.of(
                new AdvancePayment(new AdvancePaymentId(UUID.randomUUID()), partyA, "Familie A", new BigDecimal("30.00"), true, LocalDate.of(2026, 7, 4), ALICE),
                new AdvancePayment(new AdvancePaymentId(UUID.randomUUID()), partyB, "Familie B", new BigDecimal("10.00"), false)
            ),
            expense.reviewRequired()
        );
        final List<TripParticipant> participants = List.of(
            new TripParticipant(ALICE, "Alice", null, null, partyA, "Familie A"),
            new TripParticipant(BOB, "Bob", null, null, partyB, "Familie B")
        );

        final ExpenseRepresentation repr = ExpenseRepresentation.from(expenseWithAdvances, null, null, null, participants);

        assertThat(repr.partyAccounts()).hasSize(2);
        final PartyAccountRepresentation partyAAccount = repr.partyAccounts().stream()
            .filter(account -> account.partyTenantId().equals(partyA))
            .findFirst().orElseThrow();
        assertThat(partyAAccount.receiptCredits()).isEqualByComparingTo("80.00");
        assertThat(partyAAccount.advancePaymentsPlanned()).isEqualByComparingTo("30.00");
        assertThat(partyAAccount.advancePaymentsPaid()).isEqualByComparingTo("30.00");
        assertThat(partyAAccount.advancePaymentsOutstanding()).isEqualByComparingTo("0.00");
        assertThat(partyAAccount.fairShare()).isEqualByComparingTo("40.00");
        assertThat(partyAAccount.creditAmount()).isEqualByComparingTo("70.00");
        assertThat(partyAAccount.entries())
            .extracting(PartyAccountEntryRepresentation::type, PartyAccountEntryRepresentation::label)
            .contains(tuple(PartyAccountEntryType.ADVANCE_PAYMENT, "Familie A bezahlt am 2026-07-04 von Alice"));

        final PartyAccountRepresentation partyBAccount = repr.partyAccounts().stream()
            .filter(account -> account.partyTenantId().equals(partyB))
            .findFirst().orElseThrow();
        assertThat(partyBAccount.receiptCredits()).isEqualByComparingTo("0.00");
        assertThat(partyBAccount.advancePaymentsPlanned()).isEqualByComparingTo("10.00");
        assertThat(partyBAccount.advancePaymentsPaid()).isEqualByComparingTo("0.00");
        assertThat(partyBAccount.advancePaymentsOutstanding()).isEqualByComparingTo("10.00");
        assertThat(partyBAccount.fairShare()).isEqualByComparingTo("40.00");
        assertThat(partyBAccount.outstandingAmount()).isEqualByComparingTo("40.00");
        assertThat(partyBAccount.entries())
            .extracting(PartyAccountEntryRepresentation::type, PartyAccountEntryRepresentation::amount)
            .contains(tuple(PartyAccountEntryType.ADVANCE_PAYMENT_DUE, new BigDecimal("-10.00")));
        assertThat(partyAAccount.entries().getFirst().label()).isEqualTo("Dinner (" + LocalDate.now() + ")");
        assertThat(partyAAccount.entries().getFirst().runningBalance()).isEqualByComparingTo("80.00");
        assertThat(partyBAccount.entries().getFirst().runningBalance()).isEqualByComparingTo("-10.00");
    }

    @Test
    void partyAccountsIncludeAccommodationShareWeightedByStayPeriod() {
        final UUID partyA = UUID.randomUUID();
        final UUID partyB = UUID.randomUUID();
        final Expense expense = createExpense();
        final List<TripParticipant> participants = List.of(
            new TripParticipant(ALICE, "Alice", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), partyA, "Familie A"),
            new TripParticipant(BOB, "Bob", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), partyB, "Familie B")
        );

        final ExpenseRepresentation repr = ExpenseRepresentation.from(
            expense,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 5),
            new BigDecimal("300.00"),
            participants
        );

        assertThat(repr.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(repr.categoryBreakdown()).singleElement().satisfies(category -> {
            assertThat(category.category()).isEqualTo(ExpenseCategory.ACCOMMODATION);
            assertThat(category.total()).isEqualByComparingTo("300.00");
        });

        final PartyAccountRepresentation partyAAccount = repr.partyAccounts().stream()
            .filter(account -> account.partyTenantId().equals(partyA))
            .findFirst().orElseThrow();
        assertThat(partyAAccount.fairShare()).isEqualByComparingTo("200.00");
        assertThat(partyAAccount.outstandingAmount()).isEqualByComparingTo("200.00");
        assertThat(partyAAccount.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.label()).isEqualTo("Alice - 4 Naechte x 1");
            assertThat(entry.amount()).isEqualByComparingTo("-200.00");
            assertThat(entry.runningBalance()).isEqualByComparingTo("-200.00");
        });

        final PartyAccountRepresentation partyBAccount = repr.partyAccounts().stream()
            .filter(account -> account.partyTenantId().equals(partyB))
            .findFirst().orElseThrow();
        assertThat(partyBAccount.fairShare()).isEqualByComparingTo("100.00");
        assertThat(partyBAccount.outstandingAmount()).isEqualByComparingTo("100.00");
        assertThat(partyBAccount.entries()).singleElement().satisfies(entry -> {
            assertThat(entry.label()).isEqualTo("Bob - 2 Naechte x 1");
            assertThat(entry.amount()).isEqualByComparingTo("-100.00");
        });
    }

    @Test
    void weightingsExposeAgeBasedRecommendationForUi() {
        final Expense expense = createExpense();
        final List<TripParticipant> participants = List.of(
            new TripParticipant(ALICE, "Alice", null, null, null, null, LocalDate.of(2024, 8, 1), false),
            new TripParticipant(BOB, "Bob", null, null, null, null, LocalDate.of(2015, 6, 1), false)
        );

        final ExpenseRepresentation repr = ExpenseRepresentation.from(
            expense,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 14),
            null,
            participants
        );

        assertThat(repr.weightings())
            .extracting(WeightingRepresentation::participantId,
                WeightingRepresentation::recommendedWeight,
                WeightingRepresentation::ageOnTripStart,
                WeightingRepresentation::recommendationType)
            .containsExactlyInAnyOrder(
                tuple(ALICE, BigDecimal.ZERO, 1, "INFANT"),
                tuple(BOB, new BigDecimal("0.5"), 11, "CHILD")
            );
    }

    private Expense createExpense() {
        return Expense.create(TENANT_ID, TRIP_ID, List.of(
            new ParticipantWeighting(ALICE, BigDecimal.ONE),
            new ParticipantWeighting(BOB, BigDecimal.ONE)
        ));
    }
}
