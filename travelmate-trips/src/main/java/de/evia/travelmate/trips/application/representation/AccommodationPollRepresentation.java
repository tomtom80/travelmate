package de.evia.travelmate.trips.application.representation;

import java.util.List;
import java.math.BigDecimal;
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
        , List<RoomRepresentation> rooms
    ) {
        public CandidateRepresentation(final AccommodationCandidate candidate, final AccommodationPoll poll) {
            this(
                candidate.candidateId().value(),
                candidate.name(),
                candidate.url(),
                candidate.description(),
                poll.voteCountForCandidate(candidate.candidateId()),
                candidate.rooms().stream()
                    .map(r -> new RoomRepresentation(r.name(), r.bedCount(), r.pricePerNight(), r.features()))
                    .toList()
            );
        }
    }

    public record RoomRepresentation(String name, int bedCount, BigDecimal pricePerNight, String features) {
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

    public long totalVotes() {
        return candidates().stream()
            .mapToLong(CandidateRepresentation::voteCount)
            .sum();
    }
}
