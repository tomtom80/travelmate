package de.evia.travelmate.common.domain;

public class BusinessRuleViolationException extends RuntimeException {

    private final String messageKey;

    public BusinessRuleViolationException(final String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
