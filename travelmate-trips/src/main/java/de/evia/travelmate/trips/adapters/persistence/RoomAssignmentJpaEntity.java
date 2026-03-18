package de.evia.travelmate.trips.adapters.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_assignment")
public class RoomAssignmentJpaEntity {

    @Id
    @Column(name = "assignment_id")
    private UUID assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private AccommodationJpaEntity accommodation;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "party_tenant_id", nullable = false)
    private UUID partyTenantId;

    @Column(name = "party_name", nullable = false, length = 200)
    private String partyName;

    @Column(name = "person_count", nullable = false)
    private int personCount;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected RoomAssignmentJpaEntity() {
    }

    public RoomAssignmentJpaEntity(final UUID assignmentId,
                                    final AccommodationJpaEntity accommodation,
                                    final UUID roomId,
                                    final UUID partyTenantId,
                                    final String partyName,
                                    final int personCount,
                                    final Instant assignedAt) {
        this.assignmentId = assignmentId;
        this.accommodation = accommodation;
        this.roomId = roomId;
        this.partyTenantId = partyTenantId;
        this.partyName = partyName;
        this.personCount = personCount;
        this.assignedAt = assignedAt;
    }

    public UUID getAssignmentId() { return assignmentId; }
    public UUID getRoomId() { return roomId; }
    public UUID getPartyTenantId() { return partyTenantId; }
    public String getPartyName() { return partyName; }
    public int getPersonCount() { return personCount; }
    public void setPersonCount(final int personCount) { this.personCount = personCount; }
    public Instant getAssignedAt() { return assignedAt; }
}
