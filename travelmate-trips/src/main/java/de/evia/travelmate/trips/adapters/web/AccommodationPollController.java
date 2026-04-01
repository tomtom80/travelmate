package de.evia.travelmate.trips.adapters.web;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
import de.evia.travelmate.trips.application.AccommodationPollService;
import de.evia.travelmate.trips.application.TripService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.evia.travelmate.trips.application.command.CastAccommodationVoteCommand;
import de.evia.travelmate.trips.application.command.CreateAccommodationPollCommand;
import de.evia.travelmate.trips.application.command.CreateAccommodationPollCommand.CandidateProposalCommand;
import de.evia.travelmate.trips.application.command.AddAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.command.RecordAccommodationBookingFailureCommand;
import de.evia.travelmate.trips.application.command.RecordAccommodationBookingSuccessCommand;
import de.evia.travelmate.trips.application.command.RemoveAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.command.SelectAccommodationCandidateCommand;
import de.evia.travelmate.trips.application.representation.AccommodationPollRepresentation;
import de.evia.travelmate.trips.application.representation.TripRepresentation;
import de.evia.travelmate.trips.domain.accommodation.AccommodationImportResult;
import de.evia.travelmate.trips.domain.accommodation.ImportedRoom;
import de.evia.travelmate.trips.domain.accommodationpoll.AccommodationPollId;
import de.evia.travelmate.trips.domain.accommodationpoll.Amenity;
import de.evia.travelmate.trips.domain.trip.TripId;
import de.evia.travelmate.trips.domain.travelparty.TravelPartyRepository;

@Controller
public class AccommodationPollController {

    private final AccommodationService accommodationService;
    private final AccommodationPollService accommodationPollService;
    private final TripService tripService;
    private final TravelPartyRepository travelPartyRepository;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<CreateAccommodationPollCommand.RoomProposalCommand>> ROOM_COMMANDS_TYPE =
        new TypeReference<>() {};

    public AccommodationPollController(final AccommodationService accommodationService,
                                       final AccommodationPollService accommodationPollService,
                                       final TripService tripService,
                                       final TravelPartyRepository travelPartyRepository) {
        this.accommodationService = accommodationService;
        this.accommodationPollService = accommodationPollService;
        this.tripService = tripService;
        this.travelPartyRepository = travelPartyRepository;
    }

    @GetMapping("/{tripId}/accommodationpoll")
    public String overview(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        populateOverviewModel(model, trip, identity, null, null);
        return "layout/default";
    }

    @GetMapping("/{tripId}/accommodationpoll/create")
    public String createForm(@AuthenticationPrincipal final Jwt jwt,
                             @PathVariable final UUID tripId,
                             final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        model.addAttribute("view", "accommodationpoll/create");
        model.addAttribute("trip", trip);
        model.addAttribute("allAmenities", Amenity.values());
        return "layout/default";
    }

    @PostMapping("/{tripId}/accommodationpoll/create")
    public String create(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @RequestParam("candidateName") final List<String> names,
                         @RequestParam(value = "candidateUrl", required = false) final List<String> urls,
                         @RequestParam(value = "candidateDescription", required = false) final List<String> descriptions,
                         @RequestParam(value = "candidateRoomsJson", required = false) final List<String> roomsJson,
                         @RequestParam(value = "candidateAmenitiesJson", required = false) final List<String> amenitiesJson) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        final List<CandidateProposalCommand> candidates = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            final String url = urls != null && i < urls.size() ? emptyToNull(urls.get(i)) : null;
            final String desc = descriptions != null && i < descriptions.size() ? emptyToNull(descriptions.get(i)) : null;
            final String amenJson = amenitiesJson != null && i < amenitiesJson.size() ? amenitiesJson.get(i) : null;
            candidates.add(new CandidateProposalCommand(
                names.get(i), url, desc,
                parseRoomCommands(i < roomsJsonSize(roomsJson) ? roomsJson.get(i) : null),
                parseAmenitiesJson(amenJson)
            ));
        }

        accommodationPollService.createPoll(new CreateAccommodationPollCommand(
            trip.tenantId(), tripId, candidates));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/candidates/add")
    public String addCandidate(@AuthenticationPrincipal final Jwt jwt,
                               @PathVariable final UUID tripId,
                               @PathVariable final UUID pollId,
                               @RequestParam final String name,
                               @RequestParam(required = false) final String url,
                               @RequestParam(required = false) final String description,
                               @RequestParam(value = "roomsJson", required = false) final String roomsJson,
                               @RequestParam(value = "amenities", required = false) final List<String> amenityNames) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);

        accommodationPollService.addCandidate(new AddAccommodationCandidateCommand(
            trip.tenantId(), pollId, name, emptyToNull(url), emptyToNull(description),
            parseRoomCommands(roomsJson), parseAmenities(amenityNames)));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/candidates/{candidateId}/remove")
    public String removeCandidate(@AuthenticationPrincipal final Jwt jwt,
                                  @PathVariable final UUID tripId,
                                  @PathVariable final UUID pollId,
                                  @PathVariable final UUID candidateId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        accommodationPollService.removeCandidate(new RemoveAccommodationCandidateCommand(
            trip.tenantId(), pollId, candidateId));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/vote")
    public String castVote(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable final UUID tripId,
                           @PathVariable final UUID pollId,
                           @RequestParam final UUID selectedCandidateId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);

        accommodationPollService.castVote(new CastAccommodationVoteCommand(
            trip.tenantId(), pollId, identity.memberId(), selectedCandidateId));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/import")
    public String importCandidate(@AuthenticationPrincipal final Jwt jwt,
                                  @PathVariable final UUID tripId,
                                  @PathVariable final UUID pollId,
                                  @RequestParam final String url,
                                  final Model model) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);

        try {
            final Optional<AccommodationImportResult> importResult = accommodationService.importFromUrl(url);
            if (importResult.isEmpty()) {
                populateOverviewModel(model, trip, identity, "accommodation.import.error", null);
                return "layout/default";
            }

            final AccommodationImportResult imported = importResult.get();
            accommodationPollService.addCandidate(new AddAccommodationCandidateCommand(
                trip.tenantId(),
                pollId,
                candidateName(imported),
                emptyToNull(imported.bookingUrl()),
                candidateDescription(imported),
                roomsFromImport(imported),
                Set.of()));
            return "redirect:/" + tripId + "/accommodationpoll";
        } catch (final IllegalArgumentException e) {
            populateOverviewModel(model, trip, identity, "accommodation.import.error.url", null);
            return "layout/default";
        }
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/select")
    public String select(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @PathVariable final UUID pollId,
                         @RequestParam final UUID selectedCandidateId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        accommodationPollService.selectCandidate(new SelectAccommodationCandidateCommand(
            trip.tenantId(), tripId, pollId, selectedCandidateId));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/book")
    public String recordBookingSuccess(@AuthenticationPrincipal final Jwt jwt,
                                       @PathVariable final UUID tripId,
                                       @PathVariable final UUID pollId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        accommodationPollService.recordBookingSuccess(new RecordAccommodationBookingSuccessCommand(
            trip.tenantId(), tripId, pollId));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/fail")
    public String recordBookingFailure(@AuthenticationPrincipal final Jwt jwt,
                                       @PathVariable final UUID tripId,
                                       @PathVariable final UUID pollId,
                                       @RequestParam(required = false) final String note) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        accommodationPollService.recordBookingFailure(new RecordAccommodationBookingFailureCommand(
            trip.tenantId(), pollId, emptyToNull(note)));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    @PostMapping("/{tripId}/accommodationpoll/{pollId}/cancel")
    public String cancel(@AuthenticationPrincipal final Jwt jwt,
                         @PathVariable final UUID tripId,
                         @PathVariable final UUID pollId) {
        final ResolvedIdentity identity = requireIdentity(jwt);
        final TripRepresentation trip = validateTripAccess(tripId, identity);
        requireOrganizer(trip, identity);

        accommodationPollService.cancelPoll(tripTenantId(trip), new AccommodationPollId(pollId));

        return "redirect:/" + tripId + "/accommodationpoll";
    }

    private String emptyToNull(final String value) {
        return value != null && !value.isBlank() ? value : null;
    }

    private Set<Amenity> parseAmenitiesJson(final String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Set.of();
        }
        try {
            final List<String> names = OBJECT_MAPPER.readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            return parseAmenities(names);
        } catch (final Exception e) {
            return Set.of();
        }
    }

    private Set<Amenity> parseAmenities(final List<String> amenityNames) {
        if (amenityNames == null || amenityNames.isEmpty()) {
            return Set.of();
        }
        final EnumSet<Amenity> result = EnumSet.noneOf(Amenity.class);
        for (final String name : amenityNames) {
            if (name != null && !name.isBlank()) {
                try {
                    result.add(Amenity.valueOf(name));
                } catch (final IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    private void populateOverviewModel(final Model model,
                                       final TripRepresentation trip,
                                       final ResolvedIdentity identity,
                                       final String importError,
                                       final String importSuccess) {
        AccommodationPollRepresentation poll = null;
        try {
            poll = accommodationPollService.findLatestByTripId(tripTenantId(trip), new TripId(trip.tripId()));
        } catch (final Exception ignored) {
        }

        UUID currentVoteCandidateId = null;
        if (poll != null) {
            currentVoteCandidateId = poll.votes().stream()
                .filter(v -> v.voterId().equals(identity.memberId()))
                .findFirst()
                .map(AccommodationPollRepresentation.VoteRepresentation::selectedCandidateId)
                .orElse(null);
        }

        model.addAttribute("view", "accommodationpoll/overview");
        model.addAttribute("trip", trip);
        model.addAttribute("accommodationPoll", poll);
        model.addAttribute("isOrganizer", trip.isOrganizer(identity.memberId()));
        model.addAttribute("currentMemberId", identity.memberId());
        model.addAttribute("currentVoteCandidateId", currentVoteCandidateId);
        model.addAttribute("importError", importError);
        model.addAttribute("importSuccess", importSuccess);
        model.addAttribute("allAmenities", Amenity.values());
    }

    private String candidateName(final AccommodationImportResult imported) {
        final String importedName = emptyToNull(imported.name());
        if (importedName != null) {
            return importedName;
        }

        final String importedUrl = emptyToNull(imported.bookingUrl());
        if (importedUrl != null) {
            return importedUrl;
        }

        return "Imported accommodation";
    }

    private String candidateDescription(final AccommodationImportResult imported) {
        final String notes = emptyToNull(imported.notes());
        if (notes != null) {
            return notes;
        }
        return emptyToNull(imported.address());
    }

    private List<CreateAccommodationPollCommand.RoomProposalCommand> roomsFromImport(final AccommodationImportResult imported) {
        final List<ImportedRoom> importedRooms = imported.rooms();
        if (importedRooms != null && !importedRooms.isEmpty()) {
            return importedRooms.stream()
                .map(room -> new CreateAccommodationPollCommand.RoomProposalCommand(
                    emptyToNull(room.name()) != null ? room.name() : "Zimmer",
                    Math.max(room.bedCount(), 1),
                    room.pricePerNight(),
                    null))
                .toList();
        }
        return List.of(new CreateAccommodationPollCommand.RoomProposalCommand(
            "Zimmer",
            2,
            null,
            null
        ));
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organizers can manage accommodation polls");
        }
    }

    private TenantId tripTenantId(final TripRepresentation trip) {
        return new TenantId(trip.tenantId());
    }

    private record ResolvedIdentity(TenantId tenantId, UUID memberId) {
    }

    private int roomsJsonSize(final List<String> roomsJson) {
        return roomsJson == null ? 0 : roomsJson.size();
    }

    private List<CreateAccommodationPollCommand.RoomProposalCommand> parseRoomCommands(final String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, ROOM_COMMANDS_TYPE);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Invalid room data", e);
        }
    }
}
