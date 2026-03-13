CREATE TABLE invitation_token (
    token_value  VARCHAR(255) PRIMARY KEY,
    account_id   UUID         NOT NULL REFERENCES account(account_id),
    expires_at   TIMESTAMP    NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_invitation_token_account_id ON invitation_token(account_id);
