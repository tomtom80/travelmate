-- Advance payment tracking per travel party
CREATE TABLE advance_payment (
    advance_payment_id UUID PRIMARY KEY,
    expense_id UUID NOT NULL,
    party_tenant_id UUID NOT NULL,
    party_name VARCHAR(200) NOT NULL,
    amount NUMERIC NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_advance_payment_expense FOREIGN KEY (expense_id) REFERENCES expense (expense_id) ON DELETE CASCADE
);

-- Accommodation total price on trip projection for advance payment suggestion
ALTER TABLE trip_projection ADD COLUMN accommodation_total_price NUMERIC;
