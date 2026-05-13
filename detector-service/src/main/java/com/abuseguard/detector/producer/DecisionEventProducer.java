package com.abuseguard.detector.producer;

import com.abuseguard.common.events.DecisionEvent;
import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.common.model.Decision;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DecisionEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "decision-events";

    public DecisionEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(RequestEvent requestEvent, Decision decision, String triggeredRules) {
        try {
            DecisionEvent event = DecisionEvent.fromRequestEvent(requestEvent, decision, triggeredRules).build();
            String json = objectMapper.writeValueAsString(event);
            String key = requestEvent.apiKey() != null ? requestEvent.apiKey() : requestEvent.ip();
            kafkaTemplate.send(TOPIC, key, json);
        } catch (Exception e) {
        }
    }
}