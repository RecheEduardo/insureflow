package br.com.eduardoreche.insureflow.infrastructure.kafka;

import br.com.eduardoreche.insureflow.adapter.in.messaging.policy.PolicyEventReader;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String DEAD_LETTER_SUFFIX = ".DLT";

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafka) {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(kafka,
                (record, exception) -> new TopicPartition(record.topic() + DEAD_LETTER_SUFFIX, record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(deadLetter, new FixedBackOff(1_000L, 2L));
        handler.addNotRetryableExceptions(PolicyEventReader.UnreadablePolicyEventException.class);
        return handler;
    }
}
