package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class FullNameTest {

    @Test
    void createsWithValidNames() {
        final FullName name = new FullName("Max", "Mustermann");
        assertThat(name.firstName()).isEqualTo("Max");
        assertThat(name.lastName()).isEqualTo("Mustermann");
    }

    @Test
    void throwsForNullFirstName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new FullName(null, "Mustermann"))
            .withMessageContaining("firstName");
    }

    @Test
    void throwsForNullLastName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new FullName("Max", null))
            .withMessageContaining("lastName");
    }

    @Test
    void throwsForBlankFirstName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new FullName("  ", "Mustermann"))
            .withMessageContaining("firstName");
    }

    @Test
    void throwsForBlankLastName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new FullName("Max", ""))
            .withMessageContaining("lastName");
    }
}
