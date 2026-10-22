package br.com.eduardoreche.insureflow.application.policy.dto;

public record IdempotencyKeyDTO(String key, String requestHash) {
}
