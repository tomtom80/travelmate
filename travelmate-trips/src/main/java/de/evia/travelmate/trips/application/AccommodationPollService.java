package de.evia.travelmate.trips.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AddAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.command.CastAccommodationVoteCommand;
import de.evia.travelmate.trips.application.command.ConfirmAccommodationPollCommand;
import de.evia.travelmate.trips.application.command.CreateAccommodationPollCommand;
import de.evia.travelmate.trips.application.command.RemoveAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.representation.AccommodationPollRepresentation;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidateId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPoll;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollRepository;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateProposal;
import de.evia.travelmate.trips.domain.trip.TripId;

@Service
@Transactional
public class AccommodationPollService {

    private final AccommodationPollRepository accommodationPollRepository;

    public AccommodationPollService(final AccommodationPollRepository accommodationPollRepository) {
        this.accommodationPollRepository = accommodationPollRepository;
    }

    public AccommodationPollRepresentation createPoll(final CreateAccommodationPollCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());

        accommodationPollRepository.findOpenByTripId(tenantId, tripId).ifPresent(existing -> {
            throw new IllegalStateException(
                "An open accommodation poll already exists for trip " + command.tripId() + ".");
        });

        final List<CandidateProposal> proposals = command.candidates().stream()
            .map(c -> new CandidateProposal(c.name(), c.url(), c.description()))
            .toList();

        final AccommodationPoll poll = AccommodationPoll.create(tenantId, tripId, proposals);
        accommodationPollRepository.save(poll);
        return new AccommodationPollRepresentation(poll);
    }

    public AccommodationPollRepresentation addCandidate(final AddAccommodationCandidateCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));
        poll.addCandidate(command.name(), command.url(), command.description());
        accommodationPollRepository.save(poll);
        return new AccommodationPollRepresentation(poll);
    }

    public AccommodationPollRepresentation removeCandidate(final RemoveAccommodationCandidateCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));
        poll.removeCandidate(new AccommodationCandidateId(command.candidateId()));
        accommodationPollRepository.save(poll);
        return new AccommodationPollRepresentation(poll);
    }

    public AccommodationPollRepresentation castVote(final CastAccommodationVoteCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));

        final boolean alreadyVoted = poll.votes().stream()
            .anyMatch(v -> v.voterId().equals(command.voterId()));

        if (alreadyVoted) {
            poll.changeVote(command.voterId(), new AccommodationCandidateId(command.selectedCandidateId()));
        } else {
            poll.castVote(command.voterId(), new AccommodationCandidateId(command.selectedCandidateId()));
        }

        accommodationPollRepository.save(poll);
        return new AccommodationPollRepresentation(poll);
    }

    public AccommodationPollRepresentation confirmPoll(final ConfirmAccommodationPollCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));
        poll.confirm(new AccommodationCandidateId(command.confirmedCandidateId()));
        accommodationPollRepository.save(poll);
        return new AccommodationPollRepresentation(poll);
    }

    public void cancelPoll(final TenantId tenantId, final AccommodationPollId pollId) {
        final AccommodationPoll poll = findPoll(tenantId, pollId);
        poll.cancel();
        accommodationPollRepository.save(poll);
    }

    @Transactional(readOnly = true)
    public boolean existsOpenByTripId(final TenantId tenantId, final TripId tripId) {
        return accommodationPollRepository.findOpenByTripId(tenantId, tripId).isPresent();
    }

    @Transactional(readOnly = true)
    public AccommodationPollRepresentation findByTripId(final TenantId tenantId, final TripId tripId) {
        final AccommodationPoll poll = accommodationPollRepository.findOpenByTripId(tenantId, tripId)
            .orElseThrow(() -> new EntityNotFoundException("AccommodationPoll", "trip " + tripId.value()));
        return new AccommodationPollRepresentation(poll);
    }

    @Transactional(readOnly = true)
    public AccommodationPollRepresentation findLatestByTripId(final TenantId tenantId, final TripId tripId) {
        final AccommodationPoll poll = accommodationPollRepository.findLatestByTripId(tenantId, tripId)
            .orElseThrow(() -> new EntityNotFoundException("AccommodationPoll", "trip " + tripId.value()));
        return new AccommodationPollRepresentation(poll);
    }

    @Transactional(readOnly = true)
    public AccommodationPollRepresentation findById(final TenantId tenantId, final AccommodationPollId pollId) {
        return new AccommodationPollRepresentation(findPoll(tenantId, pollId));
    }

    private AccommodationPoll findPoll(final TenantId tenantId, final AccommodationPollId pollId) {
        return accommodationPollRepository.findById(tenantId, pollId)
            .orElseThrow(() -> new EntityNotFoundException("AccommodationPoll", pollId.value().toString()));
    }
}
