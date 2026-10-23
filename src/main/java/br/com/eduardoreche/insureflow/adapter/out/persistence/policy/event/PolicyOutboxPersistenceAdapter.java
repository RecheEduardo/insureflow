package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.event;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventOutbox;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Repository
class PolicyOutboxPersistenceAdapter implements PolicyEventOutbox {

    private final JdbcClient jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    PolicyOutboxPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbc = JdbcClient.create(jdbcTemplate);
    }

    @Override
    public void append(PolicyEventDTO event) {
        jdbc.sql("""
                        INSERT INTO policy_outbox (event_id, policy_id, payload)
                        VALUES (:eventId, :policyId, CAST(:payload AS JSONB))
                        """)
                .param("eventId", event.eventId())
                .param("policyId", event.policyId())
                .param("payload", json.writeValueAsString(event))
                .update();
    }

    @Override
    public List<PolicyEventDTO> lockPending(int limit) {
        // SKIP LOCKED faz publicadores concorrentes pegarem lotes diferentes em vez de esperarem o lock um do outro.
        return jdbc.sql("""
                        SELECT payload
                        FROM policy_outbox
                        ORDER BY id
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                        """)
                .param("limit", limit)
                .query((rs, row) -> json.readValue(rs.getString("payload"), PolicyEventDTO.class))
                .list();
    }

    @Override
    public void delete(List<UUID> eventIds) {
        jdbc.sql("DELETE FROM policy_outbox WHERE event_id IN (:eventIds)")
                .param("eventIds", eventIds)
                .update();
    }
}
