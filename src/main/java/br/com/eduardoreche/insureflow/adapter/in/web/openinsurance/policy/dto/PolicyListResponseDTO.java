package br.com.eduardoreche.insureflow.adapter.in.web.openinsurance.policy.dto;

import br.com.eduardoreche.insureflow.application.policy.dto.PolicyPageDTO;

import java.util.List;

public record PolicyListResponseDTO(List<PolicyDTO> data, MetaDTO meta) {

    public static PolicyListResponseDTO from(PolicyPageDTO page) {
        return new PolicyListResponseDTO(
                page.policies().stream().map(policy -> new PolicyDTO(policy.policyId(), policy.productName())).toList(),
                new MetaDTO(page.totalRecords(), page.totalPages()));
    }

    public record PolicyDTO(String policyId, String productName) {
    }

    public record MetaDTO(long totalRecords, int totalPages) {
    }
}
