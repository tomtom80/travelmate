package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public class AccommodationVote {

    private final AccommodationVoteId voteId;
    private final UUID voterId;
    private AccommodationCandidateId selectedCandidateId;

    public AccommodationVote(final AccommodationVoteId voteId,
                             final UUID voterId,
                             final AccommodationCandidateId selectedCandidateId) {
        argumentIsNotNull(voteId, "voteId");
        argumentIsNotNull(voterId, "voterId");
        argumentIsNotNull(selectedCandidateId, "selectedCandidateId");
        this.voteId = voteId;
        this.voterId = voterId;
        this.selectedCandidateId = selectedCandidateId;
    }

    public void changeSelection(final AccommodationCandidateId newCandidateId) {
        argumentIsNotNull(newCandidateId, "newCandidateId");
        this.selectedCandidateId = newCandidateId;
    }

    public AccommodationVoteId voteId() {
        return voteId;
    }

    public UUID voterId() {
        return voterId;
    }

    public AccommodationCandidateId selectedCandidateId() {
        return selectedCandidateId;
    }
}
