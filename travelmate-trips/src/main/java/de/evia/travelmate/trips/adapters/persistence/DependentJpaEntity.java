package de.evia.travelmate.trips.adapters.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "travel_party_dependent")
public class DependentJpaEntity {

    @Id
    @Column(name = "dependent_id")
    private UUID dependentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TravelPartyJpaEntity travelParty;

    @Column(name = "guardian_member_id", nullable = false)
    private UUID guardianMemberId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    protected DependentJpaEntity() {
    }

    public DependentJpaEntity(final UUID dependentId, final TravelPartyJpaEntity travelParty,
                              final UUID guardianMemberId, final String firstName, final String lastName) {
        this.dependentId = dependentId;
        this.travelParty = travelParty;
        this.guardianMemberId = guardianMemberId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getDependentId() {
        return dependentId;
    }

    public UUID getGuardianMemberId() {
        return guardianMemberId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
