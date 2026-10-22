package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import br.com.eduardoreche.insureflow.application.policy.IdempotencyRecord;
import br.com.eduardoreche.insureflow.application.policy.IdempotencyRecordRepository;
import br.com.eduardoreche.insureflow.application.policy.dto.IssuePolicyResultDTO;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
class IdempotencyRecordPersistenceAdapter implements IdempotencyRecordRepository {

    private final JdbcClient jdbc;

    IdempotencyRecordPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbc = JdbcClient.create(jdbcTemplate);
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String key) {
        return jdbc.sql("""
                        SELECT idempotency_key, request_hash, policy_id, policy_status, policy_version, created_at
                        FROM idempotency_record
                        WHERE idempotency_key = :key
                        """)
                .param("key", key)
                .query((rs, row) -> new IdempotencyRecord(
                        rs.getString("idempotency_key"),
                        rs.getString("request_hash"),
                        new IssuePolicyResultDTO(
                                rs.getString("policy_id"),
                                PolicyStatus.valueOf(rs.getString("policy_status")),
                                rs.getLong("policy_version")),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    @Override
    public boolean saveIfAbsent(IdempotencyRecord idempotencyRecord) {
        // ON CONFLICT espera a transação concorrente com a mesma chave terminar, sem abortar a transação atual.
        int inserted = jdbc.sql("""
                        INSERT INTO idempotency_record
                            (idempotency_key, request_hash, policy_id, policy_status, policy_version, created_at)
                        VALUES (:key, :requestHash, :policyId, :status, :version, :createdAt)
                        ON CONFLICT (idempotency_key) DO NOTHING
                        """)
                .param("key", idempotencyRecord.key())
                .param("requestHash", idempotencyRecord.requestHash())
                .param("policyId", idempotencyRecord.originalResult().policyId())
                .param("status", idempotencyRecord.originalResult().status().name())
                .param("version", idempotencyRecord.originalResult().version())
                .param("createdAt", OffsetDateTime.ofInstant(idempotencyRecord.createdAt(), ZoneOffset.UTC))
                .update();
        return inserted == 1;
    }
}
