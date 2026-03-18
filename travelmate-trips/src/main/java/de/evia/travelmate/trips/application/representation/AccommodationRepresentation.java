package de.evia.travelmate.trips.application.representation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.trips.domain.accommodation.Accommodation;

public record AccommodationRepresentation(
    UUID accommodationId,
    UUID tripId,
    String name,
    String address,
    String url,
    LocalDate checkIn,
    LocalDate checkOut,
    BigDecimal totalPrice,
    List<RoomRepresentation> rooms,
    int totalBedCount,
    List<RoomAssignmentRepresentation> assignments,
    int totalAssignedPersons
) {

    public AccommodationRepresentation(final Accommodation accommodation) {
        this(
            accommodation.accommodationId().value(),
            accommodation.tripId().value(),
            accommodation.name(),
            accommodation.address(),
            accommodation.url(),
            accommodation.checkIn(),
            accommodation.checkOut(),
            accommodation.totalPrice(),
            accommodation.rooms().stream()
                .map(RoomRepresentation::new)
                .toList(),
            accommodation.totalBedCount(),
            accommodation.assignments().stream()
                .map(RoomAssignmentRepresentation::new)
                .toList(),
            accommodation.totalAssignedPersons()
        );
    }
}
