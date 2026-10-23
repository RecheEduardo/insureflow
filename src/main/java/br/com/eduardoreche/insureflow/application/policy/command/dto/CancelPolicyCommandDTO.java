package br.com.eduardoreche.insureflow.application.policy.command.dto;

import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;

public record CancelPolicyCommandDTO(String policyId, CancellationReason reason) {
}
