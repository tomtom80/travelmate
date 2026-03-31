package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.List;

public record CandidateProposal(String name, String url, String description, List<CandidateRoom> rooms) {

    public CandidateProposal {
        argumentIsNotNull(name, "name");
        argumentIsTrue(!name.isBlank(), "Candidate name must not be blank.");
        rooms = rooms == null ? List.of() : List.copyOf(rooms);
    }
}
