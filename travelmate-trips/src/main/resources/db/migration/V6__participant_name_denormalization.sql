-- Denormalize participant names for cross-tenant trip visibility

ALTER TABLE trip_participant ADD COLUMN first_name VARCHAR(255);
ALTER TABLE trip_participant ADD COLUMN last_name VARCHAR(255);
