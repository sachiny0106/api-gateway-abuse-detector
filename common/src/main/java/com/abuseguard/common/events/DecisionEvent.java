package com.abuseguard.common.events;

import com.abuseguard.common.model.ActionType;
import com.abuseguard.common.model.Decision;

import java.time.Instant;
import java.util.UUID;

public record DecisionEvent(
    UUID eventId,
    UUID requestEventId,
    Instant timestamp,
    String ip,
    String apiKey,
    ActionType action,
    double score,
    String reason,
    Instant expiresAt,
    String triggeredRules
) {
    public static Builder builder() {
        return new Builder();
    }

    public static Builder fromRequestEvent(RequestEvent requestEvent, Decision decision, String triggeredRules) {
        return new Builder()
            .requestEventId(requestEvent.eventId())
            .ip(requestEvent.ip())
            .apiKey(requestEvent.apiKey())
            .action(decision.action())
            .score(decision.score())
            .reason(decision.reason())
            .expiresAt(decision.expiresAt())
            .triggeredRules(triggeredRules);
    }

    public static class Builder {
        private UUID eventId = UUID.randomUUID();
        private UUID requestEventId;
        private Instant timestamp = Instant.now();
        private String ip;
        private String apiKey;
        private ActionType action = ActionType.ALLOW;
        private double score = 0.0;
        private String reason;
        private Instant expiresAt;
        private String triggeredRules;

        public Builder eventId(UUID eventId) { this.eventId = eventId; return this; }
        public Builder requestEventId(UUID requestEventId) { this.requestEventId = requestEventId; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder ip(String ip) { this.ip = ip; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder action(ActionType action) { this.action = action; return this; }
        public Builder score(double score) { this.score = score; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder triggeredRules(String triggeredRules) { this.triggeredRules = triggeredRules; return this; }

        public DecisionEvent build() {
            return new DecisionEvent(eventId, requestEventId, timestamp, ip, apiKey,
                action, score, reason, expiresAt, triggeredRules);
        }
    }
}