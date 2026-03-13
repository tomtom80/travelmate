package de.evia.travelmate.expense.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.expense.ExpenseCreated;
import de.evia.travelmate.common.events.expense.ExpenseSettled;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.expense.application.command.AddReceiptCommand;
import de.evia.travelmate.expense.application.command.UpdateWeightingCommand;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseRepository;
import de.evia.travelmate.expense.domain.expense.ExpenseStatus;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final TenantId TENANT_ID = new TenantId(TENANT_UUID);
    private static final UUID TRIP_ID = UUID.randomUUID();
    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private TripProjectionRepository tripProjectionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ExpenseService expenseService;

    // --- onTripCreated ---

    @Test
    void onTripCreatedSavesProjection() {
        final TripCreated event = new TripCreated(
            TENANT_UUID, TRIP_ID, "Summer Vacation",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14), LocalDate.now()
        );
        when(tripProjectionRepository.existsByTripId(TRIP_ID)).thenReturn(false);
        when(tripProjectionRepository.save(any(TripProjection.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        expenseService.onTripCreated(event);

        final ArgumentCaptor<TripProjection> captor = ArgumentCaptor.forClass(TripProjection.class);
        verify(tripProjectionRepository).save(captor.capture());
        final TripProjection saved = captor.getValue();
        assertThat(saved.tripId()).isEqualTo(TRIP_ID);
        assertThat(saved.tripName()).isEqualTo("Summer Vacation");
    }

    @Test
    void onTripCreatedSkipsIfProjectionAlreadyExists() {
        final TripCreated event = new TripCreated(
            TENANT_UUID, TRIP_ID, "Summer Vacation",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 14), LocalDate.now()
        );
        when(tripProjectionRepository.existsByTripId(TRIP_ID)).thenReturn(true);

        expenseService.onTripCreated(event);

        verify(tripProjectionRepository, never()).save(any());
    }

    // --- onParticipantJoined ---

    @Test
    void onParticipantJoinedAddsToExistingProjection() {
        final ParticipantJoinedTrip event = new ParticipantJoinedTrip(
            TENANT_UUID, TRIP_ID, ALICE, "Alice", LocalDate.now()
        );
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Summer Vacation");
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(projection));
        when(tripProjectionRepository.save(any(TripProjection.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        expenseService.onParticipantJoined(event);

        final ArgumentCaptor<TripProjection> captor = ArgumentCaptor.forClass(TripProjection.class);
        verify(tripProjectionRepository).save(captor.capture());
        assertThat(captor.getValue().participants()).hasSize(1);
        assertThat(captor.getValue().participants().getFirst().participantId()).isEqualTo(ALICE);
    }

    @Test
    void onParticipantJoinedCreatesStubIfProjectionMissing() {
        final ParticipantJoinedTrip event = new ParticipantJoinedTrip(
            TENANT_UUID, TRIP_ID, ALICE, "Alice", LocalDate.now()
        );
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());
        when(tripProjectionRepository.save(any(TripProjection.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        expenseService.onParticipantJoined(event);

        final ArgumentCaptor<TripProjection> captor = ArgumentCaptor.forClass(TripProjection.class);
        verify(tripProjectionRepository).save(captor.capture());
        final TripProjection saved = captor.getValue();
        assertThat(saved.tripName()).isEqualTo("Unknown Trip");
        assertThat(saved.participants()).hasSize(1);
    }

    // --- onTripCompleted ---

    @Test
    void onTripCompletedCreatesExpenseWithDefaultWeightings() {
        final TripCompleted event = new TripCompleted(TENANT_UUID, TRIP_ID, LocalDate.now());
        final TripProjection projection = projectionWithParticipants();
        when(expenseRepository.existsByTripId(TRIP_ID)).thenReturn(false);
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(projection));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        expenseService.onTripCompleted(event);

        final ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        final Expense saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(ExpenseStatus.OPEN);
        assertThat(saved.weightings()).hasSize(2);
        assertThat(saved.weightings()).allSatisfy(
            w -> assertThat(w.weight()).isEqualByComparingTo(BigDecimal.ONE)
        );
        final ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ExpenseCreated.class);
    }

    @Test
    void onTripCompletedSkipsIfExpenseAlreadyExists() {
        final TripCompleted event = new TripCompleted(TENANT_UUID, TRIP_ID, LocalDate.now());
        when(expenseRepository.existsByTripId(TRIP_ID)).thenReturn(true);

        expenseService.onTripCompleted(event);

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void onTripCompletedThrowsIfProjectionMissing() {
        final TripCompleted event = new TripCompleted(TENANT_UUID, TRIP_ID, LocalDate.now());
        when(expenseRepository.existsByTripId(TRIP_ID)).thenReturn(false);
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.onTripCompleted(event))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // --- addReceipt ---

    @Test
    void addReceiptReturnsRepresentationWithReceipt() {
        final Expense expense = createOpenExpense();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        final AddReceiptCommand command = new AddReceiptCommand(
            TRIP_ID, "Groceries", new BigDecimal("50.00"), ALICE, LocalDate.of(2026, 7, 2)
        );

        final ExpenseRepresentation result = expenseService.addReceipt(TENANT_ID, command);

        assertThat(result.receipts()).hasSize(1);
        assertThat(result.receipts().getFirst().description()).isEqualTo("Groceries");
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    // --- removeReceipt ---

    @Test
    void removeReceiptReturnsRepresentationWithoutReceipt() {
        final Expense expense = createOpenExpense();
        expense.addReceipt("Groceries", new de.evia.travelmate.expense.domain.expense.Amount(new BigDecimal("50.00")),
            ALICE, LocalDate.of(2026, 7, 2));
        final UUID receiptId = expense.receipts().getFirst().receiptId().value();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final ExpenseRepresentation result = expenseService.removeReceipt(TENANT_ID, TRIP_ID, receiptId);

        assertThat(result.receipts()).isEmpty();
    }

    // --- updateWeighting ---

    @Test
    void updateWeightingChangesParticipantWeight() {
        final Expense expense = createOpenExpense();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        final UpdateWeightingCommand command = new UpdateWeightingCommand(
            TRIP_ID, BOB, new BigDecimal("0.5")
        );

        final ExpenseRepresentation result = expenseService.updateWeighting(TENANT_ID, command);

        assertThat(result.weightings()).anySatisfy(w -> {
            assertThat(w.participantId()).isEqualTo(BOB);
            assertThat(w.weight()).isEqualByComparingTo(new BigDecimal("0.5"));
        });
    }

    // --- settle ---

    @Test
    void settleChangesStatusToSettled() {
        final Expense expense = createOpenExpense();
        expense.addReceipt("Groceries", new de.evia.travelmate.expense.domain.expense.Amount(new BigDecimal("50.00")),
            ALICE, LocalDate.of(2026, 7, 2));
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final ExpenseRepresentation result = expenseService.settle(TENANT_ID, TRIP_ID);

        assertThat(result.status()).isEqualTo(ExpenseStatus.SETTLED);
        final ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(ExpenseSettled.class);
    }

    // --- findByTripId ---

    @Test
    void findByTripIdReturnsRepresentation() {
        final Expense expense = createOpenExpense();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));

        final ExpenseRepresentation result = expenseService.findByTripId(TENANT_ID, TRIP_ID, true);

        assertThat(result.tripId()).isEqualTo(TRIP_ID);
        assertThat(result.status()).isEqualTo(ExpenseStatus.OPEN);
    }

    @Test
    void findByTripIdThrowsWhenNotFound() {
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.findByTripId(TENANT_ID, TRIP_ID, true))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // --- helpers ---

    private TripProjection projectionWithParticipants() {
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Summer Vacation");
        projection.addParticipant(new TripParticipant(ALICE, "Alice"));
        projection.addParticipant(new TripParticipant(BOB, "Bob"));
        return projection;
    }

    private Expense createOpenExpense() {
        final Expense expense = Expense.create(
            TENANT_ID, TRIP_ID,
            List.of(
                new ParticipantWeighting(ALICE, BigDecimal.ONE),
                new ParticipantWeighting(BOB, BigDecimal.ONE)
            )
        );
        expense.clearDomainEvents();
        return expense;
    }
}
