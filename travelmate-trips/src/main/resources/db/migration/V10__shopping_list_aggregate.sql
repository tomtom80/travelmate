CREATE TABLE shopping_list (
    shopping_list_id UUID PRIMARY KEY,
    tenant_id        UUID         NOT NULL,
    trip_id          UUID         NOT NULL,
    CONSTRAINT fk_shopping_list_trip FOREIGN KEY (trip_id) REFERENCES trip (trip_id) ON DELETE CASCADE,
    CONSTRAINT uq_shopping_list_trip UNIQUE (trip_id)
);

CREATE TABLE shopping_item (
    shopping_item_id UUID PRIMARY KEY,
    shopping_list_id UUID         NOT NULL,
    name             VARCHAR(100) NOT NULL,
    quantity         NUMERIC      NOT NULL,
    unit             VARCHAR(20)  NOT NULL,
    source           VARCHAR(10)  NOT NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    assigned_to      UUID,
    CONSTRAINT fk_shopping_item_list FOREIGN KEY (shopping_list_id) REFERENCES shopping_list (shopping_list_id) ON DELETE CASCADE
);

CREATE INDEX idx_shopping_list_tenant ON shopping_list (tenant_id);
CREATE INDEX idx_shopping_item_list ON shopping_item (shopping_list_id);
