package de.evia.travelmate.trips.domain.accommodation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record RoomAssignmentId(UUID value) {

    public RoomAssignmentId {
        argumentIsNotNull(value, "roomAssignmentId");
    }
}
