package de.evia.travelmate.trips.adapters.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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
import de.evia.travelmate.trips.application.AccommodationService;
import de.evia.travelmate.trips.application.TripService;
import de.evia.travelmate.trips.application.command.AddRoomCommand;
import de.evia.travelmate.trips.application.command.AssignPartyToRoomCommand;
import de.evia.travelmate.trips.application.command.RemoveRoomAssignmentCommand;
import de.evia.travelmate.trips.application.command.RemoveRoomCommand;
import de.evia.travelmate.trips.application.command.RoomCommand;
import de.evia.travelmate.trips.application.command.SetAccommodationCommand;
import de.evia.travelmate.trips.application.representation.AccommodationRepresentation;
import de.evia.travelmate.trips.application.representation.PartyOption;
import de.evia.travelmate.trips.application.representation.RoomAssignmentRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelParty;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
public class AccommodationController {

    private final AccommodationService accommodationService;
    private final TripService tripService;
    private final TravelPartyRepository travelPartyRepository;

    public AccommodationController(final AccommodationService accommodationService,
                                    final TripService tripService,
                                    final TravelPartyRepository travelPartyRepository) {
        this.accommodationService = accommodationService;
        this.tripService = tripService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping("/{tripId}/accommodation")
    public String overview(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        final Optional<AccommodationRepresentation> accommodation =
            accommodationService.findByTripId(new TripId(tripId));
        final boolean isOrganizer = trip.organizerId().equals(identity.memberId());
        final boolean isEditable = isOrganizer
            && !"COMPLETED".equals(trip.status())
            && !"CANCELLED".equals(trip.status());

        model.addAttribute("view", "accommodation/overview");
        model.addAttribute("trip", trip);
        model.addAttribute("accommodation", accommodation.orElse(null));
        model.addAttribute("isOrganizer", isOrganizer);
        model.addAttribute("isEditable", isEditable);

        if (accommodation.isPresent()) {
            final List<PartyOption> partyOptions = resolvePartyOptions(trip);
            model.addAttribute("partyOptions", partyOptions);

            final List<UUID> assignedPartyIds = accommodation.get().assignments().stream()
                .map(a -> a.partyTenantId())
                .distinct()
                .toList();
            final List<PartyOption> unassignedParties = partyOptions.stream()
                .filter(p -> !assignedPartyIds.contains(p.partyTenantId()))
                .toList();
            model.addAttribute("unassignedParties", unassignedParties);

            final Map<UUID, List<RoomAssignmentRepresentation>> assignmentsByRoom = new LinkedHashMap<>();
            final Map<UUID, Integer> assignedCountByRoom = new LinkedHashMap<>();
            for (final var room : accommodation.get().rooms()) {
                final List<RoomAssignmentRepresentation> roomAssignments = accommodation.get().assignments().stream()
                    .filter(a -> a.roomId().equals(room.roomId()))
                    .toList();
                assignmentsByRoom.put(room.roomId(), roomAssignments);
                assignedCountByRoom.put(room.roomId(), roomAssignments.stream()
                    .mapToInt(RoomAssignmentRepresentation::personCount).sum());
            }
            model.addAttribute("assignmentsByRoom", assignmentsByRoom);
            model.addAttribute("assignedCountByRoom", assignedCountByRoom);
        }

        return "layout/default";
    }

    @PostMapping("/{tripId}/accommodation")
    public String setAccommodation(@AuthenticationPrincipal final Jwt jwt,
                                    @PathVariable final UUID tripId,
                                    @RequestParam final String name,
                                    @RequestParam(required = false) final String address,
                                    @RequestParam(required = false) final String url,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate checkIn,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate checkOut,
                                    @RequestParam(required = false) final BigDecimal totalPrice,
                                    @RequestParam(value = "roomName") final List<String> roomNames,
                                    @RequestParam(value = "roomBedCount") final List<Integer> roomBedCounts,
                                    @RequestParam(value = "roomPricePerNight", required = false) final List<String> roomPrices) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        final List<RoomCommand> rooms = new ArrayList<>();
        for (int i = 0; i < roomNames.size(); i++) {
            final BigDecimal pricePerNight = (roomPrices != null && i < roomPrices.size()
                && roomPrices.get(i) != null && !roomPrices.get(i).isBlank())
                ? new BigDecimal(roomPrices.get(i))
                : null;
            rooms.add(new RoomCommand(roomNames.get(i), roomBedCounts.get(i), pricePerNight));
        }

        final SetAccommodationCommand command = new SetAccommodationCommand(
            identity.tenantId().value(), tripId, name, address, url,
            checkIn, checkOut, totalPrice, rooms
        );
        accommodationService.setAccommodation(command);
        return "redirect:/" + tripId + "/accommodation";
    }

    @PostMapping("/{tripId}/accommodation/rooms")
    public String addRoom(@AuthenticationPrincipal final Jwt jwt,
                          @PathVariable final UUID tripId,
                          @RequestParam final String name,
                          @RequestParam final int bedCount,
                          @RequestParam(required = false) final BigDecimal pricePerNight) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        final AddRoomCommand command = new AddRoomCommand(
            identity.tenantId().value(), tripId, name, bedCount, pricePerNight
        );
        accommodationService.addRoom(command);
        return "redirect:/" + tripId + "/accommodation";
    }

    @PostMapping("/{tripId}/accommodation/rooms/{roomId}/update")
    public String updateRoom(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId,
                             @PathVariable final UUID roomId,
                             @RequestParam final String name,
                             @RequestParam final int bedCount) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        accommodationService.updateRoom(identity.tenantId(), tripId, roomId, name, bedCount);
        return "redirect:/" + tripId + "/accommodation";
    }

    @PostMapping("/{tripId}/accommodation/rooms/{roomId}/delete")
    public String removeRoom(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId,
                             @PathVariable final UUID roomId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        final RemoveRoomCommand command = new RemoveRoomCommand(
            identity.tenantId().value(), tripId, roomId
        );
        accommodationService.removeRoom(command);
        return "redirect:/" + tripId + "/accommodation";
    }

    @PostMapping("/{tripId}/accommodation/rooms/{roomId}/assign")
    public String assignPartyToRoom(@AuthenticationPrincipal final Jwt jwt,
                                     @PathVariable final UUID tripId,
                                     @PathVariable final UUID roomId,
                                     @RequestParam final UUID partyTenantId,
                                     @RequestParam final String partyName,
                                     @RequestParam final int personCount) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        final AssignPartyToRoomCommand command = new AssignPartyToRoomCommand(
            identity.tenantId().value(), tripId, roomId, partyTenantId, partyName, personCount
        );
        accommodationService.assignPartyToRoom(command);
        return "redirect:/" + tripId + "/accommodation";
    }

    @PostMapping("/{tripId}/accommodation/assignments/{assignmentId}/delete")
    public String removeAssignment(@AuthenticationPrincipal final Jwt jwt,
                                    @PathVariable final UUID tripId,
                                    @PathVariable final UUID assignmentId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        final RemoveRoomAssignmentCommand command = new RemoveRoomAssignmentCommand(
            identity.tenantId().value(), tripId, assignmentId
        );
        accommodationService.removeRoomAssignment(command);
        return "redirect:/" + tripId + "/accommodation";
    }

    @PostMapping("/{tripId}/accommodation/import")
    public String importAccommodation(@AuthenticationPrincipal final Jwt jwt,
                                       @PathVariable final UUID tripId,
                                       @RequestParam final String url,
                                       final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        model.addAttribute("view", "accommodation/overview");
        model.addAttribute("trip", trip);
        model.addAttribute("isOrganizer", true);
        model.addAttribute("isEditable", true);

        try {
            final Optional<AccommodationImportResult> importResult = accommodationService.importFromUrl(url);
            if (importResult.isPresent()) {
                final AccommodationImportResult data = importResult.get();
                model.addAttribute("importResult", data);
                model.addAttribute("importSuccess", true);
                model.addAttribute("importUrl", url);
                model.addAttribute("prefillName", data.name());
                model.addAttribute("prefillAddress", data.address());
                model.addAttribute("prefillUrl", data.bookingUrl());
                model.addAttribute("prefillCheckIn", data.checkIn());
                model.addAttribute("prefillCheckOut", data.checkOut());
                model.addAttribute("prefillTotalPrice", data.totalPrice());
                model.addAttribute("prefillNotes", data.notes());
                model.addAttribute("prefillRooms", data.rooms());
            } else {
                model.addAttribute("importError", "accommodation.import.error");
                model.addAttribute("prefillUrl", url);
            }
        } catch (final IllegalArgumentException e) {
            model.addAttribute("importError", "accommodation.import.error.url");
            model.addAttribute("prefillUrl", url);
        }

        return "layout/default";
    }

    @PostMapping("/{tripId}/accommodation/delete")
    public String removeAccommodation(@AuthenticationPrincipal final Jwt jwt,
                                      @PathVariable final UUID tripId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        validateTripAccess(tripId, identity);
        validateEditable(tripId, identity);

        accommodationService.removeAccommodation(new TripId(tripId));
        return "redirect:/" + tripId + "/accommodation";
    }

    private List<PartyOption> resolvePartyOptions(final TripRepresentation trip) {
        final List<UUID> participantIds = trip.participantIds();
        final List<TravelParty> allParties = travelPartyRepository.findAll();

        final Map<UUID, PartyOption> partyMap = new LinkedHashMap<>();
        for (final TravelParty party : allParties) {
            final long memberCount = party.members().stream()
                .filter(m -> participantIds.contains(m.memberId()))
                .count();
            final long dependentCount = party.dependents().stream()
                .filter(d -> participantIds.contains(d.dependentId()))
                .count();
            final int totalCount = (int) (memberCount + dependentCount);
            if (totalCount > 0) {
                partyMap.put(party.tenantId().value(),
                    new PartyOption(party.tenantId().value(), party.name(), totalCount));
            }
        }
        return new ArrayList<>(partyMap.values());
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

    private void validateTripAccess(final UUID tripId, final ResolvedIdentity identity) {
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        if (!trip.tenantId().equals(identity.tenantId().value())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    private void validateEditable(final UUID tripId, final ResolvedIdentity identity) {
        final TripRepresentation trip = tripService.findById(new TripId(tripId));
        if (!trip.organizerId().equals(identity.memberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can modify accommodation");
        }
        if ("COMPLETED".equals(trip.status()) || "CANCELLED".equals(trip.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify accommodation for a completed or cancelled trip");
        }
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }
}
