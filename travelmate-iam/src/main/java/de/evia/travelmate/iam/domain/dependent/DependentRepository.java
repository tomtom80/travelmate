package de.evia.travelmate.iam.domain.dependent;

import java.util.List;
import java.util.Optional;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.domain.account.AccountId;

public interface DependentRepository {

    Dependent save(Dependent dependent);

    Optional<Dependent> findById(DependentId dependentId);

    List<Dependent> findAllByGuardian(AccountId guardianAccountId);

    List<Dependent> findAllByTenantId(TenantId tenantId);
}
