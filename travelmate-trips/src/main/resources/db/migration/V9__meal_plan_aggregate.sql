-- MealPlan aggregate: day x meal grid per trip

CREATE TABLE meal_plan (
    meal_plan_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    trip_id UUID NOT NULL REFERENCES trip(trip_id),
    CONSTRAINT uq_meal_plan_trip UNIQUE (trip_id)
);

CREATE INDEX idx_meal_plan_tenant_id ON meal_plan(tenant_id);

CREATE TABLE meal_slot (
    meal_slot_id UUID PRIMARY KEY,
    meal_plan_id UUID NOT NULL REFERENCES meal_plan(meal_plan_id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    recipe_id UUID REFERENCES recipe(recipe_id) ON DELETE SET NULL
);

CREATE INDEX idx_meal_slot_meal_plan_id ON meal_slot(meal_plan_id);
CREATE INDEX idx_meal_slot_recipe_id ON meal_slot(recipe_id);
