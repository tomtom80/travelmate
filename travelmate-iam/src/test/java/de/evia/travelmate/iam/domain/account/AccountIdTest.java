package de.evia.travelmate.iam.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AccountIdTest {

    @Test
    void createsWithValidUUID() {
        final UUID uuid = UUID.randomUUID();
        final AccountId accountId = new AccountId(uuid);
        assertThat(accountId.value()).isEqualTo(uuid);
    }

    @Test
    void throwsForNullUUID() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new AccountId(null))
            .withMessageContaining("accountId");
    }
}
