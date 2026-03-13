-- Trip projection: local read model for trip data received via events
CREATE TABLE trip_projection (
    trip_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    trip_name VARCHAR(255) NOT NULL
);

CREATE TABLE trip_participant (
    trip_id UUID NOT NULL REFERENCES trip_projection(trip_id) ON DELETE CASCADE,
    participant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (trip_id, participant_id)
);
