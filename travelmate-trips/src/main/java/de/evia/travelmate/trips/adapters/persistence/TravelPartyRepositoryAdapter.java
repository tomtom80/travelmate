package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        return jpaRepository.findAllByMemberEmail(email).stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<TravelParty> findByMemberId(final UUID memberId) {
        return jpaRepository.findByMemberId(memberId).map(this::toDomain);
    }

    @Override
    public List<TravelParty> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private void syncMembers(final TravelPartyJpaEntity entity, final TravelParty domain) {
        for (final Member member : domain.members()) {
            final var existing = entity.getMembers().stream()
                .filter(m -> m.getMemberId().equals(member.memberId()))
                .findFirst();
            if (existing.isPresent()) {
                entity.getMembers().remove(existing.get());
                entity.getMembers().add(new MemberJpaEntity(
                    member.memberId(), entity,
                    member.email(), member.firstName(), member.lastName(), member.dateOfBirth()
                ));
            } else {
                entity.getMembers().add(new MemberJpaEntity(
                    member.memberId(), entity,
                    member.email(), member.firstName(), member.lastName(), member.dateOfBirth()
                ));
            }
        }
    }

    private void syncDependents(final TravelPartyJpaEntity entity, final TravelParty domain) {
        for (final TravelPartyDependent dep : domain.dependents()) {
            final var existing = entity.getDependents().stream()
                .filter(d -> d.getDependentId().equals(dep.dependentId()))
                .findFirst();
            if (existing.isPresent()) {
                entity.getDependents().remove(existing.get());
                entity.getDependents().add(new DependentJpaEntity(
                    dep.dependentId(), entity,
                    dep.guardianMemberId(), dep.firstName(), dep.lastName(), dep.dateOfBirth()
                ));
            } else {
                entity.getDependents().add(new DependentJpaEntity(
                    dep.dependentId(), entity,
                    dep.guardianMemberId(), dep.firstName(), dep.lastName(), dep.dateOfBirth()
                ));
            }
        }
    }

    private TravelParty toDomain(final TravelPartyJpaEntity entity) {
        final var members = entity.getMembers().stream()
            .map(m -> new Member(m.getMemberId(), m.getEmail(), m.getFirstName(), m.getLastName(), m.getDateOfBirth()))
            .toList();
        final var dependents = entity.getDependents().stream()
            .map(d -> new TravelPartyDependent(d.getDependentId(), d.getGuardianMemberId(),
                d.getFirstName(), d.getLastName(), d.getDateOfBirth()))
            .toList();
        return new TravelParty(
            new TenantId(entity.getTenantId()),
            entity.getName(),
            members,
            dependents
        );
    }
}
