package de.evia.travelmate.expense.domain.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

/**
 * Value object representing the cost breakdown per expense category.
 * Computed from a list of approved receipts.
 */
public record CategoryBreakdown(
    BigDecimal totalAmount,
    List<CategoryShare> categories
) {

    public CategoryBreakdown {
        argumentIsNotNull(totalAmount, "totalAmount");
        argumentIsNotNull(categories, "categories");
        categories = List.copyOf(categories);
    }

    /**
     * Computes category breakdown from approved receipts.
     * Categories with zero amount are excluded.
     */
    public static CategoryBreakdown fromReceipts(final List<Receipt> receipts) {
        argumentIsNotNull(receipts, "receipts");

        final Map<ExpenseCategory, BigDecimal> totals = new HashMap<>();
        final Map<ExpenseCategory, Integer> counts = new HashMap<>();

        for (final Receipt receipt : receipts) {
            if (receipt.reviewStatus() == ReviewStatus.APPROVED) {
                final ExpenseCategory cat = receipt.category();
                totals.merge(cat, receipt.amount().value(), BigDecimal::add);
                counts.merge(cat, 1, Integer::sum);
            }
        }

        final BigDecimal total = totals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final List<CategoryShare> shares = totals.entrySet().stream()
            .filter(e -> e.getValue().signum() > 0)
            .sorted(Comparator.comparing(Map.Entry<ExpenseCategory, BigDecimal>::getValue).reversed())
            .map(e -> {
                final BigDecimal pct = total.signum() > 0
                    ? e.getValue().multiply(new BigDecimal("100"))
                        .divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return new CategoryShare(e.getKey(), e.getValue(), pct, counts.get(e.getKey()));
            })
            .toList();

        return new CategoryBreakdown(total, shares);
    }

    /**
     * Computes category breakdown from approved receipts, adding accommodation total
     * from TripProjection to the ACCOMMODATION bucket.
     */
    public static CategoryBreakdown fromReceipts(final List<Receipt> receipts,
                                                  final BigDecimal accommodationTotal) {
        argumentIsNotNull(receipts, "receipts");

        final Map<ExpenseCategory, BigDecimal> totals = new HashMap<>();
        final Map<ExpenseCategory, Integer> counts = new HashMap<>();

        for (final Receipt receipt : receipts) {
            if (receipt.reviewStatus() == ReviewStatus.APPROVED) {
                final ExpenseCategory cat = receipt.category();
                totals.merge(cat, receipt.amount().value(), BigDecimal::add);
                counts.merge(cat, 1, Integer::sum);
            }
        }

        if (accommodationTotal != null && accommodationTotal.signum() > 0) {
            totals.merge(ExpenseCategory.ACCOMMODATION, accommodationTotal, BigDecimal::add);
            counts.merge(ExpenseCategory.ACCOMMODATION, 0, Integer::sum);
        }

        final BigDecimal total = totals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        final List<CategoryShare> shares = totals.entrySet().stream()
            .filter(e -> e.getValue().signum() > 0)
            .sorted(Comparator.comparing(Map.Entry<ExpenseCategory, BigDecimal>::getValue).reversed())
            .map(e -> {
                final BigDecimal pct = total.signum() > 0
                    ? e.getValue().multiply(new BigDecimal("100"))
                        .divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return new CategoryShare(e.getKey(), e.getValue(), pct, counts.get(e.getKey()));
            })
            .toList();

        return new CategoryBreakdown(total, shares);
    }
}
