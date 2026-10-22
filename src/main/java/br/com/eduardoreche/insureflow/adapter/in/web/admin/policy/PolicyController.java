package br.com.eduardoreche.insureflow.adapter.in.web.admin.policy;

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

import java.net.URI;

@RestController
@RequestMapping("/api/v1/policies")
class PolicyController {

    private final IssuePolicy issuePolicy;
    private final GetPolicy getPolicy;
    private final CancelPolicy cancelPolicy;
    private final EndorsePolicy endorsePolicy;

    PolicyController(IssuePolicy issuePolicy, GetPolicy getPolicy, CancelPolicy cancelPolicy,
                     EndorsePolicy endorsePolicy) {
        this.issuePolicy = issuePolicy;
        this.getPolicy = getPolicy;
        this.cancelPolicy = cancelPolicy;
        this.endorsePolicy = endorsePolicy;
    }

    @PostMapping
    ResponseEntity<IssuedPolicyResponseDTO> issue(@Valid @RequestBody IssuePolicyRequestDTO request) {
        Policy policy = issuePolicy.execute(request.toCommand());
        return ResponseEntity
                .created(location(policy))
                .eTag(eTag(policy))
                .body(new IssuedPolicyResponseDTO(policy.getPolicyId(), policy.getStatus().name(), policy.getVersion()));
    }

    @GetMapping("/{policyId}")
    ResponseEntity<PolicyResponseDTO> get(@PathVariable String policyId) {
        Policy policy = getPolicy.execute(policyId);
        return ResponseEntity.ok()
                .eTag(eTag(policy))
                .body(PolicyResponseDTO.from(policy));
    }

    @PatchMapping("/{policyId}")
    ResponseEntity<PolicyResponseDTO> endorse(@PathVariable String policyId,
                                              @RequestHeader("If-Match") String ifMatch,
                                              @Valid @RequestBody EndorsePolicyRequestDTO request) {
        Policy policy = endorsePolicy.execute(request.toCommand(policyId, IfMatch.toVersion(ifMatch)));
        return ResponseEntity.ok()
                .eTag(eTag(policy))
                .body(PolicyResponseDTO.from(policy));
    }

    @PostMapping("/{policyId}/cancellation")
    ResponseEntity<PolicyResponseDTO> cancel(@PathVariable String policyId,
                                             @RequestBody(required = false) CancelPolicyRequestDTO request) {
        Policy policy = cancelPolicy.execute(CancelPolicyRequestDTO.toCommand(policyId, request));
        return ResponseEntity.ok()
                .eTag(eTag(policy))
                .body(PolicyResponseDTO.from(policy));
    }

    private static String eTag(Policy policy) {
        return "\"" + policy.getVersion() + "\"";
    }

    private static URI location(Policy policy) {
        return UriComponentsBuilder.fromPath("/api/v1/policies/{policyId}")
                .buildAndExpand(policy.getPolicyId()).encode().toUri();
    }
}
