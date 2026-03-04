package de.evia.travelmate.iam.adapters.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant")
public class TenantJpaEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    protected TenantJpaEntity() {
    }

    public TenantJpaEntity(final UUID tenantId, final String name, final String description) {
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(final UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
