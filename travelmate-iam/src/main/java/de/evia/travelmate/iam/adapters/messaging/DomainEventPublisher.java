package de.evia.travelmate.iam.adapters.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.DependentRemovedFromTenant;
import de.evia.travelmate.common.events.iam.MemberAddedToTenant;
import de.evia.travelmate.common.events.iam.MemberRemovedFromTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.common.events.iam.TenantDeleted;
import de.evia.travelmate.common.events.iam.TenantRenamed;
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
    public void onTenantCreated(final TenantCreated event) {
        publishSafely(RoutingKeys.TENANT_CREATED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountRegistered(final AccountRegistered event) {
        publishSafely(RoutingKeys.ACCOUNT_REGISTERED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberAddedToTenant(final MemberAddedToTenant event) {
        publishSafely(RoutingKeys.MEMBER_ADDED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDependentAddedToTenant(final DependentAddedToTenant event) {
        publishSafely(RoutingKeys.DEPENDENT_ADDED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberRemovedFromTenant(final MemberRemovedFromTenant event) {
        publishSafely(RoutingKeys.MEMBER_REMOVED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDependentRemovedFromTenant(final DependentRemovedFromTenant event) {
        publishSafely(RoutingKeys.DEPENDENT_REMOVED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantRenamed(final TenantRenamed event) {
        publishSafely(RoutingKeys.TENANT_RENAMED, event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantDeleted(final TenantDeleted event) {
        publishSafely(RoutingKeys.TENANT_DELETED, event);
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
