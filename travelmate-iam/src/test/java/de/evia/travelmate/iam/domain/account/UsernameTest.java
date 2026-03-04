package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class UsernameTest {

    @Test
    void createsWithValidValue() {
        final Username username = new Username("testuser");
        assertThat(username.value()).isEqualTo("testuser");
    }

    @Test
    void throwsForNullValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Username(null))
            .withMessageContaining("username");
    }

    @Test
    void throwsForBlankValue() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Username(""))
            .withMessageContaining("username");
    }
}
