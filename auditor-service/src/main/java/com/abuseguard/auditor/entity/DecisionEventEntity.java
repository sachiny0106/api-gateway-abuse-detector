package com.abuseguard.auditor.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "decision_events")
public class DecisionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "request_event_id")
    private UUID requestEventId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "ip")
    private String ip;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "score")
    private Double score;

    @Column(name = "reason")
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "triggered_rules")
    private String triggeredRules;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getRequestEventId() { return requestEventId; }
    public void setRequestEventId(UUID requestEventId) { this.requestEventId = requestEventId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(String triggeredRules) { this.triggeredRules = triggeredRules; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}