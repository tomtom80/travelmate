package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PartySettlementRepresentation(
    UUID partyTenantId,
    String partyName,
    BigDecimal totalPaid,
    BigDecimal totalOwed,
    BigDecimal balance,
    List<String> memberNames
) {
}
