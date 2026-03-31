package de.evia.travelmate.trips.domain.accommodationpoll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationCandidate;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationVote;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationVoteId;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateRoom;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateRoom;

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
            List.of(new CandidateProposal("Hotel A", null, null, candidateRooms("Balcony")))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 2");
    }

    @Test
    void createWithEmptyCandidatesFails() {
        assertThatThrownBy(() -> AccommodationPoll.create(TENANT_ID, TRIP_ID, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 2");
    }

    // --- Add/Remove Candidates ---

    @Test
    void addCandidateIncreasesCount() {
        final AccommodationPoll poll = createOpenPoll();

        final AccommodationCandidateId newId = poll.addCandidate("Hotel C", "https://hotelc.com", "Nice pool",
            candidateRooms("Poolside"));

        assertThat(newId).isNotNull();
        assertThat(poll.candidates()).hasSize(3);
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
    void confirmSetsStatusAndSelectedCandidate() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();

        poll.confirm(candidateId);

        assertThat(poll.status()).isEqualTo(AccommodationPollStatus.CONFIRMED);
        assertThat(poll.selectedCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void confirmWithUnknownCandidateFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId unknownId = new AccommodationCandidateId(UUID.randomUUID());

        assertThatThrownBy(() -> poll.confirm(unknownId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void confirmOnAlreadyConfirmedPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        final AccommodationCandidateId candidateId = poll.candidates().getFirst().candidateId();
        poll.confirm(candidateId);

        assertThatThrownBy(() -> poll.confirm(candidateId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    // --- Cancel ---

    @Test
    void cancelSetsStatus() {
        final AccommodationPoll poll = createOpenPoll();

        poll.cancel();

        assertThat(poll.status()).isEqualTo(AccommodationPollStatus.CANCELLED);
    }

    @Test
    void cancelOnConfirmedPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        poll.confirm(poll.candidates().getFirst().candidateId());

        assertThatThrownBy(() -> poll.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    // --- Status Guards ---

    @Test
    void castVoteOnConfirmedPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        poll.confirm(poll.candidates().getFirst().candidateId());

        assertThatThrownBy(() -> poll.castVote(VOTER_A, poll.candidates().get(1).candidateId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
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
    void addCandidateOnConfirmedPollFails() {
        final AccommodationPoll poll = createOpenPoll();
        poll.confirm(poll.candidates().getFirst().candidateId());

        assertThatThrownBy(() -> poll.addCandidate("Hotel C", null, null, candidateRooms("Poolside")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    // --- Encapsulation ---

    @Test
    void candidatesListIsUnmodifiable() {
        final AccommodationPoll poll = createOpenPoll();

        assertThatThrownBy(() -> poll.candidates().add(
            new AccommodationCandidate("Hotel C", null, null, candidateRooms("Extra"))))
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

    // --- Helper ---

    private AccommodationPoll createOpenPoll() {
        return AccommodationPoll.create(TENANT_ID, TRIP_ID, List.of(
            new CandidateProposal("Hotel Alpenblick", "https://alpenblick.at", "Great view", candidateRooms("Balcony view")),
            new CandidateProposal("Berghuette Sonnstein", null, "Cozy cabin", candidateRooms("Wood stove"))
        ));
    }

    private static List<CandidateRoom> candidateRooms(final String features) {
        return List.of(new CandidateRoom("Room", 2, null, features));
    }
}
