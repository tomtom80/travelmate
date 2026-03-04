package de.evia.travelmate.iam.domain.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TenantNameTest {

    @Test
    void createsWithValidName() {
        final TenantName name = new TenantName("Reisegruppe Alpen");
        assertThat(name.value()).isEqualTo("Reisegruppe Alpen");
    }

    @Test
    void throwsForNullName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new TenantName(null))
            .withMessageContaining("tenantName");
    }

    @Test
    void throwsForBlankName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new TenantName("   "))
            .withMessageContaining("tenantName");
    }

    @Test
    void throwsForEmptyName() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new TenantName(""))
            .withMessageContaining("tenantName");
    }
}
