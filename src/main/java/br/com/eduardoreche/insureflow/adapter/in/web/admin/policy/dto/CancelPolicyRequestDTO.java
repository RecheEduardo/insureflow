package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto;

import br.com.eduardoreche.insureflow.application.policy.dto.CancelPolicyCommandDTO;
import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;

public record CancelPolicyRequestDTO(CancellationReason reason) {

    public static CancelPolicyCommandDTO toCommand(String policyId, CancelPolicyRequestDTO request) {
        CancellationReason reason = request == null || request.reason() == null
                ? CancellationReason.OUTROS : request.reason();
        return new CancelPolicyCommandDTO(policyId, reason);
    }
}
