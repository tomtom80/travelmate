package de.evia.travelmate.trips.adapters.persistence;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "date_vote")
public class DateVoteJpaEntity {

    @Id
    @Column(name = "date_vote_id")
    private UUID dateVoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "date_poll_id", nullable = false)
    private DatePollJpaEntity datePoll;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "date_vote_option", joinColumns = @JoinColumn(name = "date_vote_id"))
    @Column(name = "date_option_id", nullable = false)
    private Set<UUID> selectedOptionIds = new LinkedHashSet<>();

    protected DateVoteJpaEntity() {
    }

    public DateVoteJpaEntity(final UUID dateVoteId, final DatePollJpaEntity datePoll,
                             final UUID voterId, final Set<UUID> selectedOptionIds) {
        this.dateVoteId = dateVoteId;
        this.datePoll = datePoll;
        this.voterId = voterId;
        this.selectedOptionIds = new LinkedHashSet<>(selectedOptionIds);
    }

    public UUID getDateVoteId() { return dateVoteId; }
    public DatePollJpaEntity getDatePoll() { return datePoll; }
    public UUID getVoterId() { return voterId; }
    public Set<UUID> getSelectedOptionIds() { return selectedOptionIds; }
    public void setSelectedOptionIds(final Set<UUID> selectedOptionIds) {
        this.selectedOptionIds.clear();
        this.selectedOptionIds.addAll(selectedOptionIds);
    }
}
