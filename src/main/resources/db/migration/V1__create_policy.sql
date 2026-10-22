CREATE TABLE policy (
    id                    UUID           NOT NULL,
    policy_id             VARCHAR(60)    NOT NULL,
    document_type         VARCHAR(30)    NOT NULL,
    group_certificate_id  VARCHAR(60),
    issuance_type         VARCHAR(30)    NOT NULL,
    issuance_date         DATE           NOT NULL,
    term_start_date       DATE           NOT NULL,
    term_end_date         DATE           NOT NULL,
    max_lmg_amount        NUMERIC(18, 2) NOT NULL,
    max_lmg_currency      VARCHAR(3)       NOT NULL,
    proposal_id           VARCHAR(60)    NOT NULL,
    product_name          VARCHAR(70)    NOT NULL,
    insureds              JSONB          NOT NULL,
    insured_objects       JSONB          NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    cancelled_at          TIMESTAMPTZ,
    cancellation_reason   VARCHAR(30),
    version               BIGINT         NOT NULL,
    created_at            TIMESTAMPTZ    NOT NULL,
    updated_at            TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_policy PRIMARY KEY (id),
    CONSTRAINT uk_policy_policy_id UNIQUE (policy_id),
    CONSTRAINT ck_policy_term CHECK (term_end_date >= term_start_date),
    CONSTRAINT ck_policy_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT ck_policy_cancellation CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancellation_reason IS NOT NULL)
        OR (status = 'ACTIVE' AND cancelled_at IS NULL AND cancellation_reason IS NULL))
);
