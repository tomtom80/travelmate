package de.evia.travelmate.trips.adapters.persistence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip")
public class TripJpaEntity {

    @Id
    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ParticipantJpaEntity> participants = new ArrayList<>();

    protected TripJpaEntity() {
    }

    public TripJpaEntity(final UUID tripId, final UUID tenantId, final String name,
                         final String description, final LocalDate startDate, final LocalDate endDate,
                         final String status, final UUID organizerId) {
        this.tripId = tripId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.organizerId = organizerId;
    }

    public UUID getTripId() { return tripId; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public void setStatus(final String status) { this.status = status; }
    public UUID getOrganizerId() { return organizerId; }
    public List<ParticipantJpaEntity> getParticipants() { return participants; }
}
