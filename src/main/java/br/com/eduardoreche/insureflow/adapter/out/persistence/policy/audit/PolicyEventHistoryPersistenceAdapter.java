package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.audit;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventHistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
class PolicyEventHistoryPersistenceAdapter implements PolicyEventHistoryRepository {

    private final JdbcClient jdbc;

    PolicyEventHistoryPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbc = JdbcClient.create(jdbcTemplate);
    }

    @Override
    public boolean saveIfAbsent(PolicyEventDTO event) {
        int inserted = jdbc.sql("""
                        INSERT INTO policy_event_history (event_id, event_type, policy_id, policy_version, occurred_at)
                        VALUES (:eventId, :eventType, :policyId, :policyVersion, :occurredAt)
                        ON CONFLICT (event_id) DO NOTHING
                        """)
                .param("eventId", event.eventId())
                .param("eventType", event.type().name())
                .param("policyId", event.policyId())
                .param("policyVersion", event.policyVersion())
                .param("occurredAt", OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                .update();
        return inserted == 1;
    }
}
