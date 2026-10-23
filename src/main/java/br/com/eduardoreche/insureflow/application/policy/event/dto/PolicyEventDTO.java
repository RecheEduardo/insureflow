package br.com.eduardoreche.insureflow.application.policy.event.dto;

import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.domain.model.Policy;

import java.time.Instant;
import java.util.UUID;

public record PolicyEventDTO(
        UUID eventId,
        PolicyEventType type,
        Instant occurredAt,
        String policyId,
        long policyVersion,
        PolicySnapshotDTO policy) {

    public static PolicyEventDTO of(PolicyEventType type, Policy policy) {
        return new PolicyEventDTO(UUID.randomUUID(), type, policy.getUpdatedAt(), policy.getPolicyId(),
                policy.getVersion(), PolicySnapshotDTO.from(policy));
    }
}
