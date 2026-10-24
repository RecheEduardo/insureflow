package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.event;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PolicyOutboxMetrics.class)
@Testcontainers
class PolicyOutboxMetricsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Autowired
    PolicyOutboxMetrics metrics;

    @Autowired
    JdbcTemplate jdbc;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void bind() {
        metrics.bindTo(registry);
    }

    @Test
    void reportsHowManyEventsWaitInTheOutbox() {
        assertThat(pending()).isZero();

        insertEvent("AP-001");
        insertEvent("AP-002");

        assertThat(pending()).isEqualTo(2);
    }

    private void insertEvent(String policyId) {
        jdbc.update("INSERT INTO policy_outbox (event_id, policy_id, payload) VALUES (gen_random_uuid(), ?, '{}'::jsonb)",
                policyId);
    }

    private double pending() {
        return registry.get(PolicyOutboxMetrics.PENDING).gauge().value();
    }
}
