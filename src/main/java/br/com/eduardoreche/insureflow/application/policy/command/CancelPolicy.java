package br.com.eduardoreche.insureflow.application.policy.command;

import br.com.eduardoreche.insureflow.application.policy.command.dto.CancelPolicyCommandDTO;
import br.com.eduardoreche.insureflow.application.policy.event.PolicyEventType;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventOutbox;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyRepository;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class CancelPolicy {

    private final PolicyRepository policies;
    private final PolicyEventOutbox events;
    private final Clock clock;

    public CancelPolicy(PolicyRepository policies, PolicyEventOutbox events, Clock clock) {
        this.policies = policies;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Policy execute(CancelPolicyCommandDTO command) {
        Policy policy = policies.findByPolicyId(command.policyId())
                .orElseThrow(() -> new PolicyNotFoundException(command.policyId()));
        if (policy.isCancelled()) {
            return policy;
        }
        policy.cancel(command.reason(), Instant.now(clock));
        Policy cancelled = policies.update(policy);
        events.append(PolicyEventDTO.of(PolicyEventType.POLICY_CANCELLED, cancelled));
        return cancelled;
    }
}
