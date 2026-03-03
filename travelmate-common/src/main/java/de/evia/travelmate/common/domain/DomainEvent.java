package de.evia.travelmate.common.domain;

import java.time.LocalDate;

public interface DomainEvent {

    LocalDate occurredOn();
}
