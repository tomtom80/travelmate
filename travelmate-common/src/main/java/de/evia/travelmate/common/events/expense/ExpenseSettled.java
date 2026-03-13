package de.evia.travelmate.common.events.expense;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record ExpenseSettled(
    UUID tenantId,
    UUID tripId,
    UUID expenseId,
    LocalDate occurredOn
) implements DomainEvent {
}
