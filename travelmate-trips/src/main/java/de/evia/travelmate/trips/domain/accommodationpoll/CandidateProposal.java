package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

public record CandidateProposal(String name, String url, String description) {

    public CandidateProposal {
        argumentIsNotNull(name, "name");
        argumentIsTrue(!name.isBlank(), "Candidate name must not be blank.");
    }
}
