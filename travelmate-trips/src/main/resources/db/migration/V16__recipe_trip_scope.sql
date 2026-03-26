-- Add optional trip_id to recipe for trip-scoped recipes
-- NULL = personal recipe (tenant-scoped), non-NULL = trip recipe (shared)
ALTER TABLE recipe ADD COLUMN trip_id UUID;
ALTER TABLE recipe ADD COLUMN contributed_by VARCHAR(200);

CREATE INDEX idx_recipe_trip_id ON recipe(trip_id);
