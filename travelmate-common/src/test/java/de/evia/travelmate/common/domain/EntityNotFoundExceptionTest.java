package de.evia.travelmate.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EntityNotFoundExceptionTest {

    @Test
    void messageContainsEntityTypeAndId() {
        final var exception = new EntityNotFoundException("Account", "abc-123");

        assertThat(exception.getMessage()).isEqualTo("Account not found: abc-123");
        assertThat(exception.entityType()).isEqualTo("Account");
        assertThat(exception.entityId()).isEqualTo("abc-123");
    }

    @Test
    void duplicateEntityExceptionCarriesMessageKey() {
        final var exception = new DuplicateEntityException("member.error.alreadyExists");

        assertThat(exception.getMessage()).isEqualTo("member.error.alreadyExists");
        assertThat(exception.messageKey()).isEqualTo("member.error.alreadyExists");
    }

    @Test
    void businessRuleViolationExceptionCarriesMessageKey() {
        final var exception = new BusinessRuleViolationException("member.error.lastMember");

        assertThat(exception.getMessage()).isEqualTo("member.error.lastMember");
        assertThat(exception.messageKey()).isEqualTo("member.error.lastMember");
    }
}
