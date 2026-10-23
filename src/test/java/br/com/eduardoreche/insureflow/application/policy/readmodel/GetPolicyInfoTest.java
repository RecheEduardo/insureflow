package br.com.eduardoreche.insureflow.application.policy.readmodel;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicySnapshotDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyInfoViewRepository;
import br.com.eduardoreche.insureflow.application.policy.readmodel.dto.PolicyInfoPageDTO;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static br.com.eduardoreche.insureflow.application.policy.PolicyFixtures.activePolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetPolicyInfoTest {

    @Test
    void returnsTheProjectedPolicy() {
        PolicySnapshotDTO projected = PolicySnapshotDTO.from(activePolicy("AP-001"));
        GetPolicyInfo getPolicyInfo = new GetPolicyInfo(viewWith(projected));

        assertThat(getPolicyInfo.execute("AP-001")).isEqualTo(projected);
    }

    @Test
    void failsWhenThePolicyIsNotInTheProjection() {
        GetPolicyInfo getPolicyInfo = new GetPolicyInfo(viewWith(PolicySnapshotDTO.from(activePolicy("AP-001"))));

        assertThatThrownBy(() -> getPolicyInfo.execute("AP-999"))
                .isInstanceOfSatisfying(PolicyNotFoundException.class,
                        e -> assertThat(e.getCode()).isEqualTo("POLICY_NOT_FOUND"));
    }

    private static PolicyInfoViewRepository viewWith(PolicySnapshotDTO projected) {
        return new PolicyInfoViewRepository() {
            @Override
            public boolean saveIfNewer(long policyVersion, PolicySnapshotDTO policy) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<PolicySnapshotDTO> findByPolicyId(String policyId) {
                return projected.policyId().equals(policyId) ? Optional.of(projected) : Optional.empty();
            }

            @Override
            public PolicyInfoPageDTO findPage(int page, int pageSize) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
