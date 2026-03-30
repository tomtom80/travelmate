package de.evia.travelmate.trips.application.representation;

import java.util.List;
import java.util.UUID;

import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidate;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPoll;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationVote;

public record AccommodationPollRepresentation(
    UUID accommodationPollId,
    UUID tenantId,
    UUID tripId,
    String status,
    UUID selectedCandidateId,
    List<CandidateRepresentation> candidates,
    List<VoteRepresentation> votes
) {

    public AccommodationPollRepresentation(final AccommodationPoll poll) {
        this(
            poll.accommodationPollId().value(),
            poll.tenantId().value(),
            poll.tripId().value(),
            poll.status().name(),
            poll.selectedCandidateId() != null ? poll.selectedCandidateId().value() : null,
            poll.candidates().stream().map(c -> new CandidateRepresentation(c, poll)).toList(),
            poll.votes().stream().map(VoteRepresentation::new).toList()
        );
    }

    public record CandidateRepresentation(
        UUID candidateId,
        String name,
        String url,
        String description,
        long voteCount
    ) {
        public CandidateRepresentation(final AccommodationCandidate candidate, final AccommodationPoll poll) {
            this(
                candidate.candidateId().value(),
                candidate.name(),
                candidate.url(),
                candidate.description(),
                poll.voteCountForCandidate(candidate.candidateId())
            );
        }
    }

    public record VoteRepresentation(
        UUID voteId,
        UUID voterId,
        UUID selectedCandidateId
    ) {
        public VoteRepresentation(final AccommodationVote vote) {
            this(
                vote.voteId().value(),
                vote.voterId(),
                vote.selectedCandidateId().value()
            );
        }
    }
}
