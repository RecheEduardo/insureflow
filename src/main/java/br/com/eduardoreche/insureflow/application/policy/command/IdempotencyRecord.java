package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.application.policy.dto.IssuePolicyResultDTO;

import java.time.Instant;

public record IdempotencyRecord(String key, String requestHash, IssuePolicyResultDTO originalResult, Instant createdAt) {
}
