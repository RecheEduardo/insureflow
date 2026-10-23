package br.com.eduardoreche.insureflow.adapter.in.messaging.policy.audit;

import br.com.eduardoreche.insureflow.adapter.in.messaging.policy.PolicyEventReader;
import br.com.eduardoreche.insureflow.application.policy.audit.RecordPolicyEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class PolicyAuditConsumer {

    private final PolicyEventReader reader;
    private final RecordPolicyEvent recordPolicyEvent;

    PolicyAuditConsumer(PolicyEventReader reader, RecordPolicyEvent recordPolicyEvent) {
        this.reader = reader;
        this.recordPolicyEvent = recordPolicyEvent;
    }

    @KafkaListener(id = "policy-audit",
            topics = "${insureflow.kafka.policy-events.topic}",
            groupId = "${insureflow.kafka.policy-events.audit-group}",
            concurrency = "${insureflow.kafka.policy-events.consumers-per-instance}")
    void on(String payload) {
        recordPolicyEvent.execute(reader.read(payload));
    }
}
