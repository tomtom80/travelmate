package de.evia.travelmate.iam.adapters.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invitation_token")
public class InvitationTokenJpaEntity {

    @Id
    @Column(name = "token_value")
    private String tokenValue;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    protected InvitationTokenJpaEntity() {
    }

    public InvitationTokenJpaEntity(final String tokenValue, final UUID accountId,
                                    final LocalDateTime expiresAt, final boolean used) {
        this.tokenValue = tokenValue;
        this.accountId = accountId;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(final String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(final UUID accountId) {
        this.accountId = accountId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(final LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(final boolean used) {
        this.used = used;
    }
}
