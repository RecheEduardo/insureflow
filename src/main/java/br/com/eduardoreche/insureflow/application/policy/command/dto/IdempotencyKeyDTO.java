package br.com.eduardoreche.insureflow.application.policy.command.dto;

public record IdempotencyKeyDTO(String key, String requestHash) {
}
