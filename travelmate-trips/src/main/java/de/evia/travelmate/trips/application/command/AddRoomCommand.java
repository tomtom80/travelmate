package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record AddRoomCommand(
    UUID tenantId,
    UUID tripId,
    String name,
    String roomType,
    int bedCount,
    BigDecimal pricePerNight
) {
}
