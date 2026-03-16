package de.evia.travelmate.expense.adapters.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.StayPeriodUpdated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.expense.application.ExpenseService;

@Component
@Profile("!test")
public class TripEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TripEventConsumer.class);

    private final ExpenseService expenseService;
    private final Timer tripCreatedTimer;
    private final Timer participantJoinedTimer;
    private final Timer stayPeriodUpdatedTimer;
    private final Timer tripCompletedTimer;

    public TripEventConsumer(final ExpenseService expenseService, final MeterRegistry meterRegistry) {
        this.expenseService = expenseService;
        this.tripCreatedTimer = eventTimer(meterRegistry, "TripCreated");
        this.participantJoinedTimer = eventTimer(meterRegistry, "ParticipantJoinedTrip");
        this.stayPeriodUpdatedTimer = eventTimer(meterRegistry, "StayPeriodUpdated");
        this.tripCompletedTimer = eventTimer(meterRegistry, "TripCompleted");
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TRIP_CREATED)
    public void onTripCreated(final TripCreated event) {
        LOG.info("Received TripCreated for trip {} in tenant {}", event.tripId(), event.tenantId());
        tripCreatedTimer.record(() -> expenseService.onTripCreated(event));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PARTICIPANT_JOINED)
    public void onParticipantJoined(final ParticipantJoinedTrip event) {
        LOG.info("Received ParticipantJoinedTrip for trip {} participant {}",
            event.tripId(), event.participantId());
        participantJoinedTimer.record(() -> expenseService.onParticipantJoined(event));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_STAY_PERIOD_UPDATED)
    public void onStayPeriodUpdated(final StayPeriodUpdated event) {
        LOG.info("Received StayPeriodUpdated for trip {} participant {}",
            event.tripId(), event.participantId());
        stayPeriodUpdatedTimer.record(() -> expenseService.onStayPeriodUpdated(event));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TRIP_COMPLETED)
    public void onTripCompleted(final TripCompleted event) {
        LOG.info("Received TripCompleted for trip {} in tenant {}", event.tripId(), event.tenantId());
        tripCompletedTimer.record(() -> expenseService.onTripCompleted(event));
    }

    private static Timer eventTimer(final MeterRegistry registry, final String eventType) {
        return Timer.builder("travelmate.event.processing")
            .tag("scs", "expense")
            .tag("event", eventType)
            .description("Time spent processing domain events")
            .register(registry);
    }
}
