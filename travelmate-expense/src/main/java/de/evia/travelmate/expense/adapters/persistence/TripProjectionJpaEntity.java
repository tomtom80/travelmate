package de.evia.travelmate.expense.adapters.persistence;

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
@Table(name = "trip_projection")
public class TripProjectionJpaEntity {

    @Id
    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_name", nullable = false)
    private String tripName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @OneToMany(mappedBy = "tripProjection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TripParticipantJpaEntity> participants = new ArrayList<>();

    protected TripProjectionJpaEntity() {
    }

    public TripProjectionJpaEntity(final UUID tripId, final UUID tenantId, final String tripName) {
        this.tripId = tripId;
        this.tenantId = tenantId;
        this.tripName = tripName;
    }

    public UUID getTripId() { return tripId; }
    public UUID getTenantId() { return tenantId; }
    public String getTripName() { return tripName; }
    public void setTripName(final String tripName) { this.tripName = tripName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(final LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(final LocalDate endDate) { this.endDate = endDate; }
    public List<TripParticipantJpaEntity> getParticipants() { return participants; }
}
