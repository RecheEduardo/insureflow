package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class CancelPolicy {

    private final PolicyRepository policies;
    private final Clock clock;

    public CancelPolicy(PolicyRepository policies, Clock clock) {
        this.policies = policies;
        this.clock = clock;
    }

    @Transactional
    public Policy execute(CancelPolicyCommand command) {
        Policy policy = policies.findByPolicyId(command.policyId())
                .orElseThrow(() -> new PolicyNotFoundException(command.policyId()));
        if (policy.isCancelled()) {
            return policy;
        }
        policy.cancel(command.reason(), Instant.now(clock));
        return policies.update(policy);
    }
}
