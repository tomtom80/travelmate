package de.evia.travelmate.trips.adapters.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import org.springframework.format.annotation.DateTimeFormat;

import de.evia.travelmate.trips.application.InvitationService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.command.InviteExternalCommand;
import de.evia.travelmate.trips.application.command.InviteParticipantCommand;
import de.evia.travelmate.trips.application.command.SetStayPeriodCommand;
import de.evia.travelmate.trips.application.representation.InvitationRepresentation;
import de.evia.travelmate.trips.application.representation.InvitationView;
import de.evia.travelmate.trips.application.representation.ParticipantView;
import de.evia.travelmate.trips.application.representation.PendingInvitationView;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.invitation.InvitationId;
import de.evia.travelmate.trips.domain.travelparty.Member;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@Controller
public class TripController {

    private final TripService tripService;
    private final InvitationService invitationService;
    private final TravelPartyRepository travelPartyRepository;

    public TripController(final TripService tripService,
                          final InvitationService invitationService,
                          final TravelPartyRepository travelPartyRepository) {
        this.tripService = tripService;
        this.invitationService = invitationService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal final Jwt jwt, final Model model) {
        final Optional<ResolvedIdentity> maybeIdentity = resolveIdentity(jwt);
        model.addAttribute("view", "trip/list");
        model.addAttribute("trips", maybeIdentity
            .map(id -> tripService.findAllByTenantId(id.tenantId()))
            .orElse(List.of()));
        model.addAttribute("pendingInvitations", maybeIdentity
            .map(id -> toPendingInvitationViews(id))
            .orElse(List.of()));
        return "layout/default";
    }

    @GetMapping("/{tripId}")
    public String detail(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        validateTenantAccess(trip.tenantId(), identity.tenantId());

        final TravelParty party = identity.party();

        final List<InvitationRepresentation> invitations = invitationService.findByTripId(new TripId(tripId));
        final List<InvitationView> invitationViews = toInvitationViews(invitations, party);

        final List<UUID> alreadyInvitedOrParticipant = new java.util.ArrayList<>(
            invitations.stream().map(InvitationRepresentation::inviteeId).toList()
        );
        alreadyInvitedOrParticipant.addAll(trip.participantIds());

        final List<Member> invitableMembers = party.members().stream()
            .filter(m -> !alreadyInvitedOrParticipant.contains(m.memberId()))
            .toList();

        final List<ParticipantView> participantViews = toParticipantViews(trip, party);

        model.addAttribute("view", "trip/detail");
        model.addAttribute("trip", trip);
        model.addAttribute("participants", participantViews);
        model.addAttribute("invitations", invitationViews);
        model.addAttribute("invitableMembers", invitableMembers);
        model.addAttribute("currentMemberId", identity.memberId());
        model.addAttribute("isOrganizer", trip.organizerId().equals(identity.memberId()));
        return "layout/default";
    }

    @GetMapping("/new")
    public String form(@AuthenticationPrincipal final Jwt jwt, final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        model.addAttribute("view", "trip/form");
        model.addAttribute("tenantId", identity.tenantId().value());
        model.addAttribute("organizerId", identity.memberId());
        return "layout/default";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal final Jwt jwt,
                         @RequestParam final String name,
                         @RequestParam(required = false) final String description,
                         @RequestParam final LocalDate startDate,
                         @RequestParam final LocalDate endDate) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        tripService.createTrip(
            new CreateTripCommand(identity.tenantId().value(), name, description, startDate, endDate, identity.memberId())
        );
        return "redirect:/";
    }

    @PostMapping("/{tripId}/invitations")
    public String invite(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @RequestParam final UUID inviteeId,
                         final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.invite(new InviteParticipantCommand(
            identity.tenantId().value(), tripId, inviteeId, identity.memberId()));
        return populateInvitationFragment(tripId, identity, model);
    }

    @PostMapping("/{tripId}/invitations/external")
    public String inviteExternal(@AuthenticationPrincipal final Jwt jwt,
                                 @PathVariable final UUID tripId,
                                 @RequestParam final String email,
                                 @RequestParam final String firstName,
                                 @RequestParam final String lastName,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateOfBirth,
                                 final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.inviteExternal(new InviteExternalCommand(
            identity.tenantId().value(), tripId, email, firstName, lastName, dateOfBirth, identity.memberId()));
        return populateInvitationFragment(tripId, identity, model);
    }

    @PostMapping("/{tripId}/invitations/{invitationId}/accept")
    public String accept(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @PathVariable final UUID invitationId,
                         final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.accept(new InvitationId(invitationId));
        return populateInvitationFragment(tripId, identity, model);
    }

    @PostMapping("/{tripId}/invitations/{invitationId}/decline")
    public String decline(@AuthenticationPrincipal final Jwt jwt,
                          @PathVariable final UUID tripId,
                          @PathVariable final UUID invitationId,
                          final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.decline(new InvitationId(invitationId));
        return populateInvitationFragment(tripId, identity, model);
    }

    @PostMapping("/{tripId}/confirm")
    public String confirm(@AuthenticationPrincipal final Jwt jwt,
                          @PathVariable final UUID tripId) {
        requireIdentity(jwt);
        tripService.confirmTrip(new TripId(tripId));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/start")
    public String start(@AuthenticationPrincipal final Jwt jwt,
                        @PathVariable final UUID tripId) {
        requireIdentity(jwt);
        tripService.startTrip(new TripId(tripId));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/complete")
    public String complete(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId) {
        requireIdentity(jwt);
        tripService.completeTrip(new TripId(tripId));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/cancel")
    public String cancel(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId) {
        requireIdentity(jwt);
        tripService.cancelTrip(new TripId(tripId));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/participants/{participantId}/stay-period")
    public String setStayPeriod(@AuthenticationPrincipal final Jwt jwt,
                                @PathVariable final UUID tripId,
                                @PathVariable final UUID participantId,
                                @RequestParam final LocalDate arrivalDate,
                                @RequestParam final LocalDate departureDate) {
        requireIdentity(jwt);
        tripService.setStayPeriod(new SetStayPeriodCommand(tripId, participantId, arrivalDate, departureDate));
        return "redirect:/" + tripId;
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
                .map(m -> new ResolvedIdentity(party.tenantId(), m.memberId(), party)));
    }

    private ResolvedIdentity requireIdentity(final Jwt jwt) {
        return resolveIdentity(jwt)
            .orElseThrow(() -> new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "No travel party found for user"));
    }

    private void validateTenantAccess(final UUID tripTenantId, final TenantId userTenantId) {
        if (!tripTenantId.equals(userTenantId.value())) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private String populateInvitationFragment(final UUID tripId, final ResolvedIdentity identity,
                                              final Model model) {
        final List<InvitationRepresentation> invitations = invitationService.findByTripId(new TripId(tripId));
        model.addAttribute("invitations", toInvitationViews(invitations, identity.party()));
        model.addAttribute("currentMemberId", identity.memberId());
        return "trip/invitations :: invitationList";
    }

    private List<ParticipantView> toParticipantViews(final TripRepresentation trip,
                                                    final TravelParty party) {
        return trip.participantDetails().stream()
            .map(pd -> {
                final Member member = party.members().stream()
                    .filter(m -> m.memberId().equals(pd.participantId()))
                    .findFirst()
                    .orElse(null);
                final String firstName = member != null ? member.firstName() : "Unknown";
                final String lastName = member != null ? member.lastName() : "";
                return new ParticipantView(pd.participantId(), firstName, lastName,
                    pd.arrivalDate(), pd.departureDate());
            })
            .toList();
    }

    private List<InvitationView> toInvitationViews(final List<InvitationRepresentation> invitations,
                                                   final TravelParty party) {
        return invitations.stream()
            .map(inv -> {
                final String name;
                if ("EXTERNAL".equals(inv.invitationType())) {
                    name = inv.inviteeEmail() != null ? inv.inviteeEmail() : "Unknown";
                } else {
                    name = party.members().stream()
                        .filter(m -> m.memberId().equals(inv.inviteeId()))
                        .findFirst()
                        .map(m -> m.firstName() + " " + m.lastName())
                        .orElse("Unknown");
                }
                return new InvitationView(inv.invitationId(), inv.tripId(), inv.inviteeId(),
                    name, inv.invitationType(), inv.status());
            })
            .toList();
    }

    private List<PendingInvitationView> toPendingInvitationViews(final ResolvedIdentity identity) {
        final List<InvitationRepresentation> pending = invitationService.findPendingByInviteeId(identity.memberId());
        return pending.stream()
            .map(inv -> {
                final TripRepresentation trip = tripService.findById(new TripId(inv.tripId()));
                final String inviterName = identity.party().members().stream()
                    .filter(m -> m.memberId().equals(inv.invitedBy()))
                    .findFirst()
                    .map(m -> m.firstName() + " " + m.lastName())
                    .orElse("Unknown");
                return new PendingInvitationView(
                    inv.invitationId(), inv.tripId(),
                    trip.name(), trip.startDate(), trip.endDate(),
                    inviterName
                );
            })
            .toList();
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId, TravelParty party) {
    }
}
