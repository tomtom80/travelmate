CREATE TABLE audit_log (
    audit_id        UUID         NOT NULL,
    occurred_on     TIMESTAMPTZ  NOT NULL,
    tenant_id       UUID         NOT NULL,
    actor_account_id UUID,
    actor_role      VARCHAR(64),
    action          VARCHAR(128) NOT NULL,
    resource_type   VARCHAR(128) NOT NULL,
    resource_id     UUID,
    outcome         VARCHAR(16)  NOT NULL,
    reason          TEXT,
    PRIMARY KEY (audit_id)
);

CREATE INDEX idx_audit_log_tenant_id ON audit_log (tenant_id);
CREATE INDEX idx_audit_log_occurred_on ON audit_log (occurred_on);
