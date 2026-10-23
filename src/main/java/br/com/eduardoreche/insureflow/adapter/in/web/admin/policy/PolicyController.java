package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

import br.com.eduardoreche.insureflow.adapter.in.web.admin.IdempotencyKey;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.IfMatch;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.CancelPolicyRequestDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.EndorsePolicyRequestDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.IssuePolicyRequestDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.IssuedPolicyResponseDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.PolicyResponseDTO;
import br.com.eduardoreche.insureflow.application.policy.command.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.command.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.dto.IssuePolicyResultDTO;
import br.com.eduardoreche.insureflow.application.policy.query.GetPolicy;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;

@Tag(name = "Apólices (administrativa)", description = "Emissão, consulta, endosso e cancelamento pelo backoffice.")
@RestController
@RequestMapping("/api/v1/policies")
class PolicyController {

    private final IssuePolicy issuePolicy;
    private final GetPolicy getPolicy;
    private final CancelPolicy cancelPolicy;
    private final EndorsePolicy endorsePolicy;
    private final JsonMapper json;

    PolicyController(IssuePolicy issuePolicy, GetPolicy getPolicy, CancelPolicy cancelPolicy,
                     EndorsePolicy endorsePolicy, JsonMapper json) {
        this.issuePolicy = issuePolicy;
        this.getPolicy = getPolicy;
        this.cancelPolicy = cancelPolicy;
        this.endorsePolicy = endorsePolicy;
        this.json = json;
    }

    @Operation(summary = "Emite uma apólice",
            description = "Repetir a mesma Idempotency-Key com o mesmo corpo devolve a resposta original sem emitir de novo.")
    @ApiResponse(responseCode = "201", description = "Apólice emitida.", headers = {
            @Header(name = "Location", description = "URL da apólice emitida."),
            @Header(name = "ETag", description = "Versão atual, a enviar no If-Match do endosso.")})
    @ApiResponse(responseCode = "400", description = "Corpo ou Idempotency-Key ausente ou inválido.")
    @ApiResponse(responseCode = "409", description = "Já existe apólice com este policyId (POLICY_DUPLICATE).")
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada, ou Idempotency-Key reutilizada com outro corpo.")
    @PostMapping
    ResponseEntity<IssuedPolicyResponseDTO> issue(
            @Parameter(description = "Identificador único da emissão, de 1 a 255 caracteres ASCII visíveis.",
                    example = "3f1c2b9e-7d4a-4c1e-9b8a-2e6f5d4c3b2a")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = PolicyExamples.ISSUE)))
            @Valid @RequestBody IssuePolicyRequestDTO request) {
        IssuePolicyResultDTO result = issuePolicy.execute(
                request.toCommand(),
                IdempotencyKey.of(idempotencyKey, json.writeValueAsString(request)));

        return ResponseEntity
                .created(location(result.policyId()))
                .eTag(String.valueOf(result.version()))
                .body(new IssuedPolicyResponseDTO(result.policyId(), result.status().name(), result.version()));
    }

    @Operation(summary = "Consulta uma apólice",
            description = "Apólices canceladas continuam consultáveis, com status, motivo e data do cancelamento.")
    @ApiResponse(responseCode = "200", description = "Apólice encontrada.",
            headers = @Header(name = "ETag", description = "Versão atual, a enviar no If-Match do endosso."))
    @ApiResponse(responseCode = "404", description = "Apólice não encontrada (POLICY_NOT_FOUND).")
    @GetMapping("/{policyId}")
    ResponseEntity<PolicyResponseDTO> get(@Parameter(example = "AP-2026-000123") @PathVariable String policyId) {
        Policy policy = getPolicy.execute(policyId);
        return ResponseEntity.ok()
                .eTag(String.valueOf(policy.getVersion()))
                .body(PolicyResponseDTO.from(policy));
    }

    @Operation(summary = "Endossa uma apólice",
            description = "Altera só os campos enviados; listas enviadas substituem as atuais por inteiro. "
                    + "Identificadores não fazem parte do corpo e não podem ser alterados.")
    @ApiResponse(responseCode = "200", description = "Apólice endossada.",
            headers = @Header(name = "ETag", description = "Nova versão da apólice."))
    @ApiResponse(responseCode = "400", description = "Corpo ou If-Match inválido.")
    @ApiResponse(responseCode = "404", description = "Apólice não encontrada (POLICY_NOT_FOUND).")
    @ApiResponse(responseCode = "409", description = "Apólice cancelada não aceita endosso (POLICY_CANCELLED).")
    @ApiResponse(responseCode = "412", description = "A apólice mudou desde a leitura (POLICY_VERSION_MISMATCH).")
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada, ou endosso sem nenhum campo.")
    @ApiResponse(responseCode = "428", description = "If-Match ausente (IF_MATCH_REQUIRED).")
    @PatchMapping("/{policyId}")
    ResponseEntity<PolicyResponseDTO> endorse(
            @Parameter(example = "AP-2026-000123") @PathVariable String policyId,
            @Parameter(description = "ETag devolvido na última leitura da apólice.", example = "0")
            @RequestHeader("If-Match") String ifMatch,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = PolicyExamples.ENDORSE)))
            @Valid @RequestBody EndorsePolicyRequestDTO request) {
        Policy policy = endorsePolicy.execute(request.toCommand(policyId, IfMatch.toVersion(ifMatch)));
        return ResponseEntity.ok()
                .eTag(String.valueOf(policy.getVersion()))
                .body(PolicyResponseDTO.from(policy));
    }

    @Operation(summary = "Cancela uma apólice",
            description = "Cancelar não apaga: a apólice continua consultável. Sem corpo, o motivo é OUTROS. "
                    + "Cancelar de novo devolve a apólice como está, sem mudar motivo nem data.")
    @ApiResponse(responseCode = "200", description = "Apólice cancelada.",
            headers = @Header(name = "ETag", description = "Versão atual da apólice."))
    @ApiResponse(responseCode = "400", description = "Motivo de cancelamento inválido.")
    @ApiResponse(responseCode = "404", description = "Apólice não encontrada (POLICY_NOT_FOUND).")
    @PostMapping("/{policyId}/cancellation")
    ResponseEntity<PolicyResponseDTO> cancel(
            @Parameter(example = "AP-2026-000123") @PathVariable String policyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false, content = @Content(
                    examples = @ExampleObject(value = PolicyExamples.CANCEL)))
            @RequestBody(required = false) CancelPolicyRequestDTO request) {
        Policy policy = cancelPolicy.execute(CancelPolicyRequestDTO.toCommand(policyId, request));
        return ResponseEntity.ok()
                .eTag(String.valueOf(policy.getVersion()))
                .body(PolicyResponseDTO.from(policy));
    }

    private static URI location(String policyId) {
        return UriComponentsBuilder.fromPath("/api/v1/policies/{policyId}")
                .buildAndExpand(policyId).encode().toUri();
    }
}
