CREATE TABLE idempotency_record (
    idempotency_key  VARCHAR(255) NOT NULL,
    request_hash     CHAR(64)     NOT NULL,
    policy_id        VARCHAR(60)  NOT NULL,
    policy_status    VARCHAR(20)  NOT NULL,
    policy_version   BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_idempotency_record PRIMARY KEY (idempotency_key),
    -- A chave é gravada antes da apólice para que requisições concorrentes com a mesma chave esperem na PK.
    CONSTRAINT fk_idempotency_record_policy FOREIGN KEY (policy_id) REFERENCES policy (policy_id)
        DEFERRABLE INITIALLY DEFERRED
);
