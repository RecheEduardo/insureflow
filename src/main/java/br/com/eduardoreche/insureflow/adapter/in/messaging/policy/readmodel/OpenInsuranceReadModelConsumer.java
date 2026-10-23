package br.com.eduardoreche.insureflow.adapter.in.messaging.policy.readmodel;

import br.com.eduardoreche.insureflow.adapter.in.messaging.policy.PolicyEventReader;
import br.com.eduardoreche.insureflow.application.policy.readmodel.ProjectPolicyInfo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OpenInsuranceReadModelConsumer {

    private final PolicyEventReader reader;
    private final ProjectPolicyInfo projectPolicyInfo;

    OpenInsuranceReadModelConsumer(PolicyEventReader reader, ProjectPolicyInfo projectPolicyInfo) {
        this.reader = reader;
        this.projectPolicyInfo = projectPolicyInfo;
    }

    @KafkaListener(id = "open-insurance-read-model",
            topics = "${insureflow.kafka.policy-events.topic}",
            groupId = "${insureflow.kafka.policy-events.read-model-group}",
            concurrency = "${insureflow.kafka.policy-events.consumers-per-instance}")
    void on(String payload) {
        projectPolicyInfo.execute(reader.read(payload));
    }
}
