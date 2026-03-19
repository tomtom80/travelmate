package de.evia.travelmate.trips.application.representation;

import java.math.BigDecimal;
import java.util.UUID;

import de.evia.travelmate.trips.domain.accommodation.Room;

public record RoomRepresentation(
    UUID roomId,
    String name,
    int bedCount,
    BigDecimal pricePerNight
) {

    public RoomRepresentation(final Room room) {
        this(
            room.roomId().value(),
            room.name(),
            room.bedCount(),
            room.pricePerNight()
        );
    }
}
