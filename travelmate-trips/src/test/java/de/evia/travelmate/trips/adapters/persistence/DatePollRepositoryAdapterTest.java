package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.datepoll.DateOptionId;
import de.evia.travelmate.trips.domain.datepoll.DatePoll;
import de.evia.travelmate.trips.domain.datepoll.DatePollId;
import de.evia.travelmate.trips.domain.datepoll.DatePollRepository;
import de.evia.travelmate.trips.domain.datepoll.PollStatus;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@ActiveProfiles("test")
class DatePollRepositoryAdapterTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());
    private static final DateRange JULY = new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14));
    private static final DateRange AUGUST = new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14));

    @Autowired
    private DatePollRepository repository;

    @Test
    void savesAndFindsDatePollById() {
        final DatePoll poll = DatePoll.create(TENANT_ID, TRIP_ID, List.of(JULY, AUGUST));

        repository.save(poll);

        final Optional<DatePoll> found = repository.findById(TENANT_ID, poll.datePollId());
        assertThat(found).isPresent();
        assertThat(found.get().datePollId()).isEqualTo(poll.datePollId());
        assertThat(found.get().tenantId()).isEqualTo(TENANT_ID);
        assertThat(found.get().tripId()).isEqualTo(TRIP_ID);
        assertThat(found.get().status()).isEqualTo(PollStatus.OPEN);
        assertThat(found.get().options()).hasSize(2);
        assertThat(found.get().votes()).isEmpty();
    }

    @Test
    void findsOpenPollByTripId() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final DatePoll poll = DatePoll.create(TENANT_ID, tripId, List.of(JULY, AUGUST));
        repository.save(poll);

        final Optional<DatePoll> found = repository.findOpenByTripId(TENANT_ID, tripId);
        assertThat(found).isPresent();
        assertThat(found.get().datePollId()).isEqualTo(poll.datePollId());
    }

    @Test
    void findOpenByTripIdIgnoresConfirmedPolls() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final DatePoll poll = DatePoll.create(TENANT_ID, tripId, List.of(JULY, AUGUST));
        poll.confirm(poll.options().getFirst().dateOptionId());
        repository.save(poll);

        final Optional<DatePoll> found = repository.findOpenByTripId(TENANT_ID, tripId);
        assertThat(found).isEmpty();
    }

    @Test
    void findByIdReturnsEmptyForDifferentTenant() {
        final DatePoll poll = DatePoll.create(TENANT_ID, TRIP_ID, List.of(JULY, AUGUST));
        repository.save(poll);

        final TenantId otherTenant = new TenantId(UUID.randomUUID());
        final Optional<DatePoll> found = repository.findById(otherTenant, poll.datePollId());
        assertThat(found).isEmpty();
    }

    @Test
    void savesAndReloadsVotes() {
        final DatePoll poll = DatePoll.create(TENANT_ID, new TripId(UUID.randomUUID()), List.of(JULY, AUGUST));
        final DateOptionId option1 = poll.options().get(0).dateOptionId();
        final DateOptionId option2 = poll.options().get(1).dateOptionId();
        final UUID voterA = UUID.randomUUID();
        poll.castVote(voterA, Set.of(option1, option2));
        repository.save(poll);

        final DatePoll reloaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        assertThat(reloaded.votes()).hasSize(1);
        assertThat(reloaded.votes().getFirst().voterId()).isEqualTo(voterA);
        assertThat(reloaded.votes().getFirst().selectedOptionIds()).hasSize(2);
    }

    @Test
    void updatesVoteSelection() {
        final DatePoll poll = DatePoll.create(TENANT_ID, new TripId(UUID.randomUUID()), List.of(JULY, AUGUST));
        final DateOptionId option1 = poll.options().get(0).dateOptionId();
        final DateOptionId option2 = poll.options().get(1).dateOptionId();
        final UUID voterA = UUID.randomUUID();
        poll.castVote(voterA, Set.of(option1));
        repository.save(poll);

        final DatePoll loaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        loaded.changeVote(voterA, Set.of(option2));
        repository.save(loaded);

        final DatePoll reloaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        assertThat(reloaded.votes().getFirst().selectedOptionIds())
            .containsExactly(new DateOptionId(option2.value()));
    }

    @Test
    void savesConfirmedStatus() {
        final DatePoll poll = DatePoll.create(TENANT_ID, new TripId(UUID.randomUUID()), List.of(JULY, AUGUST));
        final DateOptionId optionId = poll.options().getFirst().dateOptionId();
        poll.confirm(optionId);
        repository.save(poll);

        final DatePoll reloaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(PollStatus.CONFIRMED);
        assertThat(reloaded.confirmedOptionId()).isEqualTo(optionId);
    }

    @Test
    void addOptionPersistsNewOption() {
        final DatePoll poll = DatePoll.create(TENANT_ID, new TripId(UUID.randomUUID()), List.of(JULY, AUGUST));
        repository.save(poll);

        final DatePoll loaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        loaded.addOption(new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)));
        repository.save(loaded);

        final DatePoll reloaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        assertThat(reloaded.options()).hasSize(3);
    }

    @Test
    void removeOptionPersistsRemoval() {
        final DatePoll poll = DatePoll.create(TENANT_ID, new TripId(UUID.randomUUID()), List.of(JULY, AUGUST));
        repository.save(poll);

        final DatePoll loaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        loaded.removeOption(loaded.options().getLast().dateOptionId());
        repository.save(loaded);

        final DatePoll reloaded = repository.findById(TENANT_ID, poll.datePollId()).orElseThrow();
        assertThat(reloaded.options()).hasSize(1);
    }

    @Test
    void deletePollRemovesFromDatabase() {
        final DatePoll poll = DatePoll.create(TENANT_ID, new TripId(UUID.randomUUID()), List.of(JULY, AUGUST));
        repository.save(poll);

        repository.delete(poll);

        final Optional<DatePoll> found = repository.findById(TENANT_ID, poll.datePollId());
        assertThat(found).isEmpty();
    }
}
