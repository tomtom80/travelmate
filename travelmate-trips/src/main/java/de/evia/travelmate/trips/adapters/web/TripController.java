package de.evia.travelmate.trips.adapters.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import de.evia.travelmate.common.domain.TenantId;

import de.evia.travelmate.trips.application.AccommodationPollService;
import de.evia.travelmate.trips.application.AccommodationService;
import de.evia.travelmate.trips.application.DatePollService;
import de.evia.travelmate.trips.application.InvitationService;
import de.evia.travelmate.trips.application.MealPlanService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.CreateTripCommand;
import de.evia.travelmate.trips.application.command.EditTripCommand;
import de.evia.travelmate.trips.application.command.AddParticipantToTripCommand;
import de.evia.travelmate.trips.application.command.GrantTripOrganizerCommand;
import de.evia.travelmate.trips.application.command.InviteExternalCommand;
import de.evia.travelmate.trips.application.command.InviteParticipantCommand;
import de.evia.travelmate.trips.application.command.RemoveParticipantFromTripCommand;
import de.evia.travelmate.trips.application.command.SetStayPeriodCommand;
import de.evia.travelmate.trips.application.representation.InvitationRepresentation;
import de.evia.travelmate.trips.application.representation.InvitationView;
import de.evia.travelmate.trips.application.representation.PartyParticipantOption;
import de.evia.travelmate.trips.application.representation.ParticipantView;
import de.evia.travelmate.trips.application.representation.PendingInvitationView;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.invitation.InvitationId;
import de.evia.travelmate.trips.domain.travelparty.Member;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyDependent;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;
import de.evia.travelmate.trips.domain.trip.TripId;

@Controller
public class TripController {

    private final TripService tripService;
    private final InvitationService invitationService;
    private final MealPlanService mealPlanService;
    private final AccommodationService accommodationService;
    private final DatePollService datePollService;
    private final AccommodationPollService accommodationPollService;
    private final TravelPartyRepository travelPartyRepository;
    private final MessageSource messageSource;

    public TripController(final TripService tripService,
                          final InvitationService invitationService,
                          final MealPlanService mealPlanService,
                          final AccommodationService accommodationService,
                          final DatePollService datePollService,
                          final AccommodationPollService accommodationPollService,
                          final TravelPartyRepository travelPartyRepository,
                          final MessageSource messageSource) {
        this.tripService = tripService;
        this.invitationService = invitationService;
        this.mealPlanService = mealPlanService;
        this.accommodationService = accommodationService;
        this.datePollService = datePollService;
        this.accommodationPollService = accommodationPollService;
        this.travelPartyRepository = travelPartyRepository;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal final Jwt jwt, final Model model) {
        final Optional<ResolvedIdentity> maybeIdentity = resolveIdentity(jwt);
        model.addAttribute("view", "trip/list");
        model.addAttribute("trips", maybeIdentity
            .map(this::mergeTrips)
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
        validateTripAccess(trip, identity);

        final TravelParty party = identity.party();

        final List<InvitationRepresentation> invitations = invitationService.findByTripId(new TripId(tripId));
        final List<InvitationView> invitationViews = toInvitationViews(invitations);

        final List<UUID> alreadyInvitedOrParticipant = new java.util.ArrayList<>(
            invitations.stream().map(InvitationRepresentation::inviteeId).toList()
        );
        alreadyInvitedOrParticipant.addAll(trip.participantIds());

        final List<Member> invitableMembers = party.members().stream()
            .filter(m -> !alreadyInvitedOrParticipant.contains(m.memberId()))
            .toList();

        final List<ParticipantView> participantViews = toParticipantViews(trip, party);
        final List<PartyParticipantOption> availableOwnPartyParticipants = toAvailableOwnPartyParticipants(trip, party);

        model.addAttribute("view", "trip/detail");
        model.addAttribute("trip", trip);
        model.addAttribute("participants", participantViews);
        model.addAttribute("availableOwnPartyParticipants", availableOwnPartyParticipants);
        model.addAttribute("invitations", invitationViews);
        model.addAttribute("invitableMembers", invitableMembers);
        model.addAttribute("currentMemberId", identity.memberId());
        model.addAttribute("isOrganizer", trip.isOrganizer(identity.memberId()));
        model.addAttribute("hasMealPlan", mealPlanService.existsByTripId(new TripId(tripId)));
        model.addAttribute("hasAccommodation", accommodationService.existsByTripId(new TripId(tripId)));
        model.addAttribute("hasDatePoll", datePollService.existsOpenByTripId(new TenantId(trip.tenantId()), new TripId(tripId)));
        model.addAttribute("hasAccommodationPoll", accommodationPollService.existsOpenByTripId(new TenantId(trip.tenantId()), new TripId(tripId)));
        model.addAttribute("accommodationDecisionConfirmed", isAccommodationDecisionConfirmed(trip));
        return "layout/default";
    }

    @GetMapping("/invitations/{invitationId}")
    public String invitationLanding(@AuthenticationPrincipal final Jwt jwt,
                                    @PathVariable final UUID invitationId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final InvitationRepresentation invitation = invitationService.findById(new InvitationId(invitationId));
        if (!identity.memberId().equals(invitation.inviteeId())) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }
        return "redirect:/";
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
                         @RequestParam(required = false) final String description) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        tripService.createTrip(
            new CreateTripCommand(identity.tenantId().value(), name, description, identity.memberId())
        );
        return "redirect:/";
    }

    @GetMapping("/{tripId}/edit")
    public String editForm(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        validateTripAccess(trip, identity);
        model.addAttribute("view", "trip/edit");
        model.addAttribute("trip", trip);
        return "layout/default";
    }

    @PostMapping("/{tripId}/edit")
    public String edit(@AuthenticationPrincipal final Jwt jwt,
                       @PathVariable final UUID tripId,
                       @RequestParam final String name,
                       @RequestParam(required = false) final String description) {
        requireIdentity(jwt);
        tripService.editTrip(new EditTripCommand(tripId, name, description));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/delete")
    public String delete(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId) {
        requireIdentity(jwt);
        tripService.deleteTrip(new TripId(tripId));
        return "redirect:/";
    }

    @PostMapping("/{tripId}/invitations")
    public String invite(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @RequestParam final UUID inviteeId,
                         final HttpServletResponse response,
                         final Locale locale,
                         final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.invite(new InviteParticipantCommand(
            identity.tenantId().value(), tripId, inviteeId, identity.memberId()));
        triggerSuccessToast(response, messageSource.getMessage("invitation.memberInvited", null, locale));
        return populateInvitationFragment(tripId, identity, model);
    }

    @PostMapping("/{tripId}/invitations/external")
    public String inviteExternal(@AuthenticationPrincipal final Jwt jwt,
                                 @PathVariable final UUID tripId,
                                 @RequestParam final String email,
                                 @RequestParam final String firstName,
                                 @RequestParam final String lastName,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate dateOfBirth,
                                 final HttpServletResponse response,
                                 final Locale locale,
                                 final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.inviteExternal(new InviteExternalCommand(
            identity.tenantId().value(), tripId, email, firstName, lastName, dateOfBirth, identity.memberId()));
        triggerSuccessToast(response, messageSource.getMessage("invitation.externalInvited", null, locale));
        return populateInvitationFragment(tripId, identity, model);
    }

    @PostMapping("/{tripId}/invitations/{invitationId}/accept")
    public String accept(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @PathVariable final UUID invitationId,
                         final HttpServletRequest request,
                         final HttpServletResponse response,
        final Locale locale,
        final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.accept(new InvitationId(invitationId), identity.memberId());
        if (!isHtmxRequest(request)) {
            return "redirect:/";
        }
        triggerSuccessToast(response, messageSource.getMessage("invitation.accept", null, locale));
        return populateInvitationFragment(tripId, identity, model);
    }

    @PostMapping("/{tripId}/invitations/{invitationId}/decline")
    public String decline(@AuthenticationPrincipal final Jwt jwt,
                          @PathVariable final UUID tripId,
                          @PathVariable final UUID invitationId,
                          final HttpServletRequest request,
                          final HttpServletResponse response,
        final Locale locale,
        final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        invitationService.decline(new InvitationId(invitationId), identity.memberId());
        if (!isHtmxRequest(request)) {
            return "redirect:/";
        }
        triggerSuccessToast(response, messageSource.getMessage("invitation.decline", null, locale));
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
        final ResolvedIdentity identity = requireIdentity(jwt);
        tripService.setStayPeriod(new SetStayPeriodCommand(
            tripId, participantId, identity.memberId(), identity.tenantId().value(), arrivalDate, departureDate
        ));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/participants")
    public String addOwnParticipant(@AuthenticationPrincipal final Jwt jwt,
                                    @PathVariable final UUID tripId,
                                    @RequestParam final UUID participantId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        tripService.addParticipantToTrip(new AddParticipantToTripCommand(
            tripId, participantId, identity.memberId(), identity.tenantId().value()
        ));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/participants/{participantId}/remove")
    public String removeOwnParticipant(@AuthenticationPrincipal final Jwt jwt,
                                       @PathVariable final UUID tripId,
                                       @PathVariable final UUID participantId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        tripService.removeParticipantFromTrip(new RemoveParticipantFromTripCommand(
            tripId, participantId, identity.memberId(), identity.tenantId().value()
        ));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/organizers/{participantId}")
    public String grantOrganizer(@AuthenticationPrincipal final Jwt jwt,
                                 @PathVariable final UUID tripId,
                                 @PathVariable final UUID participantId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        tripService.grantTripOrganizer(new GrantTripOrganizerCommand(
            tripId, participantId, identity.memberId()
        ));
        return "redirect:/" + tripId;
    }

    @PostMapping("/{tripId}/organizers/{participantId}/revoke")
    public String revokeOrganizer(@AuthenticationPrincipal final Jwt jwt,
                                  @PathVariable final UUID tripId,
                                  @PathVariable final UUID participantId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        tripService.revokeTripOrganizer(new de.evia.travelmate.trips.application.command.RevokeTripOrganizerCommand(
            tripId, participantId, identity.memberId()
        ));
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

    private void validateTripAccess(final TripRepresentation trip, final ResolvedIdentity identity) {
        final boolean isTenantMember = trip.tenantId().equals(identity.tenantId().value());
        final boolean isParticipant = trip.participantIds().contains(identity.memberId());
        if (!isTenantMember && !isParticipant) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private List<TripRepresentation> mergeTrips(final ResolvedIdentity identity) {
        final List<TripRepresentation> tenantTrips = tripService.findAllByTenantId(identity.tenantId());
        final List<TripRepresentation> participantTrips = tripService.findAllByParticipantId(identity.memberId());
        final java.util.Set<UUID> seen = new java.util.LinkedHashSet<>();
        final List<TripRepresentation> merged = new java.util.ArrayList<>(tenantTrips);
        tenantTrips.forEach(t -> seen.add(t.tripId()));
        for (final TripRepresentation t : participantTrips) {
            if (seen.add(t.tripId())) {
                merged.add(t);
            }
        }
        return merged;
    }

    private String populateInvitationFragment(final UUID tripId, final ResolvedIdentity identity,
                                              final Model model) {
        final List<InvitationRepresentation> invitations = invitationService.findByTripId(new TripId(tripId));
        model.addAttribute("invitations", toInvitationViews(invitations));
        model.addAttribute("currentMemberId", identity.memberId());
        return "trip/invitations :: invitationList";
    }

    private List<ParticipantView> toParticipantViews(final TripRepresentation trip,
                                                    final TravelParty party) {
        final List<TravelParty> allParties = travelPartyRepository.findAll();
        final java.util.Set<UUID> accountHolderIds = allParties.stream()
            .flatMap(tp -> tp.members().stream())
            .map(Member::memberId)
            .collect(java.util.stream.Collectors.toSet());
        final java.util.Map<UUID, String> participantToPartyName = new java.util.HashMap<>();
        for (final TravelParty tp : allParties) {
            tp.members().forEach(m -> participantToPartyName.put(m.memberId(), tp.name()));
            tp.dependents().forEach(d -> participantToPartyName.put(d.dependentId(), tp.name()));
        }
        return trip.participantDetails().stream()
            .map(pd -> {
                final boolean manageableByCurrentParty = party.hasParticipant(pd.participantId());
                final boolean organizerEligible = accountHolderIds.contains(pd.participantId());
                final String partyName = participantToPartyName.getOrDefault(pd.participantId(), "");
                if (pd.firstName() != null) {
                    return new ParticipantView(pd.participantId(), pd.firstName(), pd.lastName(),
                        partyName, pd.arrivalDate(), pd.departureDate(), manageableByCurrentParty, organizerEligible);
                }
                final Member member = party.members().stream()
                    .filter(m -> m.memberId().equals(pd.participantId()))
                    .findFirst()
                    .orElse(null);
                if (member != null) {
                    return new ParticipantView(pd.participantId(), member.firstName(), member.lastName(),
                        partyName, pd.arrivalDate(), pd.departureDate(), true, true);
                }
                final TravelPartyDependent dependent = party.dependents().stream()
                    .filter(d -> d.dependentId().equals(pd.participantId()))
                    .findFirst()
                    .orElse(null);
                final String firstName = dependent != null ? dependent.firstName() : "Unknown";
                final String lastName = dependent != null ? dependent.lastName() : "";
                return new ParticipantView(pd.participantId(), firstName, lastName,
                    partyName, pd.arrivalDate(), pd.departureDate(), manageableByCurrentParty, organizerEligible);
            })
            .sorted(java.util.Comparator.comparing(ParticipantView::partyName)
                .thenComparing(ParticipantView::lastName)
                .thenComparing(ParticipantView::firstName))
            .toList();
    }

    private List<PartyParticipantOption> toAvailableOwnPartyParticipants(final TripRepresentation trip,
                                                                         final TravelParty party) {
        final java.util.Set<UUID> currentParticipants = new java.util.HashSet<>(trip.participantIds());
        final List<PartyParticipantOption> options = new java.util.ArrayList<>();
        party.members().stream()
            .filter(m -> !currentParticipants.contains(m.memberId()))
            .forEach(m -> options.add(new PartyParticipantOption(
                m.memberId(), m.firstName() + " " + m.lastName()
            )));
        party.dependents().stream()
            .filter(d -> !currentParticipants.contains(d.dependentId()))
            .forEach(d -> options.add(new PartyParticipantOption(
                d.dependentId(), d.firstName() + " " + d.lastName()
            )));
        return options;
    }

    private List<InvitationView> toInvitationViews(final List<InvitationRepresentation> invitations) {
        final List<TravelParty> allParties = travelPartyRepository.findAll();
        return invitations.stream()
            .map(inv -> {
                final String name;
                if ("EXTERNAL".equals(inv.invitationType())) {
                    name = inv.inviteeEmail() != null ? inv.inviteeEmail() : "Unknown";
                } else {
                    name = allParties.stream()
                        .flatMap(travelParty -> travelParty.members().stream())
                        .filter(member -> member.memberId().equals(inv.inviteeId()))
                        .findFirst()
                        .map(member -> member.firstName() + " " + member.lastName())
                        .orElse("Unknown");
                }
                final String targetPartyName = resolveInviteePartyName(inv.inviteeId(), inv.targetPartyTenantId());
                return new InvitationView(inv.invitationId(), inv.tripId(), inv.inviteeId(),
                    name, targetPartyName, inv.invitationType(), inv.status());
            })
            .toList();
    }

    private String resolveInviteePartyName(final UUID inviteeId,
                                          final UUID fallbackPartyTenantId) {
        if (inviteeId != null) {
            final var party = travelPartyRepository.findByMemberId(inviteeId);
            if (party.isPresent()) {
                return party.get().name();
            }
        }
        return resolvePartyName(fallbackPartyTenantId);
    }

    private String resolvePartyName(final UUID partyTenantId) {
        if (partyTenantId == null) {
            return null;
        }
        return travelPartyRepository.findByTenantId(new TenantId(partyTenantId))
            .map(TravelParty::name)
            .orElse(null);
    }

    private List<PendingInvitationView> toPendingInvitationViews(final ResolvedIdentity identity) {
        final List<InvitationRepresentation> pending = invitationService.findPendingByInviteeId(identity.memberId());
        final List<TravelParty> allParties = travelPartyRepository.findAll();
        return pending.stream()
            .map(inv -> {
                final TripRepresentation trip = tripService.findById(new TripId(inv.tripId()));
                final String inviterName = allParties.stream()
                    .flatMap(travelParty -> travelParty.members().stream())
                    .filter(member -> member.memberId().equals(inv.invitedBy()))
                    .findFirst()
                    .map(member -> member.firstName() + " " + member.lastName())
                    .orElse("Unknown");
                return new PendingInvitationView(
                    inv.invitationId(), inv.tripId(),
                    trip.name(), trip.startDate(), trip.endDate(),
                    inviterName
                );
            })
            .toList();
    }

    private void triggerSuccessToast(final HttpServletResponse response, final String message) {
        response.setHeader("HX-Trigger",
            "{\"showToast\":{\"level\":\"success\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}}");
    }

    private boolean isAccommodationDecisionConfirmed(final TripRepresentation trip) {
        try {
            return "BOOKED".equals(accommodationPollService.findLatestByTripId(
                new TenantId(trip.tenantId()),
                new TripId(trip.tripId())
            ).status());
        } catch (final Exception ignored) {
            return false;
        }
    }

    private boolean isHtmxRequest(final HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId, TravelParty party) {
    }
}
