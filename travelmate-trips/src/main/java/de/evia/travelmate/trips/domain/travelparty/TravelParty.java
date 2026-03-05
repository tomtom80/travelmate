package de.evia.travelmate.trips.domain.travelparty;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.evia.travelmate.common.domain.TenantId;

public class TravelParty {

    private final TenantId tenantId;
    private final String name;
    private final List<Member> members;
    private final List<TravelPartyDependent> dependents;

    public TravelParty(final TenantId tenantId,
                       final String name,
                       final List<Member> members,
                       final List<TravelPartyDependent> dependents) {
        argumentIsNotNull(tenantId, "tenantId");
        argumentIsNotBlank(name, "name");
        this.tenantId = tenantId;
        this.name = name;
        this.members = new ArrayList<>(members);
        this.dependents = new ArrayList<>(dependents);
    }

    public static TravelParty create(final TenantId tenantId, final String name) {
        return new TravelParty(tenantId, name, List.of(), List.of());
    }

    public void addMember(final UUID memberId, final String email,
                          final String firstName, final String lastName) {
        if (hasMember(memberId)) {
            throw new IllegalArgumentException("Member " + memberId + " already exists in travel party.");
        }
        members.add(new Member(memberId, email, firstName, lastName));
    }

    public void addDependent(final UUID dependentId, final UUID guardianMemberId,
                             final String firstName, final String lastName) {
        dependents.add(new TravelPartyDependent(dependentId, guardianMemberId, firstName, lastName));
    }

    public boolean hasMember(final UUID memberId) {
        return members.stream().anyMatch(m -> m.memberId().equals(memberId));
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String name() {
        return name;
    }

    public List<Member> members() {
        return Collections.unmodifiableList(members);
    }

    public List<TravelPartyDependent> dependents() {
        return Collections.unmodifiableList(dependents);
    }
}
