package br.com.eduardoreche.insureflow.application.policy.readmodel;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicySnapshotDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyInfoViewRepository;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import org.springframework.transaction.annotation.Transactional;

public class GetPolicyInfo {

    private final PolicyInfoViewRepository views;

    public GetPolicyInfo(PolicyInfoViewRepository views) {
        this.views = views;
    }

    @Transactional(readOnly = true)
    public PolicySnapshotDTO execute(String policyId) {
        return views.findByPolicyId(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
    }
}
