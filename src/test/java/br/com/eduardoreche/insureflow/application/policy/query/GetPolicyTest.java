package br.com.eduardoreche.insureflow.application.policy.query;

import br.com.eduardoreche.insureflow.application.policy.port.PolicyRepository;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.NOW;
import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetPolicyTest {

    @Test
    void returnsPolicyFoundByPolicyId() {
        Policy stored = activePolicy("AP-001");
        GetPolicy getPolicy = new GetPolicy(repositoryWith(stored));

        assertThat(getPolicy.execute("AP-001")).isSameAs(stored);
    }

    @Test
    void returnsCancelledPolicyWithItsStatus() {
        Policy stored = activePolicy("AP-001");
        stored.cancel(CancellationReason.PEDIDO_DO_SEGURADO, NOW);
        GetPolicy getPolicy = new GetPolicy(repositoryWith(stored));

        assertThat(getPolicy.execute("AP-001").getStatus()).isEqualTo(PolicyStatus.CANCELLED);
    }

    @Test
    void failsWhenPolicyDoesNotExist() {
        GetPolicy getPolicy = new GetPolicy(repositoryWith(activePolicy("AP-001")));

        assertThatThrownBy(() -> getPolicy.execute("AP-999"))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessageContaining("AP-999");
    }

    private static PolicyRepository repositoryWith(Policy stored) {
        return new PolicyRepository() {
            @Override
            public void save(Policy policy) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Policy update(Policy policy) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Policy> findByPolicyId(String policyId) {
                return stored.getPolicyId().equals(policyId) ? Optional.of(stored) : Optional.empty();
            }
        };
    }
}
