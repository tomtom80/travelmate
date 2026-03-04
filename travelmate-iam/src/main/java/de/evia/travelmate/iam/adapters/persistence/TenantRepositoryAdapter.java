package de.evia.travelmate.iam.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.tenant.Description;
import de.evia.travelmate.iam.domain.tenant.Tenant;
import de.evia.travelmate.iam.domain.tenant.TenantName;
import de.evia.travelmate.iam.domain.tenant.TenantRepository;

@Repository
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository jpaRepository;

    public TenantRepositoryAdapter(final TenantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Tenant save(final Tenant tenant) {
        final TenantJpaEntity entity = toJpaEntity(tenant);
        jpaRepository.save(entity);
        return tenant;
    }

    @Override
    public Optional<Tenant> findById(final TenantId tenantId) {
        return jpaRepository.findById(tenantId.value()).map(this::toDomain);
    }

    @Override
    public List<Tenant> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByName(final TenantName name) {
        return jpaRepository.existsByName(name.value());
    }

    private TenantJpaEntity toJpaEntity(final Tenant tenant) {
        return new TenantJpaEntity(
            tenant.tenantId().value(),
            tenant.name().value(),
            tenant.description() != null ? tenant.description().value() : null
        );
    }

    private Tenant toDomain(final TenantJpaEntity entity) {
        return new Tenant(
            new TenantId(entity.getTenantId()),
            new TenantName(entity.getName()),
            entity.getDescription() != null ? new Description(entity.getDescription()) : null
        );
    }
}
