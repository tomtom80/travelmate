package de.evia.travelmate.trips.domain.accommodation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AccommodationImportResult(
    String name,
    String address,
    String bookingUrl,
    LocalDate checkIn,
    LocalDate checkOut,
    BigDecimal totalPrice,
    String notes,
    List<ImportedRoom> rooms
) {
}
