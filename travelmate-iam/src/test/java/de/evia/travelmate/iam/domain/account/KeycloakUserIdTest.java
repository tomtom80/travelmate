package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class KeycloakUserIdTest {

    @Test
    void createsWithValidValue() {
        final KeycloakUserId id = new KeycloakUserId("abc-123-def");
        assertThat(id.value()).isEqualTo("abc-123-def");
    }

    @Test
    void throwsForNullValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new KeycloakUserId(null))
            .withMessageContaining("keycloakUserId");
    }

    @Test
    void throwsForBlankValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new KeycloakUserId("  "))
            .withMessageContaining("keycloakUserId");
    }
}
