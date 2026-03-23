package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PartyAccountRepresentation(
    UUID partyTenantId,
    String partyName,
    List<String> memberNames,
    List<PartyAccountEntryRepresentation> entries,
    BigDecimal receiptCredits,
    BigDecimal advancePaymentsPlanned,
    BigDecimal advancePaymentsPaid,
    BigDecimal advancePaymentsOutstanding,
    BigDecimal fairShare,
    BigDecimal currentBalance,
    BigDecimal outstandingAmount,
    BigDecimal creditAmount
) {
}
