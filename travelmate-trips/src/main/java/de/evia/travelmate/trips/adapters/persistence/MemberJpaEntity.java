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
@Table(name = "travel_party_member")
public class MemberJpaEntity {

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TravelPartyJpaEntity travelParty;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    protected MemberJpaEntity() {
    }

    public MemberJpaEntity(final UUID memberId, final TravelPartyJpaEntity travelParty,
                           final String email, final String firstName, final String lastName) {
        this.memberId = memberId;
        this.travelParty = travelParty;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
