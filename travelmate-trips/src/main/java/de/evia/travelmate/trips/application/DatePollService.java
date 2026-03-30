package de.evia.travelmate.trips.application;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AddDateOptionCommand;
import de.evia.travelmate.trips.application.command.CastDateVoteCommand;
import de.evia.travelmate.trips.application.command.ConfirmDatePollCommand;
import de.evia.travelmate.trips.application.command.CreateDatePollCommand;
import de.evia.travelmate.trips.application.command.RemoveDateOptionCommand;
import de.evia.travelmate.trips.application.representation.DatePollRepresentation;
import de.evia.travelmate.trips.domain.datepoll.DateOptionId;
import de.evia.travelmate.trips.domain.datepoll.DatePoll;
import de.evia.travelmate.trips.domain.datepoll.DatePollId;
import de.evia.travelmate.trips.domain.datepoll.DatePollRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

@Service
@Transactional
public class DatePollService {

    private final DatePollRepository datePollRepository;
    private final TripService tripService;

    public DatePollService(final DatePollRepository datePollRepository,
                           final TripService tripService) {
        this.datePollRepository = datePollRepository;
        this.tripService = tripService;
    }

    public DatePollRepresentation createDatePoll(final CreateDatePollCommand command) {
        final TenantId tenantId = new TenantId(command.tenantId());
        final TripId tripId = new TripId(command.tripId());

        datePollRepository.findOpenByTripId(tenantId, tripId).ifPresent(existing -> {
            throw new IllegalStateException(
                "An open date poll already exists for trip " + command.tripId() + ".");
        });

        final List<DateRange> dateRanges = command.dateRanges().stream()
            .map(dr -> new DateRange(dr.startDate(), dr.endDate()))
            .toList();

        final DatePoll poll = DatePoll.create(tenantId, tripId, dateRanges);
        datePollRepository.save(poll);
        return new DatePollRepresentation(poll);
    }

    public DatePollRepresentation addOption(final AddDateOptionCommand command) {
        final DatePoll poll = findPoll(new TenantId(command.tenantId()), new DatePollId(command.datePollId()));
        poll.addOption(new DateRange(command.startDate(), command.endDate()));
        datePollRepository.save(poll);
        return new DatePollRepresentation(poll);
    }

    public DatePollRepresentation removeOption(final RemoveDateOptionCommand command) {
        final DatePoll poll = findPoll(new TenantId(command.tenantId()), new DatePollId(command.datePollId()));
        poll.removeOption(new DateOptionId(command.dateOptionId()));
        datePollRepository.save(poll);
        return new DatePollRepresentation(poll);
    }

    public DatePollRepresentation castVote(final CastDateVoteCommand command) {
        final DatePoll poll = findPoll(new TenantId(command.tenantId()), new DatePollId(command.datePollId()));
        final Set<DateOptionId> selectedOptionIds = command.selectedOptionIds().stream()
            .map(DateOptionId::new)
            .collect(Collectors.toSet());

        final boolean alreadyVoted = poll.votes().stream()
            .anyMatch(v -> v.voterId().equals(command.voterId()));

        if (alreadyVoted) {
            poll.changeVote(command.voterId(), selectedOptionIds);
        } else {
            poll.castVote(command.voterId(), selectedOptionIds);
        }

        datePollRepository.save(poll);
        return new DatePollRepresentation(poll);
    }

    public DatePollRepresentation confirmPoll(final ConfirmDatePollCommand command) {
        final DatePoll poll = findPoll(new TenantId(command.tenantId()), new DatePollId(command.datePollId()));
        final DateRange confirmedDateRange = poll.confirm(new DateOptionId(command.confirmedOptionId()));
        datePollRepository.save(poll);
        tripService.updateDateRange(poll.tripId(), confirmedDateRange);
        return new DatePollRepresentation(poll);
    }

    public void cancelPoll(final TenantId tenantId, final DatePollId datePollId) {
        final DatePoll poll = findPoll(tenantId, datePollId);
        poll.cancel();
        datePollRepository.save(poll);
    }

    @Transactional(readOnly = true)
    public boolean existsOpenByTripId(final TenantId tenantId, final TripId tripId) {
        return datePollRepository.findOpenByTripId(tenantId, tripId).isPresent();
    }

    @Transactional(readOnly = true)
    public DatePollRepresentation findByTripId(final TenantId tenantId, final TripId tripId) {
        final DatePoll poll = datePollRepository.findOpenByTripId(tenantId, tripId)
            .orElseThrow(() -> new EntityNotFoundException("DatePoll", "trip " + tripId.value()));
        return new DatePollRepresentation(poll);
    }

    @Transactional(readOnly = true)
    public DatePollRepresentation findLatestByTripId(final TenantId tenantId, final TripId tripId) {
        final DatePoll poll = datePollRepository.findLatestByTripId(tenantId, tripId)
            .orElseThrow(() -> new EntityNotFoundException("DatePoll", "trip " + tripId.value()));
        return new DatePollRepresentation(poll);
    }

    @Transactional(readOnly = true)
    public DatePollRepresentation findById(final TenantId tenantId, final DatePollId datePollId) {
        return new DatePollRepresentation(findPoll(tenantId, datePollId));
    }

    private DatePoll findPoll(final TenantId tenantId, final DatePollId datePollId) {
        return datePollRepository.findById(tenantId, datePollId)
            .orElseThrow(() -> new EntityNotFoundException("DatePoll", datePollId.value().toString()));
    }
}
