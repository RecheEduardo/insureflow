CREATE TABLE policy_outbox (
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    event_id   UUID        NOT NULL,
    policy_id  VARCHAR(60) NOT NULL,
    payload    JSONB       NOT NULL,
    CONSTRAINT pk_policy_outbox PRIMARY KEY (id),
    CONSTRAINT uk_policy_outbox_event_id UNIQUE (event_id)
);
