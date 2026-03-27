package de.evia.travelmate.trips.adapters.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.domain.invitation.Invitation;
import de.evia.travelmate.trips.domain.invitation.InvitationId;
import de.evia.travelmate.trips.domain.invitation.InvitationRepository;
import de.evia.travelmate.trips.domain.invitation.InvitationStatus;
import de.evia.travelmate.trips.domain.invitation.InvitationType;
import de.evia.travelmate.trips.domain.trip.TripId;

@Repository
public class InvitationRepositoryAdapter implements InvitationRepository {

    private final InvitationJpaRepository jpaRepository;

    public InvitationRepositoryAdapter(final InvitationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Invitation save(final Invitation invitation) {
        final InvitationJpaEntity entity = jpaRepository.findById(invitation.invitationId().value())
            .orElseGet(() -> new InvitationJpaEntity(
                invitation.invitationId().value(),
                invitation.tenantId().value(),
                invitation.tripId().value(),
                invitation.inviteeId(),
                invitation.invitedBy(),
                invitation.inviteeEmail(),
                invitation.targetPartyTenantId(),
                invitation.invitationType().name(),
                invitation.status().name()
            ));
        entity.setStatus(invitation.status().name());
        entity.setInviteeId(invitation.inviteeId());
        entity.setTargetPartyTenantId(invitation.targetPartyTenantId());
        jpaRepository.save(entity);
        return invitation;
    }

    @Override
    public Optional<Invitation> findById(final InvitationId invitationId) {
        return jpaRepository.findById(invitationId.value()).map(this::toDomain);
    }

    @Override
    public List<Invitation> findByTripId(final TripId tripId) {
        return jpaRepository.findByTripId(tripId.value()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Invitation> findByInviteeIdAndStatus(final UUID inviteeId, final InvitationStatus status) {
        return jpaRepository.findByInviteeIdAndStatus(inviteeId, status.name()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Invitation> findByInviteeEmailAndStatus(final String inviteeEmail, final InvitationStatus status) {
        return jpaRepository.findByInviteeEmailAndStatus(inviteeEmail, status.name()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean existsByTripIdAndTargetPartyTenantIdInStatuses(final TripId tripId,
                                                                  final UUID targetPartyTenantId,
                                                                  final List<InvitationStatus> statuses) {
        return jpaRepository.existsByTripIdAndTargetPartyTenantIdAndStatusIn(
            tripId.value(),
            targetPartyTenantId,
            statuses.stream().map(InvitationStatus::name).toList()
        );
    }

    @Override
    public boolean existsByTripIdAndInviteeId(final TripId tripId, final UUID inviteeId) {
        return jpaRepository.existsByTripIdAndInviteeId(tripId.value(), inviteeId);
    }

    @Override
    public boolean existsByTripIdAndInviteeEmail(final TripId tripId, final String inviteeEmail) {
        return jpaRepository.existsByTripIdAndInviteeEmail(tripId.value(), inviteeEmail);
    }

    private Invitation toDomain(final InvitationJpaEntity entity) {
        return new Invitation(
            new InvitationId(entity.getInvitationId()),
            new TenantId(entity.getTenantId()),
            new TripId(entity.getTripId()),
            entity.getInviteeId(),
            entity.getInvitedBy(),
            entity.getInviteeEmail(),
            entity.getTargetPartyTenantId(),
            InvitationType.valueOf(entity.getInvitationType()),
            InvitationStatus.valueOf(entity.getStatus())
        );
    }
}
