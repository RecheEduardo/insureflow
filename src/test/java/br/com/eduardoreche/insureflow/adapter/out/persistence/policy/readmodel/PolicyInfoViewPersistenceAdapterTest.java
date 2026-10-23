package br.com.eduardoreche.insureflow.adapter.out.persistence.policy.readmodel;

import br.com.eduardoreche.insureflow.application.policy.PolicyFixtures;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicySnapshotDTO;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoPageDTO;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoSummaryDTO;
import br.com.eduardoreche.insureflow.domain.model.Policy;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PolicyInfoViewPersistenceAdapter.class)
@Testcontainers
class PolicyInfoViewPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.6-alpine");

    @Autowired
    PolicyInfoViewPersistenceAdapter views;

    @Test
    void storesTheFirstVersionAndRestoresTheSameSnapshot() {
        PolicySnapshotDTO issued = snapshot("AP-001", "Residencial Essencial");

        assertThat(views.saveIfNewer(0, issued)).isTrue();

        assertThat(views.findByPolicyId("AP-001")).contains(issued);
        assertThat(views.findByPolicyId("AP-999")).isEmpty();
    }

    @Test
    void replacesTheSnapshotWithANewerVersion() {
        views.saveIfNewer(0, snapshot("AP-001", "Residencial Essencial"));

        assertThat(views.saveIfNewer(1, snapshot("AP-001", "Residencial Plus"))).isTrue();

        assertThat(views.findByPolicyId("AP-001").orElseThrow().productName()).isEqualTo("Residencial Plus");
    }

    @Test
    void ignoresARepeatedVersion() {
        views.saveIfNewer(1, snapshot("AP-001", "Residencial Plus"));

        assertThat(views.saveIfNewer(1, snapshot("AP-001", "Repetido"))).isFalse();

        assertThat(views.findByPolicyId("AP-001").orElseThrow().productName()).isEqualTo("Residencial Plus");
    }

    @Test
    void ignoresAnOlderVersionThatArrivesLate() {
        views.saveIfNewer(2, snapshot("AP-001", "Residencial Premium"));

        assertThat(views.saveIfNewer(1, snapshot("AP-001", "Residencial Plus"))).isFalse();

        assertThat(views.findByPolicyId("AP-001").orElseThrow().productName()).isEqualTo("Residencial Premium");
    }

    @Test
    void keepsACancelledPolicyVisible() {
        Policy policy = PolicyFixtures.activePolicy("AP-001");
        policy.cancel(CancellationReason.OUTROS, Instant.parse("2026-11-05T09:30:00Z"));

        views.saveIfNewer(1, PolicySnapshotDTO.from(policy));

        assertThat(views.findByPolicyId("AP-001").orElseThrow().cancellationReason())
                .isEqualTo(CancellationReason.OUTROS);
    }

    @Test
    void returnsAnEmptyPageWhenThereAreNoPolicies() {
        PolicyInfoPageDTO page = views.findPage(1, 25);

        assertThat(page.policies()).isEmpty();
        assertThat(page.totalRecords()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void ordersByPolicyIdAndReportsTheTotals() {
        views.saveIfNewer(0, snapshot("AP-003", "Residencial Essencial"));
        views.saveIfNewer(0, snapshot("AP-001", "Residencial Essencial"));
        views.saveIfNewer(0, snapshot("AP-002", "Empresarial Plus"));

        PolicyInfoPageDTO page = views.findPage(1, 2);

        assertThat(page.policies()).containsExactly(
                new PolicyInfoSummaryDTO("AP-001", "Residencial Essencial"),
                new PolicyInfoSummaryDTO("AP-002", "Empresarial Plus"));
        assertThat(page.totalRecords()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void returnsTheRemainingPoliciesOnTheLastPage() {
        views.saveIfNewer(0, snapshot("AP-001", "Residencial Essencial"));
        views.saveIfNewer(0, snapshot("AP-002", "Residencial Essencial"));
        views.saveIfNewer(0, snapshot("AP-003", "Residencial Essencial"));

        PolicyInfoPageDTO page = views.findPage(2, 2);

        assertThat(page.policies()).extracting(PolicyInfoSummaryDTO::policyId).containsExactly("AP-003");
    }

    @Test
    void returnsNoPoliciesBeyondTheLastPageButKeepsTheTotals() {
        views.saveIfNewer(0, snapshot("AP-001", "Residencial Essencial"));

        PolicyInfoPageDTO page = views.findPage(5, 25);

        assertThat(page.policies()).isEmpty();
        assertThat(page.totalRecords()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    private static PolicySnapshotDTO snapshot(String policyId, String productName) {
        Policy policy = PolicyFixtures.activePolicy(policyId);
        policy.endorse(policy.getTerm(), policy.getMaxLmg(), productName, policy.getInsureds(),
                policy.getInsuredObjects(), PolicyFixtures.NOW);
        return PolicySnapshotDTO.from(policy);
    }
}
