package de.evia.travelmate.trips.adapters.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import de.evia.travelmate.common.events.iam.AccountRegistered;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.trips.application.TravelPartyService;

@Component
@Profile("!test")
public class IamEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IamEventConsumer.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    private final TravelPartyService travelPartyService;
    private final Timer tenantCreatedTimer;
    private final Timer accountRegisteredTimer;
    private final Timer dependentAddedTimer;

    public IamEventConsumer(final TravelPartyService travelPartyService, final MeterRegistry meterRegistry) {
        this.travelPartyService = travelPartyService;
        this.tenantCreatedTimer = eventTimer(meterRegistry, "TenantCreated");
        this.accountRegisteredTimer = eventTimer(meterRegistry, "AccountRegistered");
        this.dependentAddedTimer = eventTimer(meterRegistry, "DependentAddedToTenant");
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TENANT_CREATED)
    public void onTenantCreated(final TenantCreated event) {
        tenantCreatedTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onTenantCreated(event), "TenantCreated", event.tenantId().toString()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ACCOUNT_REGISTERED)
    public void onAccountRegistered(final AccountRegistered event) {
        accountRegisteredTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onAccountRegistered(event), "AccountRegistered", event.tenantId().toString()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_DEPENDENT_ADDED)
    public void onDependentAdded(final DependentAddedToTenant event) {
        dependentAddedTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onDependentAdded(event), "DependentAdded", event.tenantId().toString()));
    }

    private void executeWithRetry(final Runnable action, final String eventType, final String tenantId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                action.run();
                return;
            } catch (final DataIntegrityViolationException e) {
                LOG.info("Concurrent TravelParty modification for tenant {} on {} (attempt {}/{})",
                    tenantId, eventType, attempt, MAX_RETRIES);
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private static Timer eventTimer(final MeterRegistry registry, final String eventType) {
        return Timer.builder("travelmate.event.processing")
            .tag("scs", "trips")
            .tag("event", eventType)
            .description("Time spent processing domain events")
            .register(registry);
    }
}
