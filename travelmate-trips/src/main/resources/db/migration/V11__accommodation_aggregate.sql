CREATE TABLE accommodation (
    accommodation_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    trip_id UUID NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    url VARCHAR(1000),
    check_in DATE,
    check_out DATE,
    total_price NUMERIC,
    CONSTRAINT fk_accommodation_trip FOREIGN KEY (trip_id) REFERENCES trip (trip_id) ON DELETE CASCADE
);

CREATE TABLE accommodation_room (
    room_id UUID PRIMARY KEY,
    accommodation_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    room_type VARCHAR(20) NOT NULL,
    bed_count INT NOT NULL,
    price_per_night NUMERIC,
    CONSTRAINT fk_room_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodation (accommodation_id) ON DELETE CASCADE
);
