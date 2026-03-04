package de.evia.travelmate.iam.adapters.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dependent")
public class DependentJpaEntity {

    @Id
    @Column(name = "dependent_id")
    private UUID dependentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "guardian_account_id", nullable = false)
    private UUID guardianAccountId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    protected DependentJpaEntity() {
    }

    public DependentJpaEntity(final UUID dependentId, final UUID tenantId, final UUID guardianAccountId,
                              final String firstName, final String lastName) {
        this.dependentId = dependentId;
        this.tenantId = tenantId;
        this.guardianAccountId = guardianAccountId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getDependentId() {
        return dependentId;
    }

    public void setDependentId(final UUID dependentId) {
        this.dependentId = dependentId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(final UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getGuardianAccountId() {
        return guardianAccountId;
    }

    public void setGuardianAccountId(final UUID guardianAccountId) {
        this.guardianAccountId = guardianAccountId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }
}
