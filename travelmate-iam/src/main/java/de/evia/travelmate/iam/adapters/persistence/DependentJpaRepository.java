package de.evia.travelmate.iam.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DependentJpaRepository extends JpaRepository<DependentJpaEntity, UUID> {

    List<DependentJpaEntity> findAllByGuardianAccountId(UUID guardianAccountId);

    List<DependentJpaEntity> findAllByTenantId(UUID tenantId);
}
