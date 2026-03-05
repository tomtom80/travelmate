CREATE TABLE invitation (
    invitation_id UUID PRIMARY KEY,
    tenant_id     UUID        NOT NULL,
    trip_id       UUID        NOT NULL REFERENCES trip(trip_id),
    invitee_id    UUID        NOT NULL,
    invited_by    UUID        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    UNIQUE (trip_id, invitee_id)
);
