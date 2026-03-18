CREATE TABLE room_assignment (
    assignment_id UUID PRIMARY KEY,
    accommodation_id UUID NOT NULL,
    room_id UUID NOT NULL,
    party_tenant_id UUID NOT NULL,
    party_name VARCHAR(200) NOT NULL,
    person_count INT NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_assignment_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodation (accommodation_id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_room FOREIGN KEY (room_id) REFERENCES accommodation_room (room_id) ON DELETE CASCADE
);
