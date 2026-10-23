package br.com.eduardoreche.insureflow.infrastructure.policy;

import br.com.eduardoreche.insureflow.application.policy.audit.RecordPolicyEvent;
import br.com.eduardoreche.insureflow.application.policy.command.CancelPolicy;
import br.com.eduardoreche.insureflow.application.policy.command.EndorsePolicy;
import br.com.eduardoreche.insureflow.application.policy.command.IssuePolicy;
import br.com.eduardoreche.insureflow.application.policy.event.RelayPolicyEvents;
import br.com.eduardoreche.insureflow.application.policy.port.IdempotencyRecordRepository;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventHistoryRepository;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventOutbox;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventPublisher;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyInfoViewRepository;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyRepository;
import br.com.eduardoreche.insureflow.application.policy.query.GetPolicy;
import br.com.eduardoreche.insureflow.application.policy.readmodel.GetPolicyInfo;
import br.com.eduardoreche.insureflow.application.policy.readmodel.ListPolicyInfo;
import br.com.eduardoreche.insureflow.application.policy.readmodel.ProjectPolicyInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class PolicyUseCaseConfig {

    @Bean
    IssuePolicy issuePolicy(PolicyRepository policies, IdempotencyRecordRepository idempotencyRecords,
                            PolicyEventOutbox events, Clock clock) {
        return new IssuePolicy(policies, idempotencyRecords, events, clock);
    }

    @Bean
    EndorsePolicy endorsePolicy(PolicyRepository policies, PolicyEventOutbox events, Clock clock) {
        return new EndorsePolicy(policies, events, clock);
    }

    @Bean
    CancelPolicy cancelPolicy(PolicyRepository policies, PolicyEventOutbox events, Clock clock) {
        return new CancelPolicy(policies, events, clock);
    }

    @Bean
    GetPolicy getPolicy(PolicyRepository policies) {
        return new GetPolicy(policies);
    }

    @Bean
    RelayPolicyEvents relayPolicyEvents(PolicyEventOutbox outbox, PolicyEventPublisher publisher,
                                        @Value("${insureflow.outbox.batch-size}") int batchSize) {
        return new RelayPolicyEvents(outbox, publisher, batchSize);
    }

    @Bean
    ProjectPolicyInfo projectPolicyInfo(PolicyInfoViewRepository views) {
        return new ProjectPolicyInfo(views);
    }

    @Bean
    GetPolicyInfo getPolicyInfo(PolicyInfoViewRepository views) {
        return new GetPolicyInfo(views);
    }

    @Bean
    ListPolicyInfo listPolicyInfo(PolicyInfoViewRepository views) {
        return new ListPolicyInfo(views);
    }

    @Bean
    RecordPolicyEvent recordPolicyEvent(PolicyEventHistoryRepository history) {
        return new RecordPolicyEvent(history);
    }
}
