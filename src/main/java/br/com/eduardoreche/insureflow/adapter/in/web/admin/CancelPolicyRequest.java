package br.com.eduardoreche.insureflow.adapter.in.web.admin;

import br.com.eduardoreche.insureflow.application.policy.CancelPolicyCommand;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;

public record CancelPolicyRequest(CancellationReason reason) {

    static CancelPolicyCommand toCommand(String policyId, CancelPolicyRequest request) {
        CancellationReason reason = request == null || request.reason() == null
                ? CancellationReason.OUTROS : request.reason();
        return new CancelPolicyCommand(policyId, reason);
    }
}
