package de.evia.travelmate.trips.domain.invitation;

import static de.evia.travelmate.common.domain.Assertion.argumentIsNotNull;

import java.util.UUID;

public record InvitationId(UUID value) {

    public InvitationId {
        argumentIsNotNull(value, "invitationId");
    }
}
