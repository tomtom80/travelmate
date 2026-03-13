package de.evia.travelmate.iam.domain.registration;

import java.util.Optional;

public interface InvitationTokenRepository {

    InvitationToken save(InvitationToken token);

    Optional<InvitationToken> findByTokenValue(String tokenValue);
}
