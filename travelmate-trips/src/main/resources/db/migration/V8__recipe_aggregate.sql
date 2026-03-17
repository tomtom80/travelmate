-- Recipe aggregate: tenant-scoped recipe library

CREATE TABLE recipe (
    recipe_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    servings INT NOT NULL CHECK (servings > 0)
);

CREATE INDEX idx_recipe_tenant_id ON recipe(tenant_id);

CREATE TABLE recipe_ingredient (
    ingredient_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID NOT NULL REFERENCES recipe(recipe_id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    quantity NUMERIC(10,3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(50) NOT NULL
);

CREATE INDEX idx_recipe_ingredient_recipe_id ON recipe_ingredient(recipe_id);
