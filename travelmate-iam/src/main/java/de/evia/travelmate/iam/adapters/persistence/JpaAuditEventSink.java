package de.evia.travelmate.iam.adapters.persistence;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import de.evia.travelmate.webcommons.audit.AuditEvent;
import de.evia.travelmate.webcommons.audit.AuditEventSink;

@Service
@Profile("!test")
public class JpaAuditEventSink implements AuditEventSink {

    private final AuditLogJpaRepository auditLogJpaRepository;

    public JpaAuditEventSink(final AuditLogJpaRepository auditLogJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
    }

    @Override
    public void record(final AuditEvent event) {
        auditLogJpaRepository.save(AuditLogEntity.from(event));
    }
}
