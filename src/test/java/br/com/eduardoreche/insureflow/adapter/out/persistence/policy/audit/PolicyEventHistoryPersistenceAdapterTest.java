package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.audit;

import br.com.eduardoreche.insureflow.application.policy.PolicyFixtures;
import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
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
@Import(PolicyEventHistoryPersistenceAdapter.class)
@Testcontainers
class PolicyEventHistoryPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Autowired
    PolicyEventHistoryPersistenceAdapter history;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void recordsEachEventOnceEvenWhenDeliveredAgain() {
        PolicyEventDTO event = PolicyEventDTO.of(PolicyEventType.POLICY_ISSUED, PolicyFixtures.activePolicy("AP-001"));

        assertThat(history.saveIfAbsent(event)).isTrue();
        assertThat(history.saveIfAbsent(event)).isFalse();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM policy_event_history WHERE policy_id = 'AP-001'",
                Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT event_type FROM policy_event_history WHERE policy_id = 'AP-001'",
                String.class)).isEqualTo("POLICY_ISSUED");
    }
}
