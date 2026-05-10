package com.abuseguard.gateway.ratelimit;

public record RateLimitResult(
    boolean allowed,
    long currentCount,
    long limit,
    long remaining
) {
    public String getResetHeader() {
        return String.valueOf(limit);
    }
}