package com.abuseguard.gateway.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic requestEventsTopic() {
        return TopicBuilder.name("request-events")
            .partitions(6)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic decisionEventsTopic() {
        return TopicBuilder.name("decision-events")
            .partitions(2)
            .replicas(1)
            .build();
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}