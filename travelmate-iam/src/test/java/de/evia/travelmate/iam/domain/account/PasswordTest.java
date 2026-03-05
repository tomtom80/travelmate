package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PasswordTest {

    @Test
    void createsWithValidPassword() {
        final Password password = new Password("secureP4ss!");
        assertThat(password.value()).isEqualTo("secureP4ss!");
    }

    @Test
    void throwsForNullValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Password(null))
            .withMessageContaining("password");
    }

    @Test
    void throwsForBlankValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Password(""))
            .withMessageContaining("password");
    }

    @Test
    void throwsForTooShortPassword() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Password("short"))
            .withMessageContaining("at least 8 characters");
    }

    @Test
    void acceptsExactlyEightCharacters() {
        final Password password = new Password("12345678");
        assertThat(password.value()).isEqualTo("12345678");
    }
}
