package de.evia.travelmate.iam.domain.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.evia.travelmate.iam.domain.account.AccountId;

class InvitationTokenTest {

    private static final AccountId ACCOUNT_ID = new AccountId(UUID.randomUUID());

    @Test
    void generateSetsExpiryTo72Hours() {
        final InvitationToken token = InvitationToken.generate(ACCOUNT_ID);

        assertThat(token.tokenValue()).isNotBlank();
        assertThat(token.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(token.expiresAt()).isAfter(LocalDateTime.now().plusHours(71));
        assertThat(token.expiresAt()).isBefore(LocalDateTime.now().plusHours(73));
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void useMarksTokenAsUsed() {
        final InvitationToken token = InvitationToken.generate(ACCOUNT_ID);

        token.use();

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void cannotUseExpiredToken() {
        final InvitationToken token = new InvitationToken(
            UUID.randomUUID().toString(),
            ACCOUNT_ID,
            LocalDateTime.now().minusHours(1),
            false
        );

        assertThatThrownBy(token::use)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void cannotUseAlreadyUsedToken() {
        final InvitationToken token = InvitationToken.generate(ACCOUNT_ID);
        token.use();

        assertThatThrownBy(token::use)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been used");
    }

    @Test
    void isExpiredReturnsTrueForPastExpiry() {
        final InvitationToken token = new InvitationToken(
            UUID.randomUUID().toString(),
            ACCOUNT_ID,
            LocalDateTime.now().minusMinutes(1),
            false
        );

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void isExpiredReturnsFalseForFutureExpiry() {
        final InvitationToken token = InvitationToken.generate(ACCOUNT_ID);

        assertThat(token.isExpired()).isFalse();
    }
}
