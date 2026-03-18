package de.evia.travelmate.trips.domain.accommodation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record RoomId(UUID value) {

    public RoomId {
        argumentIsNotNull(value, "roomId");
    }
}
