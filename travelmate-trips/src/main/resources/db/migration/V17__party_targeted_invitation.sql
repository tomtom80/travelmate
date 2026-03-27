ALTER TABLE invitation
    ADD COLUMN IF NOT EXISTS target_party_tenant_id UUID;

CREATE INDEX IF NOT EXISTS idx_invitation_trip_status_target_party
    ON invitation (trip_id, status, target_party_tenant_id);
