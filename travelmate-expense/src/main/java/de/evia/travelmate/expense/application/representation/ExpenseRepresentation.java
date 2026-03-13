package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;

public record ExpenseRepresentation(
    UUID expenseId,
    UUID tripId,
    ExpenseStatus status,
    List<ReceiptRepresentation> receipts,
    List<WeightingRepresentation> weightings,
    Map<UUID, BigDecimal> balances,
    BigDecimal totalAmount
) {

    public static ExpenseRepresentation from(final Expense expense) {
        final List<ReceiptRepresentation> receipts = expense.receipts().stream()
            .map(r -> new ReceiptRepresentation(
                r.receiptId().value(),
                r.description(),
                r.amount().value(),
                r.paidBy(),
                r.date()
            ))
            .toList();

        final List<WeightingRepresentation> weightings = expense.weightings().stream()
            .map(w -> new WeightingRepresentation(w.participantId(), w.weight()))
            .toList();

        final BigDecimal totalAmount = expense.receipts().stream()
            .map(r -> r.amount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ExpenseRepresentation(
            expense.expenseId().value(),
            expense.tripId(),
            expense.status(),
            receipts,
            weightings,
            expense.calculateBalances(),
            totalAmount
        );
    }
}
