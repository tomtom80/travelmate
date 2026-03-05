package de.evia.travelmate.iam.adapters.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.common.messaging.RoutingKeys;

@Component
@Profile("!test")
public class DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(final RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(final TenantCreated event) {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.TENANT_CREATED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountRegistered(final AccountRegistered event) {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.ACCOUNT_REGISTERED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDependentAddedToTenant(final DependentAddedToTenant event) {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.DEPENDENT_ADDED, event);
    }
}
