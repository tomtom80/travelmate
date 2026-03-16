package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.util.UUID;

public record ParticipantSummaryRepresentation(
    UUID participantId,
    BigDecimal totalPaid,
    BigDecimal fairShare,
    BigDecimal balance
) {
}
