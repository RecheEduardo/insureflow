package br.com.eduardoreche.insureflow.application.policy.command;

import br.com.eduardoreche.insureflow.application.policy.command.dto.IssuePolicyResultDTO;

import java.time.Instant;

public record IdempotencyRecord(String key, String requestHash, IssuePolicyResultDTO originalResult, Instant createdAt) {
}
