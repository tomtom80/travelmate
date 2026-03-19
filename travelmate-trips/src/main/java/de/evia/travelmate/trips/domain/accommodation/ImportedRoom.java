package de.evia.travelmate.trips.domain.accommodation;

import java.math.BigDecimal;

public record ImportedRoom(
    String name,
    RoomType roomType,
    int bedCount,
    BigDecimal pricePerNight
) {
}
