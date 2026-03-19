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
    List<ImportedRoom> rooms,
    Integer maxGuests
) {
    public AccommodationImportResult(
        final String name,
        final String address,
        final String bookingUrl,
        final LocalDate checkIn,
        final LocalDate checkOut,
        final BigDecimal totalPrice,
        final String notes,
        final List<ImportedRoom> rooms
    ) {
        this(name, address, bookingUrl, checkIn, checkOut, totalPrice, notes, rooms, null);
    }
}
