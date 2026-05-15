package com.abuseguard.auditor.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_events")
public class RequestEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "ip", nullable = false)
    private String ip;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    @Column(name = "method")
    private String method;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "payload_size")
    private Integer payloadSize;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "forwarded_for")
    private String forwardedFor;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Integer getPayloadSize() { return payloadSize; }
    public void setPayloadSize(Integer payloadSize) { this.payloadSize = payloadSize; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getForwardedFor() { return forwardedFor; }
    public void setForwardedFor(String forwardedFor) { this.forwardedFor = forwardedFor; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}