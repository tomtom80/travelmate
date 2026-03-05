package de.evia.travelmate.iam.domain.account;

import java.util.List;
import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(AccountId accountId);

    Optional<Account> findByKeycloakUserId(TenantId tenantId, KeycloakUserId keycloakUserId);

    Optional<Account> findByKeycloakUserId(KeycloakUserId keycloakUserId);

    List<Account> findAllByTenantId(TenantId tenantId);

    boolean existsByUsername(TenantId tenantId, Username username);
}
