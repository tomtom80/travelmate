CREATE TABLE IF NOT EXISTS trip_participation (
    participant_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    PRIMARY KEY (participant_id, trip_id)
);

CREATE INDEX IF NOT EXISTS idx_trip_participation_participant
    ON trip_participation (participant_id);
