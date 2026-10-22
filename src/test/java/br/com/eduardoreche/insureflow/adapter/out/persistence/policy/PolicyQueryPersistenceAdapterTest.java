package br.com.eduardoreche.insureflow.adapter.out.persistence.policy;

import br.com.eduardoreche.insureflow.application.policy.PolicyFixtures;
import br.com.eduardoreche.insureflow.application.policy.dto.PolicyPageDTO;
import br.com.eduardoreche.insureflow.application.policy.dto.PolicySummaryDTO;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PolicyPersistenceAdapter.class, PolicyQueryPersistenceAdapter.class, PolicyMapper.class})
@Testcontainers
class PolicyQueryPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Autowired
    PolicyPersistenceAdapter writer;

    @Autowired
    PolicyQueryPersistenceAdapter query;

    @Test
    void returnsAnEmptyPageWhenThereAreNoPolicies() {
        PolicyPageDTO page = query.findPage(1, 25);

        assertThat(page.policies()).isEmpty();
        assertThat(page.totalRecords()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void ordersByPolicyIdAndReportsTheTotals() {
        writer.save(PolicyFixtures.activePolicy("AP-003"));
        writer.save(PolicyFixtures.activePolicy("AP-001"));
        writer.save(PolicyFixtures.activePolicy("AP-002"));

        PolicyPageDTO page = query.findPage(1, 2);

        assertThat(page.policies()).containsExactly(
                new PolicySummaryDTO("AP-001", "Residencial Essencial"),
                new PolicySummaryDTO("AP-002", "Residencial Essencial"));
        assertThat(page.totalRecords()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void returnsTheRemainingPoliciesOnTheLastPage() {
        writer.save(PolicyFixtures.activePolicy("AP-001"));
        writer.save(PolicyFixtures.activePolicy("AP-002"));
        writer.save(PolicyFixtures.activePolicy("AP-003"));

        PolicyPageDTO page = query.findPage(2, 2);

        assertThat(page.policies()).extracting(PolicySummaryDTO::policyId).containsExactly("AP-003");
    }

    @Test
    void returnsNoPoliciesBeyondTheLastPageButKeepsTheTotals() {
        writer.save(PolicyFixtures.activePolicy("AP-001"));

        PolicyPageDTO page = query.findPage(5, 25);

        assertThat(page.policies()).isEmpty();
        assertThat(page.totalRecords()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void keepsListingCancelledPolicies() {
        writer.save(PolicyFixtures.activePolicy("AP-001"));
        var loaded = writer.findByPolicyId("AP-001").orElseThrow();
        loaded.cancel(CancellationReason.OUTROS, PolicyFixtures.NOW);
        writer.update(loaded);

        assertThat(query.findPage(1, 25).policies()).extracting(PolicySummaryDTO::policyId)
                .containsExactly("AP-001");
    }
}
