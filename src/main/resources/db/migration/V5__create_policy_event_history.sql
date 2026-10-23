CREATE TABLE policy_event_history (
    event_id        UUID        NOT NULL,
    event_type      VARCHAR(30) NOT NULL,
    policy_id       VARCHAR(60) NOT NULL,
    policy_version  BIGINT      NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_policy_event_history PRIMARY KEY (event_id)
);

CREATE INDEX ix_policy_event_history_policy_id ON policy_event_history (policy_id, policy_version);
