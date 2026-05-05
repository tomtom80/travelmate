package de.evia.travelmate.webcommons.audit;

public interface AuditEventSink {
    void record(AuditEvent event);
}
