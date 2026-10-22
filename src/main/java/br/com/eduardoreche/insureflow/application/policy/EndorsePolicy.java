package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.application.policy.dto.EndorsePolicyCommandDTO;
import br.com.eduardoreche.insureflow.domain.exception.BusinessRuleViolationException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyNotFoundException;
import br.com.eduardoreche.insureflow.domain.exception.PolicyVersionMismatchException;
import br.com.eduardoreche.insureflow.domain.model.Policy;
import br.com.eduardoreche.insureflow.domain.model.Term;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

public class EndorsePolicy {

    private final PolicyRepository policies;
    private final Clock clock;

    public EndorsePolicy(PolicyRepository policies, Clock clock) {
        this.policies = policies;
        this.clock = clock;
    }

    @Transactional
    public Policy execute(EndorsePolicyCommandDTO command) {
        Policy policy = policies.findByPolicyId(command.policyId())
                .orElseThrow(() -> new PolicyNotFoundException(command.policyId()));
        if (policy.getVersion() != command.expectedVersion()) {
            throw new PolicyVersionMismatchException(command.policyId(), command.expectedVersion(),
                    policy.getVersion());
        }
        if (command.changesNothing()) {
            throw new BusinessRuleViolationException("ENDORSEMENT_WITHOUT_CHANGES",
                    "endorsement must change at least one field");
        }

        policy.endorse(
                new Term(
                        command.termStartDate() != null ? command.termStartDate() : policy.getTerm().startDate(),
                        command.termEndDate() != null ? command.termEndDate() : policy.getTerm().endDate()),
                command.maxLmg() != null ? command.maxLmg() : policy.getMaxLmg(),
                command.productName() != null ? command.productName() : policy.getProductName(),
                command.insureds() != null ? command.insureds() : policy.getInsureds(),
                command.insuredObjects() != null ? command.insuredObjects() : policy.getInsuredObjects(),
                Instant.now(clock));
        return policies.update(policy);
    }
}
