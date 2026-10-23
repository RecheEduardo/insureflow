package br.com.eduardoreche.insureflow.application.policy.readmodel.dto;

import java.util.List;

public record PolicyInfoPageDTO(List<PolicyInfoSummaryDTO> policies, long totalRecords, int totalPages) {
}
