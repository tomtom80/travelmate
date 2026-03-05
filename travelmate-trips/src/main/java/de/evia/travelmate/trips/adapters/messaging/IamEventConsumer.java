package de.evia.travelmate.trips.adapters.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.trips.application.TravelPartyService;

@Component
@Profile("!test")
public class IamEventConsumer {

    private final TravelPartyService travelPartyService;

    public IamEventConsumer(final TravelPartyService travelPartyService) {
        this.travelPartyService = travelPartyService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TENANT_CREATED)
    public void onTenantCreated(final TenantCreated event) {
        travelPartyService.onTenantCreated(event);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ACCOUNT_REGISTERED)
    public void onAccountRegistered(final AccountRegistered event) {
        travelPartyService.onAccountRegistered(event);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_DEPENDENT_ADDED)
    public void onDependentAdded(final DependentAddedToTenant event) {
        travelPartyService.onDependentAdded(event);
    }
}
