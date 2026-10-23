CREATE TABLE policy_info_view (
    policy_id       VARCHAR(60) NOT NULL,
    policy_version  BIGINT      NOT NULL,
    product_name    VARCHAR(70) NOT NULL,
    snapshot        JSONB       NOT NULL,
    CONSTRAINT pk_policy_info_view PRIMARY KEY (policy_id)
);
