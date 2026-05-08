package com.abuseguard.common.events;

import java.time.Instant;
import java.util.UUID;

public record RequestEvent(
    UUID eventId,
    Instant timestamp,
    String ip,
    String apiKey,
    String endpoint,
    String method,
    int statusCode,
    int payloadSize,
    String userAgent,
    int latencyMs,
    String contentType,
    String forwardedFor
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID eventId = UUID.randomUUID();
        private Instant timestamp = Instant.now();
        private String ip;
        private String apiKey;
        private String endpoint;
        private String method = "GET";
        private int statusCode = 200;
        private int payloadSize = 0;
        private String userAgent;
        private int latencyMs = 0;
        private String contentType;
        private String forwardedFor;

        public Builder eventId(UUID eventId) { this.eventId = eventId; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder ip(String ip) { this.ip = ip; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public Builder payloadSize(int payloadSize) { this.payloadSize = payloadSize; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder latencyMs(int latencyMs) { this.latencyMs = latencyMs; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder forwardedFor(String forwardedFor) { this.forwardedFor = forwardedFor; return this; }

        public RequestEvent build() {
            return new RequestEvent(eventId, timestamp, ip, apiKey, endpoint, method,
                statusCode, payloadSize, userAgent, latencyMs, contentType, forwardedFor);
        }
    }
}