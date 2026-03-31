package de.evia.travelmate.trips.domain.travelparty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.evia.travelmate.common.domain.TenantId;

public interface TravelPartyRepository {

    TravelParty save(TravelParty travelParty);

    Optional<TravelParty> findByTenantId(TenantId tenantId);

    Optional<TravelParty> findByMemberEmail(String email);

    Optional<TravelParty> findByMemberId(UUID memberId);

    List<TravelParty> findAll();
}
