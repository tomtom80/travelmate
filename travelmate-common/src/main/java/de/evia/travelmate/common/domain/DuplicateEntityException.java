package de.evia.travelmate.common.domain;

public class DuplicateEntityException extends RuntimeException {

    private final String messageKey;

    public DuplicateEntityException(final String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
