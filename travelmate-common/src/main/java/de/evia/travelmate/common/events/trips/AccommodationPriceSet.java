package de.evia.travelmate.common.events.trips;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record AccommodationPriceSet(
    UUID tenantId,
    UUID tripId,
    BigDecimal totalPrice,
    LocalDate occurredOn
) implements DomainEvent {
}
