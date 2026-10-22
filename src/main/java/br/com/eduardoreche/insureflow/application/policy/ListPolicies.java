package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.application.policy.dto.PolicyPageDTO;
import org.springframework.transaction.annotation.Transactional;

public class ListPolicies {

    private final PolicyQueryRepository policies;

    public ListPolicies(PolicyQueryRepository policies) {
        this.policies = policies;
    }

    @Transactional(readOnly = true)
    public PolicyPageDTO execute(int page, int pageSize) {
        return policies.findPage(page, pageSize);
    }
}
