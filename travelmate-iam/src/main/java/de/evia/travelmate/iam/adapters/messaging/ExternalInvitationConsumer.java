package de.evia.travelmate.iam.adapters.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.trips.ExternalUserInvitedToTrip;

@Component
@Profile("!test")
public class ExternalInvitationConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalInvitationConsumer.class);

    private final ExistingAccountInviteRouter router;
    private final Timer externalUserInvitedTimer;

    public ExternalInvitationConsumer(final ExistingAccountInviteRouter router,
                                      final MeterRegistry meterRegistry) {
        this.router = router;
        this.externalUserInvitedTimer = Timer.builder("travelmate.event.processing")
            .tag("scs", "iam")
            .tag("event", "ExternalUserInvitedToTrip")
            .description("Time spent processing domain events")
            .register(meterRegistry);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_EXTERNAL_USER_INVITED)
    public void onExternalUserInvited(final ExternalUserInvitedToTrip event) {
        externalUserInvitedTimer.record(() -> processExternalUserInvited(event));
    }

    private void processExternalUserInvited(final ExternalUserInvitedToTrip event) {
        try {
            router.route(
                event.email(),
                event.firstName(),
                event.lastName(),
                event.dateOfBirth(),
                event.tripName(),
                event.tripId(),
                event.invitationId(),
                event.inviterFirstName(),
                event.inviterLastName()
            );
        } catch (final Exception e) {
            LOG.error("Failed to route external invitation for {}", event.email(), e);
            throw e;
        }
    }
}
