package com.abuseguard.common.model;

import java.time.Instant;

public record Decision(
    ActionType action,
    double score,
    String reason,
    Instant expiresAt
) {
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}