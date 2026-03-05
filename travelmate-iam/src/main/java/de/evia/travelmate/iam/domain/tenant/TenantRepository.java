package de.evia.travelmate.iam.domain.tenant;

import java.util.List;
import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;

public interface TenantRepository {

    Tenant save(Tenant tenant);

    Optional<Tenant> findById(TenantId tenantId);

    List<Tenant> findAll();

    boolean existsByName(TenantName name);

    void deleteById(TenantId tenantId);
}
