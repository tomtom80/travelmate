package de.evia.travelmate.trips.adapters.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
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

import de.evia.travelmate.common.events.trips.TripCreated;
import de.evia.travelmate.common.messaging.RoutingKeys;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DomainEventPublisher publisher;

    @Test
    void publishesTripCreatedEvent() {
        final TripCreated event = new TripCreated(
            UUID.randomUUID(), UUID.randomUUID(), "Skiurlaub",
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8), LocalDate.now()
        );

        publisher.onTripCreated(event);

        verify(rabbitTemplate).convertAndSend(
            RoutingKeys.EXCHANGE, RoutingKeys.TRIP_CREATED, event);
    }

    @Test
    void doesNotPropagateExceptionWhenRabbitMqFails() {
        final TripCreated event = new TripCreated(
            UUID.randomUUID(), UUID.randomUUID(), "Skiurlaub",
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 8), LocalDate.now()
        );

        doThrow(new AmqpException("Connection refused"))
            .when(rabbitTemplate).convertAndSend(
                eq(RoutingKeys.EXCHANGE), eq(RoutingKeys.TRIP_CREATED), eq(event));

        assertThatCode(() -> publisher.onTripCreated(event))
            .doesNotThrowAnyException();
    }
}
