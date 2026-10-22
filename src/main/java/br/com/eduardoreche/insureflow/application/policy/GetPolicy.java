package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import org.springframework.transaction.annotation.Transactional;

public class GetPolicy {

    private final PolicyRepository policies;

    public GetPolicy(PolicyRepository policies) {
        this.policies = policies;
    }

    @Transactional(readOnly = true)
    public Policy execute(String policyId) {
        return policies.findByPolicyId(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
    }
}
