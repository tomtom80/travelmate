package de.evia.travelmate.expense.adapters.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.expense.application.ExpenseService;

@Component
@Profile("!test")
public class TripEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TripEventConsumer.class);

    private final ExpenseService expenseService;

    public TripEventConsumer(final ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TRIP_CREATED)
    public void onTripCreated(final TripCreated event) {
        LOG.info("Received TripCreated for trip {} in tenant {}", event.tripId(), event.tenantId());
        expenseService.onTripCreated(event);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PARTICIPANT_JOINED)
    public void onParticipantJoined(final ParticipantJoinedTrip event) {
        LOG.info("Received ParticipantJoinedTrip for trip {} participant {}",
            event.tripId(), event.participantId());
        expenseService.onParticipantJoined(event);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TRIP_COMPLETED)
    public void onTripCompleted(final TripCompleted event) {
        LOG.info("Received TripCompleted for trip {} in tenant {}", event.tripId(), event.tenantId());
        expenseService.onTripCompleted(event);
    }
}
