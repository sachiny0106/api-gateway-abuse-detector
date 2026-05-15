package com.abuseguard.auditor.consumer;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.auditor.entity.RequestEventEntity;
import com.abuseguard.auditor.service.BatchAuditWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RequestEventAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(RequestEventAuditConsumer.class);

    private final BatchAuditWriter batchAuditWriter;
    private final ObjectMapper objectMapper;

    public RequestEventAuditConsumer(BatchAuditWriter batchAuditWriter, ObjectMapper objectMapper) {
        this.batchAuditWriter = batchAuditWriter;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "request-events", groupId = "auditor-group")
    public void consume(String message) {
        try {
            RequestEvent event = objectMapper.readValue(message, RequestEvent.class);
            RequestEventEntity entity = mapToEntity(event);
            batchAuditWriter.add(entity);
        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    private RequestEventEntity mapToEntity(RequestEvent event) {
        RequestEventEntity entity = new RequestEventEntity();
        entity.setEventId(event.eventId());
        entity.setTimestamp(event.timestamp());
        entity.setIp(event.ip());
        entity.setApiKey(event.apiKey());
        entity.setEndpoint(event.endpoint());
        entity.setMethod(event.method());
        entity.setStatusCode(event.statusCode());
        entity.setPayloadSize(event.payloadSize());
        entity.setUserAgent(event.userAgent());
        entity.setLatencyMs(event.latencyMs());
        entity.setContentType(event.contentType());
        entity.setForwardedFor(event.forwardedFor());
        return entity;
    }
}