package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.util.UUID;

public record PartyTransferRepresentation(
    UUID fromPartyId,
    String fromPartyName,
    UUID toPartyId,
    String toPartyName,
    BigDecimal amount
) {
}
