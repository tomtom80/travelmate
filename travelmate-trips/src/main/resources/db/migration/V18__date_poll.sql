CREATE TABLE date_poll (
    date_poll_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    confirmed_option_id UUID
);

CREATE TABLE date_option (
    date_option_id UUID PRIMARY KEY,
    date_poll_id UUID NOT NULL REFERENCES date_poll(date_poll_id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL
);

CREATE TABLE date_vote (
    date_vote_id UUID PRIMARY KEY,
    date_poll_id UUID NOT NULL REFERENCES date_poll(date_poll_id) ON DELETE CASCADE,
    voter_id UUID NOT NULL
);

CREATE TABLE date_vote_option (
    date_vote_id UUID NOT NULL REFERENCES date_vote(date_vote_id) ON DELETE CASCADE,
    date_option_id UUID NOT NULL,
    PRIMARY KEY (date_vote_id, date_option_id)
);

CREATE INDEX idx_date_poll_tenant_trip ON date_poll(tenant_id, trip_id);
CREATE INDEX idx_date_poll_tenant ON date_poll(tenant_id);
