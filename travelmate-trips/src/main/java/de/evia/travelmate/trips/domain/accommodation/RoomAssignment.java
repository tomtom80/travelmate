package de.evia.travelmate.trips.domain.accommodation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.time.Instant;
import java.util.UUID;

public class RoomAssignment {

    private final RoomAssignmentId assignmentId;
    private final RoomId roomId;
    private final UUID partyTenantId;
    private final String partyName;
    private int personCount;
    private final Instant assignedAt;

    public RoomAssignment(final RoomAssignmentId assignmentId,
                          final RoomId roomId,
                          final UUID partyTenantId,
                          final String partyName,
                          final int personCount,
                          final Instant assignedAt) {
        argumentIsNotNull(assignmentId, "assignmentId");
        argumentIsNotNull(roomId, "roomId");
        argumentIsNotNull(partyTenantId, "partyTenantId");
        argumentIsNotBlank(partyName, "partyName");
        argumentIsTrue(personCount > 0, "Person count must be at least 1.");
        argumentIsNotNull(assignedAt, "assignedAt");
        this.assignmentId = assignmentId;
        this.roomId = roomId;
        this.partyTenantId = partyTenantId;
        this.partyName = partyName;
        this.personCount = personCount;
        this.assignedAt = assignedAt;
    }

    public static RoomAssignment create(final RoomId roomId,
                                         final UUID partyTenantId,
                                         final String partyName,
                                         final int personCount) {
        return new RoomAssignment(
            new RoomAssignmentId(UUID.randomUUID()),
            roomId,
            partyTenantId,
            partyName,
            personCount,
            Instant.now()
        );
    }

    public void updatePersonCount(final int personCount) {
        argumentIsTrue(personCount > 0, "Person count must be at least 1.");
        this.personCount = personCount;
    }

    public RoomAssignmentId assignmentId() {
        return assignmentId;
    }

    public RoomId roomId() {
        return roomId;
    }

    public UUID partyTenantId() {
        return partyTenantId;
    }

    public String partyName() {
        return partyName;
    }

    public int personCount() {
        return personCount;
    }

    public Instant assignedAt() {
        return assignedAt;
    }
}
