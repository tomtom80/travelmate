ALTER TABLE expense ADD COLUMN review_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE receipt ADD COLUMN submitted_by UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE receipt ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE receipt ADD COLUMN reviewer_id UUID;
ALTER TABLE receipt ADD COLUMN rejection_reason TEXT;

-- Backfill: existing receipts have submitted_by = paid_by
UPDATE receipt SET submitted_by = paid_by WHERE submitted_by = '00000000-0000-0000-0000-000000000000';

-- Remove the temporary default
ALTER TABLE receipt ALTER COLUMN submitted_by DROP DEFAULT;
