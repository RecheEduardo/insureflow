package br.com.eduardoreche.insureflow.application.policy.command.dto;

import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.enums.PolicyStatus;

public record IssuePolicyResultDTO(String policyId, PolicyStatus status, long version) {

    public static IssuePolicyResultDTO from(Policy policy) {
        return new IssuePolicyResultDTO(policy.getPolicyId(), policy.getStatus(), policy.getVersion());
    }
}
