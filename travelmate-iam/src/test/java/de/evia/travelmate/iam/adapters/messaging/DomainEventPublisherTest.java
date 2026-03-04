package de.evia.travelmate.iam.adapters.messaging;

import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.messaging.RoutingKeys;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DomainEventPublisher publisher;

    @Test
    void publishesAccountRegisteredEvent() {
        final AccountRegistered event = new AccountRegistered(
            UUID.randomUUID(), UUID.randomUUID(), "testuser",
            "Max", "Mustermann", "test@example.com", LocalDate.now()
        );

        publisher.onAccountRegistered(event);

        verify(rabbitTemplate).convertAndSend(
            RoutingKeys.EXCHANGE, RoutingKeys.ACCOUNT_REGISTERED, event);
    }

    @Test
    void publishesDependentAddedToTenantEvent() {
        final DependentAddedToTenant event = new DependentAddedToTenant(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "Lena", "Mustermann", LocalDate.now()
        );

        publisher.onDependentAddedToTenant(event);

        verify(rabbitTemplate).convertAndSend(
            RoutingKeys.EXCHANGE, RoutingKeys.DEPENDENT_ADDED, event);
    }
}
