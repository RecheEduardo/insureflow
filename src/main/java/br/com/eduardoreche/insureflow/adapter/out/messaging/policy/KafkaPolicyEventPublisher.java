package br.com.eduardoreche.insureflow.adapter.out.messaging.policy;

import br.com.eduardoreche.insureflow.application.policy.event.dto.PolicyEventDTO;
import br.com.eduardoreche.insureflow.application.policy.port.PolicyEventPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
class KafkaPolicyEventPublisher implements PolicyEventPublisher {

    static final String EVENT_ID_HEADER = "eventId";
    static final String EVENT_TYPE_HEADER = "eventType";

    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final JsonMapper json = JsonMapper.builder().build();

    KafkaPolicyEventPublisher(KafkaTemplate<String, String> kafka,
                              @Value("${insureflow.kafka.policy-events.topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void publish(List<PolicyEventDTO> events) {
        // Dispara o lote inteiro antes de esperar: o producer idempotente agrupa por partição sem perder a ordem.
        CompletableFuture<?>[] sends = events.stream().map(this::send).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(sends).join();
    }

    private CompletableFuture<?> send(PolicyEventDTO event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, event.policyId(), json.writeValueAsString(event));
        record.headers().add(EVENT_ID_HEADER, event.eventId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EVENT_TYPE_HEADER, event.type().name().getBytes(StandardCharsets.UTF_8));
        return kafka.send(record);
    }
}
