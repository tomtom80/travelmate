package de.evia.travelmate.iam.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.DateOfBirth;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Username;

@Repository
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryAdapter(final AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Account save(final Account account) {
        final AccountJpaEntity entity = toJpaEntity(account);
        jpaRepository.save(entity);
        return account;
    }

    @Override
    public Optional<Account> findById(final AccountId accountId) {
        return jpaRepository.findById(accountId.value()).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByKeycloakUserId(final TenantId tenantId, final KeycloakUserId keycloakUserId) {
        return jpaRepository.findByTenantIdAndKeycloakUserId(tenantId.value(), keycloakUserId.value())
            .map(this::toDomain);
    }

    @Override
    public Optional<Account> findByKeycloakUserId(final KeycloakUserId keycloakUserId) {
        return jpaRepository.findByKeycloakUserId(keycloakUserId.value())
            .map(this::toDomain);
    }

    @Override
    public List<Account> findAllByTenantId(final TenantId tenantId) {
        return jpaRepository.findAllByTenantId(tenantId.value()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean existsByUsername(final TenantId tenantId, final Username username) {
        return jpaRepository.existsByTenantIdAndUsername(tenantId.value(), username.value());
    }

    @Override
    public void deleteById(final AccountId accountId) {
        jpaRepository.deleteById(accountId.value());
    }

    @Override
    public void deleteAllByTenantId(final TenantId tenantId) {
        jpaRepository.deleteAllByTenantId(tenantId.value());
    }

    @Override
    public long countByTenantId(final TenantId tenantId) {
        return jpaRepository.countByTenantId(tenantId.value());
    }

    private AccountJpaEntity toJpaEntity(final Account account) {
        return new AccountJpaEntity(
            account.accountId().value(),
            account.tenantId().value(),
            account.keycloakUserId().value(),
            account.username().value(),
            account.email().value(),
            account.fullName().firstName(),
            account.fullName().lastName(),
            account.dateOfBirth().value()
        );
    }

    private Account toDomain(final AccountJpaEntity entity) {
        return new Account(
            new AccountId(entity.getAccountId()),
            new TenantId(entity.getTenantId()),
            new KeycloakUserId(entity.getKeycloakUserId()),
            new Username(entity.getUsername()),
            new Email(entity.getEmail()),
            new FullName(entity.getFirstName(), entity.getLastName()),
            new DateOfBirth(entity.getDateOfBirth())
        );
    }
}
