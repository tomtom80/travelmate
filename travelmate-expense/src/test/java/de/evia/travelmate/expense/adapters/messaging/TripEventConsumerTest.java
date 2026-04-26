package de.evia.travelmate.expense.adapters.messaging;

import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.UUID;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.evia.travelmate.common.events.trips.TripDeleted;
import de.evia.travelmate.expense.application.ExpenseService;

@ExtendWith(MockitoExtension.class)
class TripEventConsumerTest {

    @Mock
    private ExpenseService expenseService;

    @Test
    void onTripDeletedDispatchesToService() {
        final TripEventConsumer consumer = new TripEventConsumer(expenseService, new SimpleMeterRegistry());
        final TripDeleted event = new TripDeleted(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());

        consumer.onTripDeleted(event);

        verify(expenseService).onTripDeleted(event);
    }
}
