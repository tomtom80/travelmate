package de.evia.travelmate.trips.adapters.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.messaging.RoutingKeys;
import de.evia.travelmate.trips.application.TripParticipationEventPublisher;

@Component
public class RabbitMqTripParticipationEventPublisher implements TripParticipationEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMqTripParticipationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqTripParticipationEventPublisher(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishParticipantJoinedAfterCommit(final ParticipantJoinedTrip event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event);
                }
            });
            return;
        }
        publish(event);
    }

    private void publish(final ParticipantJoinedTrip event) {
        try {
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.PARTICIPANT_CONFIRMED, event);
        } catch (final Exception ex) {
            LOG.error("Failed to publish ParticipantJoinedTrip for trip {} participant {}: {}",
                event.tripId(), event.participantId(), ex.getMessage(), ex);
        }
    }
}
