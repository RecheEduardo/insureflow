package br.com.eduardoreche.insureflow.application.policy.dto;

import java.util.List;

public record PolicyPageDTO(List<PolicySummaryDTO> policies, long totalRecords, int totalPages) {
}
