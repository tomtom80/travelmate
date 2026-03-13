package de.evia.travelmate.iam.adapters.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.common.messaging.RoutingKeys;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DomainEventPublisher publisher;

    @Test
    void publishesTenantCreatedEvent() {
        final TenantCreated event = new TenantCreated(
            UUID.randomUUID(), "Hüttenurlaub 2026", LocalDate.now()
        );

        publisher.onTenantCreated(event);

        verify(rabbitTemplate).convertAndSend(
            RoutingKeys.EXCHANGE, RoutingKeys.TENANT_CREATED, event);
    }

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

    @Test
    void doesNotPropagateExceptionWhenRabbitMqFails() {
        final TenantCreated event = new TenantCreated(
            UUID.randomUUID(), "Hüttenurlaub 2026", LocalDate.now()
        );

        doThrow(new AmqpException("Connection refused"))
            .when(rabbitTemplate).convertAndSend(
                eq(RoutingKeys.EXCHANGE), eq(RoutingKeys.TENANT_CREATED), eq(event));

        assertThatCode(() -> publisher.onTenantCreated(event))
            .doesNotThrowAnyException();
    }
}
