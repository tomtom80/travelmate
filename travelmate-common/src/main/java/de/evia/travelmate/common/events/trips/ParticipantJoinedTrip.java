package de.evia.travelmate.common.events.trips;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.DomainEvent;

public record ParticipantJoinedTrip(
    UUID tenantId,
    UUID tripId,
    UUID participantId,
    String username,
    UUID participantTenantId,
    String partyName,
    LocalDate occurredOn
) implements DomainEvent {

    /**
     * Backward-compatible constructor without party information.
     */
    public ParticipantJoinedTrip(final UUID tenantId, final UUID tripId,
                                  final UUID participantId, final String username,
                                  final LocalDate occurredOn) {
        this(tenantId, tripId, participantId, username, null, null, occurredOn);
    }
}
