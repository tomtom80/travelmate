package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.List;
import java.util.Set;

public record CandidateProposal(String name, String url, String address, String description,
                                 List<CandidateRoom> rooms, Set<Amenity> amenities) {

    public CandidateProposal {
        argumentIsNotNull(name, "name");
        argumentIsTrue(!name.isBlank(), "Candidate name must not be blank.");
        rooms = rooms == null ? List.of() : List.copyOf(rooms);
        amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
    }
}
