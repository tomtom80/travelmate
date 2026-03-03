-- IAM SCS: Baseline schema
-- Managed by Flyway — do not modify after deployment

CREATE TABLE tenant (
    tenant_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE role (
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    PRIMARY KEY (tenant_id, name)
);

CREATE TABLE policy (
    policy_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id),
    user_id UUID NOT NULL,
    role_name VARCHAR(255) NOT NULL
);

CREATE TABLE user_group (
    group_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id),
    name VARCHAR(255) NOT NULL,
    description TEXT
);
