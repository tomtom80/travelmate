package de.evia.travelmate.expense.application.representation;

import java.math.BigDecimal;
import java.util.UUID;

public record WeightingRepresentation(
    UUID participantId,
    BigDecimal weight,
    BigDecimal recommendedWeight,
    Integer ageOnTripStart,
    String recommendationType
) {
}
