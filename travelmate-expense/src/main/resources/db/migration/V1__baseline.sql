-- Expense SCS: Baseline schema
-- Managed by Flyway — do not modify after deployment

CREATE TABLE expense (
    expense_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN'
);

CREATE TABLE receipt (
    receipt_id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expense(expense_id),
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    paid_by_username VARCHAR(255) NOT NULL,
    image_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
