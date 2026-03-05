-- Trip aggregate: core domain

ALTER TABLE trip ADD COLUMN organizer_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE trip ALTER COLUMN organizer_id DROP DEFAULT;

CREATE TABLE trip_participant (
    participant_id UUID NOT NULL,
    trip_id UUID NOT NULL REFERENCES trip(trip_id),
    arrival_date DATE,
    departure_date DATE,
    PRIMARY KEY (participant_id, trip_id)
);
