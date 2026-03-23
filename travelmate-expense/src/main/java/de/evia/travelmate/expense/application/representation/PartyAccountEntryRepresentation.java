package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;

import de.evia.travelmate.expense.domain.expense.PartyAccountEntryType;

public record PartyAccountEntryRepresentation(
    PartyAccountEntryType type,
    String label,
    BigDecimal amount,
    BigDecimal runningBalance
) {
}
