package de.evia.travelmate.expense.domain.trip;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record TripParticipant(UUID participantId, String name) {

    public TripParticipant {
        argumentIsNotNull(participantId, "participantId");
        argumentIsNotBlank(name, "name");
    }
}
