package de.evia.travelmate.iam.domain.dependent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class DependentIdTest {

    @Test
    void createsWithValidUUID() {
        final UUID uuid = UUID.randomUUID();
        final DependentId dependentId = new DependentId(uuid);
        assertThat(dependentId.value()).isEqualTo(uuid);
    }

    @Test
    void throwsForNullUUID() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new DependentId(null))
            .withMessageContaining("dependentId");
    }
}
