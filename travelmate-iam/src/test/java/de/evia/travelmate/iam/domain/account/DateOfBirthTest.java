package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DateOfBirthTest {

    @Test
    void createsWithValidDate() {
        final LocalDate date = LocalDate.of(1990, 5, 15);
        final DateOfBirth dateOfBirth = new DateOfBirth(date);
        assertThat(dateOfBirth.value()).isEqualTo(date);
    }

    @Test
    void throwsForNullDate() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new DateOfBirth(null))
            .withMessageContaining("dateOfBirth");
    }
}
