package de.evia.travelmate.trips.adapters.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.evia.travelmate.common.events.trips.ExternalUserInvitedToTrip;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.messaging.RoutingKeys;

@Component
@Profile("!test")
public class DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCreated(final TripCreated event) {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.TRIP_CREATED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantJoinedTrip(final ParticipantJoinedTrip event) {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.PARTICIPANT_CONFIRMED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCompleted(final TripCompleted event) {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.TRIP_COMPLETED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalUserInvited(final ExternalUserInvitedToTrip event) {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.EXTERNAL_USER_INVITED, event);
    }
}
