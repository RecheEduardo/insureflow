package br.com.eduardoreche.insureflow.adapter.in.web.admin;

import br.com.eduardoreche.insureflow.application.policy.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.IssuePolicy;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    PolicyController(IssuePolicy issuePolicy, GetPolicy getPolicy, CancelPolicy cancelPolicy) {
        this.issuePolicy = issuePolicy;
        this.getPolicy = getPolicy;
        this.cancelPolicy = cancelPolicy;
    }

    @PostMapping
    ResponseEntity<IssuedPolicyResponse> issue(@Valid @RequestBody IssuePolicyRequest request) {
        Policy policy = issuePolicy.execute(request.toCommand());
        return ResponseEntity
                .created(location(policy))
                .eTag(eTag(policy))
                .body(new IssuedPolicyResponse(policy.getPolicyId(), policy.getStatus().name(), policy.getVersion()));
    }

    @GetMapping("/{policyId}")
    ResponseEntity<PolicyResponse> get(@PathVariable String policyId) {
        Policy policy = getPolicy.execute(policyId);
        return ResponseEntity.ok()
                .eTag(eTag(policy))
                .body(PolicyResponse.from(policy));
    }

    @PostMapping("/{policyId}/cancellation")
    ResponseEntity<PolicyResponse> cancel(@PathVariable String policyId,
                                          @RequestBody(required = false) CancelPolicyRequest request) {
        Policy policy = cancelPolicy.execute(CancelPolicyRequest.toCommand(policyId, request));
        return ResponseEntity.ok()
                .eTag(eTag(policy))
                .body(PolicyResponse.from(policy));
    }

    private static String eTag(Policy policy) {
        return "\"" + policy.getVersion() + "\"";
    }

    private static URI location(Policy policy) {
        return UriComponentsBuilder.fromPath("/api/v1/policies/{policyId}")
                .buildAndExpand(policy.getPolicyId()).encode().toUri();
    }

    record IssuedPolicyResponse(String policyId, String status, long version) {
    }
}
