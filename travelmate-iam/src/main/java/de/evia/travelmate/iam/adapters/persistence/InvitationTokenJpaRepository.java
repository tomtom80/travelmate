package de.evia.travelmate.iam.adapters.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationTokenJpaRepository extends JpaRepository<InvitationTokenJpaEntity, String> {

    Optional<InvitationTokenJpaEntity> findByTokenValue(String tokenValue);
}
