package de.evia.travelmate.trips.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.command.AddDateOptionCommand;
import de.evia.travelmate.trips.application.command.CastDateVoteCommand;
import de.evia.travelmate.trips.application.command.ConfirmDatePollCommand;
import de.evia.travelmate.trips.application.command.CreateDatePollCommand;
import de.evia.travelmate.trips.application.command.CreateDatePollCommand.DateRangeCommand;
import de.evia.travelmate.trips.application.command.RemoveDateOptionCommand;
import de.evia.travelmate.trips.application.representation.DatePollRepresentation;
import de.evia.travelmate.trips.domain.datepoll.DatePoll;
import de.evia.travelmate.trips.domain.datepoll.DatePollId;
import de.evia.travelmate.trips.domain.datepoll.DatePollRepository;
import de.evia.travelmate.trips.domain.trip.DateRange;
import de.evia.travelmate.trips.domain.trip.TripId;

@ExtendWith(MockitoExtension.class)
class DatePollServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID TRIP_UUID = UUID.randomUUID();
    private static final UUID VOTER_A = UUID.randomUUID();

    private static final LocalDate JULY_START = LocalDate.of(2026, 7, 1);
    private static final LocalDate JULY_END = LocalDate.of(2026, 7, 14);
    private static final LocalDate AUG_START = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_END = LocalDate.of(2026, 8, 14);

    @Mock
    private DatePollRepository datePollRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private DatePollService datePollService;

    @Test
    void createDatePollSavesPollAndReturnsRepresentation() {
        when(datePollRepository.findOpenByTripId(any(), any())).thenReturn(Optional.empty());
        when(datePollRepository.save(any(DatePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final CreateDatePollCommand command = new CreateDatePollCommand(
            TENANT_UUID, TRIP_UUID,
            List.of(
                new DateRangeCommand(JULY_START, JULY_END),
                new DateRangeCommand(AUG_START, AUG_END)
            )
        );

        final DatePollRepresentation result = datePollService.createDatePoll(command);

        assertThat(result.datePollId()).isNotNull();
        assertThat(result.tripId()).isEqualTo(TRIP_UUID);
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.options()).hasSize(2);
        assertThat(result.votes()).isEmpty();

        final ArgumentCaptor<DatePoll> captor = ArgumentCaptor.forClass(DatePoll.class);
        verify(datePollRepository).save(captor.capture());
        assertThat(captor.getValue().options()).hasSize(2);
    }

    @Test
    void createDatePollRejectsDuplicateOpenPoll() {
        final DatePoll existing = DatePoll.create(
            new TenantId(TENANT_UUID), new TripId(TRIP_UUID),
            List.of(new DateRange(JULY_START, JULY_END), new DateRange(AUG_START, AUG_END))
        );
        when(datePollRepository.findOpenByTripId(any(), any())).thenReturn(Optional.of(existing));

        final CreateDatePollCommand command = new CreateDatePollCommand(
            TENANT_UUID, TRIP_UUID,
            List.of(
                new DateRangeCommand(JULY_START, JULY_END),
                new DateRangeCommand(AUG_START, AUG_END)
            )
        );

        assertThatThrownBy(() -> datePollService.createDatePoll(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void addOptionSavesUpdatedPoll() {
        final DatePoll poll = createSavedPoll();
        when(datePollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(datePollRepository.save(any(DatePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final AddDateOptionCommand command = new AddDateOptionCommand(
            TENANT_UUID, poll.datePollId().value(),
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14)
        );

        final DatePollRepresentation result = datePollService.addOption(command);

        assertThat(result.options()).hasSize(3);
    }

    @Test
    void removeOptionSavesUpdatedPoll() {
        final DatePoll poll = createSavedPoll();
        final UUID optionId = poll.options().getLast().dateOptionId().value();
        when(datePollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(datePollRepository.save(any(DatePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final RemoveDateOptionCommand command = new RemoveDateOptionCommand(
            TENANT_UUID, poll.datePollId().value(), optionId
        );

        final DatePollRepresentation result = datePollService.removeOption(command);

        assertThat(result.options()).hasSize(1);
    }

    @Test
    void castVoteCreatesNewVote() {
        final DatePoll poll = createSavedPoll();
        final UUID optionId = poll.options().getFirst().dateOptionId().value();
        when(datePollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(datePollRepository.save(any(DatePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final CastDateVoteCommand command = new CastDateVoteCommand(
            TENANT_UUID, poll.datePollId().value(), VOTER_A, Set.of(optionId)
        );

        final DatePollRepresentation result = datePollService.castVote(command);

        assertThat(result.votes()).hasSize(1);
        assertThat(result.votes().getFirst().voterId()).isEqualTo(VOTER_A);
    }

    @Test
    void castVoteChangesExistingVoteInsteadOfDuplicate() {
        final DatePoll poll = createSavedPoll();
        final UUID option1 = poll.options().get(0).dateOptionId().value();
        final UUID option2 = poll.options().get(1).dateOptionId().value();
        poll.castVote(VOTER_A, Set.of(poll.options().get(0).dateOptionId()));

        when(datePollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(datePollRepository.save(any(DatePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final CastDateVoteCommand command = new CastDateVoteCommand(
            TENANT_UUID, poll.datePollId().value(), VOTER_A, Set.of(option2)
        );

        final DatePollRepresentation result = datePollService.castVote(command);

        assertThat(result.votes()).hasSize(1);
        assertThat(result.votes().getFirst().selectedOptionIds()).containsExactly(option2);
    }

    @Test
    void confirmPollSetsConfirmedStatus() {
        final DatePoll poll = createSavedPoll();
        final UUID optionId = poll.options().getFirst().dateOptionId().value();
        when(datePollRepository.findById(any(), any())).thenReturn(Optional.of(poll));
        when(datePollRepository.save(any(DatePoll.class))).thenAnswer(inv -> inv.getArgument(0));

        final ConfirmDatePollCommand command = new ConfirmDatePollCommand(
            TENANT_UUID, poll.datePollId().value(), optionId
        );

        final DatePollRepresentation result = datePollService.confirmPoll(command);

        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.confirmedOptionId()).isEqualTo(optionId);
        verify(tripService).updateDateRange(
            new TripId(TRIP_UUID),
            new DateRange(JULY_START, JULY_END)
        );
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(datePollRepository.findById(any(), any())).thenReturn(Optional.empty());

        final TenantId tenantId = new TenantId(TENANT_UUID);
        final DatePollId pollId = new DatePollId(UUID.randomUUID());

        assertThatThrownBy(() -> datePollService.findById(tenantId, pollId))
            .isInstanceOf(EntityNotFoundException.class);
    }

    private DatePoll createSavedPoll() {
        return DatePoll.create(
            new TenantId(TENANT_UUID),
            new TripId(TRIP_UUID),
            List.of(new DateRange(JULY_START, JULY_END), new DateRange(AUG_START, AUG_END))
        );
    }
}
