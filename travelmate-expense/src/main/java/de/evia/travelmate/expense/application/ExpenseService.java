package de.evia.travelmate.expense.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.evia.travelmate.common.domain.DomainEvent;
import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.expense.application.command.AddReceiptCommand;
import de.evia.travelmate.expense.application.command.UpdateWeightingCommand;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.domain.expense.Amount;
import de.evia.travelmate.expense.domain.expense.Expense;
import de.evia.travelmate.expense.domain.expense.ExpenseRepository;
import de.evia.travelmate.expense.domain.expense.ParticipantWeighting;
import de.evia.travelmate.expense.domain.expense.ReceiptId;
import de.evia.travelmate.expense.domain.trip.TripParticipant;
import de.evia.travelmate.expense.domain.trip.TripProjection;
import de.evia.travelmate.expense.domain.trip.TripProjectionRepository;

@Service
@Transactional
public class ExpenseService {

    private static final Logger LOG = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;
    private final TripProjectionRepository tripProjectionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ExpenseService(final ExpenseRepository expenseRepository,
                          final TripProjectionRepository tripProjectionRepository,
                          final ApplicationEventPublisher eventPublisher) {
        this.expenseRepository = expenseRepository;
        this.tripProjectionRepository = tripProjectionRepository;
        this.eventPublisher = eventPublisher;
    }

    public void onTripCreated(final TripCreated event) {
        if (tripProjectionRepository.existsByTripId(event.tripId())) {
            LOG.info("TripProjection already exists for trip {}, skipping", event.tripId());
            return;
        }
        final TripProjection projection = TripProjection.create(
            event.tripId(), new TenantId(event.tenantId()), event.tripName()
        );
        tripProjectionRepository.save(projection);
    }

    public void onParticipantJoined(final ParticipantJoinedTrip event) {
        final TripProjection projection = tripProjectionRepository.findByTripId(event.tripId())
            .orElseGet(() -> {
                LOG.warn("TripProjection not found for trip {}, creating stub", event.tripId());
                return TripProjection.create(event.tripId(), new TenantId(event.tenantId()), "Unknown Trip");
            });
        projection.addParticipant(new TripParticipant(event.participantId(), event.username()));
        tripProjectionRepository.save(projection);
    }

    public void onTripCompleted(final TripCompleted event) {
        if (expenseRepository.existsByTripId(event.tripId())) {
            LOG.info("Expense already exists for trip {}, skipping", event.tripId());
            return;
        }
        final TripProjection projection = tripProjectionRepository.findByTripId(event.tripId())
            .orElseThrow(() -> new EntityNotFoundException("TripProjection", event.tripId().toString()));

        final List<ParticipantWeighting> weightings = projection.participants().stream()
            .map(p -> new ParticipantWeighting(p.participantId(), BigDecimal.ONE))
            .toList();

        final Expense expense = Expense.create(
            new TenantId(event.tenantId()), event.tripId(), weightings
        );
        expenseRepository.save(expense);
        publishEvents(expense);
        LOG.info("Created expense {} for completed trip {}", expense.expenseId().value(), event.tripId());
    }

    public ExpenseRepresentation addReceipt(final TenantId tenantId, final AddReceiptCommand command) {
        final Expense expense = findByTripId(tenantId, command.tripId());
        expense.addReceipt(command.description(), new Amount(command.amount()), command.paidBy(), command.date());
        expenseRepository.save(expense);
        return ExpenseRepresentation.from(expense);
    }

    public ExpenseRepresentation removeReceipt(final TenantId tenantId, final UUID tripId, final UUID receiptId) {
        final Expense expense = findByTripId(tenantId, tripId);
        expense.removeReceipt(new ReceiptId(receiptId));
        expenseRepository.save(expense);
        return ExpenseRepresentation.from(expense);
    }

    public ExpenseRepresentation updateWeighting(final TenantId tenantId, final UpdateWeightingCommand command) {
        final Expense expense = findByTripId(tenantId, command.tripId());
        expense.updateWeighting(command.participantId(), command.weight());
        expenseRepository.save(expense);
        return ExpenseRepresentation.from(expense);
    }

    public ExpenseRepresentation settle(final TenantId tenantId, final UUID tripId) {
        final Expense expense = findByTripId(tenantId, tripId);
        expense.settle();
        expenseRepository.save(expense);
        publishEvents(expense);
        return ExpenseRepresentation.from(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseRepresentation findByTripId(final TenantId tenantId, final UUID tripId,
                                               final boolean representationOnly) {
        final Expense expense = findByTripId(tenantId, tripId);
        return ExpenseRepresentation.from(expense);
    }

    private Expense findByTripId(final TenantId tenantId, final UUID tripId) {
        return expenseRepository.findByTripId(tenantId, tripId)
            .orElseThrow(() -> new EntityNotFoundException("Expense", tripId.toString()));
    }

    private void publishEvents(final Expense expense) {
        for (final DomainEvent event : expense.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        expense.clearDomainEvents();
    }
}
