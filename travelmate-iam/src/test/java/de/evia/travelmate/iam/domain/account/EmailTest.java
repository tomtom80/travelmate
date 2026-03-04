package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void createsWithValidEmail() {
        final Email email = new Email("user@example.com");
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    void throwsForNullValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Email(null))
            .withMessageContaining("email");
    }

    @Test
    void throwsForBlankValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Email(""))
            .withMessageContaining("email");
    }

    @Test
    void throwsForInvalidFormat() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Email("not-an-email"))
            .withMessageContaining("valid email");
    }

    @Test
    void acceptsEmailWithPlus() {
        final Email email = new Email("user+tag@example.com");
        assertThat(email.value()).isEqualTo("user+tag@example.com");
    }
}
