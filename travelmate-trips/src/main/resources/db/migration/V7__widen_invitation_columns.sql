-- Widen invitation status and type columns to accommodate AWAITING_REGISTRATION (22 chars)
ALTER TABLE invitation ALTER COLUMN status TYPE VARCHAR(30);
ALTER TABLE invitation ALTER COLUMN invitation_type TYPE VARCHAR(30);
