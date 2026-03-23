package de.evia.travelmate.expense.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import de.evia.travelmate.common.events.trips.StayPeriodUpdated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.common.events.trips.AccommodationPriceSet;
import de.evia.travelmate.expense.application.command.AddReceiptCommand;
import de.evia.travelmate.expense.application.command.ApproveReceiptCommand;
import de.evia.travelmate.expense.application.command.ConfirmAdvancePaymentsCommand;
import de.evia.travelmate.expense.application.command.RejectReceiptCommand;
import de.evia.travelmate.expense.application.command.ResubmitReceiptCommand;
import de.evia.travelmate.expense.application.command.ToggleAdvancePaymentPaidCommand;
import de.evia.travelmate.expense.application.command.UpdateWeightingCommand;
import de.evia.travelmate.expense.application.representation.ExpenseRepresentation;
import de.evia.travelmate.expense.domain.expense.AdvancePaymentId;
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
            event.tripId(), new TenantId(event.tenantId()), event.tripName(),
            event.startDate(), event.endDate()
        );
        tripProjectionRepository.save(projection);
    }

    public void onParticipantJoined(final ParticipantJoinedTrip event) {
        final TripProjection projection = tripProjectionRepository.findByTripId(event.tripId())
            .orElseGet(() -> {
                LOG.warn("TripProjection not found for trip {}, creating stub", event.tripId());
                return TripProjection.create(event.tripId(), new TenantId(event.tenantId()), "Unknown Trip");
            });
        projection.addParticipant(new TripParticipant(
            event.participantId(), event.username(), null, null,
            event.participantTenantId(), event.partyName(), event.dateOfBirth(), event.accountHolder()));
        tripProjectionRepository.save(projection);
        synchronizeExpenseForParticipantJoin(projection, event.participantId());
    }

    public void onStayPeriodUpdated(final StayPeriodUpdated event) {
        final TripProjection projection = tripProjectionRepository.findByTripId(event.tripId())
            .orElseThrow(() -> new EntityNotFoundException("TripProjection", event.tripId().toString()));
        projection.updateParticipantStayPeriod(
            event.participantId(), event.arrivalDate(), event.departureDate()
        );
        tripProjectionRepository.save(projection);
        LOG.info("Updated stay period for participant {} in trip {}", event.participantId(), event.tripId());
    }

    public void onTripCompleted(final TripCompleted event) {
        if (expenseRepository.existsByTripId(event.tripId())) {
            LOG.info("Expense already exists for trip {}, skipping", event.tripId());
            return;
        }
        final TripProjection projection = tripProjectionRepository.findByTripId(event.tripId())
            .orElseThrow(() -> new EntityNotFoundException("TripProjection", event.tripId().toString()));

        final List<ParticipantWeighting> weightings = projection.participants().stream()
            .map(p -> new ParticipantWeighting(p.participantId(), defaultWeightingFor(p, projection.startDate())))
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
        expense.addReceipt(command.description(), new Amount(command.amount()), command.paidBy(), command.submittedBy(), command.date(), command.category());
        expenseRepository.save(expense);
        return ExpenseRepresentation.from(expense);
    }

    public ExpenseRepresentation approveReceipt(final TenantId tenantId,
                                                final ApproveReceiptCommand command) {
        final Expense expense = findByTripId(tenantId, command.tripId());
        expense.approveReceipt(new ReceiptId(command.receiptId()), command.reviewerId());
        expenseRepository.save(expense);
        return ExpenseRepresentation.from(expense);
    }

    public ExpenseRepresentation rejectReceipt(final TenantId tenantId,
                                               final RejectReceiptCommand command) {
        final Expense expense = findByTripId(tenantId, command.tripId());
        expense.rejectReceipt(new ReceiptId(command.receiptId()), command.reviewerId(),
            command.reason());
        expenseRepository.save(expense);
        return ExpenseRepresentation.from(expense);
    }

    public ExpenseRepresentation resubmitReceipt(final TenantId tenantId,
                                                 final ResubmitReceiptCommand command) {
        final Expense expense = findByTripId(tenantId, command.tripId());
        expense.resubmitReceipt(new ReceiptId(command.receiptId()), command.description(),
            new Amount(command.amount()), command.date(), command.category());
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
        final TripProjection projection = tripProjectionRepository.findByTripId(command.tripId())
            .orElseThrow(() -> new EntityNotFoundException("TripProjection", command.tripId().toString()));
        expense.updateWeighting(command.participantId(), command.weight());
        expenseRepository.save(expense);
        return toRepresentation(expense, projection);
    }

    public void onAccommodationPriceSet(final AccommodationPriceSet event) {
        final TripProjection projection = tripProjectionRepository.findByTripId(event.tripId())
            .orElseGet(() -> {
                LOG.warn("TripProjection not found for trip {}, skipping accommodation price", event.tripId());
                return null;
            });
        if (projection == null) {
            return;
        }
        projection.setAccommodationTotalPrice(event.totalPrice());
        tripProjectionRepository.save(projection);
        LOG.info("Updated accommodation total price for trip {} to {}", event.tripId(), event.totalPrice());
    }

    public ExpenseRepresentation confirmAdvancePayments(final TenantId tenantId,
                                                         final ConfirmAdvancePaymentsCommand command) {
        final Expense expense = findByTripId(tenantId, command.tripId());
        final TripProjection projection = tripProjectionRepository.findByTripId(command.tripId())
            .orElseThrow(() -> new EntityNotFoundException("TripProjection", command.tripId().toString()));

        final List<Expense.PartyInfo> parties = buildPartyInfos(projection);
        expense.confirmAdvancePayments(command.amount(), parties);
        expenseRepository.save(expense);
        return toRepresentation(expense, projection);
    }

    public ExpenseRepresentation removeAdvancePayments(final TenantId tenantId, final UUID tripId) {
        final Expense expense = findByTripId(tenantId, tripId);
        final TripProjection projection = tripProjectionRepository.findByTripId(tripId)
            .orElseThrow(() -> new EntityNotFoundException("TripProjection", tripId.toString()));
        expense.removeAdvancePayments();
        expenseRepository.save(expense);
        return toRepresentation(expense, projection);
    }

    public ExpenseRepresentation toggleAdvancePaymentPaid(final TenantId tenantId,
                                                           final ToggleAdvancePaymentPaidCommand command) {
        final Expense expense = findByTripId(tenantId, command.tripId());
        final TripProjection projection = tripProjectionRepository.findByTripId(command.tripId())
            .orElseThrow(() -> new EntityNotFoundException("TripProjection", command.tripId().toString()));
        expense.toggleAdvancePaymentPaid(
            new AdvancePaymentId(command.advancePaymentId()),
            command.markedByParticipantId()
        );
        expenseRepository.save(expense);
        return toRepresentation(expense, projection);
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
        final TripProjection projection = tripProjectionRepository.findByTripId(tripId).orElse(null);
        if (projection != null) {
            return ExpenseRepresentation.from(expense, projection.startDate(), projection.endDate(),
                projection.accommodationTotalPrice(), projection.participants());
        }
        return ExpenseRepresentation.from(expense);
    }

    private Expense findByTripId(final TenantId tenantId, final UUID tripId) {
        return expenseRepository.findByTripId(tenantId, tripId)
            .orElseGet(() -> createExpenseFromProjection(tenantId, tripId));
    }

    private Expense createExpenseFromProjection(final TenantId tenantId, final UUID tripId) {
        final TripProjection projection = tripProjectionRepository.findByTripId(tripId)
            .orElseThrow(() -> new EntityNotFoundException("Expense", tripId.toString()));
        final List<ParticipantWeighting> weightings = projection.participants().stream()
            .map(p -> new ParticipantWeighting(p.participantId(), defaultWeightingFor(p, projection.startDate())))
            .toList();
        if (weightings.isEmpty()) {
            throw new EntityNotFoundException("Expense", tripId.toString());
        }
        final Expense expense = Expense.create(tenantId, tripId, weightings);
        expenseRepository.save(expense);
        publishEvents(expense);
        LOG.info("Created expense {} on demand for trip {}", expense.expenseId().value(), tripId);
        return expense;
    }

    private List<Expense.PartyInfo> buildPartyInfos(final TripProjection projection) {
        final LinkedHashMap<UUID, String> partyMap = new LinkedHashMap<>();
        for (final TripParticipant p : projection.participants()) {
            if (p.hasPartyInfo()) {
                partyMap.putIfAbsent(p.partyTenantId(), p.partyName());
            }
        }
        return partyMap.entrySet().stream()
            .map(e -> new Expense.PartyInfo(e.getKey(), e.getValue()))
            .toList();
    }

    private ExpenseRepresentation toRepresentation(final Expense expense, final TripProjection projection) {
        return ExpenseRepresentation.from(
            expense,
            projection.startDate(),
            projection.endDate(),
            projection.accommodationTotalPrice(),
            projection.participants()
        );
    }

    private void synchronizeExpenseForParticipantJoin(final TripProjection projection,
                                                      final UUID participantId) {
        expenseRepository.findByTripId(projection.tenantId(), projection.tripId())
            .ifPresentOrElse(expense -> {
                final boolean alreadyPresent = expense.weightings().stream()
                    .anyMatch(weighting -> weighting.participantId().equals(participantId));
                if (!alreadyPresent) {
                    final TripParticipant participant = projection.participants().stream()
                        .filter(candidate -> candidate.participantId().equals(participantId))
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("TripParticipant", participantId.toString()));
                    expense.updateWeighting(participantId, defaultWeightingFor(participant, projection.startDate()));
                    expenseRepository.save(expense);
                    LOG.info("Added default weighting for participant {} in trip {}", participantId, projection.tripId());
                }
            }, () -> {
                final List<ParticipantWeighting> weightings = projection.participants().stream()
                    .map(p -> new ParticipantWeighting(p.participantId(), defaultWeightingFor(p, projection.startDate())))
                    .toList();
                final Expense expense = Expense.create(
                    projection.tenantId(), projection.tripId(), weightings
                );
                expenseRepository.save(expense);
                publishEvents(expense);
                LOG.info("Created expense {} for active trip {}", expense.expenseId().value(), projection.tripId());
            });
    }

    private BigDecimal defaultWeightingFor(final TripParticipant participant, final LocalDate tripStartDate) {
        final Integer age = participant.ageOn(tripStartDate);
        if (age == null) {
            return BigDecimal.ONE;
        }
        if (age < 3) {
            return BigDecimal.ZERO;
        }
        if (age <= 16) {
            return new BigDecimal("0.5");
        }
        return BigDecimal.ONE;
    }

    private void publishEvents(final Expense expense) {
        for (final DomainEvent event : expense.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        expense.clearDomainEvents();
    }
}
