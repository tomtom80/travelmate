package de.evia.travelmate.trips.adapters.persistence;

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
@Table(name = "accommodation_poll")
public class AccommodationPollJpaEntity {

    @Id
    @Column(name = "accommodation_poll_id")
    private UUID accommodationPollId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "selected_candidate_id")
    private UUID selectedCandidateId;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AccommodationCandidateJpaEntity> candidates = new ArrayList<>();

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AccommodationVoteJpaEntity> votes = new ArrayList<>();

    protected AccommodationPollJpaEntity() {
    }

    public AccommodationPollJpaEntity(final UUID accommodationPollId, final UUID tenantId,
                                      final UUID tripId, final String status) {
        this.accommodationPollId = accommodationPollId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.status = status;
    }

    public UUID getAccommodationPollId() { return accommodationPollId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public String getStatus() { return status; }
    public void setStatus(final String status) { this.status = status; }
    public UUID getSelectedCandidateId() { return selectedCandidateId; }
    public void setSelectedCandidateId(final UUID selectedCandidateId) { this.selectedCandidateId = selectedCandidateId; }
    public List<AccommodationCandidateJpaEntity> getCandidates() { return candidates; }
    public List<AccommodationVoteJpaEntity> getVotes() { return votes; }
}
