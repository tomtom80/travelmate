-- Add trip date range to trip_projection for per-day cost breakdown
ALTER TABLE trip_projection ADD COLUMN start_date DATE;
ALTER TABLE trip_projection ADD COLUMN end_date DATE;
