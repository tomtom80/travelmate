package de.evia.travelmate.trips.domain.accommodation;

import java.math.BigDecimal;

public record ImportedRoom(
    String name,
    int bedCount,
    BigDecimal pricePerNight
) {
}
