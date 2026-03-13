package de.evia.travelmate.iam.domain.registration;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDateTime;
import java.util.UUID;

import de.evia.travelmate.iam.domain.account.AccountId;

public class InvitationToken {

    private static final int EXPIRY_HOURS = 72;

    private final String tokenValue;
    private final AccountId accountId;
    private final LocalDateTime expiresAt;
    private boolean used;

    public InvitationToken(final String tokenValue,
                           final AccountId accountId,
                           final LocalDateTime expiresAt,
                           final boolean used) {
        argumentIsNotBlank(tokenValue, "tokenValue");
        argumentIsNotNull(accountId, "accountId");
        argumentIsNotNull(expiresAt, "expiresAt");
        this.tokenValue = tokenValue;
        this.accountId = accountId;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    public static InvitationToken generate(final AccountId accountId) {
        return new InvitationToken(
            UUID.randomUUID().toString(),
            accountId,
            LocalDateTime.now().plusHours(EXPIRY_HOURS),
            false
        );
    }

    public void use() {
        if (used) {
            throw new IllegalStateException("This invitation token has already been used.");
        }
        if (isExpired()) {
            throw new IllegalStateException("This invitation token has expired.");
        }
        this.used = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String tokenValue() {
        return tokenValue;
    }

    public AccountId accountId() {
        return accountId;
    }

    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }
}
