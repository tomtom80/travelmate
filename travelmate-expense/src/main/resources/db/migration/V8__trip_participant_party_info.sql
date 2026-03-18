-- Add party information to trip participants for party-level settlement
ALTER TABLE trip_participant ADD COLUMN party_tenant_id UUID;
ALTER TABLE trip_participant ADD COLUMN party_name VARCHAR(255);
