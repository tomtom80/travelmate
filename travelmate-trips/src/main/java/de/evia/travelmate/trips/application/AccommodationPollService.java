package de.evia.travelmate.trips.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AddAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.command.CastAccommodationVoteCommand;
import de.evia.travelmate.trips.application.command.CreateAccommodationPollCommand;
import de.evia.travelmate.trips.application.command.RecordAccommodationBookingFailureCommand;
import de.evia.travelmate.trips.application.command.RecordAccommodationBookingSuccessCommand;
import de.evia.travelmate.trips.application.command.RemoveAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.command.SelectAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.command.SetAccommodationCommand;
import de.evia.travelmate.trips.application.representation.AccommodationPollRepresentation;
import de.evia.travelmate.trips.application.command.RoomCommand;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidate;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidateId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPoll;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollRepository;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateProposal;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateRoom;
import de.evia.travelmate.trips.domain.trip.TripId;

@Service
@Transactional
public class AccommodationPollService {

    private final AccommodationPollRepository accommodationPollRepository;
    private final AccommodationService accommodationService;

    public AccommodationPollService(final AccommodationPollRepository accommodationPollRepository,
                                    final AccommodationService accommodationService) {
        this.accommodationPollRepository = accommodationPollRepository;
        this.accommodationService = accommodationService;
    }

    public AccommodationPollRepresentation createPoll(final CreateAccommodationPollCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());

        accommodationPollRepository.findOpenByTripId(tenantId, tripId).ifPresent(existing -> {
            throw new IllegalStateException(
                "An open accommodation poll already exists for trip " + command.tripId() + ".");
        });

        final List<CandidateProposal> proposals = command.candidates().stream()
            .map(c -> new CandidateProposal(
                c.name(), c.url(), c.address(), c.description(),
                mapRooms(c.rooms()), c.amenities()
            ))
            .toList();

        final AccommodationPoll poll = AccommodationPoll.create(tenantId, tripId, proposals);
        accommodationPollRepository.save(poll);
        return new AccommodationPollRepresentation(poll);
    }

    public AccommodationPollRepresentation addCandidate(final AddAccommodationCandidateCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));
        poll.addCandidate(command.name(), command.url(), command.address(), command.description(), mapRooms(command.rooms()), command.amenities());
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

    public AccommodationPollRepresentation selectCandidate(final SelectAccommodationCandidateCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));
        poll.select(new AccommodationCandidateId(command.candidateId()));
        accommodationPollRepository.save(poll);
        return new AccommodationPollRepresentation(poll);
    }

    public AccommodationPollRepresentation recordBookingSuccess(final RecordAccommodationBookingSuccessCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));
        poll.recordBookingSuccess();
        accommodationPollRepository.save(poll);
        applyCandidateAsAccommodation(
            poll, new TenantId(command.tenantId()), new TripId(command.tripId()));
        return new AccommodationPollRepresentation(poll);
    }

    public AccommodationPollRepresentation recordBookingFailure(final RecordAccommodationBookingFailureCommand command) {
        final AccommodationPoll poll = findPoll(
            new TenantId(command.tenantId()), new AccommodationPollId(command.accommodationPollId()));
        poll.recordBookingFailure(command.note());
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

    private void applyCandidateAsAccommodation(final AccommodationPoll poll,
                                               final TenantId tenantId,
                                               final TripId tripId) {
        if (poll.selectedCandidateId() == null) {
            return;
        }
        final AccommodationCandidate winner = poll.candidates().stream()
            .filter(c -> c.candidateId().equals(poll.selectedCandidateId()))
            .findFirst()
            .orElse(null);
        if (winner == null) {
            return;
        }
        final List<RoomCommand> rooms = winner.rooms().stream()
            .map(room -> new RoomCommand(room.name(), room.bedCount(), room.pricePerNight()))
            .toList();
        final SetAccommodationCommand command = new SetAccommodationCommand(
            tenantId.value(), tripId.value(),
            winner.name(),
            winner.address(),
            winner.url(),
            null, null, null,
            rooms
        );
        accommodationService.setAccommodation(command);
    }

    private List<CandidateRoom> mapRooms(
        final List<CreateAccommodationPollCommand.RoomProposalCommand> commands
    ) {
        if (commands == null || commands.isEmpty()) {
            throw new BusinessRuleViolationException("accommodationpoll.error.roomRequired");
        }
        return commands.stream()
            .map(this::toCandidateRoom)
            .toList();
    }

    private CandidateRoom toCandidateRoom(final CreateAccommodationPollCommand.RoomProposalCommand command) {
        if (command == null) {
            throw new BusinessRuleViolationException("accommodationpoll.error.roomRequired");
        }
        return new CandidateRoom(command.name(), command.bedCount(), command.pricePerNight(),
            command.bedDescription() != null && !command.bedDescription().isBlank()
                ? command.bedDescription().trim() : null);
    }
}
