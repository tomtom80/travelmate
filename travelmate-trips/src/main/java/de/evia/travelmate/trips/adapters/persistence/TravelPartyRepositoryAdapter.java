package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.travelparty.Member;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyDependent;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Repository
public class TravelPartyRepositoryAdapter implements TravelPartyRepository {

    private final TravelPartyJpaRepository jpaRepository;

    public TravelPartyRepositoryAdapter(final TravelPartyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TravelParty save(final TravelParty travelParty) {
        final TravelPartyJpaEntity entity = jpaRepository.findById(travelParty.tenantId().value())
            .orElseGet(() -> new TravelPartyJpaEntity(travelParty.tenantId().value(), travelParty.name()));

        syncMembers(entity, travelParty);
        syncDependents(entity, travelParty);

        jpaRepository.save(entity);
        return travelParty;
    }

    @Override
    public Optional<TravelParty> findByTenantId(final TenantId tenantId) {
        return jpaRepository.findById(tenantId.value()).map(this::toDomain);
    }

    @Override
    public Optional<TravelParty> findByMemberEmail(final String email) {
        return jpaRepository.findByMemberEmail(email).map(this::toDomain);
    }

    @Override
    public List<TravelParty> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private void syncMembers(final TravelPartyJpaEntity entity, final TravelParty domain) {
        for (final Member member : domain.members()) {
            final boolean exists = entity.getMembers().stream()
                .anyMatch(m -> m.getMemberId().equals(member.memberId()));
            if (!exists) {
                entity.getMembers().add(new MemberJpaEntity(
                    member.memberId(), entity,
                    member.email(), member.firstName(), member.lastName()
                ));
            }
        }
    }

    private void syncDependents(final TravelPartyJpaEntity entity, final TravelParty domain) {
        for (final TravelPartyDependent dep : domain.dependents()) {
            final boolean exists = entity.getDependents().stream()
                .anyMatch(d -> d.getDependentId().equals(dep.dependentId()));
            if (!exists) {
                entity.getDependents().add(new DependentJpaEntity(
                    dep.dependentId(), entity,
                    dep.guardianMemberId(), dep.firstName(), dep.lastName()
                ));
            }
        }
    }

    private TravelParty toDomain(final TravelPartyJpaEntity entity) {
        final var members = entity.getMembers().stream()
            .map(m -> new Member(m.getMemberId(), m.getEmail(), m.getFirstName(), m.getLastName()))
            .toList();
        final var dependents = entity.getDependents().stream()
            .map(d -> new TravelPartyDependent(d.getDependentId(), d.getGuardianMemberId(),
                d.getFirstName(), d.getLastName()))
            .toList();
        return new TravelParty(
            new TenantId(entity.getTenantId()),
            entity.getName(),
            members,
            dependents
        );
    }
}
