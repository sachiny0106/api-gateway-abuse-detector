package com.abuseguard.auditor.consumer;

import com.abuseguard.common.events.DecisionEvent;
import com.abuseguard.auditor.entity.DecisionEventEntity;
import com.abuseguard.auditor.repository.DecisionEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DecisionEventAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(DecisionEventAuditConsumer.class);

    private final DecisionEventRepository decisionRepository;
    private final ObjectMapper objectMapper;
    private final List<DecisionEventEntity> buffer = new ArrayList<>();

    public DecisionEventAuditConsumer(DecisionEventRepository decisionRepository, ObjectMapper objectMapper) {
        this.decisionRepository = decisionRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "decision-events", groupId = "auditor-group")
    public void consume(String message) {
        try {
            DecisionEvent event = objectMapper.readValue(message, DecisionEvent.class);
            DecisionEventEntity entity = mapToEntity(event);

            synchronized (buffer) {
                buffer.add(entity);
                if (buffer.size() >= 100) {
                    flush();
                }
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    private DecisionEventEntity mapToEntity(DecisionEvent event) {
        DecisionEventEntity entity = new DecisionEventEntity();
        entity.setEventId(event.eventId());
        entity.setRequestEventId(event.requestEventId());
        entity.setTimestamp(event.timestamp());
        entity.setIp(event.ip());
        entity.setApiKey(event.apiKey());
        entity.setAction(event.action().name());
        entity.setScore(event.score());
        entity.setReason(event.reason());
        entity.setExpiresAt(event.expiresAt());
        entity.setTriggeredRules(event.triggeredRules());
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private synchronized void flush() {
        if (buffer.isEmpty()) return;

        List<DecisionEventEntity> batch = new ArrayList<>(buffer);
        buffer.clear();

        for (DecisionEventEntity entity : batch) {
            if (!decisionRepository.existsByEventId(entity.getEventId())) {
                decisionRepository.save(entity);
            }
        }
    }
}