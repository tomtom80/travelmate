-- IAM SCS: Account and Dependent tables
-- Managed by Flyway — do not modify after deployment

CREATE TABLE account (
    account_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id),
    keycloak_user_id VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    UNIQUE (tenant_id, username),
    UNIQUE (tenant_id, keycloak_user_id)
);
CREATE INDEX idx_account_tenant_id ON account(tenant_id);

CREATE TABLE dependent (
    dependent_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id),
    guardian_account_id UUID NOT NULL REFERENCES account(account_id),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL
);
CREATE INDEX idx_dependent_tenant_id ON dependent(tenant_id);
CREATE INDEX idx_dependent_guardian ON dependent(guardian_account_id);

ALTER TABLE policy ADD CONSTRAINT fk_policy_account
    FOREIGN KEY (user_id) REFERENCES account(account_id);
