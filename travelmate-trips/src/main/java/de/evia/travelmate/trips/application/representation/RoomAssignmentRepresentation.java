package de.evia.travelmate.trips.application.representation;

import java.time.Instant;
import java.util.UUID;

import de.evia.travelmate.trips.domain.accommodation.RoomAssignment;

public record RoomAssignmentRepresentation(
    UUID assignmentId,
    UUID roomId,
    UUID partyTenantId,
    String partyName,
    int personCount,
    Instant assignedAt
) {

    public RoomAssignmentRepresentation(final RoomAssignment assignment) {
        this(
            assignment.assignmentId().value(),
            assignment.roomId().value(),
            assignment.partyTenantId(),
            assignment.partyName(),
            assignment.personCount(),
            assignment.assignedAt()
        );
    }
}
