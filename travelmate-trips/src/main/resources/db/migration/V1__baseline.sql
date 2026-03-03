-- Trips SCS: Baseline schema
-- Managed by Flyway — do not modify after deployment

CREATE TABLE trip (
    trip_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNING'
);

CREATE TABLE participant (
    participant_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    username VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    family_name VARCHAR(255),
    email VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);
