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

    @Test
    void rejectsFutureDate() {
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new DateOfBirth(tomorrow))
            .withMessageContaining("future");
    }

    @Test
    void rejectsDateMoreThan150YearsAgo() {
        final LocalDate tooOld = LocalDate.now().minusYears(150).minusDays(1);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new DateOfBirth(tooOld))
            .withMessageContaining("150");
    }

    @Test
    void acceptsTodayAsDateOfBirth() {
        final LocalDate today = LocalDate.now();
        final DateOfBirth dateOfBirth = new DateOfBirth(today);
        assertThat(dateOfBirth.value()).isEqualTo(today);
    }

    @Test
    void accepts150YearsAgoExactly() {
        final LocalDate exactly150 = LocalDate.now().minusYears(150);
        final DateOfBirth dateOfBirth = new DateOfBirth(exactly150);
        assertThat(dateOfBirth.value()).isEqualTo(exactly150);
    }
}
