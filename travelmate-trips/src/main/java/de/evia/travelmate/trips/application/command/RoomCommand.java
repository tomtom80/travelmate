package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;

public record RoomCommand(
    String name,
    int bedCount,
    BigDecimal pricePerNight
) {
}
