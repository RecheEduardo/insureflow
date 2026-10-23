package br.com.eduardoreche.insureflow.infrastructure.policy;

import br.com.eduardoreche.insureflow.infrastructure.kafka.KafkaConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class PolicyTopicConfig {

    private final String topic;
    private final int partitions;
    private final int replicas;
    private final long retentionMs;

    PolicyTopicConfig(@Value("${insureflow.kafka.policy-events.topic}") String topic,
                      @Value("${insureflow.kafka.policy-events.partitions}") int partitions,
                      @Value("${insureflow.kafka.policy-events.replicas}") int replicas,
                      @Value("${insureflow.kafka.policy-events.retention-ms}") long retentionMs) {
        this.topic = topic;
        this.partitions = partitions;
        this.replicas = replicas;
        this.retentionMs = retentionMs;
    }

    @Bean
    NewTopic policyEventsTopic() {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs))
                .build();
    }

    @Bean
    NewTopic policyEventsDeadLetterTopic() {
        return TopicBuilder.name(topic + KafkaConfig.DEAD_LETTER_SUFFIX)
                .partitions(partitions)
                .replicas(replicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs))
                .build();
    }
}
