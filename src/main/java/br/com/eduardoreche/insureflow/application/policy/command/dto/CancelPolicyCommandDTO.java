package br.com.eduardoreche.insureflow.application.policy.dto;

import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;

public record CancelPolicyCommandDTO(String policyId, CancellationReason reason) {
}
