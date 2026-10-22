package br.com.eduardoreche.insureflow.adapter.in.web.admin;

import br.com.eduardoreche.insureflow.application.policy.IssuePolicy;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    PolicyController(IssuePolicy issuePolicy) {
        this.issuePolicy = issuePolicy;
    }

    @PostMapping
    ResponseEntity<IssuedPolicyResponse> issue(@Valid @RequestBody IssuePolicyRequest request) {
        Policy policy = issuePolicy.execute(request.toCommand());
        return ResponseEntity
                .created(location(policy))
                .eTag("\"" + policy.getVersion() + "\"")
                .body(new IssuedPolicyResponse(policy.getPolicyId(), policy.getStatus().name(), policy.getVersion()));
    }

    private static URI location(Policy policy) {
        return UriComponentsBuilder.fromPath("/api/v1/policies/{policyId}")
                .buildAndExpand(policy.getPolicyId()).encode().toUri();
    }

    record IssuedPolicyResponse(String policyId, String status, long version) {
    }
}
