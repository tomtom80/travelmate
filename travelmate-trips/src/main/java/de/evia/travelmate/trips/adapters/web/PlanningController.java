package de.evia.travelmate.trips.adapters.web;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.AccommodationPollService;
import de.evia.travelmate.trips.application.DatePollService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.representation.AccommodationPollRepresentation;
import de.evia.travelmate.trips.application.representation.DatePollRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
public class PlanningController {

    private final TripService tripService;
    private final DatePollService datePollService;
    private final AccommodationPollService accommodationPollService;
    private final TravelPartyRepository travelPartyRepository;

    public PlanningController(final TripService tripService,
                              final DatePollService datePollService,
                              final AccommodationPollService accommodationPollService,
                              final TravelPartyRepository travelPartyRepository) {
        this.tripService = tripService;
        this.datePollService = datePollService;
        this.accommodationPollService = accommodationPollService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping("/{tripId}/planning")
    public String overview(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);

        DatePollRepresentation datePoll = null;
        try {
            datePoll = datePollService.findLatestByTripId(new TenantId(trip.tenantId()), new TripId(tripId));
        } catch (final Exception ignored) {
        }

        AccommodationPollRepresentation accommodationPoll = null;
        try {
            accommodationPoll = accommodationPollService.findLatestByTripId(new TenantId(trip.tenantId()), new TripId(tripId));
        } catch (final Exception ignored) {
        }

        model.addAttribute("view", "planning/overview");
        model.addAttribute("trip", trip);
        model.addAttribute("datePoll", datePoll);
        model.addAttribute("accommodationPoll", accommodationPoll);
        model.addAttribute("isOrganizer", trip.isOrganizer(identity.memberId()));
        return "layout/default";
    }

    private Optional<ResolvedIdentity> resolveIdentity(final Jwt jwt) {
        final String email = jwt.getClaimAsString("email");
        if (email == null) {
            return Optional.empty();
        }
        return travelPartyRepository.findByMemberEmail(email)
            .flatMap(party -> party.members().stream()
                .filter(m -> email.equals(m.email()))
                .findFirst()
                .map(m -> new ResolvedIdentity(party.tenantId(), m.memberId())));
    }

    private ResolvedIdentity requireIdentity(final Jwt jwt) {
        return resolveIdentity(jwt)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN, "No travel party found for user"));
    }

    private TripRepresentation validateTripAccess(final UUID tripId, final ResolvedIdentity identity) {
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final boolean isTenantMember = trip.tenantId().equals(identity.tenantId().value());
        final boolean isParticipant = trip.participantIds().contains(identity.memberId());
        if (!isTenantMember && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return trip;
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }
}
