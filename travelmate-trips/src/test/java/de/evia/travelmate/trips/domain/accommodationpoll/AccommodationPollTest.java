package de.evia.travelmate.trips.domain.accommodationpoll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.TripId;

class AccommodationPollTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());
    private static final UUID VOTER_A = UUID.randomUUID();
    private static final UUID VOTER_B = UUID.randomUUID();

    // --- Creation ---

    @Test
    void createWithTwoCandidatesSucceeds() {
        final AccommodationPoll poll = createOpenPoll();

        assertThat(poll.accommodationPollId()).isNotNull();
        assertThat(poll.tenantId()).isEqualTo(TENANT_ID);
        assertThat(poll.tripId()).isEqualTo(TRIP_ID);
        assertThat(poll.status()).isEqualTo(AccommodationPollStatus.OPEN);
        assertThat(poll.candidates()).hasSize(2);
        assertThat(poll.votes()).isEmpty();
        assertThat(poll.selectedCandidateId()).isNull();
    }

    @Test
    void createWithOneCandidateFails() {
        assertThatThrownBy(() -> AccommodationPoll.create(
            TENANT_ID, TRIP_ID,
            List.of(new CandidateProposal("Hotel A", null, null, null, candidateRooms(), Set.of()))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 2");
    }

    @Test
    void createWithEmptyCandidatesFails() {
        assertThatThrownBy(() -> AccommodationPoll.create(TENANT_ID, TRIP_ID, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 2");
    }

    @Test
    void createPreservesAmenities() {
        final AccommodationPoll poll = AccommodationPoll.create(TENANT_ID, TRIP_ID, List.of(
            new CandidateProposal("Hotel A", null, null, null, candidateRooms(), Set.of(Amenity.WIFI, Amenity.POOL)),
            new CandidateProposal("Hotel B", null, null, null, candidateRooms(), Set.of(Amenity.KITCHEN))
        ));

        assertThat(poll.candidates().get(0).amenities()).containsExactlyInAnyOrder(Amenity.WIFI, Amenity.POOL);
        assertThat(poll.candidates().get(1).amenities()).containsExactly(Amenity.KITCHEN);
    }

    @Test
    void createWithEmptyAmenitiesIsValid() {
        final AccommodationPoll poll = AccommodationPoll.create(TENANT_ID, TRIP_ID, List.of(
            new CandidateProposal("Hotel A", null, null, null, candidateRooms(), Set.of()),
            new CandidateProposal("Hotel B", null, null, null, candidateRooms(), null)
        ));

        assertThat(poll.candidates().get(0).amenities()).isEmpty();
        assertThat(poll.candidates().get(1).amenities()).isEmpty();
    }

    // --- Add/Remove Candidates ---

    @Test
    void addCandidateIncreasesCount() {
        final AccommodationPoll poll = createOpenPoll();

        final AccommodationCandidateId newId = poll.addCandidate("Hotel C", "https://hotelc.com", null, "Nice pool",
            candidateRooms(), Set.of(Amenity.POOL));

        assertThat(newId).isNotNull();
        assertThat(poll.candidates()).hasSize(3);
        assertThat(poll.candidates().get(2).amenities()).containsExactly(Amenity.POOL);
    }

    @Test
    void removeCandidateWithoutVotesSucceeds() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();

        poll.removeCandidate(candidateId);

        assertThat(poll.candidates()).hasSize(1);
    }

    @Test
    void removeCandidateWithVotesFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();
        poll.castVote(VOTER_A, candidateId);

        assertThatThrownBy(() -> poll.removeCandidate(candidateId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("votes");
    }

    @Test
    void removeUnknownCandidateFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId unknownId = new AccommodationCandidateId(UUID.randomUUID());

        assertThatThrownBy(() -> poll.removeCandidate(unknownId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    // --- Voting (single-select) ---

    @Test
    void castVoteSucceeds() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();

        final AccommodationVoteId voteId = poll.castVote(VOTER_A, candidateId);

        assertThat(voteId).isNotNull();
        assertThat(poll.votes()).hasSize(1);
        assertThat(poll.votes().getFirst().voterId()).isEqualTo(VOTER_A);
        assertThat(poll.votes().getFirst().selectedCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void castVoteWithUnknownCandidateFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId unknownId = new AccommodationCandidateId(UUID.randomUUID());

        assertThatThrownBy(() -> poll.castVote(VOTER_A, unknownId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void castVoteTwiceBySameVoterFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();
        poll.castVote(VOTER_A, candidateId);

        assertThatThrownBy(() -> poll.castVote(VOTER_A, candidateId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already voted");
    }

    @Test
    void multipleVotersCanVoteIndependently() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidate1 = poll.candidates().get(0).candidateId();
        final AccommodationCandidateId candidate2 = poll.candidates().get(1).candidateId();

        poll.castVote(VOTER_A, candidate1);
        poll.castVote(VOTER_B, candidate2);

        assertThat(poll.votes()).hasSize(2);
    }

    // --- Change Vote (re-vote) ---

    @Test
    void changeVoteReplacesSelection() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidate1 = poll.candidates().get(0).candidateId();
        final AccommodationCandidateId candidate2 = poll.candidates().get(1).candidateId();
        poll.castVote(VOTER_A, candidate1);

        poll.changeVote(VOTER_A, candidate2);

        assertThat(poll.votes().getFirst().selectedCandidateId()).isEqualTo(candidate2);
    }

    @Test
    void changeVoteWithoutExistingVoteFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();

        assertThatThrownBy(() -> poll.changeVote(VOTER_A, candidateId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has not voted");
    }

    // --- Vote Count ---

    @Test
    void voteCountReflectsSingleSelectVotes() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidate1 = poll.candidates().get(0).candidateId();
        final AccommodationCandidateId candidate2 = poll.candidates().get(1).candidateId();

        poll.castVote(VOTER_A, candidate1);
        poll.castVote(VOTER_B, candidate1);

        assertThat(poll.voteCountForCandidate(candidate1)).isEqualTo(2);
        assertThat(poll.voteCountForCandidate(candidate2)).isEqualTo(0);
    }

    // --- Confirm ---

    @Test
    void selectSetsAwaitingBookingAndSelectedCandidate() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();

        poll.select(candidateId);

        assertThat(poll.status()).isEqualTo(AccommodationPollStatus.AWAITING_BOOKING);
        assertThat(poll.selectedCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void selectWithUnknownCandidateFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId unknownId = new AccommodationCandidateId(UUID.randomUUID());

        assertThatThrownBy(() -> poll.select(unknownId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void selectOnAlreadyAwaitingBookingPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();
        poll.select(candidateId);

        assertThatThrownBy(() -> poll.select(candidateId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AWAITING_BOOKING");
    }

    @Test
    void bookingSuccessSetsBookedStatus() {
        final AccommodationPoll poll = createOpenPoll();
        poll.select(poll.candidates().getFirst().candidateId());

        poll.recordBookingSuccess();

        assertThat(poll.status()).isEqualTo(AccommodationPollStatus.BOOKED);
    }

    @Test
    void bookingFailureReopensPollAndStoresFailureContext() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();
        poll.select(candidateId);

        poll.recordBookingFailure("Listing unavailable");

        assertThat(poll.status()).isEqualTo(AccommodationPollStatus.OPEN);
        assertThat(poll.selectedCandidateId()).isNull();
        assertThat(poll.lastFailedCandidateId()).isEqualTo(candidateId);
        assertThat(poll.lastFailedCandidateNote()).isEqualTo("Listing unavailable");
    }

    // --- Cancel ---

    @Test
    void cancelSetsStatus() {
        final AccommodationPoll poll = createOpenPoll();

        poll.cancel();

        assertThat(poll.status()).isEqualTo(AccommodationPollStatus.CANCELLED);
    }

    @Test
    void cancelOnBookedPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        poll.select(poll.candidates().getFirst().candidateId());
        poll.recordBookingSuccess();

        assertThatThrownBy(() -> poll.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BOOKED");
    }

    // --- Status Guards ---

    @Test
    void castVoteOnAwaitingBookingPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        poll.select(poll.candidates().getFirst().candidateId());

        assertThatThrownBy(() -> poll.castVote(VOTER_A, poll.candidates().get(1).candidateId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AWAITING_BOOKING");
    }

    @Test
    void castVoteOnCancelledPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        poll.cancel();

        assertThatThrownBy(() -> poll.castVote(VOTER_A, poll.candidates().getFirst().candidateId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CANCELLED");
    }

    @Test
    void addCandidateOnAwaitingBookingPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        poll.select(poll.candidates().getFirst().candidateId());

        assertThatThrownBy(() -> poll.addCandidate("Hotel C", null, null, null, candidateRooms(), Set.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AWAITING_BOOKING");
    }

    // --- Encapsulation ---

    @Test
    void candidatesListIsUnmodifiable() {
        final AccommodationPoll poll = createOpenPoll();

        assertThatThrownBy(() -> poll.candidates().add(
            new AccommodationCandidate("Hotel C", null, null, null, candidateRooms(), Set.of())))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void votesListIsUnmodifiable() {
        final AccommodationPoll poll = createOpenPoll();

        assertThatThrownBy(() -> poll.votes().add(
            new AccommodationVote(
                new AccommodationVoteId(UUID.randomUUID()),
                VOTER_A,
                poll.candidates().getFirst().candidateId())))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void candidateAmenitiesSetIsUnmodifiable() {
        final AccommodationPoll poll = AccommodationPoll.create(TENANT_ID, TRIP_ID, List.of(
            new CandidateProposal("Hotel A", null, null, null, candidateRooms(), Set.of(Amenity.WIFI)),
            new CandidateProposal("Hotel B", null, null, null, candidateRooms(), Set.of())
        ));

        assertThatThrownBy(() -> poll.candidates().get(0).amenities().add(Amenity.POOL))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- CandidateRoom bedDescription ---

    @Test
    void candidateRoomWithBedDescription() {
        final CandidateRoom room = new CandidateRoom("Suite", 3, null, "King bed + sofa bed");

        assertThat(room.name()).isEqualTo("Suite");
        assertThat(room.bedCount()).isEqualTo(3);
        assertThat(room.bedDescription()).isEqualTo("King bed + sofa bed");
    }

    @Test
    void candidateRoomWithNullBedDescriptionIsValid() {
        final CandidateRoom room = new CandidateRoom("Room", 2, null, null);

        assertThat(room.bedDescription()).isNull();
    }

    // --- Helper ---

    private AccommodationPoll createOpenPoll() {
        return AccommodationPoll.create(TENANT_ID, TRIP_ID, List.of(
            new CandidateProposal("Hotel Alpenblick", "https://alpenblick.at", "Alpweg 12, Tirol", "Great view",
                candidateRooms(), Set.of(Amenity.WIFI, Amenity.BALCONY)),
            new CandidateProposal("Berghuette Sonnstein", null, null, "Cozy cabin",
                candidateRooms(), Set.of(Amenity.FIREPLACE, Amenity.SAUNA))
        ));
    }

    private static List<CandidateRoom> candidateRooms() {
        return List.of(new CandidateRoom("Room", 2, null, null));
    }
}
