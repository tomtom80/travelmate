CREATE TABLE external_invite_followups (
    email          VARCHAR(255) NOT NULL,
    trip_id        UUID         NOT NULL,
    action_type    VARCHAR(64)  NOT NULL,
    dispatched_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (email, trip_id)
);

CREATE INDEX idx_external_invite_followups_dispatched_at
    ON external_invite_followups (dispatched_at);
