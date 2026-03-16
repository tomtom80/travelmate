-- Add stay period columns to trip_participant for accommodation cost splitting
ALTER TABLE trip_participant ADD COLUMN arrival_date DATE;
ALTER TABLE trip_participant ADD COLUMN departure_date DATE;
