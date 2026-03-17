package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeJpaRepository extends JpaRepository<RecipeJpaEntity, UUID> {

    List<RecipeJpaEntity> findAllByTenantId(UUID tenantId);
}
