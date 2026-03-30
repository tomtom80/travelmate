package de.evia.travelmate.trips.adapters.persistence;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.datepoll.DateOption;
import de.evia.travelmate.trips.domain.datepoll.DateOptionId;
import de.evia.travelmate.trips.domain.datepoll.DatePoll;
import de.evia.travelmate.trips.domain.datepoll.DatePollId;
import de.evia.travelmate.trips.domain.datepoll.DatePollRepository;
import de.evia.travelmate.trips.domain.datepoll.DateVote;
import de.evia.travelmate.trips.domain.datepoll.DateVoteId;
import de.evia.travelmate.trips.domain.datepoll.PollStatus;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

@Repository
public class DatePollRepositoryAdapter implements DatePollRepository {

    private final DatePollJpaRepository jpaRepository;

    public DatePollRepositoryAdapter(final DatePollJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DatePoll save(final DatePoll poll) {
        final DatePollJpaEntity entity = jpaRepository.findById(poll.datePollId().value())
            .orElseGet(() -> new DatePollJpaEntity(
                poll.datePollId().value(),
                poll.tenantId().value(),
                poll.tripId().value(),
                poll.status().name()
            ));
        entity.setStatus(poll.status().name());
        entity.setConfirmedOptionId(
            poll.confirmedOptionId() != null ? poll.confirmedOptionId().value() : null
        );
        syncOptions(entity, poll);
        syncVotes(entity, poll);
        jpaRepository.save(entity);
        return poll;
    }

    @Override
    public Optional<DatePoll> findById(final TenantId tenantId, final DatePollId datePollId) {
        return jpaRepository.findByTenantIdAndDatePollId(tenantId.value(), datePollId.value())
            .map(this::toDomain);
    }

    @Override
    public Optional<DatePoll> findOpenByTripId(final TenantId tenantId, final TripId tripId) {
        return jpaRepository.findByTenantIdAndTripId(tenantId.value(), tripId.value()).stream()
            .filter(e -> PollStatus.OPEN.name().equals(e.getStatus()))
            .findFirst()
            .map(this::toDomain);
    }

    @Override
    public Optional<DatePoll> findLatestByTripId(final TenantId tenantId, final TripId tripId) {
        return jpaRepository.findByTenantIdAndTripId(tenantId.value(), tripId.value()).stream()
            .max(Comparator.comparing(DatePollJpaEntity::getDatePollId))
            .map(this::toDomain);
    }

    @Override
    public void delete(final DatePoll poll) {
        jpaRepository.deleteById(poll.datePollId().value());
    }

    private void syncOptions(final DatePollJpaEntity entity, final DatePoll poll) {
        final Set<UUID> currentOptionIds = poll.options().stream()
            .map(o -> o.dateOptionId().value())
            .collect(Collectors.toSet());

        entity.getOptions().removeIf(e -> !currentOptionIds.contains(e.getDateOptionId()));

        for (final DateOption option : poll.options()) {
            final boolean exists = entity.getOptions().stream()
                .anyMatch(e -> e.getDateOptionId().equals(option.dateOptionId().value()));
            if (!exists) {
                entity.getOptions().add(new DateOptionJpaEntity(
                    option.dateOptionId().value(), entity,
                    option.dateRange().startDate(), option.dateRange().endDate()
                ));
            }
        }
    }

    private void syncVotes(final DatePollJpaEntity entity, final DatePoll poll) {
        final Set<UUID> currentVoteIds = poll.votes().stream()
            .map(v -> v.dateVoteId().value())
            .collect(Collectors.toSet());

        entity.getVotes().removeIf(e -> !currentVoteIds.contains(e.getDateVoteId()));

        for (final DateVote vote : poll.votes()) {
            final Set<UUID> selectedIds = vote.selectedOptionIds().stream()
                .map(DateOptionId::value)
                .collect(Collectors.toSet());

            final Optional<DateVoteJpaEntity> existing = entity.getVotes().stream()
                .filter(e -> e.getDateVoteId().equals(vote.dateVoteId().value()))
                .findFirst();

            if (existing.isPresent()) {
                existing.get().setSelectedOptionIds(selectedIds);
            } else {
                entity.getVotes().add(new DateVoteJpaEntity(
                    vote.dateVoteId().value(), entity,
                    vote.voterId(), selectedIds
                ));
            }
        }
    }

    private DatePoll toDomain(final DatePollJpaEntity entity) {
        final var options = entity.getOptions().stream()
            .map(o -> new DateOption(
                new DateOptionId(o.getDateOptionId()),
                new DateRange(o.getStartDate(), o.getEndDate())
            ))
            .toList();

        final var votes = entity.getVotes().stream()
            .map(v -> new DateVote(
                new DateVoteId(v.getDateVoteId()),
                v.getVoterId(),
                v.getSelectedOptionIds().stream()
                    .map(DateOptionId::new)
                    .collect(Collectors.toSet())
            ))
            .toList();

        return new DatePoll(
            new DatePollId(entity.getDatePollId()),
            new TenantId(entity.getTenantId()),
            new TripId(entity.getTripId()),
            PollStatus.valueOf(entity.getStatus()),
            options,
            votes,
            entity.getConfirmedOptionId() != null
                ? new DateOptionId(entity.getConfirmedOptionId())
                : null
        );
    }
}
