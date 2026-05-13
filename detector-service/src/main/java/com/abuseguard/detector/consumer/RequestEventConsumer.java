package com.abuseguard.detector.consumer;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.engine.DetectionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RequestEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RequestEventConsumer.class);

    private final DetectionEngine detectionEngine;
    private final ObjectMapper objectMapper;

    public RequestEventConsumer(DetectionEngine detectionEngine, ObjectMapper objectMapper) {
        this.detectionEngine = detectionEngine;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "request-events", groupId = "detector-group")
    public void consume(String message) {
        try {
            RequestEvent event = objectMapper.readValue(message, RequestEvent.class);
            log.debug("Processing request event: {}", event.eventId());
            detectionEngine.process(event);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }
}