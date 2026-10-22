package br.com.eduardoreche.insureflow.application.policy;

import br.com.eduardoreche.insureflow.domain.model.Policy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public class IssuePolicy {

    private final PolicyRepository policies;
    private final Clock clock;

    public IssuePolicy(PolicyRepository policies, Clock clock) {
        this.policies = policies;
        this.clock = clock;
    }

    @Transactional
    public Policy execute(IssuePolicyCommand command) {
        Policy policy = Policy.create(
                command.policyId(),
                command.documentType(),
                command.groupCertificateId(),
                command.issuanceType(),
                command.issuanceDate(),
                command.term(),
                command.maxLmg(),
                command.proposalId(),
                command.productName(),
                command.insureds(),
                command.insuredObjects(),
                LocalDate.now(clock),
                Instant.now(clock));
        policies.save(policy);
        return policy;
    }
}
