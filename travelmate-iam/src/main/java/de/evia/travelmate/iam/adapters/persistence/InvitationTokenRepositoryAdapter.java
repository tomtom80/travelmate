package de.evia.travelmate.iam.adapters.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.registration.InvitationToken;
import de.evia.travelmate.iam.domain.registration.InvitationTokenRepository;

@Repository
public class InvitationTokenRepositoryAdapter implements InvitationTokenRepository {

    private final InvitationTokenJpaRepository jpaRepository;

    public InvitationTokenRepositoryAdapter(final InvitationTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public InvitationToken save(final InvitationToken token) {
        final InvitationTokenJpaEntity entity = toEntity(token);
        final InvitationTokenJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<InvitationToken> findByTokenValue(final String tokenValue) {
        return jpaRepository.findByTokenValue(tokenValue)
            .map(this::toDomain);
    }

    private InvitationTokenJpaEntity toEntity(final InvitationToken token) {
        return new InvitationTokenJpaEntity(
            token.tokenValue(),
            token.accountId().value(),
            token.expiresAt(),
            token.isUsed()
        );
    }

    private InvitationToken toDomain(final InvitationTokenJpaEntity entity) {
        return new InvitationToken(
            entity.getTokenValue(),
            new AccountId(entity.getAccountId()),
            entity.getExpiresAt(),
            entity.isUsed()
        );
    }
}
