package br.com.eduardoreche.insureflow.infrastructure.kafka;

import br.com.eduardoreche.insureflow.adapter.in.messaging.policy.PolicyEventReader;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String DEAD_LETTER_SUFFIX = ".DLT";
    public static final String DEAD_LETTERED = "insureflow.policy.events.dead_lettered";

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafka, MeterRegistry meters) {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(kafka,
                (record, exception) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, record.partition()));
        ConsumerAwareRecordRecoverer countedDeadLetter = (record, consumer, exception) -> {
            meters.counter(DEAD_LETTERED).increment();
            log.atError().addKeyValue("policyId", record.key()).log("Policy event sent to the dead-letter topic");
            deadLetter.accept(record, consumer, exception);
        };
        DefaultErrorHandler handler = new DefaultErrorHandler(countedDeadLetter, new FixedBackOff(1_000L, 2L));
        handler.addNotRetryableExceptions(PolicyEventReader.UnreadablePolicyEventException.class);
        return handler;
    }
}
