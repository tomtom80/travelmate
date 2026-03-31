package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

public class AccommodationPoll extends AggregateRoot {

    private final AccommodationPollId accommodationPollId;
    private final TenantId tenantId;
    private final TripId tripId;
    private final List<AccommodationCandidate> candidates;
    private final List<AccommodationVote> votes;
    private AccommodationPollStatus status;
    private AccommodationCandidateId selectedCandidateId;

    public AccommodationPoll(final AccommodationPollId accommodationPollId,
                             final TenantId tenantId,
                             final TripId tripId,
                             final AccommodationPollStatus status,
                             final List<AccommodationCandidate> candidates,
                             final List<AccommodationVote> votes,
                             final AccommodationCandidateId selectedCandidateId) {
        argumentIsNotNull(accommodationPollId, "accommodationPollId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(status, "status");
        argumentIsNotNull(candidates, "candidates");
        argumentIsNotNull(votes, "votes");
        this.accommodationPollId = accommodationPollId;
        this.tenantId = tenantId;
        this.tripId = tripId;
        this.status = status;
        this.candidates = new ArrayList<>(candidates);
        this.votes = new ArrayList<>(votes);
        this.selectedCandidateId = selectedCandidateId;
    }

    public static AccommodationPoll create(final TenantId tenantId,
                                           final TripId tripId,
                                           final List<CandidateProposal> proposals) {
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(tripId, "tripId");
        argumentIsNotNull(proposals, "proposals");
        argumentIsTrue(proposals.size() >= 2,
            "An accommodation poll requires at least 2 candidates.");

        final List<AccommodationCandidate> candidates = proposals.stream()
            .map(p -> new AccommodationCandidate(p.name(), p.url(), p.description(), p.rooms()))
            .toList();

        return new AccommodationPoll(
            new AccommodationPollId(UUID.randomUUID()),
            tenantId, tripId,
            AccommodationPollStatus.OPEN,
            candidates,
            List.of(),
            null
        );
    }

    public AccommodationCandidateId addCandidate(final String name, final String url,
                                                 final String description, final List<CandidateRoom> rooms) {
        assertOpen();
        final AccommodationCandidate candidate = new AccommodationCandidate(
            name, url, description, rooms != null ? rooms : List.of()
        );
        candidates.add(candidate);
        return candidate.candidateId();
    }

    public void removeCandidate(final AccommodationCandidateId candidateId) {
        assertOpen();
        argumentIsNotNull(candidateId, "candidateId");
        final AccommodationCandidate candidate = findCandidate(candidateId);
        final boolean hasVotes = votes.stream()
            .anyMatch(v -> v.selectedCandidateId().equals(candidateId));
        if (hasVotes) {
            throw new IllegalArgumentException(
                "Cannot remove candidate " + candidateId.value()
                    + " because it has votes. Remove votes first or cancel the poll.");
        }
        candidates.remove(candidate);
    }

    public AccommodationVoteId castVote(final UUID voterId, final AccommodationCandidateId candidateId) {
        assertOpen();
        argumentIsNotNull(voterId, "voterId");
        argumentIsNotNull(candidateId, "candidateId");
        findCandidate(candidateId);

        if (findVoteByVoter(voterId) != null) {
            throw new IllegalArgumentException(
                "Voter " + voterId + " has already voted. Use changeVote() instead.");
        }

        final AccommodationVote vote = new AccommodationVote(
            new AccommodationVoteId(UUID.randomUUID()),
            voterId,
            candidateId
        );
        votes.add(vote);
        return vote.voteId();
    }

    public void changeVote(final UUID voterId, final AccommodationCandidateId newCandidateId) {
        assertOpen();
        argumentIsNotNull(voterId, "voterId");
        argumentIsNotNull(newCandidateId, "newCandidateId");
        findCandidate(newCandidateId);

        final AccommodationVote vote = findVoteByVoter(voterId);
        if (vote == null) {
            throw new IllegalArgumentException(
                "Voter " + voterId + " has not voted yet. Use castVote() first.");
        }
        vote.changeSelection(newCandidateId);
    }

    public void confirm(final AccommodationCandidateId candidateId) {
        assertOpen();
        argumentIsNotNull(candidateId, "candidateId");
        findCandidate(candidateId);
        this.selectedCandidateId = candidateId;
        this.status = AccommodationPollStatus.CONFIRMED;
    }

    public void cancel() {
        assertOpen();
        this.status = AccommodationPollStatus.CANCELLED;
    }

    public long voteCountForCandidate(final AccommodationCandidateId candidateId) {
        return votes.stream()
            .filter(v -> v.selectedCandidateId().equals(candidateId))
            .count();
    }

    private AccommodationCandidate findCandidate(final AccommodationCandidateId candidateId) {
        return candidates.stream()
            .filter(c -> c.candidateId().equals(candidateId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Accommodation candidate " + candidateId.value() + " not found in this poll."));
    }

    private AccommodationVote findVoteByVoter(final UUID voterId) {
        return votes.stream()
            .filter(v -> v.voterId().equals(voterId))
            .findFirst()
            .orElse(null);
    }

    private void assertOpen() {
        if (this.status != AccommodationPollStatus.OPEN) {
            throw new IllegalStateException(
                "Accommodation poll is " + this.status + ", expected OPEN.");
        }
    }

    public AccommodationPollId accommodationPollId() {
        return accommodationPollId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public TripId tripId() {
        return tripId;
    }

    public AccommodationPollStatus status() {
        return status;
    }

    public List<AccommodationCandidate> candidates() {
        return Collections.unmodifiableList(candidates);
    }

    public List<AccommodationVote> votes() {
        return Collections.unmodifiableList(votes);
    }

    public AccommodationCandidateId selectedCandidateId() {
        return selectedCandidateId;
    }
}
