package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.CastAccommodationVoteCommand;
import de.evia.travelmate.trips.application.command.ConfirmAccommodationPollCommand;
import de.evia.travelmate.trips.application.command.CreateAccommodationPollCommand;
import de.evia.travelmate.trips.application.command.CreateAccommodationPollCommand.CandidateProposalCommand;
import de.evia.travelmate.trips.application.representation.AccommodationPollRepresentation;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPoll;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollId;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollRepository;
import de.evia.travelmate.trips.domain.accommodationpoll.CandidateProposal;
import de.evia.travelmate.trips.domain.trip.TripId;

@ExtendWith(MockitoExtension.class)
class AccommodationPollServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID VOTER_A = UUID.randomUUID();

    @Mock
    private AccommodationPollRepository accommodationPollRepository;

    @InjectMocks
    private AccommodationPollService accommodationPollService;

    @Test
    void createPollSavesAndReturnsRepresentation() {
        when(accommodationPollRepository.findOpenByTripId(any(), any())).thenReturn(Optional.empty());
        when(accommodationPollRepository.save(any(AccommodationPoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateAccommodationPollCommand command = new CreateAccommodationPollCommand(
            TENANT_UUID, TRIP_UUID,
            List.of(
                new CandidateProposalCommand("Hotel A", "https://a.com", "Nice"),
                new CandidateProposalCommand("Hotel B", null, "Cozy")
            )
        );

        final AccommodationPollRepresentation result = accommodationPollService.createPoll(command);

        assertThat(result.accommodationPollId()).isNotNull();
        assertThat(result.tripId()).isEqualTo(TRIP_UUID);
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.candidates()).hasSize(2);
        assertThat(result.votes()).isEmpty();

        final ArgumentCaptor<AccommodationPoll> captor = ArgumentCaptor.forClass(AccommodationPoll.class);
        verify(accommodationPollRepository).save(captor.capture());
        assertThat(captor.getValue().candidates()).hasSize(2);
    }

    @Test
    void createPollRejectsDuplicateOpenPoll() {
        final AccommodationPoll existing = createSavedPoll();
        when(accommodationPollRepository.findOpenByTripId(any(), any())).thenReturn(Optional.of(existing));

        final CreateAccommodationPollCommand command = new CreateAccommodationPollCommand(
            TENANT_UUID, TRIP_UUID,
            List.of(
                new CandidateProposalCommand("Hotel A", null, null),
                new CandidateProposalCommand("Hotel B", null, null)
            )
        );

        assertThatThrownBy(() -> accommodationPollService.createPoll(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void castVoteCreatesNewVote() {
        final AccommodationPoll poll = createSavedPoll();
        final UUID candidateId = poll.candidates().getFirst().candidateId().value();
        when(accommodationPollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(accommodationPollRepository.save(any(AccommodationPoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final CastAccommodationVoteCommand command = new CastAccommodationVoteCommand(
            TENANT_UUID, poll.accommodationPollId().value(), VOTER_A, candidateId
        );

        final AccommodationPollRepresentation result = accommodationPollService.castVote(command);

        assertThat(result.votes()).hasSize(1);
        assertThat(result.votes().getFirst().voterId()).isEqualTo(VOTER_A);
        assertThat(result.votes().getFirst().selectedCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void castVoteChangesExistingVoteInsteadOfDuplicate() {
        final AccommodationPoll poll = createSavedPoll();
        final UUID candidate1 = poll.candidates().get(0).candidateId().value();
        final UUID candidate2 = poll.candidates().get(1).candidateId().value();
        poll.castVote(VOTER_A, poll.candidates().get(0).candidateId());

        when(accommodationPollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(accommodationPollRepository.save(any(AccommodationPoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final CastAccommodationVoteCommand command = new CastAccommodationVoteCommand(
            TENANT_UUID, poll.accommodationPollId().value(), VOTER_A, candidate2
        );

        final AccommodationPollRepresentation result = accommodationPollService.castVote(command);

        assertThat(result.votes()).hasSize(1);
        assertThat(result.votes().getFirst().selectedCandidateId()).isEqualTo(candidate2);
    }

    @Test
    void confirmPollSetsConfirmedStatus() {
        final AccommodationPoll poll = createSavedPoll();
        final UUID candidateId = poll.candidates().getFirst().candidateId().value();
        when(accommodationPollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(accommodationPollRepository.save(any(AccommodationPoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final ConfirmAccommodationPollCommand command = new ConfirmAccommodationPollCommand(
            TENANT_UUID, poll.accommodationPollId().value(), candidateId
        );

        final AccommodationPollRepresentation result = accommodationPollService.confirmPoll(command);

        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.selectedCandidateId()).isEqualTo(candidateId);
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(accommodationPollRepository.findById(any(), any())).thenReturn(Optional.empty());

        final TenantId tenantId = new TenantId(TENANT_UUID);
        final AccommodationPollId pollId = new AccommodationPollId(UUID.randomUUID());

        assertThatThrownBy(() -> accommodationPollService.findById(tenantId, pollId))
            .isInstanceOf(EntityNotFoundException.class);
    }

    private AccommodationPoll createSavedPoll() {
        return AccommodationPoll.create(
            new TenantId(TENANT_UUID),
            new TripId(TRIP_UUID),
            List.of(
                new CandidateProposal("Hotel Alpenblick", "https://alpenblick.at", "Great"),
                new CandidateProposal("Berghuette Sonnstein", null, "Cozy")
            )
        );
    }
}
