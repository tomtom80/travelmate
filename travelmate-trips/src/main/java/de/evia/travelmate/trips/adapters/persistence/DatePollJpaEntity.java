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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "date_poll")
public class DatePollJpaEntity {

    @Id
    @Column(name = "date_poll_id")
    private UUID datePollId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "confirmed_option_id")
    private UUID confirmedOptionId;

    @OneToMany(mappedBy = "datePoll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("startDate ASC")
    private List<DateOptionJpaEntity> options = new ArrayList<>();

    @OneToMany(mappedBy = "datePoll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DateVoteJpaEntity> votes = new ArrayList<>();

    protected DatePollJpaEntity() {
    }

    public DatePollJpaEntity(final UUID datePollId, final UUID tenantId, final UUID tripId, final String status) {
        this.datePollId = datePollId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.status = status;
    }

    public UUID getDatePollId() { return datePollId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTripId() { return tripId; }
    public String getStatus() { return status; }
    public void setStatus(final String status) { this.status = status; }
    public UUID getConfirmedOptionId() { return confirmedOptionId; }
    public void setConfirmedOptionId(final UUID confirmedOptionId) { this.confirmedOptionId = confirmedOptionId; }
    public List<DateOptionJpaEntity> getOptions() { return options; }
    public List<DateVoteJpaEntity> getVotes() { return votes; }
}
