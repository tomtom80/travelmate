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
import de.evia.travelmate.common.events.trips.AccommodationPriceSet;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.StayPeriodUpdated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.expense.application.command.AddReceiptCommand;
import de.evia.travelmate.expense.application.command.ApproveReceiptCommand;
import de.evia.travelmate.expense.application.command.ConfirmAdvancePaymentsCommand;
import de.evia.travelmate.expense.application.command.RejectReceiptCommand;
import de.evia.travelmate.expense.application.command.ResubmitReceiptCommand;
import de.evia.travelmate.expense.application.command.ToggleAdvancePaymentPaidCommand;
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
    void onParticipantJoinedStoresPartyInfo() {
        final UUID partyTenantId = UUID.randomUUID();
        final ParticipantJoinedTrip event = new ParticipantJoinedTrip(
            TENANT_UUID, TRIP_ID, ALICE, "Alice", partyTenantId, "Familie Schmidt", LocalDate.now()
        );
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Summer Vacation");
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(projection));
        when(tripProjectionRepository.save(any(TripProjection.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        expenseService.onParticipantJoined(event);

        final ArgumentCaptor<TripProjection> captor = ArgumentCaptor.forClass(TripProjection.class);
        verify(tripProjectionRepository).save(captor.capture());
        final TripParticipant saved = captor.getValue().participants().getFirst();
        assertThat(saved.partyTenantId()).isEqualTo(partyTenantId);
        assertThat(saved.partyName()).isEqualTo("Familie Schmidt");
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

    // --- onStayPeriodUpdated ---

    @Test
    void onStayPeriodUpdatedSetsParticipantDates() {
        final TripProjection projection = projectionWithParticipants();
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(projection));
        when(tripProjectionRepository.save(any(TripProjection.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        final StayPeriodUpdated event = new StayPeriodUpdated(
            TENANT_UUID, TRIP_ID, ALICE,
            LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 10), LocalDate.now()
        );

        expenseService.onStayPeriodUpdated(event);

        final ArgumentCaptor<TripProjection> captor = ArgumentCaptor.forClass(TripProjection.class);
        verify(tripProjectionRepository).save(captor.capture());
        final TripParticipant updated = captor.getValue().participants().stream()
            .filter(p -> p.participantId().equals(ALICE))
            .findFirst().orElseThrow();
        assertThat(updated.arrivalDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(updated.departureDate()).isEqualTo(LocalDate.of(2026, 7, 10));
        assertThat(updated.nights()).isEqualTo(8);
    }

    @Test
    void onStayPeriodUpdatedThrowsIfProjectionMissing() {
        final StayPeriodUpdated event = new StayPeriodUpdated(
            TENANT_UUID, TRIP_ID, ALICE,
            LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 10), LocalDate.now()
        );
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.onStayPeriodUpdated(event))
            .isInstanceOf(EntityNotFoundException.class);
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
            TRIP_ID, "Groceries", new BigDecimal("50.00"), ALICE, ALICE, LocalDate.of(2026, 7, 2), null
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
            ALICE, ALICE, LocalDate.of(2026, 7, 2), null);
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
            ALICE, ALICE, LocalDate.of(2026, 7, 2), null);
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
    void findByTripIdIncludesPartySettlementsWhenPartyInfoAvailable() {
        final UUID partySchmidt = UUID.randomUUID();
        final UUID partyMueller = UUID.randomUUID();
        final Expense expense = createOpenExpense();
        expense.addReceipt("Dinner", new de.evia.travelmate.expense.domain.expense.Amount(
            new java.math.BigDecimal("100.00")), ALICE, ALICE, LocalDate.of(2026, 7, 2), null);
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));

        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Summer Vacation");
        projection.addParticipant(new TripParticipant(
            ALICE, "Alice", null, null, partySchmidt, "Familie Schmidt"));
        projection.addParticipant(new TripParticipant(
            BOB, "Bob", null, null, partyMueller, "Familie Mueller"));
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(projection));

        final ExpenseRepresentation result = expenseService.findByTripId(TENANT_ID, TRIP_ID, true);

        assertThat(result.partySettlements()).hasSize(2);
        assertThat(result.partyTransfers()).hasSize(1);
        assertThat(result.partyTransfers().getFirst().fromPartyName()).isEqualTo("Familie Mueller");
        assertThat(result.partyTransfers().getFirst().toPartyName()).isEqualTo("Familie Schmidt");
    }

    @Test
    void findByTripIdThrowsWhenNotFound() {
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.findByTripId(TENANT_ID, TRIP_ID, true))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // --- approveReceipt ---

    @Test
    void approveReceiptReturnsRepresentationWithApprovedStatus() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new de.evia.travelmate.expense.domain.expense.Amount(new BigDecimal("50.00")),
            ALICE, ALICE, LocalDate.of(2026, 7, 2), null);
        final UUID receiptId = expense.receipts().getFirst().receiptId().value();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final ExpenseRepresentation result = expenseService.approveReceipt(
            TENANT_ID, new ApproveReceiptCommand(TRIP_ID, receiptId, BOB));

        assertThat(result.receipts().getFirst().reviewStatus())
            .isEqualTo(de.evia.travelmate.expense.domain.expense.ReviewStatus.APPROVED);
    }

    // --- rejectReceipt ---

    @Test
    void rejectReceiptReturnsRepresentationWithRejectedStatus() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new de.evia.travelmate.expense.domain.expense.Amount(new BigDecimal("50.00")),
            ALICE, ALICE, LocalDate.of(2026, 7, 2), null);
        final UUID receiptId = expense.receipts().getFirst().receiptId().value();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final ExpenseRepresentation result = expenseService.rejectReceipt(
            TENANT_ID, new RejectReceiptCommand(TRIP_ID, receiptId, BOB, "Wrong amount"));

        assertThat(result.receipts().getFirst().reviewStatus())
            .isEqualTo(de.evia.travelmate.expense.domain.expense.ReviewStatus.REJECTED);
        assertThat(result.receipts().getFirst().rejectionReason()).isEqualTo("Wrong amount");
    }

    // --- resubmitReceipt ---

    @Test
    void resubmitReceiptResetsToSubmitted() {
        final Expense expense = createReviewExpense();
        expense.addReceipt("Groceries", new de.evia.travelmate.expense.domain.expense.Amount(new BigDecimal("50.00")),
            ALICE, ALICE, LocalDate.of(2026, 7, 2), null);
        final UUID receiptId = expense.receipts().getFirst().receiptId().value();
        expense.rejectReceipt(new de.evia.travelmate.expense.domain.expense.ReceiptId(receiptId), BOB, "Wrong amount");
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final ExpenseRepresentation result = expenseService.resubmitReceipt(
            TENANT_ID, new ResubmitReceiptCommand(TRIP_ID, receiptId, "Groceries corrected",
                new BigDecimal("45.00"), LocalDate.of(2026, 7, 2), null));

        assertThat(result.receipts().getFirst().reviewStatus())
            .isEqualTo(de.evia.travelmate.expense.domain.expense.ReviewStatus.SUBMITTED);
        assertThat(result.receipts().getFirst().description()).isEqualTo("Groceries corrected");
        assertThat(result.receipts().getFirst().amount()).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    // --- onAccommodationPriceSet ---

    @Test
    void onAccommodationPriceSetUpdatesProjectionPrice() {
        final AccommodationPriceSet event = new AccommodationPriceSet(
            TENANT_UUID, TRIP_ID, new BigDecimal("3000.00"), LocalDate.now()
        );
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Summer Vacation");
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(projection));
        when(tripProjectionRepository.save(any(TripProjection.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        expenseService.onAccommodationPriceSet(event);

        final ArgumentCaptor<TripProjection> captor = ArgumentCaptor.forClass(TripProjection.class);
        verify(tripProjectionRepository).save(captor.capture());
        assertThat(captor.getValue().accommodationTotalPrice()).isEqualByComparingTo("3000.00");
    }

    @Test
    void onAccommodationPriceSetSkipsIfProjectionMissing() {
        final AccommodationPriceSet event = new AccommodationPriceSet(
            TENANT_UUID, TRIP_ID, new BigDecimal("3000.00"), LocalDate.now()
        );
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.empty());

        expenseService.onAccommodationPriceSet(event);

        verify(tripProjectionRepository, never()).save(any());
    }

    // --- confirmAdvancePayments ---

    @Test
    void confirmAdvancePaymentsCreatesPaymentsFromParties() {
        final UUID partySchmidt = UUID.randomUUID();
        final UUID partyMueller = UUID.randomUUID();
        final Expense expense = createOpenExpense();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Summer Vacation");
        projection.addParticipant(new TripParticipant(
            ALICE, "Alice", null, null, partySchmidt, "Familie Schmidt"));
        projection.addParticipant(new TripParticipant(
            BOB, "Bob", null, null, partyMueller, "Familie Mueller"));
        when(tripProjectionRepository.findByTripId(TRIP_ID)).thenReturn(Optional.of(projection));

        final ExpenseRepresentation result = expenseService.confirmAdvancePayments(
            TENANT_ID, new ConfirmAdvancePaymentsCommand(TRIP_ID, new BigDecimal("500.00")));

        assertThat(result.advancePayments()).hasSize(2);
        assertThat(result.advancePayments()).allSatisfy(
            ap -> assertThat(ap.amount()).isEqualByComparingTo("500.00"));
    }

    // --- removeAdvancePayments ---

    @Test
    void removeAdvancePaymentsClearsAll() {
        final Expense expense = createOpenExpense();
        expense.confirmAdvancePayments(new BigDecimal("500.00"), List.of(
            new de.evia.travelmate.expense.domain.expense.Expense.PartyInfo(UUID.randomUUID(), "Test")));
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final ExpenseRepresentation result = expenseService.removeAdvancePayments(TENANT_ID, TRIP_ID);

        assertThat(result.advancePayments()).isEmpty();
    }

    // --- toggleAdvancePaymentPaid ---

    @Test
    void toggleAdvancePaymentPaidTogglesPaidStatus() {
        final Expense expense = createOpenExpense();
        expense.confirmAdvancePayments(new BigDecimal("500.00"), List.of(
            new de.evia.travelmate.expense.domain.expense.Expense.PartyInfo(UUID.randomUUID(), "Test")));
        final UUID apId = expense.advancePayments().getFirst().advancePaymentId().value();
        when(expenseRepository.findByTripId(TENANT_ID, TRIP_ID)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        final ExpenseRepresentation result = expenseService.toggleAdvancePaymentPaid(
            TENANT_ID, new ToggleAdvancePaymentPaidCommand(TRIP_ID, apId));

        assertThat(result.advancePayments().getFirst().paid()).isTrue();
    }

    // --- helpers ---

    private TripProjection projectionWithParticipants() {
        final TripProjection projection = TripProjection.create(TRIP_ID, TENANT_ID, "Summer Vacation");
        projection.addParticipant(new TripParticipant(ALICE, "Alice"));
        projection.addParticipant(new TripParticipant(BOB, "Bob"));
        return projection;
    }

    private Expense createReviewExpense() {
        final Expense expense = Expense.create(
            TENANT_ID, TRIP_ID,
            List.of(
                new ParticipantWeighting(ALICE, BigDecimal.ONE),
                new ParticipantWeighting(BOB, BigDecimal.ONE)
            ),
            true
        );
        expense.clearDomainEvents();
        return expense;
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
