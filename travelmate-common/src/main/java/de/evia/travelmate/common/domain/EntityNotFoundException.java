package de.evia.travelmate.common.domain;

public class EntityNotFoundException extends RuntimeException {

    private final String entityType;
    private final String entityId;

    public EntityNotFoundException(final String entityType, final String entityId) {
        super(entityType + " not found: " + entityId);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String entityType() {
        return entityType;
    }

    public String entityId() {
        return entityId;
    }
}
