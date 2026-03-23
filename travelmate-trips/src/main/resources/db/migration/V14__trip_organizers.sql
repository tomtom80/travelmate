CREATE TABLE trip_organizer (
    trip_id UUID NOT NULL REFERENCES trip(trip_id) ON DELETE CASCADE,
    organizer_id UUID NOT NULL,
    PRIMARY KEY (trip_id, organizer_id)
);

INSERT INTO trip_organizer (trip_id, organizer_id)
SELECT trip_id, organizer_id
FROM trip;
