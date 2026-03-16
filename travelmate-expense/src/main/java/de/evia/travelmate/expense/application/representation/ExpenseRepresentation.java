package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseCategory;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.expense.Receipt;
import de.evia.travelmate.expense.domain.expense.SettlementPlan;

public record ExpenseRepresentation(
    UUID expenseId,
    UUID tripId,
    ExpenseStatus status,
    boolean reviewRequired,
    List<ReceiptRepresentation> receipts,
    List<WeightingRepresentation> weightings,
    Map<UUID, BigDecimal> balances,
    List<TransferRepresentation> transfers,
    List<CategoryBreakdownRepresentation> categoryBreakdown,
    List<ParticipantSummaryRepresentation> participantSummaries,
    List<DailyCostRepresentation> dailyCosts,
    BigDecimal totalAmount
) {

    public static ExpenseRepresentation from(final Expense expense) {
        return from(expense, null, null);
    }

    public static ExpenseRepresentation from(final Expense expense,
                                              final LocalDate tripStartDate,
                                              final LocalDate tripEndDate) {
        final List<ReceiptRepresentation> receipts = expense.receipts().stream()
            .map(r -> new ReceiptRepresentation(
                r.receiptId().value(),
                r.description(),
                r.amount().value(),
                r.paidBy(),
                r.submittedBy(),
                r.date(),
                r.category(),
                r.reviewStatus(),
                r.reviewerId(),
                r.rejectionReason()
            ))
            .toList();

        final List<WeightingRepresentation> weightings = expense.weightings().stream()
            .map(w -> new WeightingRepresentation(w.participantId(), w.weight()))
            .toList();

        final BigDecimal totalAmount = expense.receipts().stream()
            .map(r -> r.amount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final SettlementPlan plan = expense.calculateSettlementPlan();
        final List<TransferRepresentation> transfers = plan.transfers().stream()
            .map(t -> new TransferRepresentation(t.from(), t.to(), t.amount()))
            .toList();

        final List<CategoryBreakdownRepresentation> categoryBreakdown =
            buildCategoryBreakdown(expense.receipts());

        final Map<UUID, BigDecimal> balances = expense.calculateBalances();
        final List<ParticipantSummaryRepresentation> participantSummaries =
            buildParticipantSummaries(expense.receipts(), expense.weightings(), balances, totalAmount);

        final List<DailyCostRepresentation> dailyCosts =
            buildDailyCosts(expense.receipts(), tripStartDate, tripEndDate);

        return new ExpenseRepresentation(
            expense.expenseId().value(),
            expense.tripId(),
            expense.status(),
            expense.reviewRequired(),
            receipts,
            weightings,
            balances,
            transfers,
            categoryBreakdown,
            participantSummaries,
            dailyCosts,
            totalAmount
        );
    }

    private static List<CategoryBreakdownRepresentation> buildCategoryBreakdown(
            final List<Receipt> receipts) {
        final Map<ExpenseCategory, BigDecimal> totals = new HashMap<>();
        final Map<ExpenseCategory, Integer> counts = new HashMap<>();

        for (final Receipt receipt : receipts) {
            final ExpenseCategory cat = receipt.category();
            totals.merge(cat, receipt.amount().value(), BigDecimal::add);
            counts.merge(cat, 1, Integer::sum);
        }

        return totals.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry<ExpenseCategory, BigDecimal>::getValue).reversed())
            .map(e -> new CategoryBreakdownRepresentation(e.getKey(), e.getValue(), counts.get(e.getKey())))
            .toList();
    }

    private static List<ParticipantSummaryRepresentation> buildParticipantSummaries(
            final List<Receipt> receipts,
            final List<ParticipantWeighting> weightings,
            final Map<UUID, BigDecimal> balances,
            final BigDecimal totalAmount) {
        final BigDecimal totalWeight = weightings.stream()
            .map(ParticipantWeighting::weight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final Map<UUID, BigDecimal> paid = new HashMap<>();
        for (final Receipt receipt : receipts) {
            paid.merge(receipt.paidBy(), receipt.amount().value(), BigDecimal::add);
        }

        return weightings.stream()
            .map(w -> {
                final BigDecimal participantPaid = paid.getOrDefault(w.participantId(), BigDecimal.ZERO);
                final BigDecimal fairShare = totalWeight.signum() == 0
                    ? BigDecimal.ZERO
                    : totalAmount.multiply(w.weight())
                        .divide(totalWeight, 2, RoundingMode.HALF_UP);
                final BigDecimal balance = balances.getOrDefault(w.participantId(), BigDecimal.ZERO);
                return new ParticipantSummaryRepresentation(
                    w.participantId(), participantPaid, fairShare, balance);
            })
            .toList();
    }

    static List<DailyCostRepresentation> buildDailyCosts(
            final List<Receipt> receipts,
            final LocalDate tripStartDate,
            final LocalDate tripEndDate) {
        if (tripStartDate == null || tripEndDate == null) {
            return List.of();
        }

        final Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        final Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        for (LocalDate date = tripStartDate; !date.isAfter(tripEndDate); date = date.plusDays(1)) {
            totals.put(date, BigDecimal.ZERO);
            counts.put(date, 0);
        }

        for (final Receipt receipt : receipts) {
            final LocalDate date = receipt.date();
            if (date != null) {
                totals.merge(date, receipt.amount().value(), BigDecimal::add);
                counts.merge(date, 1, Integer::sum);
            }
        }

        final List<DailyCostRepresentation> result = new ArrayList<>();
        for (final Map.Entry<LocalDate, BigDecimal> entry : totals.entrySet()) {
            result.add(new DailyCostRepresentation(
                entry.getKey(), entry.getValue(), counts.get(entry.getKey())));
        }
        return result;
    }
}
