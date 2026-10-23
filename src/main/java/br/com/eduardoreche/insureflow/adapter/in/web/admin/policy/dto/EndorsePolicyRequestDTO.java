package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto;

import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.IssuePolicyRequestDTO.InsuredObjectRequestDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.IssuePolicyRequestDTO.InsuredRequestDTO;
import br.com.eduardoreche.insureflow.application.policy.command.dto.EndorsePolicyCommandDTO;
import br.com.eduardoreche.insureflow.domain.model.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EndorsePolicyRequestDTO(
        LocalDate termStartDate,
        LocalDate termEndDate,
        @Pattern(regexp = "^\\d{1,16}\\.\\d{2}$") String maxLMG,
        @Size(min = 1, max = 70) @Pattern(regexp = ".*\\S.*") String productName,
        @Size(min = 1) @Valid List<InsuredRequestDTO> insureds,
        @Size(min = 1) @Valid List<InsuredObjectRequestDTO> insuredObjects) {

    public EndorsePolicyCommandDTO toCommand(String policyId, long expectedVersion) {
        return new EndorsePolicyCommandDTO(
                policyId, expectedVersion, termStartDate, termEndDate,
                maxLMG == null ? null : Money.brl(new BigDecimal(maxLMG)),
                productName,
                insureds == null ? null : insureds.stream().map(InsuredRequestDTO::toDomain).toList(),
                insuredObjects == null ? null : insuredObjects.stream().map(InsuredObjectRequestDTO::toDomain).toList());
    }
}
