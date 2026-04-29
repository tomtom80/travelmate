package de.evia.travelmate.trips.adapters.messaging;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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
import de.evia.travelmate.common.events.iam.DependentRemovedFromTenant;
import de.evia.travelmate.common.events.iam.MemberRemovedFromTenant;
import de.evia.travelmate.common.events.iam.TenantCreated;
import de.evia.travelmate.common.events.iam.TenantRenamed;
import de.evia.travelmate.trips.application.ProjectionNotReadyException;
import de.evia.travelmate.trips.application.TravelPartyService;

@Component
@Profile("!test")
public class IamEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IamEventConsumer.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    private final TravelPartyService travelPartyService;
    private final ConcurrentHashMap<UUID, ReentrantLock> tenantLocks = new ConcurrentHashMap<>();
    private final Timer tenantCreatedTimer;
    private final Timer accountRegisteredTimer;
    private final Timer dependentAddedTimer;
    private final Timer memberRemovedTimer;
    private final Timer dependentRemovedTimer;
    private final Timer tenantRenamedTimer;

    public IamEventConsumer(final TravelPartyService travelPartyService, final MeterRegistry meterRegistry) {
        this.travelPartyService = travelPartyService;
        this.tenantCreatedTimer = eventTimer(meterRegistry, "TenantCreated");
        this.accountRegisteredTimer = eventTimer(meterRegistry, "AccountRegistered");
        this.dependentAddedTimer = eventTimer(meterRegistry, "DependentAddedToTenant");
        this.memberRemovedTimer = eventTimer(meterRegistry, "MemberRemovedFromTenant");
        this.dependentRemovedTimer = eventTimer(meterRegistry, "DependentRemovedFromTenant");
        this.tenantRenamedTimer = eventTimer(meterRegistry, "TenantRenamed");
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TENANT_CREATED)
    public void onTenantCreated(final TenantCreated event) {
        tenantCreatedTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onTenantCreated(event), "TenantCreated", event.tenantId()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ACCOUNT_REGISTERED)
    public void onAccountRegistered(final AccountRegistered event) {
        accountRegisteredTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onAccountRegistered(event), "AccountRegistered", event.tenantId()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_TENANT_RENAMED)
    public void onTenantRenamed(final TenantRenamed event) {
        tenantRenamedTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onTenantRenamed(event), "TenantRenamed", event.tenantId()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_DEPENDENT_ADDED)
    public void onDependentAdded(final DependentAddedToTenant event) {
        dependentAddedTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onDependentAdded(event), "DependentAdded", event.tenantId()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_MEMBER_REMOVED)
    public void onMemberRemoved(final MemberRemovedFromTenant event) {
        memberRemovedTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onMemberRemoved(event), "MemberRemoved", event.tenantId()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_DEPENDENT_REMOVED)
    public void onDependentRemoved(final DependentRemovedFromTenant event) {
        dependentRemovedTimer.record(() ->
            executeWithRetry(() -> travelPartyService.onDependentRemoved(event), "DependentRemoved", event.tenantId()));
    }

    private void executeWithRetry(final Runnable action, final String eventType, final UUID tenantId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                withTenantLock(tenantId, action);
                return;
            } catch (final DataIntegrityViolationException | ProjectionNotReadyException e) {
                LOG.info("TravelParty projection for tenant {} could not process {} yet (attempt {}/{}): {}",
                    tenantId, eventType, attempt, MAX_RETRIES, e.getMessage());
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

    private void withTenantLock(final UUID tenantId, final Runnable action) {
        final ReentrantLock lock = tenantLocks.computeIfAbsent(tenantId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
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
