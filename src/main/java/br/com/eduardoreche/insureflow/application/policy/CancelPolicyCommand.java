package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.domain.model.enums.CancellationReason;

public record CancelPolicyCommand(String policyId, CancellationReason reason) {
}
