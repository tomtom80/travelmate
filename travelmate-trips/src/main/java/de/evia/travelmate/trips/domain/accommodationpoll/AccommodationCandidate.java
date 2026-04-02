package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AccommodationCandidate {

    private final AccommodationCandidateId candidateId;
    private final String name;
    private final String url;
    private final String address;
    private final String description;
    private final List<CandidateRoom> rooms;
    private final Set<Amenity> amenities;

    public AccommodationCandidate(final String name, final String url, final String address,
                                  final String description, final List<CandidateRoom> rooms,
                                  final Set<Amenity> amenities) {
        argumentIsNotNull(name, "name");
        argumentIsTrue(!name.isBlank(), "Candidate name must not be blank.");
        this.candidateId = new AccommodationCandidateId(UUID.randomUUID());
        this.name = name;
        this.url = url;
        this.address = address;
        this.description = description;
        this.rooms = rooms == null ? List.of() : List.copyOf(rooms);
        this.amenities = amenities == null || amenities.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(amenities));
    }

    public AccommodationCandidate(final AccommodationCandidateId candidateId,
                                  final String name, final String url, final String address,
                                  final String description, final List<CandidateRoom> rooms,
                                  final Set<Amenity> amenities) {
        argumentIsNotNull(candidateId, "candidateId");
        argumentIsNotNull(name, "name");
        this.candidateId = candidateId;
        this.name = name;
        this.url = url;
        this.address = address;
        this.description = description;
        this.rooms = rooms == null ? List.of() : List.copyOf(rooms);
        this.amenities = amenities == null || amenities.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(amenities));
    }

    public AccommodationCandidateId candidateId() {
        return candidateId;
    }

    public String name() {
        return name;
    }

    public String url() {
        return url;
    }

    public String address() {
        return address;
    }

    public String description() {
        return description;
    }

    public List<CandidateRoom> rooms() {
        return rooms;
    }

    public Set<Amenity> amenities() {
        return amenities;
    }
}
