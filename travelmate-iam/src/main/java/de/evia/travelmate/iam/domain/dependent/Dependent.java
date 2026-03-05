package de.evia.travelmate.iam.domain.dependent;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.UUID;

import de.evia.travelmate.common.domain.AggregateRoot;
import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.common.events.iam.DependentAddedToTenant;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.FullName;

public class Dependent extends AggregateRoot {

    private final DependentId dependentId;
    private final TenantId tenantId;
    private final AccountId guardianAccountId;
    private final FullName fullName;
    private final LocalDate dateOfBirth;

    public Dependent(final DependentId dependentId,
                     final TenantId tenantId,
                     final AccountId guardianAccountId,
                     final FullName fullName,
                     final LocalDate dateOfBirth) {
        argumentIsNotNull(dependentId, "dependentId");
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotNull(guardianAccountId, "guardianAccountId");
        argumentIsNotNull(fullName, "fullName");
        this.dependentId = dependentId;
        this.tenantId = tenantId;
        this.guardianAccountId = guardianAccountId;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
    }

    public static Dependent add(final TenantId tenantId,
                                final AccountId guardianAccountId,
                                final FullName fullName,
                                final LocalDate dateOfBirth) {
        final Dependent dependent = new Dependent(
            new DependentId(UUID.randomUUID()),
            tenantId,
            guardianAccountId,
            fullName,
            dateOfBirth
        );
        dependent.registerEvent(new DependentAddedToTenant(
            tenantId.value(),
            dependent.dependentId.value(),
            guardianAccountId.value(),
            fullName.firstName(),
            fullName.lastName(),
            LocalDate.now()
        ));
        return dependent;
    }

    public static Dependent add(final TenantId tenantId,
                                final AccountId guardianAccountId,
                                final FullName fullName) {
        return add(tenantId, guardianAccountId, fullName, null);
    }

    public DependentId dependentId() {
        return dependentId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public AccountId guardianAccountId() {
        return guardianAccountId;
    }

    public FullName fullName() {
        return fullName;
    }

    public LocalDate dateOfBirth() {
        return dateOfBirth;
    }
}
