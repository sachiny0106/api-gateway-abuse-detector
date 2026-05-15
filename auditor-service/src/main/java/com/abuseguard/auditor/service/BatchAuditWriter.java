package com.abuseguard.auditor.service;

import com.abuseguard.auditor.entity.RequestEventEntity;
import com.abuseguard.auditor.repository.RequestEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class BatchAuditWriter {

    private static final Logger log = LoggerFactory.getLogger(BatchAuditWriter.class);
    private static final int BATCH_SIZE = 100;

    private final RequestEventRepository requestRepository;
    private final Counter auditCounter;
    private final ConcurrentLinkedQueue<RequestEventEntity> buffer = new ConcurrentLinkedQueue<>();

    public BatchAuditWriter(RequestEventRepository requestRepository, MeterRegistry meterRegistry) {
        this.requestRepository = requestRepository;
        this.auditCounter = Counter.builder("auditor_events_written_total")
            .description("Total number of audit events written")
            .register(meterRegistry);
    }

    public void add(RequestEventEntity event) {
        event.setCreatedAt(Instant.now());
        buffer.add(event);

        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    @Scheduled(fixedDelay = 500)
    public void scheduledFlush() {
        if (!buffer.isEmpty()) {
            flush();
        }
    }

    private void flush() {
        List<RequestEventEntity> batch = new ArrayList<>();
        RequestEventEntity event;
        while ((event = buffer.poll()) != null) {
            if (!requestRepository.existsByEventId(event.getEventId())) {
                batch.add(event);
            }
        }

        if (!batch.isEmpty()) {
            try {
                requestRepository.saveAll(batch);
                auditCounter.increment(batch.size());
                log.debug("Flushed {} audit events", batch.size());
            } catch (Exception e) {
                log.error("Error flushing audit batch: {}", e.getMessage());
                buffer.addAll(batch);
            }
        }
    }
}