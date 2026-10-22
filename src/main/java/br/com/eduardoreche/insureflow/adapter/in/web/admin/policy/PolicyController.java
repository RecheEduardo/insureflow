package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

import br.com.eduardoreche.insureflow.adapter.in.web.admin.IdempotencyKey;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.IfMatch;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.CancelPolicyRequestDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.EndorsePolicyRequestDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.IssuePolicyRequestDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.IssuedPolicyResponseDTO;
import br.com.eduardoreche.insureflow.adapter.in.web.admin.policy.dto.PolicyResponseDTO;
import br.com.eduardoreche.insureflow.application.policy.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.dto.IssuePolicyResultDTO;
import br.com.eduardoreche.insureflow.domain.model.Policy;
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

    @PostMapping
    ResponseEntity<IssuedPolicyResponseDTO> issue(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                  @Valid @RequestBody IssuePolicyRequestDTO request) {
        IssuePolicyResultDTO result = issuePolicy.execute(
                request.toCommand(),
                IdempotencyKey.of(idempotencyKey, json.writeValueAsString(request)));

        return ResponseEntity
                .created(location(result.policyId()))
                .eTag(String.valueOf(result.version()))
                .body(new IssuedPolicyResponseDTO(result.policyId(), result.status().name(), result.version()));
    }

    @GetMapping("/{policyId}")
    ResponseEntity<PolicyResponseDTO> get(@PathVariable String policyId) {
        Policy policy = getPolicy.execute(policyId);
        return ResponseEntity.ok()
                .eTag(String.valueOf(policy.getVersion()))
                .body(PolicyResponseDTO.from(policy));
    }

    @PatchMapping("/{policyId}")
    ResponseEntity<PolicyResponseDTO> endorse(@PathVariable String policyId,
                                              @RequestHeader("If-Match") String ifMatch,
                                              @Valid @RequestBody EndorsePolicyRequestDTO request) {
        Policy policy = endorsePolicy.execute(request.toCommand(policyId, IfMatch.toVersion(ifMatch)));
        return ResponseEntity.ok()
                .eTag(String.valueOf(policy.getVersion()))
                .body(PolicyResponseDTO.from(policy));
    }

    @PostMapping("/{policyId}/cancellation")
    ResponseEntity<PolicyResponseDTO> cancel(@PathVariable String policyId,
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
