package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.readmodel;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicySnapshotDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyInfoViewRepository;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoPageDTO;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoSummaryDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

@Repository
class PolicyInfoViewPersistenceAdapter implements PolicyInfoViewRepository {

    private final JdbcClient jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    PolicyInfoViewPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbc = JdbcClient.create(jdbcTemplate);
    }

    @Override
    public boolean saveIfNewer(long policyVersion, PolicySnapshotDTO policy) {
        // A entrega é pelo menos uma vez: o WHERE descarta evento repetido ou atrasado, que não traz versão maior.
        int changed = jdbc.sql("""
                        INSERT INTO policy_info_view (policy_id, policy_version, product_name, snapshot)
                        VALUES (:policyId, :policyVersion, :productName, CAST(:snapshot AS JSONB))
                        ON CONFLICT (policy_id) DO UPDATE
                        SET policy_version = EXCLUDED.policy_version,
                            product_name = EXCLUDED.product_name,
                            snapshot = EXCLUDED.snapshot
                        WHERE policy_info_view.policy_version < EXCLUDED.policy_version
                        """)
                .param("policyId", policy.policyId())
                .param("policyVersion", policyVersion)
                .param("productName", policy.productName())
                .param("snapshot", json.writeValueAsString(policy))
                .update();
        return changed == 1;
    }

    @Override
    public Optional<PolicySnapshotDTO> findByPolicyId(String policyId) {
        return jdbc.sql("SELECT snapshot FROM policy_info_view WHERE policy_id = :policyId")
                .param("policyId", policyId)
                .query((rs, row) -> json.readValue(rs.getString("snapshot"), PolicySnapshotDTO.class))
                .optional();
    }

    @Override
    public PolicyInfoPageDTO findPage(int page, int pageSize) {
        long totalRecords = jdbc.sql("SELECT count(*) FROM policy_info_view").query(Long.class).single();
        List<PolicyInfoSummaryDTO> policies = jdbc.sql("""
                        SELECT policy_id, product_name
                        FROM policy_info_view
                        ORDER BY policy_id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("limit", pageSize)
                .param("offset", (long) (page - 1) * pageSize)
                .query((rs, row) -> new PolicyInfoSummaryDTO(rs.getString("policy_id"), rs.getString("product_name")))
                .list();
        int totalPages = (int) ((totalRecords + pageSize - 1) / pageSize);
        return new PolicyInfoPageDTO(policies, totalRecords, totalPages);
    }
}
