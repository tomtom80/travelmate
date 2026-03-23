ALTER TABLE trip_participant ADD COLUMN date_of_birth DATE;
ALTER TABLE trip_participant ADD COLUMN account_holder BOOLEAN NOT NULL DEFAULT FALSE;
