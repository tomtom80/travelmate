-- Add external invitation support: invitee_email, invitation_type
ALTER TABLE invitation ADD COLUMN invitee_email VARCHAR(255);
ALTER TABLE invitation ADD COLUMN invitation_type VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

-- Allow invitee_id to be NULL (for external invitations where user hasn't registered yet)
ALTER TABLE invitation ALTER COLUMN invitee_id DROP NOT NULL;

-- Drop the old unique constraint on (trip_id, invitee_id) since invitee_id can now be NULL
ALTER TABLE invitation DROP CONSTRAINT invitation_trip_id_invitee_id_key;
