package de.evia.travelmate.expense.domain.expense;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.math.BigDecimal;
import java.util.UUID;

public record ParticipantWeighting(UUID participantId, BigDecimal weight) {

    public ParticipantWeighting {
        argumentIsNotNull(participantId, "participantId");
        argumentIsNotNull(weight, "weight");
        argumentIsTrue(weight.compareTo(BigDecimal.ZERO) >= 0,
            "Weight must not be negative, but was: " + weight);
    }
}
