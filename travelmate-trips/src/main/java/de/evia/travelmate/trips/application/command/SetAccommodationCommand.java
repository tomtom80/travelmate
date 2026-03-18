package de.evia.travelmate.trips.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SetAccommodationCommand(
    UUID tenantId,
    UUID tripId,
    String name,
    String address,
    String url,
    LocalDate checkIn,
    LocalDate checkOut,
    BigDecimal totalPrice,
    List<RoomCommand> rooms
) {
}
