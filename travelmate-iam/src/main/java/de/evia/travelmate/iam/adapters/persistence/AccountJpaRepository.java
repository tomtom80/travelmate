package de.evia.travelmate.iam.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    Optional<AccountJpaEntity> findByTenantIdAndKeycloakUserId(UUID tenantId, String keycloakUserId);

    List<AccountJpaEntity> findAllByTenantId(UUID tenantId);

    boolean existsByTenantIdAndUsername(UUID tenantId, String username);
}
