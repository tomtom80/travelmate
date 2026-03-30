package de.evia.travelmate.trips.domain.accommodationpoll;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;
import static de.evia.travelmate.common.domain.Assertion.argumentIsTrue;

import java.util.UUID;

public class AccommodationCandidate {

    private final AccommodationCandidateId candidateId;
    private final String name;
    private final String url;
    private final String description;

    public AccommodationCandidate(final String name, final String url, final String description) {
        argumentIsNotNull(name, "name");
        argumentIsTrue(!name.isBlank(), "Candidate name must not be blank.");
        this.candidateId = new AccommodationCandidateId(UUID.randomUUID());
        this.name = name;
        this.url = url;
        this.description = description;
    }

    public AccommodationCandidate(final AccommodationCandidateId candidateId,
                                  final String name, final String url, final String description) {
        argumentIsNotNull(candidateId, "candidateId");
        argumentIsNotNull(name, "name");
        this.candidateId = candidateId;
        this.name = name;
        this.url = url;
        this.description = description;
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

    public String description() {
        return description;
    }
}
