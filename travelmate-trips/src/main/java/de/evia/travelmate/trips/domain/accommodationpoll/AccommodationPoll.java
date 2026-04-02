package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private AccommodationCandidateId lastFailedCandidateId;
    private String lastFailedCandidateNote;

    public AccommodationPoll(final AccommodationPollId accommodationPollId,
                             final TenantId tenantId,
                             final TripId tripId,
                             final AccommodationPollStatus status,
                             final List<AccommodationCandidate> candidates,
                             final List<AccommodationVote> votes,
                             final AccommodationCandidateId selectedCandidateId,
                             final AccommodationCandidateId lastFailedCandidateId,
                             final String lastFailedCandidateNote) {
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
        this.lastFailedCandidateId = lastFailedCandidateId;
        this.lastFailedCandidateNote = lastFailedCandidateNote;
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
            .map(p -> new AccommodationCandidate(p.name(), p.url(), p.address(), p.description(), p.rooms(), p.amenities()))
            .toList();

        return new AccommodationPoll(
            new AccommodationPollId(UUID.randomUUID()),
            tenantId, tripId,
            AccommodationPollStatus.OPEN,
            candidates,
            List.of(),
            null,
            null,
            null
        );
    }

    public AccommodationCandidateId addCandidate(final String name, final String url,
                                                 final String address, final String description,
                                                 final List<CandidateRoom> rooms,
                                                 final Set<Amenity> amenities) {
        assertOpen();
        final AccommodationCandidate candidate = new AccommodationCandidate(
            name, url, address, description, rooms != null ? rooms : List.of(), amenities
        );
        candidates.add(candidate);
        return candidate.candidateId();
    }

    public void editCandidate(final AccommodationCandidateId candidateId,
                              final String name, final String url, final String address,
                              final String description, final List<CandidateRoom> rooms,
                              final Set<Amenity> amenities) {
        assertOpen();
        argumentIsNotNull(candidateId, "candidateId");
        final int index = findCandidateIndex(candidateId);
        final AccommodationCandidate updated = new AccommodationCandidate(
            candidateId, name, url, address, description,
            rooms != null ? rooms : List.of(), amenities
        );
        candidates.set(index, updated);
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

    public void select(final AccommodationCandidateId candidateId) {
        assertOpen();
        argumentIsNotNull(candidateId, "candidateId");
        findCandidate(candidateId);
        this.selectedCandidateId = candidateId;
        this.status = AccommodationPollStatus.AWAITING_BOOKING;
    }

    public void recordBookingSuccess() {
        assertStatus(AccommodationPollStatus.AWAITING_BOOKING);
        this.status = AccommodationPollStatus.BOOKED;
    }

    public void recordBookingFailure(final String note) {
        assertStatus(AccommodationPollStatus.AWAITING_BOOKING);
        this.lastFailedCandidateId = this.selectedCandidateId;
        this.lastFailedCandidateNote = note != null && !note.isBlank() ? note.trim() : null;
        this.selectedCandidateId = null;
        this.status = AccommodationPollStatus.OPEN;
    }

    public void cancel() {
        if (this.status != AccommodationPollStatus.OPEN && this.status != AccommodationPollStatus.AWAITING_BOOKING) {
            throw new IllegalStateException(
                "Accommodation poll is " + this.status + ", expected OPEN or AWAITING_BOOKING.");
        }
        this.status = AccommodationPollStatus.CANCELLED;
    }

    public long voteCountForCandidate(final AccommodationCandidateId candidateId) {
        return votes.stream()
            .filter(v -> v.selectedCandidateId().equals(candidateId))
            .count();
    }

    private int findCandidateIndex(final AccommodationCandidateId candidateId) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).candidateId().equals(candidateId)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
            "Accommodation candidate " + candidateId.value() + " not found in this poll.");
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
        assertStatus(AccommodationPollStatus.OPEN);
    }

    private void assertStatus(final AccommodationPollStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException(
                "Accommodation poll is " + this.status + ", expected " + expectedStatus + ".");
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

    public AccommodationCandidateId lastFailedCandidateId() {
        return lastFailedCandidateId;
    }

    public String lastFailedCandidateNote() {
        return lastFailedCandidateNote;
    }
}
