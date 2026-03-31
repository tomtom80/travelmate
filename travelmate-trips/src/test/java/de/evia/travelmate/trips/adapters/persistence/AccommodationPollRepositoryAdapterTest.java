package de.evia.travelmate.trips.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPoll;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollRepository;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollStatus;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateProposal;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateRoom;
import de.evia.travelmate.trips.domain.trip.TripId;

@SpringBootTest
@ActiveProfiles("test")
class AccommodationPollRepositoryAdapterTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());

    @Autowired
    private AccommodationPollRepository repository;

    @Test
    void savesAndFindsPollById() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final AccommodationPoll poll = createPoll(tripId);

        repository.save(poll);

        final Optional<AccommodationPoll> found = repository.findById(TENANT_ID, poll.accommodationPollId());
        assertThat(found).isPresent();
        assertThat(found.get().accommodationPollId()).isEqualTo(poll.accommodationPollId());
        assertThat(found.get().candidates()).hasSize(2);
        assertThat(found.get().candidates().getFirst().name()).isEqualTo("Hotel A");
        assertThat(found.get().candidates().getFirst().url()).isEqualTo("https://a.com");
        assertThat(found.get().votes()).isEmpty();
    }

    @Test
    void findsOpenPollByTripId() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final AccommodationPoll poll = createPoll(tripId);
        repository.save(poll);

        final Optional<AccommodationPoll> found = repository.findOpenByTripId(TENANT_ID, tripId);
        assertThat(found).isPresent();
    }

    @Test
    void findOpenByTripIdIgnoresConfirmedPolls() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final AccommodationPoll poll = createPoll(tripId);
        poll.confirm(poll.candidates().getFirst().candidateId());
        repository.save(poll);

        final Optional<AccommodationPoll> found = repository.findOpenByTripId(TENANT_ID, tripId);
        assertThat(found).isEmpty();
    }

    @Test
    void findByIdReturnsEmptyForDifferentTenant() {
        final AccommodationPoll poll = createPoll(new TripId(UUID.randomUUID()));
        repository.save(poll);

        final TenantId otherTenant = new TenantId(UUID.randomUUID());
        assertThat(repository.findById(otherTenant, poll.accommodationPollId())).isEmpty();
    }

    @Test
    void savesAndReloadsVotes() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final AccommodationPoll poll = createPoll(tripId);
        final UUID voterA = UUID.randomUUID();
        poll.castVote(voterA, poll.candidates().getFirst().candidateId());
        repository.save(poll);

        final AccommodationPoll reloaded = repository.findById(TENANT_ID, poll.accommodationPollId()).orElseThrow();
        assertThat(reloaded.votes()).hasSize(1);
        assertThat(reloaded.votes().getFirst().voterId()).isEqualTo(voterA);
    }

    @Test
    void updatesVoteSelection() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final AccommodationPoll poll = createPoll(tripId);
        final UUID voterA = UUID.randomUUID();
        poll.castVote(voterA, poll.candidates().get(0).candidateId());
        repository.save(poll);

        final AccommodationPoll loaded = repository.findById(TENANT_ID, poll.accommodationPollId()).orElseThrow();
        loaded.changeVote(voterA, loaded.candidates().get(1).candidateId());
        repository.save(loaded);

        final AccommodationPoll reloaded = repository.findById(TENANT_ID, poll.accommodationPollId()).orElseThrow();
        assertThat(reloaded.votes().getFirst().selectedCandidateId())
            .isEqualTo(reloaded.candidates().get(1).candidateId());
    }

    @Test
    void savesConfirmedStatus() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final AccommodationPoll poll = createPoll(tripId);
        poll.confirm(poll.candidates().getFirst().candidateId());
        repository.save(poll);

        final AccommodationPoll reloaded = repository.findById(TENANT_ID, poll.accommodationPollId()).orElseThrow();
        assertThat(reloaded.status()).isEqualTo(AccommodationPollStatus.CONFIRMED);
        assertThat(reloaded.selectedCandidateId()).isEqualTo(poll.candidates().getFirst().candidateId());
    }

    @Test
    void addCandidatePersistsNewCandidate() {
        final TripId tripId = new TripId(UUID.randomUUID());
        final AccommodationPoll poll = createPoll(tripId);
        repository.save(poll);

        final AccommodationPoll loaded = repository.findById(TENANT_ID, poll.accommodationPollId()).orElseThrow();
        loaded.addCandidate("Hotel C", "https://c.com", "Great pool", candidateRooms("Poolside"));
        repository.save(loaded);

        final AccommodationPoll reloaded = repository.findById(TENANT_ID, poll.accommodationPollId()).orElseThrow();
        assertThat(reloaded.candidates()).hasSize(3);
    }

    @Test
    void deletePollRemovesFromDatabase() {
        final AccommodationPoll poll = createPoll(new TripId(UUID.randomUUID()));
        repository.save(poll);

        repository.delete(poll);

        assertThat(repository.findById(TENANT_ID, poll.accommodationPollId())).isEmpty();
    }

    private AccommodationPoll createPoll(final TripId tripId) {
        return AccommodationPoll.create(TENANT_ID, tripId, List.of(
            new CandidateProposal("Hotel A", "https://a.com", "Nice view", candidateRooms("Balcony")),
            new CandidateProposal("Hotel B", null, "Cozy cabin", candidateRooms("Fireplace"))
        ));
    }

    private static List<CandidateRoom> candidateRooms(final String features) {
        return List.of(new CandidateRoom("Room", 2, null, features));
    }
}
