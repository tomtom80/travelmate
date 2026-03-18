package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain service that suggests a round advance payment amount per travel party.
 * Formula: ceil(accommodationCost / partyCount / 50) × 50
 */
public final class AdvancePaymentSuggestion {

    private static final BigDecimal FIFTY = new BigDecimal("50");

    private AdvancePaymentSuggestion() {
    }

    public static BigDecimal suggest(final BigDecimal accommodationCost, final int partyCount) {
        argumentIsNotNull(accommodationCost, "accommodationCost");
        argumentIsTrue(accommodationCost.compareTo(BigDecimal.ZERO) > 0,
            "Accommodation cost must be greater than 0.");
        argumentIsTrue(partyCount > 0, "Party count must be greater than 0.");

        final BigDecimal perParty = accommodationCost
            .divide(BigDecimal.valueOf(partyCount), 2, RoundingMode.CEILING);
        final BigDecimal rounded = perParty
            .divide(FIFTY, 0, RoundingMode.CEILING)
            .multiply(FIFTY);
        return rounded;
    }
}
