package de.evia.travelmate.trips.adapters.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accommodation_vote")
public class AccommodationVoteJpaEntity {

    @Id
    @Column(name = "vote_id")
    private UUID voteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_poll_id", nullable = false)
    private AccommodationPollJpaEntity poll;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @Column(name = "selected_candidate_id", nullable = false)
    private UUID selectedCandidateId;

    protected AccommodationVoteJpaEntity() {
    }

    public AccommodationVoteJpaEntity(final UUID voteId, final AccommodationPollJpaEntity poll,
                                      final UUID voterId, final UUID selectedCandidateId) {
        this.voteId = voteId;
        this.poll = poll;
        this.voterId = voterId;
        this.selectedCandidateId = selectedCandidateId;
    }

    public UUID getVoteId() { return voteId; }
    public AccommodationPollJpaEntity getPoll() { return poll; }
    public UUID getVoterId() { return voterId; }
    public UUID getSelectedCandidateId() { return selectedCandidateId; }
    public void setSelectedCandidateId(final UUID selectedCandidateId) { this.selectedCandidateId = selectedCandidateId; }
}
