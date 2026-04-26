package de.evia.travelmate.trips.adapters.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.evia.travelmate.common.events.trips.AccommodationPriceSet;
import de.evia.travelmate.common.events.trips.ExternalUserInvitedToTrip;
import de.evia.travelmate.common.events.trips.OrganizerRoleGranted;
import de.evia.travelmate.common.events.trips.OrganizerRoleRevoked;
import de.evia.travelmate.common.events.trips.ParticipantJoinedTrip;
import de.evia.travelmate.common.events.trips.ParticipantRemovedFromTrip;
import de.evia.travelmate.common.events.trips.StayPeriodUpdated;
import de.evia.travelmate.common.events.trips.TripCompleted;
import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.common.events.trips.TripDeleted;
import de.evia.travelmate.common.messaging.RoutingKeys;

@Component
@Profile("!test")
public class DomainEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCreated(final TripCreated event) {
        publishSafely(RoutingKeys.TRIP_CREATED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantJoinedTrip(final ParticipantJoinedTrip event) {
        publishSafely(RoutingKeys.PARTICIPANT_CONFIRMED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantRemovedTrip(final ParticipantRemovedFromTrip event) {
        publishSafely(RoutingKeys.PARTICIPANT_REMOVED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCompleted(final TripCompleted event) {
        publishSafely(RoutingKeys.TRIP_COMPLETED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStayPeriodUpdated(final StayPeriodUpdated event) {
        publishSafely(RoutingKeys.STAY_PERIOD_UPDATED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalUserInvited(final ExternalUserInvitedToTrip event) {
        publishSafely(RoutingKeys.EXTERNAL_USER_INVITED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripDeleted(final TripDeleted event) {
        publishSafely(RoutingKeys.TRIP_DELETED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccommodationPriceSet(final AccommodationPriceSet event) {
        publishSafely(RoutingKeys.ACCOMMODATION_PRICE_SET, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganizerRoleGranted(final OrganizerRoleGranted event) {
        publishSafely(RoutingKeys.ORGANIZER_ROLE_GRANTED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrganizerRoleRevoked(final OrganizerRoleRevoked event) {
        publishSafely(RoutingKeys.ORGANIZER_ROLE_REVOKED, event);
    }

    private void publishSafely(final String routingKey, final Object event) {
        try {
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, event);
        } catch (final Exception ex) {
            LOG.error("Failed to publish {} with routing key {}: {}",
                event.getClass().getSimpleName(), routingKey, ex.getMessage(), ex);
        }
    }
}
