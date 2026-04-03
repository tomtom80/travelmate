package de.evia.travelmate.trips.adapters.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "travel_party")
public class TravelPartyJpaEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "travelParty", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MemberJpaEntity> members = new ArrayList<>();

    @OneToMany(mappedBy = "travelParty", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DependentJpaEntity> dependents = new ArrayList<>();

    protected TravelPartyJpaEntity() {
    }

    public TravelPartyJpaEntity(final UUID tenantId, final String name) {
        this.tenantId = tenantId;
        this.name = name;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<MemberJpaEntity> getMembers() {
        return members;
    }

    public List<DependentJpaEntity> getDependents() {
        return dependents;
    }
}
