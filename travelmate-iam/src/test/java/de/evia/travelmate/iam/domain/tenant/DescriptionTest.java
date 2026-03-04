package de.evia.travelmate.iam.domain.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DescriptionTest {

    @Test
    void createsWithValue() {
        final Description description = new Description("Eine Reisegruppe");
        assertThat(description.value()).isEqualTo("Eine Reisegruppe");
    }

    @Test
    void allowsNullValue() {
        final Description description = new Description(null);
        assertThat(description.value()).isNull();
    }
}
