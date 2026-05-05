package de.evia.travelmate.iam.adapters.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.evia.travelmate.webcommons.audit.AuditEvent;
import de.evia.travelmate.webcommons.audit.AuditEventSink;

@Component
@Profile("test")
public class TestAuditSink implements AuditEventSink {

    private final List<AuditEvent> events = new ArrayList<>();

    @Override
    public void record(final AuditEvent event) {
        events.add(event);
    }

    public List<AuditEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public void clear() {
        events.clear();
    }
}
