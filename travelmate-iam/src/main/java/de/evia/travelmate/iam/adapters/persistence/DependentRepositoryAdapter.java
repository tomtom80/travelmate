package de.evia.travelmate.iam.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.DateOfBirth;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.dependent.Dependent;
import de.evia.travelmate.iam.domain.dependent.DependentId;
import de.evia.travelmate.iam.domain.dependent.DependentRepository;

@Repository
public class DependentRepositoryAdapter implements DependentRepository {

    private final DependentJpaRepository jpaRepository;

    public DependentRepositoryAdapter(final DependentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Dependent save(final Dependent dependent) {
        final DependentJpaEntity entity = toJpaEntity(dependent);
        jpaRepository.save(entity);
        return dependent;
    }

    @Override
    public Optional<Dependent> findById(final DependentId dependentId) {
        return jpaRepository.findById(dependentId.value()).map(this::toDomain);
    }

    @Override
    public List<Dependent> findAllByGuardian(final AccountId guardianAccountId) {
        return jpaRepository.findAllByGuardianAccountId(guardianAccountId.value()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Dependent> findAllByTenantId(final TenantId tenantId) {
        return jpaRepository.findAllByTenantId(tenantId.value()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public void deleteById(final DependentId dependentId) {
        jpaRepository.deleteById(dependentId.value());
    }

    @Override
    public void deleteAllByTenantId(final TenantId tenantId) {
        jpaRepository.deleteAllByTenantId(tenantId.value());
    }

    private DependentJpaEntity toJpaEntity(final Dependent dependent) {
        return new DependentJpaEntity(
            dependent.dependentId().value(),
            dependent.tenantId().value(),
            dependent.guardianAccountId().value(),
            dependent.fullName().firstName(),
            dependent.fullName().lastName(),
            dependent.dateOfBirth() != null ? dependent.dateOfBirth().value() : null
        );
    }

    private Dependent toDomain(final DependentJpaEntity entity) {
        return new Dependent(
            new DependentId(entity.getDependentId()),
            new TenantId(entity.getTenantId()),
            new AccountId(entity.getGuardianAccountId()),
            new FullName(entity.getFirstName(), entity.getLastName()),
            entity.getDateOfBirth() != null ? new DateOfBirth(entity.getDateOfBirth()) : null
        );
    }
}
