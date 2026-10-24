package br.com.eduardoreche.insureflow.adapter.in.messaging.policy.readmodel;

import br.com.eduardoreche.insureflow.adapter.in.messaging.policy.PolicyEventReader;
import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.readmodel.ProjectPolicyInfo;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
class OpenInsuranceReadModelConsumer {

    static final String PROJECTION_DELAY = "insureflow.policy.projection.delay";

    private final PolicyEventReader reader;
    private final ProjectPolicyInfo projectPolicyInfo;
    private final Clock clock;
    private final Timer projectionDelay;

    OpenInsuranceReadModelConsumer(PolicyEventReader reader, ProjectPolicyInfo projectPolicyInfo, Clock clock,
                                   MeterRegistry meters) {
        this.reader = reader;
        this.projectPolicyInfo = projectPolicyInfo;
        this.clock = clock;
        this.projectionDelay = Timer.builder(PROJECTION_DELAY)
                .description("Time from a policy change until it is visible in policy-info")
                .register(meters);
    }

    @KafkaListener(id = "open-insurance-read-model",
            topics = "${insureflow.kafka.policy-events.topic}",
            groupId = "${insureflow.kafka.policy-events.read-model-group}",
            concurrency = "${insureflow.kafka.policy-events.consumers-per-instance}")
    void on(String payload) {
        PolicyEventDTO event = reader.read(payload);
        // Evento antigo descartado pelo saveIfNewer não muda o que o mercado vê; medi-lo distorceria o atraso.
        if (projectPolicyInfo.execute(event)) {
            projectionDelay.record(Duration.between(event.occurredAt(), clock.instant()));
        }
    }
}
