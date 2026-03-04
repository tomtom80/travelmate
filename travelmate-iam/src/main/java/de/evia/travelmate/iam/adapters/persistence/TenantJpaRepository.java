package de.evia.travelmate.iam.adapters.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, UUID> {

    boolean existsByName(String name);
}
