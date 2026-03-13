-- Add weighting table for expense participant weightings
CREATE TABLE weighting (
    expense_id UUID NOT NULL REFERENCES expense(expense_id) ON DELETE CASCADE,
    participant_id UUID NOT NULL,
    weight NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    PRIMARY KEY (expense_id, participant_id)
);

-- Alter receipt: rename paid_by_username to paid_by (UUID), remove image_path, add date
ALTER TABLE receipt DROP COLUMN paid_by_username;
ALTER TABLE receipt ADD COLUMN paid_by UUID NOT NULL;
ALTER TABLE receipt DROP COLUMN image_path;
ALTER TABLE receipt DROP COLUMN created_at;
ALTER TABLE receipt ADD COLUMN date DATE NOT NULL DEFAULT CURRENT_DATE;

-- Recreate receipt FK with ON DELETE CASCADE
ALTER TABLE receipt DROP CONSTRAINT receipt_expense_id_fkey;
ALTER TABLE receipt ADD CONSTRAINT receipt_expense_id_fkey
    FOREIGN KEY (expense_id) REFERENCES expense(expense_id) ON DELETE CASCADE;
