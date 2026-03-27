package de.evia.travelmate.expense.adapters.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.iam.TenantRenamed;
import de.evia.travelmate.expense.application.ExpenseService;

@Component
@Profile("!test")
public class IamEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IamEventConsumer.class);

    private final ExpenseService expenseService;
    private final Timer tenantRenamedTimer;

    public IamEventConsumer(final ExpenseService expenseService, final MeterRegistry meterRegistry) {
        this.expenseService = expenseService;
        this.tenantRenamedTimer = Timer.builder("travelmate.event.processing")
            .tag("scs", "expense")
            .tag("event", "TenantRenamed")
            .description("Time spent processing domain events")
            .register(meterRegistry);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TENANT_RENAMED)
    public void onTenantRenamed(final TenantRenamed event) {
        LOG.info("Received TenantRenamed for tenant {}", event.tenantId());
        tenantRenamedTimer.record(() -> expenseService.onTenantRenamed(event));
    }
}
