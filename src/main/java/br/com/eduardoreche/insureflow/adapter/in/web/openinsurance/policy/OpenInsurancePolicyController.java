package br.com.eduardoreche.insureflow.adapter.in.web.openinsurance.policy;

import br.com.eduardoreche.insureflow.adapter.in.web.openinsurance.policy.dto.PolicyInfoResponseDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.openinsurance.policy.dto.PolicyListResponseDTO;
import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.ListPolicies;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Open Insurance - Seguro Patrimonial",
        description = "Consultas inspiradas na API InsurancePatrimonial, sem consentimento nem OAuth2.")
@RestController
@RequestMapping("/insurance-patrimonial")
class OpenInsurancePolicyController {

    private final ListPolicies listPolicies;
    private final GetPolicy getPolicy;

    OpenInsurancePolicyController(ListPolicies listPolicies, GetPolicy getPolicy) {
        this.listPolicies = listPolicies;
        this.getPolicy = getPolicy;
    }

    @Operation(summary = "Lista as apólices", description = "Paginação simples, começando na página 1.")
    @ApiResponse(responseCode = "200", description = "Página de apólices.")
    @ApiResponse(responseCode = "400", description = "page ou page-size fora dos limites.")
    @GetMapping
    ResponseEntity<PolicyListResponseDTO> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                                               @RequestParam(name = "page-size", defaultValue = "25")
                                               @Min(1) @Max(1000) int pageSize) {
        return ResponseEntity.ok()
                .body(PolicyListResponseDTO.from(listPolicies.execute(page, pageSize)));
    }

    @Operation(summary = "Consulta os dados de uma apólice",
            description = "O documento do segurado sai mascarado. Apólices canceladas continuam visíveis.")
    @ApiResponse(responseCode = "200", description = "Dados da apólice.")
    @ApiResponse(responseCode = "404", description = "Apólice não encontrada (POLICY_NOT_FOUND).")
    @GetMapping("/{policyId}/policy-info")
    ResponseEntity<PolicyInfoResponseDTO> get(@Parameter(example = "AP-2026-000123") @PathVariable String policyId) {
        Policy policy = getPolicy.execute(policyId);
        return ResponseEntity.ok()
                .body(PolicyInfoResponseDTO.from(policy));
    }
}
