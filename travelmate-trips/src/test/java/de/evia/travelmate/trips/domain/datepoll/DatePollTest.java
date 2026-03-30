package de.evia.travelmate.trips.domain.datepoll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

class DatePollTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final TripId TRIP_ID = new TripId(UUID.randomUUID());
    private static final UUID VOTER_A = UUID.randomUUID();
    private static final UUID VOTER_B = UUID.randomUUID();

    private static final DateRange JULY = new DateRange(
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14));
    private static final DateRange AUGUST = new DateRange(
        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 14));
    private static final DateRange SEPTEMBER = new DateRange(
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14));

    // --- Creation ---

    @Test
    void createWithTwoOptionsSucceeds() {
        final DatePoll poll = DatePoll.create(TENANT_ID, TRIP_ID, List.of(JULY, AUGUST));

        assertThat(poll.datePollId()).isNotNull();
        assertThat(poll.tenantId()).isEqualTo(TENANT_ID);
        assertThat(poll.tripId()).isEqualTo(TRIP_ID);
        assertThat(poll.status()).isEqualTo(PollStatus.OPEN);
        assertThat(poll.options()).hasSize(2);
        assertThat(poll.votes()).isEmpty();
        assertThat(poll.confirmedOptionId()).isNull();
    }

    @Test
    void createWithOneOptionFails() {
        assertThatThrownBy(() -> DatePoll.create(TENANT_ID, TRIP_ID, List.of(JULY)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 2");
    }

    @Test
    void createWithEmptyOptionsFails() {
        assertThatThrownBy(() -> DatePoll.create(TENANT_ID, TRIP_ID, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 2");
    }

    // --- Add/Remove Options ---

    @Test
    void addOptionIncreasesOptionCount() {
        final DatePoll poll = createOpenPoll();

        final DateOptionId newId = poll.addOption(SEPTEMBER);

        assertThat(newId).isNotNull();
        assertThat(poll.options()).hasSize(3);
    }

    @Test
    void removeOptionWithoutVotesSucceeds() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId optionId = poll.options().getFirst().dateOptionId();

        poll.removeOption(optionId);

        assertThat(poll.options()).hasSize(1);
    }

    @Test
    void removeOptionWithVotesFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId optionId = poll.options().getFirst().dateOptionId();
        poll.castVote(VOTER_A, Set.of(optionId));

        assertThatThrownBy(() -> poll.removeOption(optionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("votes");
    }

    @Test
    void removeUnknownOptionFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId unknownId = new DateOptionId(UUID.randomUUID());

        assertThatThrownBy(() -> poll.removeOption(unknownId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    // --- Voting ---

    @Test
    void castVoteWithMultipleOptionsSucceeds() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().get(0).dateOptionId();
        final DateOptionId option2 = poll.options().get(1).dateOptionId();

        final DateVoteId voteId = poll.castVote(VOTER_A, Set.of(option1, option2));

        assertThat(voteId).isNotNull();
        assertThat(poll.votes()).hasSize(1);
        assertThat(poll.votes().getFirst().selectedOptionIds()).containsExactlyInAnyOrder(option1, option2);
    }

    @Test
    void castVoteWithSingleOptionSucceeds() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().getFirst().dateOptionId();

        poll.castVote(VOTER_A, Set.of(option1));

        assertThat(poll.votes()).hasSize(1);
        assertThat(poll.votes().getFirst().voterId()).isEqualTo(VOTER_A);
    }

    @Test
    void castVoteWithEmptySelectionFails() {
        final DatePoll poll = createOpenPoll();

        assertThatThrownBy(() -> poll.castVote(VOTER_A, Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one option");
    }

    @Test
    void castVoteWithUnknownOptionFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId unknownId = new DateOptionId(UUID.randomUUID());

        assertThatThrownBy(() -> poll.castVote(VOTER_A, Set.of(unknownId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void castVoteTwiceBySameVoterFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().getFirst().dateOptionId();
        poll.castVote(VOTER_A, Set.of(option1));

        assertThatThrownBy(() -> poll.castVote(VOTER_A, Set.of(option1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already has a vote");
    }

    @Test
    void multipleVotersCanVoteIndependently() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().get(0).dateOptionId();
        final DateOptionId option2 = poll.options().get(1).dateOptionId();

        poll.castVote(VOTER_A, Set.of(option1));
        poll.castVote(VOTER_B, Set.of(option1, option2));

        assertThat(poll.votes()).hasSize(2);
    }

    // --- Change Vote ---

    @Test
    void changeVoteReplacesSelection() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().get(0).dateOptionId();
        final DateOptionId option2 = poll.options().get(1).dateOptionId();
        poll.castVote(VOTER_A, Set.of(option1));

        poll.changeVote(VOTER_A, Set.of(option2));

        assertThat(poll.votes().getFirst().selectedOptionIds()).containsExactly(option2);
    }

    @Test
    void changeVoteWithoutExistingVoteFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().getFirst().dateOptionId();

        assertThatThrownBy(() -> poll.changeVote(VOTER_A, Set.of(option1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("has not voted yet");
    }

    @Test
    void changeVoteWithEmptySelectionFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().getFirst().dateOptionId();
        poll.castVote(VOTER_A, Set.of(option1));

        assertThatThrownBy(() -> poll.changeVote(VOTER_A, Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("At least one option");
    }

    // --- Vote Count ---

    @Test
    void voteCountReflectsMultiSelectVotes() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId option1 = poll.options().get(0).dateOptionId();
        final DateOptionId option2 = poll.options().get(1).dateOptionId();

        poll.castVote(VOTER_A, Set.of(option1, option2));
        poll.castVote(VOTER_B, Set.of(option1));

        assertThat(poll.voteCountForOption(option1)).isEqualTo(2);
        assertThat(poll.voteCountForOption(option2)).isEqualTo(1);
    }

    // --- Confirm ---

    @Test
    void confirmSetsStatusAndConfirmedOption() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId optionId = poll.options().getFirst().dateOptionId();

        final DateRange confirmed = poll.confirm(optionId);

        assertThat(poll.status()).isEqualTo(PollStatus.CONFIRMED);
        assertThat(poll.confirmedOptionId()).isEqualTo(optionId);
        assertThat(confirmed).isEqualTo(JULY);
    }

    @Test
    void confirmWithUnknownOptionFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId unknownId = new DateOptionId(UUID.randomUUID());

        assertThatThrownBy(() -> poll.confirm(unknownId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void confirmOnAlreadyConfirmedPollFails() {
        final DatePoll poll = createOpenPoll();
        final DateOptionId optionId = poll.options().getFirst().dateOptionId();
        poll.confirm(optionId);

        assertThatThrownBy(() -> poll.confirm(optionId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    // --- Cancel ---

    @Test
    void cancelSetsStatusToCancelled() {
        final DatePoll poll = createOpenPoll();

        poll.cancel();

        assertThat(poll.status()).isEqualTo(PollStatus.CANCELLED);
    }

    @Test
    void cancelOnConfirmedPollFails() {
        final DatePoll poll = createOpenPoll();
        poll.confirm(poll.options().getFirst().dateOptionId());

        assertThatThrownBy(() -> poll.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    // --- Status Guards ---

    @Test
    void castVoteOnConfirmedPollFails() {
        final DatePoll poll = createOpenPoll();
        poll.confirm(poll.options().getFirst().dateOptionId());
        final DateOptionId option2 = poll.options().get(1).dateOptionId();

        assertThatThrownBy(() -> poll.castVote(VOTER_A, Set.of(option2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    @Test
    void castVoteOnCancelledPollFails() {
        final DatePoll poll = createOpenPoll();
        poll.cancel();
        final DateOptionId option1 = poll.options().getFirst().dateOptionId();

        assertThatThrownBy(() -> poll.castVote(VOTER_A, Set.of(option1)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CANCELLED");
    }

    @Test
    void addOptionOnConfirmedPollFails() {
        final DatePoll poll = createOpenPoll();
        poll.confirm(poll.options().getFirst().dateOptionId());

        assertThatThrownBy(() -> poll.addOption(SEPTEMBER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CONFIRMED");
    }

    // --- Encapsulation ---

    @Test
    void optionsListIsUnmodifiable() {
        final DatePoll poll = createOpenPoll();

        assertThatThrownBy(() -> poll.options().add(new DateOption(SEPTEMBER)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void votesListIsUnmodifiable() {
        final DatePoll poll = createOpenPoll();

        assertThatThrownBy(() -> poll.votes().add(
            new DateVote(new DateVoteId(UUID.randomUUID()), VOTER_A,
                Set.of(poll.options().getFirst().dateOptionId()))))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- Helper ---

    private DatePoll createOpenPoll() {
        return DatePoll.create(TENANT_ID, TRIP_ID, List.of(JULY, AUGUST));
    }
}
