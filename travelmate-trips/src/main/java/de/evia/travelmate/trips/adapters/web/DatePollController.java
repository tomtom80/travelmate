package de.evia.travelmate.trips.adapters.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.DatePollService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AddDateOptionCommand;
import de.evia.travelmate.trips.application.command.CastDateVoteCommand;
import de.evia.travelmate.trips.application.command.ConfirmDatePollCommand;
import de.evia.travelmate.trips.application.command.CreateDatePollCommand;
import de.evia.travelmate.trips.application.command.CreateDatePollCommand.DateRangeCommand;
import de.evia.travelmate.trips.application.command.RemoveDateOptionCommand;
import de.evia.travelmate.trips.application.representation.DatePollRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.datepoll.DatePollId;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
public class DatePollController {

    private final DatePollService datePollService;
    private final TripService tripService;
    private final TravelPartyRepository travelPartyRepository;

    public DatePollController(final DatePollService datePollService,
                              final TripService tripService,
                              final TravelPartyRepository travelPartyRepository) {
        this.datePollService = datePollService;
        this.tripService = tripService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping("/{tripId}/datepoll")
    public String overview(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);

        DatePollRepresentation datePoll = null;
        try {
            datePoll = datePollService.findLatestByTripId(tripTenantId(trip), new TripId(tripId));
        } catch (final Exception ignored) {
            // No poll exists yet
        }

        Set<UUID> currentVoteOptionIds = Set.of();
        if (datePoll != null) {
            currentVoteOptionIds = datePoll.votes().stream()
                .filter(v -> v.voterId().equals(identity.memberId()))
                .findFirst()
                .map(DatePollRepresentation.DateVoteRepresentation::selectedOptionIds)
                .orElse(Set.of());
        }

        model.addAttribute("view", "datepoll/overview");
        model.addAttribute("trip", trip);
        model.addAttribute("datePoll", datePoll);
        model.addAttribute("isOrganizer", trip.isOrganizer(identity.memberId()));
        model.addAttribute("currentMemberId", identity.memberId());
        model.addAttribute("currentVoteOptionIds", currentVoteOptionIds);
        return "layout/default";
    }

    @GetMapping("/{tripId}/datepoll/create")
    public String createForm(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId,
                             final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        model.addAttribute("view", "datepoll/create");
        model.addAttribute("trip", trip);
        return "layout/default";
    }

    @PostMapping("/{tripId}/datepoll/create")
    public String create(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @RequestParam("startDate") final List<LocalDate> startDates,
                         @RequestParam("endDate") final List<LocalDate> endDates) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        final List<DateRangeCommand> dateRanges = new java.util.ArrayList<>();
        for (int i = 0; i < startDates.size(); i++) {
            dateRanges.add(new DateRangeCommand(startDates.get(i), endDates.get(i)));
        }

        if (dateRanges.size() < 2) {
            return "redirect:/" + tripId + "/datepoll/create";
        }

        datePollService.createDatePoll(new CreateDatePollCommand(
            trip.tenantId(), tripId, dateRanges));

        return "redirect:/" + tripId + "/datepoll";
    }

    @PostMapping("/{tripId}/datepoll/{datePollId}/options/add")
    public String addOption(@AuthenticationPrincipal final Jwt jwt,
                            @PathVariable final UUID tripId,
                            @PathVariable final UUID datePollId,
                            @RequestParam final LocalDate startDate,
                            @RequestParam final LocalDate endDate) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        datePollService.addOption(new AddDateOptionCommand(
            trip.tenantId(), datePollId, startDate, endDate));

        return "redirect:/" + tripId + "/datepoll";
    }

    @PostMapping("/{tripId}/datepoll/{datePollId}/options/{optionId}/remove")
    public String removeOption(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID tripId,
                               @PathVariable final UUID datePollId,
                               @PathVariable final UUID optionId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        datePollService.removeOption(new RemoveDateOptionCommand(
            trip.tenantId(), datePollId, optionId));

        return "redirect:/" + tripId + "/datepoll";
    }

    @PostMapping("/{tripId}/datepoll/{datePollId}/vote")
    public String castVote(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           @PathVariable final UUID datePollId,
                           @RequestParam("selectedOptionIds") final Set<UUID> selectedOptionIds) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);

        datePollService.castVote(new CastDateVoteCommand(
            trip.tenantId(), datePollId, identity.memberId(), selectedOptionIds));

        return "redirect:/" + tripId + "/datepoll";
    }

    @PostMapping("/{tripId}/datepoll/{datePollId}/confirm")
    public String confirm(@AuthenticationPrincipal final Jwt jwt,
                          @PathVariable final UUID tripId,
                          @PathVariable final UUID datePollId,
                          @RequestParam final UUID confirmedOptionId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        datePollService.confirmPoll(new ConfirmDatePollCommand(
            trip.tenantId(), datePollId, confirmedOptionId));

        return "redirect:/" + tripId + "/datepoll";
    }

    @PostMapping("/{tripId}/datepoll/{datePollId}/cancel")
    public String cancel(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @PathVariable final UUID datePollId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        datePollService.cancelPoll(tripTenantId(trip), new DatePollId(datePollId));

        return "redirect:/" + tripId + "/datepoll";
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

    private void requireOrganizer(final TripRepresentation trip, final ResolvedIdentity identity) {
        if (!trip.isOrganizer(identity.memberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organizers can manage date polls");
        }
    }

    private TenantId tripTenantId(final TripRepresentation trip) {
        return new TenantId(trip.tenantId());
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }
}
