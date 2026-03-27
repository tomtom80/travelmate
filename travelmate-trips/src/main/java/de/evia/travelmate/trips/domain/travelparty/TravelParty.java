package de.evia.travelmate.trips.domain.travelparty;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotBlank;
import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.evia.travelmate.common.domain.TenantId;

public class TravelParty {

    private final TenantId tenantId;
    private String name;
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

    public void updateName(final String name) {
        argumentIsNotBlank(name, "name");
        this.name = name;
    }

    public void addMember(final UUID memberId, final String email,
                          final String firstName, final String lastName) {
        addMember(memberId, email, firstName, lastName, null);
    }

    public void addMember(final UUID memberId, final String email,
                          final String firstName, final String lastName,
                          final LocalDate dateOfBirth) {
        if (hasMember(memberId)) {
            throw new IllegalArgumentException("Member " + memberId + " already exists in travel party.");
        }
        members.add(new Member(memberId, email, firstName, lastName, dateOfBirth));
    }

    public void addDependent(final UUID dependentId, final UUID guardianMemberId,
                             final String firstName, final String lastName) {
        addDependent(dependentId, guardianMemberId, firstName, lastName, null);
    }

    public void addDependent(final UUID dependentId, final UUID guardianMemberId,
                             final String firstName, final String lastName,
                             final LocalDate dateOfBirth) {
        dependents.add(new TravelPartyDependent(dependentId, guardianMemberId, firstName, lastName, dateOfBirth));
    }

    public void removeMember(final UUID memberId) {
        final boolean removed = members.removeIf(member -> member.memberId().equals(memberId));
        if (!removed) {
            throw new IllegalArgumentException("Member " + memberId + " not found in travel party.");
        }
    }

    public void removeDependent(final UUID dependentId) {
        final boolean removed = dependents.removeIf(dependent -> dependent.dependentId().equals(dependentId));
        if (!removed) {
            throw new IllegalArgumentException("Dependent " + dependentId + " not found in travel party.");
        }
    }

    public boolean hasMember(final UUID memberId) {
        return members.stream().anyMatch(m -> m.memberId().equals(memberId));
    }

    public boolean hasParticipant(final UUID participantId) {
        return hasMember(participantId)
            || dependents.stream().anyMatch(d -> d.dependentId().equals(participantId));
    }

    public Optional<Member> findMember(final UUID memberId) {
        return members.stream().filter(member -> member.memberId().equals(memberId)).findFirst();
    }

    public Optional<TravelPartyDependent> findDependent(final UUID dependentId) {
        return dependents.stream().filter(dependent -> dependent.dependentId().equals(dependentId)).findFirst();
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
