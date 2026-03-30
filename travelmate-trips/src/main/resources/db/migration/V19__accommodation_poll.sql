CREATE TABLE accommodation_poll (
    accommodation_poll_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    selected_candidate_id UUID
);

CREATE TABLE accommodation_candidate (
    candidate_id UUID PRIMARY KEY,
    accommodation_poll_id UUID NOT NULL REFERENCES accommodation_poll(accommodation_poll_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1000),
    description VARCHAR(2000)
);

CREATE TABLE accommodation_vote (
    vote_id UUID PRIMARY KEY,
    accommodation_poll_id UUID NOT NULL REFERENCES accommodation_poll(accommodation_poll_id) ON DELETE CASCADE,
    voter_id UUID NOT NULL,
    selected_candidate_id UUID NOT NULL
);

CREATE INDEX idx_accommodation_poll_tenant_trip ON accommodation_poll(tenant_id, trip_id);
CREATE INDEX idx_accommodation_poll_tenant ON accommodation_poll(tenant_id);
