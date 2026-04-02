package de.evia.travelmate.trips.application.representation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidate;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPoll;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationVote;
import de.evia.travelmate.trips.domain.accommodationpoll.Amenity;

public record AccommodationPollRepresentation(
    UUID accommodationPollId,
    UUID tenantId,
    UUID tripId,
    String status,
    UUID selectedCandidateId,
    UUID lastFailedCandidateId,
    String lastFailedCandidateNote,
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
            poll.lastFailedCandidateId() != null ? poll.lastFailedCandidateId().value() : null,
            poll.lastFailedCandidateNote(),
            poll.candidates().stream().map(c -> new CandidateRepresentation(c, poll)).toList(),
            poll.votes().stream().map(VoteRepresentation::new).toList()
        );
    }

    public record CandidateRepresentation(
        UUID candidateId,
        String name,
        String url,
        String address,
        String description,
        long voteCount,
        List<RoomRepresentation> rooms,
        Set<Amenity> amenities
    ) {
        public CandidateRepresentation(final AccommodationCandidate candidate, final AccommodationPoll poll) {
            this(
                candidate.candidateId().value(),
                candidate.name(),
                candidate.url(),
                candidate.address(),
                candidate.description(),
                poll.voteCountForCandidate(candidate.candidateId()),
                candidate.rooms().stream()
                    .map(r -> new RoomRepresentation(r.name(), r.bedCount(), r.pricePerNight(), r.bedDescription()))
                    .toList(),
                candidate.amenities()
            );
        }
    }

    public record RoomRepresentation(String name, int bedCount, BigDecimal pricePerNight, String bedDescription) {
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

    public CandidateRepresentation selectedCandidate() {
        if (selectedCandidateId() == null) {
            return null;
        }
        return candidates().stream()
            .filter(candidate -> selectedCandidateId().equals(candidate.candidateId()))
            .findFirst()
            .orElse(null);
    }

    public CandidateRepresentation leadingCandidate() {
        return candidates().stream()
            .max((left, right) -> Long.compare(left.voteCount(), right.voteCount()))
            .orElse(null);
    }

    public CandidateRepresentation lastFailedCandidate() {
        if (lastFailedCandidateId() == null) {
            return null;
        }
        return candidates().stream()
            .filter(candidate -> lastFailedCandidateId().equals(candidate.candidateId()))
            .findFirst()
            .orElse(null);
    }

    public String leadingCandidateName() {
        final CandidateRepresentation leading = leadingCandidate();
        return leading != null ? leading.name() : null;
    }
}
