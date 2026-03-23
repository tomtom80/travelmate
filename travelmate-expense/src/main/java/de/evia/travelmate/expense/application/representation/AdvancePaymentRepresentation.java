package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AdvancePaymentRepresentation(
    UUID advancePaymentId,
    UUID partyTenantId,
    String partyName,
    BigDecimal amount,
    boolean paid,
    LocalDate paidOn,
    UUID markedByParticipantId
) {
}
