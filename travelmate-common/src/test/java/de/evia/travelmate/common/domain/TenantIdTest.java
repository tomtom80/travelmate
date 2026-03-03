package de.evia.travelmate.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TenantIdTest {

    @Test
    void createsWithValidUUID() {
        final UUID uuid = UUID.randomUUID();
        final TenantId tenantId = new TenantId(uuid);
        assertThat(tenantId.value()).isEqualTo(uuid);
    }

    @Test
    void throwsForNullUUID() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new TenantId(null))
            .withMessageContaining("tenantId");
    }
}
