package de.evia.travelmate.trips.adapters.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.trips.application.InvitationService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.command.InviteParticipantCommand;
import de.evia.travelmate.trips.application.representation.InvitationRepresentation;
import de.evia.travelmate.trips.application.representation.InvitationView;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.invitation.InvitationId;
import de.evia.travelmate.trips.domain.travelparty.Member;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@Controller
@RequestMapping("/trips")
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
    public String list(@RequestParam final UUID tenantId, final Model model) {
        model.addAttribute("view", "trip/list");
        model.addAttribute("trips", tripService.findAllByTenantId(new TenantId(tenantId)));
        model.addAttribute("tenantId", tenantId);
        return "layout/default";
    }

    @GetMapping("/{tripId}")
    public String detail(@PathVariable final UUID tripId,
                         @RequestParam final UUID tenantId,
                         @RequestParam final UUID currentMemberId,
                         final Model model) {
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final TravelParty party = travelPartyRepository.findByTenantId(new TenantId(tenantId))
            .orElseThrow(() -> new IllegalStateException("TravelParty not found"));

        final List<InvitationRepresentation> invitations = invitationService.findByTripId(new TripId(tripId));
        final List<InvitationView> invitationViews = toInvitationViews(invitations, party);

        final List<UUID> alreadyInvitedOrParticipant = new java.util.ArrayList<>(
            invitations.stream().map(InvitationRepresentation::inviteeId).toList()
        );
        alreadyInvitedOrParticipant.addAll(trip.participantIds());

        final List<Member> invitableMembers = party.members().stream()
            .filter(m -> !alreadyInvitedOrParticipant.contains(m.memberId()))
            .toList();

        final List<Member> participants = party.members().stream()
            .filter(m -> trip.participantIds().contains(m.memberId()))
            .toList();

        model.addAttribute("view", "trip/detail");
        model.addAttribute("trip", trip);
        model.addAttribute("participants", participants);
        model.addAttribute("invitations", invitationViews);
        model.addAttribute("invitableMembers", invitableMembers);
        model.addAttribute("currentMemberId", currentMemberId);
        return "layout/default";
    }

    @GetMapping("/new")
    public String form(@RequestParam final UUID tenantId, final Model model) {
        model.addAttribute("view", "trip/form");
        model.addAttribute("tenantId", tenantId);
        return "layout/default";
    }

    @PostMapping
    public String create(@RequestParam final UUID tenantId,
                         @RequestParam final String name,
                         @RequestParam(required = false) final String description,
                         @RequestParam final LocalDate startDate,
                         @RequestParam final LocalDate endDate,
                         @RequestParam final UUID organizerId) {
        tripService.createTrip(
            new CreateTripCommand(tenantId, name, description, startDate, endDate, organizerId)
        );
        return "redirect:/trips?tenantId=" + tenantId;
    }

    @PostMapping("/{tripId}/invitations")
    public String invite(@PathVariable final UUID tripId,
                         @RequestParam final UUID tenantId,
                         @RequestParam final UUID inviteeId,
                         @RequestParam final UUID invitedBy,
                         final Model model) {
        invitationService.invite(new InviteParticipantCommand(tenantId, tripId, inviteeId, invitedBy));
        return populateInvitationFragment(tripId, tenantId, invitedBy, model);
    }

    @PostMapping("/{tripId}/invitations/{invitationId}/accept")
    public String accept(@PathVariable final UUID tripId,
                         @PathVariable final UUID invitationId,
                         @RequestParam final UUID tenantId,
                         @RequestParam final UUID currentMemberId,
                         final Model model) {
        invitationService.accept(new InvitationId(invitationId));
        return populateInvitationFragment(tripId, tenantId, currentMemberId, model);
    }

    @PostMapping("/{tripId}/invitations/{invitationId}/decline")
    public String decline(@PathVariable final UUID tripId,
                          @PathVariable final UUID invitationId,
                          @RequestParam final UUID tenantId,
                          @RequestParam final UUID currentMemberId,
                          final Model model) {
        invitationService.decline(new InvitationId(invitationId));
        return populateInvitationFragment(tripId, tenantId, currentMemberId, model);
    }

    private String populateInvitationFragment(final UUID tripId, final UUID tenantId,
                                              final UUID currentMemberId, final Model model) {
        final TravelParty party = travelPartyRepository.findByTenantId(new TenantId(tenantId))
            .orElseThrow(() -> new IllegalStateException("TravelParty not found"));

        final List<InvitationRepresentation> invitations = invitationService.findByTripId(new TripId(tripId));
        model.addAttribute("invitations", toInvitationViews(invitations, party));
        model.addAttribute("currentMemberId", currentMemberId);
        model.addAttribute("tenantId", tenantId);
        return "trip/invitations :: invitationList";
    }

    private List<InvitationView> toInvitationViews(final List<InvitationRepresentation> invitations,
                                                   final TravelParty party) {
        return invitations.stream()
            .map(inv -> {
                final String name = party.members().stream()
                    .filter(m -> m.memberId().equals(inv.inviteeId()))
                    .findFirst()
                    .map(m -> m.firstName() + " " + m.lastName())
                    .orElse("Unknown");
                return new InvitationView(inv.invitationId(), inv.tripId(), inv.inviteeId(), name, inv.status());
            })
            .toList();
    }
}
